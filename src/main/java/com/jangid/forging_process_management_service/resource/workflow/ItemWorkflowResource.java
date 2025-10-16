package com.jangid.forging_process_management_service.resource.workflow;

import com.jangid.forging_process_management_service.configuration.security.TenantContextHolder;
import com.jangid.forging_process_management_service.entities.workflow.ItemWorkflow;
import com.jangid.forging_process_management_service.entities.workflow.ItemWorkflowStep;
import com.jangid.forging_process_management_service.entities.workflow.WorkflowStep;
import com.jangid.forging_process_management_service.entities.workflow.WorkflowTemplate;
import com.jangid.forging_process_management_service.entities.product.Item;
import com.jangid.forging_process_management_service.entities.forging.Forge;
import com.jangid.forging_process_management_service.entities.forging.ForgeShift;
import com.jangid.forging_process_management_service.entities.heating.HeatTreatmentBatch;

import com.jangid.forging_process_management_service.entitiesRepresentation.machining.DailyMachiningBatchRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.machining.MachiningBatchRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.quality.InspectionBatchRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.vendor.VendorDispatchBatchRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.workflow.ItemWorkflowRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.workflow.ItemWorkflowStepRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.workflow.CompleteWorkflowRequestRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.workflow.ItemWorkflowPageResponseRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.workflow.ItemWorkflowListRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.product.ItemRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.product.ItemPageResponseRepresentation;
import com.jangid.forging_process_management_service.service.workflow.ItemWorkflowService;
import com.jangid.forging_process_management_service.service.product.ItemService;
import com.jangid.forging_process_management_service.service.workflow.WorkflowTemplateService;
import com.jangid.forging_process_management_service.service.forging.ForgeService;
import com.jangid.forging_process_management_service.service.heating.HeatTreatmentBatchService;
import com.jangid.forging_process_management_service.service.machining.MachiningBatchService;
import com.jangid.forging_process_management_service.service.quality.InspectionBatchService;
import com.jangid.forging_process_management_service.service.vendor.VendorDispatchService;

import com.jangid.forging_process_management_service.assemblers.workflow.ItemWorkflowAssembler;
import com.jangid.forging_process_management_service.assemblers.workflow.ItemWorkflowStepAssembler;
import com.jangid.forging_process_management_service.utils.ConvertorUtils;
import com.jangid.forging_process_management_service.dto.ItemWorkflowTrackingResultDTO;
import com.jangid.forging_process_management_service.dto.HeatInfoDTO;
import com.jangid.forging_process_management_service.dto.OperationEndTimeDTO;
import com.jangid.forging_process_management_service.utils.GenericResourceUtils;
import com.jangid.forging_process_management_service.utils.GenericExceptionHandler;
import com.jangid.forging_process_management_service.exception.forging.ForgeNotFoundException;
import com.jangid.forging_process_management_service.exception.heating.HeatTreatmentBatchNotFoundException;
import com.jangid.forging_process_management_service.exception.machining.MachiningBatchNotFoundException;
import com.jangid.forging_process_management_service.exception.quality.InspectionBatchNotFoundException;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private ObjectMapper objectMapper;

    @GetMapping("/workflows/{workflowId}")
    @ApiOperation(value = "Get item workflow details", 
                 notes = "Returns detailed workflow information including all workflow steps")
    public ResponseEntity<?> getItemWorkflowDetails(
            @ApiParam(value = "Workflow ID", required = true) @PathVariable Long workflowId) {
        try {
            // Get the workflow
            ItemWorkflow itemWorkflow = itemWorkflowService.getItemWorkflowById(workflowId);

            Long tenantId = TenantContextHolder.getAuthenticatedTenantId();
            // Validate that workflow belongs to the tenant
            if (itemWorkflow.getItem().getTenant().getId() != tenantId) {
                return ResponseEntity.notFound().build();
            }
            
            // Convert to representation (includes workflow steps)
            ItemWorkflowRepresentation workflowRepresentation = itemWorkflowAssembler.dissemble(itemWorkflow);
            
            return ResponseEntity.ok(workflowRepresentation);
            
        } catch (Exception exception) {
            return GenericExceptionHandler.handleException(exception, "getItemWorkflowDetails");
        }
    }

    @GetMapping("/item-workflows/{workflowId}")
    @ApiOperation(value = "Get item workflow details by item workflow ID", 
                 notes = "Returns detailed workflow information including all workflow steps - alternative endpoint for frontend compatibility")
    public ResponseEntity<?> getItemWorkflowDetailsByItemWorkflowId(
            @ApiParam(value = "Item Workflow ID", required = true) @PathVariable Long workflowId) {
        try {
            // Get the workflow
            ItemWorkflow itemWorkflow = itemWorkflowService.getItemWorkflowById(workflowId);
            Long tenantId = TenantContextHolder.getAuthenticatedTenantId();
            // Validate that workflow belongs to the tenant
            if (itemWorkflow.getItem().getTenant().getId() != tenantId) {
                return ResponseEntity.notFound().build();
            }
            
            // Convert to representation (includes workflow steps)
            ItemWorkflowRepresentation workflowRepresentation = itemWorkflowAssembler.dissemble(itemWorkflow);
            
            return ResponseEntity.ok(workflowRepresentation);
            
        } catch (Exception exception) {
            return GenericExceptionHandler.handleException(exception, "getItemWorkflowDetailsByItemWorkflowId");
        }
    }

    @GetMapping("/item-workflows/{workflowId}/available-heats")
    @ApiOperation(value = "Get available heats from first operation of item workflow", 
                 notes = "Returns list of heats available from the first operation of the workflow for vendor transfer")
    public ResponseEntity<?> getAvailableHeatsFromFirstOperation(
            @ApiParam(value = "Item Workflow ID", required = true) @PathVariable Long workflowId) {
        try {
            // Get the workflow and validate tenant
            ItemWorkflow itemWorkflow = itemWorkflowService.getItemWorkflowById(workflowId);
            Long tenantId = TenantContextHolder.getAuthenticatedTenantId();
            if (itemWorkflow.getItem().getTenant().getId() != tenantId) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            // Get available heats from first operation
            List<HeatInfoDTO> availableHeats = itemWorkflowService.getAvailableHeatsFromFirstOperation(workflowId);
            return ResponseEntity.ok(availableHeats);
            
        } catch (Exception exception) {
            return GenericExceptionHandler.handleException(exception, "getAvailableHeatsFromFirstOperation");
        }
    }

    @GetMapping("/workflows/{workflowId}/steps")
    @ApiOperation(value = "Get workflow steps for a workflow", 
                 notes = "Returns all workflow steps for the specified workflow")
    public ResponseEntity<?> getWorkflowSteps(
            @ApiParam(value = "Workflow ID", required = true) @PathVariable Long workflowId) {
        try {
            // Get the workflow
            ItemWorkflow itemWorkflow = itemWorkflowService.getItemWorkflowById(workflowId);
            Long tenantId = TenantContextHolder.getAuthenticatedTenantId();
            // Validate that workflow belongs to the tenant
            if (itemWorkflow.getItem().getTenant().getId() != tenantId) {
                return ResponseEntity.notFound().build();
            }
            
            // Convert workflow steps to representations
            List<ItemWorkflowStepRepresentation> workflowSteps = itemWorkflowStepAssembler.dissemble(
                itemWorkflow.getItemWorkflowSteps()
            );
            
            return ResponseEntity.ok(workflowSteps);
            
        } catch (Exception exception) {
            return GenericExceptionHandler.handleException(exception, "getWorkflowSteps");
        }
    }

    @GetMapping("/workflows/{workflowId}/steps/{stepId}")
    @ApiOperation(value = "Get specific workflow step details", 
                 notes = "Returns detailed information about a specific workflow step")
    public ResponseEntity<?> getWorkflowStep(

            @ApiParam(value = "Workflow ID", required = true) @PathVariable Long workflowId,
            @ApiParam(value = "Step ID", required = true) @PathVariable Long stepId) {
        try {
            // Get the workflow
            ItemWorkflow itemWorkflow = itemWorkflowService.getItemWorkflowById(workflowId);
            Long tenantId = TenantContextHolder.getAuthenticatedTenantId();
            // Validate that workflow belongs to the tenant
            if (itemWorkflow.getItem().getTenant().getId() != tenantId) {
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
            
        } catch (Exception exception) {
            return GenericExceptionHandler.handleException(exception, "getWorkflowStep");
        }
    }

    @GetMapping("/workflows/{workflowId}/steps/operation/{operationType}")
    @ApiOperation(value = "Get workflow step by operation type", 
                 notes = "Returns the workflow step for a specific operation type")
        public ResponseEntity<?> getWorkflowStepByOperation(

            @ApiParam(value = "Workflow ID", required = true) @PathVariable Long workflowId,
            @ApiParam(value = "Operation Type", required = true) @PathVariable String operationType) {
        try {
            // Get the workflow
            ItemWorkflow itemWorkflow = itemWorkflowService.getItemWorkflowById(workflowId);
            Long tenantId = TenantContextHolder.getAuthenticatedTenantId();
            // Validate that workflow belongs to the tenant
            if (itemWorkflow.getItem().getTenant().getId() != tenantId) {
                return ResponseEntity.notFound().build();
            }
            
            // Parse operation type
            WorkflowStep.OperationType operation;
            try {
                operation = WorkflowStep.OperationType.valueOf(operationType.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid operation type: {}", operationType);
                throw new IllegalArgumentException("Invalid operation type: " + operationType);
            }
            
            // Find the workflow step by operation type
            ItemWorkflowStep workflowStep = itemWorkflow.getStepByOperationType(operation);
            
            if (workflowStep == null) {
                return ResponseEntity.notFound().build();
            }
            
            // Convert to representation
            ItemWorkflowStepRepresentation stepRepresentation = itemWorkflowStepAssembler.dissemble(workflowStep);
            
            return ResponseEntity.ok(stepRepresentation);
            
        } catch (Exception exception) {
            return GenericExceptionHandler.handleException(exception, "getWorkflowStepByOperation");
        }
    }

    @GetMapping("/item/{itemId}/workflows")
    @ApiOperation(value = "Get all workflows for a specific item", 
                 notes = "Returns all item workflows associated with the specified item")
    public ResponseEntity<?> getItemWorkflows(

            @ApiParam(value = "Item ID", required = true) @PathVariable Long itemId) {
        try {
            // Get the item and validate it belongs to the tenant
            Long tenantId = TenantContextHolder.getAuthenticatedTenantId();
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
            
        } catch (Exception exception) {
            return GenericExceptionHandler.handleException(exception, "getItemWorkflows");
        }
    }

    @GetMapping("/item/{itemId}/workflows/template/{workflowTemplateId}")
    @ApiOperation(value = "Get available workflows for a specific item and workflow template combination", 
                 notes = "Returns NOT_STARTED item workflows that are not yet associated with any order. " +
                         "This ensures only available workflows are returned for order creation.")
    public ResponseEntity<?> getNotStartedItemWorkflowsByItemAndTemplate(
            @ApiParam(value = "Tenant ID", required = true) @PathVariable Long tenantId,
            @ApiParam(value = "Item ID", required = true) @PathVariable Long itemId,
            @ApiParam(value = "Workflow Template ID", required = true) @PathVariable Long workflowTemplateId) {
        try {
            // Get the item and validate it belongs to the tenant
            try {
                itemService.getItemOfTenant(tenantId, itemId);
            } catch (Exception e) {
                log.error("Item {} not found for tenant {}: {}", itemId, tenantId, e.getMessage());
                return ResponseEntity.notFound().build();
            }

            // Validate that the workflow template belongs to the tenant
            try {
                WorkflowTemplate workflowTemplate = workflowTemplateService.getWorkflowTemplateById(workflowTemplateId);
                if (workflowTemplate.getTenant().getId() != tenantId) {
                    log.error("Workflow template {} does not belong to tenant {}", workflowTemplateId, tenantId);
                    return ResponseEntity.notFound().build();
                }
            } catch (Exception e) {
                log.error("Workflow template {} not found: {}", workflowTemplateId, e.getMessage());
                return ResponseEntity.notFound().build();
            }

            // Get workflows for the specific item and workflow template combination
            List<ItemWorkflow> itemWorkflows = itemWorkflowService.getNotStartedItemWorkflowsByItemIdAndWorkflowTemplateId(itemId, workflowTemplateId);
            
            List<ItemWorkflowRepresentation> workflowRepresentations = itemWorkflows.stream()
                .map(itemWorkflowAssembler::dissemble)
                .collect(Collectors.toList());
            
            ItemWorkflowListRepresentation response = new ItemWorkflowListRepresentation(workflowRepresentations);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception exception) {
            return GenericExceptionHandler.handleException(exception, "getNotStartedItemWorkflowsByItemAndTemplate");
        }
    }

    @GetMapping("/item-workflows")
    @ApiOperation(value = "Get all item workflows for a tenant",
                 notes = "Returns paginated or non-paginated list of item workflows ordered by updatedAt DESC")
    public ResponseEntity<?> getAllItemWorkflows(

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
                Long tenantId = TenantContextHolder.getAuthenticatedTenantId();
                ItemWorkflowListRepresentation itemWorkflowListRepresentation = 
                    itemWorkflowService.getAllItemWorkflowsForTenantWithoutPaginationAsRepresentation(tenantId);
                return ResponseEntity.ok(itemWorkflowListRepresentation); // Returning list instead of paged response
            }
            Long tenantId = TenantContextHolder.getAuthenticatedTenantId();
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
            
        } catch (Exception exception) {
            return GenericExceptionHandler.handleException(exception, "getAllItemWorkflows");
        }
    }

    @GetMapping("/item-workflows/search")
    @ApiOperation(value = "Search item workflows", 
                 notes = "Search item workflows by item name or workflow identifier")
    public ResponseEntity<?> searchItemWorkflows(

            @ApiParam(value = "Search type: 'ITEM_NAME' or 'WORKFLOW_IDENTIFIER'", required = true) 
            @RequestParam String searchType,
            @ApiParam(value = "Search term", required = true) @RequestParam String searchTerm,
            @ApiParam(value = "Page number (0-based)", defaultValue = "0") @RequestParam(defaultValue = "0") int page,
            @ApiParam(value = "Page size", defaultValue = "10") @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<ItemWorkflow> itemWorkflowPage;
            Long tenantId = TenantContextHolder.getAuthenticatedTenantId();
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
            
        } catch (Exception exception) {
            return GenericExceptionHandler.handleException(exception, "searchItemWorkflows");
        }
    }

    @PostMapping("/workflow/item-workflows/{workflowId}/complete")
    @ApiOperation(value = "Complete an individual item workflow step",
                 notes = "Marks a specific workflow step as completed. Parent step must be completed first.")
    public ResponseEntity<?> completeItemWorkflowStep(
            @ApiParam(value = "Tenant ID", required = true) @PathVariable Long tenantId,
            @ApiParam(value = "Workflow Step ID", required = true) @PathVariable Long stepId,
            @ApiParam(value = "Completion time", required = true) @RequestParam String completedAt) {
        try {
            // Validate request
            if (completedAt == null || completedAt.isEmpty()) {
                log.warn("Completion time is required for completing workflow step {}", stepId);
                throw new IllegalArgumentException("Step completion time is required");
            }
            
            // Convert String to LocalDateTime using ConvertorUtils
            LocalDateTime completedAtDateTime = ConvertorUtils.convertStringToLocalDateTime(completedAt);
            
            // Complete the step
            ItemWorkflowStep completedStep = itemWorkflowService.completeItemWorkflowStep(
                stepId, tenantId, completedAtDateTime);
            
            // Convert to representation
            ItemWorkflowStepRepresentation stepRepresentation = itemWorkflowStepAssembler.dissemble(completedStep);
            
            return ResponseEntity.ok(stepRepresentation);
            
        } catch (Exception exception) {
            return GenericExceptionHandler.handleException(exception, "completeItemWorkflowStep");
        }
    }

    @PostMapping("/workflow/item-workflows/{workflowId}/complete")
    @ApiOperation(value = "Complete an item workflow",
                 notes = "Marks the workflow and all its steps as completed with individual step completion times")
    public ResponseEntity<?> completeItemWorkflow(
            @ApiParam(value = "Workflow ID", required = true) @PathVariable Long workflowId,
            @RequestBody CompleteWorkflowRequestRepresentation request) {
        try {
            // Validate request
            if (request.getCompletedAt() == null || request.getCompletedAt().isEmpty()) {
                log.warn("Completion time is required for completing workflow {}", workflowId);
                throw new IllegalArgumentException("Workflow completion time is required");
            }
            
            if (request.getStepCompletionTimes() == null || request.getStepCompletionTimes().isEmpty()) {
                log.warn("Step completion times are required for completing workflow {}", workflowId);
                throw new IllegalArgumentException("Step completion times are required for all workflow steps");
            }
            
            // Convert String to LocalDateTime using ConvertorUtils
            LocalDateTime completedAtDateTime = ConvertorUtils.convertStringToLocalDateTime(request.getCompletedAt());
            Long tenantId = TenantContextHolder.getAuthenticatedTenantId();

            // Convert step completion times from String to LocalDateTime
            Map<Long, LocalDateTime> stepCompletionTimes = new HashMap<>();
            for (Map.Entry<Long, String> entry : request.getStepCompletionTimes().entrySet()) {
                try {
                    LocalDateTime stepCompletionTime = ConvertorUtils.convertStringToLocalDateTime(entry.getValue());
                    stepCompletionTimes.put(entry.getKey(), stepCompletionTime);
                } catch (Exception e) {
                    log.error("Invalid completion time format for step {}: {}", entry.getKey(), entry.getValue());
                    throw new IllegalArgumentException(
                        String.format("Invalid completion time format for step %d: %s", entry.getKey(), entry.getValue()));
                }
            }
            
            // Get the workflow and validate it belongs to the tenant
            ItemWorkflow itemWorkflow = itemWorkflowService.getItemWorkflowById(workflowId);
            if (itemWorkflow.getItem().getTenant().getId() != tenantId) {
                log.error("Workflow {} does not belong to tenant {}", workflowId, tenantId);
                return ResponseEntity.notFound().build();
            }
            
            // Validate that all batches in the workflow are completed
            String validationError = validateAllBatchesCompleted(itemWorkflow, tenantId);
            if (validationError != null) {
                log.error("Workflow completion validation failed for workflow {}: {}", workflowId, validationError);
                throw new IllegalArgumentException(validationError);
            }
            
            // Complete the workflow with individual step completion times
            ItemWorkflow completedWorkflow = itemWorkflowService.completeItemWorkflow(
                workflowId, tenantId, completedAtDateTime, stepCompletionTimes);
            
            // Convert to representation
            ItemWorkflowRepresentation workflowRepresentation = itemWorkflowAssembler.dissemble(completedWorkflow);
            
            return ResponseEntity.ok(workflowRepresentation);
            
        } catch (Exception exception) {
            return GenericExceptionHandler.handleException(exception, "completeItemWorkflow");
        }
    }

    @GetMapping("/workflow-tracking")
    @ApiOperation(value = "Get comprehensive tracking information for an item workflow", 
                 notes = "Returns detailed tracking information including all related batches by workflow identifier. " +
                        "Optional type and batchNumber parameters can be used to filter specific batch information.")
    public ResponseEntity<?> getItemWorkflowTracking(

            @ApiParam(value = "Workflow identifier", required = true) @RequestParam String workflowIdentifier,
            @ApiParam(value = "Batch type to filter (FORGE, HEAT_TREATMENT, MACHINING, INSPECTION, VENDOR_DISPATCH, DISPATCH)", required = false) @RequestParam(required = false) String type,
            @ApiParam(value = "Batch number/identifier for the specified type", required = false) @RequestParam(required = false) String batchNumber) {
        try {
            if (workflowIdentifier == null || workflowIdentifier.trim().isEmpty()) {
                log.error("Invalid workflowIdentifier input!");
                throw new IllegalArgumentException("Workflow identifier is required");
            }
            
            ItemWorkflowTrackingResultDTO trackingResult;
            Long tenantId = TenantContextHolder.getAuthenticatedTenantId();
            // If type and batchNumber are provided, use the filtered method
            if (type != null && !type.trim().isEmpty() && batchNumber != null && !batchNumber.trim().isEmpty()) {
                trackingResult = itemWorkflowService.getItemWorkflowTrackingByWorkflowIdentifierAndBatch(
                    tenantId, workflowIdentifier.trim(), type.trim(), batchNumber.trim());
            } else {
                // Use the original comprehensive method
                trackingResult = itemWorkflowService.getItemWorkflowTrackingByWorkflowIdentifier(
                    tenantId, workflowIdentifier.trim());
            }
            
            return ResponseEntity.ok(trackingResult);
            
        } catch (Exception exception) {
            return GenericExceptionHandler.handleException(exception, "getItemWorkflowTracking");
        }
    }

    @GetMapping("/items-with-in-progress-workflows")
    @ApiOperation(value = "Get items with IN_PROGRESS workflows", 
                 notes = "Returns paginated list of items that have ItemWorkflows with IN_PROGRESS status, ordered by most recently updated workflow")
    public ResponseEntity<?> getItemsWithInProgressWorkflows(

            @ApiParam(value = "Page number (0-based)", defaultValue = "0") @RequestParam(defaultValue = "0") int page,
            @ApiParam(value = "Page size", defaultValue = "10") @RequestParam(defaultValue = "10") int size) {
        try {
            Long tenantId = TenantContextHolder.getAuthenticatedTenantId();
            Page<ItemRepresentation> itemsPage = itemService.getItemsWithInProgressWorkflows(tenantId, page, size);
            
            ItemPageResponseRepresentation response = new ItemPageResponseRepresentation(
                itemsPage.getContent(),
                itemsPage.getTotalPages(),
                itemsPage.getTotalElements(),
                itemsPage.getNumber(),
                itemsPage.getSize()
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception exception) {
            return GenericExceptionHandler.handleException(exception, "getItemsWithInProgressWorkflows");
        }
    }

    @PostMapping("/item/{itemId}/workflow")
    @ApiOperation(value = "Create item workflow", 
                 notes = "Creates a new workflow for the specified item using the provided workflow template")
    public ResponseEntity<?> createItemWorkflow(
            @ApiParam(value = "Item ID", required = true) @PathVariable Long itemId,
            @ApiParam(value = "Workflow Template ID", required = true) @RequestParam Long workflowTemplateId,
            @ApiParam(value = "Item Workflow Name (used as workflow identifier)", required = true) @RequestParam String itemWorkflowName) {
        try {
            // Validate input parameters
            if (workflowTemplateId == null) {
                log.error("Invalid input parameters: itemId={}, workflowTemplateId={}",
                         itemId, workflowTemplateId);
                throw new IllegalArgumentException("All parameters (tenantId, itemId, workflowTemplateId) are required");
            }

            // Validate itemWorkflowName
            if (itemWorkflowName == null || itemWorkflowName.trim().isEmpty()) {
                log.error("Item workflow name is required for creating workflow");
                throw new IllegalArgumentException("Item workflow name is required");
            }

            // Check if workflow identifier is already in use globally across the tenant
            String trimmedWorkflowName = itemWorkflowName.trim();
            Long tenantId = TenantContextHolder.getAuthenticatedTenantId();
            boolean workflowIdentifierExists = itemWorkflowService.isWorkflowIdentifierExistsForTenant(tenantId, trimmedWorkflowName);
            if (workflowIdentifierExists) {
                log.error("Workflow identifier '{}' is already in use for tenant {}", trimmedWorkflowName, tenantId);
                throw new IllegalStateException("Workflow identifier '" + trimmedWorkflowName + "' is already in use. Please choose a different name.");
            }

            // Get the item and validate it belongs to the tenant
            Item item;
            try {
                item =  itemService.getItemByIdAndTenantId(itemId, tenantId);
            } catch (Exception e) {
                log.error("Item {} not found for tenant {}: {}", itemId, tenantId, e.getMessage());
                throw new RuntimeException("Item not found or does not belong to the specified tenant");
            }

            // Validate that the item belongs to the tenant
            if (item.getTenant().getId() != tenantId) {
                log.error("Item {} does not belong to tenant {}", itemId, tenantId);
                throw new RuntimeException("Item does not belong to the specified tenant");
            }

            // Validate workflow template compatibility with the item
            String workflowValidationError = validateWorkflowTemplateCompatibility(item, workflowTemplateId, tenantId);
            if (workflowValidationError != null) {
                log.error("Workflow template validation failed: {}", workflowValidationError);
                throw new IllegalArgumentException(workflowValidationError);
            }

            // Create the workflow with the provided itemWorkflowName as workflowIdentifier
            ItemWorkflow workflow = itemWorkflowService.createItemWorkflow(item, workflowTemplateId, trimmedWorkflowName);
            
            // Convert to representation
            ItemWorkflowRepresentation workflowRepresentation = itemWorkflowAssembler.dissemble(workflow);
            
            log.info("Successfully created workflow {} with identifier '{}' for item {} in tenant {}", 
                     workflow.getId(), trimmedWorkflowName, itemId, tenantId);
            
            return new ResponseEntity<>(workflowRepresentation, HttpStatus.CREATED);
            
        } catch (Exception exception) {
            return GenericExceptionHandler.handleException(exception, "createItemWorkflow");
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
            if (workflowTemplate.getTenant().getId() != tenantId) {
                return "Workflow template does not belong to the specified tenant";
            }
            
            // Get the root steps of the workflow
            List<WorkflowStep> rootSteps = workflowTemplate.getRootSteps();
            if (rootSteps.isEmpty()) {
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
                // For KGS products, workflow should have FORGING or VENDOR as root steps
                boolean hasValidRootStep = workflowTemplate.isFirstOperationType(WorkflowStep.OperationType.FORGING) || 
                                         workflowTemplate.isFirstOperationType(WorkflowStep.OperationType.VENDOR);
                if (!hasValidRootStep) {
                    return "KGS items must start with either FORGING or VENDOR workflow. Please select a workflow that has FORGING or VENDOR as root operations.";
                }
            } else if (hasPiecesProduct && !hasKgsProduct) {
                // For PIECES products, workflow should NOT have FORGING as root step
                if (workflowTemplate.isFirstOperationType(WorkflowStep.OperationType.FORGING)) {
                    return "PIECES items cannot start with FORGING workflow. Please select a workflow that does not have FORGING as a root operation.";
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
     * @param operationEntityId The operation entity ID (meaning varies by operation type)
     * @param operationType The operation type (FORGING, HEAT_TREATMENT, MACHINING, QUALITY, VENDOR)
     * @return OperationEndTimeDTO containing the end time and source information
     */
    @GetMapping("/workflow/operation-end-time")
    @ApiOperation(value = "Get operation end time for workflow validation", 
                 notes = "Returns the end time of the specified operation for workflow step creation validation")
    public ResponseEntity<?> getOperationEndTimeForValidation(
        
        @ApiParam(value = "Operation entity ID (processedItemId for FORGING, processedItemHeatTreatmentBatchId for HEAT_TREATMENT, etc.)", required = true) @RequestParam("operationEntityId") String operationEntityId,
        @ApiParam(value = "Operation type", required = true, allowableValues = "FORGING,HEAT_TREATMENT,MACHINING,QUALITY,VENDOR") @RequestParam("operationType") String operationType) {

        try {
            // Validate input parameters
            if (operationEntityId == null || operationEntityId.isEmpty() ||
                operationType == null || operationType.trim().isEmpty()) {
                log.error("Invalid input for getOperationEndTimeForValidation - missing required parameters");
                throw new IllegalArgumentException("Tenant ID, operation entity ID, and operation type are required");
            }

            Long tenantIdLongValue = TenantContextHolder.getAuthenticatedTenantId();
            Long operationEntityIdLongValue = GenericResourceUtils.convertResourceIdToLong(operationEntityId)
                .orElseThrow(() -> new RuntimeException("Not valid operationEntityId!"));

            // Normalize operation type
            String normalizedOperationType = operationType.trim().toUpperCase();
            
            // Validate operation type
            if (!isValidOperationType(normalizedOperationType)) {
                log.error("Invalid operation type: {}", normalizedOperationType);
                throw new IllegalArgumentException("Invalid operation type. Allowed values: FORGING, HEAT_TREATMENT, MACHINING, QUALITY, VENDOR");
            }

            // Get end time based on operation type
            OperationEndTimeDTO result = getEndTimeForOperationType(normalizedOperationType, operationEntityIdLongValue, tenantIdLongValue);
            return ResponseEntity.ok(result);

        } catch (Exception exception) {
            return GenericExceptionHandler.handleException(exception, "getOperationEndTimeForValidation");
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
            String stepValidationError = validateItemWorkflowStepCompletion(step, tenantId);
            if (stepValidationError != null) {
                return String.format("Step '%s' (%s): %s", 
                                   step.getWorkflowStep().getStepName(), step.getOperationType(), stepValidationError);
            }
        }
        
        log.info("All batches in workflow {} are completed", itemWorkflow.getId());
        return null; // All batches are completed
    }

    /**
     * Validates completion status of a single ItemWorkflowStep using operationOutcomeData
     * @param step The ItemWorkflowStep to validate
     * @param tenantId The tenant ID for validation
     * @return null if step is completed, error message if not completed
     */
    private String validateItemWorkflowStepCompletion(ItemWorkflowStep step, Long tenantId) {
        try {
            // Check if step has operationOutcomeData
            if (step.getOperationOutcomeData() == null || step.getOperationOutcomeData().trim().isEmpty()) {
                // No outcome data means step never started
                return null;
            }

            if (step.getCompletedAt() != null && ItemWorkflowStep.StepStatus.COMPLETED==step.getStepStatus()) {
                return null;
            }

            return String.format("Step=%s of the ItemWorkflow=%s is not completed", step.getOperationType(), step.getItemWorkflow().getWorkflowIdentifier());
            
        } catch (Exception e) {
            log.error("Error validating step completion for step {} of type {}: {}", 
                     step.getId(), step.getOperationType(), e.getMessage());
            return String.format("Error validating step completion: %s", e.getMessage());
        }
    }

}