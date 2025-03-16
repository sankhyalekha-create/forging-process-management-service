package com.jangid.forging_process_management_service.assemblers.dispatch;

import com.jangid.forging_process_management_service.assemblers.quality.ProcessedItemInspectionBatchAssembler;
import com.jangid.forging_process_management_service.entities.dispatch.DispatchBatch;
import com.jangid.forging_process_management_service.entities.dispatch.ProcessedItemDispatchBatch;
import com.jangid.forging_process_management_service.entitiesRepresentation.dispatch.DispatchBatchRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.dispatch.ProcessedItemDispatchBatchRepresentation;
import com.jangid.forging_process_management_service.service.dispatch.ProcessedItemDispatchBatchService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.stream.Collectors;

@Slf4j
@Component
public class DispatchBatchAssembler {

  @Autowired
  private ProcessedItemDispatchBatchService processedItemDispatchBatchService;

  @Autowired
  private DispatchProcessedItemInspectionAssembler dispatchProcessedItemInspectionAssembler;
  @Autowired
  private ProcessedItemDispatchBatchAssembler processedItemDispatchBatchAssembler;

  /**
   * Converts DispatchBatch to DispatchBatchRepresentation.
   */
  public DispatchBatchRepresentation dissemble(DispatchBatch dispatchBatch) {
    return DispatchBatchRepresentation.builder()
        .id(dispatchBatch.getId())
        .dispatchBatchNumber(dispatchBatch.getDispatchBatchNumber())
        .dispatchProcessedItemInspections(dispatchBatch.getDispatchProcessedItemInspections() != null
                                        ? dispatchBatch.getDispatchProcessedItemInspections().stream()
                                            .map(dispatchProcessedItemInspectionAssembler::dissemble)
                                            .collect(Collectors.toList())
                                        : new ArrayList<>())
        .processedItemDispatchBatch(dispatchBatch.getProcessedItemDispatchBatch() != null
                                    ? processedItemDispatchBatchAssembler.dissemble(dispatchBatch.getProcessedItemDispatchBatch())
                                    : null)
        .dispatchBatchStatus(dispatchBatch.getDispatchBatchStatus() != null
                             ? dispatchBatch.getDispatchBatchStatus().name()
                             : null)
        .dispatchCreatedAt(dispatchBatch.getDispatchCreatedAt() != null ? dispatchBatch.getDispatchCreatedAt().toString() : null)
        .dispatchReadyAt(dispatchBatch.getDispatchReadyAt() != null ? dispatchBatch.getDispatchReadyAt().toString() : null)
        .dispatchedAt(dispatchBatch.getDispatchedAt() != null ? dispatchBatch.getDispatchedAt().toString() : null)
        .build();
  }

  /**
   * Converts DispatchBatchRepresentation to DispatchBatch.
   */
  public DispatchBatch assemble(DispatchBatchRepresentation dispatchBatchRepresentation) {
    if (dispatchBatchRepresentation != null) {
      ProcessedItemDispatchBatch processedItemDispatchBatch = null;
      if (dispatchBatchRepresentation.getProcessedItemDispatchBatch() != null
          && dispatchBatchRepresentation.getProcessedItemDispatchBatch().getId() != null) {
        processedItemDispatchBatch = processedItemDispatchBatchService.getProcessedItemDispatchBatchById(
            dispatchBatchRepresentation.getProcessedItemDispatchBatch().getId());
      }
      return DispatchBatch.builder()
          .id(dispatchBatchRepresentation.getId())
          .dispatchBatchNumber(dispatchBatchRepresentation.getDispatchBatchNumber())
          .dispatchProcessedItemInspections(dispatchBatchRepresentation.getDispatchProcessedItemInspections() != null
                                          ? dispatchBatchRepresentation.getDispatchProcessedItemInspections().stream()
                                              .map(dispatchProcessedItemInspectionAssembler::assemble)
                                              .collect(Collectors.toList())
                                          : new ArrayList<>())
          .processedItemDispatchBatch(processedItemDispatchBatch)
          .dispatchBatchStatus(dispatchBatchRepresentation.getDispatchBatchStatus() != null
                               ? DispatchBatch.DispatchBatchStatus.valueOf(dispatchBatchRepresentation.getDispatchBatchStatus())
                               : null)
          .dispatchCreatedAt(dispatchBatchRepresentation.getDispatchCreatedAt() != null
                           ? LocalDateTime.parse(dispatchBatchRepresentation.getDispatchCreatedAt())
                           : null)
          .dispatchReadyAt(dispatchBatchRepresentation.getDispatchReadyAt() != null
                           ? LocalDateTime.parse(dispatchBatchRepresentation.getDispatchReadyAt())
                           : null)
          .dispatchedAt(dispatchBatchRepresentation.getDispatchedAt() != null
                        ? LocalDateTime.parse(dispatchBatchRepresentation.getDispatchedAt())
                        : null)
          .build();
    }
    return null;
  }

  /**
   * Creates a new DispatchBatch from a representation with default settings.
   */
  public DispatchBatch createAssemble(DispatchBatchRepresentation dispatchBatchRepresentation) {
    DispatchBatch dispatchBatch = assemble(dispatchBatchRepresentation);
    dispatchBatch.setCreatedAt(LocalDateTime.now());
    dispatchBatch.getDispatchProcessedItemInspections().forEach(dispatchProcessedItemInspection -> dispatchProcessedItemInspection.setCreatedAt(LocalDateTime.now()));
    return dispatchBatch;
  }
}

