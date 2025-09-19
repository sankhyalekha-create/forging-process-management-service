package com.jangid.forging_process_management_service.assemblers;

import com.jangid.forging_process_management_service.assemblers.forging.ForgeAssembler;
import com.jangid.forging_process_management_service.assemblers.product.ItemAssembler;
import com.jangid.forging_process_management_service.entities.ProcessedItem;
import com.jangid.forging_process_management_service.entitiesRepresentation.ProcessedItemRepresentation;
import com.jangid.forging_process_management_service.service.workflow.ItemWorkflowService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class ProcessedItemAssembler {

  @Autowired
  private ForgeAssembler forgeAssembler;
  @Autowired
  private ItemAssembler itemAssembler;
  @Autowired
  private ItemWorkflowService itemWorkflowService;

  public ProcessedItemRepresentation dissemble(ProcessedItem processedItem) {
    Map<String, Object> workflowInfo = null;
    if (processedItem.getItemWorkflowId() != null && processedItem.getId() != null) {
      workflowInfo = itemWorkflowService.getCurrentForgingStepAndNextOperation(
          processedItem.getItemWorkflowId(), processedItem.getId());
    }

    Long itemWorkflowStepId = workflowInfo != null ? (Long) workflowInfo.get("currentStepId") : null;
    List<String> nextOperations = workflowInfo != null ? (List<String>) workflowInfo.get("nextOperations") : null;

    return ProcessedItemRepresentation.builder()
        .id(processedItem.getId())
        .forge(forgeAssembler.dissemble(processedItem.getForge()))
        .item(itemAssembler.dissembleBasic(processedItem.getItem())) // Use dissembleBasic to exclude itemWorkflows
        .expectedForgePiecesCount(processedItem.getExpectedForgePiecesCount())
        .actualForgePiecesCount(processedItem.getActualForgePiecesCount())
//        .availableForgePiecesCountForHeat(processedItem.getAvailableForgePiecesCountForHeat())
        .rejectedForgePiecesCount(processedItem.getRejectedForgePiecesCount())
        .otherForgeRejectionsKg(processedItem.getOtherForgeRejectionsKg())
        .workflowIdentifier(processedItem.getWorkflowIdentifier())
        .itemWorkflowId(processedItem.getItemWorkflowId())
        .itemWorkflowStepId(itemWorkflowStepId)
        .nextOperations(nextOperations)
        .createdAt(processedItem.getCreatedAt() != null ? processedItem.getCreatedAt().toString() : null)
        .updatedAt(processedItem.getUpdatedAt() != null ? processedItem.getUpdatedAt().toString() : null)
        .deletedAt(processedItem.getDeletedAt() != null ? processedItem.getDeletedAt().toString() : null)
        .deleted(processedItem.isDeleted())
        .build();
  }

  public ProcessedItem assemble(ProcessedItemRepresentation processedItemRepresentation) {

    return ProcessedItem.builder()
        .forge(forgeAssembler.assemble(processedItemRepresentation.getForge()))
        .item(itemAssembler.assemble(processedItemRepresentation.getItem()))
        .expectedForgePiecesCount(processedItemRepresentation.getExpectedForgePiecesCount())
        .actualForgePiecesCount(processedItemRepresentation.getActualForgePiecesCount())
        .rejectedForgePiecesCount(processedItemRepresentation.getRejectedForgePiecesCount())
        .otherForgeRejectionsKg(processedItemRepresentation.getOtherForgeRejectionsKg())
        .workflowIdentifier(processedItemRepresentation.getWorkflowIdentifier())
        .itemWorkflowId(processedItemRepresentation.getItemWorkflowId())
        // Note: itemWorkflowStepId and nextOperations are not stored in entity, they are computed dynamically
        .createdAt(processedItemRepresentation.getCreatedAt() != null ? LocalDateTime.parse(processedItemRepresentation.getCreatedAt()) : null)
        .updatedAt(processedItemRepresentation.getUpdatedAt() != null ? LocalDateTime.parse(processedItemRepresentation.getUpdatedAt()) : null)
        .deletedAt(processedItemRepresentation.getDeletedAt() != null ? LocalDateTime.parse(processedItemRepresentation.getDeletedAt()) : null)
        .deleted(processedItemRepresentation.getDeleted())
        .build();
  }

  public ProcessedItem createAssemble(ProcessedItemRepresentation processedItemRepresentation) {
    ProcessedItem processedItem = assemble(processedItemRepresentation);
    processedItem.setCreatedAt(LocalDateTime.now());
    return processedItem;
  }
}
