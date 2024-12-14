//package com.jangid.forging_process_management_service.assemblers.heating;
//
//import com.jangid.forging_process_management_service.entities.heating.BatchItemSelection;
//import com.jangid.forging_process_management_service.entities.heating.HeatTreatmentBatch;
//import com.jangid.forging_process_management_service.entitiesRepresentation.heating.BatchItemSelectionRepresentation;
//import com.jangid.forging_process_management_service.entitiesRepresentation.heating.HeatTreatmentBatchRepresentation;
//
//import lombok.extern.slf4j.Slf4j;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//
//import java.time.LocalDateTime;
//import java.util.List;
//
//@Slf4j
//@Component
//public class HeatTreatmentBatchAssembler {
//
//  @Autowired
//  private BatchItemSelectionAssembler batchItemSelectionAssembler;
//
//  public HeatTreatmentBatch createAssemble(HeatTreatmentBatchRepresentation heatTreatmentBatchRepresentation) {
//    return HeatTreatmentBatch.builder()
//        .heatTreatmentBatchStatus(HeatTreatmentBatch.HeatTreatmentBatchStatus.IDLE)
//        .labTestingReport(heatTreatmentBatchRepresentation.getLabTestingReport())
//        .labTestingStatus(heatTreatmentBatchRepresentation.getLabTestingStatus())
//        .createdAt(LocalDateTime.now())
//        .build();
//  }
//
//  public HeatTreatmentBatchRepresentation dissemble(HeatTreatmentBatch heatTreatmentBatch){
//    return HeatTreatmentBatchRepresentation.builder()
//        .id(heatTreatmentBatch.getId())
//        .furnace(FurnaceAssembler.dissemble(heatTreatmentBatch.getFurnace()))
//        .totalWeight(String.valueOf(heatTreatmentBatch.getTotalWeight()))
//        .heatTreatmentBatchStatus(heatTreatmentBatch.getHeatTreatmentBatchStatus().name())
//        .labTestingStatus(heatTreatmentBatch.getLabTestingStatus())
//        .labTestingReport(heatTreatmentBatch.getLabTestingReport())
//        .batchItems(getBatchItemsRepresentation(heatTreatmentBatch.getBatchItems()))
//        .startAt(String.valueOf(heatTreatmentBatch.getStartAt()))
//        .endAt(String.valueOf(heatTreatmentBatch.getEndAt()))
//        .build();
//  }
//
//  private List<BatchItemSelectionRepresentation> getBatchItemsRepresentation(List<BatchItemSelection> batchItemSelections){
//    return batchItemSelections.stream().map(batchItemSelection -> batchItemSelectionAssembler.dissemble(batchItemSelection)).toList();
//  }
//}
