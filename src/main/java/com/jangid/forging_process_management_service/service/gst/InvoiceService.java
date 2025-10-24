package com.jangid.forging_process_management_service.service.gst;

import com.jangid.forging_process_management_service.entities.gst.Invoice;
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
import com.jangid.forging_process_management_service.repositories.gst.GSTConfigurationRepository;
import com.jangid.forging_process_management_service.repositories.gst.InvoiceRepository;
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
  private final GSTConfigurationRepository gstConfigurationRepository;
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

    // Update all dispatch batch statuses to DISPATCH_APPROVED
    dispatchBatchService.updateMultipleBatchesToDispatchApproved(request.getDispatchBatchIds());

    log.info("Successfully generated invoice: {} for {} dispatch batch(es)",
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

    // Generate or use custom invoice number
    String invoiceNumber = request.getInvoiceNumber() != null ?
        request.getInvoiceNumber() :
        generateInvoiceNumber(tenantId, dispatchBatches);

    // Use custom dates or defaults
    // Parse invoice date from String (format: YYYY-MM-DDTHH:mm) to LocalDateTime
    LocalDateTime invoiceDate = ConvertorUtils.convertStringToLocalDateTime(request.getInvoiceDate());

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

    // Build invoice
    Invoice.InvoiceBuilder invoiceBuilder = Invoice.builder()
        .dispatchBatch(primaryBatch) // Link to primary batch
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
        .placeOfSupply(GSTUtils.getPlaceOfSupply(recipientBuyerEntity, recipientVendorEntity))
        .paymentTerms(request.getPaymentTerms());

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

    return invoice;
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
   * Generate unique invoice number based on WorkType of the order items
   */
  private String generateInvoiceNumber(Long tenantId, List<DispatchBatch> dispatchBatches) {
    TenantInvoiceSettings settings = tenantSettingsService.getInvoiceSettings(tenantId);

    // Determine WorkType from the first dispatch batch
    // All batches should have the same WorkType (validated in frontend)
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

    // Update sequence
    tenantSettingsService.incrementInvoiceSequence(tenantId, sequenceType);

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
   * Approve invoice (move from DRAFT to GENERATED)
   */
  public Invoice approveInvoice(Long tenantId, Long invoiceId, String approvedBy) {
    tenantService.validateTenantExists(tenantId);

    Invoice invoice = getInvoiceById(invoiceId);

    if (invoice.getStatus() != InvoiceStatus.DRAFT) {
      throw new IllegalStateException("Invoice cannot be approved in current status: " + invoice.getStatus());
    }

    invoice.setStatus(InvoiceStatus.GENERATED);
    Invoice savedInvoice = invoiceRepository.save(invoice);

    log.info("Invoice {} approved by {}", invoice.getInvoiceNumber(), approvedBy);

    return savedInvoice;
  }

  /**
   * Delete invoice (only if no dispatch batch is DISPATCHED)
   */
  public void deleteInvoice(Long tenantId, Long invoiceId) {
    tenantService.validateTenantExists(tenantId);

    Invoice invoice = getInvoiceById(invoiceId);

    // Check if any associated dispatch batch is DISPATCHED
    if (invoice.getDispatchBatch() != null) {
      DispatchBatch dispatchBatch = invoice.getDispatchBatch();
      if (dispatchBatch.getDispatchBatchStatus() == DispatchBatch.DispatchBatchStatus.DISPATCHED) {
        throw new IllegalStateException("Cannot delete invoice. Associated dispatch batch is already dispatched.");
      }

      // Revert dispatch batch status back to READY_TO_DISPATCH
      dispatchBatchService.revertStatusToReadyToDispatch(dispatchBatch.getId());
    }

    // Soft delete invoice
    invoice.setDeleted(true);
    invoice.setDeletedAt(LocalDateTime.now());
    invoiceRepository.save(invoice);

    log.info("Invoice {} deleted successfully", invoice.getInvoiceNumber());
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

      // Check if an invoice already exists for this dispatch batch
      Optional<Invoice> existingInvoice = invoiceRepository.findByDispatchBatchIdAndDeletedFalse(batch.getId());
      if (existingInvoice.isPresent()) {
        throw new IllegalStateException("An invoice already exists for dispatch batch: " + batch.getDispatchBatchNumber());
      }
    }
  }
}