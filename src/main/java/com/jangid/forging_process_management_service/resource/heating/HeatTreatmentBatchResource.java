package com.jangid.forging_process_management_service.resource.heating;

import com.jangid.forging_process_management_service.assemblers.heating.HeatTreatmentBatchAssembler;
import com.jangid.forging_process_management_service.entities.forging.Furnace;
import com.jangid.forging_process_management_service.entities.heating.HeatTreatmentBatch;
import com.jangid.forging_process_management_service.entitiesRepresentation.error.ErrorResponse;
import com.jangid.forging_process_management_service.entitiesRepresentation.heating.HeatTreatmentBatchRepresentation;
import com.jangid.forging_process_management_service.exception.TenantNotFoundException;
import com.jangid.forging_process_management_service.exception.forging.ForgeNotFoundException;
import com.jangid.forging_process_management_service.exception.heating.HeatTreatmentBatchNotFoundException;
import com.jangid.forging_process_management_service.service.heating.HeatTreatmentBatchService;
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

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Component
public class HeatTreatmentBatchResource {

  @Autowired
  private HeatTreatmentBatchService heatTreatmentBatchService;
  @Autowired
  private HeatTreatmentBatchAssembler heatTreatmentBatchAssembler;

  @PostMapping("tenant/{tenantId}/furnace/{furnaceId}/heat")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<?> applyHeatTreatmentBatch(@PathVariable String tenantId, @PathVariable String furnaceId,
                                                                      @RequestBody HeatTreatmentBatchRepresentation heatTreatmentBatchRepresentation) {
    try {
      if (furnaceId == null || furnaceId.isEmpty() || tenantId == null || tenantId.isEmpty() || isInvalidHeatTreatmentBatchDetailsForApply(heatTreatmentBatchRepresentation)) {
        log.error("invalid heatTreatmentBatch input for applyHeatTreatmentBatch!");
        throw new RuntimeException("invalid heatTreatmentBatch input for applyHeatTreatmentBatch!");
      }
      Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Not valid tenantId for applyHeatTreatmentBatch!"));
      Long furnaceIdLongValue = GenericResourceUtils.convertResourceIdToLong(furnaceId)
          .orElseThrow(() -> new RuntimeException("Not valid furnaceId for applyHeatTreatmentBatch!"));
      HeatTreatmentBatchRepresentation createdHeatTreatmentBatch = heatTreatmentBatchService.applyHeatTreatmentBatch(tenantIdLongValue, furnaceIdLongValue, heatTreatmentBatchRepresentation);
      return new ResponseEntity<>(createdHeatTreatmentBatch, HttpStatus.CREATED);
    } catch (Exception exception) {
      if (exception instanceof HeatTreatmentBatchNotFoundException) {
        return ResponseEntity.notFound().build();
      }
      if (exception instanceof IllegalStateException) {
        log.error("Heat Treatment Batch exists with the given heat treatment batch number: {}", heatTreatmentBatchRepresentation.getHeatTreatmentBatchNumber());
        return new ResponseEntity<>(new ErrorResponse("Heat Treatment Batch exists with the given heat treatment batch number"), HttpStatus.CONFLICT);
      }
      return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @PostMapping("tenant/{tenantId}/furnace/{furnaceId}/heat/{heatTreatmentBatchId}/startHeatTreatmentBatch")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<HeatTreatmentBatchRepresentation> startHeatTreatmentBatch(@PathVariable String tenantId, @PathVariable String furnaceId, @PathVariable String heatTreatmentBatchId,
                                                                    @RequestBody HeatTreatmentBatchRepresentation heatTreatmentBatchRepresentation) {
    try {
      if (furnaceId == null || furnaceId.isEmpty() || tenantId == null || tenantId.isEmpty() || heatTreatmentBatchId == null || heatTreatmentBatchId.isEmpty() || heatTreatmentBatchRepresentation.getStartAt() == null
          || heatTreatmentBatchRepresentation.getStartAt().isEmpty()) {
        log.error("invalid HeatTreatmentBatch input!");
        throw new RuntimeException("invalid HeatTreatmentBatch input for startHeatTreatmentBatch!");
      }
      Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Not valid tenantId for startHeatTreatmentBatch!"));
      Long furnaceIdLongValue = GenericResourceUtils.convertResourceIdToLong(furnaceId)
          .orElseThrow(() -> new RuntimeException("Not valid furnaceId for startHeatTreatmentBatch!"));
      Long heatTreatmentBatchIdLongValue = GenericResourceUtils.convertResourceIdToLong(heatTreatmentBatchId)
          .orElseThrow(() -> new RuntimeException("Not valid heatTreatmentBatchId for startHeatTreatmentBatch!"));

      HeatTreatmentBatchRepresentation updatedHeatTreatmentBatchRepresentation = heatTreatmentBatchService.startHeatTreatmentBatch(tenantIdLongValue, furnaceIdLongValue, heatTreatmentBatchIdLongValue, heatTreatmentBatchRepresentation.getStartAt());
      return new ResponseEntity<>(updatedHeatTreatmentBatchRepresentation, HttpStatus.ACCEPTED);
    } catch (Exception exception) {
      if (exception instanceof HeatTreatmentBatchNotFoundException) {
        return ResponseEntity.notFound().build();
      }
      return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @PostMapping("tenant/{tenantId}/furnace/{furnaceId}/heat/{heatTreatmentBatchId}/endHeatTreatmentBatch")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<HeatTreatmentBatchRepresentation> endHeatTreatmentBatch(@PathVariable String tenantId, @PathVariable String furnaceId, @PathVariable String heatTreatmentBatchId,
                                                      @RequestBody HeatTreatmentBatchRepresentation heatTreatmentBatchRepresentation) {
    try {
      if (furnaceId == null || furnaceId.isEmpty() || tenantId == null || tenantId.isEmpty() || heatTreatmentBatchId == null || heatTreatmentBatchId.isEmpty() || heatTreatmentBatchRepresentation.getEndAt() == null
          || heatTreatmentBatchRepresentation.getEndAt().isEmpty() || isInvalidHeatTreatmentBatchDetailsForComplete(heatTreatmentBatchRepresentation)) {
        log.error("invalid heatTreatmentBatchRepresentation input for endHeatTreatmentBatch!");
        throw new RuntimeException("invalid heatTreatmentBatchRepresentation input!");
      }
      Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Not valid tenantId for endHeatTreatmentBatch!"));
      Long furnaceIdLongValue = GenericResourceUtils.convertResourceIdToLong(furnaceId)
          .orElseThrow(() -> new RuntimeException("Not valid furnaceId for endHeatTreatmentBatch!"));
      Long heatTreatmentBatchIdLongValue = GenericResourceUtils.convertResourceIdToLong(heatTreatmentBatchId)
          .orElseThrow(() -> new RuntimeException("Not valid heatTreatmentBatchId for endHeatTreatmentBatch!"));

      HeatTreatmentBatchRepresentation updatedHeatTreatmentBatch = heatTreatmentBatchService.endHeatTreatmentBatch(tenantIdLongValue, furnaceIdLongValue, heatTreatmentBatchIdLongValue, heatTreatmentBatchRepresentation);
      return new ResponseEntity<>(updatedHeatTreatmentBatch, HttpStatus.ACCEPTED);
    } catch (Exception exception) {
      if (exception instanceof HeatTreatmentBatchNotFoundException) {
        return ResponseEntity.notFound().build();
      }
      return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @GetMapping(value = "tenant/{tenantId}/furnace/{furnaceId}/heat", produces = MediaType.APPLICATION_JSON)
  public ResponseEntity<HeatTreatmentBatchRepresentation> getHeatTreatmentBatchOfFurnace(
      @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable("tenantId") String tenantId,
      @ApiParam(value = "Identifier of the furnace", required = true) @PathVariable("furnaceId") String furnaceId) {

    try {
      Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Not valid tenantId!"));

      Long furnaceIdLongValue = GenericResourceUtils.convertResourceIdToLong(furnaceId)
          .orElseThrow(() -> new RuntimeException("Not valid furnaceId!"));

      Furnace furnace = heatTreatmentBatchService.getFurnaceUsingTenantIdAndFurnaceId(tenantIdLongValue, furnaceIdLongValue);
      HeatTreatmentBatch heatTreatmentBatch = heatTreatmentBatchService.getAppliedHeatTreatmentByFurnace(furnace.getId());
      HeatTreatmentBatchRepresentation representation = heatTreatmentBatchAssembler.dissemble(heatTreatmentBatch);
      return ResponseEntity.ok(representation);
    } catch (Exception e) {
      if (e instanceof ForgeNotFoundException) {
        return ResponseEntity.ok().build();
      }
      throw e;
    }
  }

  @GetMapping("tenant/{tenantId}/heats")
  public ResponseEntity<Page<HeatTreatmentBatchRepresentation>> getAllHeatTreatmentBatchByTenantId(
      @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable String tenantId,
      @RequestParam(value = "page") String page,
      @RequestParam(value = "size") String size) {
    Long tId = GenericResourceUtils.convertResourceIdToLong(tenantId)
        .orElseThrow(() -> new TenantNotFoundException(tenantId));

    int pageNumber = GenericResourceUtils.convertResourceIdToInt(page)
        .orElseThrow(() -> new RuntimeException("Invalid page="+page));

    int sizeNumber = GenericResourceUtils.convertResourceIdToInt(size)
        .orElseThrow(() -> new RuntimeException("Invalid size="+size));

    Page<HeatTreatmentBatchRepresentation> batches = heatTreatmentBatchService.getAllHeatTreatmentBatchByTenantId(tId, pageNumber, sizeNumber);
    return ResponseEntity.ok(batches);
  }

  @DeleteMapping("tenant/{tenantId}/heat/{heatTreatmentBatchId}")
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<?> deleteHeatTreatmentBatch(
      @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable String tenantId,
      @ApiParam(value = "Identifier of the heat treatment batch", required = true) @PathVariable String heatTreatmentBatchId) {

    try {
      Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Not valid tenantId!"));
      Long heatTreatmentBatchIdLongValue = GenericResourceUtils.convertResourceIdToLong(heatTreatmentBatchId)
          .orElseThrow(() -> new RuntimeException("Not valid heatTreatmentBatchId!"));

      heatTreatmentBatchService.deleteHeatTreatmentBatch(tenantIdLongValue, heatTreatmentBatchIdLongValue);
      return ResponseEntity.ok().build();

    } catch (Exception exception) {
      if (exception instanceof HeatTreatmentBatchNotFoundException) {
        return ResponseEntity.notFound().build();
      }
      if (exception instanceof IllegalStateException) {
        log.error("Error while deleting heat treatment batch: {}", exception.getMessage());
        return new ResponseEntity<>(new ErrorResponse(exception.getMessage()), HttpStatus.CONFLICT);
      }
      log.error("Error while deleting heat treatment batch: {}", exception.getMessage());
      return new ResponseEntity<>(new ErrorResponse("Error while deleting heat treatment batch"),
                                 HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  private boolean isInvalidHeatTreatmentBatchDetailsForApply(HeatTreatmentBatchRepresentation representation) {
    if (representation == null ||
        representation.getHeatTreatmentBatchNumber() == null || representation.getHeatTreatmentBatchNumber().isEmpty() ||
        representation.getApplyAt() == null || representation.getApplyAt().isEmpty() ||
        representation.getProcessedItemHeatTreatmentBatches() == null || representation.getProcessedItemHeatTreatmentBatches().isEmpty() ||
        representation.getProcessedItemHeatTreatmentBatches().stream().anyMatch(processedItemHeatTreatmentBatchRepresentation -> processedItemHeatTreatmentBatchRepresentation.getHeatTreatBatchPiecesCount() ==null || processedItemHeatTreatmentBatchRepresentation.getHeatTreatBatchPiecesCount()==0)) {
      log.error("invalid heatTreatmentBatch input for apply!");
      return true;
    }
    return false;
  }

  private boolean isInvalidHeatTreatmentBatchDetailsForComplete(HeatTreatmentBatchRepresentation representation) {
    if (representation == null ||
        representation.getProcessedItemHeatTreatmentBatches() == null || representation.getProcessedItemHeatTreatmentBatches().isEmpty() ||
        representation.getProcessedItemHeatTreatmentBatches().stream().anyMatch(processedItemRepresentation -> processedItemRepresentation.getActualHeatTreatBatchPiecesCount() == null || processedItemRepresentation.getActualHeatTreatBatchPiecesCount() == 0)) {
      log.error("invalid heatTreatmentBatch item input!");
      return true;
    }
    return false;
  }
}
