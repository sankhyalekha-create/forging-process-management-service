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

    // Set ProcessedItemMachiningBatch if it is provided in the representation
    if (machiningBatchRepresentation.getProcessedItemMachiningBatch() != null) {
      ProcessedItemMachiningBatch processedItemMachiningBatch =
          processedItemMachiningBatchAssembler.createAssemble(machiningBatchRepresentation.getProcessedItemMachiningBatch());
      machiningBatch.setProcessedItemMachiningBatch(processedItemMachiningBatch);
    }

    // Set inputProcessedItemMachiningBatch if it is provided in the representation
    if (machiningBatchRepresentation.getInputProcessedItemMachiningBatch() != null) {
      ProcessedItemMachiningBatch inputProcessedItemMachiningBatch =
          processedItemMachiningBatchAssembler.createAssemble(machiningBatchRepresentation.getInputProcessedItemMachiningBatch());
      machiningBatch.setInputProcessedItemMachiningBatch(inputProcessedItemMachiningBatch);
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

    // Handle inputProcessedItemMachiningBatch if present in the representation
    ProcessedItemMachiningBatch inputProcessedItemMachiningBatch = null;
    if (machiningBatchRepresentation.getInputProcessedItemMachiningBatch() != null) {
      inputProcessedItemMachiningBatch = processedItemMachiningBatchAssembler
          .assemble(machiningBatchRepresentation.getInputProcessedItemMachiningBatch());
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
        .inputProcessedItemMachiningBatch(inputProcessedItemMachiningBatch)  // Add inputProcessedItemMachiningBatch
        .applyAt(machiningBatchRepresentation.getApplyAt() != null
                 ? LocalDateTime.parse(machiningBatchRepresentation.getApplyAt())
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
    ProcessedItemMachiningBatchRepresentation processedItemMachiningBatchRepresentation = null;
    if (machiningBatch.getProcessedItemMachiningBatch() != null) {
      processedItemMachiningBatchRepresentation =
          processedItemMachiningBatchAssembler.dissemble(machiningBatch.getProcessedItemMachiningBatch());
    }

    // Handle inputProcessedItemMachiningBatch
    ProcessedItemMachiningBatchRepresentation inputProcessedItemMachiningBatchRepresentation = null;
    if (machiningBatch.getInputProcessedItemMachiningBatch() != null) {
      inputProcessedItemMachiningBatchRepresentation =
          processedItemMachiningBatchAssembler.dissemble(machiningBatch.getInputProcessedItemMachiningBatch());
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
        .inputProcessedItemMachiningBatch(inputProcessedItemMachiningBatchRepresentation)  // Add inputProcessedItemMachiningBatch
        .applyAt(
            machiningBatch.getApplyAt() != null
            ? String.valueOf(machiningBatch.getApplyAt())
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
