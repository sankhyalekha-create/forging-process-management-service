package com.jangid.forging_process_management_service.resource.heating;

import com.jangid.forging_process_management_service.assemblers.heating.HeatTreatmentBatchAssembler;
import com.jangid.forging_process_management_service.entities.forging.Furnace;
import com.jangid.forging_process_management_service.entities.heating.HeatTreatmentBatch;
import com.jangid.forging_process_management_service.entitiesRepresentation.heating.HeatTreatmentBatchRepresentation;
import com.jangid.forging_process_management_service.exception.TenantNotFoundException;
import com.jangid.forging_process_management_service.exception.forging.ForgeNotFoundException;
import com.jangid.forging_process_management_service.exception.heating.HeatTreatmentBatchNotFoundException;
import com.jangid.forging_process_management_service.service.heating.HeatTreatmentBatchService;
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
public class HeatTreatmentBatchResource {

  @Autowired
  private HeatTreatmentBatchService heatTreatmentBatchService;
  @Autowired
  private HeatTreatmentBatchAssembler heatTreatmentBatchAssembler;

  @PostMapping("tenant/{tenantId}/furnace/{furnaceId}/heat")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<HeatTreatmentBatchRepresentation> applyHeatTreatmentBatch(@PathVariable String tenantId, @PathVariable String furnaceId,
                                                                      @RequestBody HeatTreatmentBatchRepresentation heatTreatmentBatchRepresentation) {
    try {
      if (furnaceId == null || furnaceId.isEmpty() || tenantId == null || tenantId.isEmpty() || isInvalidHeatTreatmentBatchDetails(heatTreatmentBatchRepresentation)) {
        log.error("invalid heatTreatmentBatch input for applyHeatTreatmentBatch!");
        throw new RuntimeException("invalid heatTreatmentBatch input for applyHeatTreatmentBatch!");
      }
      Long tenantIdLongValue = ResourceUtils.convertIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Not valid tenantId for applyHeatTreatmentBatch!"));
      Long furnaceIdLongValue = ResourceUtils.convertIdToLong(furnaceId)
          .orElseThrow(() -> new RuntimeException("Not valid furnaceId for applyHeatTreatmentBatch!"));
      HeatTreatmentBatchRepresentation createdHeatTreatmentBatch = heatTreatmentBatchService.applyHeatTreatmentBatch(tenantIdLongValue, furnaceIdLongValue, heatTreatmentBatchRepresentation);
      return new ResponseEntity<>(createdHeatTreatmentBatch, HttpStatus.CREATED);
    } catch (Exception exception) {
      if (exception instanceof HeatTreatmentBatchNotFoundException) {
        return ResponseEntity.notFound().build();
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
      Long tenantIdLongValue = ResourceUtils.convertIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Not valid tenantId for startHeatTreatmentBatch!"));
      Long furnaceIdLongValue = ResourceUtils.convertIdToLong(furnaceId)
          .orElseThrow(() -> new RuntimeException("Not valid furnaceId for startHeatTreatmentBatch!"));
      Long heatTreatmentBatchIdLongValue = ResourceUtils.convertIdToLong(heatTreatmentBatchId)
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
          || heatTreatmentBatchRepresentation.getEndAt().isEmpty() || isInvalidHeatTreatmentBatchItemDetails(heatTreatmentBatchRepresentation)) {
        log.error("invalid heatTreatmentBatchRepresentation input for endHeatTreatmentBatch!");
        throw new RuntimeException("invalid heatTreatmentBatchRepresentation input!");
      }
      Long tenantIdLongValue = ResourceUtils.convertIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Not valid tenantId for endHeatTreatmentBatch!"));
      Long furnaceIdLongValue = ResourceUtils.convertIdToLong(furnaceId)
          .orElseThrow(() -> new RuntimeException("Not valid furnaceId for endHeatTreatmentBatch!"));
      Long heatTreatmentBatchIdLongValue = ResourceUtils.convertIdToLong(heatTreatmentBatchId)
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
      Long tenantIdLongValue = ResourceUtils.convertIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Not valid tenantId!"));

      Long furnaceIdLongValue = ResourceUtils.convertIdToLong(furnaceId)
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
    Long tId = ResourceUtils.convertIdToLong(tenantId)
        .orElseThrow(() -> new TenantNotFoundException(tenantId));

    int pageNumber = ResourceUtils.convertIdToInt(page)
        .orElseThrow(() -> new RuntimeException("Invalid page="+page));

    int sizeNumber = ResourceUtils.convertIdToInt(size)
        .orElseThrow(() -> new RuntimeException("Invalid size="+size));

    Page<HeatTreatmentBatchRepresentation> batches = heatTreatmentBatchService.getAllHeatTreatmentBatchByTenantId(tId, pageNumber, sizeNumber);
    return ResponseEntity.ok(batches);
  }



  private boolean isInvalidHeatTreatmentBatchDetails(HeatTreatmentBatchRepresentation representation) {
    if (representation == null ||
        representation.getProcessedItems() == null || representation.getProcessedItems().isEmpty() ||
        representation.getProcessedItems().stream().anyMatch(processedItemRepresentation -> processedItemRepresentation.getHeatTreatBatchPiecesCount() == null || processedItemRepresentation.getHeatTreatBatchPiecesCount().isEmpty())) {
      log.error("invalid heatTreatmentBatch input!");
      return true;
    }
    return false;
  }

  private boolean isInvalidHeatTreatmentBatchItemDetails(HeatTreatmentBatchRepresentation representation) {
    if (representation == null ||
        representation.getProcessedItems() == null || representation.getProcessedItems().isEmpty() ||
        representation.getProcessedItems().stream().anyMatch(processedItemRepresentation -> processedItemRepresentation.getActualHeatTreatBatchPiecesCount() == null || processedItemRepresentation.getActualHeatTreatBatchPiecesCount().isEmpty())) {
      log.error("invalid heatTreatmentBatch item input!");
      return true;
    }
    return false;
  }
}
