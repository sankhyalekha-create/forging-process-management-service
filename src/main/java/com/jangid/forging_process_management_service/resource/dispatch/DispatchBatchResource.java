package com.jangid.forging_process_management_service.resource.dispatch;

import com.jangid.forging_process_management_service.entitiesRepresentation.dispatch.DispatchBatchListRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.dispatch.DispatchBatchRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.dispatch.DispatchStatisticsRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.error.ErrorResponse;
import com.jangid.forging_process_management_service.exception.TenantNotFoundException;
import com.jangid.forging_process_management_service.exception.dispatch.DispatchBatchNotFoundException;
import com.jangid.forging_process_management_service.exception.forging.ForgeNotFoundException;
import com.jangid.forging_process_management_service.service.dispatch.DispatchBatchService;
import com.jangid.forging_process_management_service.utils.GenericResourceUtils;

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
public class DispatchBatchResource {

  @Autowired
  private final DispatchBatchService dispatchBatchService;

  @PostMapping("tenant/{tenantId}/create-dispatch-batch")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<?> createDispatchBatch(
      @PathVariable String tenantId,
      @RequestBody DispatchBatchRepresentation dispatchBatchRepresentation) {
    try {
      if (tenantId == null || tenantId.isEmpty() ||
          isInvalidDispatchBatchDetails(dispatchBatchRepresentation)) {
        log.error("Invalid createDispatchBatch input!");
        throw new RuntimeException("Invalid createDispatchBatch input!");
      }

      Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Not valid tenantId!"));

      DispatchBatchRepresentation createdDispatchBatch = dispatchBatchService.createDispatchBatch(
          tenantIdLongValue, dispatchBatchRepresentation);

      return new ResponseEntity<>(createdDispatchBatch, HttpStatus.CREATED);
    } catch (Exception exception) {
      if (exception instanceof DispatchBatchNotFoundException) {
        return ResponseEntity.notFound().build();
      }
      if (exception instanceof IllegalStateException) {
        log.error("Dispatch Batch exists with the given dispatch batch number: {}", dispatchBatchRepresentation.getDispatchBatchNumber());
        return new ResponseEntity<>(new ErrorResponse("Dispatch Batch exists with the given Dispatch batch number"), HttpStatus.CONFLICT);
      }
      return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @PostMapping("tenant/{tenantId}/dispatchBatch/{dispatchBatchId}/ready-to-dispatch")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<DispatchBatchRepresentation> readyToDispatch(
      @PathVariable String tenantId, 
      @PathVariable String dispatchBatchId,
      @RequestBody DispatchBatchRepresentation dispatchBatchRepresentation) {
    try {
        validateReadyToDispatchInput(tenantId, dispatchBatchId, dispatchBatchRepresentation);
        
        Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
            .orElseThrow(() -> new RuntimeException("Not valid tenantId!"));
        Long dispatchBatchIdLongValue = GenericResourceUtils.convertResourceIdToLong(dispatchBatchId)
            .orElseThrow(() -> new RuntimeException("Not valid dispatchBatchId!"));

        DispatchBatchRepresentation updatedDispatchBatch = dispatchBatchService
            .markReadyToDispatchBatch(tenantIdLongValue, dispatchBatchIdLongValue, dispatchBatchRepresentation);
        return new ResponseEntity<>(updatedDispatchBatch, HttpStatus.ACCEPTED);
    } catch (ForgeNotFoundException e) {
        return ResponseEntity.notFound().build();
    } catch (RuntimeException e) {
        log.error("Error in readyToDispatch: {}", e.getMessage());
        return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  private void validateReadyToDispatchInput(String tenantId, String dispatchBatchId, 
      DispatchBatchRepresentation dispatchBatchRepresentation) {
      if (isNullOrEmpty(tenantId) || 
          isNullOrEmpty(dispatchBatchId) || 
          isInvalidDispatchReadyDetails(dispatchBatchRepresentation)) {
          log.error("Invalid readyToDispatch input!");
          throw new RuntimeException("Invalid readyToDispatch input!");
      }
  }

  private boolean isInvalidDispatchReadyDetails(DispatchBatchRepresentation representation) {
      if (representation == null || isNullOrEmpty(representation.getDispatchReadyAt()) || representation.getPackagingType() == null) {
          return true;
      }
      
      // Check if using uniform packaging
      Boolean useUniformPackaging = representation.getUseUniformPackaging();
      if (useUniformPackaging == null || useUniformPackaging) {
          // For uniform packaging, validate packagingQuantity and perPackagingQuantity
          return isInvalidPackagingQuantity(representation);
      } else {
          // For non-uniform packaging, validate dispatchPackages
          return representation.getDispatchPackages() == null || 
                 representation.getDispatchPackages().isEmpty() ||
                 representation.getDispatchPackages().stream()
                     .anyMatch(pkg -> pkg.getQuantityInPackage() == null || pkg.getQuantityInPackage() <= 0);
      }
  }

  private boolean isInvalidPackagingQuantity(DispatchBatchRepresentation representation) {
      return representation.getPackagingQuantity() == null ||
             representation.getPackagingQuantity() <= 0 ||
             representation.getPerPackagingQuantity() == null ||
             representation.getPerPackagingQuantity() <= 0;
  }

  private boolean isNullOrEmpty(String value) {
      return value == null || value.isEmpty();
  }

  @PostMapping("tenant/{tenantId}/dispatchBatch/{dispatchBatchId}/dispatched")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<?> dispatched(
      @PathVariable String tenantId, 
      @PathVariable String dispatchBatchId,
      @RequestBody DispatchBatchRepresentation dispatchBatchRepresentation) {
    try {
      validateDispatchedInput(tenantId, dispatchBatchId, dispatchBatchRepresentation);
      
      Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Not valid tenantId!"));
      Long dispatchBatchIdLongValue = GenericResourceUtils.convertResourceIdToLong(dispatchBatchId)
          .orElseThrow(() -> new RuntimeException("Not valid dispatchBatchId!"));

      DispatchBatchRepresentation updatedDispatchBatch = dispatchBatchService
          .markDispatchedToDispatchBatch(tenantIdLongValue, dispatchBatchIdLongValue, dispatchBatchRepresentation);
      return new ResponseEntity<>(updatedDispatchBatch, HttpStatus.ACCEPTED);
    } catch (Exception exception) {
      if (exception instanceof DispatchBatchNotFoundException) {
        return ResponseEntity.notFound().build();
      }
      if (exception instanceof IllegalStateException) {
        log.error("Error: {}", exception.getMessage());
        return new ResponseEntity<>(new ErrorResponse(exception.getMessage()), HttpStatus.CONFLICT);
      }
      if (exception instanceof IllegalArgumentException) {
        log.error("Invalid data: {}", exception.getMessage());
        return new ResponseEntity<>(new ErrorResponse(exception.getMessage()), HttpStatus.BAD_REQUEST);
      }
      log.error("Error processing dispatch: {}", exception.getMessage());
      return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }
  
  private void validateDispatchedInput(String tenantId, String dispatchBatchId, 
      DispatchBatchRepresentation dispatchBatchRepresentation) {
    if (isNullOrEmpty(tenantId) || isNullOrEmpty(dispatchBatchId) || 
        dispatchBatchRepresentation == null || 
        isNullOrEmpty(dispatchBatchRepresentation.getDispatchedAt())) {
      log.error("Invalid dispatched input - missing required fields!");
      throw new RuntimeException("Invalid dispatched input - missing required fields!");
    }
    
    // Validate invoice fields
    if (isNullOrEmpty(dispatchBatchRepresentation.getInvoiceNumber()) || 
        isNullOrEmpty(dispatchBatchRepresentation.getInvoiceDateTime())) {
      log.error("Invoice number and date are required for dispatch!");
      throw new IllegalArgumentException("Invoice number and date are required for dispatch!");
    }
  }

  @DeleteMapping("tenant/{tenantId}/dispatchBatch/{dispatchBatchId}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<?> deleteDispatchBatch(
      @PathVariable String tenantId,
      @PathVariable String dispatchBatchId) {
    try {
      if (tenantId == null || tenantId.isEmpty() || dispatchBatchId == null || dispatchBatchId.isEmpty()) {
        log.error("Invalid deleteDispatchBatch input!");
        throw new RuntimeException("Invalid deleteDispatchBatch input!");
      }

      Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Not valid tenantId!"));
      Long dispatchBatchIdLongValue = GenericResourceUtils.convertResourceIdToLong(dispatchBatchId)
          .orElseThrow(() -> new RuntimeException("Not valid dispatchBatchId!"));

      DispatchBatchRepresentation deletedDispatchBatch = dispatchBatchService.deleteDispatchBatch(
          tenantIdLongValue, dispatchBatchIdLongValue);

      return new ResponseEntity<>(deletedDispatchBatch, HttpStatus.OK);
    } catch (Exception exception) {
      if (exception instanceof DispatchBatchNotFoundException) {
        return ResponseEntity.notFound().build();
      }
      if (exception instanceof IllegalStateException) {
        log.error("This dispatch batch cannot be deleted as it is not in the DISPATCHED status.");
        return new ResponseEntity<>(new ErrorResponse("This dispatch batch cannot be deleted as it is not in the DISPATCHED status."),
            HttpStatus.CONFLICT);
      }
      return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @GetMapping("tenant/{tenantId}/dispatch-batches")
  public ResponseEntity<?> getAllDispatchBatchesOfTenant(@ApiParam(value = "Identifier of the tenant", required = true) @PathVariable String tenantId,
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
      DispatchBatchListRepresentation dispatchBatchListRepresentation = dispatchBatchService.getAllDispatchBatchesOfTenantWithoutPagination(tId);
      return ResponseEntity.ok(dispatchBatchListRepresentation);
    }
    Page<DispatchBatchRepresentation> dispatchBatchRepresentations = dispatchBatchService.getAllDispatchBatchesOfTenant(tId, pageNumber, sizeNumber);
    return ResponseEntity.ok(dispatchBatchRepresentations);
  }

  @GetMapping("tenant/{tenantId}/dispatch-statistics")
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<?> getDispatchStatisticsByMonthRange(
      @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable String tenantId,
      @ApiParam(value = "Start month (1-12)", required = true) @RequestParam int fromMonth,
      @ApiParam(value = "Start year", required = true) @RequestParam int fromYear,
      @ApiParam(value = "End month (1-12)", required = true) @RequestParam int toMonth,
      @ApiParam(value = "End year", required = true) @RequestParam int toYear) {
    try {
      Long tId = GenericResourceUtils.convertResourceIdToLong(tenantId)
          .orElseThrow(() -> new TenantNotFoundException(tenantId));

      // Basic validation for month and year can be added here or in the service
      if (fromMonth < 1 || fromMonth > 12 || toMonth < 1 || toMonth > 12 || fromYear <= 0 || toYear <= 0) {
        log.error("Invalid month/year parameters provided.");
        return new ResponseEntity<>(new ErrorResponse("Invalid month/year parameters provided."), HttpStatus.BAD_REQUEST);
      }
      // Add more validation if needed (e.g., from date before to date)

      List<DispatchStatisticsRepresentation> statistics = dispatchBatchService.getDispatchStatisticsByMonthRange(
          tId, fromMonth, fromYear, toMonth, toYear);

      return ResponseEntity.ok(statistics);
    } catch (TenantNotFoundException e) {
      log.error("Tenant not found: {}", tenantId);
      return new ResponseEntity<>(new ErrorResponse("Tenant not found"), HttpStatus.NOT_FOUND);
    } catch (Exception e) {
      log.error("Error fetching dispatch statistics: {}", e.getMessage(), e);
      return new ResponseEntity<>(new ErrorResponse("Error fetching dispatch statistics"), HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @GetMapping(value = "tenant/{tenantId}/searchDispatchBatches", produces = MediaType.APPLICATION_JSON)
  public ResponseEntity<Page<DispatchBatchRepresentation>> searchDispatchBatches(
      @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable("tenantId") String tenantId,
      @ApiParam(value = "Type of search", required = true, allowableValues = "ITEM_NAME,FORGE_TRACEABILITY_NUMBER,DISPATCH_BATCH_NUMBER,DISPATCH_BATCH_STATUS") @RequestParam("searchType") String searchType,
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

      Page<DispatchBatchRepresentation> searchResults = dispatchBatchService.searchDispatchBatches(tenantIdLongValue, searchType.trim(), searchTerm.trim(), pageNumber, pageSize);
      return ResponseEntity.ok(searchResults);

    } catch (IllegalArgumentException e) {
      log.error("Invalid search parameters: {}", e.getMessage());
      return ResponseEntity.badRequest().build();
    } catch (Exception e) {
      log.error("Error during dispatch batch search: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  @GetMapping(value = "tenant/{tenantId}/processedItemDispatchBatches/dispatchBatches", produces = MediaType.APPLICATION_JSON)
  public ResponseEntity<?> getDispatchBatchesByProcessedItemDispatchBatchIds(
      @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable("tenantId") String tenantId,
      @ApiParam(value = "Comma-separated list of processed item dispatch batch IDs", required = true) @RequestParam("processedItemDispatchBatchIds") String processedItemDispatchBatchIds) {

    try {
      if (tenantId == null || tenantId.isEmpty() || processedItemDispatchBatchIds == null || processedItemDispatchBatchIds.isEmpty()) {
        log.error("Invalid input for getDispatchBatchesByProcessedItemDispatchBatchIds - tenantId or processedItemDispatchBatchIds is null/empty");
        throw new IllegalArgumentException("Tenant ID and Processed Item Dispatch Batch IDs are required and cannot be empty");
      }

      Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Not valid tenantId!"));

      // Parse comma-separated processed item dispatch batch IDs
      List<Long> processedItemDispatchBatchIdList = Arrays.stream(processedItemDispatchBatchIds.split(","))
          .map(String::trim)
          .filter(id -> !id.isEmpty())
          .map(id -> GenericResourceUtils.convertResourceIdToLong(id)
              .orElseThrow(() -> new RuntimeException("Not valid processedItemDispatchBatchId: " + id)))
          .collect(Collectors.toList());

      if (processedItemDispatchBatchIdList.isEmpty()) {
        log.error("No valid processed item dispatch batch IDs provided");
        throw new IllegalArgumentException("At least one valid processed item dispatch batch ID is required");
      }

      List<DispatchBatchRepresentation> dispatchBatchRepresentations = dispatchBatchService.getDispatchBatchesByProcessedItemDispatchBatchIds(processedItemDispatchBatchIdList, tenantIdLongValue);
      
      DispatchBatchListRepresentation dispatchBatchListRepresentation = DispatchBatchListRepresentation.builder()
          .dispatchBatches(dispatchBatchRepresentations)
          .build();
      
      return ResponseEntity.ok(dispatchBatchListRepresentation);

    } catch (Exception exception) {
      if (exception instanceof IllegalArgumentException) {
        log.error("Invalid data for getDispatchBatchesByProcessedItemDispatchBatchIds: {}", exception.getMessage());
        return new ResponseEntity<>(new ErrorResponse(exception.getMessage()), HttpStatus.BAD_REQUEST);
      }
      log.error("Error processing getDispatchBatchesByProcessedItemDispatchBatchIds: {}", exception.getMessage());
      return new ResponseEntity<>(new ErrorResponse("Error retrieving dispatch batches by processed item dispatch batch IDs"), HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  private boolean isInvalidDispatchBatchDetails(DispatchBatchRepresentation dispatchBatchRepresentation) {
    if (dispatchBatchRepresentation == null ||
        dispatchBatchRepresentation.getDispatchBatchNumber() == null || dispatchBatchRepresentation.getDispatchBatchNumber().isEmpty() ||
        dispatchBatchRepresentation.getDispatchCreatedAt() == null || dispatchBatchRepresentation.getDispatchCreatedAt().isEmpty() ||
        dispatchBatchRepresentation.getBuyerId() == null ||
        dispatchBatchRepresentation.getProcessedItemDispatchBatch() == null || dispatchBatchRepresentation.getProcessedItemDispatchBatch().getTotalDispatchPiecesCount() == null
        || dispatchBatchRepresentation.getProcessedItemDispatchBatch().getTotalDispatchPiecesCount() == 0 ||
        dispatchBatchRepresentation.getDispatchProcessedItemInspections().stream()
            .anyMatch(dispatchProcessedItemInspectionRepresentation -> dispatchProcessedItemInspectionRepresentation.getProcessedItemInspectionBatch() ==null ||
                dispatchProcessedItemInspectionRepresentation.getProcessedItemInspectionBatch().getSelectedDispatchPiecesCount() == null ||
                                                                       dispatchProcessedItemInspectionRepresentation.getProcessedItemInspectionBatch().getSelectedDispatchPiecesCount() == 0)) {
      return true;
    }
    return false;
  }
}
