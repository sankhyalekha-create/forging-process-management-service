package com.jangid.forging_process_management_service.service.workflow;

import com.jangid.forging_process_management_service.entities.Tenant;
import com.jangid.forging_process_management_service.entities.workflow.WorkflowStep;
import com.jangid.forging_process_management_service.entities.workflow.WorkflowTemplate;
import com.jangid.forging_process_management_service.repositories.workflow.WorkflowTemplateRepository;
import com.jangid.forging_process_management_service.service.TenantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class WorkflowTemplateService {

    @Autowired
    private WorkflowTemplateRepository workflowTemplateRepository;

    @Autowired
    private TenantService tenantService;

    @Transactional
    public void createDefaultWorkflowTemplates(Long tenantId) {
        Tenant tenant = tenantService.getTenantById(tenantId);
        
        // Create default quantity-based workflow (Forging → Heat Treatment → Machining → Quality → Dispatch)
        WorkflowTemplate quantityWorkflow = WorkflowTemplate.builder()
                .workflowName("Standard Quantity-based Workflow")
                .workflowDescription("Default workflow for items processed from heat quantity")
                .isDefault(true)
                .isActive(true)
                .tenant(tenant)
                .build();

        addStandardWorkflowSteps(quantityWorkflow, true);
        workflowTemplateRepository.save(quantityWorkflow);

        // Create default pieces-based workflow (Machining → Quality → Dispatch)
        WorkflowTemplate piecesWorkflow = WorkflowTemplate.builder()
                .workflowName("Standard Pieces-based Workflow")
                .workflowDescription("Default workflow for items processed from heat pieces")
                .isDefault(true)
                .isActive(true)
                .tenant(tenant)
                .build();

        addStandardWorkflowSteps(piecesWorkflow, false);
        workflowTemplateRepository.save(piecesWorkflow);

        log.info("Created default workflow templates for tenant {}", tenantId);
    }

    private void addStandardWorkflowSteps(WorkflowTemplate template, boolean includeForging) {
        int order = 1;
        
        if (includeForging) {
            WorkflowStep forgingStep = WorkflowStep.builder()
                    .workflowTemplate(template)
                    .operationType(WorkflowStep.OperationType.FORGING)
                    .stepOrder(order++)
                    .stepName("Forging")
                    .stepDescription("Forging operation to create forged pieces from heat quantity")
                    .isOptional(false)
                    .build();
            template.addWorkflowStep(forgingStep);

            WorkflowStep heatTreatmentStep = WorkflowStep.builder()
                    .workflowTemplate(template)
                    .operationType(WorkflowStep.OperationType.HEAT_TREATMENT)
                    .stepOrder(order++)
                    .stepName("Heat Treatment")
                    .stepDescription("Heat treatment of forged pieces")
                    .isOptional(false)
                    .build();
            template.addWorkflowStep(heatTreatmentStep);
        }

        WorkflowStep machiningStep = WorkflowStep.builder()
                .workflowTemplate(template)
                .operationType(WorkflowStep.OperationType.MACHINING)
                .stepOrder(order++)
                .stepName("Machining")
                .stepDescription("Machining operation")
                .isOptional(false)
                .build();
        template.addWorkflowStep(machiningStep);

        WorkflowStep qualityStep = WorkflowStep.builder()
                .workflowTemplate(template)
                .operationType(WorkflowStep.OperationType.QUALITY)
                .stepOrder(order++)
                .stepName("Quality Inspection")
                .stepDescription("Quality inspection and testing")
                .isOptional(false)
                .build();
        template.addWorkflowStep(qualityStep);

        WorkflowStep dispatchStep = WorkflowStep.builder()
                .workflowTemplate(template)
                .operationType(WorkflowStep.OperationType.DISPATCH)
                .stepOrder(order++)
                .stepName("Dispatch")
                .stepDescription("Final dispatch of completed items")
                .isOptional(false)
                .build();
        template.addWorkflowStep(dispatchStep);
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

    public List<WorkflowTemplate> getDefaultWorkflowTemplatesByTenant(Long tenantId) {
        return workflowTemplateRepository.findByTenantIdAndIsDefaultTrueAndDeletedFalse(tenantId);
    }

    public WorkflowTemplate getWorkflowTemplateByTenant(Long tenantId, String workflowName) {
        Optional<WorkflowTemplate> template = workflowTemplateRepository
                .findByTenantIdAndWorkflowNameAndDeletedFalse(tenantId, workflowName);
        
        return template.orElseThrow(() -> 
                new RuntimeException("Workflow template '" + workflowName + "' not found for tenant " + tenantId));
    }

    /**
     * @deprecated This method is deprecated. Users must create workflow templates explicitly.
     * Use getActiveWorkflowTemplatesByTenant() and require users to select a template.
     */
    @Deprecated
    public WorkflowTemplate getDefaultWorkflowTemplateByTenant(Long tenantId, boolean isQuantityBased) {
        List<WorkflowTemplate> templates = getActiveWorkflowTemplatesByTenant(tenantId);
        if (templates.isEmpty()) {
            throw new RuntimeException("No workflow templates found for tenant " + tenantId + 
                    ". Please create workflow templates before creating items.");
        }
        
        // Return first available template as fallback for legacy code
        log.warn("Using deprecated getDefaultWorkflowTemplateByTenant for tenant {}. " +
                "Consider updating to explicit template selection.", tenantId);
        return templates.get(0);
    }

    public WorkflowTemplate getWorkflowTemplateById(Long templateId) {
        Optional<WorkflowTemplate> template = workflowTemplateRepository.findByIdAndDeletedFalse(templateId);
        return template.orElseThrow(() -> 
                new RuntimeException("Workflow template not found with id: " + templateId));
    }

    @Transactional
    public WorkflowTemplate updateWorkflowTemplate(Long templateId, Long tenantId, String workflowName, 
            String description, Boolean isActive) {
        WorkflowTemplate template = getWorkflowTemplateById(templateId);
        
        // Validate tenant ownership
        if (template.getTenant().getId() != tenantId.longValue()) {
            throw new RuntimeException("Workflow template does not belong to tenant: " + tenantId);
        }
        
        // Cannot update default templates workflow steps, only name/description/status
        if (template.getIsDefault() && workflowName != null) {
            log.warn("Cannot change name of default workflow template {}", templateId);
            // Allow updating description and status for default templates
        }

        if (workflowName != null && !workflowName.trim().isEmpty() && !template.getIsDefault()) {
            // Check for name conflicts (excluding current template)
            Optional<WorkflowTemplate> existing = workflowTemplateRepository
                    .findByTenantIdAndWorkflowNameAndDeletedFalse(tenantId, workflowName.trim());
            if (existing.isPresent() && !existing.get().getId().equals(templateId)) {
                throw new RuntimeException("Workflow template with name '" + workflowName + "' already exists");
            }
            template.setWorkflowName(workflowName.trim());
        }

        if (description != null) {
            template.setWorkflowDescription(description);
        }

        if (isActive != null) {
            template.setIsActive(isActive);
        }

        return workflowTemplateRepository.save(template);
    }

    @Transactional
    public void deleteWorkflowTemplate(Long templateId, Long tenantId) {
        WorkflowTemplate template = getWorkflowTemplateById(templateId);
        
        // Validate tenant ownership
        if (template.getTenant().getId() != tenantId.longValue()) {
            throw new RuntimeException("Workflow template does not belong to tenant: " + tenantId);
        }
        
        // Cannot delete default templates
        if (template.getIsDefault()) {
            throw new RuntimeException("Cannot delete default workflow template");
        }

        // TODO: Check if template is in use by any items
        // This would require checking ItemWorkflow entities
        
        // Soft delete
        template.setDeleted(true);
        template.setDeletedAt(LocalDateTime.now());
        workflowTemplateRepository.save(template);
        
        log.info("Deleted workflow template {} for tenant {}", templateId, tenantId);
    }

    @Transactional
    public WorkflowTemplate activateWorkflowTemplate(Long templateId, Long tenantId) {
        WorkflowTemplate template = getWorkflowTemplateById(templateId);
        
        // Validate tenant ownership
        if (template.getTenant().getId() != tenantId.longValue()) {
            throw new RuntimeException("Workflow template does not belong to tenant: " + tenantId);
        }
        
        template.setIsActive(true);
        return workflowTemplateRepository.save(template);
    }

    @Transactional
    public WorkflowTemplate deactivateWorkflowTemplate(Long templateId, Long tenantId) {
        WorkflowTemplate template = getWorkflowTemplateById(templateId);
        
        // Validate tenant ownership
        if (template.getTenant().getId() != tenantId.longValue()) {
            throw new RuntimeException("Workflow template does not belong to tenant: " + tenantId);
        }
        
        // Cannot deactivate default templates
        if (template.getIsDefault()) {
            throw new RuntimeException("Cannot deactivate default workflow template");
        }
        
        template.setIsActive(false);
        return workflowTemplateRepository.save(template);
    }

    @Transactional
    public WorkflowTemplate createCustomWorkflowTemplate(Long tenantId, String workflowName, 
            String description, List<WorkflowStep.OperationType> operationTypes) {
        Tenant tenant = tenantService.getTenantById(tenantId);
        
        // Check if workflow name already exists for this tenant
        Optional<WorkflowTemplate> existing = workflowTemplateRepository
                .findByTenantIdAndWorkflowNameAndDeletedFalse(tenantId, workflowName);
        if (existing.isPresent()) {
            throw new RuntimeException("Workflow template with name '" + workflowName + "' already exists");
        }
        
        WorkflowTemplate template = WorkflowTemplate.builder()
                .workflowName(workflowName)
                .workflowDescription(description)
                .isDefault(false)
                .isActive(true)
                .tenant(tenant)
                .build();

        // Add steps in the provided order
        for (int i = 0; i < operationTypes.size(); i++) {
            WorkflowStep step = WorkflowStep.builder()
                    .workflowTemplate(template)
                    .operationType(operationTypes.get(i))
                    .stepOrder(i + 1)
                    .stepName(operationTypes.get(i).getDisplayName())
                    .stepDescription(operationTypes.get(i).getDisplayName() + " operation")
                    .isOptional(false)
                    .build();
            template.addWorkflowStep(step);
        }

        return workflowTemplateRepository.save(template);
    }

    public boolean validateWorkflowSequence(List<WorkflowStep.OperationType> operationTypes) {
        // Basic validation - ensure at least one operation and no duplicates
        if (operationTypes == null || operationTypes.isEmpty()) {
            return false;
        }
        
        // Check for duplicates
        long uniqueCount = operationTypes.stream().distinct().count();
        return uniqueCount == operationTypes.size();
    }
} 