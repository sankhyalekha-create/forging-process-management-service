package com.jangid.forging_process_management_service.assemblers.quality;

import com.jangid.forging_process_management_service.entities.machining.ProcessedItemMachiningBatch;
import com.jangid.forging_process_management_service.entities.quality.InspectionBatch;
import com.jangid.forging_process_management_service.entitiesRepresentation.machining.ProcessedItemMachiningBatchRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.quality.InspectionBatchRepresentation;
import com.jangid.forging_process_management_service.service.machining.ProcessedItemMachiningBatchService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class InspectionBatchAssembler {

  @Autowired
  private ProcessedItemMachiningBatchService processedItemMachiningBatchService;

  @Autowired
  private ProcessedItemInspectionBatchAssembler processedItemInspectionBatchAssembler;

  /**
   * Converts InspectionBatch to InspectionBatchRepresentation.
   */
  public InspectionBatchRepresentation dissemble(InspectionBatch inspectionBatch) {
    return InspectionBatchRepresentation.builder()
        .id(inspectionBatch.getId())
        .inspectionBatchNumber(inspectionBatch.getInspectionBatchNumber())
        .processedItemMachiningBatch(inspectionBatch.getInputProcessedItemMachiningBatch() != null
                                     ? ProcessedItemMachiningBatchRepresentation.builder()
                                         .id(inspectionBatch.getInputProcessedItemMachiningBatch().getId())
                                         .build()
                                     : null)
        .processedItemInspectionBatch(inspectionBatch.getProcessedItemInspectionBatch() != null
                                      ? processedItemInspectionBatchAssembler.dissemble(inspectionBatch.getProcessedItemInspectionBatch())
                                      : null)
        .inspectionBatchStatus(inspectionBatch.getInspectionBatchStatus() != null
                               ? inspectionBatch.getInspectionBatchStatus().name()
                               : null)
        .startAt(inspectionBatch.getStartAt() != null ? inspectionBatch.getStartAt().toString() : null)
        .endAt(inspectionBatch.getEndAt() != null ? inspectionBatch.getEndAt().toString() : null)
        .build();
  }

  /**
   * Converts InspectionBatchRepresentation to InspectionBatch.
   */
  public InspectionBatch assemble(InspectionBatchRepresentation inspectionBatchRepresentation) {
    if(inspectionBatchRepresentation != null ){
      ProcessedItemMachiningBatch processedItemMachiningBatch = null;
      if (inspectionBatchRepresentation.getProcessedItemMachiningBatch() != null
          && inspectionBatchRepresentation.getProcessedItemMachiningBatch().getId() != null) {
        processedItemMachiningBatch = processedItemMachiningBatchService.getProcessedItemMachiningBatchById(inspectionBatchRepresentation.getProcessedItemMachiningBatch().getId());
      }
      return InspectionBatch.builder()
          .id(inspectionBatchRepresentation.getId())
          .inspectionBatchNumber(inspectionBatchRepresentation.getInspectionBatchNumber())
          .inputProcessedItemMachiningBatch(processedItemMachiningBatch)
          .processedItemInspectionBatch(inspectionBatchRepresentation.getProcessedItemInspectionBatch() != null
                                        ? processedItemInspectionBatchAssembler.assemble(inspectionBatchRepresentation.getProcessedItemInspectionBatch())
                                        : null)
          .inspectionBatchStatus(inspectionBatchRepresentation.getInspectionBatchStatus() != null
                                 ? InspectionBatch.InspectionBatchStatus.valueOf(inspectionBatchRepresentation.getInspectionBatchStatus())
                                 : null)
          .startAt(inspectionBatchRepresentation.getStartAt() != null
                   ? LocalDateTime.parse(inspectionBatchRepresentation.getStartAt())
                   : null)
          .endAt(inspectionBatchRepresentation.getEndAt() != null
                 ? LocalDateTime.parse(inspectionBatchRepresentation.getEndAt())
                 : null)
          .build();
    }
    return null;
  }

  /**
   * Creates a new InspectionBatch from a representation with default settings.
   */
  public InspectionBatch createAssemble(InspectionBatchRepresentation inspectionBatchRepresentation) {
    InspectionBatch inspectionBatch = assemble(inspectionBatchRepresentation);
//    inspectionBatch.setInspectionBatchStatus(InspectionBatch.InspectionBatchStatus.COMPLETED);
    inspectionBatch.setCreatedAt(LocalDateTime.now());
    return inspectionBatch;
  }
}
