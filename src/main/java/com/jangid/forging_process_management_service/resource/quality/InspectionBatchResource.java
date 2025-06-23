package com.jangid.forging_process_management_service.resource.quality;

import com.jangid.forging_process_management_service.entitiesRepresentation.error.ErrorResponse;
import com.jangid.forging_process_management_service.entitiesRepresentation.quality.InspectionBatchListRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.quality.InspectionBatchRepresentation;
import com.jangid.forging_process_management_service.exception.TenantNotFoundException;
import com.jangid.forging_process_management_service.exception.quality.InspectionBatchNotFoundException;
import com.jangid.forging_process_management_service.service.quality.InspectionBatchService;
import com.jangid.forging_process_management_service.utils.GenericResourceUtils;
import com.jangid.forging_process_management_service.utils.GenericExceptionHandler;

import io.swagger.annotations.ApiParam;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
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

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Component
public class InspectionBatchResource {

  @Autowired
  private final InspectionBatchService inspectionBatchService;

  @PostMapping("tenant/{tenantId}/create-inspection-batch")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<?> createInspectionBatch(
      @PathVariable String tenantId,
      @RequestBody InspectionBatchRepresentation inspectionBatchRepresentation) {
    try {
      if (tenantId == null || tenantId.isEmpty() ||
          isInvalidInspectionBatchDetails(inspectionBatchRepresentation)) {
        log.error("Invalid createInspectionBatch input!");
        throw new RuntimeException("Invalid createInspectionBatch input!");
      }

      Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Not valid tenantId!"));

      // Validate workflow integration fields
      validateWorkflowIntegrationFields(inspectionBatchRepresentation);

      InspectionBatchRepresentation createdInspectionBatch = inspectionBatchService.createInspectionBatch(tenantIdLongValue, inspectionBatchRepresentation);
      return new ResponseEntity<>(createdInspectionBatch, HttpStatus.CREATED);
    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "createInspectionBatch");
    }
  }

  /**
   * Validates workflow integration fields based on the operation position in the workflow
   * Use cases:
   * 1. If selected item is the first operation of the workflow: workflowIdentifier provided, itemWorkflowId is null
   * 2. If selected item is not the first operation: both workflowIdentifier and itemWorkflowId provided
   */
  private void validateWorkflowIntegrationFields(InspectionBatchRepresentation inspectionBatchRepresentation) {
    if (inspectionBatchRepresentation.getProcessedItemInspectionBatch() == null) {
      throw new IllegalArgumentException("ProcessedItemInspectionBatch is required for workflow integration");
    }

    String workflowIdentifier = inspectionBatchRepresentation.getProcessedItemInspectionBatch().getWorkflowIdentifier();
    Long itemWorkflowId = inspectionBatchRepresentation.getProcessedItemInspectionBatch().getItemWorkflowId();

    // Workflow identifier is always required for workflow integration
    if (workflowIdentifier == null || workflowIdentifier.trim().isEmpty()) {
      throw new IllegalArgumentException("workflowIdentifier is required for inspection batch workflow integration");
    }

    // If itemWorkflowId is provided, validate it's a positive number
    if (itemWorkflowId != null && itemWorkflowId <= 0) {
      throw new IllegalArgumentException("itemWorkflowId must be a positive number when provided");
    }

    // Log the workflow integration scenario
    if (itemWorkflowId == null) {
      log.info("Creating inspection batch for first operation in workflow with identifier: {}", workflowIdentifier);
    } else {
      log.info("Creating inspection batch for non-first operation in workflow with identifier: {} and itemWorkflowId: {}", 
               workflowIdentifier, itemWorkflowId);
    }
  }

  @GetMapping("tenant/{tenantId}/inspection-batches")
  public ResponseEntity<?> getAllInspectionBatchesOfTenant(@ApiParam(value = "Identifier of the tenant", required = true) @PathVariable String tenantId,
                                                @RequestParam(value = "page", required = false) String page,
                                                @RequestParam(value = "size", required = false) String size) {
    Long tId = GenericResourceUtils.convertResourceIdToLong(tenantId)
        .orElseThrow(() -> new TenantNotFoundException(tenantId));

    Integer pageNumber = (page == null || page.isBlank()) ? -1
                                                          : GenericResourceUtils.convertResourceIdToInt(page)
                             .orElseThrow(() -> new RuntimeException("Invalid page=" + page));

    Integer sizeNumber = (size == null || size.isBlank()) ? -1
                                                          : GenericResourceUtils.convertResourceIdToInt(size)
                             .orElseThrow(() -> new RuntimeException("Invalid size=" + size));

    if (pageNumber == -1 || sizeNumber == -1) {
      InspectionBatchListRepresentation inspectionBatchListRepresentation = inspectionBatchService.getAllInspectionBatchesOfTenantWithoutPagination(tId);
      return ResponseEntity.ok(inspectionBatchListRepresentation);
    }
    Page<InspectionBatchRepresentation> inspectionBatchRepresentations = inspectionBatchService.getAllInspectionBatchesOfTenant(tId, pageNumber, sizeNumber);
    return ResponseEntity.ok(inspectionBatchRepresentations);
  }

  @DeleteMapping("tenant/{tenantId}/inspection-batch/{inspectionBatchId}")
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<?> deleteInspectionBatch(
      @PathVariable String tenantId,
      @PathVariable String inspectionBatchId) {
    try {
      if (tenantId == null || tenantId.isEmpty() || inspectionBatchId == null || inspectionBatchId.isEmpty()) {
        log.error("Invalid deleteInspectionBatch input!");
        throw new RuntimeException("Invalid deleteInspectionBatch input!");
      }

      Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Not valid tenantId!"));
      Long inspectionBatchIdLongValue = GenericResourceUtils.convertResourceIdToLong(inspectionBatchId)
          .orElseThrow(() -> new RuntimeException("Not valid inspectionBatchId!"));

      inspectionBatchService.deleteInspectionBatch(tenantIdLongValue, inspectionBatchIdLongValue);
      return ResponseEntity.ok().build();

    } catch (Exception exception) {
      if (exception instanceof InspectionBatchNotFoundException) {
        return ResponseEntity.notFound().build();
      }
      if (exception instanceof IllegalStateException) {
        log.error("Error while deleting inspection batch: {}", exception.getMessage());
        return new ResponseEntity<>(new ErrorResponse(exception.getMessage()), HttpStatus.CONFLICT);
      }
      log.error("Error while deleting inspection batch: {}", exception.getMessage());
      return new ResponseEntity<>(new ErrorResponse("Error while deleting inspection batch"),
          HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @GetMapping("tenant/{tenantId}/machining-batch/{machiningBatchId}/inspection-batches")
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<?> getInspectionBatchesByMachiningBatch(
      @PathVariable String tenantId,
      @PathVariable String machiningBatchId) {
    try {
      if (tenantId == null || tenantId.isEmpty() || machiningBatchId == null || machiningBatchId.isEmpty()) {
        log.error("Invalid getInspectionBatchByMachiningBatch input!");
        throw new RuntimeException("Invalid getInspectionBatchByMachiningBatch input!");
      }

      Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Not valid tenantId!"));
      Long machiningBatchIdLongValue = GenericResourceUtils.convertResourceIdToLong(machiningBatchId)
          .orElseThrow(() -> new RuntimeException("Not valid machiningBatchId!"));

      InspectionBatchListRepresentation inspectionBatches = 
          inspectionBatchService.getInspectionBatchesByMachiningBatch(tenantIdLongValue, machiningBatchIdLongValue);
      
      return ResponseEntity.ok(inspectionBatches);
    } catch (Exception exception) {
      if (exception instanceof InspectionBatchNotFoundException) {
        return ResponseEntity.notFound().build();
      }
      log.error("Error while fetching inspection batch by machining batch: {}", exception.getMessage());
      return new ResponseEntity<>(new ErrorResponse("Error while fetching inspection batch by machining batch"),
          HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @GetMapping(value = "tenant/{tenantId}/searchInspectionBatches", produces = MediaType.APPLICATION_JSON)
  public ResponseEntity<Page<InspectionBatchRepresentation>> searchInspectionBatches(
      @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable("tenantId") String tenantId,
      @ApiParam(value = "Type of search", required = true, allowableValues = "ITEM_NAME,FORGE_TRACEABILITY_NUMBER,INSPECTION_BATCH_NUMBER") @RequestParam("searchType") String searchType,
      @ApiParam(value = "Search term", required = true) @RequestParam("searchTerm") String searchTerm,
      @ApiParam(value = "Page number (0-based)", required = false) @RequestParam(value = "page", defaultValue = "0") String pageParam,
      @ApiParam(value = "Page size", required = false) @RequestParam(value = "size", defaultValue = "10") String sizeParam) {

    try {
      Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Not valid tenantId!"));
      
      if (searchType == null || searchType.trim().isEmpty()) {
        return ResponseEntity.badRequest().build();
      }
      
      if (searchTerm == null || searchTerm.trim().isEmpty()) {
        return ResponseEntity.badRequest().build();
      }

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

      Page<InspectionBatchRepresentation> searchResults = inspectionBatchService.searchInspectionBatches(tenantIdLongValue, searchType.trim(), searchTerm.trim(), pageNumber, pageSize);
      return ResponseEntity.ok(searchResults);

    } catch (IllegalArgumentException e) {
      log.error("Invalid search parameters: {}", e.getMessage());
      return ResponseEntity.badRequest().build();
    } catch (Exception e) {
      log.error("Error during inspection batch search: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  private boolean isInvalidInspectionBatchDetails(InspectionBatchRepresentation inspectionBatchRepresentation){
    if(inspectionBatchRepresentation == null ||
       inspectionBatchRepresentation.getInspectionBatchNumber()==null || inspectionBatchRepresentation.getInspectionBatchNumber().isEmpty() ||
       inspectionBatchRepresentation.getStartAt()==null || inspectionBatchRepresentation.getStartAt().isEmpty() ||
       inspectionBatchRepresentation.getEndAt()==null || inspectionBatchRepresentation.getEndAt().isEmpty() ||
       inspectionBatchRepresentation.getProcessedItemInspectionBatch() == null ||
       inspectionBatchRepresentation.getProcessedItemInspectionBatch().getGaugeInspectionReports()== null || inspectionBatchRepresentation.getProcessedItemInspectionBatch().getGaugeInspectionReports().isEmpty()){
      return true;
    }
    return false;
  }

  @GetMapping(value = "tenant/{tenantId}/processedItemInspectionBatches/inspectionBatches", produces = MediaType.APPLICATION_JSON)
  public ResponseEntity<?> getInspectionBatchesByProcessedItemInspectionBatchIds(
      @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable("tenantId") String tenantId,
      @ApiParam(value = "Comma-separated list of processed item inspection batch IDs", required = true) @RequestParam("processedItemInspectionBatchIds") String processedItemInspectionBatchIds) {

    try {
      if (tenantId == null || tenantId.isEmpty() || processedItemInspectionBatchIds == null || processedItemInspectionBatchIds.isEmpty()) {
        log.error("Invalid input for getInspectionBatchesByProcessedItemInspectionBatchIds - tenantId or processedItemInspectionBatchIds is null/empty");
        throw new IllegalArgumentException("Tenant ID and Processed Item Inspection Batch IDs are required and cannot be empty");
      }

      Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Not valid tenantId!"));

      // Parse comma-separated processed item inspection batch IDs
      List<Long> processedItemInspectionBatchIdList = Arrays.stream(processedItemInspectionBatchIds.split(","))
          .map(String::trim)
          .filter(id -> !id.isEmpty())
          .map(id -> GenericResourceUtils.convertResourceIdToLong(id)
              .orElseThrow(() -> new RuntimeException("Not valid processedItemInspectionBatchId: " + id)))
          .collect(Collectors.toList());

      if (processedItemInspectionBatchIdList.isEmpty()) {
        log.error("No valid processed item inspection batch IDs provided");
        throw new IllegalArgumentException("At least one valid processed item inspection batch ID is required");
      }

      List<InspectionBatchRepresentation> inspectionBatchRepresentations = inspectionBatchService.getInspectionBatchesByProcessedItemInspectionBatchIds(processedItemInspectionBatchIdList, tenantIdLongValue);
      
      InspectionBatchListRepresentation inspectionBatchListRepresentation = InspectionBatchListRepresentation.builder()
          .inspectionBatches(inspectionBatchRepresentations)
          .build();
      
      return ResponseEntity.ok(inspectionBatchListRepresentation);

    } catch (Exception exception) {
      if (exception instanceof IllegalArgumentException) {
        log.error("Invalid data for getInspectionBatchesByProcessedItemInspectionBatchIds: {}", exception.getMessage());
        return new ResponseEntity<>(new ErrorResponse(exception.getMessage()), HttpStatus.BAD_REQUEST);
      }
      log.error("Error processing getInspectionBatchesByProcessedItemInspectionBatchIds: {}", exception.getMessage());
      return new ResponseEntity<>(new ErrorResponse("Error retrieving inspection batches by processed item inspection batch IDs"), HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }
}
