package com.jangid.forging_process_management_service.assemblers;

import com.jangid.forging_process_management_service.assemblers.forging.ForgeAssembler;
import com.jangid.forging_process_management_service.assemblers.heating.FurnaceAssembler;
import com.jangid.forging_process_management_service.assemblers.product.ItemAssembler;
import com.jangid.forging_process_management_service.entities.ProcessedItem;
import com.jangid.forging_process_management_service.entities.heating.HeatTreatmentBatch;
import com.jangid.forging_process_management_service.entities.product.ItemStatus;
import com.jangid.forging_process_management_service.entitiesRepresentation.ProcessedItemRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.heating.HeatTreatmentBatchRepresentation;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class ProcessedItemAssembler {

  @Autowired
  private ForgeAssembler forgeAssembler;
  @Autowired
  private ItemAssembler itemAssembler;

  public ProcessedItemRepresentation dissemble(ProcessedItem processedItem) {
    HeatTreatmentBatch heatTreatmentBatch = processedItem.getHeatTreatmentBatch();
    HeatTreatmentBatchRepresentation heatTreatmentBatchRepresentation = null;
    if (heatTreatmentBatch != null) {
      heatTreatmentBatchRepresentation = HeatTreatmentBatchRepresentation.builder()
          .id(heatTreatmentBatch.getId())
          .furnace(FurnaceAssembler.dissemble(heatTreatmentBatch.getFurnace()))
          .totalWeight(String.valueOf(heatTreatmentBatch.getTotalWeight()))
          .heatTreatmentBatchStatus(heatTreatmentBatch.getHeatTreatmentBatchStatus().name())
          .labTestingStatus(heatTreatmentBatch.getLabTestingStatus())
          .labTestingReport(heatTreatmentBatch.getLabTestingReport())
          .startAt(String.valueOf(heatTreatmentBatch.getStartAt()))
          .endAt(String.valueOf(heatTreatmentBatch.getEndAt()))
          .build();
    }

    return ProcessedItemRepresentation.builder()
        .id(processedItem.getId())
        .forge(forgeAssembler.dissemble(processedItem.getForge()))
        .item(itemAssembler.dissemble(processedItem.getItem()))
        .itemStatus(processedItem.getItemStatus().name())
        .expectedForgePiecesCount(String.valueOf(processedItem.getExpectedForgePiecesCount()))
        .actualForgePiecesCount(String.valueOf(processedItem.getActualForgePiecesCount()))
        .availableForgePiecesCountForHeat(String.valueOf(processedItem.getAvailableForgePiecesCountForHeat()))
        .heatTreatmentBatch(heatTreatmentBatchRepresentation)
        .heatTreatBatchPiecesCount(String.valueOf(processedItem.getHeatTreatBatchPiecesCount()))
        .actualHeatTreatBatchPiecesCount(String.valueOf(processedItem.getActualHeatTreatBatchPiecesCount()))
        .build();
  }

  public ProcessedItem assemble(ProcessedItemRepresentation processedItemRepresentation) {
    HeatTreatmentBatchRepresentation heatTreatmentBatchRepresentation = processedItemRepresentation.getHeatTreatmentBatch();
    HeatTreatmentBatch heatTreatmentBatch = null;
    if (heatTreatmentBatchRepresentation != null) {
      heatTreatmentBatch = HeatTreatmentBatch.builder()
          .heatTreatmentBatchStatus(HeatTreatmentBatch.HeatTreatmentBatchStatus.IDLE)
          .labTestingReport(heatTreatmentBatchRepresentation.getLabTestingReport())
          .labTestingStatus(heatTreatmentBatchRepresentation.getLabTestingStatus())
          .build();
    }
    return ProcessedItem.builder()
        .forge(forgeAssembler.assemble(processedItemRepresentation.getForge()))
        .item(itemAssembler.assemble(processedItemRepresentation.getItem()))
        .expectedForgePiecesCount(processedItemRepresentation.getExpectedForgePiecesCount() != null ? Integer.valueOf(processedItemRepresentation.getExpectedForgePiecesCount()) : null)
        .actualForgePiecesCount(processedItemRepresentation.getActualForgePiecesCount() != null ? Integer.valueOf(processedItemRepresentation.getActualForgePiecesCount()) : null)
        .availableForgePiecesCountForHeat(
            processedItemRepresentation.getAvailableForgePiecesCountForHeat() != null ? Integer.valueOf(processedItemRepresentation.getAvailableForgePiecesCountForHeat()) : null)
        .heatTreatmentBatch(heatTreatmentBatch)
        .itemStatus(processedItemRepresentation.getItemStatus() != null ? ItemStatus.valueOf(processedItemRepresentation.getItemStatus()) : null)
        .build();
  }

  public ProcessedItem createAssemble(ProcessedItemRepresentation processedItemRepresentation) {
    ProcessedItem processedItem = assemble(processedItemRepresentation);
    processedItem.setCreatedAt(LocalDateTime.now());
    return processedItem;
  }
}
