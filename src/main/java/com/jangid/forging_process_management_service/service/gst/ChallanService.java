package com.jangid.forging_process_management_service.service.gst;

import com.jangid.forging_process_management_service.dto.gst.ChallanGenerationRequest;
import com.jangid.forging_process_management_service.entities.Tenant;
import com.jangid.forging_process_management_service.entities.buyer.BuyerEntity;
import com.jangid.forging_process_management_service.entities.dispatch.DispatchBatch;
import com.jangid.forging_process_management_service.entities.dispatch.ProcessedItemDispatchBatch;
import com.jangid.forging_process_management_service.entities.gst.ChallanDispatchBatch;
import com.jangid.forging_process_management_service.entities.gst.ChallanLineItem;
import com.jangid.forging_process_management_service.entities.gst.ChallanStatus;
import com.jangid.forging_process_management_service.entities.gst.ChallanType;
import com.jangid.forging_process_management_service.entities.gst.DeliveryChallan;
import com.jangid.forging_process_management_service.entities.gst.TransportationMode;
import com.jangid.forging_process_management_service.entities.order.OrderItemWorkflow;
import com.jangid.forging_process_management_service.entities.order.WorkType;
import com.jangid.forging_process_management_service.entities.product.UnitOfMeasurement;
import com.jangid.forging_process_management_service.entities.settings.TenantChallanSettings;
import com.jangid.forging_process_management_service.entities.settings.TenantInvoiceSettings;
import com.jangid.forging_process_management_service.entities.vendor.VendorEntity;
import com.jangid.forging_process_management_service.entities.workflow.ItemWorkflow;
import com.jangid.forging_process_management_service.repositories.buyer.BuyerEntityRepository;
import com.jangid.forging_process_management_service.repositories.dispatch.DispatchBatchRepository;
import com.jangid.forging_process_management_service.repositories.gst.DeliveryChallanRepository;
import com.jangid.forging_process_management_service.repositories.order.OrderItemWorkflowRepository;
import com.jangid.forging_process_management_service.repositories.settings.TenantChallanSettingsRepository;
import com.jangid.forging_process_management_service.repositories.settings.TenantInvoiceSettingsRepository;
import com.jangid.forging_process_management_service.repositories.vendor.VendorEntityRepository;
import com.jangid.forging_process_management_service.repositories.workflow.ItemWorkflowRepository;
import com.jangid.forging_process_management_service.service.TenantService;
import com.jangid.forging_process_management_service.service.dispatch.DispatchBatchService;
import com.jangid.forging_process_management_service.service.order.OrderService;
import com.jangid.forging_process_management_service.utils.ConvertorUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChallanService {

  private final DeliveryChallanRepository challanRepository;
  private final DispatchBatchService dispatchBatchService;
  private final DispatchBatchRepository dispatchBatchRepository;
  private final BuyerEntityRepository buyerEntityRepository;
  private final VendorEntityRepository vendorEntityRepository;
  private final TenantChallanSettingsRepository challanSettingsRepository;
  private final TenantService tenantService;
  private final OrderService orderService;
  private final ItemWorkflowRepository itemWorkflowRepository;
  private final TenantInvoiceSettingsRepository tenantInvoiceSettingsRepository;
  private final OrderItemWorkflowRepository orderItemWorkflowRepository;

  /**
   * Generate challan from dispatch batches
   */
  @Transactional
  public DeliveryChallan generateChallan(Long tenantId, ChallanGenerationRequest request) {
    log.info("Generating challan for tenant: {} with {} dispatch batch(es)", 
             tenantId, request.getDispatchBatchIds().size());

    // Validate request
    validateChallanRequest(request);

    // Get tenant
    Tenant tenant = tenantService.getTenantById(tenantId);

    // Create challan with GENERATED status (no draft/approval needed)
    DeliveryChallan challan = DeliveryChallan.builder()
        .tenant(tenant)
        .challanType(ChallanType.valueOf(request.getChallanType()))
        .otherChallanTypeDetails(request.getOtherChallanTypeDetails())
        .transportationReason(request.getTransportationReason())
        .status(ChallanStatus.GENERATED)
        .build();

    // Set challan number (auto-generate if not provided)
    String challanNumber;
    if (request.getChallanNumber() != null && !request.getChallanNumber().isBlank()) {
      challanNumber = request.getChallanNumber();
    } else {
      challanNumber = generateChallanNumber(tenantId);
    }
    
    // Validate challan number uniqueness
    if (challanRepository.existsByChallanNumberAndTenantIdAndDeletedFalse(challanNumber, tenantId)) {
      throw new IllegalArgumentException(
        String.format("Challan number '%s' already exists for this tenant. Please use a different challan number.", 
                      challanNumber));
    }
    
    challan.setChallanNumber(challanNumber);

    // Set challan date and time
    if (request.getChallanDateTime() != null && !request.getChallanDateTime().isBlank()) {
      challan.setChallanDateTime(ConvertorUtils.convertStringToLocalDateTime(request.getChallanDateTime()));
    }

    // Set consignee details
    setConsigneeDetails(challan, request);

    // Set transportation details
    setTransportationDetails(challan, request);

    // Add dispatch batches
    List<DispatchBatch> dispatchBatches = dispatchBatchRepository.findAllById(request.getDispatchBatchIds());
    if (dispatchBatches.size() != request.getDispatchBatchIds().size()) {
      throw new IllegalArgumentException("One or more dispatch batches not found");
    }

    // Validate all batches belong to same order and tenant
    validateDispatchBatches(dispatchBatches, tenantId);

    // Set order reference from first batch (get orderId from itemWorkflowId)
    Long orderId = null;
    if (dispatchBatches.get(0).getProcessedItemDispatchBatch() != null) {
      Long itemWorkflowId = dispatchBatches.get(0).getProcessedItemDispatchBatch().getItemWorkflowId();
      if (itemWorkflowId != null) {
        try {
          orderId = orderService.getOrderIdByItemWorkflowId(itemWorkflowId);
          log.debug("Resolved orderId: {} from itemWorkflowId: {}", orderId, itemWorkflowId);
        } catch (Exception e) {
          log.warn("Could not resolve orderId from itemWorkflowId: {}, challan will be created without order reference", 
                   itemWorkflowId, e);
        }
      }
    }
    challan.setOrderId(orderId);

    // Save challan first to get ID
    challan = challanRepository.save(challan);

    // Add dispatch batch associations
    for (DispatchBatch batch : dispatchBatches) {
      ChallanDispatchBatch cdb = ChallanDispatchBatch.builder()
          .deliveryChallan(challan)
          .dispatchBatch(batch)
          .tenant(tenant)
          .build();
      challan.addChallanDispatchBatch(cdb);
    }

    // Determine and set WorkType for the challan from first dispatch batch
    // This helps in challan categorization and reporting
    if (!dispatchBatches.isEmpty()) {
      WorkType challanWorkType = determineWorkTypeFromBatch(dispatchBatches.get(0));
      challan.setWorkType(challanWorkType);
      log.debug("Set challan WorkType to: {}", challanWorkType);
    }

    // Add line items (from manual input or derive from dispatch batches)
    if (request.getManualLineItems() != null && !request.getManualLineItems().isEmpty()) {
      addManualLineItems(challan, request.getManualLineItems());
    } else {
      deriveLineItemsFromDispatchBatches(challan, dispatchBatches, request.getItemOverrides());
    }

    // Calculate totals
    challan.calculateTotals();
    
    // Persist consignor details, bank details, terms & conditions, and amount in words
    persistChallanDisplayFields(challan, tenant);

    // Save challan
    challan = challanRepository.save(challan);

    // Update dispatch batch status to CHALLAN_CREATED
    for (DispatchBatch batch : dispatchBatches) {
      batch.setDispatchBatchStatus(DispatchBatch.DispatchBatchStatus.CHALLAN_CREATED);
      dispatchBatchRepository.save(batch);
      log.debug("Updated dispatch batch {} status to CHALLAN_CREATED", batch.getDispatchBatchNumber());
    }

    log.info("Successfully generated challan {} with GENERATED status for {} dispatch batch(es)", 
             challan.getChallanNumber(), dispatchBatches.size());

    return challan;
  }

  /**
   * Get challan by ID
   */
  @Transactional(readOnly = true)
  public DeliveryChallan getChallanById(Long id) {
    return challanRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Challan not found with id: " + id));
  }

  /**
   * Get challan by dispatch batch ID
   */
  @Transactional(readOnly = true)
  public DeliveryChallan getChallanByDispatchBatchId(Long dispatchBatchId) {
    return challanRepository.findByDispatchBatchId(dispatchBatchId)
        .orElseThrow(() -> new IllegalArgumentException(
            "Challan not found for dispatch batch id: " + dispatchBatchId));
  }

  /**
   * Get all challans for a tenant with pagination
   */
  @Transactional(readOnly = true)
  public Page<DeliveryChallan> getChallansByTenant(Long tenantId, Pageable pageable) {
    return challanRepository.findByTenantIdAndDeletedFalse(tenantId, pageable);
  }

  /**
   * Search challans with filters
   */
  @Transactional(readOnly = true)
  public Page<DeliveryChallan> searchChallans(Long tenantId, ChallanStatus status, 
                                               LocalDateTime fromDate, LocalDateTime toDate,
                                               String search, Pageable pageable) {
    // Implement search logic based on filters
    if (status != null) {
      return challanRepository.findByTenantIdAndStatusAndDeletedFalse(tenantId, status, pageable);
    }
    return getChallansByTenant(tenantId, pageable);
  }

  /**
   * Delete challan (only GENERATED status - before dispatch)
   * When deleted, associated dispatch batches are moved back to READY_TO_DISPATCH
   */
  @Transactional
  public void deleteChallan(Long tenantId, Long challanId) {
    DeliveryChallan challan = getChallanById(challanId);
    
    if (challan.getTenant().getId() != tenantId) {
      throw new IllegalArgumentException("Challan does not belong to tenant");
    }

    if (!challan.canBeDeleted()) {
      throw new IllegalStateException(
        String.format("Only GENERATED challans can be deleted. Current status: %s. " +
                      "DISPATCHED or DELIVERED challans cannot be deleted.", 
                      challan.getStatus()));
    }

    // Get associated dispatch batches before deletion
    List<DispatchBatch> dispatchBatches = challan.getDispatchBatches();

    // Mark challan as deleted
    challan.setDeleted(true);
    challan.setDeletedAt(LocalDateTime.now());
    challanRepository.save(challan);

    // Move dispatch batches back to READY_TO_DISPATCH
    for (DispatchBatch batch : dispatchBatches) {
      batch.setDispatchBatchStatus(DispatchBatch.DispatchBatchStatus.READY_TO_DISPATCH);
      dispatchBatchRepository.save(batch);
      log.debug("Moved dispatch batch {} back to READY_TO_DISPATCH after challan deletion", 
                batch.getDispatchBatchNumber());
    }
    
    // Note: We DON'T decrement the sequence number here.
    // Instead, the generateChallanNumber method will reuse deleted challan numbers
    // by checking for deleted=true challans with the oldest deletedAt timestamp.
    // This approach is safer and prevents sequence number inconsistencies.
    
    log.info("Deleted challan: {} (number: {}) for tenant: {}. {} dispatch batch(es) moved back to READY_TO_DISPATCH. " +
             "Challan number will be available for reuse.", 
             challanId, challan.getChallanNumber(), tenantId, dispatchBatches.size());
  }

  /**
   * Mark challan as dispatched
   * Note: This only changes the status. The challanDateTime remains unchanged as it represents
   * the official challan issue date, not the physical dispatch date.
   */
  @Transactional
  public DeliveryChallan markAsDispatched(Long tenantId, Long challanId) {
    DeliveryChallan challan = getChallanById(challanId);
    
    if (challan.getTenant().getId() != tenantId) {
      throw new IllegalArgumentException("Challan does not belong to tenant");
    }

    if (challan.getStatus() != ChallanStatus.GENERATED) {
      throw new IllegalStateException("Only GENERATED challans can be dispatched");
    }

    challan.setStatus(ChallanStatus.DISPATCHED);

    return challanRepository.save(challan);
  }

  /**
   * Mark challan as delivered
   */
  @Transactional
  public DeliveryChallan markAsDelivered(Long tenantId, Long challanId, LocalDate deliveredDate) {
    DeliveryChallan challan = getChallanById(challanId);
    
    if (challan.getTenant().getId() != tenantId) {
      throw new IllegalArgumentException("Challan does not belong to tenant");
    }

    if (challan.getStatus() != ChallanStatus.DISPATCHED) {
      throw new IllegalStateException("Only DISPATCHED challans can be marked as delivered");
    }

    challan.setStatus(ChallanStatus.DELIVERED);
    challan.setActualDeliveryDate(deliveredDate);

    return challanRepository.save(challan);
  }

  /**
   * Cancel challan
   */
  @Transactional
  public DeliveryChallan cancelChallan(Long tenantId, Long challanId, String cancelledBy, String reason) {
    DeliveryChallan challan = getChallanById(challanId);
    
    if (challan.getTenant().getId() != tenantId) {
      throw new IllegalArgumentException("Challan does not belong to tenant");
    }

    if (!challan.canBeCancelled()) {
      throw new IllegalStateException("Only DRAFT or GENERATED challans can be cancelled");
    }

    challan.setStatus(ChallanStatus.CANCELLED);
    // Store cancellation info in remarks or add fields to entity if needed

    return challanRepository.save(challan);
  }

  /**
   * Generate challan number from tenant settings
   * Reuses deleted challan numbers before generating new ones
   */
  private String generateChallanNumber(Long tenantId) {
    TenantChallanSettings settings = challanSettingsRepository
        .findByTenantIdAndIsActiveTrueAndDeletedFalse(tenantId)
        .orElseThrow(() -> new IllegalStateException("Challan settings not found for tenant"));

    // Check for deleted challan numbers that can be reused
    List<DeliveryChallan> deletedChallans = challanRepository.findDeletedChallansByTenantOrderByDeletedAtAsc(tenantId);
    
    if (!deletedChallans.isEmpty()) {
      // Reuse the oldest deleted challan number
      String reusedChallanNumber = deletedChallans.get(0).getChallanNumber();
      log.info("Reusing deleted challan number: {} for tenant: {}", reusedChallanNumber, tenantId);
      return reusedChallanNumber;
    }

    // No deleted challans to reuse - generate new challan number
    // Format: PREFIX/SERIES/SEQUENCE (Example: JSTCHN/25-26/0003)
    String challanNumber = settings.getNextChallanNumber();

    // Increment sequence for next challan
    settings.incrementSequence();
    challanSettingsRepository.save(settings);

    log.info("Generated new challan number: {} for tenant: {} using prefix: {}", 
             challanNumber, tenantId, settings.getChallanPrefix());
    return challanNumber;
  }

  /**
   * Get dispatch batches ready for challan generation
   */
  @Transactional(readOnly = true)
  public Page<DispatchBatch> getReadyToChallanBatches(Long tenantId, Pageable pageable) {
    // Get batches that are READY_TO_DISPATCH and don't have challan yet
    return dispatchBatchRepository.findDispatchBatchesByDispatchBatchStatus(
        tenantId, 
        DispatchBatch.DispatchBatchStatus.READY_TO_DISPATCH, 
        pageable
    );
  }

  /**
   * Get count of ready to dispatch batches
   */
  @Transactional(readOnly = true)
  public long getReadyToDispatchBatchesCount(Long tenantId) {
    return dispatchBatchService.getReadyToDispatchBatchesCount(tenantId);
  }

  /**
   * Get challan dashboard statistics
   * Note: DRAFT status removed from workflow - challans are created as GENERATED
   */
  @Transactional(readOnly = true)
  public Map<String, Object> getChallanDashboardStats(Long tenantId) {
    Map<String, Object> stats = new HashMap<>();
    
    // Count challans by status (DRAFT status no longer used in workflow)
    long generatedCount = challanRepository.countByTenantIdAndStatus(tenantId, ChallanStatus.GENERATED);
    long dispatchedCount = challanRepository.countByTenantIdAndStatus(tenantId, ChallanStatus.DISPATCHED);
    long deliveredCount = challanRepository.countByTenantIdAndStatus(tenantId, ChallanStatus.DELIVERED);
    long cancelledCount = challanRepository.countByTenantIdAndStatus(tenantId, ChallanStatus.CANCELLED);
    
    // Count ready to challan batches
    long readyToChallanCount = getReadyToDispatchBatchesCount(tenantId);
    
    stats.put("generatedCount", generatedCount);
    stats.put("inTransitCount", dispatchedCount); // Dispatched = In Transit
    stats.put("deliveredCount", deliveredCount);
    stats.put("cancelledCount", cancelledCount);
    stats.put("readyToChallanCount", readyToChallanCount);
    stats.put("totalActiveChallans", generatedCount + dispatchedCount + deliveredCount);
    
    return stats;
  }

  /**
   * Update an existing challan (only GENERATED status - before dispatch)
   */
  @Transactional
  public DeliveryChallan updateChallan(Long tenantId, Long challanId, ChallanGenerationRequest request) {
    DeliveryChallan challan = getChallanById(challanId);
    
    // Validate tenant ownership
    if (challan.getTenant().getId() != tenantId) {
      throw new IllegalArgumentException("Challan does not belong to tenant");
    }
    
    // Only GENERATED challans can be updated (before dispatch)
    if (!challan.canBeUpdated()) {
      throw new IllegalStateException(
        String.format("Only GENERATED challans can be updated. Current status: %s. " +
                      "DISPATCHED or DELIVERED challans cannot be modified.", 
                      challan.getStatus()));
    }
    
    // Update basic details
    challan.setChallanType(ChallanType.valueOf(request.getChallanType()));
    challan.setOtherChallanTypeDetails(request.getOtherChallanTypeDetails());
    challan.setChallanDateTime(ConvertorUtils.convertStringToLocalDateTime(request.getChallanDateTime()));
    challan.setExpectedDeliveryDate(ConvertorUtils.convertStringToLocalDate(request.getExpectedDeliveryDate()));
    challan.setTransportationReason(request.getTransportationReason());
    challan.setTransportationMode(TransportationMode.valueOf(request.getTransportationMode()));
    
    // Update transportation details
    setTransportationDetails(challan, request);
    
    // Update consignee details
    setConsigneeDetails(challan, request);
    
    // Recalculate totals
    challan.calculateTotals();
    
    log.info("Updated challan: {} for tenant: {}", challanId, tenantId);
    
    return challanRepository.save(challan);
  }

  // ========== Helper Methods ==========

  private void validateChallanRequest(ChallanGenerationRequest request) {
    if (request.getDispatchBatchIds() == null || request.getDispatchBatchIds().isEmpty()) {
      throw new IllegalArgumentException("At least one dispatch batch is required");
    }

    if (!request.hasValidConsignee()) {
      throw new IllegalArgumentException("Consignee (buyer or vendor entity) is required");
    }
  }

  private void validateDispatchBatches(List<DispatchBatch> batches, Long tenantId) {
    Long orderId = null;
    
    for (DispatchBatch batch : batches) {
      if (batch.getTenant().getId() != tenantId) {
        throw new IllegalArgumentException("Dispatch batch does not belong to tenant: " + batch.getId());
      }

      // All batches should be from same order (optional check)
      Long batchOrderId = batch.getProcessedItemDispatchBatch() != null
          ? batch.getProcessedItemDispatchBatch().getItemWorkflowId()
          : null;
      
      if (orderId == null) {
        orderId = batchOrderId;
      } else if (batchOrderId != null && !orderId.equals(batchOrderId)) {
        log.warn("Dispatch batches from different orders in same challan: {} and {}", orderId, batchOrderId);
      }
    }
  }

  private void setConsigneeDetails(DeliveryChallan challan, ChallanGenerationRequest request) {
    if (request.getConsigneeBuyerEntityId() != null) {
      BuyerEntity buyerEntity = buyerEntityRepository.findById(request.getConsigneeBuyerEntityId())
          .orElseThrow(() -> new IllegalArgumentException("Buyer entity not found"));
      challan.setConsigneeBuyerEntity(buyerEntity);
    } else if (request.getConsigneeVendorEntityId() != null) {
      VendorEntity vendorEntity = vendorEntityRepository.findById(request.getConsigneeVendorEntityId())
          .orElseThrow(() -> new IllegalArgumentException("Vendor entity not found"));
      challan.setConsigneeVendorEntity(vendorEntity);
    }
  }

  private void setTransportationDetails(DeliveryChallan challan, ChallanGenerationRequest request) {
    if (request.getTransportationMode() != null) {
      challan.setTransportationMode(TransportationMode.valueOf(request.getTransportationMode()));
    }
    
    if (request.getExpectedDeliveryDate() != null) {
      challan.setExpectedDeliveryDate(ConvertorUtils.convertStringToLocalDate(request.getExpectedDeliveryDate()));
    }
    
    challan.setTransporterName(request.getTransporterName());
    challan.setTransporterId(request.getTransporterId());
    challan.setVehicleNumber(request.getVehicleNumber());
    challan.setTransportationDistance(request.getTransportationDistance());
  }

  /**
   * Determine WorkType from a dispatch batch
   * Follows same strategy as InvoiceService.determineWorkTypeFromDispatchBatches
   * Returns JOB_WORK or WITH_MATERIAL based on the order item workflow
   */
  private WorkType determineWorkTypeFromBatch(DispatchBatch batch) {
    ProcessedItemDispatchBatch processedItem = batch.getProcessedItemDispatchBatch();
    
    if (processedItem == null || processedItem.getItemWorkflowId() == null) {
      log.warn("No processed item or itemWorkflowId found for batch {}, defaulting to WITH_MATERIAL", 
               batch.getDispatchBatchNumber());
      return WorkType.WITH_MATERIAL;
    }
    
    // Get ItemWorkflow from itemWorkflowId
    Optional<ItemWorkflow> itemWorkflowOpt = itemWorkflowRepository.findById(processedItem.getItemWorkflowId());
    if (!itemWorkflowOpt.isPresent()) {
      log.warn("ItemWorkflow not found for ID: {}, defaulting to WITH_MATERIAL", processedItem.getItemWorkflowId());
      return WorkType.WITH_MATERIAL;
    }
    
    ItemWorkflow itemWorkflow = itemWorkflowOpt.get();
    
    // Get OrderItemWorkflow from ItemWorkflow
    Optional<OrderItemWorkflow> orderItemWorkflowOpt =
        orderItemWorkflowRepository.findByItemWorkflowId(itemWorkflow.getId());
    if (!orderItemWorkflowOpt.isPresent()) {
      log.info("OrderItemWorkflow not found for ItemWorkflow ID: {} - this is a non-order batch, defaulting to WITH_MATERIAL", 
               itemWorkflow.getId());
      return WorkType.WITH_MATERIAL;
    }
    
    // Get WorkType from OrderItem
    WorkType workType =
        orderItemWorkflowOpt.get().getOrderItem().getWorkType();
    
    log.debug("Determined WorkType as {} for batch {}", workType, batch.getDispatchBatchNumber());
    return workType;
  }

  private void addManualLineItems(DeliveryChallan challan, 
                                  List<ChallanGenerationRequest.ChallanLineItemRequest> lineItemRequests) {
    log.debug("Adding {} manual line items", lineItemRequests.size());
    
    // Get tenant invoice settings (use invoice settings instead of challan settings)
    TenantInvoiceSettings invoiceSettings =
        tenantInvoiceSettingsRepository
            .findByTenantIdAndIsActiveTrueAndDeletedFalse(challan.getTenant().getId())
            .orElse(null);
    
    if (invoiceSettings == null) {
      throw new IllegalStateException("Invoice settings not found for tenant: " + challan.getTenant().getId());
    }
    
    int lineNumber = 1;
    for (ChallanGenerationRequest.ChallanLineItemRequest itemReq : lineItemRequests) {
      // Determine WorkType from request (default to WITH_MATERIAL if not provided)
      WorkType workType =
          WorkType.WITH_MATERIAL;
      
      if (itemReq.getWorkType() != null && !itemReq.getWorkType().isBlank()) {
        try {
          workType = WorkType.valueOf(
              itemReq.getWorkType().toUpperCase());
        } catch (IllegalArgumentException e) {
          log.warn("Invalid work type '{}' in manual line item, defaulting to WITH_MATERIAL", itemReq.getWorkType());
        }
      }
      
      // Get HSN code and default tax rates based on WorkType
      String defaultHsnCode;
      BigDecimal defaultCgstRate;
      BigDecimal defaultSgstRate;
      BigDecimal defaultIgstRate;
      
      if (workType == WorkType.JOB_WORK_ONLY) {
        // Use Job Work settings
        defaultHsnCode = invoiceSettings.getJobWorkHsnSacCode();
        defaultCgstRate = invoiceSettings.getJobWorkCgstRate();
        defaultSgstRate = invoiceSettings.getJobWorkSgstRate();
        defaultIgstRate = invoiceSettings.getJobWorkIgstRate();
      } else {
        // Use Material (WITH_MATERIAL) settings
        defaultHsnCode = invoiceSettings.getMaterialHsnSacCode();
        defaultCgstRate = invoiceSettings.getMaterialCgstRate();
        defaultSgstRate = invoiceSettings.getMaterialSgstRate();
        defaultIgstRate = invoiceSettings.getMaterialIgstRate();
      }
      
      // Use HSN code from request if provided, otherwise use default based on WorkType
      String hsnCode = (itemReq.getHsnCode() != null && !itemReq.getHsnCode().isBlank()) 
          ? itemReq.getHsnCode() 
          : defaultHsnCode;
      
      // Use unit of measurement from request if provided, otherwise default to PIECES
      String unitOfMeasurement = (itemReq.getUnitOfMeasurement() != null && !itemReq.getUnitOfMeasurement().isBlank())
          ? itemReq.getUnitOfMeasurement()
          : UnitOfMeasurement.PIECES.name();
      
      ChallanLineItem lineItem = ChallanLineItem.builder()
          .lineNumber(lineNumber++)
          .itemName(itemReq.getItemName())
          .hsnCode(hsnCode)
          .workType(workType != null ? workType.name() : null)
          .quantity(new BigDecimal(itemReq.getQuantity()))
          .unitOfMeasurement(unitOfMeasurement)
          .taxableValue(new BigDecimal(itemReq.getTaxableValue()))
          .remarks(itemReq.getRemarks())
          .build();

      // Set rate if provided
      if (itemReq.getRatePerUnit() != null) {
        lineItem.setRatePerUnit(new BigDecimal(itemReq.getRatePerUnit()));
      }

      // Use tax rates from request if provided, otherwise use defaults based on WorkType
      if (itemReq.getCgstRate() != null) {
        lineItem.setCgstRate(new BigDecimal(itemReq.getCgstRate()));
      } else {
        lineItem.setCgstRate(defaultCgstRate);
      }

      if (itemReq.getSgstRate() != null) {
        lineItem.setSgstRate(new BigDecimal(itemReq.getSgstRate()));
      } else {
        lineItem.setSgstRate(defaultSgstRate);
      }

      if (itemReq.getIgstRate() != null) {
        lineItem.setIgstRate(new BigDecimal(itemReq.getIgstRate()));
      } else {
        lineItem.setIgstRate(defaultIgstRate);
      }

      // Calculate tax amounts based on inter-state flag
      boolean isInterState = challan.isInterState();
      lineItem.calculateTaxAmounts(isInterState);

      // Calculate total value
      lineItem.calculateTotalValue();

      challan.addLineItem(lineItem);
      
      log.debug("Added manual line item: {} x {} ({}) with WorkType: {}", 
               itemReq.getItemName(), itemReq.getQuantity(), unitOfMeasurement, workType);
    }
  }

  private void deriveLineItemsFromDispatchBatches(DeliveryChallan challan, List<DispatchBatch> batches) {
    deriveLineItemsFromDispatchBatches(challan, batches, null);
  }

  private void deriveLineItemsFromDispatchBatches(DeliveryChallan challan, List<DispatchBatch> batches, 
                                                  List<ChallanGenerationRequest.ItemOverride> itemOverrides) {
    log.debug("Deriving line items from {} dispatch batches", batches.size());
    
    // Get tenant invoice settings (use invoice settings instead of challan settings)
    TenantInvoiceSettings invoiceSettings =
        tenantInvoiceSettingsRepository
            .findByTenantIdAndIsActiveTrueAndDeletedFalse(challan.getTenant().getId())
            .orElse(null);
    
    if (invoiceSettings == null) {
      throw new IllegalStateException("Invoice settings not found for tenant: " + challan.getTenant().getId());
    }
    
    int lineNumber = 1;
    
    for (DispatchBatch batch : batches) {
      ProcessedItemDispatchBatch processedItem = batch.getProcessedItemDispatchBatch();
      
      if (processedItem == null) {
        log.warn("Dispatch batch {} has no processed item, skipping", batch.getDispatchBatchNumber());
        continue;
      }
      
      try {
        // Get ItemWorkflow to extract item details
        ItemWorkflow itemWorkflow = itemWorkflowRepository.findById(processedItem.getItemWorkflowId())
            .orElseThrow(() -> new RuntimeException(
                "ItemWorkflow not found with id: " + processedItem.getItemWorkflowId()));
        
        // Determine WorkType for this batch to get correct HSN and tax rates
        WorkType workType =
            determineWorkTypeFromBatch(batch);
        
        // Extract item name from ItemWorkflow
        String itemName = itemWorkflow.getItem() != null 
            ? itemWorkflow.getItem().getItemName() 
            : "Unknown Item";
        
        // Get quantity from processed item
        BigDecimal quantity = BigDecimal.valueOf(processedItem.getTotalDispatchPiecesCount());
        
        // Get unit of measurement (default to PIECES)
        String unitOfMeasurement = UnitOfMeasurement.PIECES.name();
        
        // Get HSN code and tax rates based on WorkType (these are defaults)
        String hsnCode;
        BigDecimal cgstRate;
        BigDecimal sgstRate;
        BigDecimal igstRate;
        BigDecimal unitPrice = BigDecimal.ZERO; // Default to zero for non-taxable challans
        
        if (workType == WorkType.JOB_WORK_ONLY) {
          // Use Job Work settings
          hsnCode = invoiceSettings.getJobWorkHsnSacCode();
          cgstRate = invoiceSettings.getJobWorkCgstRate();
          sgstRate = invoiceSettings.getJobWorkSgstRate();
          igstRate = invoiceSettings.getJobWorkIgstRate();
          log.debug("Using JOB_WORK_ONLY settings: HSN={}, CGST={}, SGST={}, IGST={}", 
                   hsnCode, cgstRate, sgstRate, igstRate);
        } else {
          // Use Material (WITH_MATERIAL) settings
          hsnCode = invoiceSettings.getMaterialHsnSacCode();
          cgstRate = invoiceSettings.getMaterialCgstRate();
          sgstRate = invoiceSettings.getMaterialSgstRate();
          igstRate = invoiceSettings.getMaterialIgstRate();
          log.debug("Using WITH_MATERIAL settings: HSN={}, CGST={}, SGST={}, IGST={}", 
                   hsnCode, cgstRate, sgstRate, igstRate);
        }
        
        // Check if there are any overrides for this batch
        if (itemOverrides != null && !itemOverrides.isEmpty()) {
          for (ChallanGenerationRequest.ItemOverride override : itemOverrides) {
            if (override.getDispatchBatchId().equals(batch.getId())) {
              // Apply overrides if provided
              if (override.getUnitPrice() != null) {
                unitPrice = BigDecimal.valueOf(override.getUnitPrice());
                log.debug("Overriding unit price for batch {}: {}", batch.getId(), unitPrice);
              }
              if (override.getHsnCode() != null && !override.getHsnCode().isBlank()) {
                hsnCode = override.getHsnCode();
                log.debug("Overriding HSN code for batch {}: {}", batch.getId(), hsnCode);
              }
              if (override.getCgstRate() != null) {
                cgstRate = BigDecimal.valueOf(override.getCgstRate());
                log.debug("Overriding CGST rate for batch {}: {}", batch.getId(), cgstRate);
              }
              if (override.getSgstRate() != null) {
                sgstRate = BigDecimal.valueOf(override.getSgstRate());
                log.debug("Overriding SGST rate for batch {}: {}", batch.getId(), sgstRate);
              }
              if (override.getIgstRate() != null) {
                igstRate = BigDecimal.valueOf(override.getIgstRate());
                log.debug("Overriding IGST rate for batch {}: {}", batch.getId(), igstRate);
              }
              break;
            }
          }
        }
        
        // Calculate taxable value from unit price and quantity
        BigDecimal taxableValue = unitPrice.multiply(quantity);
        
        // Build line item
        ChallanLineItem lineItem = ChallanLineItem.builder()
            .lineNumber(lineNumber++)
            .itemName(itemName)
            .hsnCode(hsnCode)
            .workType(workType != null ? workType.name() : null)
            .quantity(quantity)
            .unitOfMeasurement(unitOfMeasurement)
            .ratePerUnit(unitPrice)
            .taxableValue(taxableValue)
            .cgstRate(cgstRate)
            .sgstRate(sgstRate)
            .igstRate(igstRate)
            .itemWorkflowId(processedItem.getItemWorkflowId())
            .processedItemDispatchBatchId(processedItem.getId())
            .build();
        
        // Calculate tax amounts based on inter-state flag
        // Note: At this point, challan should have consignee details set
        boolean isInterState = challan.isInterState();
        lineItem.calculateTaxAmounts(isInterState);
        
        // Calculate total value
        lineItem.calculateTotalValue();
        
        challan.addLineItem(lineItem);
        
        log.debug("Created line item: {} x {} ({}) with WorkType: {}", 
                 itemName, quantity, unitOfMeasurement, workType);
        
      } catch (Exception e) {
        log.error("Error creating line item for processed item {}: {}", 
                 processedItem.getId(), e.getMessage(), e);
        throw new RuntimeException("Failed to create challan line item: " + e.getMessage(), e);
      }
    }
  }
  
  /**
   * Persist consignor details, bank details, terms & conditions, and amount in words to challan
   * This ensures display consistency even if tenant settings change later
   */
  private void persistChallanDisplayFields(DeliveryChallan challan, Tenant tenant) {
    // Persist consignor details from tenant
    challan.setConsignorName(tenant.getTenantName());
    challan.setConsignorGstin(tenant.getGstin());
    challan.setConsignorAddress(tenant.getAddress());
    challan.setConsignorStateCode(tenant.getStateCode());
    
    // Get invoice settings for bank details and terms
    com.jangid.forging_process_management_service.entities.settings.TenantInvoiceSettings invoiceSettings = 
        tenantInvoiceSettingsRepository
            .findByTenantIdAndIsActiveTrueAndDeletedFalse(tenant.getId())
            .orElse(null);
    
    if (invoiceSettings != null) {
      // Persist bank details
      challan.setBankName(invoiceSettings.getBankName());
      challan.setAccountNumber(invoiceSettings.getAccountNumber());
      challan.setIfscCode(invoiceSettings.getIfscCode());
      
      // Persist terms and conditions (use job work terms as default for challans)
      String termsAndConditions = invoiceSettings.getJobWorkTermsAndConditions();
      if (termsAndConditions == null || termsAndConditions.isBlank()) {
        termsAndConditions = invoiceSettings.getMaterialTermsAndConditions();
      }
      challan.setTermsAndConditions(termsAndConditions);
    }
    
    // Generate and persist amount in words
    if (challan.getTotalValue() != null && challan.getTotalValue().compareTo(BigDecimal.ZERO) > 0) {
      String amountInWords = com.jangid.forging_process_management_service.utils.NumberToWordsConverter.convertToWords(challan.getTotalValue());
      challan.setAmountInWords(amountInWords);
      log.debug("Set amount in words for challan: {}", amountInWords);
    }
    
    log.debug("Persisted display fields for challan - Consignor: {}, Bank: {}, Terms: {}", 
             challan.getConsignorName(), 
             challan.getBankName() != null ? "Yes" : "No",
             challan.getTermsAndConditions() != null ? "Yes" : "No");
  }
}


