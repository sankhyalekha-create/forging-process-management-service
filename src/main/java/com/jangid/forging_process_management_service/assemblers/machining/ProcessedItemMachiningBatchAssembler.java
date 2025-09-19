package com.jangid.forging_process_management_service.assemblers.machining;

import com.jangid.forging_process_management_service.assemblers.product.ItemAssembler;
import com.jangid.forging_process_management_service.dto.HeatInfoDTO;
import com.jangid.forging_process_management_service.entities.product.Item;
import com.jangid.forging_process_management_service.entities.inventory.Heat;
import com.jangid.forging_process_management_service.entities.machining.MachiningBatch;
import com.jangid.forging_process_management_service.entities.machining.MachiningHeat;
import com.jangid.forging_process_management_service.entities.machining.ProcessedItemMachiningBatch;
import com.jangid.forging_process_management_service.entities.product.ItemStatus;
import com.jangid.forging_process_management_service.entitiesRepresentation.machining.MachiningHeatRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.product.ItemRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.machining.MachiningBatchRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.machining.ProcessedItemMachiningBatchRepresentation;
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
public class ProcessedItemMachiningBatchAssembler {

  @Autowired
  private ItemAssembler itemAssembler;
  @Autowired
  @Lazy
  private ItemService itemService;
  @Autowired
  private DailyMachiningBatchAssembler dailyMachiningBatchAssembler;
  @Autowired
  @Lazy
  private MachiningHeatAssembler machiningHeatAssembler;

  @Autowired
  @Lazy
  private HeatTraceabilityService heatTraceabilityService;

  @Autowired
  @Lazy
  private RawMaterialHeatService rawMaterialHeatService;

  @Autowired
  @Lazy
  private ItemWorkflowService itemWorkflowService;

  public ProcessedItemMachiningBatchRepresentation dissemble(ProcessedItemMachiningBatch processedItemMachiningBatch) {
    Item item = processedItemMachiningBatch.getItem();
    ItemRepresentation itemRepresentation = itemAssembler.dissembleBasic(item); // Use dissembleBasic to exclude itemWorkflows
    MachiningBatch machiningBatch = processedItemMachiningBatch.getMachiningBatch();
    MachiningBatchRepresentation machiningBatchRepresentation = machiningBatch != null ? dissemble(machiningBatch) : null;

    // Get workflow step information using the ItemWorkflowService
    Map<String, Object> workflowInfo = null;
    if (processedItemMachiningBatch.getItemWorkflowId() != null && processedItemMachiningBatch.getId() != null) {
      workflowInfo = itemWorkflowService.getCurrentStepAndNextOperation(
          processedItemMachiningBatch.getItemWorkflowId(), processedItemMachiningBatch.getId(), WorkflowStep.OperationType.MACHINING);
    }

    Long itemWorkflowStepId = workflowInfo != null ? (Long) workflowInfo.get("currentStepId") : null;
    List<String> nextOperations = workflowInfo != null ? (List<String>) workflowInfo.get("nextOperations") : null;

    return ProcessedItemMachiningBatchRepresentation.builder()
        .id(processedItemMachiningBatch.getId())
        .item(itemRepresentation)
        .machiningBatch(machiningBatchRepresentation)
        .machiningBatchesForRework(processedItemMachiningBatch.getMachiningBatchesForRework() != null ?
            processedItemMachiningBatch.getMachiningBatchesForRework().stream().map(this::dissemble).toList() : null)
        .itemStatus(processedItemMachiningBatch.getItemStatus() != null ? processedItemMachiningBatch.getItemStatus().name() : null)
        .machiningHeats(getMachiningHeatsRepresentation(processedItemMachiningBatch))
        .machiningBatchPiecesCount(processedItemMachiningBatch.getMachiningBatchPiecesCount())
        .availableMachiningBatchPiecesCount(processedItemMachiningBatch.getAvailableMachiningBatchPiecesCount())
        .actualMachiningBatchPiecesCount(processedItemMachiningBatch.getActualMachiningBatchPiecesCount())
        .rejectMachiningBatchPiecesCount(processedItemMachiningBatch.getRejectMachiningBatchPiecesCount())
        .reworkPiecesCount(processedItemMachiningBatch.getReworkPiecesCount())
        .reworkPiecesCountAvailableForRework(processedItemMachiningBatch.getReworkPiecesCountAvailableForRework())
        .initialInspectionBatchPiecesCount(processedItemMachiningBatch.getInitialInspectionBatchPiecesCount())
        .availableInspectionBatchPiecesCount(processedItemMachiningBatch.getAvailableInspectionBatchPiecesCount())
        .workflowIdentifier(processedItemMachiningBatch.getWorkflowIdentifier())
        .itemWorkflowId(processedItemMachiningBatch.getItemWorkflowId())
        .previousOperationProcessedItemId(processedItemMachiningBatch.getPreviousOperationProcessedItemId())
        .itemWorkflowStepId(itemWorkflowStepId)
        .nextOperations(nextOperations)
        .createdAt(processedItemMachiningBatch.getCreatedAt() != null ? String.valueOf(processedItemMachiningBatch.getCreatedAt()) : null)
        .updatedAt(processedItemMachiningBatch.getUpdatedAt() != null ? String.valueOf(processedItemMachiningBatch.getUpdatedAt()) : null)
        .deleted(processedItemMachiningBatch.isDeleted())
        .build();
  }

  public ProcessedItemMachiningBatch assemble(ProcessedItemMachiningBatchRepresentation processedItemMachiningBatchRepresentation) {
    if(processedItemMachiningBatchRepresentation == null){
      return null;
    }
    Item item = null;
    if (processedItemMachiningBatchRepresentation.getItem() != null) {
      if (processedItemMachiningBatchRepresentation.getItem().getId() != null) {
        item = itemService.getItemById(processedItemMachiningBatchRepresentation.getItem().getId());
      }
    }

    return ProcessedItemMachiningBatch.builder()
        .id(processedItemMachiningBatchRepresentation.getId())
        .item(item)
        .itemStatus(processedItemMachiningBatchRepresentation.getItemStatus() != null
                    ? ItemStatus.valueOf(processedItemMachiningBatchRepresentation.getItemStatus())
                    : null)
        .machiningBatchPiecesCount(processedItemMachiningBatchRepresentation.getMachiningBatchPiecesCount())
        .availableMachiningBatchPiecesCount(processedItemMachiningBatchRepresentation.getAvailableMachiningBatchPiecesCount())
        .actualMachiningBatchPiecesCount(processedItemMachiningBatchRepresentation.getActualMachiningBatchPiecesCount())
        .rejectMachiningBatchPiecesCount(processedItemMachiningBatchRepresentation.getRejectMachiningBatchPiecesCount())
        .reworkPiecesCount(processedItemMachiningBatchRepresentation.getReworkPiecesCount())
        .reworkPiecesCountAvailableForRework(processedItemMachiningBatchRepresentation.getReworkPiecesCountAvailableForRework())
        .initialInspectionBatchPiecesCount(processedItemMachiningBatchRepresentation.getInitialInspectionBatchPiecesCount())
        .availableInspectionBatchPiecesCount(processedItemMachiningBatchRepresentation.getAvailableInspectionBatchPiecesCount())
        .workflowIdentifier(processedItemMachiningBatchRepresentation.getWorkflowIdentifier())
        .itemWorkflowId(processedItemMachiningBatchRepresentation.getItemWorkflowId())
        .previousOperationProcessedItemId(processedItemMachiningBatchRepresentation.getPreviousOperationProcessedItemId())
        .createdAt(processedItemMachiningBatchRepresentation.getCreatedAt() != null
                   ? LocalDateTime.parse(processedItemMachiningBatchRepresentation.getCreatedAt())
                   : null)
        .updatedAt(processedItemMachiningBatchRepresentation.getUpdatedAt() != null
                   ? LocalDateTime.parse(processedItemMachiningBatchRepresentation.getUpdatedAt())
                   : null)
        .deleted(processedItemMachiningBatchRepresentation.isDeleted())
        .build();
  }

  public ProcessedItemMachiningBatch createAssemble(ProcessedItemMachiningBatchRepresentation processedItemMachiningBatchRepresentation) {
    ProcessedItemMachiningBatch processedItemMachiningBatch = assemble(processedItemMachiningBatchRepresentation);
    processedItemMachiningBatch.setItemStatus(ItemStatus.MACHINING_NOT_STARTED);
    processedItemMachiningBatch.setCreatedAt(LocalDateTime.now());
    
    // Create machining heats with proper parent reference
    if (processedItemMachiningBatchRepresentation.getMachiningHeats() != null) {
      List<MachiningHeat> machiningHeats = processedItemMachiningBatchRepresentation.getMachiningHeats().stream()
          .map(heatRepresentation -> machiningHeatAssembler.assemble(heatRepresentation, processedItemMachiningBatch))
          .collect(Collectors.toList());
      processedItemMachiningBatch.setMachiningHeats(machiningHeats);
    }
    
    return processedItemMachiningBatch;
  }

  public MachiningBatchRepresentation dissemble(MachiningBatch machiningBatch) {
    ProcessedItemMachiningBatchRepresentation inputProcessedItemMachiningBatchRepresentation = null;

    return MachiningBatchRepresentation.builder()
        .id(machiningBatch.getId())
        .machiningBatchNumber(machiningBatch.getMachiningBatchNumber())
        .machiningBatchStatus(
            machiningBatch.getMachiningBatchStatus() != null
            ? String.valueOf(machiningBatch.getMachiningBatchStatus())
            : null)
        .machiningBatchType(
            machiningBatch.getMachiningBatchType() != null
            ? String.valueOf(machiningBatch.getMachiningBatchType())
            : null)
        .inputProcessedItemMachiningBatch(inputProcessedItemMachiningBatchRepresentation)
        .startAt(
            machiningBatch.getStartAt() != null
            ? String.valueOf(machiningBatch.getStartAt())
            : null)
        .endAt(
            machiningBatch.getEndAt() != null
            ? String.valueOf(machiningBatch.getEndAt())
            : null)
        .dailyMachiningBatchDetail(
            machiningBatch.getDailyMachiningBatch() != null
            ? machiningBatch.getDailyMachiningBatch().stream()
                .map(dailyMachiningBatchAssembler::dissemble)
                .toList()
            : null)
        .build();
  }

  /**
   * Gets machining heats representation, either from existing data or by tracing back to first operation
   */
  private List<MachiningHeatRepresentation> getMachiningHeatsRepresentation(
      ProcessedItemMachiningBatch processedItemMachiningBatch) {
    
    // If machining heats already exist, use them
    if (processedItemMachiningBatch.getMachiningHeats() != null && 
        !processedItemMachiningBatch.getMachiningHeats().isEmpty()) {
      return processedItemMachiningBatch.getMachiningHeats().stream()
          .map(machiningHeatAssembler::dissemble)
          .collect(Collectors.toList());
    }

    // If no existing machining heats and itemWorkflowId exists, get from first operation
    if (processedItemMachiningBatch.getItemWorkflowId() != null) {
      try {
        List<HeatInfoDTO> firstOperationHeats = heatTraceabilityService
            .getFirstOperationHeatsForMachining(processedItemMachiningBatch.getItemWorkflowId());
        
        return createMachiningHeatsFromFirstOperation(firstOperationHeats, processedItemMachiningBatch);
      } catch (Exception e) {
        log.warn("Failed to retrieve first operation heats for itemWorkflowId {}: {}", 
                processedItemMachiningBatch.getItemWorkflowId(), e.getMessage());
      }
    }

    // Return empty list if no heats can be found
    return Collections.emptyList();
  }

  /**
   * Creates MachiningHeat representations from first operation heat information
   */
  private List<MachiningHeatRepresentation> createMachiningHeatsFromFirstOperation(
      List<HeatInfoDTO> heatInfos, ProcessedItemMachiningBatch processedItemMachiningBatch) {
    
    if (heatInfos == null || heatInfos.isEmpty()) {
      return Collections.emptyList();
    }

    List<MachiningHeatRepresentation> representations = new ArrayList<>();
    
    for (HeatInfoDTO heatInfo : heatInfos) {
      try {
        // Get the actual Heat entity for complete information
        Heat heat = rawMaterialHeatService.getHeatById(heatInfo.getHeatId());
        
        // Create a temporary MachiningHeat entity for the assembler
        MachiningHeat tempMachiningHeat = MachiningHeat.builder()
            .id(null) // This is derived data, not a real entity
            .processedItemMachiningBatch(processedItemMachiningBatch)
            .heat(heat)
            .piecesUsed(0) // Default since we're just showing heat info, not actual consumption
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .deleted(false)
            .build();

        representations.add(machiningHeatAssembler.dissemble(tempMachiningHeat));
      } catch (Exception e) {
        log.warn("Failed to create machining heat representation for heat {}: {}", 
                heatInfo.getHeatId(), e.getMessage());
      }
    }

    return representations;
  }
}
