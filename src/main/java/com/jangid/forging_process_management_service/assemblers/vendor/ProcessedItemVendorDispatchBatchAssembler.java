package com.jangid.forging_process_management_service.assemblers.vendor;

import com.jangid.forging_process_management_service.assemblers.product.ItemAssembler;
import com.jangid.forging_process_management_service.entities.product.ItemStatus;
import com.jangid.forging_process_management_service.entities.vendor.ProcessedItemVendorDispatchBatch;
import com.jangid.forging_process_management_service.entitiesRepresentation.vendor.ProcessedItemVendorDispatchBatchRepresentation;
import com.jangid.forging_process_management_service.service.product.ItemService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ProcessedItemVendorDispatchBatchAssembler {

    @Autowired
    private ItemAssembler itemAssembler;

    @Autowired
    @Lazy
    private VendorDispatchBatchAssembler vendorDispatchBatchAssembler;

    @Autowired
    private VendorDispatchHeatAssembler vendorDispatchHeatAssembler;

    @Autowired
    private ItemService itemService;

    /**
     * Convert ProcessedItemVendorDispatchBatch entity to representation
     */
    public ProcessedItemVendorDispatchBatchRepresentation dissemble(ProcessedItemVendorDispatchBatch entity) {
        return dissemble(entity, false);
    }

    /**
     * Convert ProcessedItemVendorDispatchBatch entity to representation with option to include full vendor dispatch batch
     */
    public ProcessedItemVendorDispatchBatchRepresentation dissemble(ProcessedItemVendorDispatchBatch entity, boolean includeVendorDispatchBatch) {
        if (entity == null) {
            return null;
        }

        return ProcessedItemVendorDispatchBatchRepresentation.builder()
                .id(entity.getId())
                .vendorDispatchBatchId(entity.getVendorDispatchBatch() != null ? entity.getVendorDispatchBatch().getId() : null)
                .vendorDispatchBatch(includeVendorDispatchBatch && entity.getVendorDispatchBatch() != null ? 
                    vendorDispatchBatchAssembler.dissemble(entity.getVendorDispatchBatch(), false) : null)
                .item(entity.getItem() != null ? itemAssembler.dissembleBasic(entity.getItem()) : null) // Use dissembleBasic to exclude itemWorkflows
                .itemStatus(entity.getItemStatus() != null ? entity.getItemStatus().name() : null)
                .workflowIdentifier(entity.getWorkflowIdentifier())
                .itemWorkflowId(entity.getItemWorkflowId())
                .previousOperationProcessedItemId(entity.getPreviousOperationProcessedItemId())
                .vendorDispatchHeats(entity.getVendorDispatchHeats() != null ? 
                    entity.getVendorDispatchHeats().stream()
                        .map(heat -> vendorDispatchHeatAssembler.dissemble(heat, false))
                        .collect(Collectors.toList()) : null)
                .isInPieces(entity.getIsInPieces())
                .dispatchedPiecesCount(entity.getDispatchedPiecesCount())
                .dispatchedQuantity(entity.getDispatchedQuantity())
                .totalExpectedPiecesCount(entity.getTotalExpectedPiecesCount())
                .totalReceivedPiecesCount(entity.getTotalReceivedPiecesCount())
                .totalRejectedPiecesCount(entity.getTotalRejectedPiecesCount())
                .totalTenantRejectsCount(entity.getTotalTenantRejectsCount())
                .totalPiecesEligibleForNextOperation(entity.getTotalPiecesEligibleForNextOperation())
                .fullyReceived(entity.getFullyReceived())
                .createdAt(entity.getCreatedAt() != null ? String.valueOf(entity.getCreatedAt()) : null)
                .updatedAt(entity.getUpdatedAt() != null ? String.valueOf(entity.getUpdatedAt()) : null)
                .deletedAt(entity.getDeletedAt() != null ? String.valueOf(entity.getDeletedAt()) : null)
                .deleted(entity.getDeleted())
                .build();
    }

    /**
     * Convert representation to ProcessedItemVendorDispatchBatch entity for creation
     */
    public ProcessedItemVendorDispatchBatch assemble(ProcessedItemVendorDispatchBatchRepresentation representation) {
        if (representation == null) {
            return null;
        }

        ItemStatus itemStatus = null;
        if (representation.getItemStatus() != null) {
            itemStatus = ItemStatus.valueOf(representation.getItemStatus());
        }

        return ProcessedItemVendorDispatchBatch.builder()
                .id(representation.getId())
                .itemStatus(itemStatus)
                .workflowIdentifier(representation.getWorkflowIdentifier())
                .itemWorkflowId(representation.getItemWorkflowId())
                .previousOperationProcessedItemId(representation.getPreviousOperationProcessedItemId())
                .isInPieces(representation.getIsInPieces() != null ? representation.getIsInPieces() : false)
                .dispatchedPiecesCount(representation.getDispatchedPiecesCount())
                .dispatchedQuantity(representation.getDispatchedQuantity())
                .totalExpectedPiecesCount(representation.getDispatchedPiecesCount())
                .totalReceivedPiecesCount(0)
                .totalRejectedPiecesCount(0)
                .totalTenantRejectsCount(0)
                .totalPiecesEligibleForNextOperation(0)
                .fullyReceived(false)
                .deletedAt(representation.getDeletedAt() != null ? LocalDateTime.parse(representation.getDeletedAt()) : null)
                .deleted(representation.getDeleted() != null ? representation.getDeleted() : false)
                .build();
    }

    /**
     * Create a new ProcessedItemVendorDispatchBatch entity from representation with itemWeightType (for creation only)
     */
    public ProcessedItemVendorDispatchBatch createAssemble(ProcessedItemVendorDispatchBatchRepresentation representation, String itemWeightType) {
        if (representation == null) {
            return null;
        }

        ItemStatus itemStatus = ItemStatus.VENDOR_DISPATCH_COMPLETED;
        if (representation.getItemStatus() != null) {
            itemStatus = ItemStatus.valueOf(representation.getItemStatus());
        }

        // Calculate totalExpectedPiecesCount based on dispatch type and itemWeightType
        Integer totalExpectedPiecesCount = calculateTotalExpectedPiecesCount(representation, itemWeightType);

        return ProcessedItemVendorDispatchBatch.builder()
                .item(itemService.getItemById(representation.getItem().getId()))
                .itemStatus(itemStatus)
                .workflowIdentifier(representation.getWorkflowIdentifier())
                .itemWorkflowId(representation.getItemWorkflowId())
                .previousOperationProcessedItemId(representation.getPreviousOperationProcessedItemId())
                .isInPieces(representation.getIsInPieces() != null ? representation.getIsInPieces() : false)
                .dispatchedPiecesCount(representation.getDispatchedPiecesCount())
                .dispatchedQuantity(representation.getDispatchedQuantity())
                .totalExpectedPiecesCount(totalExpectedPiecesCount)
                .totalReceivedPiecesCount(0)
                .totalRejectedPiecesCount(0)
                .totalTenantRejectsCount(0)
                .totalPiecesEligibleForNextOperation(0)
                .fullyReceived(false)
                .deleted(false)
                .build();
    }

    /**
     * Calculate totalExpectedPiecesCount based on dispatch type and itemWeightType
     */
    private Integer calculateTotalExpectedPiecesCount(ProcessedItemVendorDispatchBatchRepresentation representation, String itemWeightType) {
        if (representation.getIsInPieces() != null && representation.getIsInPieces()) {
            // For pieces-based dispatch, totalExpectedPiecesCount = dispatchedPiecesCount
            return representation.getDispatchedPiecesCount() != null ? representation.getDispatchedPiecesCount() : 0;
        } else {
            // For quantity-based dispatch, calculate pieces based on dispatched quantity and item weight
            if (representation.getDispatchedQuantity() != null && representation.getDispatchedQuantity() > 0 &&
                itemWeightType != null && !itemWeightType.trim().isEmpty()) {
                
                try {
                    // Get item weight for the specified weight type
                    Double itemWeight = itemService.getItemWeightByType(representation.getItem().getId(), itemWeightType);
                    
                    // Calculate expected pieces count: dispatchedQuantity / itemWeight
                    int expectedPieces = (int) Math.floor(representation.getDispatchedQuantity() / itemWeight);
                    
                    log.info("Calculated totalExpectedPiecesCount for quantity-based dispatch: dispatchedQuantity={}, itemWeight={}({}), expectedPieces={}", 
                             representation.getDispatchedQuantity(), itemWeight, itemWeightType, expectedPieces);
                    
                    return expectedPieces;
                } catch (Exception e) {
                    log.error("Failed to calculate totalExpectedPiecesCount for itemId={}, itemWeightType={}, dispatchedQuantity={}: {}", 
                             representation.getItem().getId(), itemWeightType, representation.getDispatchedQuantity(), e.getMessage());
                    return 0;
                }
            }
            
            return 0;
        }
    }

    /**
     * Update an existing ProcessedItemVendorDispatchBatch entity with values from representation
     */
    public void updateEntity(ProcessedItemVendorDispatchBatch entity, ProcessedItemVendorDispatchBatchRepresentation representation) {
        if (entity == null || representation == null) {
            return;
        }

        if (representation.getItemStatus() != null) {
            entity.setItemStatus(ItemStatus.valueOf(representation.getItemStatus()));
        }

        if (representation.getWorkflowIdentifier() != null) {
            entity.setWorkflowIdentifier(representation.getWorkflowIdentifier());
        }

        if (representation.getItemWorkflowId() != null) {
            entity.setItemWorkflowId(representation.getItemWorkflowId());
        }

        if (representation.getPreviousOperationProcessedItemId() != null) {
            entity.setPreviousOperationProcessedItemId(representation.getPreviousOperationProcessedItemId());
        }

        if (representation.getIsInPieces() != null) {
            entity.setIsInPieces(representation.getIsInPieces());
        }

        if (representation.getDispatchedPiecesCount() != null) {
            entity.setDispatchedPiecesCount(representation.getDispatchedPiecesCount());
        }

        if (representation.getDispatchedQuantity() != null) {
            entity.setDispatchedQuantity(representation.getDispatchedQuantity());
        }

        if (representation.getTotalExpectedPiecesCount() != null) {
            entity.setTotalExpectedPiecesCount(representation.getTotalExpectedPiecesCount());
        }

        if (representation.getTotalReceivedPiecesCount() != null) {
            entity.setTotalReceivedPiecesCount(representation.getTotalReceivedPiecesCount());
        }

        if (representation.getTotalRejectedPiecesCount() != null) {
            entity.setTotalRejectedPiecesCount(representation.getTotalRejectedPiecesCount());
        }

        if (representation.getTotalTenantRejectsCount() != null) {
            entity.setTotalTenantRejectsCount(representation.getTotalTenantRejectsCount());
        }

        if (representation.getTotalPiecesEligibleForNextOperation() != null) {
            entity.setTotalPiecesEligibleForNextOperation(representation.getTotalPiecesEligibleForNextOperation());
        }

        if (representation.getFullyReceived() != null) {
            entity.setFullyReceived(representation.getFullyReceived());
        }
    }
} 