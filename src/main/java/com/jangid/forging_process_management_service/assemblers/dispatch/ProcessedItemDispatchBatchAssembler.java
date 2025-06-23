package com.jangid.forging_process_management_service.assemblers.dispatch;

import com.jangid.forging_process_management_service.assemblers.product.ItemAssembler;
import com.jangid.forging_process_management_service.entities.product.Item;
import com.jangid.forging_process_management_service.entities.dispatch.ProcessedItemDispatchBatch;
import com.jangid.forging_process_management_service.entitiesRepresentation.product.ItemRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.dispatch.ProcessedItemDispatchBatchRepresentation;
import com.jangid.forging_process_management_service.service.product.ItemService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ProcessedItemDispatchBatchAssembler {

  @Autowired
  private ItemAssembler itemAssembler;

  @Autowired
  @Lazy
  private ItemService itemService;

  @Autowired
  @Lazy
  private DispatchHeatAssembler dispatchHeatAssembler;

  /**
   * Converts a ProcessedItemDispatchBatch entity to its representation.
   */
  public ProcessedItemDispatchBatchRepresentation dissemble(ProcessedItemDispatchBatch processedItemDispatchBatch) {
    Item item = processedItemDispatchBatch.getItem();
    ItemRepresentation itemRepresentation = itemAssembler.dissemble(item);

    return ProcessedItemDispatchBatchRepresentation.builder()
        .id(processedItemDispatchBatch.getId())
        .item(itemRepresentation)
        .dispatchHeats(processedItemDispatchBatch.getDispatchHeats() != null 
            ? processedItemDispatchBatch.getDispatchHeats().stream()
                .map(dispatchHeatAssembler::dissemble)
                .collect(Collectors.toList())
            : null)
        .totalDispatchPiecesCount(processedItemDispatchBatch.getTotalDispatchPiecesCount())
        .workflowIdentifier(processedItemDispatchBatch.getWorkflowIdentifier())
        .itemWorkflowId(processedItemDispatchBatch.getItemWorkflowId())
        .previousOperationProcessedItemId(processedItemDispatchBatch.getPreviousOperationProcessedItemId())
        .dispatchHeats(processedItemDispatchBatch.getDispatchHeats() != null 
            ? processedItemDispatchBatch.getDispatchHeats().stream()
                .map(dispatchHeatAssembler::dissemble)
                .collect(Collectors.toList())
            : null)
        .build();
  }

  /**
   * Converts a ProcessedItemDispatchBatchRepresentation to its entity.
   */
  public ProcessedItemDispatchBatch assemble(ProcessedItemDispatchBatchRepresentation representation) {
    Item item = null;
    if (representation.getItem() != null) {
      if (representation.getItem().getId() != null) {
        item = itemService.getItemById(representation.getItem().getId());
      }
    }

    ProcessedItemDispatchBatch processedItemDispatchBatch = ProcessedItemDispatchBatch.builder()
        .id(representation.getId())
        .item(item)
        .totalDispatchPiecesCount(representation.getTotalDispatchPiecesCount())
        .dispatchHeats(representation.getDispatchHeats() != null 
            ? representation.getDispatchHeats().stream()
                .map(dispatchHeatAssembler::assemble)
                .collect(Collectors.toList())
            : null)
        .workflowIdentifier(representation.getWorkflowIdentifier())
        .itemWorkflowId(representation.getItemWorkflowId())
        .previousOperationProcessedItemId(representation.getPreviousOperationProcessedItemId())
        .build();
    if (processedItemDispatchBatch.getDispatchHeats() != null) {
      processedItemDispatchBatch.getDispatchHeats().forEach(dispatchHeat -> {
        dispatchHeat.setProcessedItemDispatchBatch(processedItemDispatchBatch);
      });
    }
    return processedItemDispatchBatch;
  }

  /**
   * Creates a new ProcessedItemDispatchBatch from the provided representation.
   */
  public ProcessedItemDispatchBatch createAssemble(ProcessedItemDispatchBatchRepresentation representation) {
    ProcessedItemDispatchBatch processedItemDispatchBatch = assemble(representation);
    processedItemDispatchBatch.setCreatedAt(LocalDateTime.now());
    return processedItemDispatchBatch;
  }
}

