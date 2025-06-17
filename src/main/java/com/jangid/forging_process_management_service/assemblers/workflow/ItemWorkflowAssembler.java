package com.jangid.forging_process_management_service.assemblers.workflow;

import com.jangid.forging_process_management_service.entities.workflow.ItemWorkflow;
import com.jangid.forging_process_management_service.entities.workflow.ItemWorkflowStep;
import com.jangid.forging_process_management_service.entities.workflow.WorkflowStep;
import com.jangid.forging_process_management_service.entitiesRepresentation.workflow.ItemWorkflowRepresentation;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
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
                                 .sorted(Comparator.comparing(step -> step.getWorkflowStep().getStepOrder()))
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

        // Find current operation (IN_PROGRESS step)
        String currentOperation = null;
        ItemWorkflowStep currentStep = workflow.getItemWorkflowSteps().stream()
            .filter(step -> step.getStepStatus() == ItemWorkflowStep.StepStatus.IN_PROGRESS || step.getStepStatus() == ItemWorkflowStep.StepStatus.COMPLETED)
            .findFirst()
            .orElse(null);
        
        if (currentStep != null) {
            currentOperation = currentStep.getOperationType().name();
        }

        // Find next operation (next PENDING step that can be started)
        String nextOperation = null;
        if (currentStep != null) {
            // Get the current step's order from WorkflowStep
            int currentStepOrder = currentStep.getWorkflowStep().getStepOrder();
            
            // Find the next WorkflowStep in the workflow template based on step order
            ItemWorkflowStep nextStep = workflow.getItemWorkflowSteps().stream()
                .filter(step -> step.getWorkflowStep().getStepOrder() > currentStepOrder)
                .min(Comparator.comparing(step -> step.getWorkflowStep().getStepOrder()))
                .orElse(null);
            
            if (nextStep != null) {
                nextOperation = nextStep.getOperationType().name();
            }
        } else {
            // If no current step, find the first step in the workflow
            ItemWorkflowStep firstStep = workflow.getItemWorkflowSteps().stream()
                .min(Comparator.comparing(step -> step.getWorkflowStep().getStepOrder()))
                .orElse(null);
            
            if (firstStep != null) {
                nextOperation = firstStep.getOperationType().name();
            }
        }
        
        return ItemWorkflowRepresentation.builder()
                .id(workflow.getId())
                .workflowIdentifier(workflow.getWorkflowIdentifier())
                .itemId(workflow.getItem().getId())
                .itemName(workflow.getItem().getItemName())
                .workflowTemplateId(workflow.getWorkflowTemplate().getId())
                .workflowTemplateName(workflow.getWorkflowTemplate().getWorkflowName())
                .workflowStatus(workflow.getWorkflowStatus().name())
                .currentOperation(currentOperation)
                .nextOperation(nextOperation)
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
} 