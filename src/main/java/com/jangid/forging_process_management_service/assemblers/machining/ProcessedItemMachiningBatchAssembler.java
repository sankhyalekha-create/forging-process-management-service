package com.jangid.forging_process_management_service.assemblers.machining;

import com.jangid.forging_process_management_service.assemblers.forging.ForgeAssembler;
import com.jangid.forging_process_management_service.assemblers.product.ItemAssembler;
import com.jangid.forging_process_management_service.entities.ProcessedItem;
import com.jangid.forging_process_management_service.entities.machining.MachiningBatch;
import com.jangid.forging_process_management_service.entities.machining.ProcessedItemMachiningBatch;
import com.jangid.forging_process_management_service.entities.product.ItemStatus;
import com.jangid.forging_process_management_service.entitiesRepresentation.ProcessedItemRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.machining.MachiningBatchRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.machining.ProcessedItemMachiningBatchRepresentation;
import com.jangid.forging_process_management_service.service.ProcessedItemService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class ProcessedItemMachiningBatchAssembler {

  @Autowired
  private ForgeAssembler forgeAssembler;
  @Autowired
  private ItemAssembler itemAssembler;
  @Autowired
  private ProcessedItemService processedItemService;
  @Autowired
  private MachineSetAssembler machineSetAssembler;
  @Autowired
  private DailyMachiningBatchAssembler dailyMachiningBatchAssembler;
  @Autowired
  private MachiningHeatAssembler machiningHeatAssembler;


  public ProcessedItemMachiningBatchRepresentation dissemble(ProcessedItemMachiningBatch processedItemMachiningBatch) {
    ProcessedItem processedItem = processedItemMachiningBatch.getProcessedItem();
    ProcessedItemRepresentation processedItemRepresentation = processedItem != null ? dissemble(processedItem) : null;
    MachiningBatch machiningBatch = processedItemMachiningBatch.getMachiningBatch();
    MachiningBatchRepresentation machiningBatchRepresentation = machiningBatch != null ?dissemble(machiningBatch) : null;

    return ProcessedItemMachiningBatchRepresentation.builder()
        .id(processedItemMachiningBatch.getId())
        .processedItem(processedItemRepresentation)
        .itemStatus(processedItemMachiningBatch.getItemStatus().name())
        .machiningBatch(machiningBatchRepresentation)
        .machiningBatchPiecesCount(processedItemMachiningBatch.getMachiningBatchPiecesCount())
        .availableMachiningBatchPiecesCount(processedItemMachiningBatch.getAvailableMachiningBatchPiecesCount())
        .actualMachiningBatchPiecesCount(processedItemMachiningBatch.getActualMachiningBatchPiecesCount())
        .rejectMachiningBatchPiecesCount(processedItemMachiningBatch.getRejectMachiningBatchPiecesCount())
        .reworkPiecesCount(processedItemMachiningBatch.getReworkPiecesCount())
        .initialInspectionBatchPiecesCount(processedItemMachiningBatch.getInitialInspectionBatchPiecesCount())
        .availableInspectionBatchPiecesCount(processedItemMachiningBatch.getAvailableInspectionBatchPiecesCount())
        .createdAt(processedItemMachiningBatch.getCreatedAt() != null ? String.valueOf(processedItemMachiningBatch.getCreatedAt()) : null)
        .updatedAt(processedItemMachiningBatch.getUpdatedAt() != null ? String.valueOf(processedItemMachiningBatch.getUpdatedAt()) : null)
        .deleted(processedItemMachiningBatch.isDeleted())
        .build();
  }

  public ProcessedItemMachiningBatch assemble(ProcessedItemMachiningBatchRepresentation processedItemMachiningBatchRepresentation) {
    ProcessedItem processedItem = processedItemMachiningBatchRepresentation.getProcessedItem() != null ? processedItemService.getProcessedItemById(
        processedItemMachiningBatchRepresentation.getProcessedItem().getId()) : null;

    return ProcessedItemMachiningBatch.builder()
        .id(processedItemMachiningBatchRepresentation.getId())
        .processedItem(processedItem)
        .itemStatus(processedItemMachiningBatchRepresentation.getItemStatus() != null
                    ? ItemStatus.valueOf(processedItemMachiningBatchRepresentation.getItemStatus())
                    : null)
        .machiningBatchPiecesCount(processedItemMachiningBatchRepresentation.getMachiningBatchPiecesCount())
        .availableMachiningBatchPiecesCount(processedItemMachiningBatchRepresentation.getAvailableMachiningBatchPiecesCount())
        .actualMachiningBatchPiecesCount(processedItemMachiningBatchRepresentation.getActualMachiningBatchPiecesCount())
        .rejectMachiningBatchPiecesCount(processedItemMachiningBatchRepresentation.getRejectMachiningBatchPiecesCount())
        .reworkPiecesCount(processedItemMachiningBatchRepresentation.getReworkPiecesCount())
        .initialInspectionBatchPiecesCount(processedItemMachiningBatchRepresentation.getInitialInspectionBatchPiecesCount())
        .availableInspectionBatchPiecesCount(processedItemMachiningBatchRepresentation.getAvailableInspectionBatchPiecesCount())
        .createdAt(processedItemMachiningBatchRepresentation.getCreatedAt() != null
                   ? LocalDateTime.parse(processedItemMachiningBatchRepresentation.getCreatedAt())
                   : null)
        .updatedAt(processedItemMachiningBatchRepresentation.getUpdatedAt() != null
                   ? LocalDateTime.parse(processedItemMachiningBatchRepresentation.getUpdatedAt())
                   : null)
        .deleted(processedItemMachiningBatchRepresentation.isDeleted())
        .build();
  }

  public ProcessedItemMachiningBatch createAssemble(MachiningBatchRepresentation machiningBatchRepresentation, ProcessedItemMachiningBatchRepresentation processedItemMachiningBatchRepresentation) {
    ProcessedItemMachiningBatch processedItemMachiningBatch = assemble(processedItemMachiningBatchRepresentation);
    if (processedItemMachiningBatch.getProcessedItem() == null) {
      processedItemMachiningBatch.setProcessedItem(ProcessedItem.builder().item(itemAssembler.assemble(machiningBatchRepresentation.getItem())).createdAt(LocalDateTime.now()).build());
    }
    processedItemMachiningBatch.setItemStatus(ItemStatus.MACHINING_NOT_STARTED);
    processedItemMachiningBatch.setCreatedAt(LocalDateTime.now());
    return processedItemMachiningBatch;
  }

  public ProcessedItemRepresentation dissemble(ProcessedItem processedItem) {
    return ProcessedItemRepresentation.builder()
        .id(processedItem.getId())
        .forge(processedItem.getForge() != null ? forgeAssembler.dissemble(processedItem.getForge()) : null)
        .item(processedItem.getItem() != null ? itemAssembler.dissemble(processedItem.getItem()) : null)
        .expectedForgePiecesCount(processedItem.getExpectedForgePiecesCount())
        .actualForgePiecesCount(processedItem.getActualForgePiecesCount())
        .availableForgePiecesCountForHeat(processedItem.getAvailableForgePiecesCountForHeat())
        .createdAt(processedItem.getCreatedAt() != null ? processedItem.getCreatedAt().toString() : null)
        .updatedAt(processedItem.getUpdatedAt() != null ? processedItem.getUpdatedAt().toString() : null)
        .deletedAt(processedItem.getDeletedAt() != null ? processedItem.getDeletedAt().toString() : null)
        .deleted(processedItem.isDeleted())
        .build();
  }

  public MachiningBatchRepresentation dissemble(MachiningBatch machiningBatch) {

    ProcessedItemMachiningBatchRepresentation inputProcessedItemMachiningBatchRepresentation = null;

    return MachiningBatchRepresentation.builder()
        .id(machiningBatch.getId())
        .machiningBatchNumber(machiningBatch.getMachiningBatchNumber())
        .machineSet(machineSetAssembler.dissemble(machiningBatch.getMachineSet()))
        .machiningBatchStatus(
            machiningBatch.getMachiningBatchStatus() != null
            ? String.valueOf(machiningBatch.getMachiningBatchStatus())
            : null)
        .machiningBatchType(
            machiningBatch.getMachiningBatchType() != null
            ? String.valueOf(machiningBatch.getMachiningBatchType())
            : null)
        .inputProcessedItemMachiningBatch(inputProcessedItemMachiningBatchRepresentation)  // Add inputProcessedItemMachiningBatch
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
        .machiningHeats(machiningBatch.getMachiningHeats() != null ? machiningBatch.getMachiningHeats().stream().map(machiningHeatAssembler::dissemble).toList() : null)
        .build();
  }
}
