package com.jangid.forging_process_management_service.assemblers.dispatch;

import com.jangid.forging_process_management_service.assemblers.quality.ProcessedItemInspectionBatchAssembler;
import com.jangid.forging_process_management_service.entities.dispatch.DispatchProcessedItemInspection;
import com.jangid.forging_process_management_service.entitiesRepresentation.dispatch.DispatchProcessedItemInspectionRepresentation;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DispatchProcessedItemInspectionAssembler {

  private final DispatchBatchAssembler dispatchBatchAssembler;

  private final ProcessedItemInspectionBatchAssembler processedItemInspectionBatchAssembler;

  @Autowired
  public DispatchProcessedItemInspectionAssembler(@Lazy DispatchBatchAssembler dispatchBatchAssembler, ProcessedItemInspectionBatchAssembler processedItemInspectionBatchAssembler) {
    this.dispatchBatchAssembler = dispatchBatchAssembler;
    this.processedItemInspectionBatchAssembler = processedItemInspectionBatchAssembler;
  }

  public DispatchProcessedItemInspectionRepresentation dissemble(DispatchProcessedItemInspection dispatchProcessedItemInspection) {
    if (dispatchProcessedItemInspection == null) {
      return null;
    }

    return DispatchProcessedItemInspectionRepresentation.builder()
        .id(dispatchProcessedItemInspection.getId())
//        .dispatchBatch(dispatchProcessedItemInspection.getDispatchBatch() != null ?
//                       dissemble(dispatchProcessedItemInspection.getDispatchBatch()) : null)
        .processedItemInspectionBatch(dispatchProcessedItemInspection.getProcessedItemInspectionBatch() != null ?
                                      processedItemInspectionBatchAssembler.dissemble(dispatchProcessedItemInspection.getProcessedItemInspectionBatch()) : null)
        .dispatchedPiecesCount(dispatchProcessedItemInspection.getDispatchedPiecesCount())
        .build();
  }

  public DispatchProcessedItemInspection assemble(DispatchProcessedItemInspectionRepresentation dispatchProcessedItemInspectionRepresentation) {
    if (dispatchProcessedItemInspectionRepresentation == null) {
      return null;
    }

    DispatchProcessedItemInspection dispatchProcessedItemInspection = new DispatchProcessedItemInspection();
    dispatchProcessedItemInspection.setId(dispatchProcessedItemInspectionRepresentation.getId());
    dispatchProcessedItemInspection.setDispatchedPiecesCount(dispatchProcessedItemInspectionRepresentation.getProcessedItemInspectionBatch().getSelectedDispatchPiecesCount());

    if (dispatchProcessedItemInspectionRepresentation.getDispatchBatch() != null) {
      dispatchProcessedItemInspection.setDispatchBatch(dispatchBatchAssembler.assemble(dispatchProcessedItemInspectionRepresentation.getDispatchBatch()));
    }

    if (dispatchProcessedItemInspectionRepresentation.getProcessedItemInspectionBatch() != null) {
      dispatchProcessedItemInspection.setProcessedItemInspectionBatch(processedItemInspectionBatchAssembler.assemble(dispatchProcessedItemInspectionRepresentation.getProcessedItemInspectionBatch()));
    }

    return dispatchProcessedItemInspection;
  }

}

