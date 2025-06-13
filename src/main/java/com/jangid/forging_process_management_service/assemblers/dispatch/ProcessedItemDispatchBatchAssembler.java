package com.jangid.forging_process_management_service.assemblers.dispatch;

import com.jangid.forging_process_management_service.assemblers.forging.ForgeAssembler;
import com.jangid.forging_process_management_service.assemblers.product.ItemAssembler;
import com.jangid.forging_process_management_service.entities.ProcessedItem;
import com.jangid.forging_process_management_service.entities.dispatch.ProcessedItemDispatchBatch;
import com.jangid.forging_process_management_service.entities.product.ItemStatus;
import com.jangid.forging_process_management_service.entitiesRepresentation.ProcessedItemRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.dispatch.ProcessedItemDispatchBatchRepresentation;
import com.jangid.forging_process_management_service.service.ProcessedItemService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class ProcessedItemDispatchBatchAssembler {

  @Autowired
  private ForgeAssembler forgeAssembler;

  @Autowired
  private ItemAssembler itemAssembler;

  @Autowired
  private ProcessedItemService processedItemService;

  /**
   * Converts a ProcessedItemDispatchBatch entity to its representation.
   */
  public ProcessedItemDispatchBatchRepresentation dissemble(ProcessedItemDispatchBatch processedItemDispatchBatch) {
    ProcessedItem processedItem = processedItemDispatchBatch.getProcessedItem();
    ProcessedItemRepresentation processedItemRepresentation = dissemble(processedItem);

    return ProcessedItemDispatchBatchRepresentation.builder()
        .id(processedItemDispatchBatch.getId())
        .processedItem(processedItemRepresentation)
        .totalDispatchPiecesCount(processedItemDispatchBatch.getTotalDispatchPiecesCount())
        .itemStatus(processedItemDispatchBatch.getItemStatus().name())
        .build();
  }

  /**
   * Converts a ProcessedItemDispatchBatchRepresentation to its entity.
   */
  public ProcessedItemDispatchBatch assemble(ProcessedItemDispatchBatchRepresentation representation) {
    ProcessedItem processedItem = representation.getProcessedItem() != null
                                  ? processedItemService.getProcessedItemById(representation.getProcessedItem().getId())
                                  : null;

    return ProcessedItemDispatchBatch.builder()
        .id(representation.getId())
        .processedItem(processedItem)
        .totalDispatchPiecesCount(representation.getTotalDispatchPiecesCount())
        .itemStatus(representation.getItemStatus() != null
                    ? ItemStatus.valueOf(representation.getItemStatus())
                    : null)
        .build();
  }

  /**
   * Creates a new ProcessedItemDispatchBatch from the provided representation.
   */
  public ProcessedItemDispatchBatch createAssemble(ProcessedItemDispatchBatchRepresentation representation) {
    ProcessedItemDispatchBatch processedItemDispatchBatch = assemble(representation);
    processedItemDispatchBatch.setCreatedAt(LocalDateTime.now());
    return processedItemDispatchBatch;
  }

  /**
   * Helper method to convert ProcessedItem entity to its representation.
   */
  private ProcessedItemRepresentation dissemble(ProcessedItem processedItem) {
    return ProcessedItemRepresentation.builder()
        .id(processedItem.getId())
        .forge(forgeAssembler.dissemble(processedItem.getForge()))
        .item(itemAssembler.dissemble(processedItem.getItem()))
        .expectedForgePiecesCount(processedItem.getExpectedForgePiecesCount())
        .actualForgePiecesCount(processedItem.getActualForgePiecesCount())
//        .availableForgePiecesCountForHeat(processedItem.getAvailableForgePiecesCountForHeat())
        .deleted(processedItem.isDeleted())
        .build();
  }
}

