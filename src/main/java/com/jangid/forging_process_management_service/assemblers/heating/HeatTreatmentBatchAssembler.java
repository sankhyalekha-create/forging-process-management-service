package com.jangid.forging_process_management_service.assemblers.heating;

import com.jangid.forging_process_management_service.assemblers.ProcessedItemAssembler;
import com.jangid.forging_process_management_service.entities.ProcessedItem;
import com.jangid.forging_process_management_service.entities.heating.HeatTreatmentBatch;
import com.jangid.forging_process_management_service.entitiesRepresentation.ProcessedItemRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.heating.HeatTreatmentBatchRepresentation;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
public class HeatTreatmentBatchAssembler {

  @Autowired
  private ProcessedItemAssembler processedItemAssembler;

  public HeatTreatmentBatch createAssemble(HeatTreatmentBatchRepresentation heatTreatmentBatchRepresentation) {
    HeatTreatmentBatch heatTreatmentBatch = assemble(heatTreatmentBatchRepresentation);
    heatTreatmentBatch.setCreatedAt(LocalDateTime.now());
    return heatTreatmentBatch;
  }

  public HeatTreatmentBatch assemble(HeatTreatmentBatchRepresentation heatTreatmentBatchRepresentation) {
    return HeatTreatmentBatch.builder()
        .heatTreatmentBatchNumber(heatTreatmentBatchRepresentation.getHeatTreatmentBatchNumber())
        .heatTreatmentBatchStatus(HeatTreatmentBatch.HeatTreatmentBatchStatus.IDLE)
        .labTestingReport(heatTreatmentBatchRepresentation.getLabTestingReport())
        .labTestingStatus(heatTreatmentBatchRepresentation.getLabTestingStatus())
//        .processedItems(getProcessedItems(heatTreatmentBatchRepresentation.getProcessedItems()))
        .build();
  }

  public HeatTreatmentBatchRepresentation dissemble(HeatTreatmentBatch heatTreatmentBatch){
    return HeatTreatmentBatchRepresentation.builder()
        .id(heatTreatmentBatch.getId())
        .heatTreatmentBatchNumber(heatTreatmentBatch.getHeatTreatmentBatchNumber())
        .furnace(FurnaceAssembler.dissemble(heatTreatmentBatch.getFurnace()))
        .totalWeight(String.valueOf(heatTreatmentBatch.getTotalWeight()))
        .heatTreatmentBatchStatus(heatTreatmentBatch.getHeatTreatmentBatchStatus().name())
        .labTestingStatus(heatTreatmentBatch.getLabTestingStatus())
        .labTestingReport(heatTreatmentBatch.getLabTestingReport())
        .processedItems(getProcessedItemsRepresentation(heatTreatmentBatch.getProcessedItems()))
        .startAt(String.valueOf(heatTreatmentBatch.getStartAt()))
        .endAt(String.valueOf(heatTreatmentBatch.getEndAt()))
        .build();
  }

  private List<ProcessedItemRepresentation> getProcessedItemsRepresentation(List<ProcessedItem> processedItems){
    return processedItems.stream().map(processedItem -> processedItemAssembler.dissemble(processedItem)).toList();
  }

  private List<ProcessedItem> getProcessedItems(List<ProcessedItemRepresentation> processedItems){
    return processedItems.stream().map(processedItem -> processedItemAssembler.assemble(processedItem)).toList();
  }
}
