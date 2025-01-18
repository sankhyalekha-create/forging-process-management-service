package com.jangid.forging_process_management_service.assemblers.machining;

import com.jangid.forging_process_management_service.assemblers.heating.ProcessedItemHeatTreatmentBatchAssembler;
import com.jangid.forging_process_management_service.entities.machining.MachiningBatch;
import com.jangid.forging_process_management_service.entities.machining.ProcessedItemMachiningBatch;
import com.jangid.forging_process_management_service.entitiesRepresentation.machining.MachiningBatchRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.machining.ProcessedItemMachiningBatchRepresentation;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
public class MachiningBatchAssembler {

  @Autowired
  private MachineSetAssembler machineSetAssembler;

  @Autowired
  private DailyMachiningBatchAssembler dailyMachiningBatchAssembler;

  @Autowired
  private ProcessedItemMachiningBatchAssembler processedItemMachiningBatchAssembler;

  @Autowired
  private ProcessedItemHeatTreatmentBatchAssembler processedItemHeatTreatmentBatchAssembler;

  public MachiningBatch createAssemble(MachiningBatchRepresentation machiningBatchRepresentation) {
    MachiningBatch machiningBatch = assemble(machiningBatchRepresentation);

    if (machiningBatchRepresentation.getProcessedItemMachiningBatch() != null) {
      ProcessedItemMachiningBatch processedItemMachiningBatch =
          processedItemMachiningBatchAssembler.createAssemble(machiningBatchRepresentation.getProcessedItemMachiningBatch());
      machiningBatch.setProcessedItemMachiningBatch(processedItemMachiningBatch);
    }

    machiningBatch.setMachiningBatchStatus(MachiningBatch.MachiningBatchStatus.IDLE);
    machiningBatch.setCreatedAt(LocalDateTime.now());
    return machiningBatch;
  }

  public MachiningBatch assemble(MachiningBatchRepresentation machiningBatchRepresentation) {
    ProcessedItemMachiningBatch processedItemMachiningBatch = null;
    if (machiningBatchRepresentation.getProcessedItemMachiningBatch() != null) {
      processedItemMachiningBatch = processedItemMachiningBatchAssembler
          .assemble(machiningBatchRepresentation.getProcessedItemMachiningBatch());
    }

    return MachiningBatch.builder()
        .machiningBatchNumber(machiningBatchRepresentation.getMachiningBatchNumber())
        .machineSet(machineSetAssembler.assemble(machiningBatchRepresentation.getMachineSet()))
        .machiningBatchStatus(
            machiningBatchRepresentation.getMachiningBatchStatus() != null
            ? MachiningBatch.MachiningBatchStatus.valueOf(machiningBatchRepresentation.getMachiningBatchStatus())
            : null)
        .machiningBatchType(
            machiningBatchRepresentation.getMachiningBatchType() != null
            ? MachiningBatch.MachiningBatchType.valueOf(machiningBatchRepresentation.getMachiningBatchType())
            : null)
        .processedItemHeatTreatmentBatch(
            machiningBatchRepresentation.getProcessedItemHeatTreatmentBatch() != null
            ? processedItemHeatTreatmentBatchAssembler.assemble(machiningBatchRepresentation.getProcessedItemHeatTreatmentBatch())
            : null)
        .processedItemMachiningBatch(processedItemMachiningBatch)
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
    ProcessedItemMachiningBatchRepresentation processedItemMachiningBatchRepresentation = null;
    if (machiningBatch.getProcessedItemMachiningBatch() != null) {
      processedItemMachiningBatchRepresentation =
          processedItemMachiningBatchAssembler.dissemble(machiningBatch.getProcessedItemMachiningBatch());
    }

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
        .processedItemHeatTreatmentBatch(
            machiningBatch.getProcessedItemHeatTreatmentBatch() != null
            ? processedItemHeatTreatmentBatchAssembler.dissemble(machiningBatch.getProcessedItemHeatTreatmentBatch())
            : null)
        .processedItemMachiningBatch(processedItemMachiningBatchRepresentation)
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
