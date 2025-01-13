package com.jangid.forging_process_management_service.assemblers.machining;

import com.jangid.forging_process_management_service.entities.machining.MachiningBatch;
import com.jangid.forging_process_management_service.entities.machining.ProcessedItemMachiningBatch;
import com.jangid.forging_process_management_service.entitiesRepresentation.machining.MachiningBatchRepresentation;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
public class MachiningBatchAssembler {

  @Autowired
  private DailyMachiningBatchAssembler dailyMachiningBatchAssembler;

  @Autowired
  private ProcessedItemMachiningBatchAssembler processedItemMachiningBatchAssembler;



  public MachiningBatch createAssemble(MachiningBatchRepresentation machiningBatchRepresentation) {
    MachiningBatch machiningBatch = assemble(machiningBatchRepresentation);
    List<ProcessedItemMachiningBatch> processedItemMachiningBatchList = machiningBatchRepresentation.getProcessedItemMachiningBatches().stream()
        .map(processedItemMachiningBatchAssembler::createAssemble).toList();
    machiningBatch.setProcessedItemMachiningBatches(processedItemMachiningBatchList);
    machiningBatch.setMachiningBatchStatus(MachiningBatch.MachiningBatchStatus.IDLE);
    machiningBatch.setCreatedAt(LocalDateTime.now());
    return machiningBatch;
  }

  public MachiningBatch assemble(MachiningBatchRepresentation machiningBatchRepresentation) {
    return MachiningBatch.builder()
        .machiningBatchNumber(machiningBatchRepresentation.getMachiningBatchNumber())
        .machiningBatchStatus(
            machiningBatchRepresentation.getMachiningBatchStatus() != null
            ? MachiningBatch.MachiningBatchStatus.valueOf(machiningBatchRepresentation.getMachiningBatchStatus())
            : null)
        .machiningBatchType(
            machiningBatchRepresentation.getMachiningBatchType() != null
            ? MachiningBatch.MachiningBatchType.valueOf(machiningBatchRepresentation.getMachiningBatchType())
            : null)
        .startAt(machiningBatchRepresentation.getStartAt() != null
                 ? LocalDateTime.parse(machiningBatchRepresentation.getStartAt())
                 : null)
        .endAt(machiningBatchRepresentation.getEndAt() != null
               ? LocalDateTime.parse(machiningBatchRepresentation.getEndAt())
               : null)
        .dailyMachiningBatch(
            machiningBatchRepresentation.getDailyMachiningBatchDetail() != null
            ? machiningBatchRepresentation.getDailyMachiningBatchDetail().stream()
                .map(dailyMachiningBatchAssembler::assemble)
                .toList()
            : null)
        .build();
  }

  public MachiningBatchRepresentation dissemble(MachiningBatch machiningBatch) {
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
