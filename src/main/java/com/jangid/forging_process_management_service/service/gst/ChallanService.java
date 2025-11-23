package com.jangid.forging_process_management_service.service.gst;

import com.jangid.forging_process_management_service.dto.gst.ChallanGenerationRequest;
import com.jangid.forging_process_management_service.entities.Tenant;
import com.jangid.forging_process_management_service.entities.buyer.Buyer;
import com.jangid.forging_process_management_service.entities.buyer.BuyerEntity;
import com.jangid.forging_process_management_service.entities.dispatch.DispatchBatch;
import com.jangid.forging_process_management_service.entities.dispatch.ProcessedItemDispatchBatch;
import com.jangid.forging_process_management_service.entities.gst.ChallanDispatchBatch;
import com.jangid.forging_process_management_service.entities.gst.ChallanLineItem;
import com.jangid.forging_process_management_service.entities.gst.ChallanStatus;
import com.jangid.forging_process_management_service.entities.gst.ChallanType;
import com.jangid.forging_process_management_service.entities.gst.ChallanVendorDispatchBatch;
import com.jangid.forging_process_management_service.entities.gst.ChallanVendorLineItem;
import com.jangid.forging_process_management_service.entities.gst.DeliveryChallan;
import com.jangid.forging_process_management_service.entities.gst.TransportationMode;
import com.jangid.forging_process_management_service.entities.order.OrderItemWorkflow;
import com.jangid.forging_process_management_service.entities.order.WorkType;
import com.jangid.forging_process_management_service.entities.product.UnitOfMeasurement;
import com.jangid.forging_process_management_service.entities.settings.TenantChallanSettings;
import com.jangid.forging_process_management_service.entities.settings.TenantInvoiceSettings;
import com.jangid.forging_process_management_service.entities.settings.TenantVendorChallanSettings;
import com.jangid.forging_process_management_service.entities.vendor.ProcessedItemVendorDispatchBatch;
import com.jangid.forging_process_management_service.entities.vendor.Vendor;
import com.jangid.forging_process_management_service.entities.vendor.VendorDispatchBatch;
import com.jangid.forging_process_management_service.entities.vendor.VendorEntity;
import com.jangid.forging_process_management_service.entities.workflow.ItemWorkflow;
import com.jangid.forging_process_management_service.repositories.buyer.BuyerEntityRepository;
import com.jangid.forging_process_management_service.repositories.buyer.BuyerRepository;
import com.jangid.forging_process_management_service.repositories.dispatch.DispatchBatchRepository;
import com.jangid.forging_process_management_service.repositories.gst.DeliveryChallanRepository;
import com.jangid.forging_process_management_service.repositories.order.OrderItemWorkflowRepository;
import com.jangid.forging_process_management_service.repositories.settings.TenantChallanSettingsRepository;
import com.jangid.forging_process_management_service.repositories.settings.TenantInvoiceSettingsRepository;
import com.jangid.forging_process_management_service.repositories.settings.TenantVendorChallanSettingsRepository;
import com.jangid.forging_process_management_service.repositories.vendor.VendorDispatchBatchRepository;
import com.jangid.forging_process_management_service.repositories.vendor.VendorEntityRepository;
import com.jangid.forging_process_management_service.repositories.vendor.VendorRepository;
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
  private final BuyerRepository buyerRepository;
  private final VendorRepository vendorRepository;
  private final BuyerEntityRepository buyerEntityRepository;
  private final VendorEntityRepository vendorEntityRepository;
  private final TenantChallanSettingsRepository challanSettingsRepository;
  private final TenantService tenantService;
  private final OrderService orderService;
  private final ItemWorkflowRepository itemWorkflowRepository;
  private final TenantInvoiceSettingsRepository tenantInvoiceSettingsRepository;
  private final OrderItemWorkflowRepository orderItemWorkflowRepository;
  private final VendorDispatchBatchRepository vendorDispatchBatchRepository;
  private final TenantVendorChallanSettingsRepository tenantVendorChallanSettingsRepository;

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

    // Create challan with GENERATED status
    DeliveryChallan challan = DeliveryChallan.builder()
        .tenant(tenant)
        .challanType(ChallanType.valueOf(request.getChallanType()))
        .otherChallanTypeDetails(request.getOtherChallanTypeDetails())
        .transportationReason(request.getTransportationReason())
        .status(ChallanStatus.GENERATED)
        .isVendorChallan(false)  // Dispatch batch challan (uses TenantChallanSettings)
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
    WorkType challanWorkType = null;
    if (!dispatchBatches.isEmpty()) {
      challanWorkType = determineWorkTypeFromBatch(dispatchBatches.get(0));
      challan.setWorkType(challanWorkType);
      log.debug("Set challan WorkType to: {}", challanWorkType);
    }

    // Add line items (from manual input or derive from dispatch batches)
    if (request.getManualLineItems() != null && !request.getManualLineItems().isEmpty()) {
      addManualLineItems(challan, request.getManualLineItems(), request.getValueOption());
    } else {
      deriveLineItemsFromDispatchBatches(challan, dispatchBatches, request.getItemOverrides(), request.getValueOption(), challanWorkType);
    }

    // Handle value option (TAXABLE vs ESTIMATED)
    if ("ESTIMATED".equalsIgnoreCase(request.getValueOption())) {
      // For ESTIMATED value option, calculate estimated value from line items
      // and set it on the challan (this will be used instead of taxable value + taxes)
      BigDecimal estimatedValue = challan.getLineItems().stream()
          .map(item -> item.getRatePerUnit() != null && item.getQuantity() != null 
              ? item.getRatePerUnit().multiply(item.getQuantity()) 
              : BigDecimal.ZERO)
          .reduce(BigDecimal.ZERO, BigDecimal::add);
      challan.setEstimatedValue(estimatedValue);
      log.debug("Set estimated value for challan: {}", estimatedValue);
    }

    // Calculate totals
    challan.calculateTotals();
    
    // Persist consignor details, bank details, terms & conditions, and amount in words
    persistChallanDisplayFields(challan, tenant, challanWorkType);

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
        .orElse(null);
  }

  /**
   * Get challan by vendor dispatch batch ID
   */
  @Transactional(readOnly = true)
  public DeliveryChallan getChallanByVendorDispatchBatchId(Long vendorDispatchBatchId) {
    return challanRepository.findByVendorDispatchBatchId(vendorDispatchBatchId)
        .orElse(null);
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
   * When deleted, associated dispatch batches or vendor dispatch batches are moved back to READY_TO_DISPATCH
   * and associated challan dispatch batches/vendor dispatch batches and line items are also deleted
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
                      "DISPATCHED challans cannot be deleted.",
                      challan.getStatus()));
    }

    // Check if this is a vendor challan or dispatch batch challan
    if (challan.getIsVendorChallan()) {
      // Handle vendor dispatch batch challan
      List<VendorDispatchBatch> vendorDispatchBatches = challan.getVendorDispatchBatches();

      // Clean up associated entities
      cleanupChallanAssociations(challan);

      // Mark challan as deleted
      challan.setDeleted(true);
      challan.setDeletedAt(LocalDateTime.now());
      challanRepository.save(challan);

      // Move vendor dispatch batches back to READY_TO_DISPATCH
      for (VendorDispatchBatch batch : vendorDispatchBatches) {
        batch.setVendorDispatchBatchStatus(VendorDispatchBatch.VendorDispatchBatchStatus.READY_TO_DISPATCH);
        vendorDispatchBatchRepository.save(batch);
        log.debug("Moved vendor dispatch batch {} back to READY_TO_DISPATCH after challan deletion", 
                  batch.getVendorDispatchBatchNumber());
      }
      
      log.info("Deleted vendor challan: {} (number: {}) for tenant: {}. {} vendor dispatch batch(es) moved back to READY_TO_DISPATCH. " +
               "Challan number will be available for reuse.", 
               challanId, challan.getChallanNumber(), tenantId, vendorDispatchBatches.size());
    } else {
      // Handle regular dispatch batch challan
      List<DispatchBatch> dispatchBatches = challan.getDispatchBatches();

      // Clean up associated entities
      cleanupChallanAssociations(challan);

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
      
      log.info("Deleted challan: {} (number: {}) for tenant: {}. {} dispatch batch(es) moved back to READY_TO_DISPATCH. " +
               "Challan number will be available for reuse.", 
               challanId, challan.getChallanNumber(), tenantId, dispatchBatches.size());
    }
    
    // Note: We DON'T decrement the sequence number here.
    // Instead, the generateChallanNumber method will reuse deleted challan numbers
    // by checking for deleted=true challans with the oldest deletedAt timestamp.
    // This approach is safer and prevents sequence number inconsistencies.
  }

  /**
   * Cancel challan
   * When cancelled, associated dispatch batches or vendor dispatch batches are moved back to READY_TO_DISPATCH
   * and associated challan dispatch batches/vendor dispatch batches and line items are also deleted
   */
  @Transactional
  public DeliveryChallan cancelChallan(Long tenantId, Long challanId, String cancelledBy, String reason, LocalDateTime cancelledAt) {
    DeliveryChallan challan = getChallanById(challanId);
    
    if (challan.getTenant().getId() != tenantId) {
      throw new IllegalArgumentException("Challan does not belong to tenant");
    }

    if (!challan.canBeCancelled()) {
      throw new IllegalStateException("Only GENERATED or DISPATCHED challans can be cancelled");
    }

    // Clean up associated entities
    cleanupChallanAssociations(challan);

    challan.setStatus(ChallanStatus.CANCELLED);
    challan.setCancelledAt(cancelledAt != null ? cancelledAt : LocalDateTime.now());
    challan.setCancelledBy(cancelledBy);
    challan.setCancellationReason(reason);

    // Check if this is a vendor challan or dispatch batch challan and move batches back
    if (challan.getIsVendorChallan()) {
      // Handle vendor dispatch batch challan
      List<VendorDispatchBatch> vendorDispatchBatches = challan.getVendorDispatchBatches();
      for (VendorDispatchBatch batch : vendorDispatchBatches) {
        batch.setVendorDispatchBatchStatus(VendorDispatchBatch.VendorDispatchBatchStatus.READY_TO_DISPATCH);
        vendorDispatchBatchRepository.save(batch);
        log.debug("Moved vendor dispatch batch {} back to READY_TO_DISPATCH after challan cancellation",
                  batch.getVendorDispatchBatchNumber());
      }
      log.info("Cancelled vendor challan: {} for tenant: {}. {} vendor dispatch batch(es) moved back to READY_TO_DISPATCH.",
               challanId, tenantId, vendorDispatchBatches.size());
    } else {
      // Handle regular dispatch batch challan
      List<DispatchBatch> dispatchBatches = challan.getDispatchBatches();
      for (DispatchBatch batch : dispatchBatches) {
        batch.setDispatchBatchStatus(DispatchBatch.DispatchBatchStatus.READY_TO_DISPATCH);
        dispatchBatchRepository.save(batch);
        log.debug("Moved dispatch batch {} back to READY_TO_DISPATCH after challan cancellation",
                  batch.getDispatchBatchNumber());
      }
      log.info("Cancelled challan: {} for tenant: {}. {} dispatch batch(es) moved back to READY_TO_DISPATCH.",
               challanId, tenantId, dispatchBatches.size());
    }
    
    return challanRepository.save(challan);
  }

  /**
   * Helper method to clean up associated entities for a challan
   * This includes ChallanDispatchBatch, ChallanVendorDispatchBatch, ChallanLineItem, and ChallanVendorLineItem entities
   * Soft-deletes the entities to maintain data integrity
   */
  private void cleanupChallanAssociations(DeliveryChallan challan) {
    // Soft-delete associated challan dispatch batches (for regular dispatch batch challans)
    List<ChallanDispatchBatch> challanDispatchBatches = challan.getChallanDispatchBatches();
    if (challanDispatchBatches != null && !challanDispatchBatches.isEmpty()) {
      log.debug("Soft-deleting {} associated challan dispatch batches", challanDispatchBatches.size());
      for (ChallanDispatchBatch cdb : challanDispatchBatches) {
        cdb.setDeleted(true);
      }
    }

    // Soft-delete associated challan vendor dispatch batches (for vendor dispatch batch challans)
    List<ChallanVendorDispatchBatch> challanVendorDispatchBatches = challan.getChallanVendorDispatchBatches();
    if (challanVendorDispatchBatches != null && !challanVendorDispatchBatches.isEmpty()) {
      log.debug("Soft-deleting {} associated challan vendor dispatch batches", challanVendorDispatchBatches.size());
      for (ChallanVendorDispatchBatch cvdb : challanVendorDispatchBatches) {
        cvdb.setDeleted(true);
        cvdb.setDeletedAt(LocalDateTime.now());
      }
    }

    // Soft-delete associated challan line items (for regular dispatch batch challans)
    List<ChallanLineItem> lineItems = challan.getLineItems();
    if (lineItems != null && !lineItems.isEmpty()) {
      log.debug("Soft-deleting {} associated challan line items", lineItems.size());
      for (ChallanLineItem item : lineItems) {
        item.setDeleted(true);
      }
    }

    // Soft-delete associated challan vendor line items (for vendor dispatch batch challans)
    List<ChallanVendorLineItem> vendorLineItems = challan.getChallanVendorLineItems();
    if (vendorLineItems != null && !vendorLineItems.isEmpty()) {
      log.debug("Soft-deleting {} associated challan vendor line items", vendorLineItems.size());
      for (ChallanVendorLineItem item : vendorLineItems) {
        item.setDeleted(true);
        item.setDeletedAt(LocalDateTime.now());
      }
    }
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
      // Reuse the oldest deleted challan number only if it's not already used by a new challan
      String reusedChallanNumber = deletedChallans.get(0).getChallanNumber();
      // Verify that this number is not currently in use by a non-deleted challan
      if (!challanRepository.existsByChallanNumberAndTenantIdAndDeletedFalse(reusedChallanNumber, tenantId)) {
        log.info("Reusing deleted challan number: {} for tenant: {}", reusedChallanNumber, tenantId);
        return reusedChallanNumber;
      } else {
        // If the deleted number is already used by a new challan, we need to generate a new one
        log.info("Deleted challan number {} is already in use, generating new number", reusedChallanNumber);
      }
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
   * Generate vendor challan number from tenant vendor challan settings
   * Reuses deleted vendor challan numbers before generating new ones
   * Uses TenantVendorChallanSettings instead of TenantChallanSettings
   */
  private String generateVendorChallanNumber(Long tenantId) {
    TenantVendorChallanSettings settings = tenantVendorChallanSettingsRepository
        .findByTenantIdAndIsActiveTrueAndDeletedFalse(tenantId)
        .orElseThrow(() -> new IllegalStateException("Vendor challan settings not found for tenant"));

    // Check for deleted vendor challan numbers that can be reused
    // Only reuse numbers from challans where isVendorChallan = true
    List<DeliveryChallan> deletedVendorChallans = challanRepository
        .findByTenantIdAndIsVendorChallanAndDeletedOrderByDeletedAtAsc(tenantId, true);
    
    if (!deletedVendorChallans.isEmpty()) {
      // Reuse the oldest deleted vendor challan number only if it's not already used
      String reusedChallanNumber = deletedVendorChallans.get(0).getChallanNumber();
      // Verify that this number is not currently in use by a non-deleted challan
      if (!challanRepository.existsByChallanNumberAndTenantIdAndDeletedFalse(reusedChallanNumber, tenantId)) {
        log.info("Reusing deleted vendor challan number: {} for tenant: {}", reusedChallanNumber, tenantId);
        return reusedChallanNumber;
      } else {
        // If the deleted number is already used by a new challan, generate a new one
        log.info("Deleted vendor challan number {} is already in use, generating new number", reusedChallanNumber);
      }
    }

    // No deleted vendor challans to reuse - generate new vendor challan number
    // Format: PREFIX/SERIES/SEQUENCE (Example: VCH/2025-26/0001)
    String challanNumber = settings.getNextChallanNumber();

    // Increment sequence for next vendor challan
    settings.incrementSequence();
    tenantVendorChallanSettingsRepository.save(settings);

    log.info("Generated new vendor challan number: {} for tenant: {} using prefix: {}", 
             challanNumber, tenantId, settings.getChallanPrefix());
    return challanNumber;
  }


  /**
   * Get count of ready to dispatch batches
   */
  @Transactional(readOnly = true)
  public long getReadyToDispatchBatchesCount(Long tenantId) {
    return dispatchBatchService.getReadyToDispatchBatchesCount(tenantId);
  }

  /**
   * Get count of ready to dispatch vendor batches
   */
  @Transactional(readOnly = true)
  public long getReadyToDispatchVendorBatchesCount(Long tenantId) {
    return vendorDispatchBatchRepository.countByVendorDispatchBatchStatusAndTenantIdAndDeletedFalse(
        VendorDispatchBatch.VendorDispatchBatchStatus.READY_TO_DISPATCH,
        tenantId
    );
  }

  /**
   * Get challan dashboard statistics
   * Note: challans are created as GENERATED
   */
  @Transactional(readOnly = true)
  public Map<String, Object> getChallanDashboardStats(Long tenantId) {
    Map<String, Object> stats = new HashMap<>();

    long generatedCount = challanRepository.countByTenantIdAndStatus(tenantId, ChallanStatus.GENERATED);
    long dispatchedCount = challanRepository.countByTenantIdAndStatus(tenantId, ChallanStatus.DISPATCHED);
    long cancelledCount = challanRepository.countByTenantIdAndStatus(tenantId, ChallanStatus.CANCELLED);
    
    // Count ready to dispatch batches
    long readyToChallanCount = getReadyToDispatchBatchesCount(tenantId);
    long readyToVendorChallanCount = getReadyToDispatchVendorBatchesCount(tenantId);
    
    stats.put("generatedCount", generatedCount);
    stats.put("inTransitCount", dispatchedCount); // Dispatched = In Transit
    stats.put("cancelledCount", cancelledCount);
    stats.put("readyToChallanCount", readyToChallanCount);
    stats.put("readyToVendorChallanCount", readyToVendorChallanCount);
    stats.put("totalActiveChallans", generatedCount + dispatchedCount);
    
    return stats;
  }

  /**
   * Generate challan from vendor dispatch batch
   */
  @Transactional
  public DeliveryChallan generateChallanFromVendorDispatchBatch(Long tenantId, ChallanGenerationRequest request) {
    log.info("Generating challan for tenant: {} with vendor dispatch batch: {} - Type: {}",
             tenantId, request.getVendorDispatchBatchId(), request.getChallanType());

    // Validate request
    if (request.getVendorDispatchBatchId() == null) {
      throw new IllegalArgumentException("Vendor dispatch batch ID is required");
    }

    // Get tenant
    Tenant tenant = tenantService.getTenantById(tenantId);

    // Create challan with GENERATED status
    DeliveryChallan challan = DeliveryChallan.builder()
        .tenant(tenant)
        .challanType(ChallanType.valueOf(request.getChallanType()))
        .otherChallanTypeDetails(request.getOtherChallanTypeDetails())
        .transportationReason(request.getTransportationReason())
        .status(ChallanStatus.GENERATED)
        .isVendorChallan(true)  // Vendor dispatch batch challan (uses TenantVendorChallanSettings)
        .build();

    // Set challan number (auto-generate if not provided using vendor challan settings)
    String challanNumber;
    if (request.getChallanNumber() != null && !request.getChallanNumber().isBlank()) {
      challanNumber = request.getChallanNumber();
    } else {
      challanNumber = generateVendorChallanNumber(tenantId);
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

    // Get vendor dispatch batch
    VendorDispatchBatch vendorDispatchBatch = vendorDispatchBatchRepository.findById(request.getVendorDispatchBatchId())
        .orElseThrow(() -> new IllegalArgumentException("Vendor dispatch batch not found with id: " + request.getVendorDispatchBatchId()));
    
    if (vendorDispatchBatch.getTenant().getId() != tenantId) {
      throw new IllegalArgumentException("Vendor dispatch batch does not belong to tenant");
    }

    // Validate vendor dispatch batch status
    if (vendorDispatchBatch.getVendorDispatchBatchStatus() != VendorDispatchBatch.VendorDispatchBatchStatus.READY_TO_DISPATCH) {
      throw new IllegalArgumentException("Vendor dispatch batch must be in READY_TO_DISPATCH status to generate challan");
    }

    // Save challan first to get ID
    challan = challanRepository.save(challan);

    // Add vendor dispatch batch association
    ChallanVendorDispatchBatch cvdb = ChallanVendorDispatchBatch.builder()
        .deliveryChallan(challan)
        .vendorDispatchBatch(vendorDispatchBatch)
        .tenant(tenant)
        .build();
    
    // Save the association
    challan.addChallanVendorDispatchBatch(cvdb);

    // Determine and set WorkType for the challan from vendor dispatch batch
    WorkType challanWorkType = determineWorkTypeFromVendorDispatchBatch(vendorDispatchBatch);
    challan.setWorkType(challanWorkType);
    log.debug("Set challan WorkType to: {}", challanWorkType);

    // Add line items from vendor dispatch batch
    addLineItemsFromVendorDispatchBatch(challan, vendorDispatchBatch, request.getItemOverrides(), request.getValueOption(), challanWorkType);

    // Handle value option (TAXABLE vs ESTIMATED)
    if ("ESTIMATED".equalsIgnoreCase(request.getValueOption())) {
      // For ESTIMATED value option, calculate estimated value from line items
      // and set it on the challan (this will be used instead of taxable value + taxes)
      BigDecimal estimatedValue = challan.getChallanVendorLineItems().stream()
          .map(item -> item.getRatePerUnit() != null && item.getQuantity() != null 
              ? item.getRatePerUnit().multiply(item.getQuantity()) 
              : BigDecimal.ZERO)
          .reduce(BigDecimal.ZERO, BigDecimal::add);
      challan.setEstimatedValue(estimatedValue);
      log.debug("Set estimated value for challan: {}", estimatedValue);
    }

    // Calculate totals
    challan.calculateTotalsForVendorChallan();
    
    // Persist consignor details, bank details, terms & conditions, and amount in words
    persistChallanDisplayFields(challan, tenant, challanWorkType);

    // Save challan
    challan = challanRepository.save(challan);

    // Update vendor dispatch batch status to CHALLAN_CREATED
    vendorDispatchBatch.setVendorDispatchBatchStatus(VendorDispatchBatch.VendorDispatchBatchStatus.CHALLAN_CREATED);
    vendorDispatchBatchRepository.save(vendorDispatchBatch);
    log.debug("Updated vendor dispatch batch {} status to CHALLAN_CREATED", vendorDispatchBatch.getVendorDispatchBatchNumber());

    log.info("Successfully generated challan {} with GENERATED status for vendor dispatch batch {}", 
             challan.getChallanNumber(), vendorDispatchBatch.getVendorDispatchBatchNumber());

    return challan;
  }

  /**
   * Helper method to add line items from vendor dispatch batch
   */
  private void addLineItemsFromVendorDispatchBatch(DeliveryChallan challan, VendorDispatchBatch vendorDispatchBatch,
                                                    List<ChallanGenerationRequest.ItemOverride> itemOverrides,
                                                    String valueOption, WorkType challanWorkType) {
    log.debug("Deriving line items from vendor dispatch batch with value option: {}", valueOption);

    TenantInvoiceSettings invoiceSettings =
        tenantInvoiceSettingsRepository
            .findByTenantIdAndIsActiveTrueAndDeletedFalse(challan.getTenant().getId())
            .orElse(null);

    if (invoiceSettings == null) {
      throw new IllegalStateException("InvoiceSettings not found for tenant");
    }

    int lineNumber = 1;

      ProcessedItemVendorDispatchBatch processedItem = vendorDispatchBatch.getProcessedItem();

      try {
        // Get ItemWorkflow to extract item details
        ItemWorkflow itemWorkflow = itemWorkflowRepository.findById(processedItem.getItemWorkflowId())
            .orElseThrow(() -> new RuntimeException(
                "ItemWorkflow not found with id: " + processedItem.getItemWorkflowId()));

        // Determine WorkType for this batch to get correct HSN and tax rates
        if (challanWorkType != null) {
          challanWorkType = determineWorkTypeFromVendorDispatchBatch(vendorDispatchBatch);
        }

        // Extract item name from ItemWorkflow
        String itemName = itemWorkflow.getItem() != null
                          ? itemWorkflow.getItem().getItemName()
                          : "Unknown Item";

        BigDecimal quantity = BigDecimal.valueOf(processedItem.getDispatchedPiecesCount());

        // Get unit of measurement (default to PIECES)
        String unitOfMeasurement = UnitOfMeasurement.PIECES.name();

        // Get HSN code and tax rates based on WorkType (these are defaults)
        String hsnCode;
        BigDecimal cgstRate;
        BigDecimal sgstRate;
        BigDecimal igstRate;
        BigDecimal unitPrice = BigDecimal.ZERO; // Default to zero for non-taxable challans

        if (challanWorkType == WorkType.JOB_WORK_ONLY) {
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
            if (override.getBatchId().equals(vendorDispatchBatch.getId())) {
              // Apply overrides if provided
              if (override.getUnitPrice() != null) {
                unitPrice = BigDecimal.valueOf(override.getUnitPrice());
                log.debug("Overriding unit price for batch {}: {}", vendorDispatchBatch.getId(), unitPrice);
              }
              if (override.getHsnCode() != null && !override.getHsnCode().isBlank()) {
                hsnCode = override.getHsnCode();
                log.debug("Overriding HSN code for batch {}: {}", vendorDispatchBatch.getId(), hsnCode);
              }
              if (override.getCgstRate() != null) {
                cgstRate = BigDecimal.valueOf(override.getCgstRate());
                log.debug("Overriding CGST rate for batch {}: {}", vendorDispatchBatch.getId(), cgstRate);
              }
              if (override.getSgstRate() != null) {
                sgstRate = BigDecimal.valueOf(override.getSgstRate());
                log.debug("Overriding SGST rate for batch {}: {}", vendorDispatchBatch.getId(), sgstRate);
              }
              if (override.getIgstRate() != null) {
                igstRate = BigDecimal.valueOf(override.getIgstRate());
                log.debug("Overriding IGST rate for batch {}: {}", vendorDispatchBatch.getId(), igstRate);
              }
              break;
            }
          }
        }

        // Calculate taxable value from unit price and quantity
        BigDecimal taxableValue = unitPrice.multiply(quantity);

        // Check inter-state flag to determine tax applicability
        boolean isInterState = challan.isInterState();

        // Handle tax rates based on value option and inter-state flag
        if ("ESTIMATED".equalsIgnoreCase(valueOption)) {
          // For ESTIMATED value option, set all tax rates to zero
          cgstRate = BigDecimal.ZERO;
          sgstRate = BigDecimal.ZERO;
          igstRate = BigDecimal.ZERO;
          log.debug("Set tax rates to zero for ESTIMATED value option for vendorDispatchBatch {}", vendorDispatchBatch.getId());
        } else if (isInterState) {
          // Inter-state transaction: only IGST applies, CGST and SGST must be zero
          cgstRate = BigDecimal.ZERO;
          sgstRate = BigDecimal.ZERO;
          // Keep igstRate as is (either from settings or overrides)
          log.debug("Inter-state transaction: CGST and SGST set to zero for vendorDispatchBatch {}", vendorDispatchBatch.getId());
        } else {
          // Intra-state transaction: only CGST and SGST apply, IGST must be zero
          // Keep cgstRate and sgstRate as is (either from settings or overrides)
          igstRate = BigDecimal.ZERO;
          log.debug("Intra-state transaction: IGST set to zero for vendorDispatchBatch {}", vendorDispatchBatch.getId());
        }

        // Build line item
        ChallanVendorLineItem challanVendorLineItem = ChallanVendorLineItem.builder()
            .lineNumber(lineNumber++)
            .itemName(itemName)
            .hsnCode(hsnCode)
            .workType(challanWorkType != null ? challanWorkType.name() : null)
            .quantity(quantity)
            .unitOfMeasurement(unitOfMeasurement)
            .ratePerUnit(unitPrice)
            .taxableValue(taxableValue)
            .cgstRate(cgstRate)
            .sgstRate(sgstRate)
            .igstRate(igstRate)
            .vendorDispatchBatch(vendorDispatchBatch)
            .itemWorkflowId(processedItem.getItemWorkflowId())
            .processedItemVendorDispatchBatchId(processedItem.getId())
            .build();

        // Calculate tax amounts based on inter-state flag
        // Note: At this point, challan should have consignee details set
        challanVendorLineItem.calculateTaxAmounts(isInterState);

        // Calculate total value
        challanVendorLineItem.calculateTotalValue();

        challan.addChallanVendorLineItem(challanVendorLineItem);

        log.debug("Created vendor line item: {} x {} ({}) with WorkType: {}",
                  itemName, quantity, unitOfMeasurement, challanWorkType);

      } catch (Exception e) {
        log.error("Error creating vendor line item for processed item {}: {}",
                  processedItem.getId(), e.getMessage(), e);
        throw new RuntimeException("Failed to create vendor challan line item: " + e.getMessage(), e);
      }
  }

  /**
   * Determine WorkType from a vendor dispatch batch
   */
  private WorkType determineWorkTypeFromVendorDispatchBatch(VendorDispatchBatch batch) {
    ProcessedItemVendorDispatchBatch processedItem = batch.getProcessedItem();

    if (processedItem == null || processedItem.getItemWorkflowId() == null) {
      log.warn("No processed item or itemWorkflowId found for batch {}, defaulting to WITH_MATERIAL",
               batch.getVendorDispatchBatchNumber());
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

    log.debug("Determined WorkType as {} for batch {}", workType, batch.getVendorDispatchBatchNumber());
    return workType;
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
                      "DISPATCHED challans cannot be modified.",
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
    if (request.getBuyerId() != null) {
      Buyer buyer = buyerRepository.findById(request.getBuyerId())
          .orElseThrow(() -> new IllegalArgumentException("Buyer not found"));
      challan.setBuyer(buyer);

      BuyerEntity buyerBillingEntity = buyerEntityRepository.findById(request.getBuyerBillingEntityId())
          .orElseThrow(() -> new IllegalArgumentException("BuyerBillingEntity not found"));

      BuyerEntity buyerShippingEntity = buyerEntityRepository.findById(request.getBuyerShippingEntityId())
          .orElseThrow(() -> new IllegalArgumentException("BuyerShippingEntity not found"));

      challan.setBuyer(buyer);
      challan.setBuyerBillingEntity(buyerBillingEntity);
      challan.setBuyerShippingEntity(buyerShippingEntity);
    } else if (request.getVendorId() != null) {
      Vendor vendor = vendorRepository.findById(request.getVendorId())
          .orElseThrow(() -> new IllegalArgumentException("Vendor entity not found"));
      VendorEntity vendorBillingEntity = vendorEntityRepository.findById(request.getVendorBillingEntityId())
          .orElseThrow(() -> new IllegalArgumentException("VendorBillingEntity not found"));

      VendorEntity vendorShippingEntity = vendorEntityRepository.findById(request.getVendorShippingEntityId())
          .orElseThrow(() -> new IllegalArgumentException("VendorShippingEntity not found"));

      challan.setVendor(vendor);
      challan.setVendorBillingEntity(vendorBillingEntity);
      challan.setVendorShippingEntity(vendorShippingEntity);

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
                                  List<ChallanGenerationRequest.ChallanLineItemRequest> lineItemRequests,
                                  String valueOption) {
    log.debug("Adding {} manual line items with value option: {}", lineItemRequests.size(), valueOption);
    
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

      // Check inter-state flag to determine tax applicability
      boolean isInterState = challan.isInterState();

      // Handle tax rates based on value option and inter-state flag
      if ("ESTIMATED".equalsIgnoreCase(valueOption)) {
        // For ESTIMATED value option, set all tax rates to zero
        lineItem.setCgstRate(BigDecimal.ZERO);
        lineItem.setSgstRate(BigDecimal.ZERO);
        lineItem.setIgstRate(BigDecimal.ZERO);
        log.debug("Set tax rates to zero for ESTIMATED value option");
      } else {
        // For TAXABLE value option, apply tax rates based on inter-state flag
        if (isInterState) {
          // Inter-state transaction: only IGST applies, CGST and SGST must be zero
          lineItem.setCgstRate(BigDecimal.ZERO);
          lineItem.setSgstRate(BigDecimal.ZERO);
          if (itemReq.getIgstRate() != null) {
            lineItem.setIgstRate(new BigDecimal(itemReq.getIgstRate()));
          }
          log.debug("Inter-state transaction: CGST and SGST set to zero, IGST from request");
        } else {
          // Intra-state transaction: only CGST and SGST apply, IGST must be zero
          if (itemReq.getCgstRate() != null) {
            lineItem.setCgstRate(new BigDecimal(itemReq.getCgstRate()));
          }
          if (itemReq.getSgstRate() != null) {
            lineItem.setSgstRate(new BigDecimal(itemReq.getSgstRate()));
          }
          lineItem.setIgstRate(BigDecimal.ZERO);
          log.debug("Intra-state transaction: CGST and SGST from request, IGST set to zero");
        }
      }

      // Calculate tax amounts based on inter-state flag
      lineItem.calculateTaxAmounts(isInterState);

      // Calculate total value
      lineItem.calculateTotalValue();

      challan.addLineItem(lineItem);
      
      log.debug("Added manual line item: {} x {} ({}) with WorkType: {}", 
               itemReq.getItemName(), itemReq.getQuantity(), unitOfMeasurement, workType);
    }
  }

  private void deriveLineItemsFromDispatchBatches(DeliveryChallan challan, List<DispatchBatch> batches, 
                                                  List<ChallanGenerationRequest.ItemOverride> itemOverrides,
                                                  String valueOption, WorkType challanWorkType) {
    log.debug("Deriving line items from {} dispatch batches with value option: {}", batches.size(), valueOption);
    
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
        if (challanWorkType !=  null) {
          challanWorkType = determineWorkTypeFromBatch(batch);
        }

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
        
        if (challanWorkType == WorkType.JOB_WORK_ONLY) {
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
            if (override.getBatchId().equals(batch.getId())) {
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
        
        // Check inter-state flag to determine tax applicability
        boolean isInterState = challan.isInterState();
        
        // Handle tax rates based on value option and inter-state flag
        if ("ESTIMATED".equalsIgnoreCase(valueOption)) {
          // For ESTIMATED value option, set all tax rates to zero
          cgstRate = BigDecimal.ZERO;
          sgstRate = BigDecimal.ZERO;
          igstRate = BigDecimal.ZERO;
          log.debug("Set tax rates to zero for ESTIMATED value option for batch {}", batch.getId());
        } else if (isInterState) {
          // Inter-state transaction: only IGST applies, CGST and SGST must be zero
          cgstRate = BigDecimal.ZERO;
          sgstRate = BigDecimal.ZERO;
          // Keep igstRate as is (either from settings or overrides)
          log.debug("Inter-state transaction: CGST and SGST set to zero for batch {}", batch.getId());
        } else {
          // Intra-state transaction: only CGST and SGST apply, IGST must be zero
          // Keep cgstRate and sgstRate as is (either from settings or overrides)
          igstRate = BigDecimal.ZERO;
          log.debug("Intra-state transaction: IGST set to zero for batch {}", batch.getId());
        }
        
        // Build line item
        ChallanLineItem lineItem = ChallanLineItem.builder()
            .lineNumber(lineNumber++)
            .itemName(itemName)
            .hsnCode(hsnCode)
            .workType(challanWorkType != null ? challanWorkType.name() : null)
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
        lineItem.calculateTaxAmounts(isInterState);
        
        // Calculate total value
        lineItem.calculateTotalValue();
        
        challan.addLineItem(lineItem);
        
        log.debug("Created line item: {} x {} ({}) with WorkType: {}", 
                 itemName, quantity, unitOfMeasurement, challanWorkType);
        
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
  private void persistChallanDisplayFields(DeliveryChallan challan, Tenant tenant, WorkType challanWorkType) {
    // Persist consignor details from tenant
    challan.setConsignorName(tenant.getTenantName());
    challan.setConsignorGstin(tenant.getGstin());
    challan.setConsignorAddress(tenant.getAddress());
    challan.setConsignorStateCode(tenant.getStateCode());
    
    // Get invoice settings for bank details and terms
    TenantInvoiceSettings invoiceSettings =
        tenantInvoiceSettingsRepository
            .findByTenantIdAndIsActiveTrueAndDeletedFalse(tenant.getId())
            .orElse(null);
    
    if (invoiceSettings != null) {
      // Persist bank details
      challan.setBankName(invoiceSettings.getBankName());
      challan.setAccountNumber(invoiceSettings.getAccountNumber());
      challan.setIfscCode(invoiceSettings.getIfscCode());
      
      // Persist terms and conditions
      String termsAndConditions = null;
      if (challanWorkType.isJobWorkOnly()) {
        termsAndConditions = invoiceSettings.getJobWorkTermsAndConditions();
      } else {
        termsAndConditions = invoiceSettings.getMaterialTermsAndConditions();
      }

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


