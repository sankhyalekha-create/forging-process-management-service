package com.jangid.forging_process_management_service.service.gst;

import com.jangid.forging_process_management_service.entities.gst.Invoice;
import com.jangid.forging_process_management_service.entities.gst.InvoiceDispatchBatch;
import com.jangid.forging_process_management_service.entities.gst.InvoiceLineItem;
import com.jangid.forging_process_management_service.entities.gst.InvoiceStatus;
import com.jangid.forging_process_management_service.entities.gst.InvoiceType;
import com.jangid.forging_process_management_service.entities.gst.TransportationMode;
import com.jangid.forging_process_management_service.entities.dispatch.DispatchBatch;
import com.jangid.forging_process_management_service.entities.dispatch.ProcessedItemDispatchBatch;
import com.jangid.forging_process_management_service.entities.workflow.ItemWorkflow;
import com.jangid.forging_process_management_service.entities.buyer.BuyerEntity;
import com.jangid.forging_process_management_service.entities.vendor.VendorEntity;
import com.jangid.forging_process_management_service.entities.order.OrderItemWorkflow;
import com.jangid.forging_process_management_service.entities.order.WorkType;
import com.jangid.forging_process_management_service.entities.settings.TenantInvoiceSettings;
import com.jangid.forging_process_management_service.dto.gst.InvoiceGenerationRequest;
import com.jangid.forging_process_management_service.repositories.gst.InvoiceRepository;
import com.jangid.forging_process_management_service.repositories.gst.InvoiceDispatchBatchRepository;
import com.jangid.forging_process_management_service.repositories.buyer.BuyerEntityRepository;
import com.jangid.forging_process_management_service.repositories.vendor.VendorEntityRepository;
import com.jangid.forging_process_management_service.repositories.workflow.ItemWorkflowRepository;
import com.jangid.forging_process_management_service.repositories.order.OrderItemWorkflowRepository;
import com.jangid.forging_process_management_service.assemblers.gst.InvoiceLineItemAssembler;
import com.jangid.forging_process_management_service.service.dispatch.DispatchBatchService;
import com.jangid.forging_process_management_service.service.settings.TenantSettingsService;
import com.jangid.forging_process_management_service.service.TenantService;
import com.jangid.forging_process_management_service.utils.ConvertorUtils;
import com.jangid.forging_process_management_service.utils.GSTUtils;
import com.jangid.forging_process_management_service.utils.NumberToWordsConverter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class InvoiceService {

  private final InvoiceRepository invoiceRepository;
  private final InvoiceDispatchBatchRepository invoiceDispatchBatchRepository;
  private final ItemWorkflowRepository itemWorkflowRepository;
  private final OrderItemWorkflowRepository orderItemWorkflowRepository;
  private final InvoiceLineItemAssembler invoiceLineItemAssembler;
  private final DispatchBatchService dispatchBatchService;
  private final TenantSettingsService tenantSettingsService;
  private final TenantService tenantService;
  private final BuyerEntityRepository buyerEntityRepository;
  private final VendorEntityRepository vendorEntityRepository;

  /**
   * UNIFIED INVOICE GENERATION METHOD
   * Handles all invoice generation scenarios:
   * - Single dispatch batch
   * - Multiple dispatch batches
   * - Manual invoices (no dispatch batches)
   * - Custom pricing and GST rates
   * - Transportation details
   */
  public Invoice generateInvoice(Long tenantId, InvoiceGenerationRequest request) {
    tenantService.validateTenantExists(tenantId);

    // Handle manual invoices differently
    if (request.isManualInvoice()) {
      log.info("Generating manual invoice for tenant: {}", tenantId);
      return generateManualInvoice(tenantId, request);
    }

    // Standard dispatch batch invoice
    log.info("Generating invoice for tenant: {} from {} dispatch batch(es)",
             tenantId, request.getDispatchBatchIds().size());

    if (request.getDispatchBatchIds() == null || request.getDispatchBatchIds().isEmpty()) {
      throw new IllegalArgumentException("At least one dispatch batch ID is required for non-manual invoices");
    }

    // Validate and fetch all dispatch batches
    List<DispatchBatch> dispatchBatches = request.getDispatchBatchIds().stream()
        .map(dispatchBatchService::getDispatchBatchById)
        .toList();

    // Validate dispatch batches for invoicing
    validateDispatchBatchesForInvoicing(dispatchBatches, tenantId);

    // Create invoice from request
    Invoice invoice = createInvoiceFromRequest(tenantId, dispatchBatches, request);
    Invoice savedInvoice = invoiceRepository.save(invoice);

    // Create junction records linking all dispatch batches to this invoice
    createInvoiceDispatchBatchLinks(savedInvoice, dispatchBatches);

    // Update dispatch batch status to INVOICE_DRAFT_CREATED to prevent duplicate draft invoices
    // This ensures the same batch cannot be used to create another invoice until this one is approved or deleted
    dispatchBatchService.updateMultipleBatchesToInvoiceDraftCreated(request.getDispatchBatchIds());
    
    log.info("Successfully generated DRAFT invoice with temporary number: {} for {} dispatch batch(es). " +
             "Final invoice number will be assigned upon approval (GST compliance). " +
             "Dispatch batches moved to INVOICE_DRAFT_CREATED status to prevent duplicate invoicing.",
             savedInvoice.getInvoiceNumber(), request.getDispatchBatchIds().size());

    return savedInvoice;
  }

  /**
   * Generate manual invoice without dispatch batches
   */
  private Invoice generateManualInvoice(Long tenantId, InvoiceGenerationRequest request) {
    if (request.getManualLineItems() == null || request.getManualLineItems().isEmpty()) {
      throw new IllegalArgumentException("At least one line item is required for manual invoices");
    }

    if (request.getRecipientBuyerEntityId() == null) {
      throw new IllegalArgumentException("Recipient buyer entity is required for manual invoices");
    }

    // Validate all line items have the same work type
    validateManualLineItemsWorkType(request.getManualLineItems());

    // Fetch recipient
    BuyerEntity recipientBuyerEntity = buyerEntityRepository.findByIdAndDeletedFalse(request.getRecipientBuyerEntityId())
        .orElseThrow(() -> new IllegalArgumentException("Buyer entity not found with ID: " + request.getRecipientBuyerEntityId()));

    // Validate buyer entity has required GST fields for invoicing
    validateBuyerEntityForInvoicing(recipientBuyerEntity);

    // Create invoice
    Invoice invoice = createManualInvoiceFromRequest(tenantId, recipientBuyerEntity, request);
    Invoice savedInvoice = invoiceRepository.save(invoice);

    log.info("Successfully generated manual DRAFT invoice with temporary number: {} with {} line items",
             savedInvoice.getInvoiceNumber(), request.getManualLineItems().size());

    return savedInvoice;
  }

  /**
   * Create manual invoice from request
   */
  private Invoice createManualInvoiceFromRequest(Long tenantId, BuyerEntity recipientBuyerEntity,
                                                 InvoiceGenerationRequest request) {
    // Generate temporary invoice number
    String invoiceNumber = request.getInvoiceNumber() != null ?
        request.getInvoiceNumber() :
        generateTemporaryInvoiceNumber();

    // Parse dates
    LocalDateTime invoiceDate = ConvertorUtils.convertStringToLocalDateTime(request.getInvoiceDate());
    LocalDate dueDate = (request.getDueDate() != null && !request.getDueDate().trim().isEmpty()) ?
        LocalDate.parse(request.getDueDate()) :
        null;
    LocalDate customerPoDate = (request.getCustomerPoDate() != null && !request.getCustomerPoDate().trim().isEmpty()) ?
        LocalDate.parse(request.getCustomerPoDate()) :
        null;

    // Fetch settings
    TenantInvoiceSettings invoiceSettings = tenantSettingsService.getInvoiceSettings(tenantId);
    var tenant = tenantService.getTenantById(tenantId);

    // Determine inter-state
    String supplierStateCode = tenant.getStateCode();
    String recipientStateCode = recipientBuyerEntity.getStateCode();
    boolean isInterState = !supplierStateCode.equals(recipientStateCode);

    // Determine predominant WorkType from manual line items OR from request to get appropriate terms and conditions
    WorkType predominantWorkType;
    if (request.getWorkType() != null && !request.getWorkType().trim().isEmpty()) {
      // Use work type from request if provided (user selection)
      predominantWorkType = getWorkTypeFromRequest(request);
      log.info("Using WorkType {} from request for manual invoice", predominantWorkType);
    } else {
      // Fall back to determining from line items
      predominantWorkType = determineWorkTypeFromManualLineItems(request.getManualLineItems());
      log.info("Determined WorkType {} from manual line items", predominantWorkType);
    }
    
    // Get terms and conditions based on work type (persisted for legal compliance)
    String termsAndConditions = null;
    if (invoiceSettings != null) {
      if (predominantWorkType == WorkType.JOB_WORK_ONLY) {
        termsAndConditions = invoiceSettings.getJobWorkTermsAndConditions();
      } else if (predominantWorkType == WorkType.WITH_MATERIAL) {
        termsAndConditions = invoiceSettings.getMaterialTermsAndConditions();
      }
    }

    // Create invoice
    Invoice.InvoiceBuilder invoiceBuilder = Invoice.builder()
        .invoiceNumber(invoiceNumber)
        .invoiceDate(invoiceDate)
        .invoiceType(InvoiceType.TAX_INVOICE)
        .isManualInvoice(true)
        .workType(predominantWorkType) // Store work type for invoice numbering logic during approval
        .tenant(tenant)
        .recipientBuyerEntity(recipientBuyerEntity)
        .customerPoNumber(request.getCustomerPoNumber())
        .customerPoDate(customerPoDate)
        .dueDate(dueDate)
        .supplierGstin(tenant.getGstin())
        .supplierName(tenant.getTenantName())
        .supplierAddress(tenant.getAddress())
        .supplierStateCode(supplierStateCode)
        .placeOfSupply(recipientStateCode)
        .isInterState(isInterState)
        .termsAndConditions(termsAndConditions)
        .status(InvoiceStatus.DRAFT);

    // Add transportation details
    if (request.getTransportationMode() != null) {
      try {
        invoiceBuilder.transportationMode(TransportationMode.valueOf(request.getTransportationMode().toUpperCase()));
      } catch (IllegalArgumentException e) {
        log.warn("Invalid transportation mode: {}", request.getTransportationMode());
      }
    }
    invoiceBuilder
        .transportationDistance(request.getTransportationDistance())
        .transporterName(request.getTransporterName())
        .transporterId(request.getTransporterId())
        .vehicleNumber(request.getVehicleNumber());

    // Add settings-based data (bank details)
    if (invoiceSettings != null) {
      invoiceBuilder
          .bankName(invoiceSettings.getBankName())
          .accountNumber(invoiceSettings.getAccountNumber())
          .ifscCode(invoiceSettings.getIfscCode());
    }

    Invoice invoice = invoiceBuilder.build();

    // Create line items
    BigDecimal totalTaxableValue = BigDecimal.ZERO;
    BigDecimal totalCgst = BigDecimal.ZERO;
    BigDecimal totalSgst = BigDecimal.ZERO;
    BigDecimal totalIgst = BigDecimal.ZERO;

    int lineNumber = 1;
    for (InvoiceGenerationRequest.ManualInvoiceLineItem item : request.getManualLineItems()) {
      BigDecimal taxableValue = item.getQuantity().multiply(item.getUnitPrice());
      BigDecimal cgstAmount = isInterState ? BigDecimal.ZERO : taxableValue.multiply(item.getCgstRate()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
      BigDecimal sgstAmount = isInterState ? BigDecimal.ZERO : taxableValue.multiply(item.getSgstRate()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
      BigDecimal igstAmount = isInterState ? taxableValue.multiply(item.getIgstRate()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

      InvoiceLineItem lineItem = InvoiceLineItem.builder()
          .invoice(invoice)
          .lineNumber(lineNumber++)
          .itemName(item.getItemName())
          .hsnCode(item.getHsnCode())
          .quantity(item.getQuantity())
          .unitPrice(item.getUnitPrice())
          .taxableValue(taxableValue)
          .cgstRate(item.getCgstRate())
          .sgstRate(item.getSgstRate())
          .igstRate(item.getIgstRate())
          .cgstAmount(cgstAmount)
          .sgstAmount(sgstAmount)
          .igstAmount(igstAmount)
          .lineTotal(taxableValue.add(cgstAmount).add(sgstAmount).add(igstAmount))
          .build();

      invoice.addLineItem(lineItem);

      totalTaxableValue = totalTaxableValue.add(taxableValue);
      totalCgst = totalCgst.add(cgstAmount);
      totalSgst = totalSgst.add(sgstAmount);
      totalIgst = totalIgst.add(igstAmount);
    }

    // Set totals
    invoice.setTotalTaxableValue(totalTaxableValue);
    invoice.setTotalCgstAmount(totalCgst);
    invoice.setTotalSgstAmount(totalSgst);
    invoice.setTotalIgstAmount(totalIgst);
    invoice.setTotalInvoiceValue(totalTaxableValue.add(totalCgst).add(totalSgst).add(totalIgst));

    // Set amount in words
    invoice.setAmountInWords(NumberToWordsConverter.convertToWords(invoice.getTotalInvoiceValue()));

    return invoice;
  }

  /**
   * UNIFIED METHOD: Create invoice from InvoiceGenerationRequest
   * Handles all customizations and scenarios
   */
  private Invoice createInvoiceFromRequest(Long tenantId, List<DispatchBatch> dispatchBatches,
                                          InvoiceGenerationRequest request) {
    // Use first batch as primary for basic invoice details
    DispatchBatch primaryBatch = dispatchBatches.get(0);

    // Determine invoice type (default to TAX_INVOICE for standard GST invoices)
    InvoiceType invoiceType = InvoiceType.TAX_INVOICE;
    if (request.getInvoiceType() != null) {
      try {
        invoiceType = InvoiceType.valueOf(request.getInvoiceType().toUpperCase());
      } catch (IllegalArgumentException e) {
        log.warn("Invalid invoice type: {}, using TAX_INVOICE", request.getInvoiceType());
      }
    }

    // Generate temporary invoice number for DRAFT
    // Actual invoice number will be assigned when invoice is approved (for GST compliance)
    String invoiceNumber = request.getInvoiceNumber() != null ?
        request.getInvoiceNumber() :
        generateTemporaryInvoiceNumber();

    // Use custom dates or defaults
    // Parse invoice date from String (format: YYYY-MM-DDTHH:mm) to LocalDateTime
    LocalDateTime invoiceDate = ConvertorUtils.convertStringToLocalDateTime(request.getInvoiceDate());

    // Validate that invoice date is not before any dispatch batch's dispatchReadyAt
    for (DispatchBatch batch : dispatchBatches) {
      if (batch.getDispatchReadyAt() != null && invoiceDate.isBefore(batch.getDispatchReadyAt())) {
        throw new IllegalArgumentException(
            String.format("Invoice date (%s) cannot be before dispatch batch %s's ready date (%s)",
                invoiceDate, batch.getDispatchBatchNumber(), batch.getDispatchReadyAt())
        );
      }
    }

    // Parse due date from String (format: YYYY-MM-DD) to LocalDate
    LocalDate dueDate = (request.getDueDate() != null && !request.getDueDate().trim().isEmpty()) ?
        LocalDate.parse(request.getDueDate()) :
        null;

    // Determine recipient (use override if provided, otherwise from dispatch batch)
    BuyerEntity recipientBuyerEntity = null;
    VendorEntity recipientVendorEntity = null;

    if (request.getRecipientBuyerEntityId() != null) {
      // Fetch the buyer entity by the provided ID
      Optional<BuyerEntity> recipientBuyerEntityOptional = buyerEntityRepository.findByIdAndDeletedFalse(request.getRecipientBuyerEntityId());
      if (recipientBuyerEntityOptional.isPresent()) {
        recipientBuyerEntity = recipientBuyerEntityOptional.get();
      } else {
        log.warn("Buyer entity with ID {} not found, using billing entity from dispatch batch", request.getRecipientBuyerEntityId());
        recipientBuyerEntity = primaryBatch.getBillingEntity();
      }
    } else if (request.getRecipientVendorEntityId() != null) {
       Optional<VendorEntity> recipientVendorEntityOptional = vendorEntityRepository.findByIdAndDeletedFalse(request.getRecipientVendorEntityId());
       if (recipientVendorEntityOptional.isPresent()) {
         recipientVendorEntity = recipientVendorEntityOptional.get();
       } else {
         log.warn("Vendor entity with ID {} not found", request.getRecipientVendorEntityId());
       }
    } else {
      // Default to billing entity from dispatch batch
      recipientBuyerEntity = primaryBatch.getBillingEntity();
    }

    // Validate buyer entity has required fields for invoicing (if buyer entity is the recipient)
    if (recipientBuyerEntity != null) {
      validateBuyerEntityForInvoicing(recipientBuyerEntity);
    }

    // Extract order details from dispatch batch (via ProcessedItem → ItemWorkflow → OrderItemWorkflow → Order)
    Long orderId = extractOrderIdFromDispatchBatch(primaryBatch);

    // Parse customer PO date from String (format: YYYY-MM-DD) to LocalDate
    LocalDate customerPoDate = (request.getCustomerPoDate() != null && !request.getCustomerPoDate().trim().isEmpty()) ?
        LocalDate.parse(request.getCustomerPoDate()) :
        null;

    // Determine WorkType to get appropriate terms and conditions
    WorkType workType = determineWorkTypeFromDispatchBatches(dispatchBatches, request);
    TenantInvoiceSettings invoiceSettings = tenantSettingsService.getInvoiceSettings(tenantId);
    
    // Get terms and conditions based on work type (persisted for legal compliance)
    String termsAndConditions = null;
    if (invoiceSettings != null) {
      if (workType == WorkType.JOB_WORK_ONLY) {
        termsAndConditions = invoiceSettings.getJobWorkTermsAndConditions();
      } else if (workType == WorkType.WITH_MATERIAL) {
        termsAndConditions = invoiceSettings.getMaterialTermsAndConditions();
      }
    }

    // Get bank details from invoice settings (persisted for data integrity)
    String bankName = invoiceSettings != null ? invoiceSettings.getBankName() : null;
    String accountNumber = invoiceSettings != null ? invoiceSettings.getAccountNumber() : null;
    String ifscCode = invoiceSettings != null ? invoiceSettings.getIfscCode() : null;

    // Build invoice
    // Note: Dispatch batch links are created separately via InvoiceDispatchBatch junction table
    Invoice.InvoiceBuilder invoiceBuilder = Invoice.builder()
        .invoiceNumber(invoiceNumber)
        .invoiceDate(invoiceDate)
        .dueDate(dueDate)
        .status(InvoiceStatus.DRAFT)
        .invoiceType(invoiceType)
        .workType(workType) // Store work type for invoice numbering logic during approval
        .tenant(primaryBatch.getTenant())
        // Order reference (for traceability and reporting)
        .orderId(orderId)
        .customerPoNumber(request.getCustomerPoNumber())
        .customerPoDate(customerPoDate)
        .recipientBuyerEntity(recipientBuyerEntity)
        .recipientVendorEntity(recipientVendorEntity)
        // Supplier details - persisted from Tenant at invoice generation (for data integrity)
        .supplierGstin(primaryBatch.getTenant().getGstin())
        .supplierName(primaryBatch.getTenant().getTenantName())
        .supplierAddress(primaryBatch.getTenant().getAddress())
        .supplierStateCode(primaryBatch.getTenant().getStateCode())
        .placeOfSupply(GSTUtils.getPlaceOfSupply(recipientBuyerEntity, recipientVendorEntity))
        .paymentTerms(request.getPaymentTerms())
        .termsAndConditions(termsAndConditions)
        // Bank details - persisted from invoice settings at invoice generation
        .bankName(bankName)
        .accountNumber(accountNumber)
        .ifscCode(ifscCode);

    // Add transportation details if provided
    if (request.getTransportationMode() != null) {
      try {
        TransportationMode transportMode = TransportationMode.valueOf(request.getTransportationMode().toUpperCase());
        invoiceBuilder.transportationMode(transportMode);
      } catch (IllegalArgumentException e) {
        log.warn("Invalid transportation mode: {}", request.getTransportationMode());
      }
    }

    // Parse dispatch date from String (format: YYYY-MM-DDTHH:mm) to LocalDateTime
    LocalDateTime dispatchDate = ConvertorUtils.convertStringToLocalDateTime(request.getDispatchDate());

    invoiceBuilder
        .transportationDistance(request.getTransportationDistance())
        .transporterName(request.getTransporterName())
        .transporterId(request.getTransporterId())
        .vehicleNumber(request.getVehicleNumber())
        .dispatchDate(dispatchDate);

    Invoice invoice = invoiceBuilder.build();

    // Calculate amounts (with or without custom pricing/GST rates)
    calculateInvoiceAmountsFromRequest(invoice, dispatchBatches, request);

    // Set amount in words after total invoice value is calculated
    if (invoice.getTotalInvoiceValue() != null) {
      invoice.setAmountInWords(
          NumberToWordsConverter.convertToWords(invoice.getTotalInvoiceValue())
      );
    }

    return invoice;
  }

  /**
   * Create InvoiceDispatchBatch junction records linking all dispatch batches to the invoice.
   */
  private void createInvoiceDispatchBatchLinks(Invoice invoice, List<DispatchBatch> dispatchBatches) {
    log.info("Creating invoice-dispatch batch links for invoice: {} with {} dispatch batches",
             invoice.getInvoiceNumber(), dispatchBatches.size());

    int sequenceOrder = 1;
    for (DispatchBatch dispatchBatch : dispatchBatches) {
      InvoiceDispatchBatch invoiceDispatchBatch = InvoiceDispatchBatch.builder()
          .invoice(invoice)
          .dispatchBatch(dispatchBatch)
          .sequenceOrder(sequenceOrder++)
          .tenant(invoice.getTenant())
          .build();

      invoiceDispatchBatchRepository.save(invoiceDispatchBatch);
      
      log.debug("Created invoice-dispatch batch link: invoice={}, dispatchBatch={}, sequenceOrder={}",
               invoice.getId(), dispatchBatch.getId(), invoiceDispatchBatch.getSequenceOrder());
    }

    log.info("Successfully created {} invoice-dispatch batch links", dispatchBatches.size());
  }

  /**
   * Calculate invoice amounts from request (supports custom pricing and GST rates)
   * Uses TenantInvoiceSettings to determine GST rates based on WorkType
   */
  private void calculateInvoiceAmountsFromRequest(Invoice invoice, List<DispatchBatch> dispatchBatches,
                                                   InvoiceGenerationRequest request) {
    log.info("Calculating invoice amounts for {} dispatch batches with request parameters",
             dispatchBatches.size());

    // Check if inter-state transaction
    invoice.updateInterStateFlag();
    boolean isInterState = invoice.isInterState();

    // Validate that all required fields are provided by frontend
    request.validateRequiredFields(isInterState);

    // Get total GST rate from request
    BigDecimal totalGstRate = request.getTotalGstRate(isInterState);
    log.info("Using GST rate from request: {}%", totalGstRate);

    int lineNumber = 1;

    // Create line items for each dispatch batch using request values
    for (DispatchBatch dispatchBatch : dispatchBatches) {
      ProcessedItemDispatchBatch processedItem = dispatchBatch.getProcessedItemDispatchBatch();

      if (processedItem == null) {
        log.warn("Dispatch batch {} has no processed item, skipping", dispatchBatch.getDispatchBatchNumber());
        continue;
      }

      try {
        InvoiceLineItem lineItem = createLineItemFromRequest(
            processedItem,
            lineNumber++,
            isInterState,
            totalGstRate,
            request
        );

        // Apply discount if specified
        if (request.getDiscountPercentage() != null &&
            request.getDiscountPercentage().compareTo(BigDecimal.ZERO) > 0) {
          lineItem.setDiscountPercentage(request.getDiscountPercentage());
          lineItem.calculateDiscountAmount();
          lineItem.calculateLineTotal();
        }

        invoice.addLineItem(lineItem);

      } catch (Exception e) {
        log.error("Error creating line item for processed item {}: {}",
                 processedItem.getId(), e.getMessage(), e);
        throw new RuntimeException("Failed to create invoice line item: " + e.getMessage(), e);
      }
    }

    // Calculate invoice totals from line items
    invoice.calculateTotals();

    log.info("Invoice calculated: {} line items, Total value: {}",
             invoice.getLineItems().size(), invoice.getTotalInvoiceValue());
  }

  /**
   * Simplified: Create invoice line item using values from request
   * No fallback logic - all values must be provided by frontend
   */
  private InvoiceLineItem createLineItemFromRequest(
      ProcessedItemDispatchBatch processedItem,
      int lineNumber,
      boolean isInterState,
      BigDecimal totalGstRate,
      InvoiceGenerationRequest request) {

    log.debug("Creating line item {} using request values", lineNumber);

    // Get ItemWorkflow for item details
    ItemWorkflow itemWorkflow = itemWorkflowRepository.findById(processedItem.getItemWorkflowId())
        .orElseThrow(() -> new RuntimeException(
            "ItemWorkflow not found with id: " + processedItem.getItemWorkflowId()));

    // Get quantity
    BigDecimal quantity = BigDecimal.valueOf(processedItem.getTotalDispatchPiecesCount());

    // Use unit rate from request (required)
    BigDecimal unitPrice = request.getUnitRate();

    // Build item description
    String itemName = itemWorkflow.getItem() != null ?
        itemWorkflow.getItem().getItemName() : "Unknown Item";

    // Use HSN code from request (required)
    String hsnCode = request.getHsnSacCode();

    // Create line item using assembler helper
    InvoiceLineItem lineItem = invoiceLineItemAssembler.createWithCalculations(
        lineNumber,
        itemName,
        "PROCESSED_ITEM", // Indicates this is from processed item
        hsnCode,
        quantity,
        unitPrice,
        isInterState,
        totalGstRate,
        BigDecimal.ZERO, // Discount will be applied later if specified
        itemWorkflow.getId(),
        processedItem.getId()
    );

    log.debug("Created line item: {} x {} @ {} = {}, GST={}%",
             itemName, quantity, unitPrice, lineItem.getTaxableValue(), totalGstRate);

    return lineItem;
  }

  /**
   * Generate temporary invoice number for DRAFT invoices
   * Format: DRAFT-{timestamp}-{randomId}
   * This ensures uniqueness without consuming the legal invoice sequence
   */
  private String generateTemporaryInvoiceNumber() {
    String timestamp = String.valueOf(System.currentTimeMillis());
    String randomId = String.valueOf((int)(Math.random() * 10000));
    return "DRAFT-" + timestamp + "-" + randomId;
  }

  /**
   * Generate final invoice number when invoice is approved
   * This is the legally binding invoice number that follows GST sequential numbering rules
   * Only called during invoice approval to ensure no gaps in numbering
   */
  private String generateFinalInvoiceNumber(Long tenantId, Invoice invoice) {
    TenantInvoiceSettings settings = tenantSettingsService.getInvoiceSettings(tenantId);

    // Use the work type stored in the invoice entity (set during invoice creation)
    WorkType workType = invoice.getWorkType();

    String prefix;
    Integer currentSequence;
    String seriesFormat;
    String sequenceType;

    if (workType == WorkType.JOB_WORK_ONLY) {
      prefix = settings.getJobWorkInvoicePrefix();
      currentSequence = settings.getJobWorkCurrentSequence();
      seriesFormat = settings.getJobWorkSeriesFormat();
      sequenceType = "JOB_WORK_INVOICE";
    } else {
      // WorkType.WITH_MATERIAL
      prefix = settings.getMaterialInvoicePrefix();
      currentSequence = settings.getMaterialCurrentSequence();
      seriesFormat = settings.getMaterialSeriesFormat();
      sequenceType = "MATERIAL_INVOICE";
    }

    String invoiceNumber = prefix + seriesFormat + String.format("%05d", currentSequence);

    // Update sequence - this permanently consumes the number
    tenantSettingsService.incrementInvoiceSequence(tenantId, sequenceType);

    log.info("Generated final invoice number: {} (Invoice Type: {}, WorkType: {}, Sequence: {})",
             invoiceNumber, invoice.isManualInvoice() ? "Manual" : "Batch-based", workType, currentSequence);

    return invoiceNumber;
  }

  /**
   * Determine the WorkType from dispatch batches by checking the associated OrderItem.
   * For non-order batches, uses the workType from the request if provided.
   */
  private WorkType determineWorkTypeFromDispatchBatches(List<DispatchBatch> dispatchBatches, InvoiceGenerationRequest request) {

    // Get the first batch's work type
    DispatchBatch firstBatch = dispatchBatches.get(0);
    ProcessedItemDispatchBatch processedItem = firstBatch.getProcessedItemDispatchBatch();

    if (processedItem == null || processedItem.getItemWorkflowId() == null) {
      log.warn("No processed item or itemWorkflowId found, using workType from request or defaulting to WITH_MATERIAL");
      return getWorkTypeFromRequest(request);
    }

    // Get ItemWorkflow from itemWorkflowId
    Optional<ItemWorkflow> itemWorkflowOpt = itemWorkflowRepository.findById(processedItem.getItemWorkflowId());
    if (!itemWorkflowOpt.isPresent()) {
      log.warn("ItemWorkflow not found for ID: {} - using workType from request or defaulting to WITH_MATERIAL", 
               processedItem.getItemWorkflowId());
      return getWorkTypeFromRequest(request);
    }

    ItemWorkflow itemWorkflow = itemWorkflowOpt.get();

    // Get OrderItemWorkflow from ItemWorkflow
    Optional<OrderItemWorkflow> orderItemWorkflowOpt = orderItemWorkflowRepository.findByItemWorkflowId(itemWorkflow.getId());
    if (!orderItemWorkflowOpt.isPresent()) {
      log.info("OrderItemWorkflow not found for ItemWorkflow ID: {} - this is a non-order batch. Using workType from request", 
               itemWorkflow.getId());
      return getWorkTypeFromRequest(request);
    }

    // Get WorkType from OrderItem
    WorkType workType = orderItemWorkflowOpt.get().getOrderItem().getWorkType();

    log.info("Determined WorkType as {} from dispatch batches", workType);
     return workType;
   }

  /**
   * Get WorkType from the request, defaulting to WITH_MATERIAL if not provided
   */
  private WorkType getWorkTypeFromRequest(InvoiceGenerationRequest request) {
    if (request != null && request.getWorkType() != null && !request.getWorkType().trim().isEmpty()) {
      try {
        WorkType workType = WorkType.valueOf(request.getWorkType().toUpperCase());
        log.info("Using WorkType {} from request for non-order batch", workType);
        return workType;
      } catch (IllegalArgumentException e) {
        log.warn("Invalid workType '{}' in request, defaulting to WITH_MATERIAL", request.getWorkType());
        return WorkType.WITH_MATERIAL;
      }
    }
    log.debug("No workType provided in request, defaulting to WITH_MATERIAL");
    return WorkType.WITH_MATERIAL;
  }

  /**
   * Determine WorkType from manual invoice line items.
   * Since validation ensures all line items have the same work type, simply pick from first item.
   * Note: This method is called after validateManualLineItemsWorkType() which ensures uniformity.
   */
  private WorkType determineWorkTypeFromManualLineItems(List<InvoiceGenerationRequest.ManualInvoiceLineItem> lineItems) {
    if (lineItems == null || lineItems.isEmpty()) {
      log.warn("No manual line items found, defaulting to WITH_MATERIAL");
      return WorkType.WITH_MATERIAL;
    }

    // Get work type from first item (all items have same work type after validation)
    String workTypeStr = lineItems.get(0).getWorkType();
    
    if (workTypeStr == null || workTypeStr.trim().isEmpty()) {
      log.warn("Work type not specified in line items, defaulting to WITH_MATERIAL");
      return WorkType.WITH_MATERIAL;
    }

    try {
      WorkType workType = WorkType.valueOf(workTypeStr.toUpperCase());
      log.info("Determined WorkType as {} from {} manual line items (all items have same work type)", 
               workType, lineItems.size());
      return workType;
    } catch (IllegalArgumentException e) {
      log.warn("Invalid work type '{}' in line items, defaulting to WITH_MATERIAL", workTypeStr);
      return WorkType.WITH_MATERIAL;
    }
  }

  /**
   * Extract Order ID from DispatchBatch by traversing entity relationships
   * Returns null for non-order-based dispatch batches (valid scenario)
   */
  private Long extractOrderIdFromDispatchBatch(DispatchBatch dispatchBatch) {
    if (dispatchBatch == null) {
      log.warn("DispatchBatch is null, cannot extract orderId");
      return null;
    }

    ProcessedItemDispatchBatch processedItem = dispatchBatch.getProcessedItemDispatchBatch();
    if (processedItem == null || processedItem.getItemWorkflowId() == null) {
      log.debug("No processed item or itemWorkflowId found in dispatch batch {} (non-order-based)", 
               dispatchBatch.getDispatchBatchNumber());
      return null;
    }

    // Get ItemWorkflow from itemWorkflowId
    Optional<ItemWorkflow> itemWorkflowOpt = itemWorkflowRepository.findById(processedItem.getItemWorkflowId());
    if (itemWorkflowOpt.isEmpty()) {
      log.warn("ItemWorkflow not found for ID: {} in dispatch batch {}", 
               processedItem.getItemWorkflowId(), dispatchBatch.getDispatchBatchNumber());
      return null;
    }

    ItemWorkflow itemWorkflow = itemWorkflowOpt.get();

    // Get OrderItemWorkflow from ItemWorkflow - may not exist for non-order-based batches
    Optional<OrderItemWorkflow> orderItemWorkflowOpt = orderItemWorkflowRepository.findByItemWorkflowId(itemWorkflow.getId());
    if (orderItemWorkflowOpt.isEmpty()) {
      log.debug("OrderItemWorkflow not found for ItemWorkflow ID: {} - dispatch batch {} is not order-based (valid scenario)", 
               itemWorkflow.getId(), dispatchBatch.getDispatchBatchNumber());
      return null;
    }

    OrderItemWorkflow orderItemWorkflow = orderItemWorkflowOpt.get();

    // Get Order ID from OrderItem
    Long orderId = orderItemWorkflow.getOrderItem().getOrder().getId();
    
    log.debug("Extracted orderId {} from dispatch batch {}", orderId, dispatchBatch.getDispatchBatchNumber());
    return orderId;
  }

  /**
   * Get all invoices for a tenant with pagination
   */
  @Transactional(readOnly = true)
  public Page<Invoice> getInvoicesByTenant(Long tenantId, Pageable pageable) {
    tenantService.validateTenantExists(tenantId);
    return invoiceRepository.findByTenantIdAndDeletedFalse(tenantId, pageable);
  }

  /**
   * Get invoices by status
   */
  @Transactional(readOnly = true)
  public Page<Invoice> getInvoicesByStatus(Long tenantId, InvoiceStatus status, Pageable pageable) {
    tenantService.validateTenantExists(tenantId);
    return invoiceRepository.findByTenantIdAndStatusAndDeletedFalse(tenantId, status, pageable);
  }

  /**
   * Get invoice by ID
   */
  @Transactional(readOnly = true)
  public Invoice getInvoiceById(Long invoiceId) {
    return invoiceRepository.findById(invoiceId)
        .orElseThrow(() -> new RuntimeException("Invoice not found with id: " + invoiceId));
  }

  /**
   * Get invoice by dispatch batch ID
   */
  @Transactional(readOnly = true)
  public Invoice getInvoiceByDispatchBatchId(Long tenantId, Long dispatchBatchId) {
    tenantService.validateTenantExists(tenantId);
    
    // Find invoice that contains this dispatch batch
    List<InvoiceDispatchBatch> invoiceDispatchBatches = invoiceDispatchBatchRepository
        .findByDispatchBatchIdAndDeletedFalse(dispatchBatchId);
    
    if (invoiceDispatchBatches.isEmpty()) {
      throw new RuntimeException("No invoice found for dispatch batch ID: " + dispatchBatchId);
    }
    
    // Get the invoice from the first (and should be only) invoice-dispatch-batch link
    Invoice invoice = invoiceDispatchBatches.get(0).getInvoice();
    
    // Verify the invoice belongs to the tenant
    if (invoice.getTenant().getId() != tenantId) {
      throw new RuntimeException("Invoice does not belong to tenant: " + tenantId);
    }
    
    return invoice;
  }

  /**
   * Update invoice status to SENT when associated dispatch batch is dispatched.
   * This method is called automatically when a dispatch batch moves to DISPATCHED status.
   * Only updates GENERATED invoices to SENT status (DRAFT invoices are skipped).
   * 
   * @param tenantId The tenant ID for validation
   * @param dispatchBatchId The dispatch batch ID that was dispatched
   */
  public void updateInvoiceToSentOnDispatch(Long tenantId, Long dispatchBatchId) {
    tenantService.validateTenantExists(tenantId);
    
    // Find invoice that contains this dispatch batch
    List<InvoiceDispatchBatch> invoiceDispatchBatches = invoiceDispatchBatchRepository
        .findByDispatchBatchIdAndDeletedFalse(dispatchBatchId);
    
    if (invoiceDispatchBatches.isEmpty()) {
      log.warn("No invoice found for dispatched batch ID: {}. Skipping invoice status update.", dispatchBatchId);
      return;
    }
    
    // Get the invoice (there should be only one invoice per dispatch batch)
    Invoice invoice = invoiceDispatchBatches.get(0).getInvoice();
    
    // Verify the invoice belongs to the tenant
    if (invoice.getTenant().getId() != tenantId) {
      log.error("Invoice {} does not belong to tenant: {}. Skipping invoice status update.", 
                invoice.getId(), tenantId);
      return;
    }
    
    // Only update invoice status if it's currently GENERATED
    if (invoice.getStatus() == InvoiceStatus.GENERATED) {
      invoice.setStatus(InvoiceStatus.SENT);
      invoiceRepository.save(invoice);
      log.info("Invoice {} status updated from GENERATED to SENT for dispatched batch ID: {}", 
               invoice.getInvoiceNumber(), dispatchBatchId);
    } else {
      log.info("Invoice {} is in {} status. Skipping status update to SENT for dispatched batch ID: {}", 
               invoice.getInvoiceNumber(), invoice.getStatus(), dispatchBatchId);
    }
  }

  /**
   * Mark invoice as SENT (for manual invoices or user-triggered action)
   * This is typically used when goods are dispatched or invoice is shared with customer
   */
  @Transactional
  public Invoice markInvoiceAsSent(Long tenantId, Long invoiceId, String markedBy) {
    tenantService.validateTenantExists(tenantId);
    
    Invoice invoice = invoiceRepository.findById(invoiceId)
        .orElseThrow(() -> new IllegalArgumentException("Invoice not found with ID: " + invoiceId));
    
    // Check if invoice is deleted
    if (invoice.isDeleted()) {
      throw new IllegalArgumentException("Invoice has been deleted");
    }
    
    // Verify invoice belongs to the tenant
    if (invoice.getTenant().getId() != tenantId) {
      throw new IllegalArgumentException("Invoice does not belong to the specified tenant");
    }
    
    // Only GENERATED invoices can be marked as SENT
    if (invoice.getStatus() != InvoiceStatus.GENERATED) {
      throw new IllegalStateException(
        String.format("Only GENERATED invoices can be marked as SENT. Current status: %s", 
                      invoice.getStatus())
      );
    }
    
    // Update status to SENT
    invoice.setStatus(InvoiceStatus.SENT);
    Invoice savedInvoice = invoiceRepository.save(invoice);
    
    log.info("Invoice {} marked as SENT by {}. Status: GENERATED → SENT. " +
             "Invoice Type: {}", 
             invoice.getInvoiceNumber(), 
             markedBy,
             invoice.isManualInvoice() ? "Manual" : "Batch-based");
    
    return savedInvoice;
  }

  /**
   * Search invoices with filters
   */
  @Transactional(readOnly = true)
  public Page<Invoice> searchInvoices(Long tenantId, InvoiceStatus status, Long buyerEntityId,
                                      LocalDateTime fromDate, LocalDateTime toDate, String searchTerm,
                                      Pageable pageable) {
    tenantService.validateTenantExists(tenantId);
    return invoiceRepository.searchInvoices(tenantId, status, buyerEntityId, fromDate, toDate, searchTerm, pageable);
  }

  /**
   * Approve invoice - changes invoice status to GENERATED and dispatch batch to DISPATCH_INVOICE_APPROVED
   * CRITICAL: Assigns final invoice number at approval time (GST compliance - no gaps in sequence)
   * Handles both batch-based and manual invoices
   */
  public Invoice approveInvoice(Long tenantId, Long invoiceId, String approvedBy) {
    tenantService.validateTenantExists(tenantId);

    Invoice invoice = getInvoiceById(invoiceId);

    if (invoice.getStatus() != InvoiceStatus.DRAFT) {
      throw new IllegalStateException("Only DRAFT invoices can be approved. Current status: " + invoice.getStatus());
    }

    String oldInvoiceNumber = invoice.getInvoiceNumber();

    // Generate final invoice number (this consumes the sequence number permanently)
    String finalInvoiceNumber = generateFinalInvoiceNumber(tenantId, invoice);
    invoice.setInvoiceNumber(finalInvoiceNumber);

    // Update invoice status to GENERATED
    invoice.setStatus(InvoiceStatus.GENERATED);
    invoice.setApprovedBy(approvedBy);
    Invoice savedInvoice = invoiceRepository.save(invoice);

    // Update associated dispatch batches (only for batch-based invoices)
    if (!invoice.isManualInvoice()) {
      List<Long> dispatchBatchIds = invoice.getDispatchBatchIds();
      if (!dispatchBatchIds.isEmpty()) {
        dispatchBatchService.updateMultipleBatchesFromDraftToApproved(dispatchBatchIds);
        log.info("Updated {} dispatch batches from INVOICE_DRAFT_CREATED to DISPATCH_INVOICE_APPROVED status", 
                 dispatchBatchIds.size());
      }
      log.info("Batch-based invoice approved by {}. Temporary number {} replaced with final invoice number: {}. " +
               "Status: DRAFT → GENERATED. Associated dispatch batches: INVOICE_DRAFT_CREATED → DISPATCH_INVOICE_APPROVED",
               approvedBy, oldInvoiceNumber, finalInvoiceNumber);
    } else {
      log.info("Manual invoice approved by {}. Temporary number {} replaced with final invoice number: {}. " +
               "Status: DRAFT → GENERATED (No dispatch batches associated)",
               approvedBy, oldInvoiceNumber, finalInvoiceNumber);
    }

    return savedInvoice;
  }

  /**
   * Delete invoice - only DRAFT invoices can be deleted
   * Reverts associated dispatch batches back to READY_TO_DISPATCH status (for batch-based invoices)
   * Handles both batch-based and manual invoices
   */
  public void deleteInvoice(Long tenantId, Long invoiceId) {
    tenantService.validateTenantExists(tenantId);

    Invoice invoice = getInvoiceById(invoiceId);

    // Only DRAFT invoices can be deleted
    if (invoice.getStatus() != InvoiceStatus.DRAFT) {
      throw new IllegalStateException("Only DRAFT invoices can be deleted. Current status: " + invoice.getStatus() + 
                                      ". Approved invoices cannot be deleted.");
    }

    // Revert associated dispatch batches (only for batch-based invoices)
    if (!invoice.isManualInvoice()) {
      List<Long> dispatchBatchIds = invoice.getDispatchBatchIds();
      if (!dispatchBatchIds.isEmpty()) {
        // Process all associated dispatch batches
        for (Long batchId : dispatchBatchIds) {
          DispatchBatch batch = dispatchBatchService.getDispatchBatchById(batchId);
          
          // Check if batch is not already dispatched
          if (batch.getDispatchBatchStatus() == DispatchBatch.DispatchBatchStatus.DISPATCHED) {
            throw new IllegalStateException("Cannot delete invoice. One or more dispatch batches are already dispatched.");
          }
          
          // Validate batch is in expected status (should be INVOICE_DRAFT_CREATED)
          if (batch.getDispatchBatchStatus() != DispatchBatch.DispatchBatchStatus.INVOICE_DRAFT_CREATED) {
            log.warn("Expected batch {} to be in INVOICE_DRAFT_CREATED status, but found: {}. Attempting to revert anyway.",
                     batch.getDispatchBatchNumber(), batch.getDispatchBatchStatus());
          }
          
          // Revert to READY_TO_DISPATCH
          if (batch.getDispatchBatchStatus() != DispatchBatch.DispatchBatchStatus.READY_TO_DISPATCH) {
            dispatchBatchService.revertStatusFromDraftToReadyToDispatch(batch.getId());
            log.info("Reverted dispatch batch {} from INVOICE_DRAFT_CREATED to READY_TO_DISPATCH status", 
                     batch.getDispatchBatchNumber());
          }
        }
      }
    }

    // Delete all associated InvoiceLineItem records
    // InvoiceLineItem doesn't have soft delete fields, so we use orphanRemoval mechanism
    if (invoice.getLineItems() != null && !invoice.getLineItems().isEmpty()) {
      int lineItemCount = invoice.getLineItems().size();
      invoice.getLineItems().clear(); // This triggers orphanRemoval = true, deleting all line items
      log.info("Deleted {} line item(s) for invoice {} via orphan removal", lineItemCount, invoice.getInvoiceNumber());
    }

    // Soft delete all associated InvoiceDispatchBatch records (only for batch-based invoices)
    if (!invoice.isManualInvoice() && invoice.getInvoiceDispatchBatches() != null && !invoice.getInvoiceDispatchBatches().isEmpty()) {
      LocalDateTime deletionTime = LocalDateTime.now();
      invoice.getInvoiceDispatchBatches().forEach(invoiceDispatchBatch -> {
        invoiceDispatchBatch.setDeleted(true);
        invoiceDispatchBatch.setDeletedAt(deletionTime);
      });
      log.info("Soft deleted {} invoice-dispatch batch link(s) for invoice {}", 
               invoice.getInvoiceDispatchBatches().size(), invoice.getInvoiceNumber());
    }

    // Soft delete invoice
    invoice.setDeleted(true);
    invoice.setDeletedAt(LocalDateTime.now());
    invoiceRepository.save(invoice);

    if (invoice.isManualInvoice()) {
      log.info("DRAFT manual invoice {} and all associated records deleted successfully (No dispatch batches to revert).",
               invoice.getInvoiceNumber());
    } else {
      log.info("DRAFT invoice {} and all associated records deleted successfully. " +
               "Associated dispatch batches reverted from INVOICE_DRAFT_CREATED to READY_TO_DISPATCH.",
               invoice.getInvoiceNumber());
    }
  }

  /**
   * Cancel invoice - only GENERATED invoices with no payments can be cancelled
   * Sets invoice status to CANCELLED and reverts associated dispatch batches back to READY_TO_DISPATCH status
   * Handles both batch-based and manual invoices
   * 
   * BUSINESS RULE: 
   * - DRAFT invoices: Can be deleted (soft-delete + rollback)
   * - GENERATED invoices: Can be cancelled (status=CANCELLED + revert batches to READY_TO_DISPATCH for batch-based)
   * - SENT invoices: CANNOT be cancelled (goods already dispatched - use Credit Note instead)
   * - PARTIALLY_PAID/PAID invoices: CANNOT be cancelled (payment received - use Credit Note + Refund)
   * 
   * NOTE: Orders can only be cancelled before workflow starts. Since invoices are generated 
   * after dispatch batches (post-workflow), an invoice will never exist for a cancelled order.
   * Therefore, all invoice cancellations are for invoice-level errors, and batches should 
   * always be reverted for re-invoicing (for batch-based invoices).
   */
  public Invoice cancelInvoice(Long tenantId, Long invoiceId, String cancelledBy, String cancellationReason) {
    tenantService.validateTenantExists(tenantId);

    Invoice invoice = getInvoiceById(invoiceId);

    // Validate invoice can be cancelled
    if (!invoice.canBeCancelled()) {
      throw new IllegalStateException(
        "Invoice cannot be cancelled. Only GENERATED invoices with no payments can be cancelled. " +
        "Current status: " + invoice.getStatus() + ", Total paid: ₹" + invoice.getTotalPaidAmount() + ". " +
        "For SENT/PAID invoices, use Credit Note process instead."
      );
    }

    // Update invoice status to CANCELLED
    invoice.setStatus(InvoiceStatus.CANCELLED);
    invoice.setCancelledBy(cancelledBy);
    invoice.setCancellationReason(cancellationReason);
    invoice.setCancellationDate(LocalDateTime.now());

    // Soft delete invoice-dispatch batch links and revert batches (only for batch-based invoices)
    if (!invoice.isManualInvoice()) {
      // Soft delete all associated InvoiceDispatchBatch records to allow re-invoicing
      // This is critical: without this, the dispatch batches cannot be included in new invoices
      // because the validation check (findByDispatchBatchIdAndDeletedFalse) would find these old entries
      if (invoice.getInvoiceDispatchBatches() != null && !invoice.getInvoiceDispatchBatches().isEmpty()) {
        LocalDateTime deletionTime = LocalDateTime.now();
        invoice.getInvoiceDispatchBatches().forEach(invoiceDispatchBatch -> {
          invoiceDispatchBatch.setDeleted(true);
          invoiceDispatchBatch.setDeletedAt(deletionTime);
        });
        log.info("Soft deleted {} invoice-dispatch batch link(s) for cancelled invoice {}", 
                 invoice.getInvoiceDispatchBatches().size(), invoice.getInvoiceNumber());
      }

      // Revert all associated dispatch batches from DISPATCH_INVOICE_APPROVED back to READY_TO_DISPATCH
      List<Long> dispatchBatchIds = invoice.getDispatchBatchIds();
      if (!dispatchBatchIds.isEmpty()) {
        dispatchBatchService.updateMultipleBatchesFromApprovedToReadyToDispatch(dispatchBatchIds);
        log.info("Reverted {} dispatch batches from DISPATCH_INVOICE_APPROVED to READY_TO_DISPATCH status",
                 dispatchBatchIds.size());
      }

      log.info("Batch-based invoice {} cancelled by {}. Reason: {}. Status: {} → CANCELLED. " +
               "Invoice-dispatch batch links soft deleted. Associated dispatch batches reverted to READY_TO_DISPATCH for re-invoicing.",
               invoice.getInvoiceNumber(), cancelledBy, cancellationReason, invoice.getStatus());
    } else {
      log.info("Manual invoice {} cancelled by {}. Reason: {}. Status: {} → CANCELLED (No dispatch batches to revert).",
               invoice.getInvoiceNumber(), cancelledBy, cancellationReason, invoice.getStatus());
    }

    Invoice savedInvoice = invoiceRepository.save(invoice);
    return savedInvoice;
  }

  /**
   * Get pending invoices count
   */
  @Transactional(readOnly = true)
  public long getPendingInvoicesCount(Long tenantId) {
    return invoiceRepository.countByTenantIdAndStatusAndDeletedFalse(tenantId, InvoiceStatus.DRAFT);
  }

  /**
   * Get overdue invoices
   */
  @Transactional(readOnly = true)
  public List<Invoice> getOverdueInvoices(Long tenantId) {
    return invoiceRepository.findOverdueInvoices(tenantId, LocalDate.now());
  }

  /**
   * Get ready to dispatch batches for invoice generation
   */
  @Transactional(readOnly = true)
  public Page<DispatchBatch> getReadyToDispatchBatches(Long tenantId, Pageable pageable) {
    return dispatchBatchService.getReadyToDispatchBatches(tenantId, pageable);
  }

  /**
   * Get count of ready to dispatch batches
   */
  @Transactional(readOnly = true)
  public long getReadyToDispatchBatchesCount(Long tenantId) {
    return dispatchBatchService.getReadyToDispatchBatchesCount(tenantId);
  }

  /**
   * Validate dispatch batches for invoicing
   */
  private void validateDispatchBatchesForInvoicing(List<DispatchBatch> dispatchBatches, Long tenantId) {
    // Check all batches are READY_TO_DISPATCH and belong to same tenant
    Long firstBillingEntityId = null;
    WorkType firstWorkType = null;

    for (DispatchBatch batch : dispatchBatches) {
      if (batch.getTenant().getId() != tenantId) {
        log.error("Invoice batch {} is not assigned to tenant {}", batch.getId(), batch.getTenant().getId());
        throw new IllegalStateException("Dispatch batch " + batch.getDispatchBatchNumber() +
                                        " does not belong to tenant ");
      }

      // Validate batch status - must be READY_TO_DISPATCH
      // INVOICE_DRAFT_CREATED means a draft invoice already exists for this batch
      if (batch.getDispatchBatchStatus() == DispatchBatch.DispatchBatchStatus.INVOICE_DRAFT_CREATED) {
        throw new IllegalStateException("Dispatch batch " + batch.getDispatchBatchNumber() +
                                        " already has a draft invoice created. " +
                                        "Please approve or delete the existing draft invoice before creating a new one.");
      }

      if (batch.getDispatchBatchStatus() != DispatchBatch.DispatchBatchStatus.READY_TO_DISPATCH) {
        throw new IllegalStateException("Dispatch batch " + batch.getDispatchBatchNumber() +
                                        " must be in READY_TO_DISPATCH status. Current status: " +
                                        batch.getDispatchBatchStatus());
      }

      // Ensure all batches have same billing entity for multi-batch invoicing
      if (firstBillingEntityId == null) {
        firstBillingEntityId = batch.getBillingEntity().getId();
      } else if (!batch.getBillingEntity().getId().equals(firstBillingEntityId)) {
        throw new IllegalStateException("All selected dispatch batches must have the same billing entity for multi-batch invoicing.");
      }

      // Validate all batches have the same work type
      ProcessedItemDispatchBatch processedItem = batch.getProcessedItemDispatchBatch();
      if (processedItem != null && processedItem.getItemWorkflowId() != null) {
        try {
          ItemWorkflow itemWorkflow = itemWorkflowRepository.findById(processedItem.getItemWorkflowId())
              .orElse(null);
          if (itemWorkflow != null) {
            OrderItemWorkflow orderItemWorkflow = orderItemWorkflowRepository.findByItemWorkflowId(itemWorkflow.getId())
                .orElse(null);
            if (orderItemWorkflow != null) {
              WorkType currentWorkType = orderItemWorkflow.getOrderItem().getWorkType();
              
              if (firstWorkType == null) {
                firstWorkType = currentWorkType;
              } else if (firstWorkType != currentWorkType) {
                throw new IllegalArgumentException(
                  String.format("All items in an invoice must have the same work type. " +
                                "Found mixed work types: %s and %s. " +
                                "Batch '%s' has work type %s while previous batches have %s. " +
                                "Please create separate invoices for different work types (Job Work vs Material).",
                                firstWorkType, currentWorkType,
                                batch.getDispatchBatchNumber(), currentWorkType, firstWorkType)
                );
              }
            }
          }
        } catch (IllegalArgumentException e) {
          throw e; // Re-throw validation errors
        } catch (Exception e) {
          log.warn("Could not determine work type for batch {}: {}", batch.getDispatchBatchNumber(), e.getMessage());
        }
      }

      // Check if an invoice already exists for this dispatch batch (using junction table)
      List<InvoiceDispatchBatch> existingLinks = invoiceDispatchBatchRepository.findByDispatchBatchIdAndDeletedFalse(batch.getId());
      if (!existingLinks.isEmpty()) {
        throw new IllegalStateException("An invoice already exists for dispatch batch: " + batch.getDispatchBatchNumber());
      }
    }

    log.info("Validated {} dispatch batches for invoicing (Same tenant, billing entity, work type: {})", 
             dispatchBatches.size(), firstWorkType);
  }

  /**
   * Validate that all manual line items have the same work type
   */
  private void validateManualLineItemsWorkType(List<InvoiceGenerationRequest.ManualInvoiceLineItem> lineItems) {
    if (lineItems == null || lineItems.isEmpty()) {
      return;
    }

    String firstWorkType = null;
    int lineNumber = 1;

    for (InvoiceGenerationRequest.ManualInvoiceLineItem item : lineItems) {
      if (firstWorkType == null) {
        firstWorkType = item.getWorkType();
      } else if (!firstWorkType.equalsIgnoreCase(item.getWorkType())) {
        throw new IllegalArgumentException(
          String.format("All line items in an invoice must have the same work type. " +
                        "Line item %d ('%s') has work type '%s' while previous items have '%s'. " +
                        "Please create separate invoices for different work types (Job Work vs Material).",
                        lineNumber, item.getItemName(), item.getWorkType(), firstWorkType)
        );
      }
      lineNumber++;
    }

    log.info("Validated {} manual line items - all have work type: {}", lineItems.size(), firstWorkType);
  }

  /**
   * Validate that buyer entity has required fields for GST invoicing
   */
  private void validateBuyerEntityForInvoicing(BuyerEntity buyerEntity) {
    if (buyerEntity == null) {
      throw new IllegalArgumentException("Buyer entity cannot be null");
    }

    if (buyerEntity.getStateCode() == null || buyerEntity.getStateCode().trim().isEmpty()) {
      throw new IllegalArgumentException(
        String.format("Buyer entity '%s' (ID: %d) does not have a state code configured. " +
                      "State code is required for GST invoicing to determine place of supply and inter-state status. " +
                      "Please update the buyer entity with a valid state code before generating an invoice.",
                      buyerEntity.getBuyerEntityName(), buyerEntity.getId())
      );
    }

    log.debug("Validated buyer entity {} for invoicing - State Code: {}", 
             buyerEntity.getBuyerEntityName(), buyerEntity.getStateCode());
  }
}