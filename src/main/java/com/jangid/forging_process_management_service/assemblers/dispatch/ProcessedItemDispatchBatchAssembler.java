package com.jangid.forging_process_management_service.assemblers.dispatch;

import com.jangid.forging_process_management_service.assemblers.product.ItemAssembler;
import com.jangid.forging_process_management_service.dto.HeatInfoDTO;
import com.jangid.forging_process_management_service.entities.product.Item;
import com.jangid.forging_process_management_service.entities.dispatch.DispatchHeat;
import com.jangid.forging_process_management_service.entities.dispatch.ProcessedItemDispatchBatch;
import com.jangid.forging_process_management_service.entities.inventory.Heat;
import com.jangid.forging_process_management_service.entitiesRepresentation.dispatch.DispatchHeatRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.product.ItemRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.dispatch.ProcessedItemDispatchBatchRepresentation;
import com.jangid.forging_process_management_service.service.common.HeatTraceabilityService;
import com.jangid.forging_process_management_service.service.inventory.RawMaterialHeatService;
import com.jangid.forging_process_management_service.service.product.ItemService;
import com.jangid.forging_process_management_service.service.workflow.ItemWorkflowService;
import com.jangid.forging_process_management_service.entities.workflow.WorkflowStep;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ProcessedItemDispatchBatchAssembler {

  @Autowired
  private ItemAssembler itemAssembler;

  @Autowired
  @Lazy
  private ItemService itemService;

  @Autowired
  @Lazy
  private DispatchHeatAssembler dispatchHeatAssembler;

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
   * Converts a ProcessedItemDispatchBatch entity to its representation.
   */
  public ProcessedItemDispatchBatchRepresentation dissemble(ProcessedItemDispatchBatch processedItemDispatchBatch) {
    Item item = processedItemDispatchBatch.getItem();
    ItemRepresentation itemRepresentation = itemAssembler.dissembleBasic(item); // Use dissembleBasic to exclude itemWorkflows

    // Get workflow step information using the ItemWorkflowService
    Map<String, Object> workflowInfo = null;
    if (processedItemDispatchBatch.getItemWorkflowId() != null && processedItemDispatchBatch.getId() != null) {
      workflowInfo = itemWorkflowService.getCurrentStepAndNextOperation(
          processedItemDispatchBatch.getItemWorkflowId(), processedItemDispatchBatch.getId(), WorkflowStep.OperationType.DISPATCH);
    }

    Long itemWorkflowStepId = workflowInfo != null ? (Long) workflowInfo.get("currentStepId") : null;
    List<String> nextOperations = workflowInfo != null ? (List<String>) workflowInfo.get("nextOperations") : null;

    return ProcessedItemDispatchBatchRepresentation.builder()
        .id(processedItemDispatchBatch.getId())
        .item(itemRepresentation)
        .dispatchHeats(getDispatchHeatsRepresentation(processedItemDispatchBatch))
        .totalDispatchPiecesCount(processedItemDispatchBatch.getTotalDispatchPiecesCount())
        .workflowIdentifier(processedItemDispatchBatch.getWorkflowIdentifier())
        .itemWorkflowId(processedItemDispatchBatch.getItemWorkflowId())
        .previousOperationProcessedItemId(processedItemDispatchBatch.getPreviousOperationProcessedItemId())
        .totalParentEntitiesCount(processedItemDispatchBatch.getTotalParentEntitiesCount())
        .isMultiParentDispatch(processedItemDispatchBatch.getIsMultiParentDispatch())
        .itemWorkflowStepId(itemWorkflowStepId)
        .nextOperations(nextOperations)
        .build();
  }

  /**
   * Converts a ProcessedItemDispatchBatchRepresentation to its entity.
   */
  public ProcessedItemDispatchBatch assemble(ProcessedItemDispatchBatchRepresentation representation) {
    Item item = null;
    if (representation.getItem() != null) {
      if (representation.getItem().getId() != null) {
        item = itemService.getItemById(representation.getItem().getId());
      }
    }

    ProcessedItemDispatchBatch processedItemDispatchBatch = ProcessedItemDispatchBatch.builder()
        .id(representation.getId())
        .item(item)
        .totalDispatchPiecesCount(representation.getTotalDispatchPiecesCount())
        .dispatchHeats(representation.getDispatchHeats() != null 
            ? representation.getDispatchHeats().stream()
                .map(dispatchHeatAssembler::assemble)
                .collect(Collectors.toList())
            : null)
        .workflowIdentifier(representation.getWorkflowIdentifier())
        .itemWorkflowId(representation.getItemWorkflowId())
        .previousOperationProcessedItemId(representation.getPreviousOperationProcessedItemId())
        .totalParentEntitiesCount(representation.getTotalParentEntitiesCount())
        .isMultiParentDispatch(representation.getIsMultiParentDispatch())
        .build();
    if (processedItemDispatchBatch.getDispatchHeats() != null) {
      processedItemDispatchBatch.getDispatchHeats().forEach(dispatchHeat -> {
        dispatchHeat.setProcessedItemDispatchBatch(processedItemDispatchBatch);
      });
    }
    return processedItemDispatchBatch;
  }

  /**
   * Creates a new ProcessedItemDispatchBatch from the provided representation.
   */
  public ProcessedItemDispatchBatch createAssemble(ProcessedItemDispatchBatchRepresentation representation) {
    ProcessedItemDispatchBatch processedItemDispatchBatch = assemble(representation);
    processedItemDispatchBatch.setCreatedAt(LocalDateTime.now());
    return processedItemDispatchBatch;
  }

  /**
   * Gets dispatch heats representation, either from existing data or by tracing back to first operation
   */
  private List<DispatchHeatRepresentation> getDispatchHeatsRepresentation(
      ProcessedItemDispatchBatch processedItemDispatchBatch) {
    
    // If dispatch heats already exist, use them
    if (processedItemDispatchBatch.getDispatchHeats() != null && 
        !processedItemDispatchBatch.getDispatchHeats().isEmpty()) {
      return processedItemDispatchBatch.getDispatchHeats().stream()
          .map(dispatchHeatAssembler::dissemble)
          .collect(Collectors.toList());
    }

    // If no existing dispatch heats and itemWorkflowId exists, get from first operation
    if (processedItemDispatchBatch.getItemWorkflowId() != null) {
      try {
        List<HeatInfoDTO> firstOperationHeats = heatTraceabilityService
            .getFirstOperationHeatsForDispatch(processedItemDispatchBatch.getItemWorkflowId());
        
        return createDispatchHeatsFromFirstOperation(firstOperationHeats, processedItemDispatchBatch);
      } catch (Exception e) {
        log.warn("Failed to retrieve first operation heats for itemWorkflowId {}: {}", 
                processedItemDispatchBatch.getItemWorkflowId(), e.getMessage());
      }
    }

    // Return empty list if no heats can be found
    return Collections.emptyList();
  }

  /**
   * Creates DispatchHeat representations from first operation heat information
   */
  private List<DispatchHeatRepresentation> createDispatchHeatsFromFirstOperation(
      List<HeatInfoDTO> heatInfos, ProcessedItemDispatchBatch processedItemDispatchBatch) {
    
    if (heatInfos == null || heatInfos.isEmpty()) {
      return Collections.emptyList();
    }

    List<DispatchHeatRepresentation> representations = new ArrayList<>();
    
    for (HeatInfoDTO heatInfo : heatInfos) {
      try {
        // Get the actual Heat entity for complete information
        Heat heat = rawMaterialHeatService.getHeatById(heatInfo.getHeatId());
        
        // Create a temporary DispatchHeat entity for the assembler
        DispatchHeat tempDispatchHeat = DispatchHeat.builder()
            .id(null) // This is derived data, not a real entity
            .processedItemDispatchBatch(processedItemDispatchBatch)
            .heat(heat)
            .piecesUsed(0) // Default since we're just showing heat info, not actual consumption
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .deleted(false)
            .build();

        representations.add(dispatchHeatAssembler.dissemble(tempDispatchHeat));
      } catch (Exception e) {
        log.warn("Failed to create dispatch heat representation for heat {}: {}", 
                heatInfo.getHeatId(), e.getMessage());
      }
    }

    return representations;
  }
}

