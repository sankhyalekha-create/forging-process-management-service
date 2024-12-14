//package com.jangid.forging_process_management_service.assemblers.heating;
//
//import com.jangid.forging_process_management_service.assemblers.forging.ForgeAssembler;
//import com.jangid.forging_process_management_service.entities.forging.Forge;
//import com.jangid.forging_process_management_service.entities.heating.BatchItemSelection;
//import com.jangid.forging_process_management_service.entitiesRepresentation.heating.BatchItemSelectionRepresentation;
//import com.jangid.forging_process_management_service.service.forging.ForgeService;
//
//import lombok.extern.slf4j.Slf4j;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//
//import java.time.LocalDateTime;
//
//@Slf4j
//@Component
//public class BatchItemSelectionAssembler {
//
//  @Autowired
//  private ForgeService forgeService;
//
//  @Autowired
//  private ForgeAssembler forgeAssembler;
//
//  public BatchItemSelection createAssemble(BatchItemSelectionRepresentation batchItemSelectionRepresentation){
//    Forge existingForge = forgeService.getForgeByForgeTraceabilityNumber(batchItemSelectionRepresentation.getForge().getForgeTraceabilityNumber());
//    return BatchItemSelection.builder()
//        .forge(existingForge)
//        .availableForgedPiecesCount(existingForge.getAvailableForgedPiecesCount())
//        .heatTreatBatchPiecesCount(Integer.valueOf(batchItemSelectionRepresentation.getHeatTreatBatchPiecesCount()))
//        .createdAt(LocalDateTime.now())
//        .build();
//  }
//
//  public BatchItemSelectionRepresentation dissemble(BatchItemSelection batchItemSelection){
//    return BatchItemSelectionRepresentation.builder()
//        .id(batchItemSelection.getId())
//        .heatTreatBatchPiecesCount(String.valueOf(batchItemSelection.getHeatTreatBatchPiecesCount()))
//        .forge(forgeAssembler.dissemble(batchItemSelection.getForge()))
//        .heatTreatBatchId(batchItemSelection.getHeatTreatmentBatch().getId())
//        .build();
//  }
//
//}
