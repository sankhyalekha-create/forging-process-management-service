package com.jangid.forging_process_management_service.service.workflow;

import com.jangid.forging_process_management_service.entities.Tenant;
import com.jangid.forging_process_management_service.entities.workflow.ItemWorkflow;
import com.jangid.forging_process_management_service.entities.workflow.WorkflowStep;
import com.jangid.forging_process_management_service.entities.workflow.WorkflowTemplate;
import com.jangid.forging_process_management_service.repositories.workflow.ItemWorkflowRepository;
import com.jangid.forging_process_management_service.repositories.workflow.WorkflowTemplateRepository;
import com.jangid.forging_process_management_service.exception.ValidationException;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
public class WorkflowTemplateService {

    @Autowired
    private WorkflowTemplateRepository workflowTemplateRepository;
    
    @Lazy
    @Autowired
    private ItemWorkflowRepository itemWorkflowRepository;

    /**
     * Create a custom tree-based workflow template
     */
    @Transactional
    public WorkflowTemplate createCustomWorkflowTemplate(String name, String description, 
                                                         List<WorkflowStepDefinition> stepDefinitions,
                                                         Tenant tenant) {
        log.info("Creating custom workflow template: {} for tenant: {}", name, tenant.getId());
        
        WorkflowTemplate template = WorkflowTemplate.builder()
            .workflowName(name)
            .workflowDescription(description)
            .tenant(tenant)
            .isActive(true)
            .build();

        // Create all steps first
        List<WorkflowStep> steps = new ArrayList<>();
        for (WorkflowStepDefinition def : stepDefinitions) {
            WorkflowStep step = WorkflowStep.builder()
                .workflowTemplate(template)
                .operationType(def.getOperationType())
                .stepName(def.getStepName())
                .stepDescription(def.getStepDescription())
                // stepOrder removed in favor of parent-child tree structure
                .isOptional(def.isOptional())
                .build();
            steps.add(step);
            template.addWorkflowStep(step);
        }

        // Build parent-child relationships
        for (int i = 0; i < stepDefinitions.size(); i++) {
            WorkflowStepDefinition def = stepDefinitions.get(i);
            WorkflowStep step = steps.get(i);
            
            if (def.getParentStepIndex() != null && def.getParentStepIndex() >= 0) {
                WorkflowStep parentStep = steps.get(def.getParentStepIndex());
                step.setParentStep(parentStep);
                parentStep.addChildStep(step);
            }
        }

        validateWorkflowTemplate(template);
        
        WorkflowTemplate savedTemplate = workflowTemplateRepository.save(template);
        log.info("Successfully created custom workflow template with ID: {}", savedTemplate.getId());
        
        return savedTemplate;
    }

    /**
     * Validate workflow template structure
     */
    private void validateWorkflowTemplate(WorkflowTemplate template) {
        if (template == null) {
            throw new ValidationException("Workflow template cannot be null");
        }

        if (template.getWorkflowSteps() == null || template.getWorkflowSteps().isEmpty()) {
            throw new ValidationException("Workflow template must have at least one step");
        }

        // Validate tree structure
        if (!template.isValidWorkflowTree()) {
            throw new ValidationException("Invalid workflow tree structure detected");
        }

        // Check for at least one root step
        List<WorkflowStep> rootSteps = template.getRootSteps();
        if (rootSteps.isEmpty()) {
            throw new ValidationException("Workflow template must have at least one root step");
        }

        // FORGING-specific validation
        List<WorkflowStep> forgingSteps = template.getWorkflowSteps().stream()
            .filter(step -> !step.getDeleted() && step.getOperationType() == WorkflowStep.OperationType.FORGING)
            .toList();
            
        if (!forgingSteps.isEmpty()) {
            if (rootSteps.size() > 1) {
                throw new ValidationException("When FORGING is included, exactly one root step is allowed");
            }
            
            boolean forgingIsRoot = rootSteps.stream()
                .anyMatch(step -> step.getOperationType() == WorkflowStep.OperationType.FORGING);
                
            if (!forgingIsRoot) {
                throw new ValidationException("When FORGING is included, it must be the root step");
            }
        }

        // DISPATCH-specific validation: DISPATCH operations cannot be parent of any step
        List<WorkflowStep> dispatchSteps = template.getWorkflowSteps().stream()
            .filter(step -> !step.getDeleted() && step.getOperationType() == WorkflowStep.OperationType.DISPATCH)
            .toList();
            
        for (WorkflowStep dispatchStep : dispatchSteps) {
            boolean hasChildSteps = dispatchStep.getActiveChildSteps().size() > 0;
            if (hasChildSteps) {
                throw new ValidationException("DISPATCH operation cannot be a parent of any other step. It must be a terminal step in the workflow.");
            }
        }

        // Check for duplicate operation types in the same branch path
        List<List<WorkflowStep>> allPaths = template.getAllWorkflowPaths();
        for (List<WorkflowStep> path : allPaths) {
            List<WorkflowStep.OperationType> operationTypes = path.stream()
                .map(WorkflowStep::getOperationType)
                .toList();
            
            long uniqueCount = operationTypes.stream().distinct().count();
            if (uniqueCount != operationTypes.size()) {
                throw new ValidationException("Duplicate operation types found in workflow path");
            }
        }

        log.debug("Workflow template validation passed for: {}", template.getWorkflowName());
    }

    /**
     * Get workflow template by ID
     */
    public WorkflowTemplate getWorkflowTemplateById(Long templateId) {
        Optional<WorkflowTemplate> workflowTemplateOptional = workflowTemplateRepository.findByIdAndDeletedFalse(templateId);
        return workflowTemplateOptional.orElseThrow(() -> new RuntimeException("Workflow template not found for id: " + templateId));
    }

    /**
     * Update workflow template
     */
    @Transactional
    public WorkflowTemplate updateWorkflowTemplate(Long templateId, String name, String description) {
        WorkflowTemplate template = workflowTemplateRepository.findByIdAndDeletedFalse(templateId)
            .orElseThrow(() -> new ValidationException("Workflow template not found with ID: " + templateId));

        template.setWorkflowName(name);
        template.setWorkflowDescription(description);

        return workflowTemplateRepository.save(template);
    }

    /**
     * Deactivate workflow template
     */
    @Transactional
    public WorkflowTemplate deactivateWorkflowTemplate(Long templateId) {
        WorkflowTemplate template = workflowTemplateRepository.findByIdAndDeletedFalse(templateId)
            .orElseThrow(() -> new ValidationException("Workflow template not found with ID: " + templateId));

        template.setIsActive(false);
        return workflowTemplateRepository.save(template);
    }

    /**
     * Helper class for custom workflow step definition
     */
    public static class WorkflowStepDefinition {
      // Getters and setters
      @Setter
      @Getter
      private WorkflowStep.OperationType operationType;
        @Getter
        private String stepName;
        @Setter
        @Getter
        private String stepDescription;
        @Setter
        @Getter
        private Integer parentStepIndex; // Index in the step definitions list
        private boolean isOptional = false;

        public WorkflowStepDefinition(WorkflowStep.OperationType operationType, String stepName) {
            this.operationType = operationType;
            this.stepName = stepName;
        }

        public WorkflowStepDefinition(WorkflowStep.OperationType operationType, String stepName, 
                                     Integer parentStepIndex) {
            this.operationType = operationType;
            this.stepName = stepName;
            this.parentStepIndex = parentStepIndex;
        }

      public boolean isOptional() { return isOptional; }
        public void setOptional(boolean optional) { isOptional = optional; }
    }

    public List<WorkflowTemplate> getActiveWorkflowTemplatesByTenant(Long tenantId) {
        return workflowTemplateRepository.findByTenantIdAndIsActiveTrueAndDeletedFalse(tenantId);
    }

    public Page<WorkflowTemplate> getActiveWorkflowTemplatesByTenant(Long tenantId, Pageable pageable) {
        return workflowTemplateRepository.findActiveWorkflowTemplatesOrderedByDefault(tenantId, pageable);
    }

    public List<WorkflowTemplate> getAllWorkflowTemplatesByTenant(Long tenantId) {
        return workflowTemplateRepository.findByTenantIdAndDeletedFalse(tenantId);
    }

    public Page<WorkflowTemplate> getAllWorkflowTemplatesByTenant(Long tenantId, Pageable pageable) {
        return workflowTemplateRepository.findAllWorkflowTemplatesOrderedByDefault(tenantId, pageable);
    }

    /**
     * Check if a workflow template is currently in use by any ItemWorkflow
     */
    public boolean isWorkflowTemplateInUse(Long templateId) {
        return itemWorkflowRepository.existsByWorkflowTemplateIdAndDeletedFalse(templateId);
    }

    /**
     * Get all ItemWorkflows that are using a specific WorkflowTemplate
     */
    public List<ItemWorkflow> getItemWorkflowsUsingTemplate(Long templateId) {
        return itemWorkflowRepository.findByWorkflowTemplateIdAndDeletedFalse(templateId);
    }

    /**
     * Delete workflow template if it's not in use
     * @param templateId The ID of the template to delete
     * @param tenantId The tenant ID for security validation
     * @throws ValidationException if template is in use or not found
     */
    @Transactional
    public void deleteWorkflowTemplate(Long templateId, Long tenantId) {
        // Get the template and validate ownership
        WorkflowTemplate template = workflowTemplateRepository.findByIdAndDeletedFalse(templateId)
            .orElseThrow(() -> new ValidationException("Workflow template not found with ID: " + templateId));

        // Validate that template belongs to the tenant
        if (!Objects.equals(template.getTenant().getId(), tenantId)) {
            throw new ValidationException("Workflow template does not belong to the specified tenant");
        }

        // Check if template is in use
        if (isWorkflowTemplateInUse(templateId)) {
            List<ItemWorkflow> usingWorkflows = getItemWorkflowsUsingTemplate(templateId);
            log.warn("Cannot delete workflow template {} - it is in use by {} ItemWorkflows", 
                    templateId, usingWorkflows.size());
            
            throw new ValidationException(
                String.format("Cannot delete workflow template '%s'. It is currently in use by %d item workflow(s). " +
                            "Please complete or remove all workflows using this template before deletion.",
                            template.getWorkflowName(), usingWorkflows.size()));
        }

        // Soft delete the template
        template.setDeleted(true);
        template.setDeletedAt(LocalDateTime.now());
        workflowTemplateRepository.save(template);

        log.info("Successfully deleted workflow template: {} (ID: {})", template.getWorkflowName(), templateId);
    }
}