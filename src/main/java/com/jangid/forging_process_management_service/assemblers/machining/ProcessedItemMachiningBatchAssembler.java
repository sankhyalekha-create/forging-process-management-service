package com.jangid.forging_process_management_service.assemblers.machining;

import com.jangid.forging_process_management_service.assemblers.product.ItemAssembler;
import com.jangid.forging_process_management_service.entities.product.Item;
import com.jangid.forging_process_management_service.entities.machining.MachiningBatch;
import com.jangid.forging_process_management_service.entities.machining.ProcessedItemMachiningBatch;
import com.jangid.forging_process_management_service.entities.product.ItemStatus;
import com.jangid.forging_process_management_service.entitiesRepresentation.product.ItemRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.machining.MachiningBatchRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.machining.ProcessedItemMachiningBatchRepresentation;
import com.jangid.forging_process_management_service.service.product.ItemService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.jangid.forging_process_management_service.entities.machining.MachiningHeat;

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

  public ProcessedItemMachiningBatchRepresentation dissemble(ProcessedItemMachiningBatch processedItemMachiningBatch) {
    Item item = processedItemMachiningBatch.getItem();
    ItemRepresentation itemRepresentation = itemAssembler.dissemble(item);
    MachiningBatch machiningBatch = processedItemMachiningBatch.getMachiningBatch();
    MachiningBatchRepresentation machiningBatchRepresentation = machiningBatch != null ? dissemble(machiningBatch) : null;

    return ProcessedItemMachiningBatchRepresentation.builder()
        .id(processedItemMachiningBatch.getId())
        .item(itemRepresentation)
        .machiningBatch(machiningBatchRepresentation)
        .machiningBatchesForRework(processedItemMachiningBatch.getMachiningBatchesForRework() != null ?
            processedItemMachiningBatch.getMachiningBatchesForRework().stream().map(this::dissemble).toList() : null)
        .itemStatus(processedItemMachiningBatch.getItemStatus() != null ? processedItemMachiningBatch.getItemStatus().name() : null)
        .machiningHeats(processedItemMachiningBatch.getMachiningHeats() != null 
            ? processedItemMachiningBatch.getMachiningHeats().stream()
                .map(machiningHeatAssembler::dissemble)
                .collect(Collectors.toList())
            : null)
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
}
