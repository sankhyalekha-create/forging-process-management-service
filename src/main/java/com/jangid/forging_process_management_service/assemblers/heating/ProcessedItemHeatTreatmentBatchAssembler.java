package com.jangid.forging_process_management_service.assemblers.heating;

import com.jangid.forging_process_management_service.assemblers.product.ItemAssembler;
import com.jangid.forging_process_management_service.dto.HeatInfoDTO;
import com.jangid.forging_process_management_service.entities.heating.HeatTreatmentBatch;
import com.jangid.forging_process_management_service.entities.heating.HeatTreatmentHeat;
import com.jangid.forging_process_management_service.entities.heating.ProcessedItemHeatTreatmentBatch;
import com.jangid.forging_process_management_service.entities.inventory.Heat;
import com.jangid.forging_process_management_service.entities.product.Item;
import com.jangid.forging_process_management_service.entities.product.ItemStatus;
import com.jangid.forging_process_management_service.entitiesRepresentation.heating.HeatTreatmentHeatRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.product.ItemRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.heating.HeatTreatmentBatchRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.heating.ProcessedItemHeatTreatmentBatchRepresentation;
import com.jangid.forging_process_management_service.service.common.HeatTraceabilityService;
import com.jangid.forging_process_management_service.service.product.ItemService;
import com.jangid.forging_process_management_service.service.inventory.RawMaterialHeatService;
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
public class ProcessedItemHeatTreatmentBatchAssembler {

  @Autowired
  private ItemAssembler itemAssembler;
  @Autowired
  @Lazy
  private ItemService itemService;

  @Autowired
  @Lazy
  private HeatTreatmentHeatAssembler heatTreatmentHeatAssembler;

  @Autowired
  @Lazy
  private HeatTraceabilityService heatTraceabilityService;

  @Autowired
  @Lazy
  private RawMaterialHeatService rawMaterialHeatService;

  @Autowired
  @Lazy
  private ItemWorkflowService itemWorkflowService;

  public ProcessedItemHeatTreatmentBatchRepresentation dissemble(ProcessedItemHeatTreatmentBatch processedItemHeatTreatmentBatch) {
    Item item = processedItemHeatTreatmentBatch.getItem();
    ItemRepresentation itemRepresentation = itemAssembler.dissembleBasic(item); // Use dissembleBasic to exclude itemWorkflows
    
    // Get workflow step information using the ItemWorkflowService
    Map<String, Object> workflowInfo = null;
    if (processedItemHeatTreatmentBatch.getItemWorkflowId() != null && processedItemHeatTreatmentBatch.getId() != null) {
      workflowInfo = itemWorkflowService.getCurrentStepAndNextOperation(
          processedItemHeatTreatmentBatch.getItemWorkflowId(), processedItemHeatTreatmentBatch.getId(), WorkflowStep.OperationType.HEAT_TREATMENT);
    }

    Long itemWorkflowStepId = workflowInfo != null ? (Long) workflowInfo.get("currentStepId") : null;
    List<String> nextOperations = workflowInfo != null ? (List<String>) workflowInfo.get("nextOperations") : null;
    
    return ProcessedItemHeatTreatmentBatchRepresentation.builder()
        .id(processedItemHeatTreatmentBatch.getId())
        .item(itemRepresentation)
        .heatTreatmentBatch(dissemble(processedItemHeatTreatmentBatch.getHeatTreatmentBatch()))
        .itemStatus(processedItemHeatTreatmentBatch.getItemStatus().name())
        .heatTreatmentHeats(getHeatTreatmentHeatsRepresentation(processedItemHeatTreatmentBatch))
        .heatTreatBatchPiecesCount(processedItemHeatTreatmentBatch.getHeatTreatBatchPiecesCount())
        .actualHeatTreatBatchPiecesCount(processedItemHeatTreatmentBatch.getActualHeatTreatBatchPiecesCount())
//        .initialMachiningBatchPiecesCount(processedItemHeatTreatmentBatch.getInitialMachiningBatchPiecesCount())
//        .availableMachiningBatchPiecesCount(processedItemHeatTreatmentBatch.getAvailableMachiningBatchPiecesCount())
        .workflowIdentifier(processedItemHeatTreatmentBatch.getWorkflowIdentifier())
        .itemWorkflowId(processedItemHeatTreatmentBatch.getItemWorkflowId())
        .previousOperationProcessedItemId(processedItemHeatTreatmentBatch.getPreviousOperationProcessedItemId())
        .itemWorkflowStepId(itemWorkflowStepId)
        .nextOperations(nextOperations)
        .createdAt(processedItemHeatTreatmentBatch.getCreatedAt() != null ? String.valueOf(processedItemHeatTreatmentBatch.getCreatedAt()) : null)
        .updatedAt(processedItemHeatTreatmentBatch.getUpdatedAt() != null ? String.valueOf(processedItemHeatTreatmentBatch.getUpdatedAt()) : null)
        .deletedAt(processedItemHeatTreatmentBatch.getDeletedAt() != null ? String.valueOf(processedItemHeatTreatmentBatch.getDeletedAt()) : null)
        .deleted(processedItemHeatTreatmentBatch.isDeleted())
        .build();
  }

  public ProcessedItemHeatTreatmentBatch assemble(ProcessedItemHeatTreatmentBatchRepresentation processedItemHeatTreatmentBatchRepresentation) {
    if(processedItemHeatTreatmentBatchRepresentation == null){
      return null;
    }
    Item item = null;
    if (processedItemHeatTreatmentBatchRepresentation.getItem() != null) {
      if (processedItemHeatTreatmentBatchRepresentation.getItem().getId() != null) {
        item = itemService.getItemById(processedItemHeatTreatmentBatchRepresentation.getItem().getId());
      }
    }
    HeatTreatmentBatch heatTreatmentBatch = null;
    if ( processedItemHeatTreatmentBatchRepresentation.getHeatTreatmentBatch() != null) {
      heatTreatmentBatch = assemble(processedItemHeatTreatmentBatchRepresentation.getHeatTreatmentBatch());
    }

    // Build the ProcessedItemHeatTreatmentBatch first without heatTreatmentHeats
    ProcessedItemHeatTreatmentBatch processedItemHeatTreatmentBatch = ProcessedItemHeatTreatmentBatch.builder()
        .id(processedItemHeatTreatmentBatchRepresentation.getId())
        .item(item)
        .heatTreatmentBatch(heatTreatmentBatch)
        .itemStatus(processedItemHeatTreatmentBatchRepresentation.getItemStatus() != null ? ItemStatus.valueOf(processedItemHeatTreatmentBatchRepresentation.getItemStatus()) : null)
        .heatTreatBatchPiecesCount(processedItemHeatTreatmentBatchRepresentation.getHeatTreatBatchPiecesCount())
        .actualHeatTreatBatchPiecesCount(processedItemHeatTreatmentBatchRepresentation.getActualHeatTreatBatchPiecesCount())
//        .initialMachiningBatchPiecesCount(processedItemHeatTreatmentBatchRepresentation.getInitialMachiningBatchPiecesCount())
//        .availableMachiningBatchPiecesCount(processedItemHeatTreatmentBatchRepresentation.getAvailableMachiningBatchPiecesCount())
        .workflowIdentifier(processedItemHeatTreatmentBatchRepresentation.getWorkflowIdentifier())
        .itemWorkflowId(processedItemHeatTreatmentBatchRepresentation.getItemWorkflowId())
        .previousOperationProcessedItemId(processedItemHeatTreatmentBatchRepresentation.getPreviousOperationProcessedItemId())
        .createdAt(processedItemHeatTreatmentBatchRepresentation.getCreatedAt() != null ? LocalDateTime.parse(processedItemHeatTreatmentBatchRepresentation.getCreatedAt()) : null)
        .updatedAt(processedItemHeatTreatmentBatchRepresentation.getUpdatedAt() != null ? LocalDateTime.parse(processedItemHeatTreatmentBatchRepresentation.getUpdatedAt()) : null)
        .deletedAt(processedItemHeatTreatmentBatchRepresentation.getDeletedAt() != null ? LocalDateTime.parse(processedItemHeatTreatmentBatchRepresentation.getDeletedAt()) : null)
        .deleted(processedItemHeatTreatmentBatchRepresentation.getDeleted() != null ? processedItemHeatTreatmentBatchRepresentation.getDeleted() : false)
        .build();

    // Now convert and set heatTreatmentHeats
    if (processedItemHeatTreatmentBatchRepresentation.getHeatTreatmentHeats() != null) {
      processedItemHeatTreatmentBatch.setHeatTreatmentHeats(
          processedItemHeatTreatmentBatchRepresentation.getHeatTreatmentHeats().stream()
              .map(heatRepresentation -> heatTreatmentHeatAssembler.assemble(heatRepresentation, processedItemHeatTreatmentBatch))
              .collect(Collectors.toList())
      );
    }

    return processedItemHeatTreatmentBatch;
  }

  public ProcessedItemHeatTreatmentBatch createAssemble(ProcessedItemHeatTreatmentBatchRepresentation processedItemHeatTreatmentBatchRepresentation) {
    ProcessedItemHeatTreatmentBatch processedItemHeatTreatmentBatch = assemble(processedItemHeatTreatmentBatchRepresentation);
    processedItemHeatTreatmentBatch.setCreatedAt(LocalDateTime.now());
    return processedItemHeatTreatmentBatch;
  }

  public HeatTreatmentBatch assemble(HeatTreatmentBatchRepresentation heatTreatmentBatchRepresentation) {
    HeatTreatmentBatch heatTreatmentBatch = null;
    if (heatTreatmentBatchRepresentation != null) {
      heatTreatmentBatch = HeatTreatmentBatch.builder()
          .heatTreatmentBatchNumber(heatTreatmentBatchRepresentation.getHeatTreatmentBatchNumber())
          .labTestingReport(heatTreatmentBatchRepresentation.getLabTestingReport())
          .labTestingStatus(heatTreatmentBatchRepresentation.getLabTestingStatus())
//        .processedItemHeatTreatmentBatches(getProcessedItemHeatTreatmentBatches(heatTreatmentBatchRepresentation.getProcessedItemHeatTreatmentBatches()))
//        .startAt(parseDate(heatTreatmentBatchRepresentation.getStartAt()))
//        .endAt(parseDate(heatTreatmentBatchRepresentation.getEndAt()))
          .build();

      // Calculate total weight after assembly
      heatTreatmentBatch.calculateTotalWeight();
    }

    return heatTreatmentBatch;
  }

  public HeatTreatmentBatchRepresentation dissemble(HeatTreatmentBatch heatTreatmentBatch) {
    return HeatTreatmentBatchRepresentation.builder()
        .id(heatTreatmentBatch.getId())
        .heatTreatmentBatchNumber(heatTreatmentBatch.getHeatTreatmentBatchNumber())
        .totalWeight(String.valueOf(heatTreatmentBatch.getTotalWeight()))
        .heatTreatmentBatchStatus(heatTreatmentBatch.getHeatTreatmentBatchStatus().name())
        .labTestingReport(heatTreatmentBatch.getLabTestingReport())
        .labTestingStatus(heatTreatmentBatch.getLabTestingStatus())
        .applyAt(heatTreatmentBatch.getApplyAt()!=null?heatTreatmentBatch.getApplyAt().toString():null)
        .startAt(heatTreatmentBatch.getStartAt()!=null?heatTreatmentBatch.getStartAt().toString():null)
        .endAt(heatTreatmentBatch.getEndAt()!=null?heatTreatmentBatch.getEndAt().toString():null)
        .build();
  }

  /**
   * Gets heat treatment heats representation, either from existing data or by tracing back to first operation
   */
  private List<HeatTreatmentHeatRepresentation> getHeatTreatmentHeatsRepresentation(
      ProcessedItemHeatTreatmentBatch processedItemHeatTreatmentBatch) {
    
    // If heat treatment heats already exist, use them
    if (processedItemHeatTreatmentBatch.getHeatTreatmentHeats() != null && 
        !processedItemHeatTreatmentBatch.getHeatTreatmentHeats().isEmpty()) {
      return processedItemHeatTreatmentBatch.getHeatTreatmentHeats().stream()
          .map(heatTreatmentHeatAssembler::dissemble)
          .collect(Collectors.toList());
    }

    // If no existing heat treatment heats and itemWorkflowId exists, get from first operation
    if (processedItemHeatTreatmentBatch.getItemWorkflowId() != null) {
      try {
        List<HeatInfoDTO> firstOperationHeats = heatTraceabilityService
            .getFirstOperationHeatsForHeatTreatment(processedItemHeatTreatmentBatch.getItemWorkflowId());
        
        return createHeatTreatmentHeatsFromFirstOperation(firstOperationHeats, processedItemHeatTreatmentBatch);
      } catch (Exception e) {
        log.warn("Failed to retrieve first operation heats for itemWorkflowId {}: {}", 
                processedItemHeatTreatmentBatch.getItemWorkflowId(), e.getMessage());
      }
    }

    // Return empty list if no heats can be found
    return Collections.emptyList();
  }

  /**
   * Creates HeatTreatmentHeat representations from first operation heat information
   */
  private List<HeatTreatmentHeatRepresentation> createHeatTreatmentHeatsFromFirstOperation(
      List<HeatInfoDTO> heatInfos, ProcessedItemHeatTreatmentBatch processedItemHeatTreatmentBatch) {
    
    if (heatInfos == null || heatInfos.isEmpty()) {
      return Collections.emptyList();
    }

    List<HeatTreatmentHeatRepresentation> representations = new ArrayList<>();
    
    for (HeatInfoDTO heatInfo : heatInfos) {
      try {
        // Get the actual Heat entity for complete information
        Heat heat = rawMaterialHeatService.getHeatById(heatInfo.getHeatId());
        
        // Create a temporary HeatTreatmentHeat entity for the assembler
        HeatTreatmentHeat tempHeatTreatmentHeat = HeatTreatmentHeat.builder()
            .id(null) // This is derived data, not a real entity
            .processedItemHeatTreatmentBatch(processedItemHeatTreatmentBatch)
            .heat(heat)
            .piecesUsed(0) // Default since we're just showing heat info, not actual consumption
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .deleted(false)
            .build();

        representations.add(heatTreatmentHeatAssembler.dissemble(tempHeatTreatmentHeat));
      } catch (Exception e) {
        log.warn("Failed to create heat treatment heat representation for heat {}: {}", 
                heatInfo.getHeatId(), e.getMessage());
      }
    }

    return representations;
  }
}

