package com.jangid.forging_process_management_service.resource.workflow;

import com.jangid.forging_process_management_service.entities.workflow.ItemWorkflow;
import com.jangid.forging_process_management_service.entities.workflow.ItemWorkflowStep;
import com.jangid.forging_process_management_service.entities.workflow.WorkflowStep;
import com.jangid.forging_process_management_service.entitiesRepresentation.workflow.ItemWorkflowRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.workflow.ItemWorkflowStepRepresentation;
import com.jangid.forging_process_management_service.service.workflow.ItemWorkflowService;
import com.jangid.forging_process_management_service.assemblers.workflow.ItemWorkflowAssembler;
import com.jangid.forging_process_management_service.assemblers.workflow.ItemWorkflowStepAssembler;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
} 