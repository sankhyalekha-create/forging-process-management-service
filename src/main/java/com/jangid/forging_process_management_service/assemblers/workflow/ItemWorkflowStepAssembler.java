package com.jangid.forging_process_management_service.assemblers.workflow;

import com.jangid.forging_process_management_service.entities.workflow.ItemWorkflowStep;
import com.jangid.forging_process_management_service.entitiesRepresentation.workflow.ItemWorkflowStepRepresentation;
import com.jangid.forging_process_management_service.dto.workflow.OperationOutcomeData;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ItemWorkflowStepAssembler {

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Converts ItemWorkflowStep entity to ItemWorkflowStepRepresentation
     */
    public ItemWorkflowStepRepresentation dissemble(ItemWorkflowStep itemWorkflowStep) {
        if (itemWorkflowStep == null) {
            return null;
        }

        // Convert String JSON to OperationOutcomeData object for better UI performance
        OperationOutcomeData operationOutcomeData = null;
        if (itemWorkflowStep.getOperationOutcomeData() != null && 
            !itemWorkflowStep.getOperationOutcomeData().trim().isEmpty()) {
            try {
                operationOutcomeData = objectMapper.readValue(
                    itemWorkflowStep.getOperationOutcomeData(), 
                    OperationOutcomeData.class
                );
            } catch (Exception e) {
                log.warn("Failed to parse operation outcome data for workflow step {}: {}", 
                        itemWorkflowStep.getId(), e.getMessage());
                // Keep operationOutcomeData as null if parsing fails
            }
        }

        return ItemWorkflowStepRepresentation.builder()
                .id(itemWorkflowStep.getId())
                .itemWorkflowId(itemWorkflowStep.getItemWorkflow() != null ? 
                              itemWorkflowStep.getItemWorkflow().getId() : null)
                .workflowStepId(itemWorkflowStep.getWorkflowStep() != null ? 
                              itemWorkflowStep.getWorkflowStep().getId() : null)
                .parentItemWorkflowStepId(itemWorkflowStep.getParentItemWorkflowStep() != null ? 
                              itemWorkflowStep.getParentItemWorkflowStep().getId() : null)
                .treeLevel(itemWorkflowStep.getItemWorkflowTreeLevel())
                .operationType(itemWorkflowStep.getOperationType() != null ? 
                             itemWorkflowStep.getOperationType().name() : null)
                .stepStatus(itemWorkflowStep.getStepStatus() != null ? 
                          itemWorkflowStep.getStepStatus().name() : null)
                .startedAt(itemWorkflowStep.getStartedAt() != null ? 
                         itemWorkflowStep.getStartedAt().toString() : null)
                .completedAt(itemWorkflowStep.getCompletedAt() != null ? 
                           itemWorkflowStep.getCompletedAt().toString() : null)
                .operationReferenceId(itemWorkflowStep.getOperationReferenceId())
                .operationOutcomeData(operationOutcomeData)
                .relatedEntityIds(itemWorkflowStep.getRelatedEntityIds())
                .initialPiecesCount(itemWorkflowStep.getInitialPiecesCount())
                .piecesAvailableForNext(itemWorkflowStep.getPiecesAvailableForNext())
                .consumedPiecesCount(itemWorkflowStep.getConsumedPiecesCount())
                .piecesUtilizationPercentage(itemWorkflowStep.getPiecesUtilizationPercentage())
                .notes(itemWorkflowStep.getNotes())
                .createdAt(itemWorkflowStep.getCreatedAt() != null ? 
                         itemWorkflowStep.getCreatedAt().toString() : null)
                .updatedAt(itemWorkflowStep.getUpdatedAt() != null ? 
                         itemWorkflowStep.getUpdatedAt().toString() : null)
                // Workflow Step Template Information
                .parentWorkflowStepId(itemWorkflowStep.getWorkflowStep() != null && 
                         itemWorkflowStep.getWorkflowStep().getParentStep() != null ? 
                         itemWorkflowStep.getWorkflowStep().getParentStep().getId() : null)
                .workflowTreeLevel(itemWorkflowStep.getWorkflowStep() != null ? 
                         itemWorkflowStep.getWorkflowStep().getTreeLevel() : null)
                .isOptional(itemWorkflowStep.getWorkflowStep() != null ? 
                          itemWorkflowStep.getWorkflowStep().getIsOptional() : null)
                .stepDescription(itemWorkflowStep.getWorkflowStep() != null ? 
                               itemWorkflowStep.getWorkflowStep().getStepDescription() : null)
                .build();
    }

    /**
     * Converts a list of ItemWorkflowStep entities to representations
     */
    public List<ItemWorkflowStepRepresentation> dissemble(List<ItemWorkflowStep> itemWorkflowSteps) {
        if (itemWorkflowSteps == null) {
            return null;
        }

        return itemWorkflowSteps.stream()
                .map(this::dissemble)
                .collect(Collectors.toList());
    }

    /**
     * Converts ItemWorkflowStepRepresentation to ItemWorkflowStep entity
     * Note: This is a basic conversion - in practice, you'd need to 
     * resolve ItemWorkflow and WorkflowStep entities from their IDs
     */
    public ItemWorkflowStep assemble(ItemWorkflowStepRepresentation representation) {
        if (representation == null) {
            return null;
        }

        // Convert OperationOutcomeData object to String JSON for entity storage
        String operationOutcomeDataJson = null;
        if (representation.getOperationOutcomeData() != null) {
            try {
                operationOutcomeDataJson = objectMapper.writeValueAsString(
                    representation.getOperationOutcomeData()
                );
            } catch (Exception e) {
                log.warn("Failed to serialize operation outcome data for representation {}: {}", 
                        representation.getId(), e.getMessage());
                // Keep operationOutcomeDataJson as null if serialization fails
            }
        }

        return ItemWorkflowStep.builder()
                .id(representation.getId())
                .operationType(representation.getOperationType() != null ? 
                    com.jangid.forging_process_management_service.entities.workflow.WorkflowStep.OperationType.valueOf(representation.getOperationType()) : 
                    null)
                .stepStatus(representation.getStepStatus() != null ? 
                    ItemWorkflowStep.StepStatus.valueOf(representation.getStepStatus()) : 
                    ItemWorkflowStep.StepStatus.PENDING)
                .operationReferenceId(representation.getOperationReferenceId())
                .operationOutcomeData(operationOutcomeDataJson)
                .relatedEntityIds(representation.getRelatedEntityIds())
                .initialPiecesCount(representation.getInitialPiecesCount())
                .piecesAvailableForNext(representation.getPiecesAvailableForNext())
                .notes(representation.getNotes())
                .build();
    }
} 