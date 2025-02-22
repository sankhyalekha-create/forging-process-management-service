package com.jangid.forging_process_management_service.assemblers.heating;

import com.jangid.forging_process_management_service.assemblers.forging.ForgeAssembler;
import com.jangid.forging_process_management_service.assemblers.product.ItemAssembler;
import com.jangid.forging_process_management_service.entities.ProcessedItem;
import com.jangid.forging_process_management_service.entities.heating.HeatTreatmentBatch;
import com.jangid.forging_process_management_service.entities.heating.ProcessedItemHeatTreatmentBatch;
import com.jangid.forging_process_management_service.entities.product.ItemStatus;
import com.jangid.forging_process_management_service.entitiesRepresentation.ProcessedItemRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.heating.HeatTreatmentBatchRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.heating.ProcessedItemHeatTreatmentBatchRepresentation;
import com.jangid.forging_process_management_service.service.ProcessedItemService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class ProcessedItemHeatTreatmentBatchAssembler {

  @Autowired
  private ForgeAssembler forgeAssembler;
  @Autowired
  private ItemAssembler itemAssembler;
  @Autowired
  private ProcessedItemService processedItemService;

  public ProcessedItemHeatTreatmentBatchRepresentation dissemble(ProcessedItemHeatTreatmentBatch processedItemHeatTreatmentBatch) {
    ProcessedItem processedItem = processedItemHeatTreatmentBatch.getProcessedItem();
    ProcessedItemRepresentation processedItemRepresentation = dissemble(processedItem);
    return ProcessedItemHeatTreatmentBatchRepresentation.builder()
        .id(processedItemHeatTreatmentBatch.getId())
        .processedItem(processedItemRepresentation)
        .heatTreatmentBatch(dissemble(processedItemHeatTreatmentBatch.getHeatTreatmentBatch()))
        .itemStatus(processedItemHeatTreatmentBatch.getItemStatus().name())
        .heatTreatBatchPiecesCount(processedItemHeatTreatmentBatch.getHeatTreatBatchPiecesCount())
        .actualHeatTreatBatchPiecesCount(processedItemHeatTreatmentBatch.getActualHeatTreatBatchPiecesCount())
        .initialMachiningBatchPiecesCount(processedItemHeatTreatmentBatch.getInitialMachiningBatchPiecesCount())
        .availableMachiningBatchPiecesCount(processedItemHeatTreatmentBatch.getAvailableMachiningBatchPiecesCount())
        .createdAt(processedItemHeatTreatmentBatch.getCreatedAt() != null ? String.valueOf(processedItemHeatTreatmentBatch.getCreatedAt()) : null)
        .updatedAt(processedItemHeatTreatmentBatch.getUpdatedAt() != null ? String.valueOf(processedItemHeatTreatmentBatch.getUpdatedAt()) : null)
        .deletedAt(processedItemHeatTreatmentBatch.getDeletedAt() != null ? String.valueOf(processedItemHeatTreatmentBatch.getDeletedAt()) : null)
        .deleted(processedItemHeatTreatmentBatch.isDeleted())
        .build();
  }

  public ProcessedItemHeatTreatmentBatch assemble(ProcessedItemHeatTreatmentBatchRepresentation processedItemHeatTreatmentBatchRepresentation) {
    if(processedItemHeatTreatmentBatchRepresentation == null){
      return null;
    }
    ProcessedItem processedItem = null;
    if (processedItemHeatTreatmentBatchRepresentation.getProcessedItem() != null) {
      processedItem = processedItemService.getProcessedItemById(processedItemHeatTreatmentBatchRepresentation.getProcessedItem().getId());

    }
    HeatTreatmentBatch heatTreatmentBatch = null;
    if ( processedItemHeatTreatmentBatchRepresentation.getHeatTreatmentBatch() != null) {
      heatTreatmentBatch = assemble(processedItemHeatTreatmentBatchRepresentation.getHeatTreatmentBatch());
    }

    return ProcessedItemHeatTreatmentBatch.builder()
        .id(processedItemHeatTreatmentBatchRepresentation.getId())
        .processedItem(processedItem)
        .heatTreatmentBatch(heatTreatmentBatch)
        .itemStatus(processedItemHeatTreatmentBatchRepresentation.getItemStatus() != null ? ItemStatus.valueOf(processedItemHeatTreatmentBatchRepresentation.getItemStatus()) : null)
        .heatTreatBatchPiecesCount(processedItemHeatTreatmentBatchRepresentation.getHeatTreatBatchPiecesCount())
        .actualHeatTreatBatchPiecesCount(processedItemHeatTreatmentBatchRepresentation.getActualHeatTreatBatchPiecesCount())
        .initialMachiningBatchPiecesCount(processedItemHeatTreatmentBatchRepresentation.getInitialMachiningBatchPiecesCount())
        .availableMachiningBatchPiecesCount(processedItemHeatTreatmentBatchRepresentation.getAvailableMachiningBatchPiecesCount())
        .createdAt(processedItemHeatTreatmentBatchRepresentation.getCreatedAt() != null ? LocalDateTime.parse(processedItemHeatTreatmentBatchRepresentation.getCreatedAt()) : null)
        .updatedAt(processedItemHeatTreatmentBatchRepresentation.getUpdatedAt() != null ? LocalDateTime.parse(processedItemHeatTreatmentBatchRepresentation.getUpdatedAt()) : null)
        .deletedAt(processedItemHeatTreatmentBatchRepresentation.getDeletedAt() != null ? LocalDateTime.parse(processedItemHeatTreatmentBatchRepresentation.getDeletedAt()) : null)
        .deleted(processedItemHeatTreatmentBatchRepresentation.getDeleted() != null ? processedItemHeatTreatmentBatchRepresentation.getDeleted() : false)
        .build();
  }

  public ProcessedItemHeatTreatmentBatch createAssemble(ProcessedItemHeatTreatmentBatchRepresentation processedItemHeatTreatmentBatchRepresentation) {
    ProcessedItemHeatTreatmentBatch processedItemHeatTreatmentBatch = assemble(processedItemHeatTreatmentBatchRepresentation);
    processedItemHeatTreatmentBatch.setCreatedAt(LocalDateTime.now());
    return processedItemHeatTreatmentBatch;
  }

  public ProcessedItemRepresentation dissemble(ProcessedItem processedItem) {

    return ProcessedItemRepresentation.builder()
        .id(processedItem.getId())
        .forge(forgeAssembler.dissemble(processedItem.getForge()))
        .item(itemAssembler.dissemble(processedItem.getItem()))
        .expectedForgePiecesCount(processedItem.getExpectedForgePiecesCount())
        .actualForgePiecesCount(processedItem.getActualForgePiecesCount())
        .availableForgePiecesCountForHeat(processedItem.getAvailableForgePiecesCountForHeat())
        .createdAt(processedItem.getCreatedAt() != null ? processedItem.getCreatedAt().toString() : null)
        .updatedAt(processedItem.getUpdatedAt() != null ? processedItem.getUpdatedAt().toString() : null)
        .deletedAt(processedItem.getDeletedAt() != null ? processedItem.getDeletedAt().toString() : null)
        .deleted(processedItem.isDeleted())
        .build();
  }

  public HeatTreatmentBatch assemble(HeatTreatmentBatchRepresentation heatTreatmentBatchRepresentation) {
    HeatTreatmentBatch heatTreatmentBatch = null;
    if (heatTreatmentBatchRepresentation != null) {
      heatTreatmentBatch = HeatTreatmentBatch.builder()
          .heatTreatmentBatchNumber(heatTreatmentBatchRepresentation.getHeatTreatmentBatchNumber())
          .labTestingReport(heatTreatmentBatchRepresentation.getLabTestingReport())
          .labTestingStatus(heatTreatmentBatchRepresentation.getLabTestingStatus())
//        .processedItemHeatTreatmentBatches(getProcessedItemHeatTreatmentBatches(heatTreatmentBatchRepresentation.getProcessedItemHeatTreatmentBatches()))
//        .startAt(parseDate(heatTreatmentBatchRepresentation.getStartAt()))
//        .endAt(parseDate(heatTreatmentBatchRepresentation.getEndAt()))
          .build();

      // Calculate total weight after assembly
      heatTreatmentBatch.calculateTotalWeight();
    }

    return heatTreatmentBatch;
  }

  public HeatTreatmentBatchRepresentation dissemble(HeatTreatmentBatch heatTreatmentBatch) {
    return HeatTreatmentBatchRepresentation.builder()
        .id(heatTreatmentBatch.getId())
        .heatTreatmentBatchNumber(heatTreatmentBatch.getHeatTreatmentBatchNumber())
        .totalWeight(String.valueOf(heatTreatmentBatch.getTotalWeight()))
        .heatTreatmentBatchStatus(heatTreatmentBatch.getHeatTreatmentBatchStatus().name())
        .labTestingReport(heatTreatmentBatch.getLabTestingReport())
        .labTestingStatus(heatTreatmentBatch.getLabTestingStatus())
        .startAt(heatTreatmentBatch.getStartAt()!=null?heatTreatmentBatch.getStartAt().toString():null)
        .endAt(heatTreatmentBatch.getEndAt()!=null?heatTreatmentBatch.getEndAt().toString():null)
        .build();
  }
}

