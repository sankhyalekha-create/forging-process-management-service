package com.jangid.forging_process_management_service.resource.workflow;

import com.jangid.forging_process_management_service.entities.workflow.ItemWorkflow;
import com.jangid.forging_process_management_service.entities.workflow.ItemWorkflowStep;
import com.jangid.forging_process_management_service.entities.workflow.WorkflowStep;
import com.jangid.forging_process_management_service.entitiesRepresentation.workflow.ItemWorkflowRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.workflow.ItemWorkflowStepRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.workflow.CompleteWorkflowRequestRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.workflow.ItemWorkflowPageResponseRepresentation;
import com.jangid.forging_process_management_service.service.workflow.ItemWorkflowService;
import com.jangid.forging_process_management_service.assemblers.workflow.ItemWorkflowAssembler;
import com.jangid.forging_process_management_service.assemblers.workflow.ItemWorkflowStepAssembler;
import com.jangid.forging_process_management_service.utils.ConvertorUtils;
import com.jangid.forging_process_management_service.dto.ItemWorkflowTrackingResultDTO;
import com.jangid.forging_process_management_service.entitiesRepresentation.error.ErrorResponse;

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

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api")
@Api(tags = "Item Workflow Management", description = "Operations for managing item workflows and workflow steps")
public class ItemWorkflowResource {

    @Autowired
    private ItemWorkflowService itemWorkflowService;

    @Autowired
    private ItemWorkflowAssembler itemWorkflowAssembler;

    @Autowired
    private ItemWorkflowStepAssembler itemWorkflowStepAssembler;

    @GetMapping("/tenant/{tenantId}/workflows/{workflowId}")
    @ApiOperation(value = "Get item workflow details", 
                 notes = "Returns detailed workflow information including all workflow steps")
    public ResponseEntity<ItemWorkflowRepresentation> getItemWorkflowDetails(
            @ApiParam(value = "Tenant ID", required = true) @PathVariable Long tenantId,
            @ApiParam(value = "Workflow ID", required = true) @PathVariable Long workflowId) {
        try {
            // Get the workflow
            ItemWorkflow itemWorkflow = itemWorkflowService.getItemWorkflowById(workflowId);
            
            // Validate that workflow belongs to the tenant
            if (itemWorkflow.getItem().getTenant().getId() != tenantId.longValue()) {
                return ResponseEntity.notFound().build();
            }
            
            // Convert to representation (includes workflow steps)
            ItemWorkflowRepresentation workflowRepresentation = itemWorkflowAssembler.dissemble(itemWorkflow);
            
            return ResponseEntity.ok(workflowRepresentation);
            
        } catch (RuntimeException e) {
            log.error("Error fetching workflow {} for tenant {}: {}", workflowId, tenantId, e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Unexpected error fetching workflow {} for tenant {}: {}", workflowId, tenantId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/tenant/{tenantId}/workflows/{workflowId}/steps")
    @ApiOperation(value = "Get workflow steps for a workflow", 
                 notes = "Returns all workflow steps for the specified workflow")
    public ResponseEntity<List<ItemWorkflowStepRepresentation>> getWorkflowSteps(
            @ApiParam(value = "Tenant ID", required = true) @PathVariable Long tenantId,
            @ApiParam(value = "Workflow ID", required = true) @PathVariable Long workflowId) {
        try {
            // Get the workflow
            ItemWorkflow itemWorkflow = itemWorkflowService.getItemWorkflowById(workflowId);
            
            // Validate that workflow belongs to the tenant
            if (itemWorkflow.getItem().getTenant().getId() != tenantId.longValue()) {
                return ResponseEntity.notFound().build();
            }
            
            // Convert workflow steps to representations
            List<ItemWorkflowStepRepresentation> workflowSteps = itemWorkflowStepAssembler.dissemble(
                itemWorkflow.getItemWorkflowSteps()
            );
            
            return ResponseEntity.ok(workflowSteps);
            
        } catch (RuntimeException e) {
            log.error("Error fetching workflow steps for workflow {} in tenant {}: {}", workflowId, tenantId, e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Unexpected error fetching workflow steps for workflow {} in tenant {}: {}", workflowId, tenantId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/tenant/{tenantId}/workflows/{workflowId}/steps/{stepId}")
    @ApiOperation(value = "Get specific workflow step details", 
                 notes = "Returns detailed information about a specific workflow step")
    public ResponseEntity<ItemWorkflowStepRepresentation> getWorkflowStep(
            @ApiParam(value = "Tenant ID", required = true) @PathVariable Long tenantId,
            @ApiParam(value = "Workflow ID", required = true) @PathVariable Long workflowId,
            @ApiParam(value = "Step ID", required = true) @PathVariable Long stepId) {
        try {
            // Get the workflow
            ItemWorkflow itemWorkflow = itemWorkflowService.getItemWorkflowById(workflowId);
            
            // Validate that workflow belongs to the tenant
            if (itemWorkflow.getItem().getTenant().getId() != tenantId.longValue()) {
                return ResponseEntity.notFound().build();
            }
            
            // Find the specific workflow step
            ItemWorkflowStep workflowStep = itemWorkflow.getItemWorkflowSteps().stream()
                .filter(step -> step.getId().equals(stepId))
                .findFirst()
                .orElse(null);
            
            if (workflowStep == null) {
                return ResponseEntity.notFound().build();
            }
            
            // Convert to representation
            ItemWorkflowStepRepresentation stepRepresentation = itemWorkflowStepAssembler.dissemble(workflowStep);
            
            return ResponseEntity.ok(stepRepresentation);
            
        } catch (RuntimeException e) {
            log.error("Error fetching workflow step {} for workflow {} in tenant {}: {}", stepId, workflowId, tenantId, e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Unexpected error fetching workflow step {} for workflow {} in tenant {}: {}", stepId, workflowId, tenantId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/tenant/{tenantId}/workflows/{workflowId}/steps/operation/{operationType}")
    @ApiOperation(value = "Get workflow step by operation type", 
                 notes = "Returns the workflow step for a specific operation type")
        public ResponseEntity<ItemWorkflowStepRepresentation> getWorkflowStepByOperation(
            @ApiParam(value = "Tenant ID", required = true) @PathVariable Long tenantId,
            @ApiParam(value = "Workflow ID", required = true) @PathVariable Long workflowId,
            @ApiParam(value = "Operation Type", required = true) @PathVariable String operationType) {
        try {
            // Get the workflow
            ItemWorkflow itemWorkflow = itemWorkflowService.getItemWorkflowById(workflowId);
            
            // Validate that workflow belongs to the tenant
            if (itemWorkflow.getItem().getTenant().getId() != tenantId.longValue()) {
                return ResponseEntity.notFound().build();
            }
            
            // Parse operation type
            WorkflowStep.OperationType operation;
            try {
                operation = WorkflowStep.OperationType.valueOf(operationType.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid operation type: {}", operationType);
                return ResponseEntity.badRequest().build();
            }
            
            // Find the workflow step by operation type
            ItemWorkflowStep workflowStep = itemWorkflow.getStepByOperationType(operation);
            
            if (workflowStep == null) {
                return ResponseEntity.notFound().build();
            }
            
            // Convert to representation
            ItemWorkflowStepRepresentation stepRepresentation = itemWorkflowStepAssembler.dissemble(workflowStep);
            
            return ResponseEntity.ok(stepRepresentation);
            
        } catch (RuntimeException e) {
            log.error("Error fetching workflow step for operation {} in workflow {} for tenant {}: {}", operationType, workflowId, tenantId, e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Unexpected error fetching workflow step for operation {} in workflow {} for tenant {}: {}", operationType, workflowId, tenantId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/tenant/{tenantId}/item-workflows")
    @ApiOperation(value = "Get all item workflows for a tenant", 
                 notes = "Returns paginated list of item workflows ordered by updatedAt DESC")
    public ResponseEntity<ItemWorkflowPageResponseRepresentation> getAllItemWorkflows(
            @ApiParam(value = "Tenant ID", required = true) @PathVariable Long tenantId,
            @ApiParam(value = "Page number (0-based)", defaultValue = "0") @RequestParam(defaultValue = "0") int page,
            @ApiParam(value = "Page size", defaultValue = "10") @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<ItemWorkflow> itemWorkflowPage = itemWorkflowService.getAllItemWorkflowsForTenant(tenantId, pageable);
            
            List<ItemWorkflowRepresentation> workflowRepresentations = itemWorkflowPage.getContent().stream()
                .map(itemWorkflowAssembler::dissemble)
                .collect(Collectors.toList());
            
            ItemWorkflowPageResponseRepresentation response = new ItemWorkflowPageResponseRepresentation(
                workflowRepresentations,
                itemWorkflowPage.getTotalPages(),
                itemWorkflowPage.getTotalElements(),
                itemWorkflowPage.getNumber(),
                itemWorkflowPage.getSize()
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error fetching item workflows for tenant {}: {}", tenantId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/tenant/{tenantId}/item-workflows/search")
    @ApiOperation(value = "Search item workflows", 
                 notes = "Search item workflows by item name or workflow identifier")
    public ResponseEntity<ItemWorkflowPageResponseRepresentation> searchItemWorkflows(
            @ApiParam(value = "Tenant ID", required = true) @PathVariable Long tenantId,
            @ApiParam(value = "Search type: 'ITEM_NAME' or 'WORKFLOW_IDENTIFIER'", required = true) 
            @RequestParam String searchType,
            @ApiParam(value = "Search term", required = true) @RequestParam String searchTerm,
            @ApiParam(value = "Page number (0-based)", defaultValue = "0") @RequestParam(defaultValue = "0") int page,
            @ApiParam(value = "Page size", defaultValue = "10") @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<ItemWorkflow> itemWorkflowPage;
            
            switch (searchType.toUpperCase()) {
                case "ITEM_NAME":
                    itemWorkflowPage = itemWorkflowService.searchItemWorkflowsByItemName(tenantId, searchTerm, pageable);
                    break;
                case "WORKFLOW_IDENTIFIER":
                    itemWorkflowPage = itemWorkflowService.searchItemWorkflowsByWorkflowIdentifier(tenantId, searchTerm, pageable);
                    break;
                default:
                    return ResponseEntity.badRequest().build();
            }
            
            List<ItemWorkflowRepresentation> workflowRepresentations = itemWorkflowPage.getContent().stream()
                .map(itemWorkflowAssembler::dissemble)
                .collect(Collectors.toList());
            
            ItemWorkflowPageResponseRepresentation response = new ItemWorkflowPageResponseRepresentation(
                workflowRepresentations,
                itemWorkflowPage.getTotalPages(),
                itemWorkflowPage.getTotalElements(),
                itemWorkflowPage.getNumber(),
                itemWorkflowPage.getSize()
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error searching item workflows for tenant {} with search type {} and term '{}': {}", 
                     tenantId, searchType, searchTerm, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/tenant/{tenantId}/workflow/item-workflows/{workflowId}/complete")
    @ApiOperation(value = "Complete an item workflow", 
                 notes = "Marks the workflow and all its steps as completed with the provided completion time")
    public ResponseEntity<ItemWorkflowRepresentation> completeItemWorkflow(
            @ApiParam(value = "Tenant ID", required = true) @PathVariable Long tenantId,
            @ApiParam(value = "Workflow ID", required = true) @PathVariable Long workflowId,
            @RequestBody CompleteWorkflowRequestRepresentation request) {
        try {
            // Validate request
            if (request.getCompletedAt() == null || request.getCompletedAt().isEmpty()) {
                log.warn("Completion time is required for completing workflow {}", workflowId);
                return ResponseEntity.badRequest().build();
            }
            
            // Convert String to LocalDateTime using ConvertorUtils
            LocalDateTime completedAtDateTime = ConvertorUtils.convertStringToLocalDateTime(request.getCompletedAt());
            
            // Complete the workflow
            ItemWorkflow completedWorkflow = itemWorkflowService.completeItemWorkflow(
                workflowId, tenantId, completedAtDateTime);
            
            // Convert to representation
            ItemWorkflowRepresentation workflowRepresentation = itemWorkflowAssembler.dissemble(completedWorkflow);
            
            return ResponseEntity.ok(workflowRepresentation);
            
        } catch (RuntimeException e) {
            log.error("Error completing workflow {} for tenant {}: {}", workflowId, tenantId, e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Unexpected error completing workflow {} for tenant {}: {}", workflowId, tenantId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/tenant/{tenantId}/workflow-tracking")
    @ApiOperation(value = "Get comprehensive tracking information for an item workflow", 
                 notes = "Returns detailed tracking information including all related batches by workflow identifier")
    public ResponseEntity<?> getItemWorkflowTracking(
            @ApiParam(value = "Tenant ID", required = true) @PathVariable Long tenantId,
            @ApiParam(value = "Workflow identifier", required = true) @RequestParam String workflowIdentifier) {
        try {
            if (workflowIdentifier == null || workflowIdentifier.trim().isEmpty()) {
                log.error("Invalid workflowIdentifier input!");
                return new ResponseEntity<>(new ErrorResponse("Workflow identifier is required"), HttpStatus.BAD_REQUEST);
            }
            
            ItemWorkflowTrackingResultDTO trackingResult = itemWorkflowService.getItemWorkflowTrackingByWorkflowIdentifier(
                tenantId, workflowIdentifier.trim());
            return ResponseEntity.ok(trackingResult);
            
        } catch (RuntimeException e) {
            if (e.getMessage().contains("No workflow found")) {
                return ResponseEntity.notFound().build();
            }
            if (e.getMessage().contains("does not belong to the specified tenant")) {
                return ResponseEntity.notFound().build();
            }
            log.error("Error fetching workflow tracking for identifier {} in tenant {}: {}", workflowIdentifier, tenantId, e.getMessage());
            return new ResponseEntity<>(new ErrorResponse(e.getMessage()), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("Unexpected error fetching workflow tracking for identifier {} in tenant {}: {}", workflowIdentifier, tenantId, e.getMessage());
            return new ResponseEntity<>(new ErrorResponse("Error fetching workflow tracking"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


} 