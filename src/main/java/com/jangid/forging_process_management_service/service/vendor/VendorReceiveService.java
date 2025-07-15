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
import com.jangid.forging_process_management_service.entities.workflow.ItemWorkflow;
import com.jangid.forging_process_management_service.entities.workflow.ItemWorkflowStep;
import com.jangid.forging_process_management_service.entities.workflow.WorkflowStep;
import com.jangid.forging_process_management_service.entitiesRepresentation.vendor.VendorReceiveBatchRepresentation;
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
import java.util.ArrayList;
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

    public VendorReceiveBatchRepresentation createVendorReceiveBatch(
            VendorReceiveBatchRepresentation representation, Long tenantId) {
        
        log.info("Creating vendor receive batch: {} for tenant: {}", 
                representation.getVendorReceiveBatchNumber(), tenantId);

        // Phase 1: Validate tenant and entities
        Tenant tenant = validateTenantAndEntities(representation, tenantId);
        
        // Phase 2: Validate vendor dispatch batch and get associated data
        VendorDispatchBatch vendorDispatchBatch = validateVendorDispatchBatch(representation, tenantId);
        ProcessedItemVendorDispatchBatch processedItemVendorDispatchBatch = vendorDispatchBatch.getProcessedItem();
        
        // Phase 3: Validate quantity/pieces consistency with dispatch batch
        validateReceiveBatchConsistency(representation, processedItemVendorDispatchBatch);
        
        // Phase 4: Create and save receive batch
        VendorReceiveBatch batch = createAndSaveReceiveBatch(representation, tenant, vendorDispatchBatch);
        
        // Phase 5: Update ProcessedItemVendorDispatchBatch totals
        updateProcessedItemVendorDispatchBatchTotals(processedItemVendorDispatchBatch, representation);
        
        // Phase 6: Update workflow step for vendor process
        updateWorkflowForVendorReceiveBatch(processedItemVendorDispatchBatch, representation);
        
        log.info("Successfully created vendor receive batch with ID: {}", batch.getId());
        return vendorReceiveBatchAssembler.dissemble(batch);
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
        
        // Validate isInPieces matches the dispatch batch
        if (!representation.getIsInPieces().equals(processedItemVendorDispatchBatch.getIsInPieces())) {
            throw new ValidationException("Receive batch isInPieces must match dispatch batch isInPieces: " + 
                    processedItemVendorDispatchBatch.getIsInPieces());
        }

        // Validate received quantities are provided based on dispatch batch type
        if (processedItemVendorDispatchBatch.getIsInPieces()) {
            // Dispatch batch is in pieces - validate pieces are provided
            if (representation.getReceivedPiecesCount() == null || representation.getReceivedPiecesCount() <= 0) {
                throw new ValidationException("Received pieces count is required and must be positive for pieces-based dispatch batch");
            }
            if (representation.getRejectedPiecesCount() == null || representation.getRejectedPiecesCount() < 0) {
                throw new ValidationException("Rejected pieces count is required and must be non-negative for pieces-based dispatch batch");
            }
            if (representation.getTenantRejectsCount() == null || representation.getTenantRejectsCount() < 0) {
                throw new ValidationException("Tenant rejects count is required and must be non-negative for pieces-based dispatch batch");
            }
            if (representation.getPiecesEligibleForNextOperation() == null || representation.getPiecesEligibleForNextOperation() < 0) {
                throw new ValidationException("Pieces eligible for next operation is required and must be non-negative for pieces-based dispatch batch");
            }
        } else {
            // Even for quantity-based dispatch, receiving is always in pieces
            // This case should not occur anymore since receiving is always in pieces
            throw new ValidationException("Receiving from vendor must always be in pieces, regardless of dispatch type");
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

        // Create receive batch
        VendorReceiveBatch batch = VendorReceiveBatch.builder()
                .vendorReceiveBatchNumber(representation.getVendorReceiveBatchNumber())
                .originalVendorReceiveBatchNumber(representation.getVendorReceiveBatchNumber())
                .vendorReceiveBatchStatus(VendorReceiveBatch.VendorReceiveBatchStatus.RECEIVED)
                .receivedAt(representation.getReceivedAt() != null ? 
                           ConvertorUtils.convertStringToLocalDateTime(representation.getReceivedAt()) : LocalDateTime.now())
                .isInPieces(representation.getIsInPieces())
                .receivedPiecesCount(representation.getReceivedPiecesCount())
                .rejectedPiecesCount(representation.getRejectedPiecesCount())
                .tenantRejectsCount(representation.getTenantRejectsCount())
                .piecesEligibleForNextOperation(representation.getPiecesEligibleForNextOperation())
                .qualityCheckRequired(representation.getQualityCheckRequired() != null ?
                        representation.getQualityCheckRequired() : false)
                .qualityCheckCompleted(false) // Always set to false on creation
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
                .deleted(false)
                .build();

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
        
        if (processedItemVendorDispatchBatch.getIsInPieces()) {
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

            // Check if fully received
            Integer totalExpectedPieces = processedItemVendorDispatchBatch.getTotalExpectedPiecesCount();
            Integer totalReceivedPieces = processedItemVendorDispatchBatch.getTotalReceivedPiecesCount();
            
            if (totalExpectedPieces != null && totalReceivedPieces != null && 
                totalExpectedPieces.equals(totalReceivedPieces)) {
                processedItemVendorDispatchBatch.setFullyReceived(true);
                log.info("ProcessedItemVendorDispatchBatch {} is now fully received (pieces): expected={}, received={}", 
                         processedItemVendorDispatchBatch.getId(), totalExpectedPieces, totalReceivedPieces);
            }
        } else {
            // This case should not occur since receiving is always in pieces now
            throw new IllegalStateException("Receiving from vendor must always be in pieces");
        }

        // Save the updated processed item
        processedItemVendorDispatchBatchRepository.save(processedItemVendorDispatchBatch);
        
        log.info("Updated ProcessedItemVendorDispatchBatch totals for ID: {}", processedItemVendorDispatchBatch.getId());
    }

    /**
     * Phase 6: Update workflow step for vendor process (similar to updateWorkflowForDailyMachiningBatchUpdate)
     */
    private void updateWorkflowForVendorReceiveBatch(ProcessedItemVendorDispatchBatch processedItemVendorDispatchBatch, 
                                                    VendorReceiveBatchRepresentation representation) {
        try {
            // Get workflow information
            Long itemWorkflowId = processedItemVendorDispatchBatch.getItemWorkflowId();
            
            if (itemWorkflowId == null) {
                log.warn("No workflow ID found for ProcessedItemVendorDispatchBatch {}. Skipping workflow update.", 
                         processedItemVendorDispatchBatch.getId());
                return;
            }

            // Get the workflow
            ItemWorkflow workflow = itemWorkflowService.getItemWorkflowById(itemWorkflowId);
            
            // Get existing vendor operation step
            ItemWorkflowStep vendorItemWorkflowStep = itemWorkflowService.getWorkflowStepByOperation(
                itemWorkflowId, WorkflowStep.OperationType.VENDOR);

            if (vendorItemWorkflowStep == null) {
                log.warn("No vendor operation step found in workflow {}. Skipping workflow update.", itemWorkflowId);
                return;
            }

            List<OperationOutcomeData.BatchOutcome> existingBatchData = new ArrayList<>();
            
            // Parse existing batch data if it exists
            if (vendorItemWorkflowStep.getOperationOutcomeData() != null && 
                !vendorItemWorkflowStep.getOperationOutcomeData().trim().isEmpty()) {
                
                try {
                    OperationOutcomeData existingOutcomeData = objectMapper.readValue(
                        vendorItemWorkflowStep.getOperationOutcomeData(), OperationOutcomeData.class);
                    
                    if (existingOutcomeData.getBatchData() != null) {
                        existingBatchData.addAll(existingOutcomeData.getBatchData());
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse existing workflow outcome data for vendor step in workflow {}: {}", 
                             itemWorkflowId, e.getMessage());
                }
            }

            // Find and update the specific batch outcome for this vendor dispatch batch
            boolean batchFound = false;
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
                    
                    batchFound = true;
                    
                    log.info("Updated vendor batch outcome: ID={}, initialPieces={}, availablePieces={}, increment={}", 
                             processedItemVendorDispatchBatch.getId(), batchOutcome.getInitialPiecesCount(), 
                             batchOutcome.getPiecesAvailableForNext(), piecesEligibleForNextOperation);
                    break;
                }
            }

            // If batch outcome doesn't exist, create a new one (this shouldn't normally happen)
            if (!batchFound) {
                log.warn("Batch outcome for vendor dispatch batch {} not found in existing data. Creating new batch outcome.", 
                         processedItemVendorDispatchBatch.getId());
                
                // Receiving is always in pieces now
                int piecesEligibleForNextOperation = representation.getPiecesEligibleForNextOperation();
                
                OperationOutcomeData.BatchOutcome newBatchOutcome = OperationOutcomeData.BatchOutcome.builder()
                    .id(processedItemVendorDispatchBatch.getId())
                    .initialPiecesCount(piecesEligibleForNextOperation)
                    .piecesAvailableForNext(piecesEligibleForNextOperation)
                    .createdAt(processedItemVendorDispatchBatch.getCreatedAt())
                    .updatedAt(LocalDateTime.now())
                    .deletedAt(processedItemVendorDispatchBatch.getDeletedAt())
                    .deleted(processedItemVendorDispatchBatch.getDeleted())
                    .build();
                existingBatchData.add(newBatchOutcome);
            }

            // Update workflow step with all batch data (preserving existing batches)
            itemWorkflowService.updateWorkflowStepForOperation(
                itemWorkflowId,
                WorkflowStep.OperationType.VENDOR,
                OperationOutcomeData.forVendorOperation(existingBatchData, LocalDateTime.now())
            );
            
            log.info("Successfully updated workflow {} with vendor receive batch data", itemWorkflowId);

        } catch (Exception e) {
            // Log the error but don't fail the entire operation
            log.error("Failed to update workflow for vendor receive batch on ProcessedItemVendorDispatchBatch ID={}: {}. Continuing with receive batch creation.",
                      processedItemVendorDispatchBatch.getId(), e.getMessage());
        }
    }


    @Transactional(readOnly = true)
    public VendorReceiveBatchRepresentation getVendorReceiveBatch(Long batchId, Long tenantId) {
        VendorReceiveBatch batch = vendorReceiveBatchRepository.findByIdAndTenantIdAndDeletedFalse(batchId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor receive batch not found with id: " + batchId));
        
        return vendorReceiveBatchAssembler.dissemble(batch);
    }

    public void deleteVendorReceiveBatch(Long batchId, Long tenantId) {
        log.info("Deleting vendor receive batch: {} for tenant: {}", batchId, tenantId);

        // Phase 1: Validate and fetch the batch to delete
        VendorReceiveBatch batchToDelete = validateAndFetchBatchForDeletion(batchId, tenantId);
        
        // Phase 2: Revert ProcessedItemVendorDispatchBatch totals
        revertProcessedItemVendorDispatchBatchTotals(batchToDelete);
        
        // Phase 3: Revert workflow step changes
        revertWorkflowForVendorReceiveBatch(batchToDelete);
        
        // Phase 4: Soft delete the VendorReceiveBatch
        softDeleteVendorReceiveBatch(batchToDelete);
        
        log.info("Successfully deleted vendor receive batch with ID: {}", batchId);
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
                .collect(Collectors.toList());

        // Check if there are any receive batches created after the one being deleted
        List<VendorReceiveBatch> newerBatches = allReceiveBatches.stream()
                .filter(batch -> !batch.getId().equals(batchToDelete.getId())) // Exclude the batch being deleted
                .filter(batch -> batch.getCreatedAt().isAfter(batchToDelete.getCreatedAt())) // Find newer batches
                .collect(Collectors.toList());

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
    private void revertWorkflowForVendorReceiveBatch(VendorReceiveBatch batchToDelete) {
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

            // Get the workflow
            ItemWorkflow workflow = itemWorkflowService.getItemWorkflowById(itemWorkflowId);
            
            // Get existing vendor operation step
            ItemWorkflowStep vendorItemWorkflowStep = itemWorkflowService.getWorkflowStepByOperation(
                itemWorkflowId, WorkflowStep.OperationType.VENDOR);

            if (vendorItemWorkflowStep == null) {
                log.warn("No vendor operation step found in workflow {}. Skipping workflow reversion.", itemWorkflowId);
                return;
            }

            List<OperationOutcomeData.BatchOutcome> existingBatchData = new ArrayList<>();
            
            // Parse existing batch data if it exists
            if (vendorItemWorkflowStep.getOperationOutcomeData() != null && 
                !vendorItemWorkflowStep.getOperationOutcomeData().trim().isEmpty()) {
                
                try {
                    OperationOutcomeData existingOutcomeData = objectMapper.readValue(
                        vendorItemWorkflowStep.getOperationOutcomeData(), OperationOutcomeData.class);
                    
                    if (existingOutcomeData.getBatchData() != null) {
                        existingBatchData.addAll(existingOutcomeData.getBatchData());
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse existing workflow outcome data for vendor step in workflow {}: {}", 
                             itemWorkflowId, e.getMessage());
                }
            }

            // Find and revert the specific batch outcome for this vendor dispatch batch
            boolean batchFound = false;
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
                    
                    batchFound = true;
                    
                    log.info("Reverted vendor batch outcome: ID={}, initialPieces={}, availablePieces={}, subtracted={}", 
                             processedItemVendorDispatchBatch.getId(), batchOutcome.getInitialPiecesCount(), 
                             batchOutcome.getPiecesAvailableForNext(), piecesToSubtract);
                    break;
                }
            }

            if (!batchFound) {
                log.warn("Batch outcome for vendor dispatch batch {} not found in existing data. Cannot revert workflow changes.", 
                         processedItemVendorDispatchBatch.getId());
                return;
            }

            // Update workflow step with reverted batch data
            itemWorkflowService.updateWorkflowStepForOperation(
                itemWorkflowId,
                WorkflowStep.OperationType.VENDOR,
                OperationOutcomeData.forVendorOperation(existingBatchData, LocalDateTime.now())
            );
            
            log.info("Successfully reverted workflow {} with vendor receive batch deletion", itemWorkflowId);

        } catch (Exception e) {
            // Log the error but don't fail the entire operation
            log.error("Failed to revert workflow for vendor receive batch deletion ID={}: {}. Continuing with batch deletion.",
                      batchToDelete.getId(), e.getMessage());
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
                .collect(Collectors.toList());
        
        // Convert to representations
        List<VendorReceiveBatchRepresentation> representations = receiveBatches.stream()
                .map(vendorReceiveBatchAssembler::dissemble)
                .collect(Collectors.toList());
        
        log.info("Found {} vendor receive batches for dispatch batch: {}", representations.size(), dispatchBatchId);
        return representations;
    }

}