package com.jangid.forging_process_management_service.resource.dispatch;

import com.jangid.forging_process_management_service.entitiesRepresentation.dispatch.DispatchBatchListRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.dispatch.DispatchBatchRepresentation;
import com.jangid.forging_process_management_service.exception.TenantNotFoundException;
import com.jangid.forging_process_management_service.exception.dispatch.DispatchBatchNotFoundException;
import com.jangid.forging_process_management_service.exception.forging.ForgeNotFoundException;
import com.jangid.forging_process_management_service.service.dispatch.DispatchBatchService;
import com.jangid.forging_process_management_service.utils.ResourceUtils;

import io.swagger.annotations.ApiParam;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
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
  public ResponseEntity<DispatchBatchRepresentation> createDispatchBatch(
      @PathVariable String tenantId,
      @RequestBody DispatchBatchRepresentation dispatchBatchRepresentation) {
    try {
      if (tenantId == null || tenantId.isEmpty() ||
          isInvalidDispatchBatchDetails(dispatchBatchRepresentation)) {
        log.error("Invalid createDispatchBatch input!");
        throw new RuntimeException("Invalid createDispatchBatch input!");
      }

      Long tenantIdLongValue = ResourceUtils.convertIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Not valid tenantId!"));

      DispatchBatchRepresentation createdDispatchBatch = dispatchBatchService.createDispatchBatch(
          tenantIdLongValue, dispatchBatchRepresentation);

      return new ResponseEntity<>(createdDispatchBatch, HttpStatus.CREATED);
    } catch (Exception exception) {
      if (exception instanceof DispatchBatchNotFoundException) {
        return ResponseEntity.notFound().build();
      }
      return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @PostMapping("tenant/{tenantId}/dispatchBatch/{dispatchBatchId}/ready-to-dispatch")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<DispatchBatchRepresentation> readyToDispatch(@PathVariable String tenantId, @PathVariable String dispatchBatchId,
                                                                     @RequestBody DispatchBatchRepresentation dispatchBatchRepresentation) {
    try {
      if (dispatchBatchId == null || dispatchBatchId.isEmpty() || tenantId == null || tenantId.isEmpty() || dispatchBatchRepresentation == null
          || dispatchBatchRepresentation.getDispatchReadyAt() == null
          || dispatchBatchRepresentation.getDispatchReadyAt().isEmpty()) {
        log.error("invalid readyToDispatch input!");
        throw new RuntimeException("invalid readyToDispatch input!");
      }
      Long tenantIdLongValue = ResourceUtils.convertIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Not valid tenantId!"));
      Long dispatchBatchIdLongValue = ResourceUtils.convertIdToLong(dispatchBatchId)
          .orElseThrow(() -> new RuntimeException("Not valid dispatchBatchId!"));

      DispatchBatchRepresentation updatedDispatchBatch = dispatchBatchService.markReadyToDispatchBatch(tenantIdLongValue, dispatchBatchIdLongValue, dispatchBatchRepresentation.getDispatchReadyAt());
      return new ResponseEntity<>(updatedDispatchBatch, HttpStatus.ACCEPTED);
    } catch (Exception exception) {
      if (exception instanceof ForgeNotFoundException) {
        return ResponseEntity.notFound().build();
      }
      return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }


  @PostMapping("tenant/{tenantId}/dispatchBatch/{dispatchBatchId}/dispatched")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<DispatchBatchRepresentation> dispatched(@PathVariable String tenantId, @PathVariable String dispatchBatchId,
                                                                @RequestBody DispatchBatchRepresentation dispatchBatchRepresentation) {
    try {
      if (dispatchBatchId == null || dispatchBatchId.isEmpty() || tenantId == null || tenantId.isEmpty() || dispatchBatchRepresentation == null
          || dispatchBatchRepresentation.getDispatchedAt() == null || dispatchBatchRepresentation.getDispatchedAt().isEmpty()) {
        log.error("invalid dispatched input!");
        throw new RuntimeException("invalid dispatched input!");
      }
      Long tenantIdLongValue = ResourceUtils.convertIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Not valid tenantId!"));
      Long dispatchBatchIdLongValue = ResourceUtils.convertIdToLong(dispatchBatchId)
          .orElseThrow(() -> new RuntimeException("Not valid dispatchBatchId!"));

      DispatchBatchRepresentation updatedDispatchBatch = dispatchBatchService.markDispatchedToDispatchBatch(tenantIdLongValue, dispatchBatchIdLongValue, dispatchBatchRepresentation.getDispatchedAt());
      return new ResponseEntity<>(updatedDispatchBatch, HttpStatus.ACCEPTED);
    } catch (Exception exception) {
      if (exception instanceof ForgeNotFoundException) {
        return ResponseEntity.notFound().build();
      }
      return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }


  @GetMapping("tenant/{tenantId}/dispatch-batches")
  public ResponseEntity<?> getAllDispatchBatchesOfTenant(@ApiParam(value = "Identifier of the tenant", required = true) @PathVariable String tenantId,
                                                         @RequestParam(value = "page", required = false) String page,
                                                         @RequestParam(value = "size", required = false) String size) {
    Long tId = ResourceUtils.convertIdToLong(tenantId)
        .orElseThrow(() -> new TenantNotFoundException(tenantId));

    Integer pageNumber = (page == null || page.isBlank()) ? -1
                                                          : ResourceUtils.convertIdToInt(page)
                             .orElseThrow(() -> new RuntimeException("Invalid page=" + page));

    Integer sizeNumber = (size == null || size.isBlank()) ? -1
                                                          : ResourceUtils.convertIdToInt(size)
                             .orElseThrow(() -> new RuntimeException("Invalid size=" + size));

    if (pageNumber == -1 || sizeNumber == -1) {
      DispatchBatchListRepresentation dispatchBatchListRepresentation = dispatchBatchService.getAllDispatchBatchesOfTenantWithoutPagination(tId);
      return ResponseEntity.ok(dispatchBatchListRepresentation);
    }
    Page<DispatchBatchRepresentation> dispatchBatchRepresentations = dispatchBatchService.getAllDispatchBatchesOfTenant(tId, pageNumber, sizeNumber);
    return ResponseEntity.ok(dispatchBatchRepresentations);
  }


  private boolean isInvalidDispatchBatchDetails(DispatchBatchRepresentation dispatchBatchRepresentation) {
    if (dispatchBatchRepresentation == null ||
        dispatchBatchRepresentation.getDispatchBatchNumber() == null || dispatchBatchRepresentation.getDispatchBatchNumber().isEmpty() ||
        dispatchBatchRepresentation.getDispatchCreatedAt() == null || dispatchBatchRepresentation.getDispatchCreatedAt().isEmpty() ||
        dispatchBatchRepresentation.getProcessedItemInspectionBatches() == null || dispatchBatchRepresentation.getProcessedItemInspectionBatches().isEmpty() ||
        dispatchBatchRepresentation.getProcessedItemDispatchBatch() == null || dispatchBatchRepresentation.getProcessedItemDispatchBatch().getTotalDispatchPiecesCount() == null
        || dispatchBatchRepresentation.getProcessedItemDispatchBatch().getTotalDispatchPiecesCount() == 0 ||
        dispatchBatchRepresentation.getProcessedItemInspectionBatches().stream()
            .anyMatch(processedItemInspectionBatchRepresentation -> processedItemInspectionBatchRepresentation.getAvailableDispatchPiecesCount() == 0)) {
      return true;
    }
    return false;
  }
}
