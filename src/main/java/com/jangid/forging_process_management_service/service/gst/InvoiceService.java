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
   * - Custom pricing and GST rates
   * - Transportation details
   */
  public Invoice generateInvoice(Long tenantId, InvoiceGenerationRequest request) {
    log.info("Generating invoice for tenant: {} from {} dispatch batch(es)",
             tenantId, request.getDispatchBatchIds().size());

    tenantService.validateTenantExists(tenantId);

    if (request.getDispatchBatchIds() == null || request.getDispatchBatchIds().isEmpty()) {
      throw new IllegalArgumentException("At least one dispatch batch ID is required");
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

    // Extract order details from dispatch batch (via ProcessedItem → ItemWorkflow → OrderItemWorkflow → Order)
    Long orderId = extractOrderIdFromDispatchBatch(primaryBatch);

    // Parse customer PO date from String (format: YYYY-MM-DD) to LocalDate
    LocalDate customerPoDate = (request.getCustomerPoDate() != null && !request.getCustomerPoDate().trim().isEmpty()) ?
        LocalDate.parse(request.getCustomerPoDate()) :
        null;

    // Determine WorkType to get appropriate terms and conditions
    WorkType workType = determineWorkTypeFromDispatchBatches(dispatchBatches);
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

    // Determine WorkType from dispatch batches
    List<DispatchBatch> dispatchBatches = invoice.getDispatchBatches();
    WorkType workType = determineWorkTypeFromDispatchBatches(dispatchBatches);

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

    log.info("Generated final invoice number: {} (WorkType: {}, Sequence: {})",
             invoiceNumber, workType, currentSequence);

    return invoiceNumber;
  }

  /**
   * Determine the WorkType from dispatch batches by checking the associated OrderItem
   */
  private WorkType determineWorkTypeFromDispatchBatches(List<DispatchBatch> dispatchBatches) {

    // Get the first batch's work type
    DispatchBatch firstBatch = dispatchBatches.get(0);
    ProcessedItemDispatchBatch processedItem = firstBatch.getProcessedItemDispatchBatch();

    if (processedItem == null || processedItem.getItemWorkflowId() == null) {
      log.warn("No processed item or itemWorkflowId found, defaulting to WITH_MATERIAL");
      return WorkType.WITH_MATERIAL;
    }

    // Get ItemWorkflow from itemWorkflowId
    ItemWorkflow itemWorkflow = itemWorkflowRepository.findById(processedItem.getItemWorkflowId())
        .orElseThrow(() -> new RuntimeException("ItemWorkflow not found for ID: " + processedItem.getItemWorkflowId()));

    // Get OrderItemWorkflow from ItemWorkflow
    OrderItemWorkflow orderItemWorkflow = orderItemWorkflowRepository.findByItemWorkflowId(itemWorkflow.getId())
        .orElseThrow(() -> new RuntimeException("OrderItemWorkflow not found for ItemWorkflow ID: " + itemWorkflow.getId()));

    // Get WorkType from OrderItem
    WorkType workType = orderItemWorkflow.getOrderItem().getWorkType();

    log.info("Determined WorkType as {} from dispatch batches", workType);
     return workType;
   }

  /**
   * Extract Order ID from DispatchBatch by traversing entity relationships
   */
  private Long extractOrderIdFromDispatchBatch(DispatchBatch dispatchBatch) {
    if (dispatchBatch == null) {
      log.warn("DispatchBatch is null, cannot extract orderId");
      return null;
    }

    ProcessedItemDispatchBatch processedItem = dispatchBatch.getProcessedItemDispatchBatch();
    if (processedItem == null || processedItem.getItemWorkflowId() == null) {
      log.warn("No processed item or itemWorkflowId found in dispatch batch {}", 
               dispatchBatch.getDispatchBatchNumber());
      return null;
    }

    try {
      // Get ItemWorkflow from itemWorkflowId
      ItemWorkflow itemWorkflow = itemWorkflowRepository.findById(processedItem.getItemWorkflowId())
          .orElseThrow(() -> new RuntimeException("ItemWorkflow not found for ID: " + processedItem.getItemWorkflowId()));

      // Get OrderItemWorkflow from ItemWorkflow
      OrderItemWorkflow orderItemWorkflow = orderItemWorkflowRepository.findByItemWorkflowId(itemWorkflow.getId())
          .orElseThrow(() -> new RuntimeException("OrderItemWorkflow not found for ItemWorkflow ID: " + itemWorkflow.getId()));

      // Get Order ID from OrderItem
      Long orderId = orderItemWorkflow.getOrderItem().getOrder().getId();
      
      log.debug("Extracted orderId {} from dispatch batch {}", orderId, dispatchBatch.getDispatchBatchNumber());
      return orderId;

    } catch (Exception e) {
      log.error("Error extracting orderId from dispatch batch {}: {}", 
               dispatchBatch.getDispatchBatchNumber(), e.getMessage(), e);
      return null;
    }
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

    // Update all associated dispatch batches from INVOICE_DRAFT_CREATED to DISPATCH_INVOICE_APPROVED
    List<Long> dispatchBatchIds = invoice.getDispatchBatchIds();
    if (!dispatchBatchIds.isEmpty()) {
      dispatchBatchService.updateMultipleBatchesFromDraftToApproved(dispatchBatchIds);
      log.info("Updated {} dispatch batches from INVOICE_DRAFT_CREATED to DISPATCH_INVOICE_APPROVED status", 
               dispatchBatchIds.size());
    }

    log.info("Invoice approved by {}. Temporary number {} replaced with final invoice number: {}. " +
             "Status: DRAFT → GENERATED. Associated dispatch batches: INVOICE_DRAFT_CREATED → DISPATCH_INVOICE_APPROVED",
             approvedBy, oldInvoiceNumber, finalInvoiceNumber);

    return savedInvoice;
  }

  /**
   * Delete invoice - only DRAFT invoices can be deleted
   * Reverts associated dispatch batches back to READY_TO_DISPATCH status
   */
  public void deleteInvoice(Long tenantId, Long invoiceId) {
    tenantService.validateTenantExists(tenantId);

    Invoice invoice = getInvoiceById(invoiceId);

    // Only DRAFT invoices can be deleted
    if (invoice.getStatus() != InvoiceStatus.DRAFT) {
      throw new IllegalStateException("Only DRAFT invoices can be deleted. Current status: " + invoice.getStatus() + 
                                      ". Approved invoices cannot be deleted.");
    }

    // Revert all associated dispatch batches from INVOICE_DRAFT_CREATED back to READY_TO_DISPATCH
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

    // Delete all associated InvoiceLineItem records
    // InvoiceLineItem doesn't have soft delete fields, so we use orphanRemoval mechanism
    if (invoice.getLineItems() != null && !invoice.getLineItems().isEmpty()) {
      int lineItemCount = invoice.getLineItems().size();
      invoice.getLineItems().clear(); // This triggers orphanRemoval = true, deleting all line items
      log.info("Deleted {} line item(s) for invoice {} via orphan removal", lineItemCount, invoice.getInvoiceNumber());
    }

    // Soft delete all associated InvoiceDispatchBatch records
    if (invoice.getInvoiceDispatchBatches() != null && !invoice.getInvoiceDispatchBatches().isEmpty()) {
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

    log.info("DRAFT invoice {} and all associated records deleted successfully. " +
             "Associated dispatch batches reverted from INVOICE_DRAFT_CREATED to READY_TO_DISPATCH.",
             invoice.getInvoiceNumber());
  }

  /**
   * Cancel invoice - only GENERATED invoices with no payments can be cancelled
   * Sets invoice status to CANCELLED and reverts associated dispatch batches back to READY_TO_DISPATCH status
   * 
   * BUSINESS RULE: 
   * - DRAFT invoices: Can be deleted (soft-delete + rollback)
   * - GENERATED invoices: Can be cancelled (status=CANCELLED + revert batches to READY_TO_DISPATCH)
   * - SENT invoices: CANNOT be cancelled (goods already dispatched - use Credit Note instead)
   * - PARTIALLY_PAID/PAID invoices: CANNOT be cancelled (payment received - use Credit Note + Refund)
   * 
   * NOTE: Orders can only be cancelled before workflow starts. Since invoices are generated 
   * after dispatch batches (post-workflow), an invoice will never exist for a cancelled order.
   * Therefore, all invoice cancellations are for invoice-level errors, and batches should 
   * always be reverted for re-invoicing.
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

    Invoice savedInvoice = invoiceRepository.save(invoice);

    // Revert all associated dispatch batches from DISPATCH_INVOICE_APPROVED back to READY_TO_DISPATCH
    List<Long> dispatchBatchIds = invoice.getDispatchBatchIds();
    if (!dispatchBatchIds.isEmpty()) {
      dispatchBatchService.updateMultipleBatchesFromApprovedToReadyToDispatch(dispatchBatchIds);
      log.info("Reverted {} dispatch batches from DISPATCH_INVOICE_APPROVED to READY_TO_DISPATCH status",
               dispatchBatchIds.size());
    }

    log.info("Invoice {} cancelled by {}. Reason: {}. Status: {} → CANCELLED. " +
             "Invoice-dispatch batch links soft deleted. Associated dispatch batches reverted to READY_TO_DISPATCH for re-invoicing.",
             invoice.getInvoiceNumber(), cancelledBy, cancellationReason, invoice.getStatus());

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

      // Check if an invoice already exists for this dispatch batch (using junction table)
      List<InvoiceDispatchBatch> existingLinks = invoiceDispatchBatchRepository.findByDispatchBatchIdAndDeletedFalse(batch.getId());
      if (!existingLinks.isEmpty()) {
        throw new IllegalStateException("An invoice already exists for dispatch batch: " + batch.getDispatchBatchNumber());
      }
    }
  }
}