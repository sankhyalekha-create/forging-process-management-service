package com.jangid.forging_process_management_service.resource.workflow;

import com.jangid.forging_process_management_service.entities.workflow.ItemWorkflow;
import com.jangid.forging_process_management_service.entities.workflow.ItemWorkflowStep;
import com.jangid.forging_process_management_service.entities.workflow.WorkflowStep;
import com.jangid.forging_process_management_service.entities.workflow.WorkflowTemplate;
import com.jangid.forging_process_management_service.entities.product.Item;
import com.jangid.forging_process_management_service.entities.forging.Forge;
import com.jangid.forging_process_management_service.entities.forging.ForgeShift;
import com.jangid.forging_process_management_service.entities.heating.HeatTreatmentBatch;
import com.jangid.forging_process_management_service.entities.dispatch.DispatchBatch;
import com.jangid.forging_process_management_service.entitiesRepresentation.machining.DailyMachiningBatchRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.machining.MachiningBatchRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.quality.InspectionBatchRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.vendor.VendorDispatchBatchRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.workflow.ItemWorkflowRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.workflow.ItemWorkflowStepRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.workflow.CompleteWorkflowRequestRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.workflow.ItemWorkflowPageResponseRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.workflow.ItemWorkflowListRepresentation;
import com.jangid.forging_process_management_service.service.workflow.ItemWorkflowService;
import com.jangid.forging_process_management_service.service.product.ItemService;
import com.jangid.forging_process_management_service.service.workflow.WorkflowTemplateService;
import com.jangid.forging_process_management_service.service.forging.ForgeService;
import com.jangid.forging_process_management_service.service.heating.HeatTreatmentBatchService;
import com.jangid.forging_process_management_service.service.machining.MachiningBatchService;
import com.jangid.forging_process_management_service.service.quality.InspectionBatchService;
import com.jangid.forging_process_management_service.service.vendor.VendorDispatchService;
import com.jangid.forging_process_management_service.service.dispatch.DispatchBatchService;
import com.jangid.forging_process_management_service.assemblers.workflow.ItemWorkflowAssembler;
import com.jangid.forging_process_management_service.assemblers.workflow.ItemWorkflowStepAssembler;
import com.jangid.forging_process_management_service.utils.ConvertorUtils;
import com.jangid.forging_process_management_service.dto.ItemWorkflowTrackingResultDTO;
import com.jangid.forging_process_management_service.dto.HeatInfoDTO;
import com.jangid.forging_process_management_service.dto.OperationEndTimeDTO;
import com.jangid.forging_process_management_service.entitiesRepresentation.error.ErrorResponse;
import com.jangid.forging_process_management_service.utils.GenericResourceUtils;
import com.jangid.forging_process_management_service.exception.ResourceNotFoundException;
import com.jangid.forging_process_management_service.exception.forging.ForgeNotFoundException;
import com.jangid.forging_process_management_service.exception.heating.HeatTreatmentBatchNotFoundException;
import com.jangid.forging_process_management_service.exception.machining.MachiningBatchNotFoundException;
import com.jangid.forging_process_management_service.exception.quality.InspectionBatchNotFoundException;

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
    private ItemService itemService;

    @Autowired
    private WorkflowTemplateService workflowTemplateService;

    @Autowired
    private ItemWorkflowAssembler itemWorkflowAssembler;

    @Autowired
    private ItemWorkflowStepAssembler itemWorkflowStepAssembler;

    @Autowired
    private ForgeService forgeService;

    @Autowired
    private HeatTreatmentBatchService heatTreatmentBatchService;

    @Autowired
    private MachiningBatchService machiningBatchService;

    @Autowired
    private InspectionBatchService inspectionBatchService;

    @Autowired
    private VendorDispatchService vendorDispatchService;

    @Autowired
    private DispatchBatchService dispatchBatchService;

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

    @GetMapping("/tenant/{tenantId}/item-workflows/{workflowId}")
    @ApiOperation(value = "Get item workflow details by item workflow ID", 
                 notes = "Returns detailed workflow information including all workflow steps - alternative endpoint for frontend compatibility")
    public ResponseEntity<ItemWorkflowRepresentation> getItemWorkflowDetailsByItemWorkflowId(
            @ApiParam(value = "Tenant ID", required = true) @PathVariable Long tenantId,
            @ApiParam(value = "Item Workflow ID", required = true) @PathVariable Long workflowId) {
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
            log.error("Error fetching item workflow {} for tenant {}: {}", workflowId, tenantId, e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Unexpected error fetching item workflow {} for tenant {}: {}", workflowId, tenantId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/tenant/{tenantId}/item-workflows/{workflowId}/available-heats")
    @ApiOperation(value = "Get available heats from first operation of item workflow", 
                 notes = "Returns list of heats available from the first operation of the workflow for vendor transfer")
    public ResponseEntity<List<HeatInfoDTO>> getAvailableHeatsFromFirstOperation(
            @ApiParam(value = "Tenant ID", required = true) @PathVariable Long tenantId,
            @ApiParam(value = "Item Workflow ID", required = true) @PathVariable Long workflowId) {
        try {
            // Get the workflow and validate tenant
            ItemWorkflow itemWorkflow = itemWorkflowService.getItemWorkflowById(workflowId);
            
            if (itemWorkflow.getItem().getTenant().getId() != tenantId.longValue()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            // Get available heats from first operation
            List<HeatInfoDTO> availableHeats = itemWorkflowService.getAvailableHeatsFromFirstOperation(workflowId);
            return ResponseEntity.ok(availableHeats);
            
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error fetching available heats for workflow {}: {}", workflowId, e.getMessage());
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

    @GetMapping("/tenant/{tenantId}/item/{itemId}/workflows")
    @ApiOperation(value = "Get all workflows for a specific item", 
                 notes = "Returns all item workflows associated with the specified item")
    public ResponseEntity<ItemWorkflowListRepresentation> getItemWorkflows(
            @ApiParam(value = "Tenant ID", required = true) @PathVariable Long tenantId,
            @ApiParam(value = "Item ID", required = true) @PathVariable Long itemId) {
        try {
            // Get the item and validate it belongs to the tenant
            try {
                itemService.getItemOfTenant(tenantId, itemId);
            } catch (Exception e) {
                log.error("Item {} not found for tenant {}: {}", itemId, tenantId, e.getMessage());
                return ResponseEntity.notFound().build();
            }

            // Get all workflows for the specific item
            List<ItemWorkflow> itemWorkflows = itemWorkflowService.getItemWorkflowsByItemId(itemId);
            
            List<ItemWorkflowRepresentation> workflowRepresentations = itemWorkflows.stream()
                .map(itemWorkflowAssembler::dissemble)
                .collect(Collectors.toList());
            
            ItemWorkflowListRepresentation response = new ItemWorkflowListRepresentation(workflowRepresentations);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error fetching item workflows for item {} in tenant {}: {}", itemId, tenantId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/tenant/{tenantId}/item-workflows")
    @ApiOperation(value = "Get all item workflows for a tenant", 
                 notes = "Returns paginated or non-paginated list of item workflows ordered by updatedAt DESC")
    public ResponseEntity<?> getAllItemWorkflows(
            @ApiParam(value = "Tenant ID", required = true) @PathVariable Long tenantId,
            @ApiParam(value = "Page number (0-based)", defaultValue = "0") @RequestParam(value = "page") String page,
            @ApiParam(value = "Page size", defaultValue = "10") @RequestParam(value = "size") String size) {
        try {
            Integer pageNumber = (page == null || page.isBlank()) ? -1
                                                                  : GenericResourceUtils.convertResourceIdToInt(page)
                                     .orElseThrow(() -> new RuntimeException("Invalid page=" + page));

            Integer sizeNumber = (size == null || size.isBlank()) ? -1
                                                                  : GenericResourceUtils.convertResourceIdToInt(size)
                                     .orElseThrow(() -> new RuntimeException("Invalid size=" + size));

            if (pageNumber == -1 || sizeNumber == -1) {
                ItemWorkflowListRepresentation itemWorkflowListRepresentation = 
                    itemWorkflowService.getAllItemWorkflowsForTenantWithoutPaginationAsRepresentation(tenantId);
                return ResponseEntity.ok(itemWorkflowListRepresentation); // Returning list instead of paged response
            }

            Pageable pageable = PageRequest.of(pageNumber, sizeNumber);
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
    public ResponseEntity<?> completeItemWorkflow(
            @ApiParam(value = "Tenant ID", required = true) @PathVariable Long tenantId,
            @ApiParam(value = "Workflow ID", required = true) @PathVariable Long workflowId,
            @RequestBody CompleteWorkflowRequestRepresentation request) {
        try {
            // Validate request
            if (request.getCompletedAt() == null || request.getCompletedAt().isEmpty()) {
                log.warn("Completion time is required for completing workflow {}", workflowId);
                return new ResponseEntity<>(new ErrorResponse("Completion time is required"), HttpStatus.BAD_REQUEST);
            }
            
            // Convert String to LocalDateTime using ConvertorUtils
            LocalDateTime completedAtDateTime = ConvertorUtils.convertStringToLocalDateTime(request.getCompletedAt());
            
            // Get the workflow and validate it belongs to the tenant
            ItemWorkflow itemWorkflow = itemWorkflowService.getItemWorkflowById(workflowId);
            if (itemWorkflow.getItem().getTenant().getId() != tenantId.longValue()) {
                log.error("Workflow {} does not belong to tenant {}", workflowId, tenantId);
                return ResponseEntity.notFound().build();
            }
            
            // Validate that all batches in the workflow are completed
            String validationError = validateAllBatchesCompleted(itemWorkflow, tenantId);
            if (validationError != null) {
                log.error("Workflow completion validation failed for workflow {}: {}", workflowId, validationError);
                return new ResponseEntity<>(new ErrorResponse(validationError), HttpStatus.BAD_REQUEST);
            }
            
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

    @PostMapping("/tenant/{tenantId}/item/{itemId}/workflow")
    @ApiOperation(value = "Create item workflow", 
                 notes = "Creates a new workflow for the specified item using the provided workflow template")
    public ResponseEntity<?> createItemWorkflow(
            @ApiParam(value = "Tenant ID", required = true) @PathVariable Long tenantId,
            @ApiParam(value = "Item ID", required = true) @PathVariable Long itemId,
            @ApiParam(value = "Workflow Template ID", required = true) @RequestParam Long workflowTemplateId,
            @ApiParam(value = "Item Workflow Name (used as workflow identifier)", required = true) @RequestParam String itemWorkflowName) {
        try {
            // Validate input parameters
            if (tenantId == null || itemId == null || workflowTemplateId == null) {
                log.error("Invalid input parameters: tenantId={}, itemId={}, workflowTemplateId={}", 
                         tenantId, itemId, workflowTemplateId);
                return new ResponseEntity<>(new ErrorResponse("All parameters (tenantId, itemId, workflowTemplateId) are required"), 
                                          HttpStatus.BAD_REQUEST);
            }

            // Validate itemWorkflowName
            if (itemWorkflowName == null || itemWorkflowName.trim().isEmpty()) {
                log.error("Item workflow name is required for creating workflow");
                return new ResponseEntity<>(new ErrorResponse("Item workflow name is required"), 
                                          HttpStatus.BAD_REQUEST);
            }

            // Check if workflow identifier is already in use globally across the tenant
            String trimmedWorkflowName = itemWorkflowName.trim();
            boolean workflowIdentifierExists = itemWorkflowService.isWorkflowIdentifierExistsForTenant(tenantId, trimmedWorkflowName);
            if (workflowIdentifierExists) {
                log.error("Workflow identifier '{}' is already in use for tenant {}", trimmedWorkflowName, tenantId);
                return new ResponseEntity<>(new ErrorResponse("Workflow identifier '" + trimmedWorkflowName + "' is already in use. Please choose a different name."), 
                                          HttpStatus.CONFLICT);
            }

            // Get the item and validate it belongs to the tenant
            Item item;
            try {
                item =  itemService.getItemByIdAndTenantId(itemId, tenantId);
            } catch (Exception e) {
                log.error("Item {} not found for tenant {}: {}", itemId, tenantId, e.getMessage());
                return new ResponseEntity<>(new ErrorResponse("Item not found or does not belong to the specified tenant"), 
                                          HttpStatus.NOT_FOUND);
            }

            // Validate that the item belongs to the tenant
            if (item.getTenant().getId() != tenantId) {
                log.error("Item {} does not belong to tenant {}", itemId, tenantId);
                return new ResponseEntity<>(new ErrorResponse("Item does not belong to the specified tenant"), 
                                          HttpStatus.NOT_FOUND);
            }

            // Validate workflow template compatibility with the item
            String workflowValidationError = validateWorkflowTemplateCompatibility(item, workflowTemplateId, tenantId);
            if (workflowValidationError != null) {
                log.error("Workflow template validation failed: {}", workflowValidationError);
                return new ResponseEntity<>(
                    new ErrorResponse(workflowValidationError),
                    HttpStatus.BAD_REQUEST);
            }

            // Create the workflow with the provided itemWorkflowName as workflowIdentifier
            ItemWorkflow workflow = itemWorkflowService.createItemWorkflow(item, workflowTemplateId, trimmedWorkflowName);
            
            // Convert to representation
            ItemWorkflowRepresentation workflowRepresentation = itemWorkflowAssembler.dissemble(workflow);
            
            log.info("Successfully created workflow {} with identifier '{}' for item {} in tenant {}", 
                     workflow.getId(), trimmedWorkflowName, itemId, tenantId);
            
            return new ResponseEntity<>(workflowRepresentation, HttpStatus.CREATED);
            
        } catch (RuntimeException e) {
            if (e.getMessage().contains("already has an assigned workflow")) {
                log.error("Item {} already has a workflow: {}", itemId, e.getMessage());
                return new ResponseEntity<>(new ErrorResponse("Item already has an assigned workflow"), 
                                          HttpStatus.CONFLICT);
            }
            if (e.getMessage().contains("Workflow template does not belong to the same tenant")) {
                log.error("Workflow template validation failed for tenant {}: {}", tenantId, e.getMessage());
                return new ResponseEntity<>(new ErrorResponse("Workflow template does not belong to the specified tenant"), 
                                          HttpStatus.BAD_REQUEST);
            }
            if (e.getMessage().contains("Workflow template ID is required")) {
                log.error("Workflow template validation failed: {}", e.getMessage());
                return new ResponseEntity<>(new ErrorResponse(e.getMessage()), 
                                          HttpStatus.BAD_REQUEST);
            }
            log.error("Error creating workflow for item {} in tenant {}: {}", itemId, tenantId, e.getMessage());
            return new ResponseEntity<>(new ErrorResponse("Error creating workflow: " + e.getMessage()), 
                                      HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("Unexpected error creating workflow for item {} in tenant {}: {}", itemId, tenantId, e.getMessage());
            return new ResponseEntity<>(new ErrorResponse("Unexpected error creating workflow"), 
                                      HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Validates that a workflow template is compatible with an item
     * @param item The item to validate against
     * @param workflowTemplateId The workflow template ID to validate
     * @param tenantId The tenant ID for validation
     * @return null if valid, error message if invalid
     */
    private String validateWorkflowTemplateCompatibility(Item item, Long workflowTemplateId, Long tenantId) {
        try {
            // Get the workflow template
            WorkflowTemplate workflowTemplate = workflowTemplateService.getWorkflowTemplateById(workflowTemplateId);
            
            // Validate that template belongs to the same tenant
            if (workflowTemplate.getTenant().getId() != tenantId.longValue()) {
                return "Workflow template does not belong to the specified tenant";
            }
            
            // Get the first step of the workflow
            WorkflowStep firstStep = workflowTemplate.getFirstStep();
            if (firstStep == null) {
                return "Workflow template has no steps defined";
            }
            
            // Determine if the item has KGS or PIECES products
            boolean hasKgsProduct = false;
            boolean hasPiecesProduct = false;
            
            if (item.getItemProducts() != null) {
                for (var itemProduct : item.getItemProducts()) {
                    if (itemProduct.getProduct() != null && itemProduct.getProduct().getUnitOfMeasurement() != null) {
                        String unitOfMeasurement = itemProduct.getProduct().getUnitOfMeasurement().name();
                        if ("KGS".equals(unitOfMeasurement)) {
                            hasKgsProduct = true;
                        } else if ("PIECES".equals(unitOfMeasurement)) {
                            hasPiecesProduct = true;
                        }
                    }
                }
            }
            
            // Validate workflow compatibility
            if (hasKgsProduct && !hasPiecesProduct) {
                // For KGS products, first step should be FORGING or VENDOR
                if (firstStep.getOperationType() != WorkflowStep.OperationType.FORGING && 
                    firstStep.getOperationType() != WorkflowStep.OperationType.VENDOR) {
                    return "KGS items must start with either FORGING or VENDOR workflow. Please select a workflow that begins with FORGING or VENDOR.";
                }
            } else if (hasPiecesProduct && !hasKgsProduct) {
                // For PIECES products, first step should NOT be FORGING
                if (firstStep.getOperationType() == WorkflowStep.OperationType.FORGING) {
                    return "PIECES items cannot start with FORGING workflow. Please select a workflow that begins with another operation.";
                }
            } else if (hasKgsProduct && hasPiecesProduct) {
                return "Items cannot have both KGS and PIECES products. Please select only one unit of measurement.";
            }
            
            return null; // No validation errors
            
        } catch (Exception e) {
            log.error("Error validating workflow template compatibility: {}", e.getMessage());
            return "Error validating workflow template: " + e.getMessage();
        }
    }

    /**
     * Get the end time of the last operation for validation when creating a new workflow step.
     * This API is used by the UI to validate the creation time of the current operation of ItemWorkflowStep.
     * 
     * @param tenantId The tenant ID
     * @param operationEntityId The operation entity ID (meaning varies by operation type)
     * @param operationType The operation type (FORGING, HEAT_TREATMENT, MACHINING, QUALITY, VENDOR)
     * @return OperationEndTimeDTO containing the end time and source information
     */
    @GetMapping("/tenant/{tenantId}/workflow/operation-end-time")
    @ApiOperation(value = "Get operation end time for workflow validation", 
                 notes = "Returns the end time of the specified operation for workflow step creation validation")
    public ResponseEntity<?> getOperationEndTimeForValidation(
        @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable("tenantId") String tenantId,
        @ApiParam(value = "Operation entity ID (processedItemId for FORGING, processedItemHeatTreatmentBatchId for HEAT_TREATMENT, etc.)", required = true) @RequestParam("operationEntityId") String operationEntityId,
        @ApiParam(value = "Operation type", required = true, allowableValues = "FORGING,HEAT_TREATMENT,MACHINING,QUALITY,VENDOR") @RequestParam("operationType") String operationType) {

        try {
            // Validate input parameters
            if (tenantId == null || tenantId.isEmpty() || operationEntityId == null || operationEntityId.isEmpty() || 
                operationType == null || operationType.trim().isEmpty()) {
                log.error("Invalid input for getOperationEndTimeForValidation - missing required parameters");
                return new ResponseEntity<>(new ErrorResponse("Tenant ID, operation entity ID, and operation type are required"), HttpStatus.BAD_REQUEST);
            }

            Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
                .orElseThrow(() -> new RuntimeException("Not valid tenantId!"));
            Long operationEntityIdLongValue = GenericResourceUtils.convertResourceIdToLong(operationEntityId)
                .orElseThrow(() -> new RuntimeException("Not valid operationEntityId!"));

            // Normalize operation type
            String normalizedOperationType = operationType.trim().toUpperCase();
            
            // Validate operation type
            if (!isValidOperationType(normalizedOperationType)) {
                log.error("Invalid operation type: {}", normalizedOperationType);
                return new ResponseEntity<>(new ErrorResponse("Invalid operation type. Allowed values: FORGING, HEAT_TREATMENT, MACHINING, QUALITY, VENDOR"), HttpStatus.BAD_REQUEST);
            }

            // Get end time based on operation type
            OperationEndTimeDTO result = getEndTimeForOperationType(normalizedOperationType, operationEntityIdLongValue, tenantIdLongValue);
            return ResponseEntity.ok(result);

        } catch (Exception exception) {
            if (exception instanceof ForgeNotFoundException || 
                exception instanceof HeatTreatmentBatchNotFoundException ||
                exception instanceof MachiningBatchNotFoundException ||
                exception instanceof InspectionBatchNotFoundException) {
                log.error("Entity not found for operation type {} and operation entity {}: {}", operationType, operationEntityId, exception.getMessage());
                return ResponseEntity.notFound().build();
            }
            if (exception instanceof IllegalArgumentException) {
                log.error("Invalid data for getOperationEndTimeForValidation: {}", exception.getMessage());
                return new ResponseEntity<>(new ErrorResponse(exception.getMessage()), HttpStatus.BAD_REQUEST);
            }
            log.error("Error processing getOperationEndTimeForValidation: {}", exception.getMessage());
            return new ResponseEntity<>(new ErrorResponse("Error retrieving operation end time"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Validates if the operation type is supported
     */
    private boolean isValidOperationType(String operationType) {
        return "FORGING".equals(operationType) ||
               "HEAT_TREATMENT".equals(operationType) ||
               "MACHINING".equals(operationType) ||
               "QUALITY".equals(operationType) ||
               "VENDOR".equals(operationType);
    }

    /**
     * Gets the end time for the specified operation type and operation entity
     */
    private OperationEndTimeDTO getEndTimeForOperationType(String operationType, Long operationEntityId, Long tenantId) {
        switch (operationType) {
            case "FORGING":
                return getForgeEndTime(operationEntityId, tenantId);
            case "HEAT_TREATMENT":
                return getHeatTreatmentEndTime(operationEntityId, tenantId);
            case "MACHINING":
                return getMachiningEndTime(operationEntityId, tenantId);
            case "QUALITY":
                return getQualityEndTime(operationEntityId, tenantId);
            case "VENDOR":
                return getVendorEndTime(operationEntityId, tenantId);
            default:
                throw new IllegalArgumentException("Unsupported operation type: " + operationType);
        }
    }

    /**
     * Gets the end time for FORGING operation
     * Priority: forge.endAt > lastForgeShift.endDateTime
     */
    private OperationEndTimeDTO getForgeEndTime(Long processedItemId, Long tenantId) {
        try {
            Forge forge = forgeService.getForgeByProcessedItemId(processedItemId);
            
            // Validate that the forge belongs to the tenant
            if (forge.getTenant().getId() != tenantId) {
                throw new ForgeNotFoundException("Forge does not exist for the specified tenant and processed item");
            }

            // Check if forge has endAt
            if (forge.getEndAt() != null) {
                return OperationEndTimeDTO.builder()
                    .endTime(forge.getEndAt().toString())
                    .operationType("FORGING")
                    .processedItemId(processedItemId)
                    .source("forge.endAt")
                    .build();
            }

            // If no endAt, get the last forge shift's endDateTime
            if (forge.getForgeShifts() != null && !forge.getForgeShifts().isEmpty()) {
                ForgeShift lastForgeShift = forge.getForgeShifts().stream()
                    .reduce((_, second) -> second) // Get the last element
                    .orElse(null);
                
                if (lastForgeShift != null && lastForgeShift.getEndDateTime() != null) {
                    return OperationEndTimeDTO.builder()
                        .endTime(lastForgeShift.getEndDateTime().toString())
                        .operationType("FORGING")
                        .processedItemId(processedItemId)
                        .source("lastForgeShift.endDateTime")
                        .build();
                }
            }

            throw new IllegalStateException("No end time available for forge operation");
        } catch (Exception e) {
            log.error("Error getting forge end time for processedItemId {}: {}", processedItemId, e.getMessage());
            throw e;
        }
    }

    /**
     * Gets the end time for HEAT_TREATMENT operation
     */
    private OperationEndTimeDTO getHeatTreatmentEndTime(Long processedItemHeatTreatmentBatchId, Long tenantId) {
        try {
            // For heat treatment, we need to find the heat treatment batch by processed item heat treatment batch
            HeatTreatmentBatch heatTreatmentBatch = heatTreatmentBatchService.getHeatTreatmentBatchByProcessedItemHeatTreatmentBatchId(processedItemHeatTreatmentBatchId);
            
            // Validate that the heat treatment batch belongs to the tenant
            if (heatTreatmentBatch.getTenant().getId() != tenantId) {
                throw new HeatTreatmentBatchNotFoundException("Heat treatment batch does not exist for the specified tenant and processed item");
            }

            if (heatTreatmentBatch.getEndAt() != null) {
                return OperationEndTimeDTO.builder()
                    .endTime(heatTreatmentBatch.getEndAt().toString())
                    .operationType("HEAT_TREATMENT")
                    .processedItemId(processedItemHeatTreatmentBatchId)
                    .source("heatTreatmentBatch.endAt")
                    .build();
            }

            throw new IllegalStateException("No end time available for heat treatment operation");
        } catch (Exception e) {
            log.error("Error getting heat treatment end time for processedItemHeatTreatmentBatchId {}: {}", processedItemHeatTreatmentBatchId, e.getMessage());
            throw e;
        }
    }

    /**
     * Gets the end time for MACHINING operation
     * Priority: machiningBatch.endAt > lastDailyMachiningBatch.endDateTime
     */
    private OperationEndTimeDTO getMachiningEndTime(Long processedItemMachiningBatchId, Long tenantId) {
        try {
            // For machining, we need to find the machining batch by processed item machining batch
            MachiningBatchRepresentation machiningBatchRep =
                machiningBatchService.getMachiningBatchByProcessedItemMachiningBatchId(processedItemMachiningBatchId, tenantId);
            
            if (machiningBatchRep == null) {
                throw new MachiningBatchNotFoundException("No machining batch found for processed item machining batch ID");
            }

            // Check if machining batch has endAt
            if (machiningBatchRep.getEndAt() != null && !machiningBatchRep.getEndAt().isEmpty()) {
                return OperationEndTimeDTO.builder()
                    .endTime(machiningBatchRep.getEndAt())
                    .operationType("MACHINING")
                    .processedItemId(processedItemMachiningBatchId)
                    .source("machiningBatch.endAt")
                    .build();
            }

            // If no endAt, get the last daily machining batch's endDateTime
            if (machiningBatchRep.getDailyMachiningBatchDetail() != null && !machiningBatchRep.getDailyMachiningBatchDetail().isEmpty()) {
                DailyMachiningBatchRepresentation lastDailyBatch =
                    machiningBatchRep.getDailyMachiningBatchDetail().stream()
                        .reduce((_, second) -> second) // Get the last element
                        .orElse(null);
                
                if (lastDailyBatch.getEndDateTime() != null && !lastDailyBatch.getEndDateTime().isEmpty()) {
                    return OperationEndTimeDTO.builder()
                        .endTime(lastDailyBatch.getEndDateTime())
                        .operationType("MACHINING")
                        .processedItemId(processedItemMachiningBatchId)
                        .source("lastDailyMachiningBatch.endDateTime")
                        .build();
                }
            }

            throw new IllegalStateException("No end time available for machining operation");
        } catch (Exception e) {
            log.error("Error getting machining end time for processedItemMachiningBatchId {}: {}", processedItemMachiningBatchId, e.getMessage());
            throw e;
        }
    }

    /**
     * Gets the end time for QUALITY operation
     */
    private OperationEndTimeDTO getQualityEndTime(Long processedItemInspectionBatchId, Long tenantId) {
        try {
            // For quality, we need to find the inspection batch by processed item inspection batch
            InspectionBatchRepresentation inspectionBatchRep =
                inspectionBatchService.getInspectionBatchByProcessedItemInspectionBatchId(processedItemInspectionBatchId, tenantId);
            
            if (inspectionBatchRep == null) {
                throw new InspectionBatchNotFoundException("No inspection batch found for processed item inspection batch ID");
            }

            if (inspectionBatchRep.getEndAt() != null && !inspectionBatchRep.getEndAt().isEmpty()) {
                return OperationEndTimeDTO.builder()
                    .endTime(inspectionBatchRep.getEndAt())
                    .operationType("QUALITY")
                    .processedItemId(processedItemInspectionBatchId)
                    .source("inspectionBatch.endAt")
                    .build();
            }

            throw new IllegalStateException("No end time available for quality operation");
        } catch (Exception e) {
            log.error("Error getting quality end time for processedItemInspectionBatchId {}: {}", processedItemInspectionBatchId, e.getMessage());
            throw e;
        }
    }

    /**
     * Gets the end time for VENDOR operation
     * Returns the last VendorReceiveBatch's receivedAt from the VendorDispatchBatch
     */
    private OperationEndTimeDTO getVendorEndTime(Long processedItemVendorDispatchBatchId, Long tenantId) {
        try {
            VendorDispatchBatchRepresentation vendorDispatchBatchRep =
                vendorDispatchService.getVendorDispatchBatchByProcessedItemVendorDispatchBatchId(processedItemVendorDispatchBatchId);
            
            if (vendorDispatchBatchRep == null) {
                throw new RuntimeException("No vendor dispatch batch found for processed item vendor dispatch batch ID");
            }

            // Get the last vendor receive batch's receivedAt
            if (vendorDispatchBatchRep.getVendorReceiveBatches() != null && !vendorDispatchBatchRep.getVendorReceiveBatches().isEmpty()) {
                com.jangid.forging_process_management_service.entitiesRepresentation.vendor.VendorReceiveBatchRepresentation lastReceiveBatch = 
                    vendorDispatchBatchRep.getVendorReceiveBatches().stream()
                        .reduce((_, second) -> second) // Get the last element
                        .orElse(null);
                
                if (lastReceiveBatch.getReceivedAt() != null && !lastReceiveBatch.getReceivedAt().isEmpty()) {
                    return OperationEndTimeDTO.builder()
                        .endTime(lastReceiveBatch.getReceivedAt())
                        .operationType("VENDOR")
                        .processedItemId(processedItemVendorDispatchBatchId)
                        .source("lastVendorReceiveBatch.receivedAt")
                        .build();
                }
            }

            throw new IllegalStateException("No receive time available for vendor operation");
        } catch (Exception e) {
            log.error("Error getting vendor end time for processedItemVendorDispatchBatchId {}: {}", processedItemVendorDispatchBatchId, e.getMessage());
            throw e;
        }
    }

    /**
     * Validates that all batches associated with ItemWorkflowSteps are completed
     * @param itemWorkflow The workflow to validate
     * @param tenantId The tenant ID for validation
     * @return null if all batches are completed, error message if any batch is not completed
     */
    private String validateAllBatchesCompleted(ItemWorkflow itemWorkflow, Long tenantId) {
        log.info("Validating completion status for all batches in workflow {}", itemWorkflow.getId());
        
        for (ItemWorkflowStep step : itemWorkflow.getItemWorkflowSteps()) {
            if (step.getRelatedEntityIds() == null || step.getRelatedEntityIds().isEmpty()) {
                // If no related entities, consider step as not started/completed
                return String.format("Step '%s' (%s) has no associated batches. All workflow steps must have completed batches before workflow completion.", 
                                   step.getWorkflowStep().getStepName(), step.getOperationType());
            }
            
            // Validate each related entity based on operation type
            for (Long relatedEntityId : step.getRelatedEntityIds()) {
                String batchValidationError = validateBatchCompletion(step.getOperationType(), relatedEntityId, tenantId);
                if (batchValidationError != null) {
                    return String.format("Step '%s' (%s): %s", 
                                       step.getWorkflowStep().getStepName(), step.getOperationType(), batchValidationError);
                }
            }
        }
        
        log.info("All batches in workflow {} are completed", itemWorkflow.getId());
        return null; // All batches are completed
    }

    /**
     * Validates completion status of a specific batch based on operation type
     * @param operationType The operation type
     * @param relatedEntityId The ID of the related entity (batch/processed item)
     * @param tenantId The tenant ID for validation
     * @return null if batch is completed, error message if not completed
     */
    private String validateBatchCompletion(WorkflowStep.OperationType operationType, Long relatedEntityId, Long tenantId) {
        try {
            switch (operationType) {
                case FORGING:
                    return validateForgingCompletion(relatedEntityId, tenantId);
                case HEAT_TREATMENT:
                    return validateHeatTreatmentCompletion(relatedEntityId, tenantId);
                case MACHINING:
                    return validateMachiningCompletion(relatedEntityId, tenantId);
                case QUALITY:
                    return validateQualityCompletion(relatedEntityId, tenantId);
                case VENDOR:
                    return validateVendorCompletion(relatedEntityId, tenantId);
                case DISPATCH:
                    return validateDispatchCompletion(relatedEntityId, tenantId);
                default:
                    return "Unsupported operation type: " + operationType;
            }
        } catch (Exception e) {
            log.error("Error validating batch completion for {} operation with ID {}: {}", 
                     operationType, relatedEntityId, e.getMessage());
            return String.format("Error validating %s batch with ID %d: %s", operationType, relatedEntityId, e.getMessage());
        }
    }

    /**
     * Validates FORGING operation completion
     */
    private String validateForgingCompletion(Long processedItemId, Long tenantId) {
        try {
            Forge forge = forgeService.getForgeByProcessedItemId(processedItemId);
            
            // Validate tenant
            if (forge.getTenant().getId() != tenantId) {
                return String.format("Forge for processed item %d does not belong to tenant %d", processedItemId, tenantId);
            }
            
            // Check completion status
            if (forge.getForgingStatus() != Forge.ForgeStatus.COMPLETED) {
                return String.format("Forge for processed item %d is not completed (current status: %s)", 
                                   processedItemId, forge.getForgingStatus());
            }
            
            return null; // Completed
        } catch (Exception e) {
            return String.format("Forge for processed item %d not found or error occurred: %s", processedItemId, e.getMessage());
        }
    }

    /**
     * Validates HEAT_TREATMENT operation completion
     */
    private String validateHeatTreatmentCompletion(Long processedItemHeatTreatmentBatchId, Long tenantId) {
        try {
            HeatTreatmentBatch heatTreatmentBatch = heatTreatmentBatchService.getHeatTreatmentBatchByProcessedItemHeatTreatmentBatchId(processedItemHeatTreatmentBatchId);
            
            // Validate tenant
            if (heatTreatmentBatch.getTenant().getId() != tenantId) {
                return String.format("Heat treatment batch for processed item %d does not belong to tenant %d", 
                                   processedItemHeatTreatmentBatchId, tenantId);
            }
            
            // Check completion status
            if (heatTreatmentBatch.getHeatTreatmentBatchStatus() != HeatTreatmentBatch.HeatTreatmentBatchStatus.COMPLETED) {
                return String.format("Heat treatment batch for processed item %d is not completed (current status: %s)", 
                                   processedItemHeatTreatmentBatchId, heatTreatmentBatch.getHeatTreatmentBatchStatus());
            }
            
            return null; // Completed
        } catch (Exception e) {
            return String.format("Heat treatment batch for processed item %d not found or error occurred: %s", 
                               processedItemHeatTreatmentBatchId, e.getMessage());
        }
    }

    /**
     * Validates MACHINING operation completion
     */
    private String validateMachiningCompletion(Long processedItemMachiningBatchId, Long tenantId) {
        try {
            MachiningBatchRepresentation machiningBatch = machiningBatchService.getMachiningBatchByProcessedItemMachiningBatchId(processedItemMachiningBatchId, tenantId);
            
            if (machiningBatch == null) {
                return String.format("Machining batch for processed item %d not found", processedItemMachiningBatchId);
            }
            
            // Check completion status
            if (!"COMPLETED".equals(machiningBatch.getMachiningBatchStatus())) {
                return String.format("Machining batch for processed item %d is not completed (current status: %s)", 
                                   processedItemMachiningBatchId, machiningBatch.getMachiningBatchStatus());
            }
            
            return null; // Completed
        } catch (Exception e) {
            return String.format("Machining batch for processed item %d not found or error occurred: %s", 
                               processedItemMachiningBatchId, e.getMessage());
        }
    }

    /**
     * Validates QUALITY operation completion
     */
    private String validateQualityCompletion(Long processedItemInspectionBatchId, Long tenantId) {
        try {
            InspectionBatchRepresentation inspectionBatch = inspectionBatchService.getInspectionBatchByProcessedItemInspectionBatchId(processedItemInspectionBatchId, tenantId);
            
            if (inspectionBatch == null) {
                return String.format("Inspection batch for processed item %d not found", processedItemInspectionBatchId);
            }
            
            // Check completion status
            if (!"COMPLETED".equals(inspectionBatch.getInspectionBatchStatus())) {
                return String.format("Inspection batch for processed item %d is not completed (current status: %s)", 
                                   processedItemInspectionBatchId, inspectionBatch.getInspectionBatchStatus());
            }
            
            return null; // Completed
        } catch (Exception e) {
            return String.format("Inspection batch for processed item %d not found or error occurred: %s", 
                               processedItemInspectionBatchId, e.getMessage());
        }
    }

    /**
     * Validates VENDOR operation completion
     * For vendor operations, we check if the processed item is fully received
     */
    private String validateVendorCompletion(Long processedItemVendorDispatchBatchId, Long tenantId) {
        try {
            VendorDispatchBatchRepresentation vendorDispatchBatch = vendorDispatchService.getVendorDispatchBatchByProcessedItemVendorDispatchBatchId(processedItemVendorDispatchBatchId);
            
            if (vendorDispatchBatch == null) {
                return String.format("Vendor dispatch batch for processed item %d not found", processedItemVendorDispatchBatchId);
            }
            
            // Check if fully received (this indicates completion of vendor processing)
            if (vendorDispatchBatch.getProcessedItem() == null || 
                !Boolean.TRUE.equals(vendorDispatchBatch.getProcessedItem().getFullyReceived())) {
                return String.format("Vendor dispatch batch for processed item %d is not fully received/completed", 
                                   processedItemVendorDispatchBatchId);
            }
            
            return null; // Completed
        } catch (Exception e) {
            return String.format("Vendor dispatch batch for processed item %d not found or error occurred: %s", 
                               processedItemVendorDispatchBatchId, e.getMessage());
        }
    }

    /**
     * Validates DISPATCH operation completion
     */
    private String validateDispatchCompletion(Long processedItemDispatchBatchId, Long tenantId) {
        try {
            DispatchBatch dispatchBatch = dispatchBatchService.getDispatchBatchById(processedItemDispatchBatchId);
            
            // Validate tenant
            if (dispatchBatch.getTenant().getId() != tenantId) {
                return String.format("Dispatch batch for processed item %d does not belong to tenant %d", 
                                   processedItemDispatchBatchId, tenantId);
            }
            
            // Check completion status
            if (dispatchBatch.getDispatchBatchStatus() != DispatchBatch.DispatchBatchStatus.DISPATCHED) {
                return String.format("Dispatch batch for processed item %d is not dispatched/completed (current status: %s)", 
                                   processedItemDispatchBatchId, dispatchBatch.getDispatchBatchStatus());
            }
            
            return null; // Completed
        } catch (Exception e) {
            return String.format("Dispatch batch for processed item %d not found or error occurred: %s", 
                               processedItemDispatchBatchId, e.getMessage());
        }
    }

} 