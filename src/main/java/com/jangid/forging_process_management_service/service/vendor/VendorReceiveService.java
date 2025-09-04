package com.jangid.forging_process_management_service.service.vendor;

import com.jangid.forging_process_management_service.assemblers.vendor.VendorReceiveBatchAssembler;
import com.jangid.forging_process_management_service.dto.workflow.OperationOutcomeData;
import com.jangid.forging_process_management_service.entities.PackagingType;
import com.jangid.forging_process_management_service.entities.Tenant;
import com.jangid.forging_process_management_service.entities.vendor.ProcessedItemVendorDispatchBatch;
import com.jangid.forging_process_management_service.entities.vendor.Vendor;
import com.jangid.forging_process_management_service.entities.vendor.VendorDispatchBatch;
import com.jangid.forging_process_management_service.entities.vendor.VendorEntity;
import com.jangid.forging_process_management_service.entities.vendor.VendorReceiveBatch;
import com.jangid.forging_process_management_service.entities.workflow.ItemWorkflowStep;
import com.jangid.forging_process_management_service.entities.workflow.WorkflowStep;
import com.jangid.forging_process_management_service.entitiesRepresentation.vendor.VendorReceiveBatchRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.vendor.VendorQualityCheckCompletionRepresentation;
import com.jangid.forging_process_management_service.exception.ResourceNotFoundException;
import com.jangid.forging_process_management_service.exception.ValidationException;
import com.jangid.forging_process_management_service.repositories.TenantRepository;
import com.jangid.forging_process_management_service.repositories.vendor.ProcessedItemVendorDispatchBatchRepository;
import com.jangid.forging_process_management_service.repositories.vendor.VendorDispatchBatchRepository;
import com.jangid.forging_process_management_service.repositories.vendor.VendorEntityRepository;
import com.jangid.forging_process_management_service.repositories.vendor.VendorReceiveBatchRepository;
import com.jangid.forging_process_management_service.repositories.vendor.VendorRepository;
import com.jangid.forging_process_management_service.service.workflow.ItemWorkflowService;
import com.jangid.forging_process_management_service.utils.ConvertorUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class VendorReceiveService {

    private final VendorReceiveBatchRepository vendorReceiveBatchRepository;
    private final VendorDispatchBatchRepository vendorDispatchBatchRepository;
    private final VendorRepository vendorRepository;
    private final VendorEntityRepository vendorEntityRepository;
    private final TenantRepository tenantRepository;
    private final VendorReceiveBatchAssembler vendorReceiveBatchAssembler;
    private final ProcessedItemVendorDispatchBatchRepository processedItemVendorDispatchBatchRepository;
    private final ItemWorkflowService itemWorkflowService;
    private final ObjectMapper objectMapper;

    @Autowired
    public VendorReceiveService(
            VendorReceiveBatchRepository vendorReceiveBatchRepository,
            VendorDispatchBatchRepository vendorDispatchBatchRepository,
            VendorRepository vendorRepository,
            VendorEntityRepository vendorEntityRepository,
            TenantRepository tenantRepository,
            VendorReceiveBatchAssembler vendorReceiveBatchAssembler,
            ProcessedItemVendorDispatchBatchRepository processedItemVendorDispatchBatchRepository,
            ItemWorkflowService itemWorkflowService,
            ObjectMapper objectMapper) {
        this.vendorReceiveBatchRepository = vendorReceiveBatchRepository;
        this.vendorDispatchBatchRepository = vendorDispatchBatchRepository;
        this.vendorRepository = vendorRepository;
        this.vendorEntityRepository = vendorEntityRepository;
        this.tenantRepository = tenantRepository;
        this.vendorReceiveBatchAssembler = vendorReceiveBatchAssembler;
        this.processedItemVendorDispatchBatchRepository = processedItemVendorDispatchBatchRepository;
        this.itemWorkflowService = itemWorkflowService;
        this.objectMapper = objectMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public VendorReceiveBatchRepresentation createVendorReceiveBatch(
            VendorReceiveBatchRepresentation representation, Long tenantId) throws Exception {
        log.info("Starting vendor receive batch creation transaction for tenant: {}, batch: {}", 
                 tenantId, representation.getVendorReceiveBatchNumber());
        
        VendorReceiveBatch savedVendorReceiveBatch = null;
        try {

        // Phase 1: Validate tenant and entities
        Tenant tenant = validateTenantAndEntities(representation, tenantId);
        
        // Phase 2: Validate vendor dispatch batch and get associated data
        VendorDispatchBatch vendorDispatchBatch = validateVendorDispatchBatch(representation, tenantId);
        ProcessedItemVendorDispatchBatch processedItemVendorDispatchBatch = vendorDispatchBatch.getProcessedItem();
        
        // Phase 3: Validate quantity/pieces consistency with dispatch batch
        validateReceiveBatchConsistency(representation, processedItemVendorDispatchBatch);
        
            // Phase 4: Create and save receive batch - this generates the required ID for workflow integration
            VendorReceiveBatch batch = createAndSaveReceiveBatch(representation, tenant, vendorDispatchBatch);
            savedVendorReceiveBatch = batch;
            log.info("Successfully persisted vendor receive batch with ID: {}", savedVendorReceiveBatch.getId());
            
            // Phase 5: Update ProcessedItemVendorDispatchBatch totals
            updateProcessedItemVendorDispatchBatchTotals(processedItemVendorDispatchBatch, representation);
            
            // Phase 6: Update workflow step for vendor process - if this fails, entire transaction will rollback
            updateWorkflowForVendorReceiveBatch(processedItemVendorDispatchBatch, representation);
            
            log.info("Successfully completed vendor receive batch creation transaction for ID: {}", batch.getId());
            return vendorReceiveBatchAssembler.dissemble(batch);
            
        } catch (Exception e) {
            log.error("Vendor receive batch creation transaction failed for tenant: {}, batch: {}. " +
                      "All changes will be rolled back. Error: {}", 
                      tenantId, representation.getVendorReceiveBatchNumber(), e.getMessage());
            
            if (savedVendorReceiveBatch != null) {
                log.error("Vendor receive batch with ID {} was persisted but workflow integration failed. " +
                          "Transaction rollback will restore database consistency.", savedVendorReceiveBatch.getId());
            }
            
            // Re-throw to ensure transaction rollback
            throw e;
        }
    }

    /**
     * Phase 1: Validate tenant and all required entities
     */
    private Tenant validateTenantAndEntities(VendorReceiveBatchRepresentation representation, Long tenantId) {
        // Validate tenant
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found with id: " + tenantId));

        // Validate vendor
        Vendor vendor = vendorRepository.findById(representation.getVendor().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found with id: " + representation.getVendor().getId()));

        // Validate billing entity
        VendorEntity billingEntity = vendorEntityRepository.findById(representation.getBillingEntityId())
                .orElseThrow(() -> new ResourceNotFoundException("Billing entity not found with id: " + representation.getBillingEntityId()));

        // Validate shipping entity
        VendorEntity shippingEntity = vendorEntityRepository.findById(representation.getShippingEntityId())
                .orElseThrow(() -> new ResourceNotFoundException("Shipping entity not found with id: " + representation.getShippingEntityId()));

        // Check if batch number already exists
        if (vendorReceiveBatchRepository.existsByVendorReceiveBatchNumberAndTenantIdAndDeletedFalse(
                representation.getVendorReceiveBatchNumber(), tenantId)) {
            throw new ValidationException("Vendor receive batch number already exists: " + 
                    representation.getVendorReceiveBatchNumber());
        }

        return tenant;
    }

    /**
     * Phase 2: Validate vendor dispatch batch and get associated data
     */
    private VendorDispatchBatch validateVendorDispatchBatch(VendorReceiveBatchRepresentation representation, Long tenantId) {
        // Validate vendor dispatch batch
        VendorDispatchBatch vendorDispatchBatch = vendorDispatchBatchRepository
                .findByIdAndTenantIdAndDeletedFalse(representation.getVendorDispatchBatch().getId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor dispatch batch not found with id: " + 
                        representation.getVendorDispatchBatch().getId()));

        // Validate that vendor matches dispatch batch vendor
        if (!representation.getVendor().getId().equals(vendorDispatchBatch.getVendor().getId())) {
            throw new ValidationException("Vendor must match the vendor from the dispatch batch");
        }

        // Validate that processed item exists
        if (vendorDispatchBatch.getProcessedItem() == null) {
            throw new ValidationException("Vendor dispatch batch must have a processed item");
        }

        return vendorDispatchBatch;
    }

    /**
     * Phase 3: Validate quantity/pieces consistency with dispatch batch
     */
    private void validateReceiveBatchConsistency(VendorReceiveBatchRepresentation representation, 
                                                ProcessedItemVendorDispatchBatch processedItemVendorDispatchBatch) {
        
        // Business Rule: Receiving from vendor must always be in pieces, regardless of how it was dispatched
        // This is because vendors process materials and return finished pieces
        if (!representation.getIsInPieces()) {
            throw new ValidationException("Receiving from vendor must always be in pieces (isInPieces=true), regardless of dispatch type");
        }

        // Validate received quantities are provided for pieces-based receive (which is always the case)
        if (representation.getReceivedPiecesCount() == null || representation.getReceivedPiecesCount() <= 0) {
            throw new ValidationException("Received pieces count is required and must be positive");
        }
        if (representation.getRejectedPiecesCount() == null || representation.getRejectedPiecesCount() < 0) {
            throw new ValidationException("Rejected pieces count is required and must be non-negative");
        }
        if (representation.getTenantRejectsCount() == null || representation.getTenantRejectsCount() < 0) {
            throw new ValidationException("Tenant rejects count is required and must be non-negative");
        }
        if (representation.getPiecesEligibleForNextOperation() == null || representation.getPiecesEligibleForNextOperation() < 0) {
            throw new ValidationException("Pieces eligible for next operation is required and must be non-negative");
        }

        // Validate quality check completion fields if qualityCheckCompleted is true
        if (Boolean.TRUE.equals(representation.getQualityCheckCompleted())) {
            validateQualityCompletionFieldsForCreation(representation);
        }

        // Log the dispatch vs receive type mismatch for auditing (this is normal and expected)
        if (!processedItemVendorDispatchBatch.getIsInPieces()) {
            log.info("Vendor dispatch batch {} was dispatched by quantity but receiving in pieces - this is expected for quantity-based dispatch", 
                     processedItemVendorDispatchBatch.getVendorDispatchBatch().getId());
        }
    }

    /**
     * Validate quality completion fields when qualityCheckCompleted is true during creation
     */
    private void validateQualityCompletionFieldsForCreation(VendorReceiveBatchRepresentation representation) {
        // If quality check is completed, final reject counts are required
        if (representation.getFinalVendorRejectsCount() == null || representation.getFinalVendorRejectsCount() < 0) {
            throw new ValidationException("Final vendor rejects count is required and must be non-negative when quality check is completed");
        }
        if (representation.getFinalTenantRejectsCount() == null || representation.getFinalTenantRejectsCount() < 0) {
            throw new ValidationException("Final tenant rejects count is required and must be non-negative when quality check is completed");
        }

        // Validate total rejects don't exceed received pieces
        Integer totalRejects = representation.getFinalVendorRejectsCount() + representation.getFinalTenantRejectsCount();
        if (totalRejects > representation.getReceivedPiecesCount()) {
            throw new ValidationException(String.format(
                "Total final rejects (%d) cannot exceed received pieces count (%d)", 
                totalRejects, representation.getReceivedPiecesCount()
            ));
        }

        // When quality check is completed, qualityCheckRequired should be false
        if (Boolean.TRUE.equals(representation.getQualityCheckRequired())) {
            throw new ValidationException("Quality check required must be false when quality check is already completed");
        }
    }

    /**
     * Phase 4: Create and save receive batch
     */
    private VendorReceiveBatch createAndSaveReceiveBatch(VendorReceiveBatchRepresentation representation, 
                                                        Tenant tenant, 
                                                        VendorDispatchBatch vendorDispatchBatch) {
        
        Vendor vendor = vendorRepository.findById(representation.getVendor().getId()).orElseThrow();
        VendorEntity billingEntity = vendorEntityRepository.findById(representation.getBillingEntityId()).orElseThrow();
        VendorEntity shippingEntity = vendorEntityRepository.findById(representation.getShippingEntityId()).orElseThrow();

        // Determine the correct status based on quality check completion
        VendorReceiveBatch.VendorReceiveBatchStatus batchStatus = VendorReceiveBatch.VendorReceiveBatchStatus.RECEIVED;
        if (Boolean.TRUE.equals(representation.getQualityCheckCompleted())) {
            batchStatus = VendorReceiveBatch.VendorReceiveBatchStatus.QUALITY_CHECK_DONE;
        } else if (Boolean.TRUE.equals(representation.getQualityCheckRequired())) {
            batchStatus = VendorReceiveBatch.VendorReceiveBatchStatus.QUALITY_CHECK_PENDING;
        }

        // Create receive batch
        VendorReceiveBatch.VendorReceiveBatchBuilder batchBuilder = VendorReceiveBatch.builder()
                .vendorReceiveBatchNumber(representation.getVendorReceiveBatchNumber())
                .originalVendorReceiveBatchNumber(representation.getVendorReceiveBatchNumber())
                .vendorReceiveBatchStatus(batchStatus)
                .receivedAt(representation.getReceivedAt() != null ? 
                           ConvertorUtils.convertStringToLocalDateTime(representation.getReceivedAt()) : LocalDateTime.now())
                .isInPieces(representation.getIsInPieces())
                .receivedPiecesCount(representation.getReceivedPiecesCount())
                .rejectedPiecesCount(representation.getRejectedPiecesCount())
                .tenantRejectsCount(representation.getTenantRejectsCount())
                .piecesEligibleForNextOperation(representation.getPiecesEligibleForNextOperation())
                .qualityCheckRequired(representation.getQualityCheckRequired() != null ?
                        representation.getQualityCheckRequired() : false)
                .qualityCheckCompleted(representation.getQualityCheckCompleted() != null ?
                        representation.getQualityCheckCompleted() : false)
                .remarks(representation.getRemarks())
                .packagingType(representation.getPackagingType() != null ?
                               PackagingType.valueOf(representation.getPackagingType()) : null)
                .packagingQuantity(representation.getPackagingQuantity())
                .perPackagingQuantity(representation.getPerPackagingQuantity())
                .tenant(tenant)
                .vendor(vendor)
                .billingEntity(billingEntity)
                .shippingEntity(shippingEntity)
                .vendorDispatchBatch(vendorDispatchBatch)
                .createdAt(LocalDateTime.now())
                .deleted(false);

        // Add quality completion fields if quality check is completed
        if (Boolean.TRUE.equals(representation.getQualityCheckCompleted())) {
            batchBuilder
                .qualityCheckCompletedAt(LocalDateTime.now())
                .finalVendorRejectsCount(representation.getFinalVendorRejectsCount())
                .finalTenantRejectsCount(representation.getFinalTenantRejectsCount())
                .qualityCheckRemarks(representation.getQualityCheckRemarks())
                .isLocked(true); // Lock the batch since quality check is completed
        }

        VendorReceiveBatch batch = batchBuilder.build();

        // Add receive batch to dispatch batch
        vendorDispatchBatch.addVendorReceiveBatch(batch);

        // Save the batch
        return vendorReceiveBatchRepository.save(batch);
    }

    /**
     * Phase 5: Update ProcessedItemVendorDispatchBatch totals
     */
    private void updateProcessedItemVendorDispatchBatchTotals(ProcessedItemVendorDispatchBatch processedItemVendorDispatchBatch, 
                                                             VendorReceiveBatchRepresentation representation) {
        
        // Receiving is always in pieces, regardless of how it was dispatched
        // Update pieces-based totals
        int currentReceivedPieces = processedItemVendorDispatchBatch.getTotalReceivedPiecesCount() != null ? 
                                   processedItemVendorDispatchBatch.getTotalReceivedPiecesCount() : 0;
        int currentRejectedPieces = processedItemVendorDispatchBatch.getTotalRejectedPiecesCount() != null ? 
                                   processedItemVendorDispatchBatch.getTotalRejectedPiecesCount() : 0;
        int currentTenantRejects = processedItemVendorDispatchBatch.getTotalTenantRejectsCount() != null ? 
                                  processedItemVendorDispatchBatch.getTotalTenantRejectsCount() : 0;
        int currentEligiblePieces = processedItemVendorDispatchBatch.getTotalPiecesEligibleForNextOperation() != null ? 
                                   processedItemVendorDispatchBatch.getTotalPiecesEligibleForNextOperation() : 0;

        // Update totals
        processedItemVendorDispatchBatch.setTotalReceivedPiecesCount(
            currentReceivedPieces + representation.getReceivedPiecesCount());
        processedItemVendorDispatchBatch.setTotalRejectedPiecesCount(
            currentRejectedPieces + representation.getRejectedPiecesCount());
        processedItemVendorDispatchBatch.setTotalTenantRejectsCount(
            currentTenantRejects + (representation.getTenantRejectsCount() != null ? representation.getTenantRejectsCount() : 0));
        processedItemVendorDispatchBatch.setTotalPiecesEligibleForNextOperation(
            currentEligiblePieces + representation.getPiecesEligibleForNextOperation());

        // Check if fully received - compare against expected pieces count
        Integer totalExpectedPieces = processedItemVendorDispatchBatch.getTotalExpectedPiecesCount();
        Integer totalReceivedPieces = processedItemVendorDispatchBatch.getTotalReceivedPiecesCount();
        
        if (totalExpectedPieces != null && totalReceivedPieces != null && 
            totalExpectedPieces.equals(totalReceivedPieces)) {
            processedItemVendorDispatchBatch.setFullyReceived(true);
            log.info("ProcessedItemVendorDispatchBatch {} is now fully received: expected={} pieces, received={} pieces", 
                     processedItemVendorDispatchBatch.getId(), totalExpectedPieces, totalReceivedPieces);
        }
        
        // Log dispatch vs receive type for auditing
        if (!processedItemVendorDispatchBatch.getIsInPieces()) {
            log.info("ProcessedItemVendorDispatchBatch {} was dispatched by quantity but receiving in pieces - updated totals: received={}, rejected={}, eligible={}", 
                     processedItemVendorDispatchBatch.getId(), 
                     processedItemVendorDispatchBatch.getTotalReceivedPiecesCount(),
                     processedItemVendorDispatchBatch.getTotalRejectedPiecesCount(),
                     processedItemVendorDispatchBatch.getTotalPiecesEligibleForNextOperation());
        }

        // Save the updated processed item
        processedItemVendorDispatchBatchRepository.save(processedItemVendorDispatchBatch);
        
        log.info("Updated ProcessedItemVendorDispatchBatch totals for ID: {}", processedItemVendorDispatchBatch.getId());
    }

    /*
     * Helper method to retrieve the VENDOR ItemWorkflowStep for a given workflow & processed item
     */
    private ItemWorkflowStep getVendorWorkflowStep(Long workflowId, Long processedItemId) {
        return itemWorkflowService.findItemWorkflowStepByRelatedEntityId(
            workflowId,
            processedItemId,
            WorkflowStep.OperationType.VENDOR);
    }

    /**
     * Helper method to check if this is the first vendor receive batch for the vendor dispatch batch
     */
    private boolean isFirstVendorReceiveBatch(ProcessedItemVendorDispatchBatch processedItemVendorDispatchBatch) {
        try {
            // Get the vendor dispatch batch ID to find the dispatch batch
            Long vendorDispatchBatchId = processedItemVendorDispatchBatch.getVendorDispatchBatch().getId();
            
            // Get the vendor dispatch batch
            VendorDispatchBatch vendorDispatchBatch = vendorDispatchBatchRepository
                    .findById(vendorDispatchBatchId)
                    .orElse(null);
            
            if (vendorDispatchBatch == null) {
                log.warn("VendorDispatchBatch not found with ID: {}", vendorDispatchBatchId);
                return true; // Assume first if can't find dispatch batch
            }
            
            // Count existing non-deleted vendor receive batches
            long existingReceiveBatchCount = vendorDispatchBatch.getVendorReceiveBatches().stream()
                    .filter(batch -> !batch.isDeleted())
                    .count();
            
            // This is the first receive batch if there are no existing non-deleted receive batches
            boolean isFirst = existingReceiveBatchCount == 0;
            
            log.debug("Checking if first vendor receive batch for dispatch batch {}: existing count = {}, isFirst = {}", 
                     vendorDispatchBatchId, existingReceiveBatchCount, isFirst);
            
            return isFirst;
            
        } catch (Exception e) {
            log.error("Error checking if first vendor receive batch: {}", e.getMessage());
            return true; // Assume first if error occurs
        }
    }

    /**
     * Phase 6: Update workflow step for vendor process (similar to updateWorkflowForDailyMachiningBatchUpdate)
     */
    private void updateWorkflowForVendorReceiveBatch(ProcessedItemVendorDispatchBatch processedItemVendorDispatchBatch, 
                                                    VendorReceiveBatchRepresentation representation) throws Exception {
        try {
            // Get workflow information
            Long itemWorkflowId = processedItemVendorDispatchBatch.getItemWorkflowId();
            
            if (itemWorkflowId == null) {
                log.warn("No workflow ID found for ProcessedItemVendorDispatchBatch {}. Skipping workflow update.", 
                         processedItemVendorDispatchBatch.getId());
                return;
            }

            // Get existing vendor operation step using helper method
            ItemWorkflowStep vendorItemWorkflowStep = getVendorWorkflowStep(itemWorkflowId, processedItemVendorDispatchBatch.getId());

            if (vendorItemWorkflowStep == null) {
                log.warn("No vendor operation step found in workflow {}. Skipping workflow update.", itemWorkflowId);
                return;
            }

            // Parse existing batch data using helper method
            List<OperationOutcomeData.BatchOutcome> existingBatchData = itemWorkflowService.extractExistingBatchData(vendorItemWorkflowStep);

            // Check if this is the first vendor receive batch for this dispatch batch
            boolean isFirstReceiveBatch = isFirstVendorReceiveBatch(processedItemVendorDispatchBatch);

            // Find and update the specific batch outcome for this vendor dispatch batch
            for (OperationOutcomeData.BatchOutcome batchOutcome : existingBatchData) {
                if (Objects.equals(batchOutcome.getId(), processedItemVendorDispatchBatch.getId())) {
                    
                    // Get the pieces eligible for next operation from the receive batch
                    // Receiving is always in pieces now
                    int piecesEligibleForNextOperation = representation.getPiecesEligibleForNextOperation();
                    
                    // Update batch outcome with incremental pieces
                    int currentInitialPieces = batchOutcome.getInitialPiecesCount() != null ? batchOutcome.getInitialPiecesCount() : 0;
                    int currentAvailablePieces = batchOutcome.getPiecesAvailableForNext() != null ? batchOutcome.getPiecesAvailableForNext() : 0;
                    
                    // Increment pieces by the pieces eligible for next operation
                    batchOutcome.setInitialPiecesCount(currentInitialPieces + piecesEligibleForNextOperation);
                    batchOutcome.setPiecesAvailableForNext(currentAvailablePieces + piecesEligibleForNextOperation);
                    batchOutcome.setUpdatedAt(LocalDateTime.now());
                    
                    // Set startedAt for the first vendor receive batch
                    if (isFirstReceiveBatch && batchOutcome.getStartedAt() == null) {
                        LocalDateTime receivedAtDateTime = ConvertorUtils.convertStringToLocalDateTime(representation.getReceivedAt());
                        batchOutcome.setStartedAt(receivedAtDateTime);
                        log.info("Set startedAt for first vendor receive batch: ID={}, startedAt={}", 
                                 processedItemVendorDispatchBatch.getId(), receivedAtDateTime);
                    }
                    
                    log.info("Updated vendor batch outcome: ID={}, initialPieces={}, availablePieces={}, increment={}",
                             processedItemVendorDispatchBatch.getId(), batchOutcome.getInitialPiecesCount(), 
                             batchOutcome.getPiecesAvailableForNext(), piecesEligibleForNextOperation);
                    break;
                }
            }

            // Update workflow step with all batch data (preserving existing batches)
            itemWorkflowService.updateWorkflowStepForOperation(vendorItemWorkflowStep, OperationOutcomeData.forVendorOperation(existingBatchData, LocalDateTime.now()));
            
            log.info("Successfully updated workflow {} with vendor receive batch data", itemWorkflowId);

        } catch (Exception e) {
            // Log the error but don't fail the entire operation
            log.error("Failed to update workflow for vendor receive batch on ProcessedItemVendorDispatchBatch ID={}: {}. Continuing with receive batch creation.",
                      processedItemVendorDispatchBatch.getId(), e.getMessage());
            throw e;
        }
    }


    @Transactional(readOnly = true)
    public VendorReceiveBatchRepresentation getVendorReceiveBatch(Long batchId, Long tenantId) {
        VendorReceiveBatch batch = vendorReceiveBatchRepository.findByIdAndTenantIdAndDeletedFalse(batchId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor receive batch not found with id: " + batchId));
        
        return vendorReceiveBatchAssembler.dissemble(batch);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteVendorReceiveBatch(Long batchId, Long tenantId) throws Exception {
        log.info("Starting vendor receive batch deletion transaction for tenant: {}, batch: {}", 
                 tenantId, batchId);
        
        try {
            // Phase 1: Validate and fetch the batch to delete
            VendorReceiveBatch batchToDelete = validateAndFetchBatchForDeletion(batchId, tenantId);
            
            // Phase 2: Revert ProcessedItemVendorDispatchBatch totals
            revertProcessedItemVendorDispatchBatchTotals(batchToDelete);
            
            // Phase 3: Revert workflow step changes - CRITICAL: Workflow operations
            revertWorkflowForVendorReceiveBatch(batchToDelete);
            
            // Phase 4: Soft delete the VendorReceiveBatch
            softDeleteVendorReceiveBatch(batchToDelete);
            log.info("Successfully persisted vendor receive batch deletion with ID: {}", batchId);
            
            log.info("Successfully completed vendor receive batch deletion transaction for ID: {}", batchId);
            
        } catch (Exception e) {
            log.error("Vendor receive batch deletion transaction failed for tenant: {}, batch: {}. " +
                      "All changes will be rolled back. Error: {}", 
                      tenantId, batchId, e.getMessage());
            
            log.error("Vendor receive batch deletion failed - workflow updates, processed item totals reversals, and entity deletions will be rolled back.");
            
            // Re-throw to ensure transaction rollback
            throw e;
        }
    }

    /**
     * Phase 1: Validate and fetch the batch to delete
     */
    private VendorReceiveBatch validateAndFetchBatchForDeletion(Long batchId, Long tenantId) {
        // Find the batch to delete
        VendorReceiveBatch batchToDelete = vendorReceiveBatchRepository.findByIdAndTenantIdAndDeletedFalse(batchId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor receive batch not found with id: " + batchId));

        // Validate that there are no newer VendorReceiveBatches that need to be deleted first
        validateNoDependentReceiveBatches(batchToDelete);
        
        return batchToDelete;
    }

    /**
     * Validates that there are no newer VendorReceiveBatches associated with the same VendorDispatchBatch
     * that are still active (not deleted). VendorReceiveBatches must be deleted in reverse chronological order.
     */
    private void validateNoDependentReceiveBatches(VendorReceiveBatch batchToDelete) {
        VendorDispatchBatch vendorDispatchBatch = batchToDelete.getVendorDispatchBatch();
        
        if (vendorDispatchBatch == null) {
            log.warn("No vendor dispatch batch found for receive batch {}. Skipping dependent batch validation.", 
                     batchToDelete.getId());
            return;
        }

        // Find all non-deleted receive batches for the same dispatch batch
        List<VendorReceiveBatch> allReceiveBatches = vendorDispatchBatch.getVendorReceiveBatches().stream()
                .filter(batch -> !batch.isDeleted()) // Only non-deleted batches
                .toList();

        // Check if there are any receive batches created after the one being deleted
        List<VendorReceiveBatch> newerBatches = allReceiveBatches.stream()
                .filter(batch -> !batch.getId().equals(batchToDelete.getId())) // Exclude the batch being deleted
                .filter(batch -> batch.getCreatedAt().isAfter(batchToDelete.getCreatedAt())) // Find newer batches
                .toList();

        if (!newerBatches.isEmpty()) {
            // Build error message with details of dependent batches
            String newerBatchNumbers = newerBatches.stream()
                    .map(VendorReceiveBatch::getVendorReceiveBatchNumber)
                    .collect(Collectors.joining(", "));
            
            String errorMessage = String.format(
                "Cannot delete vendor receive batch '%s' because there are newer receive batches that must be deleted first: [%s]. " +
                "Vendor receive batches must be deleted in reverse chronological order (newest first).",
                batchToDelete.getVendorReceiveBatchNumber(), 
                newerBatchNumbers
            );
            
            log.error("Validation failed for vendor receive batch deletion: {}", errorMessage);
            throw new ValidationException(errorMessage);
        }

        log.info("Validation passed: No newer receive batches found for vendor dispatch batch {}. " +
                 "Batch {} can be safely deleted.", 
                 vendorDispatchBatch.getVendorDispatchBatchNumber(),
                 batchToDelete.getVendorReceiveBatchNumber());
    }

    /**
     * Phase 2: Revert ProcessedItemVendorDispatchBatch totals
     */
    private void revertProcessedItemVendorDispatchBatchTotals(VendorReceiveBatch batchToDelete) {
        VendorDispatchBatch vendorDispatchBatch = batchToDelete.getVendorDispatchBatch();
        if (vendorDispatchBatch == null || vendorDispatchBatch.getProcessedItem() == null) {
            log.warn("No processed item found for vendor receive batch {}. Skipping totals reversion.", batchToDelete.getId());
            return;
        }

        ProcessedItemVendorDispatchBatch processedItemVendorDispatchBatch = vendorDispatchBatch.getProcessedItem();

        // Revert the totals by subtracting the values from the batch being deleted
        int currentReceivedPieces = processedItemVendorDispatchBatch.getTotalReceivedPiecesCount() != null ? 
                                   processedItemVendorDispatchBatch.getTotalReceivedPiecesCount() : 0;
        int currentRejectedPieces = processedItemVendorDispatchBatch.getTotalRejectedPiecesCount() != null ? 
                                   processedItemVendorDispatchBatch.getTotalRejectedPiecesCount() : 0;
        int currentTenantRejects = processedItemVendorDispatchBatch.getTotalTenantRejectsCount() != null ? 
                                  processedItemVendorDispatchBatch.getTotalTenantRejectsCount() : 0;
        int currentEligiblePieces = processedItemVendorDispatchBatch.getTotalPiecesEligibleForNextOperation() != null ? 
                                   processedItemVendorDispatchBatch.getTotalPiecesEligibleForNextOperation() : 0;

        // Calculate new totals by subtracting the deleted batch values
        int receivedPiecesToSubtract = batchToDelete.getReceivedPiecesCount() != null ? batchToDelete.getReceivedPiecesCount() : 0;
        int rejectedPiecesToSubtract = batchToDelete.getRejectedPiecesCount() != null ? batchToDelete.getRejectedPiecesCount() : 0;
        int tenantRejectsToSubtract = batchToDelete.getTenantRejectsCount() != null ? batchToDelete.getTenantRejectsCount() : 0;
        int eligiblePiecesToSubtract = batchToDelete.getPiecesEligibleForNextOperation() != null ? batchToDelete.getPiecesEligibleForNextOperation() : 0;

        // Update totals
        processedItemVendorDispatchBatch.setTotalReceivedPiecesCount(
            Math.max(0, currentReceivedPieces - receivedPiecesToSubtract));
        processedItemVendorDispatchBatch.setTotalRejectedPiecesCount(
            Math.max(0, currentRejectedPieces - rejectedPiecesToSubtract));
        processedItemVendorDispatchBatch.setTotalTenantRejectsCount(
            Math.max(0, currentTenantRejects - tenantRejectsToSubtract));
        processedItemVendorDispatchBatch.setTotalPiecesEligibleForNextOperation(
            Math.max(0, currentEligiblePieces - eligiblePiecesToSubtract));

        // Recalculate fully received status
        Integer totalExpectedPieces = processedItemVendorDispatchBatch.getTotalExpectedPiecesCount();
        Integer totalReceivedPieces = processedItemVendorDispatchBatch.getTotalReceivedPiecesCount();
        
        boolean wasFullyReceived = processedItemVendorDispatchBatch.getFullyReceived() != null ? 
                                  processedItemVendorDispatchBatch.getFullyReceived() : false;
        
        if (totalExpectedPieces != null && totalReceivedPieces != null) {
            boolean isNowFullyReceived = totalExpectedPieces.equals(totalReceivedPieces);
            processedItemVendorDispatchBatch.setFullyReceived(isNowFullyReceived);
            
            if (wasFullyReceived && !isNowFullyReceived) {
                log.info("ProcessedItemVendorDispatchBatch {} is no longer fully received after deletion: expected={}, received={}", 
                         processedItemVendorDispatchBatch.getId(), totalExpectedPieces, totalReceivedPieces);
            }
        }

        // Save the updated processed item
        processedItemVendorDispatchBatchRepository.save(processedItemVendorDispatchBatch);
        
        log.info("Reverted ProcessedItemVendorDispatchBatch totals for ID: {}", processedItemVendorDispatchBatch.getId());
    }

    /**
     * Phase 3: Revert workflow step changes
     */
    private void revertWorkflowForVendorReceiveBatch(VendorReceiveBatch batchToDelete) throws Exception {
        try {
            VendorDispatchBatch vendorDispatchBatch = batchToDelete.getVendorDispatchBatch();
            if (vendorDispatchBatch == null || vendorDispatchBatch.getProcessedItem() == null) {
                log.warn("No processed item found for vendor receive batch {}. Skipping workflow reversion.", batchToDelete.getId());
                return;
            }

            ProcessedItemVendorDispatchBatch processedItemVendorDispatchBatch = vendorDispatchBatch.getProcessedItem();
            Long itemWorkflowId = processedItemVendorDispatchBatch.getItemWorkflowId();
            
            if (itemWorkflowId == null) {
                log.warn("No workflow ID found for ProcessedItemVendorDispatchBatch {}. Skipping workflow reversion.", 
                         processedItemVendorDispatchBatch.getId());
                return;
            }

            // Get existing vendor operation step using helper method
            ItemWorkflowStep vendorItemWorkflowStep = getVendorWorkflowStep(itemWorkflowId, processedItemVendorDispatchBatch.getId());

            if (vendorItemWorkflowStep == null) {
                log.error("No vendor operation step found in workflow {}.", itemWorkflowId);
                throw new RuntimeException("No vendor operation step found in workflow " + itemWorkflowId);
            }

            // Parse existing batch data using helper method
            List<OperationOutcomeData.BatchOutcome> existingBatchData = itemWorkflowService.extractExistingBatchData(vendorItemWorkflowStep);

            // Find and revert the specific batch outcome for this vendor dispatch batch
            for (OperationOutcomeData.BatchOutcome batchOutcome : existingBatchData) {
                if (Objects.equals(batchOutcome.getId(), processedItemVendorDispatchBatch.getId())) {
                    
                    // Get the pieces eligible for next operation that we need to subtract
                    int piecesToSubtract = batchToDelete.getPiecesEligibleForNextOperation() != null ? 
                                          batchToDelete.getPiecesEligibleForNextOperation() : 0;
                    
                    // Revert batch outcome by subtracting pieces
                    int currentInitialPieces = batchOutcome.getInitialPiecesCount() != null ? batchOutcome.getInitialPiecesCount() : 0;
                    int currentAvailablePieces = batchOutcome.getPiecesAvailableForNext() != null ? batchOutcome.getPiecesAvailableForNext() : 0;
                    
                    // Subtract pieces from the batch outcome
                    batchOutcome.setInitialPiecesCount(Math.max(0, currentInitialPieces - piecesToSubtract));
                    batchOutcome.setPiecesAvailableForNext(Math.max(0, currentAvailablePieces - piecesToSubtract));
                    batchOutcome.setUpdatedAt(LocalDateTime.now());
                    
                    log.info("Reverted vendor batch outcome: ID={}, initialPieces={}, availablePieces={}, subtracted={}",
                             processedItemVendorDispatchBatch.getId(), batchOutcome.getInitialPiecesCount(), 
                             batchOutcome.getPiecesAvailableForNext(), piecesToSubtract);
                    break;
                }
            }

            // Update workflow step with reverted batch data
            itemWorkflowService.updateWorkflowStepForOperation(vendorItemWorkflowStep, OperationOutcomeData.forVendorOperation(existingBatchData, LocalDateTime.now()));
            
            log.info("Successfully reverted workflow {} with vendor receive batch deletion", itemWorkflowId);

        } catch (Exception e) {
            log.error("Failed to revert workflow for vendor receive batch deletion ID={}: {}. Continuing with batch deletion.",
                      batchToDelete.getId(), e.getMessage());
            throw e;
        }
    }

    /**
     * Phase 4: Soft delete the VendorReceiveBatch
     */
    private void softDeleteVendorReceiveBatch(VendorReceiveBatch batchToDelete) {
        batchToDelete.setDeleted(true);
        batchToDelete.setDeletedAt(LocalDateTime.now());
        
        vendorReceiveBatchRepository.save(batchToDelete);
        
        log.info("Soft deleted VendorReceiveBatch with ID: {}", batchToDelete.getId());
    }

    /**
     * Get all vendor receive batches for a specific vendor dispatch batch
     */
    @Transactional(readOnly = true)
    public List<VendorReceiveBatchRepresentation> getVendorReceiveBatchesForDispatchBatch(Long dispatchBatchId, Long tenantId) {
        log.info("Fetching vendor receive batches for dispatch batch: {} and tenant: {}", dispatchBatchId, tenantId);
        
        // Validate that the vendor dispatch batch exists
        VendorDispatchBatch vendorDispatchBatch = vendorDispatchBatchRepository
                .findByIdAndTenantIdAndDeletedFalse(dispatchBatchId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor dispatch batch not found with id: " + dispatchBatchId));
        
        // Get all non-deleted vendor receive batches for this dispatch batch
        List<VendorReceiveBatch> receiveBatches = vendorDispatchBatch.getVendorReceiveBatches().stream()
                .filter(batch -> !batch.isDeleted())
                .toList();
        
        // Convert to representations
        List<VendorReceiveBatchRepresentation> representations = receiveBatches.stream()
                .map(vendorReceiveBatchAssembler::dissemble)
                .collect(Collectors.toList());
        
        log.info("Found {} vendor receive batches for dispatch batch: {}", representations.size(), dispatchBatchId);
        return representations;
    }

    /**
     * Complete quality check for a vendor receive batch
     */
    public VendorReceiveBatchRepresentation completeQualityCheck(
            Long batchId, 
            VendorQualityCheckCompletionRepresentation completionRequest, 
            Long tenantId) {
        
        log.info("Completing quality check for vendor receive batch: {} for tenant: {}", batchId, tenantId);

        // Phase 1: Validate and fetch batch
        VendorReceiveBatch batch = validateAndFetchBatchForQualityCheck(batchId, tenantId);
        
        // Phase 2: Validate quality check completion request
        validateQualityCheckCompletionRequest(completionRequest, batch);
        
        // Phase 3: Complete quality check
        batch.completeQualityCheck(
            completionRequest.getFinalVendorRejectsCount(),
            completionRequest.getFinalTenantRejectsCount(),
            completionRequest.getQualityCheckRemarks()
        );
        
        // Phase 4: Save the batch
        VendorReceiveBatch savedBatch = vendorReceiveBatchRepository.save(batch);
        
        log.info("Successfully completed quality check for vendor receive batch: {}", batchId);
        return vendorReceiveBatchAssembler.dissemble(savedBatch);
    }

    /**
     * Validate and fetch batch for quality check completion
     */
    private VendorReceiveBatch validateAndFetchBatchForQualityCheck(Long batchId, Long tenantId) {
        VendorReceiveBatch batch = vendorReceiveBatchRepository
                .findByIdAndTenantIdAndDeletedFalse(batchId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor receive batch not found with id: " + batchId));
        
        if (!batch.canBeModified()) {
            throw new ValidationException("Vendor receive batch is locked and cannot be modified");
        }
        
        return batch;
    }

    /**
     * Validate quality check completion request
     */
    private void validateQualityCheckCompletionRequest(VendorQualityCheckCompletionRepresentation request, VendorReceiveBatch batch) {
        // Validate total rejects don't exceed received pieces
        Integer totalRejects = request.getTotalFinalRejectsCount();
        Integer receivedPieces = batch.getReceivedPiecesCount();
        
        if (receivedPieces != null && totalRejects > receivedPieces) {
            throw new ValidationException(String.format(
                "Total final rejects (%d) cannot exceed received pieces count (%d)", 
                totalRejects, receivedPieces
            ));
        }
        
        // Validate individual reject counts
        if (request.getFinalVendorRejectsCount() < 0 || request.getFinalTenantRejectsCount() < 0) {
            throw new ValidationException("Reject counts must be non-negative");
        }
    }



    /**
     * Get vendor receive batches pending quality check for a tenant
     */
    public List<VendorReceiveBatchRepresentation> getVendorReceiveBatchesPendingQualityCheck(Long tenantId) {
        log.info("Fetching vendor receive batches pending quality check for tenant: {}", tenantId);
        
        List<VendorReceiveBatch> pendingBatches = vendorReceiveBatchRepository
                .findByTenantIdAndQualityCheckRequiredTrueAndQualityCheckCompletedFalseAndDeletedFalse(tenantId);
        
        return pendingBatches.stream()
                .map(vendorReceiveBatchAssembler::dissemble)
                .collect(Collectors.toList());
    }

}