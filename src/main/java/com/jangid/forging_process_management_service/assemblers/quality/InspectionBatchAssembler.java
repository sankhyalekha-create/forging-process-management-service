package com.jangid.forging_process_management_service.assemblers.quality;

import com.jangid.forging_process_management_service.assemblers.machining.ProcessedItemMachiningBatchAssembler;
import com.jangid.forging_process_management_service.entities.machining.ProcessedItemMachiningBatch;
import com.jangid.forging_process_management_service.entities.quality.InspectionBatch;
import com.jangid.forging_process_management_service.entities.quality.ProcessedItemInspectionBatch;
import com.jangid.forging_process_management_service.entitiesRepresentation.quality.InspectionBatchRepresentation;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class InspectionBatchAssembler {


  @Autowired
  private ProcessedItemInspectionBatchAssembler processedItemInspectionBatchAssembler;
  @Autowired
  private ProcessedItemMachiningBatchAssembler processedItemMachiningBatchAssembler;

  /**
   * Converts InspectionBatch to InspectionBatchRepresentation.
   */
  public InspectionBatchRepresentation dissemble(InspectionBatch inspectionBatch) {
    return InspectionBatchRepresentation.builder()
        .id(inspectionBatch.getId())
        .inspectionBatchNumber(inspectionBatch.getInspectionBatchNumber())
        .processedItemInspectionBatch(inspectionBatch.getProcessedItemInspectionBatch() != null
                                      ? processedItemInspectionBatchAssembler.dissemble(inspectionBatch.getProcessedItemInspectionBatch())
                                      : null)
        .processedItemMachiningBatch(inspectionBatch.getInputProcessedItemMachiningBatch() != null
                                     ? processedItemMachiningBatchAssembler.dissemble(inspectionBatch.getInputProcessedItemMachiningBatch())
                                     : null)
        .inspectionBatchStatus(inspectionBatch.getInspectionBatchStatus() != null
                               ? inspectionBatch.getInspectionBatchStatus().name()
                               : null)
        .startAt(inspectionBatch.getStartAt() != null ? inspectionBatch.getStartAt().toString() : null)
        .endAt(inspectionBatch.getEndAt() != null ? inspectionBatch.getEndAt().toString() : null)
        .tenantId(inspectionBatch.getTenant() != null ? inspectionBatch.getTenant().getId() : null)
        .build();
  }

  /**
   * Converts InspectionBatchRepresentation to InspectionBatch.
   */
  public InspectionBatch assemble(InspectionBatchRepresentation inspectionBatchRepresentation) {
    if(inspectionBatchRepresentation != null ){
      ProcessedItemInspectionBatch processedItemInspectionBatch = null;
      if (inspectionBatchRepresentation.getProcessedItemInspectionBatch() != null) {
        processedItemInspectionBatch =processedItemInspectionBatchAssembler.assemble(inspectionBatchRepresentation.getProcessedItemInspectionBatch());
      }

      ProcessedItemMachiningBatch inputProcessedItemMachiningBatch = null;
      if (inspectionBatchRepresentation.getProcessedItemMachiningBatch() != null) {
        inputProcessedItemMachiningBatch = processedItemMachiningBatchAssembler.assemble(inspectionBatchRepresentation.getProcessedItemMachiningBatch());
      }

      return InspectionBatch.builder()
          .inspectionBatchNumber(inspectionBatchRepresentation.getInspectionBatchNumber())
          .processedItemInspectionBatch(processedItemInspectionBatch)
          .inputProcessedItemMachiningBatch(inputProcessedItemMachiningBatch)
          .inspectionBatchStatus(InspectionBatch.InspectionBatchStatus.PENDING)
          .startAt(inspectionBatchRepresentation.getStartAt() != null ? LocalDateTime.parse(inspectionBatchRepresentation.getStartAt()) : null)
          .endAt(inspectionBatchRepresentation.getEndAt() != null ? LocalDateTime.parse(inspectionBatchRepresentation.getEndAt()) : null)
          .build();
    }
    return null;
  }

  /**
   * Creates a new InspectionBatch from a representation with default settings.
   */
  public InspectionBatch createAssemble(InspectionBatchRepresentation inspectionBatchRepresentation) {
    InspectionBatch inspectionBatch = assemble(inspectionBatchRepresentation);
    inspectionBatch.setCreatedAt(LocalDateTime.now());
    return inspectionBatch;
  }
}
