package com.jangid.forging_process_management_service.resource.vendor;


import com.jangid.forging_process_management_service.entities.vendor.VendorProcessType;
import com.jangid.forging_process_management_service.entities.workflow.ItemWorkflow;
import com.jangid.forging_process_management_service.entities.workflow.WorkflowStep;
import com.jangid.forging_process_management_service.entities.workflow.WorkflowTemplate;
import com.jangid.forging_process_management_service.entitiesRepresentation.vendor.VendorDispatchBatchRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.vendor.VendorDispatchBatchListRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.error.ErrorResponse;
import com.jangid.forging_process_management_service.exception.TenantNotFoundException;
import com.jangid.forging_process_management_service.service.vendor.VendorDispatchService;
import com.jangid.forging_process_management_service.service.workflow.ItemWorkflowService;
import com.jangid.forging_process_management_service.utils.GenericResourceUtils;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;

import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;


import com.jangid.forging_process_management_service.entitiesRepresentation.vendor.ProcessedItemVendorDispatchBatchRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.vendor.VendorDispatchHeatRepresentation;

@Api(value = "Vendor Dispatch Management")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Component
@Slf4j
public class VendorDispatchResource {

    @Autowired
    private final VendorDispatchService vendorDispatchService;
  @Autowired
  private ItemWorkflowService itemWorkflowService;

    @PostMapping("tenant/{tenantId}/vendor-dispatch-batch")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Create a new vendor dispatch batch")
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Vendor dispatch batch created successfully"),
            @ApiResponse(code = 400, message = "Bad request - validation failed"),
            @ApiResponse(code = 404, message = "Vendor, heat, or entity not found"),
            @ApiResponse(code = 409, message = "Batch number already exists")
    })
    public ResponseEntity<?> createVendorDispatchBatch(
            @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable String tenantId,
            @ApiParam(value = "Vendor dispatch batch details", required = true) @Valid @RequestBody VendorDispatchBatchRepresentation representation) {

        try {
            if (tenantId == null || tenantId.isEmpty() || representation == null) {
                log.error("Invalid vendor dispatch batch input!");
                return new ResponseEntity<>(new ErrorResponse("Invalid vendor dispatch batch input!"), HttpStatus.BAD_REQUEST);
            }

            // Validate vendor dispatch batch details
            String validationError = validateVendorDispatchBatchForCreation(representation);
            if (validationError != null) {
                log.error("Validation failed for vendor dispatch batch: {}", validationError);
                return new ResponseEntity<>(new ErrorResponse(validationError), HttpStatus.BAD_REQUEST);
            }

            Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
                    .orElseThrow(() -> new RuntimeException("Not valid tenantId!"));

            log.info("Creating vendor dispatch batch for tenant: {}", tenantIdLongValue);
            VendorDispatchBatchRepresentation created = vendorDispatchService.createVendorDispatchBatch(representation, tenantIdLongValue);
            return new ResponseEntity<>(created, HttpStatus.CREATED);
        } catch (Exception exception) {
            if (exception instanceof IllegalStateException) {
                log.error("Vendor dispatch batch exists with the given batch number: {}", representation.getVendorDispatchBatchNumber());
                return new ResponseEntity<>(new ErrorResponse("Vendor dispatch batch exists with the given batch number"), HttpStatus.CONFLICT);
            }
            log.error("Error creating vendor dispatch batch: {}", exception.getMessage());
            return new ResponseEntity<>(new ErrorResponse("Error creating vendor dispatch batch"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Validates vendor dispatch batch representation for creation with workflow integration
     */
    private String validateVendorDispatchBatchForCreation(VendorDispatchBatchRepresentation representation) {
        // Basic validation
        if (representation.getVendorDispatchBatchNumber() == null || representation.getVendorDispatchBatchNumber().trim().isEmpty()) {
            return "Vendor dispatch batch number is required";
        }

        if (representation.getVendor() == null) {
            return "Vendor is required";
        }

        if (representation.getBillingEntityId() == null) {
            return "Billing entity ID is required";
        }

        if (representation.getShippingEntityId() == null) {
            return "Shipping entity ID is required";
        }

        if (representation.getProcesses() == null || representation.getProcesses().isEmpty()) {
            return "At least one process type must be specified";
        }

        // Validate processed item
        if (representation.getProcessedItem() == null) {
            return "Processed item vendor dispatch batch is required";
        }

        return validateProcessedItemVendorDispatchBatch(representation.getProcessedItem(), representation.getProcesses());
    }

    /**
     * Validates processed item vendor dispatch batch with workflow integration
     */
    private String validateProcessedItemVendorDispatchBatch(ProcessedItemVendorDispatchBatchRepresentation processedItem, 
                                                           List<com.jangid.forging_process_management_service.entities.vendor.VendorProcessType> processes) {
        // Basic validation
        if (processedItem.getItem() == null || processedItem.getItem().getId() == null) {
            return "Item is required for processed item vendor dispatch batch";
        }

        if (processedItem.getIsInPieces() && (processedItem.getDispatchedPiecesCount() == null || processedItem.getDispatchedPiecesCount() <= 0)) {
            return "Dispatched pieces count must be greater than 0";
        }

        if (!processedItem.getIsInPieces() && (processedItem.getDispatchedQuantity() == null || processedItem.getDispatchedQuantity() <= 0)) {
            return "Dispatched quantity must be greater than 0";
        }

        // Workflow Integration Validation
        if (processedItem.getWorkflowIdentifier() == null || processedItem.getWorkflowIdentifier().trim().isEmpty()) {
            return "Workflow identifier is required for vendor dispatch batch";
        }

        // Check if this is a first operation or non-first operation based on itemWorkflowId
        ItemWorkflow selectedItemWorkflow = itemWorkflowService.getItemWorkflowById(processedItem.getItemWorkflowId());
        WorkflowTemplate workflowTemplate =  selectedItemWorkflow.getWorkflowTemplate();
        boolean isFirstOperation = itemWorkflowService.isFirstOperationInWorkflow(workflowTemplate.getId(), WorkflowStep.OperationType.VENDOR);

        if (isFirstOperation) {
            // First operation case: should have vendorDispatchHeats for inventory consumption
            if (processedItem.getVendorDispatchHeats() == null || processedItem.getVendorDispatchHeats().isEmpty()) {
                return "Vendor dispatch heats are required for first operation vendor dispatch batch";
            }

            // Validate heat consumption based on operation type and process type
            String heatValidationError = validateHeatConsumption(processedItem.getVendorDispatchHeats(), processes, true);
            if (heatValidationError != null) {
                return heatValidationError;
            }

            // For first operation, previousOperationProcessedItemId should not be present
            if (processedItem.getPreviousOperationProcessedItemId() != null) {
                return "Previous operation processed item ID should not be present for first operation";
            }
        } else {
            // Non-first operation case: should have itemWorkflowId and no vendorDispatchHeats
            if (processedItem.getItemWorkflowId() == null) {
                return "Item workflow ID is required for non-first operation vendor dispatch batch";
            }

            // For non-first operation, vendorDispatchHeats should not be present
            if (processedItem.getVendorDispatchHeats() != null && !processedItem.getVendorDispatchHeats().isEmpty()) {
                return "Vendor dispatch heats should not be present for non-first operation";
            }

            // The pieces will come from the previous operation in the workflow
        }

        return null;
    }

    /**
     * Validates heat consumption based on operation type and process type
     */
    private String validateHeatConsumption(List<VendorDispatchHeatRepresentation> vendorDispatchHeats, 
                                         List<com.jangid.forging_process_management_service.entities.vendor.VendorProcessType> processes,
                                         boolean isFirstOperation) {
        if (!isFirstOperation) {
            return null; // No heat consumption validation for non-first operations
        }

        // Determine if FORGING is the first process
        boolean isFirstProcessForging = !processes.isEmpty() &&
                                        processes.get(0) == VendorProcessType.FORGING;

        for (VendorDispatchHeatRepresentation heatRep : vendorDispatchHeats) {
            if (heatRep.getHeat() == null || heatRep.getHeat().getId() == null) {
                return "Heat is required for vendor dispatch heat consumption";
            }

            if (heatRep.getConsumptionType() == null || heatRep.getConsumptionType().trim().isEmpty()) {
                return "Consumption type is required for vendor dispatch heat";
            }

            // Validate consumption type and values
            if ("QUANTITY".equals(heatRep.getConsumptionType())) {
                if (heatRep.getQuantityUsed() == null || heatRep.getQuantityUsed() <= 0) {
                    return "Quantity used must be greater than 0 for QUANTITY consumption type";
                }
                if (heatRep.getPiecesUsed() != null) {
                    return "Pieces used should not be set for QUANTITY consumption type";
                }
            } else if ("PIECES".equals(heatRep.getConsumptionType())) {
                if (heatRep.getPiecesUsed() == null || heatRep.getPiecesUsed() <= 0) {
                    return "Pieces used must be greater than 0 for PIECES consumption type";
                }
                if (heatRep.getQuantityUsed() != null) {
                    return "Quantity used should not be set for PIECES consumption type";
                }
            } else {
                return "Consumption type must be either QUANTITY or PIECES";
            }

            // Business rule: If Vendor is the first operation and Forging is the first process, use QUANTITY consumption
            // For all other cases, use PIECES consumption
            if (isFirstProcessForging) {
                if (!"QUANTITY".equals(heatRep.getConsumptionType())) {
                    return "When Vendor is the first operation and Forging is the first process, heat consumption must be QUANTITY-based";
                }
            } else {
                if (!"PIECES".equals(heatRep.getConsumptionType())) {
                    return "When Vendor is the first operation and Forging is not the first process, heat consumption must be PIECES-based";
                }
            }
        }

        return null;
    }


    @GetMapping("tenant/{tenantId}/vendor-dispatch-batch/{batchId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get vendor dispatch batch by ID")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Vendor dispatch batch retrieved successfully"),
            @ApiResponse(code = 404, message = "Vendor dispatch batch not found")
    })
    public ResponseEntity<?> getVendorDispatchBatch(
            @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable String tenantId,
            @ApiParam(value = "Batch ID", required = true) @PathVariable String batchId) {

        try {
            if (tenantId == null || tenantId.isEmpty() || batchId == null || batchId.isEmpty()) {
                log.error("Invalid input for getting vendor dispatch batch!");
                throw new RuntimeException("Invalid input for getting vendor dispatch batch!");
            }

            Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
                    .orElseThrow(() -> new RuntimeException("Not valid tenantId!"));
            Long batchIdLongValue = GenericResourceUtils.convertResourceIdToLong(batchId)
                    .orElseThrow(() -> new RuntimeException("Not valid batchId!"));

            VendorDispatchBatchRepresentation batch = vendorDispatchService.getVendorDispatchBatch(batchIdLongValue, tenantIdLongValue);
            return ResponseEntity.ok(batch);
        } catch (Exception exception) {
            if (exception instanceof RuntimeException && exception.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            log.error("Error getting vendor dispatch batch: {}", exception.getMessage());
            return new ResponseEntity<>(new ErrorResponse("Error getting vendor dispatch batch"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("tenant/{tenantId}/vendor-dispatch-batches")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get all vendor dispatch batches")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Vendor dispatch batches retrieved successfully")
    })
    public ResponseEntity<?> getAllVendorDispatchBatches(
            @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable String tenantId,
            @ApiParam(value = "Filter by vendor ID") @RequestParam(required = false) String vendorId,
            @ApiParam(value = "Page number") @RequestParam(required = false) String page,
            @ApiParam(value = "Page size") @RequestParam(required = false) String size) {

        try {
            Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
                    .orElseThrow(() -> new TenantNotFoundException(tenantId));

            Integer pageNumber = (page == null || page.isBlank()) ? -1
                    : GenericResourceUtils.convertResourceIdToInt(page)
                            .orElseThrow(() -> new RuntimeException("Invalid page=" + page));

            Integer sizeNumber = (size == null || size.isBlank()) ? -1
                    : GenericResourceUtils.convertResourceIdToInt(size)
                            .orElseThrow(() -> new RuntimeException("Invalid size=" + size));

            // Handle vendor filtering with optional Long conversion
            Long vendorIdLongValue = null;
            if (vendorId != null && !vendorId.isEmpty()) {
                vendorIdLongValue = GenericResourceUtils.convertResourceIdToLong(vendorId)
                        .orElseThrow(() -> new RuntimeException("Not valid vendorId!"));
            }

            // If no pagination parameters, return all batches
            if (pageNumber == -1 || sizeNumber == -1) {
                if (vendorIdLongValue != null) {
                    return ResponseEntity.ok(vendorDispatchService.getVendorDispatchBatchesByVendorWithoutPagination(vendorIdLongValue, tenantIdLongValue));
                } else {
                    return ResponseEntity.ok(vendorDispatchService.getAllVendorDispatchBatchesWithoutPagination(tenantIdLongValue));
                }
            }

            // Return paginated results
            if (vendorIdLongValue != null) {
                return ResponseEntity.ok(vendorDispatchService.getVendorDispatchBatchesByVendor(vendorIdLongValue, tenantIdLongValue, pageNumber, sizeNumber));
            } else {
                return ResponseEntity.ok(vendorDispatchService.getAllVendorDispatchBatches(tenantIdLongValue, pageNumber, sizeNumber));
            }
        } catch (Exception exception) {
            if (exception instanceof TenantNotFoundException) {
                return ResponseEntity.notFound().build();
            }
            log.error("Error getting vendor dispatch batches: {}", exception.getMessage());
            return new ResponseEntity<>(new ErrorResponse("Error getting vendor dispatch batches"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping(value = "tenant/{tenantId}/processedItemVendorDispatchBatches/vendorDispatchBatches", produces = MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get vendor dispatch batches by processed item vendor dispatch batch IDs")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Vendor dispatch batches retrieved successfully"),
            @ApiResponse(code = 400, message = "Invalid input parameters"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    public ResponseEntity<?> getVendorDispatchBatchesByProcessedItemVendorDispatchBatchIds(
            @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable("tenantId") String tenantId,
            @ApiParam(value = "Comma-separated list of processed item vendor dispatch batch IDs", required = true) 
            @RequestParam("processedItemVendorDispatchBatchIds") String processedItemVendorDispatchBatchIds) {

        try {
            if (tenantId == null || tenantId.isEmpty() || processedItemVendorDispatchBatchIds == null || processedItemVendorDispatchBatchIds.isEmpty()) {
                log.error("Invalid input for getVendorDispatchBatchesByProcessedItemVendorDispatchBatchIds - tenantId or processedItemVendorDispatchBatchIds is null/empty");
                throw new IllegalArgumentException("Tenant ID and Processed Item Vendor Dispatch Batch IDs are required and cannot be empty");
            }

            Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
                    .orElseThrow(() -> new RuntimeException("Not valid tenantId!"));

            // Parse comma-separated processed item vendor dispatch batch IDs
            List<Long> processedItemVendorDispatchBatchIdList = Arrays.stream(processedItemVendorDispatchBatchIds.split(","))
                    .map(String::trim)
                    .filter(id -> !id.isEmpty())
                    .map(id -> GenericResourceUtils.convertResourceIdToLong(id)
                            .orElseThrow(() -> new RuntimeException("Not valid processedItemVendorDispatchBatchId: " + id)))
                    .collect(Collectors.toList());

            if (processedItemVendorDispatchBatchIdList.isEmpty()) {
                log.error("No valid processed item vendor dispatch batch IDs provided");
                throw new IllegalArgumentException("At least one valid processed item vendor dispatch batch ID is required");
            }

            List<VendorDispatchBatchRepresentation> vendorDispatchBatchRepresentations = 
                vendorDispatchService.getVendorDispatchBatchesByProcessedItemVendorDispatchBatchIds(processedItemVendorDispatchBatchIdList, tenantIdLongValue);

            VendorDispatchBatchListRepresentation vendorDispatchBatchListRepresentation = VendorDispatchBatchListRepresentation.builder()
                    .vendorDispatchBatches(vendorDispatchBatchRepresentations)
                    .build();

            return ResponseEntity.ok(vendorDispatchBatchListRepresentation);

        } catch (Exception exception) {
            if (exception instanceof IllegalArgumentException) {
                log.error("Invalid data for getVendorDispatchBatchesByProcessedItemVendorDispatchBatchIds: {}", exception.getMessage());
                return new ResponseEntity<>(new ErrorResponse(exception.getMessage()), HttpStatus.BAD_REQUEST);
            }
            log.error("Error processing getVendorDispatchBatchesByProcessedItemVendorDispatchBatchIds: {}", exception.getMessage());
            return new ResponseEntity<>(new ErrorResponse("Error retrieving vendor dispatch batches by processed item vendor dispatch batch IDs"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("tenant/{tenantId}/vendor-dispatch-batches/search")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Search vendor dispatch batches by various criteria")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Vendor dispatch batches search completed successfully"),
            @ApiResponse(code = 400, message = "Invalid search parameters"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    public ResponseEntity<?> searchVendorDispatchBatches(
            @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable String tenantId,
            @ApiParam(value = "Search type", required = true, allowableValues = "VENDOR_DISPATCH_BATCH_NUMBER,ITEM_NAME,ITEM_WORKFLOW_NAME,VENDOR_RECEIVE_BATCH_NUMBER") @RequestParam String searchType,
            @ApiParam(value = "Search term", required = true) @RequestParam String searchTerm,
            @ApiParam(value = "Page number (0-based)", required = false) @RequestParam(value = "page", defaultValue = "0") String pageParam,
            @ApiParam(value = "Page size", required = false) @RequestParam(value = "size", defaultValue = "10") String sizeParam) {

        try {
            if (tenantId == null || tenantId.isEmpty() || searchType == null || searchType.trim().isEmpty() || 
                searchTerm == null || searchTerm.trim().isEmpty()) {
                log.error("Invalid search parameters for vendor dispatch batches");
                return new ResponseEntity<>(new ErrorResponse("Tenant ID, search type, and search term are required"), HttpStatus.BAD_REQUEST);
            }

            Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
                    .orElseThrow(() -> new RuntimeException("Not valid tenantId!"));

            int pageNumber = GenericResourceUtils.convertResourceIdToInt(pageParam)
                    .orElseThrow(() -> new RuntimeException("Invalid page=" + pageParam));

            int pageSize = GenericResourceUtils.convertResourceIdToInt(sizeParam)
                    .orElseThrow(() -> new RuntimeException("Invalid size=" + sizeParam));

            if (pageNumber < 0) {
                pageNumber = 0;
            }

            if (pageSize <= 0) {
                pageSize = 10; // Default page size
            }

            // Validate search type
            if (!isValidSearchType(searchType.trim().toUpperCase())) {
                return new ResponseEntity<>(new ErrorResponse("Invalid search type. Allowed values: VENDOR_DISPATCH_BATCH_NUMBER, ITEM_NAME, ITEM_WORKFLOW_NAME, VENDOR_RECEIVE_BATCH_NUMBER"), HttpStatus.BAD_REQUEST);
            }

            // Perform search based on type
            Page<VendorDispatchBatchRepresentation> searchResults = vendorDispatchService.searchVendorDispatchBatches(
                    tenantIdLongValue, searchType.trim().toUpperCase(), searchTerm.trim(), pageNumber, pageSize);
            
            return ResponseEntity.ok(searchResults);

        } catch (IllegalArgumentException e) {
            log.error("Invalid search parameters for vendor dispatch batches: {}", e.getMessage());
            return new ResponseEntity<>(new ErrorResponse("Invalid search parameters: " + e.getMessage()), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("Error during vendor dispatch batch search: {}", e.getMessage());
            return new ResponseEntity<>(new ErrorResponse("Error during search"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Validates the search type parameter
     */
    private boolean isValidSearchType(String searchType) {
        return "VENDOR_DISPATCH_BATCH_NUMBER".equals(searchType) ||
               "ITEM_NAME".equals(searchType) ||
               "ITEM_WORKFLOW_NAME".equals(searchType) ||
               "VENDOR_RECEIVE_BATCH_NUMBER".equals(searchType);
    }

    @DeleteMapping("tenant/{tenantId}/vendor-dispatch-batch/{batchId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Delete a vendor dispatch batch")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Vendor dispatch batch deleted successfully"),
            @ApiResponse(code = 404, message = "Vendor dispatch batch not found"),
            @ApiResponse(code = 409, message = "Vendor dispatch batch cannot be deleted due to business rules")
    })
    public ResponseEntity<?> deleteVendorDispatchBatch(
            @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable String tenantId,
            @ApiParam(value = "Vendor dispatch batch ID", required = true) @PathVariable String batchId) {

        try {
            if (tenantId == null || tenantId.isEmpty() || batchId == null || batchId.isEmpty()) {
                log.error("Invalid input for deleting vendor dispatch batch!");
                return new ResponseEntity<>(new ErrorResponse("Invalid input for deleting vendor dispatch batch!"), HttpStatus.BAD_REQUEST);
            }

            Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
                    .orElseThrow(() -> new RuntimeException("Not valid tenantId!"));
            Long batchIdLongValue = GenericResourceUtils.convertResourceIdToLong(batchId)
                    .orElseThrow(() -> new RuntimeException("Not valid batchId!"));

            vendorDispatchService.deleteVendorDispatchBatch(batchIdLongValue, tenantIdLongValue);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            if (exception instanceof RuntimeException && exception.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            if (exception instanceof IllegalStateException) {
                log.error("Error deleting vendor dispatch batch: {}", exception.getMessage());
                return new ResponseEntity<>(new ErrorResponse(exception.getMessage()), HttpStatus.CONFLICT);
            }
            log.error("Error deleting vendor dispatch batch: {}", exception.getMessage());
            return new ResponseEntity<>(new ErrorResponse("Error deleting vendor dispatch batch"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}