package com.jangid.forging_process_management_service.resource.workflow;

import com.jangid.forging_process_management_service.entities.workflow.WorkflowStep;
import com.jangid.forging_process_management_service.entities.workflow.WorkflowTemplate;
import com.jangid.forging_process_management_service.entitiesRepresentation.workflow.WorkflowStepRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.workflow.WorkflowTemplateRepresentation;
import com.jangid.forging_process_management_service.service.workflow.WorkflowTemplateService;
import com.jangid.forging_process_management_service.service.workflow.ItemWorkflowService;
import com.jangid.forging_process_management_service.entitiesRepresentation.workflow.ItemWorkflowRepresentation;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api")
@Api(tags = "Workflow Management", description = "Operations for managing workflow templates")
public class WorkflowController {

    @Autowired
    private WorkflowTemplateService workflowTemplateService;

    @Autowired
    private ItemWorkflowService itemWorkflowService;

    @GetMapping("/tenant/{tenantId}/workflows/templates")
    @ApiOperation(value = "Get all workflow templates for a tenant", 
                 notes = "Returns user-created workflow templates")
    public ResponseEntity<List<WorkflowTemplateRepresentation>> getWorkflowTemplates(
            @ApiParam(value = "Tenant ID", required = true) @PathVariable Long tenantId,
            @ApiParam(value = "Include inactive templates", defaultValue = "false") 
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        try {
            List<WorkflowTemplate> templates;
            if (includeInactive) {
                templates = workflowTemplateService.getAllWorkflowTemplatesByTenant(tenantId);
            } else {
                templates = workflowTemplateService.getActiveWorkflowTemplatesByTenant(tenantId);
            }
            
            if (templates.isEmpty()) {
                return ResponseEntity.ok(List.of()); // Return empty list instead of error
            }
            
            List<WorkflowTemplateRepresentation> representations = templates.stream()
                    .map(this::convertToRepresentation)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(representations);
        } catch (Exception e) {
            log.error("Error fetching workflow templates for tenant {}: {}", tenantId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/tenant/{tenantId}/workflows/templates/{templateId}")
    @ApiOperation(value = "Get a specific workflow template with detailed steps")
    public ResponseEntity<WorkflowTemplateRepresentation> getWorkflowTemplate(
            @ApiParam(value = "Tenant ID", required = true) @PathVariable Long tenantId,
            @ApiParam(value = "Template ID", required = true) @PathVariable Long templateId) {
        try {
            WorkflowTemplate template = workflowTemplateService.getWorkflowTemplateById(templateId);
            
            // Validate that template belongs to the tenant
            if (template.getTenant().getId() != tenantId.longValue()) {
                return ResponseEntity.notFound().build();
            }
            
            WorkflowTemplateRepresentation representation = convertToRepresentation(template);
            return ResponseEntity.ok(representation);
        } catch (RuntimeException e) {
            log.error("Error fetching workflow template {} for tenant {}: {}", templateId, tenantId, e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Unexpected error fetching workflow template {} for tenant {}: {}", templateId, tenantId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/tenant/{tenantId}/workflows/templates")
    @ApiOperation(value = "Create a custom workflow template")
    public ResponseEntity<WorkflowTemplateRepresentation> createWorkflowTemplate(
            @ApiParam(value = "Tenant ID", required = true) @PathVariable Long tenantId,
            @RequestBody CreateWorkflowTemplateRequest request) {
        try {
            // Validate input
            if (request.getWorkflowName() == null || request.getWorkflowName().trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            
            if (request.getOperationTypes() == null || request.getOperationTypes().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            // Validate and convert operation types
            List<WorkflowStep.OperationType> operationTypes;
            try {
                operationTypes = request.getOperationTypes().stream()
                        .map(WorkflowStep.OperationType::valueOf)
                        .collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid operation type in request: {}", e.getMessage());
                return ResponseEntity.badRequest().build();
            }

            if (!workflowTemplateService.validateWorkflowSequence(operationTypes)) {
                return ResponseEntity.badRequest().build();
            }

            WorkflowTemplate template = workflowTemplateService.createCustomWorkflowTemplate(
                    tenantId, request.getWorkflowName().trim(), request.getDescription(), operationTypes);

            WorkflowTemplateRepresentation representation = convertToRepresentation(template);
            return ResponseEntity.status(HttpStatus.CREATED).body(representation);

        } catch (RuntimeException e) {
            log.error("Error creating workflow template for tenant {}: {}", tenantId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Unexpected error creating workflow template for tenant {}: {}", tenantId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/tenant/{tenantId}/workflows/templates/{templateId}")
    @ApiOperation(value = "Update a workflow template")
    public ResponseEntity<WorkflowTemplateRepresentation> updateWorkflowTemplate(
            @ApiParam(value = "Tenant ID", required = true) @PathVariable Long tenantId,
            @ApiParam(value = "Template ID", required = true) @PathVariable Long templateId,
            @RequestBody UpdateWorkflowTemplateRequest request) {
        try {
            WorkflowTemplate template = workflowTemplateService.updateWorkflowTemplate(
                    templateId, tenantId, request.getWorkflowName(), request.getDescription(), request.getIsActive());
            
            WorkflowTemplateRepresentation representation = convertToRepresentation(template);
            return ResponseEntity.ok(representation);
        } catch (RuntimeException e) {
            log.error("Error updating workflow template {} for tenant {}: {}", templateId, tenantId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Unexpected error updating workflow template {} for tenant {}: {}", templateId, tenantId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/tenant/{tenantId}/workflows/templates/{templateId}")
    @ApiOperation(value = "Soft delete a workflow template", 
                 notes = "Marks the template as deleted. Cannot delete default templates or templates in use.")
    public ResponseEntity<Void> deleteWorkflowTemplate(
            @ApiParam(value = "Tenant ID", required = true) @PathVariable Long tenantId,
            @ApiParam(value = "Template ID", required = true) @PathVariable Long templateId) {
        try {
            workflowTemplateService.deleteWorkflowTemplate(templateId, tenantId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            log.error("Error deleting workflow template {} for tenant {}: {}", templateId, tenantId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Unexpected error deleting workflow template {} for tenant {}: {}", templateId, tenantId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/tenant/{tenantId}/workflows/templates/{templateId}/activate")
    @ApiOperation(value = "Activate a workflow template")
    public ResponseEntity<WorkflowTemplateRepresentation> activateWorkflowTemplate(
            @ApiParam(value = "Tenant ID", required = true) @PathVariable Long tenantId,
            @ApiParam(value = "Template ID", required = true) @PathVariable Long templateId) {
        try {
            WorkflowTemplate template = workflowTemplateService.activateWorkflowTemplate(templateId, tenantId);
            WorkflowTemplateRepresentation representation = convertToRepresentation(template);
            return ResponseEntity.ok(representation);
        } catch (RuntimeException e) {
            log.error("Error activating workflow template {} for tenant {}: {}", templateId, tenantId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Unexpected error activating workflow template {} for tenant {}: {}", templateId, tenantId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/tenant/{tenantId}/workflows/templates/{templateId}/deactivate")
    @ApiOperation(value = "Deactivate a workflow template")
    public ResponseEntity<WorkflowTemplateRepresentation> deactivateWorkflowTemplate(
            @ApiParam(value = "Tenant ID", required = true) @PathVariable Long tenantId,
            @ApiParam(value = "Template ID", required = true) @PathVariable Long templateId) {
        try {
            WorkflowTemplate template = workflowTemplateService.deactivateWorkflowTemplate(templateId, tenantId);
            WorkflowTemplateRepresentation representation = convertToRepresentation(template);
            return ResponseEntity.ok(representation);
        } catch (RuntimeException e) {
            log.error("Error deactivating workflow template {} for tenant {}: {}", templateId, tenantId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Unexpected error deactivating workflow template {} for tenant {}: {}", templateId, tenantId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/tenant/{tenantId}/workflows/operation-types")
    @ApiOperation(value = "Get available operation types for workflow creation")
    public ResponseEntity<List<OperationTypeInfo>> getOperationTypes() {
        List<OperationTypeInfo> operationTypes = List.of(
                new OperationTypeInfo(WorkflowStep.OperationType.FORGING.name(), 
                                    WorkflowStep.OperationType.FORGING.getDisplayName()),
                new OperationTypeInfo(WorkflowStep.OperationType.HEAT_TREATMENT.name(), 
                                    WorkflowStep.OperationType.HEAT_TREATMENT.getDisplayName()),
                new OperationTypeInfo(WorkflowStep.OperationType.MACHINING.name(), 
                                    WorkflowStep.OperationType.MACHINING.getDisplayName()),
                new OperationTypeInfo(WorkflowStep.OperationType.QUALITY.name(), 
                                    WorkflowStep.OperationType.QUALITY.getDisplayName()),
                new OperationTypeInfo(WorkflowStep.OperationType.DISPATCH.name(), 
                                    WorkflowStep.OperationType.DISPATCH.getDisplayName())
        );
        return ResponseEntity.ok(operationTypes);
    }

    @GetMapping("/tenant/{tenantId}/workflows/templates/{templateId}/operations/{operationType}/isFirst")
    @ApiOperation(value = "Check if an operation is the first step in a workflow template",
                 notes = "Returns true if the specified operation is the first step in the workflow template")
    public ResponseEntity<Boolean> isFirstOperationInWorkflow(
            @ApiParam(value = "Tenant ID", required = true) @PathVariable Long tenantId,
            @ApiParam(value = "Template ID", required = true) @PathVariable Long templateId,
            @ApiParam(value = "Operation Type", required = true) @PathVariable String operationType) {
        try {
            // Validate that the template exists and belongs to the tenant
            WorkflowTemplate template = workflowTemplateService.getWorkflowTemplateById(templateId);
            if (template.getTenant().getId() != tenantId.longValue()) {
                return ResponseEntity.notFound().build();
            }

            // Validate and convert operation type
            WorkflowStep.OperationType operation;
            try {
                operation = WorkflowStep.OperationType.valueOf(operationType.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid operation type: {}", operationType);
                return ResponseEntity.badRequest().build();
            }

            // Check if operation is first in workflow
            boolean isFirst = itemWorkflowService.isFirstOperationInWorkflow(templateId, operation);
            return ResponseEntity.ok(isFirst);

        } catch (RuntimeException e) {
            log.error("Error checking if operation {} is first in template {} for tenant {}: {}", 
                     operationType, templateId, tenantId, e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Unexpected error checking if operation {} is first in template {} for tenant {}: {}", 
                     operationType, templateId, tenantId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/tenant/{tenantId}/items/{itemId}/workflows/active")
    @ApiOperation(value = "Get active workflows for an item", 
                 notes = "Returns workflows where the item has operations ready to proceed")
    public ResponseEntity<List<ItemWorkflowRepresentation>> getActiveWorkflowsForItem(
            @ApiParam(value = "Tenant ID", required = true) @PathVariable Long tenantId,
            @ApiParam(value = "Item ID", required = true) @PathVariable Long itemId) {
        try {
            List<ItemWorkflowRepresentation> activeWorkflows = itemWorkflowService.getActiveWorkflowsForItem(itemId, tenantId);
            return ResponseEntity.ok(activeWorkflows);
        } catch (RuntimeException e) {
            log.error("Error fetching active workflows for item {} in tenant {}: {}", itemId, tenantId, e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Unexpected error fetching active workflows for item {} in tenant {}: {}", itemId, tenantId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Helper method to convert entity to representation with steps
    private WorkflowTemplateRepresentation convertToRepresentation(WorkflowTemplate template) {
        List<WorkflowStepRepresentation> steps = template.getWorkflowSteps().stream()
                .map(this::convertStepToRepresentation)
                .collect(Collectors.toList());

        return WorkflowTemplateRepresentation.builder()
                .id(template.getId())
                .workflowName(template.getWorkflowName())
                .workflowDescription(template.getWorkflowDescription())
                .isDefault(template.getIsDefault())
                .isActive(template.getIsActive())
                .workflowSteps(steps)
                .tenantId(template.getTenant().getId())
                .createdAt(template.getCreatedAt() != null ? template.getCreatedAt().toString() : null)
                .updatedAt(template.getUpdatedAt() != null ? template.getUpdatedAt().toString() : null)
                .build();
    }

    private WorkflowStepRepresentation convertStepToRepresentation(WorkflowStep step) {
        return WorkflowStepRepresentation.builder()
                .id(step.getId())
                .operationType(step.getOperationType().name())
                .stepOrder(step.getStepOrder())
                .stepName(step.getStepName())
                .stepDescription(step.getStepDescription())
                .isOptional(step.getIsOptional())
                .isParallel(step.getIsParallel())
                .createdAt(step.getCreatedAt() != null ? step.getCreatedAt().toString() : null)
                .updatedAt(step.getUpdatedAt() != null ? step.getUpdatedAt().toString() : null)
                .build();
    }

    // Request/Response DTOs
    public static class CreateWorkflowTemplateRequest {
        private String workflowName;
        private String description;
        private List<String> operationTypes;

        public CreateWorkflowTemplateRequest() {}

        public CreateWorkflowTemplateRequest(String workflowName, String description, List<String> operationTypes) {
            this.workflowName = workflowName;
            this.description = description;
            this.operationTypes = operationTypes;
        }

        public String getWorkflowName() { return workflowName; }
        public void setWorkflowName(String workflowName) { this.workflowName = workflowName; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public List<String> getOperationTypes() { return operationTypes; }
        public void setOperationTypes(List<String> operationTypes) { this.operationTypes = operationTypes; }
    }

    public static class UpdateWorkflowTemplateRequest {
        private String workflowName;
        private String description;
        private Boolean isActive;

        public UpdateWorkflowTemplateRequest() {}

        public UpdateWorkflowTemplateRequest(String workflowName, String description, Boolean isActive) {
            this.workflowName = workflowName;
            this.description = description;
            this.isActive = isActive;
        }

        public String getWorkflowName() { return workflowName; }
        public void setWorkflowName(String workflowName) { this.workflowName = workflowName; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public Boolean getIsActive() { return isActive; }
        public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    }

    public static class OperationTypeInfo {
        private String value;
        private String displayName;

        public OperationTypeInfo() {}

        public OperationTypeInfo(String value, String displayName) {
            this.value = value;
            this.displayName = displayName;
        }

        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }

        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
    }
} 