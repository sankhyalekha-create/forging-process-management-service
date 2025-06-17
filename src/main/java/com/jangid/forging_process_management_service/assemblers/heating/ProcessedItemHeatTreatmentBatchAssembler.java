package com.jangid.forging_process_management_service.assemblers.heating;

import com.jangid.forging_process_management_service.assemblers.product.ItemAssembler;
import com.jangid.forging_process_management_service.entities.heating.HeatTreatmentBatch;
import com.jangid.forging_process_management_service.entities.heating.ProcessedItemHeatTreatmentBatch;
import com.jangid.forging_process_management_service.entities.product.Item;
import com.jangid.forging_process_management_service.entities.product.ItemStatus;
import com.jangid.forging_process_management_service.entitiesRepresentation.product.ItemRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.heating.HeatTreatmentBatchRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.heating.ProcessedItemHeatTreatmentBatchRepresentation;
import com.jangid.forging_process_management_service.service.product.ItemService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ProcessedItemHeatTreatmentBatchAssembler {

  @Autowired
  private ItemAssembler itemAssembler;
  @Autowired
  @Lazy
  private ItemService itemService;

  @Autowired
  @Lazy
  private HeatTreatmentHeatAssembler heatTreatmentHeatAssembler;

  public ProcessedItemHeatTreatmentBatchRepresentation dissemble(ProcessedItemHeatTreatmentBatch processedItemHeatTreatmentBatch) {
    Item item = processedItemHeatTreatmentBatch.getItem();
    ItemRepresentation itemRepresentation = itemAssembler.dissemble(item);
    return ProcessedItemHeatTreatmentBatchRepresentation.builder()
        .id(processedItemHeatTreatmentBatch.getId())
        .item(itemRepresentation)
        .heatTreatmentBatch(dissemble(processedItemHeatTreatmentBatch.getHeatTreatmentBatch()))
        .itemStatus(processedItemHeatTreatmentBatch.getItemStatus().name())
        .heatTreatmentHeats(processedItemHeatTreatmentBatch.getHeatTreatmentHeats() != null 
            ? processedItemHeatTreatmentBatch.getHeatTreatmentHeats().stream()
                .map(heatTreatmentHeatAssembler::dissemble)
                .collect(Collectors.toList())
            : null)
        .heatTreatBatchPiecesCount(processedItemHeatTreatmentBatch.getHeatTreatBatchPiecesCount())
        .actualHeatTreatBatchPiecesCount(processedItemHeatTreatmentBatch.getActualHeatTreatBatchPiecesCount())
//        .initialMachiningBatchPiecesCount(processedItemHeatTreatmentBatch.getInitialMachiningBatchPiecesCount())
//        .availableMachiningBatchPiecesCount(processedItemHeatTreatmentBatch.getAvailableMachiningBatchPiecesCount())
        .workflowIdentifier(processedItemHeatTreatmentBatch.getWorkflowIdentifier())
        .itemWorkflowId(processedItemHeatTreatmentBatch.getItemWorkflowId())
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
    Item item = null;
    if (processedItemHeatTreatmentBatchRepresentation.getItem() != null) {
      if (processedItemHeatTreatmentBatchRepresentation.getItem().getId() != null) {
        item = itemService.getItemById(processedItemHeatTreatmentBatchRepresentation.getItem().getId());
      }
    }
    HeatTreatmentBatch heatTreatmentBatch = null;
    if ( processedItemHeatTreatmentBatchRepresentation.getHeatTreatmentBatch() != null) {
      heatTreatmentBatch = assemble(processedItemHeatTreatmentBatchRepresentation.getHeatTreatmentBatch());
    }

    // Build the ProcessedItemHeatTreatmentBatch first without heatTreatmentHeats
    ProcessedItemHeatTreatmentBatch processedItemHeatTreatmentBatch = ProcessedItemHeatTreatmentBatch.builder()
        .id(processedItemHeatTreatmentBatchRepresentation.getId())
        .item(item)
        .heatTreatmentBatch(heatTreatmentBatch)
        .itemStatus(processedItemHeatTreatmentBatchRepresentation.getItemStatus() != null ? ItemStatus.valueOf(processedItemHeatTreatmentBatchRepresentation.getItemStatus()) : null)
        .heatTreatBatchPiecesCount(processedItemHeatTreatmentBatchRepresentation.getHeatTreatBatchPiecesCount())
        .actualHeatTreatBatchPiecesCount(processedItemHeatTreatmentBatchRepresentation.getActualHeatTreatBatchPiecesCount())
//        .initialMachiningBatchPiecesCount(processedItemHeatTreatmentBatchRepresentation.getInitialMachiningBatchPiecesCount())
//        .availableMachiningBatchPiecesCount(processedItemHeatTreatmentBatchRepresentation.getAvailableMachiningBatchPiecesCount())
        .workflowIdentifier(processedItemHeatTreatmentBatchRepresentation.getWorkflowIdentifier())
        .itemWorkflowId(processedItemHeatTreatmentBatchRepresentation.getItemWorkflowId())
        .createdAt(processedItemHeatTreatmentBatchRepresentation.getCreatedAt() != null ? LocalDateTime.parse(processedItemHeatTreatmentBatchRepresentation.getCreatedAt()) : null)
        .updatedAt(processedItemHeatTreatmentBatchRepresentation.getUpdatedAt() != null ? LocalDateTime.parse(processedItemHeatTreatmentBatchRepresentation.getUpdatedAt()) : null)
        .deletedAt(processedItemHeatTreatmentBatchRepresentation.getDeletedAt() != null ? LocalDateTime.parse(processedItemHeatTreatmentBatchRepresentation.getDeletedAt()) : null)
        .deleted(processedItemHeatTreatmentBatchRepresentation.getDeleted() != null ? processedItemHeatTreatmentBatchRepresentation.getDeleted() : false)
        .build();

    // Now convert and set heatTreatmentHeats
    if (processedItemHeatTreatmentBatchRepresentation.getHeatTreatmentHeats() != null) {
      processedItemHeatTreatmentBatch.setHeatTreatmentHeats(
          processedItemHeatTreatmentBatchRepresentation.getHeatTreatmentHeats().stream()
              .map(heatRepresentation -> heatTreatmentHeatAssembler.assemble(heatRepresentation, processedItemHeatTreatmentBatch))
              .collect(Collectors.toList())
      );
    }

    return processedItemHeatTreatmentBatch;
  }

  public ProcessedItemHeatTreatmentBatch createAssemble(ProcessedItemHeatTreatmentBatchRepresentation processedItemHeatTreatmentBatchRepresentation) {
    ProcessedItemHeatTreatmentBatch processedItemHeatTreatmentBatch = assemble(processedItemHeatTreatmentBatchRepresentation);
    processedItemHeatTreatmentBatch.setCreatedAt(LocalDateTime.now());
    return processedItemHeatTreatmentBatch;
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
        .applyAt(heatTreatmentBatch.getApplyAt()!=null?heatTreatmentBatch.getApplyAt().toString():null)
        .startAt(heatTreatmentBatch.getStartAt()!=null?heatTreatmentBatch.getStartAt().toString():null)
        .endAt(heatTreatmentBatch.getEndAt()!=null?heatTreatmentBatch.getEndAt().toString():null)
        .build();
  }
}

