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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
                 notes = "Returns user-created workflow templates with or without pagination support")
    public ResponseEntity<?> getWorkflowTemplates(
            @ApiParam(value = "Tenant ID", required = true) @PathVariable Long tenantId,
            @ApiParam(value = "Include inactive templates", defaultValue = "false") 
            @RequestParam(defaultValue = "false") boolean includeInactive,
            @ApiParam(value = "Page number (0-based)") @RequestParam(value = "page", required = false) String page,
            @ApiParam(value = "Page size") @RequestParam(value = "size", required = false) String size) {
        try {
            Integer pageNumber = (page == null || page.isBlank()) ? -1
                    : Integer.parseInt(page);
            Integer sizeNumber = (size == null || size.isBlank()) ? -1
                    : Integer.parseInt(size);

            if (pageNumber == -1 || sizeNumber == -1) {
                // Return non-paginated response
                List<WorkflowTemplate> templates;
                if (includeInactive) {
                    templates = workflowTemplateService.getAllWorkflowTemplatesByTenant(tenantId);
                } else {
                    templates = workflowTemplateService.getActiveWorkflowTemplatesByTenant(tenantId);
                }
                
                List<WorkflowTemplateRepresentation> representations = templates.stream()
                        .map(this::convertToRepresentation)
                        .collect(Collectors.toList());
                        
                return ResponseEntity.ok(representations);
            }

            // Return paginated response
            Pageable pageable = PageRequest.of(pageNumber, sizeNumber);
            Page<WorkflowTemplate> templatePage;
            
            if (includeInactive) {
                templatePage = workflowTemplateService.getAllWorkflowTemplatesByTenant(tenantId, pageable);
            } else {
                templatePage = workflowTemplateService.getActiveWorkflowTemplatesByTenant(tenantId, pageable);
            }
            
            List<WorkflowTemplateRepresentation> representations = templatePage.getContent().stream()
                    .map(this::convertToRepresentation)
                    .collect(Collectors.toList());
                    
            WorkflowTemplatePageResponse response = new WorkflowTemplatePageResponse(
                    representations,
                    templatePage.getTotalPages(),
                    templatePage.getTotalElements(),
                    templatePage.getNumber(),
                    templatePage.getSize(),
                    templatePage.isFirst(),
                    templatePage.isLast()
            );
            
            return ResponseEntity.ok(response);
        } catch (NumberFormatException e) {
            log.error("Invalid page or size parameter for tenant {}: page={}, size={}", tenantId, page, size);
            return ResponseEntity.badRequest().build();
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
                new OperationTypeInfo(WorkflowStep.OperationType.VENDOR.name(), 
                                    WorkflowStep.OperationType.VENDOR.getDisplayName()),
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

    @GetMapping("/tenant/{tenantId}/workflows/templates/search")
    @ApiOperation(value = "Search workflow templates", 
                 notes = "Search workflow templates by template name or operation type")
    public ResponseEntity<WorkflowTemplatePageResponse> searchWorkflowTemplates(
            @ApiParam(value = "Tenant ID", required = true) @PathVariable Long tenantId,
            @ApiParam(value = "Search type: 'WORKFLOW_TEMPLATE_NAME' or 'OPERATION_TYPE'", required = true) 
            @RequestParam String searchType,
            @ApiParam(value = "Search term", required = true) @RequestParam String searchTerm,
            @ApiParam(value = "Page number (0-based)", defaultValue = "0") @RequestParam(defaultValue = "0") int page,
            @ApiParam(value = "Page size", defaultValue = "10") @RequestParam(defaultValue = "10") int size) {
        try {
            if (searchType == null || searchType.trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            
            if (searchTerm == null || searchTerm.trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            if (page < 0) {
                page = 0;
            }

            if (size <= 0) {
                size = 10; // Default page size
            }

            Pageable pageable = PageRequest.of(page, size);
            Page<WorkflowTemplate> templatePage;
            
            switch (searchType.toUpperCase()) {
                case "WORKFLOW_TEMPLATE_NAME":
                    templatePage = workflowTemplateService.searchWorkflowTemplatesByName(tenantId, searchTerm.trim(), pageable);
                    break;
                case "OPERATION_TYPE":
                    templatePage = workflowTemplateService.searchWorkflowTemplatesByOperationType(tenantId, searchTerm.trim(), pageable);
                    break;
                default:
                    return ResponseEntity.badRequest().build();
            }
            
            List<WorkflowTemplateRepresentation> representations = templatePage.getContent().stream()
                    .map(this::convertToRepresentation)
                    .collect(Collectors.toList());
                    
            WorkflowTemplatePageResponse response = new WorkflowTemplatePageResponse(
                    representations,
                    templatePage.getTotalPages(),
                    templatePage.getTotalElements(),
                    templatePage.getNumber(),
                    templatePage.getSize(),
                    templatePage.isFirst(),
                    templatePage.isLast()
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error searching workflow templates for tenant {} with search type {} and term '{}': {}", 
                     tenantId, searchType, searchTerm, e.getMessage());
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
    
    public static class WorkflowTemplatePageResponse {
        private List<WorkflowTemplateRepresentation> content;
        private int totalPages;
        private long totalElements;
        private int currentPage;
        private int pageSize;
        private boolean first;
        private boolean last;

        public WorkflowTemplatePageResponse() {}

        public WorkflowTemplatePageResponse(List<WorkflowTemplateRepresentation> content, int totalPages, 
                long totalElements, int currentPage, int pageSize, boolean first, boolean last) {
            this.content = content;
            this.totalPages = totalPages;
            this.totalElements = totalElements;
            this.currentPage = currentPage;
            this.pageSize = pageSize;
            this.first = first;
            this.last = last;
        }

        public List<WorkflowTemplateRepresentation> getContent() { return content; }
        public void setContent(List<WorkflowTemplateRepresentation> content) { this.content = content; }

        public int getTotalPages() { return totalPages; }
        public void setTotalPages(int totalPages) { this.totalPages = totalPages; }

        public long getTotalElements() { return totalElements; }
        public void setTotalElements(long totalElements) { this.totalElements = totalElements; }

        public int getCurrentPage() { return currentPage; }
        public void setCurrentPage(int currentPage) { this.currentPage = currentPage; }

        public int getPageSize() { return pageSize; }
        public void setPageSize(int pageSize) { this.pageSize = pageSize; }

        public boolean isFirst() { return first; }
        public void setFirst(boolean first) { this.first = first; }

        public boolean isLast() { return last; }
        public void setLast(boolean last) { this.last = last; }
    }
} 