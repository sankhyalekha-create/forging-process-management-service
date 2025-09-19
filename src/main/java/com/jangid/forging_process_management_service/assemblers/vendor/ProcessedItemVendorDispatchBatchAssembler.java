package com.jangid.forging_process_management_service.assemblers.vendor;

import com.jangid.forging_process_management_service.assemblers.product.ItemAssembler;
import com.jangid.forging_process_management_service.dto.HeatInfoDTO;
import com.jangid.forging_process_management_service.entities.inventory.Heat;
import com.jangid.forging_process_management_service.entities.product.ItemStatus;
import com.jangid.forging_process_management_service.entities.vendor.ProcessedItemVendorDispatchBatch;
import com.jangid.forging_process_management_service.entities.vendor.VendorDispatchHeat;
import com.jangid.forging_process_management_service.entitiesRepresentation.vendor.ProcessedItemVendorDispatchBatchRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.vendor.VendorDispatchHeatRepresentation;
import com.jangid.forging_process_management_service.service.common.HeatTraceabilityService;
import com.jangid.forging_process_management_service.service.inventory.RawMaterialHeatService;
import com.jangid.forging_process_management_service.service.product.ItemService;
import com.jangid.forging_process_management_service.service.workflow.ItemWorkflowService;
import com.jangid.forging_process_management_service.entities.workflow.WorkflowStep;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

    @Autowired
    @Lazy
    private HeatTraceabilityService heatTraceabilityService;

    @Autowired
    @Lazy
    private RawMaterialHeatService rawMaterialHeatService;

    @Autowired
    @Lazy
    private ItemWorkflowService itemWorkflowService;

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

        // Get workflow step information using the ItemWorkflowService
        Map<String, Object> workflowInfo = null;
        if (entity.getItemWorkflowId() != null && entity.getId() != null) {
            workflowInfo = itemWorkflowService.getCurrentStepAndNextOperation(
                    entity.getItemWorkflowId(), entity.getId(), WorkflowStep.OperationType.VENDOR);
        }

        Long itemWorkflowStepId = workflowInfo != null ? (Long) workflowInfo.get("currentStepId") : null;
        List<String> nextOperations = workflowInfo != null ? (List<String>) workflowInfo.get("nextOperations") : null;

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
                .vendorDispatchHeats(getVendorDispatchHeatsRepresentation(entity))
                .isInPieces(entity.getIsInPieces())
                .dispatchedPiecesCount(entity.getDispatchedPiecesCount())
                .dispatchedQuantity(entity.getDispatchedQuantity())
                .totalExpectedPiecesCount(entity.getTotalExpectedPiecesCount())
                .totalReceivedPiecesCount(entity.getTotalReceivedPiecesCount())
                .totalRejectedPiecesCount(entity.getTotalRejectedPiecesCount())
                .totalTenantRejectsCount(entity.getTotalTenantRejectsCount())
                .totalPiecesEligibleForNextOperation(entity.getTotalPiecesEligibleForNextOperation())
                .fullyReceived(entity.getFullyReceived())
                .itemWorkflowStepId(itemWorkflowStepId)
                .nextOperations(nextOperations)
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

    /**
     * Gets vendor dispatch heats representation, either from existing data or by tracing back to first operation
     */
    private List<VendorDispatchHeatRepresentation> getVendorDispatchHeatsRepresentation(
        ProcessedItemVendorDispatchBatch processedItemVendorDispatchBatch) {
      
      // If vendor dispatch heats already exist, use them
      if (processedItemVendorDispatchBatch.getVendorDispatchHeats() != null && 
          !processedItemVendorDispatchBatch.getVendorDispatchHeats().isEmpty()) {
        return processedItemVendorDispatchBatch.getVendorDispatchHeats().stream()
            .map(heat -> vendorDispatchHeatAssembler.dissemble(heat, false))
            .collect(Collectors.toList());
      }

      // If no existing vendor dispatch heats and itemWorkflowId exists, get from first operation
      if (processedItemVendorDispatchBatch.getItemWorkflowId() != null) {
        try {
          List<HeatInfoDTO> firstOperationHeats = heatTraceabilityService
              .getFirstOperationHeatsForVendorDispatch(processedItemVendorDispatchBatch.getItemWorkflowId());
          
          return createVendorDispatchHeatsFromFirstOperation(firstOperationHeats, processedItemVendorDispatchBatch);
        } catch (Exception e) {
          log.warn("Failed to retrieve first operation heats for itemWorkflowId {}: {}", 
                  processedItemVendorDispatchBatch.getItemWorkflowId(), e.getMessage());
        }
      }

      log.debug("No vendor dispatch heats found for ProcessedItemVendorDispatchBatch {}", 
                processedItemVendorDispatchBatch.getId());
      return new ArrayList<>();
    }

    /**
     * Creates vendor dispatch heat representations from first operation heat information
     */
    private List<VendorDispatchHeatRepresentation> createVendorDispatchHeatsFromFirstOperation(
        List<HeatInfoDTO> firstOperationHeats, ProcessedItemVendorDispatchBatch processedItemVendorDispatchBatch) {
      
      return firstOperationHeats.stream()
          .map(heatInfo -> createVendorDispatchHeatFromHeatInfo(heatInfo, processedItemVendorDispatchBatch))
          .filter(heat -> heat != null)
          .collect(Collectors.toList());
    }

    /**
     * Creates a vendor dispatch heat representation from heat info
     */
    private VendorDispatchHeatRepresentation createVendorDispatchHeatFromHeatInfo(
        HeatInfoDTO heatInfo, ProcessedItemVendorDispatchBatch processedItemVendorDispatchBatch) {
      
      try {
        Heat heat = rawMaterialHeatService.getHeatById(heatInfo.getHeatId());
        if (heat == null) {
          log.warn("Heat with ID {} not found for ProcessedItemVendorDispatchBatch {}", 
                  heatInfo.getHeatId(), processedItemVendorDispatchBatch.getId());
          return null;
        }

        // Create a vendor dispatch heat for representation purposes (not persisted)
        VendorDispatchHeat vendorDispatchHeat = VendorDispatchHeat.builder()
            .heat(heat)
            .piecesUsed(0) // Set to 0 since we're only tracing heat lineage, not actual usage
            .build();

        return vendorDispatchHeatAssembler.dissemble(vendorDispatchHeat, false);
      } catch (Exception e) {
        log.error("Error creating vendor dispatch heat from heat info for heat ID {}: {}", 
                 heatInfo.getHeatId(), e.getMessage());
        return null;
      }
    }
} 