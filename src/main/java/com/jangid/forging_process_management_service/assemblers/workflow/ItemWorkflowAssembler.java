package com.jangid.forging_process_management_service.assemblers.workflow;

import com.jangid.forging_process_management_service.entities.workflow.ItemWorkflow;
import com.jangid.forging_process_management_service.entities.workflow.ItemWorkflowStep;
import com.jangid.forging_process_management_service.entitiesRepresentation.workflow.ItemWorkflowRepresentation;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Slf4j
@Component
public class ItemWorkflowAssembler {

    @Autowired
    private ItemWorkflowStepAssembler itemWorkflowStepAssembler;

    /**
     * Converts ItemWorkflow entity to ItemWorkflowRepresentation
     * Basic conversion with all fields including workflow steps
     */
    public ItemWorkflowRepresentation dissemble(ItemWorkflow workflow) {
        if (workflow == null) {
            return null;
        }

        return ItemWorkflowRepresentation.builder()
                .id(workflow.getId())
                .workflowIdentifier(workflow.getWorkflowIdentifier())
                .itemId(workflow.getItem().getId())
                .itemName(workflow.getItem().getItemName())
                .workflowTemplateId(workflow.getWorkflowTemplate().getId())
                .workflowTemplateName(workflow.getWorkflowTemplate().getWorkflowName())
                .workflowStatus(workflow.getWorkflowStatus().name())
                .startedAt(workflow.getStartedAt() != null ? workflow.getStartedAt().toString() : null)
                .completedAt(workflow.getCompletedAt() != null ? workflow.getCompletedAt().toString() : null)
                .createdAt(workflow.getCreatedAt() != null ? workflow.getCreatedAt().toString() : null)
                .updatedAt(workflow.getUpdatedAt() != null ? workflow.getUpdatedAt().toString() : null)
                .workflowSteps(workflow.getItemWorkflowSteps() != null ? 
                             itemWorkflowStepAssembler.dissemble(workflow.getItemWorkflowSteps()
                                 .stream()
                                 .sorted(this::compareWorkflowSteps)
                                 .collect(Collectors.toList())) : null)
                .build();
    }

    /**
     * Converts ItemWorkflow entity to ItemWorkflowRepresentation for active workflow view
     * Includes current and next operation information
     */
    public ItemWorkflowRepresentation dissembleActiveWorkflow(ItemWorkflow workflow) {
        if (workflow == null) {
            return null;
        }

        return ItemWorkflowRepresentation.builder()
                .id(workflow.getId())
                .workflowIdentifier(workflow.getWorkflowIdentifier())
                .itemId(workflow.getItem().getId())
                .itemName(workflow.getItem().getItemName())
                .workflowTemplateId(workflow.getWorkflowTemplate().getId())
                .workflowTemplateName(workflow.getWorkflowTemplate().getWorkflowName())
                .workflowStatus(workflow.getWorkflowStatus().name())
                .startedAt(workflow.getStartedAt() != null ? workflow.getStartedAt().toString() : null)
                .completedAt(workflow.getCompletedAt() != null ? workflow.getCompletedAt().toString() : null)
                .createdAt(workflow.getCreatedAt() != null ? workflow.getCreatedAt().toString() : null)
                .updatedAt(workflow.getUpdatedAt() != null ? workflow.getUpdatedAt().toString() : null)
                .build();
    }

    /**
     * Converts ItemWorkflowRepresentation to ItemWorkflow entity
     * This would typically be used for creating/updating workflows from API requests
     */
    public ItemWorkflow assemble(ItemWorkflowRepresentation representation) {
        if (representation == null) {
            return null;
        }

        // Note: This is a basic conversion - in practice, you'd need to 
        // resolve Item and WorkflowTemplate entities from their IDs
        return ItemWorkflow.builder()
                .id(representation.getId())
                .workflowIdentifier(representation.getWorkflowIdentifier())
                .workflowStatus(representation.getWorkflowStatus() != null ? 
                    ItemWorkflow.WorkflowStatus.valueOf(representation.getWorkflowStatus()) : 
                    ItemWorkflow.WorkflowStatus.NOT_STARTED)
                .build();
    }

    /**
     * Custom comparator for workflow steps that handles tree-based structure
     * Sorts by tree level first, then by operation type, then by creation time
     */
    private int compareWorkflowSteps(ItemWorkflowStep step1, ItemWorkflowStep step2) {
        // First, sort by tree level (root steps first)
        int level1 = step1.getWorkflowStep().getTreeLevel();
        int level2 = step2.getWorkflowStep().getTreeLevel();
        
        if (level1 != level2) {
            return Integer.compare(level1, level2);
        }
        
        // For steps at the same level, sort by operation type for consistency
        // This gives a predictable ordering within each tree level
        int operationComparison = step1.getOperationType().name().compareTo(step2.getOperationType().name());
        
        if (operationComparison != 0) {
            return operationComparison;
        }
        
        // Finally, sort by creation time for tie-breaking
        return step1.getCreatedAt().compareTo(step2.getCreatedAt());
    }
} 