package com.jangid.forging_process_management_service.resource.machining;

import com.jangid.forging_process_management_service.assemblers.machining.MachiningBatchAssembler;
import com.jangid.forging_process_management_service.entities.machining.MachineSet;
import com.jangid.forging_process_management_service.entities.machining.MachiningBatch;
import com.jangid.forging_process_management_service.entitiesRepresentation.machining.DailyMachiningBatchRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.machining.MachiningBatchRepresentation;
import com.jangid.forging_process_management_service.exception.TenantNotFoundException;
import com.jangid.forging_process_management_service.exception.forging.ForgeNotFoundException;
import com.jangid.forging_process_management_service.exception.machining.MachiningBatchNotFoundException;
import com.jangid.forging_process_management_service.service.machining.MachiningBatchService;
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
public class MachiningBatchResource {

  @Autowired
  private final MachiningBatchService machiningBatchService;

  @Autowired
  private final MachiningBatchAssembler machiningBatchAssembler;

  @PostMapping("tenant/{tenantId}/machine-set/{machineSetId}/apply-machining-batch")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<MachiningBatchRepresentation> applyMachiningBatch(
      @PathVariable String tenantId,
      @PathVariable String machineSetId,
      @RequestBody MachiningBatchRepresentation machiningBatchRepresentation,
      @RequestParam(required = false, defaultValue = "false") boolean rework) {
    try {
      if (machineSetId == null || machineSetId.isEmpty() ||
          tenantId == null || tenantId.isEmpty() ||
          isInvalidMachiningBatchDetailsForApplying(machiningBatchRepresentation, rework)) {
        log.error("Invalid applyMachiningBatch input!");
        throw new RuntimeException("Invalid applyMachiningBatch input!");
      }

      Long tenantIdLongValue = ResourceUtils.convertIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Not valid tenantId!"));
      Long machineSetIdLongValue = ResourceUtils.convertIdToLong(machineSetId)
          .orElseThrow(() -> new RuntimeException("Not valid machineSetId!"));

      log.info("Rework flag: {} for machine-set: {}", rework, machineSetId);

      // Pass the rework flag to the service layer for further handling
      MachiningBatchRepresentation createdMachiningBatch = machiningBatchService.applyMachiningBatch(
          tenantIdLongValue, machineSetIdLongValue, machiningBatchRepresentation, rework);

      return new ResponseEntity<>(createdMachiningBatch, HttpStatus.CREATED);
    } catch (Exception exception) {
      if (exception instanceof MachiningBatchNotFoundException) {
        return ResponseEntity.notFound().build();
      }
      return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @PostMapping("tenant/{tenantId}/machine-set/{machineSetId}/machining-batch/{machiningBatchId}/start-matchining-batch")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<MachiningBatchRepresentation> startMachiningBatch(@PathVariable String tenantId, @PathVariable String machineSetId, @PathVariable String machiningBatchId,
                                                                          @RequestBody MachiningBatchRepresentation machiningBatchRepresentation,
                                                                          @RequestParam(required = false, defaultValue = "false") boolean rework) {
    try {
      if (machineSetId == null || machineSetId.isEmpty() || tenantId == null || tenantId.isEmpty() || machiningBatchId == null || machiningBatchId.isEmpty()
          || machiningBatchRepresentation.getStartAt() == null
          || machiningBatchRepresentation.getStartAt().isEmpty()) {
        log.error("invalid startMachiningBatch input!");
        throw new RuntimeException("invalid startMachiningBatch input!");
      }
      Long tenantIdLongValue = ResourceUtils.convertIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Not valid tenantId!"));
      Long machineSetIdLongValue = ResourceUtils.convertIdToLong(machineSetId)
          .orElseThrow(() -> new RuntimeException("Not valid machineSetId!"));
      Long machiningBatchIdLongValue = ResourceUtils.convertIdToLong(machiningBatchId)
          .orElseThrow(() -> new RuntimeException("Not valid machiningBatchId!"));

      MachiningBatchRepresentation startedMachiningBatch = machiningBatchService.startMachiningBatch(tenantIdLongValue, machineSetIdLongValue, machiningBatchIdLongValue,
                                                                                                     machiningBatchRepresentation.getStartAt(), rework);
      return new ResponseEntity<>(startedMachiningBatch, HttpStatus.ACCEPTED);
    } catch (Exception exception) {
      if (exception instanceof MachiningBatchNotFoundException) {
        return ResponseEntity.notFound().build();
      }
      return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @PostMapping("tenant/{tenantId}/machine-set/{machineSetId}/machining-batch/{machiningBatchId}/end-matchining-batch")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<MachiningBatchRepresentation> endMachiningBatch(@PathVariable String tenantId, @PathVariable String machineSetId, @PathVariable String machiningBatchId,
                                                                        @RequestBody MachiningBatchRepresentation machiningBatchRepresentation,
                                                                        @RequestParam(required = false, defaultValue = "false") boolean rework) {
    try {
      if (machineSetId == null || machineSetId.isEmpty() || tenantId == null || tenantId.isEmpty() || machiningBatchId == null || machiningBatchId.isEmpty() || isInvalidMachiningBatchDetailsForEnding(
          machiningBatchRepresentation)) {
        log.error("invalid endMachiningBatch input!");
        throw new RuntimeException("invalid endMachiningBatch input!");
      }
      Long tenantIdLongValue = ResourceUtils.convertIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Not valid tenantId!"));
      Long machineSetIdLongValue = ResourceUtils.convertIdToLong(machineSetId)
          .orElseThrow(() -> new RuntimeException("Not valid machineSetId!"));
      Long machiningBatchIdLongValue = ResourceUtils.convertIdToLong(machiningBatchId)
          .orElseThrow(() -> new RuntimeException("Not valid machiningBatchId!"));

      MachiningBatchRepresentation endedMachiningBatch = machiningBatchService.endMachiningBatch(tenantIdLongValue, machineSetIdLongValue, machiningBatchIdLongValue, machiningBatchRepresentation, rework);
      return new ResponseEntity<>(endedMachiningBatch, HttpStatus.ACCEPTED);
    } catch (Exception exception) {
      if (exception instanceof MachiningBatchNotFoundException) {
        return ResponseEntity.notFound().build();
      }
      return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @PostMapping("tenant/{tenantId}/machine-set/{machineSetId}/machining-batch/{machiningBatchId}/daily-matchining-batch-update")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<MachiningBatchRepresentation> dailyMachiningBatchUpdate(@PathVariable String tenantId, @PathVariable String machineSetId, @PathVariable String machiningBatchId,
                                                                                @RequestBody DailyMachiningBatchRepresentation dailyMachiningBatchRepresentation) {
    try {
      if (machineSetId == null || machineSetId.isEmpty() || tenantId == null || tenantId.isEmpty() || machiningBatchId == null || machiningBatchId.isEmpty()
          || isInvalidDailyMachiningBatchRepresentation(dailyMachiningBatchRepresentation)) {
        log.error("invalid dailyMachiningBatchUpdate input!");
        throw new RuntimeException("invalid dailyMachiningBatchUpdate input!");
      }
      Long tenantIdLongValue = ResourceUtils.convertIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Not valid tenantId!"));
      Long machineSetIdLongValue = ResourceUtils.convertIdToLong(machineSetId)
          .orElseThrow(() -> new RuntimeException("Not valid machineSetId!"));
      Long machiningBatchIdLongValue = ResourceUtils.convertIdToLong(machiningBatchId)
          .orElseThrow(() -> new RuntimeException("Not valid machiningBatchId!"));

      MachiningBatchRepresentation dailyMachiningBatchUpdate = machiningBatchService.dailyMachiningBatchUpdate(tenantIdLongValue, machineSetIdLongValue, machiningBatchIdLongValue,
                                                                                                               dailyMachiningBatchRepresentation);
      return new ResponseEntity<>(dailyMachiningBatchUpdate, HttpStatus.ACCEPTED);
    } catch (Exception exception) {
      if (exception instanceof MachiningBatchNotFoundException) {
        return ResponseEntity.notFound().build();
      }
      return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }


  @GetMapping(value = "tenant/{tenantId}/machine-set/{machineSetId}/machining-batch", produces = MediaType.APPLICATION_JSON)
  public ResponseEntity<MachiningBatchRepresentation> getMachiningBatchOfMachineSet(
      @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable("tenantId") String tenantId,
      @ApiParam(value = "Identifier of the machineSet", required = true) @PathVariable("machineSetId") String machineSetId) {

    try {
      Long tenantIdLongValue = ResourceUtils.convertIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Not valid tenantId!"));

      Long machineSetIdLongValue = ResourceUtils.convertIdToLong(machineSetId)
          .orElseThrow(() -> new RuntimeException("Not valid machineSetId!"));

      MachineSet machineSet = machiningBatchService.getMachineSetUsingTenantIdAndMachineSetId(tenantIdLongValue, machineSetIdLongValue);
      MachiningBatch machiningBatch = machiningBatchService.getAppliedMachiningBatchByMachineSet(machineSet.getId());
      MachiningBatchRepresentation representation = machiningBatchAssembler.dissemble(machiningBatch);
      return ResponseEntity.ok(representation);
    } catch (Exception e) {
      if (e instanceof ForgeNotFoundException) {
        return ResponseEntity.ok().build();
      }
      throw e;
    }
  }

  @GetMapping("tenant/{tenantId}/machining-batches")
  public ResponseEntity<Page<MachiningBatchRepresentation>> getAllMachiningBatchByTenantId(
      @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable String tenantId,
      @RequestParam(value = "page", required = false) String page,
      @RequestParam(value = "size", required = false) String size) {
    Long tId = ResourceUtils.convertIdToLong(tenantId)
        .orElseThrow(() -> new TenantNotFoundException(tenantId));

    int pageNumber = ResourceUtils.convertIdToInt(page)
        .orElseThrow(() -> new RuntimeException("Invalid page=" + page));

    int sizeNumber = ResourceUtils.convertIdToInt(size)
        .orElseThrow(() -> new RuntimeException("Invalid size=" + size));

    Page<MachiningBatchRepresentation> batches = machiningBatchService.getAllMachiningBatchByTenantId(tId, pageNumber, sizeNumber);
    return ResponseEntity.ok(batches);
  }


  private boolean isInvalidMachiningBatchDetailsForApplying(MachiningBatchRepresentation representation, boolean rework) {
    if (representation == null ||
        isNullOrEmpty(representation.getMachiningBatchNumber()) ||
        representation.getProcessedItemMachiningBatch() == null ||
        isInvalidMachiningBatchPiecesCount(representation.getProcessedItemMachiningBatch().getMachiningBatchPiecesCount())) {
      return true;
    }

    if (rework) {
      return isReworkInvalid(representation);
    } else {
      return isNonReworkInvalid(representation);
    }
  }

  private boolean isReworkInvalid(MachiningBatchRepresentation representation) {
    return representation.getInputProcessedItemMachiningBatch() == null ||
           representation.getInputProcessedItemMachiningBatch().getId() == null ||
           isInvalidMachiningBatchPiecesCount(representation.getInputProcessedItemMachiningBatch().getReworkPiecesCount());
  }

  private boolean isNonReworkInvalid(MachiningBatchRepresentation representation) {
    return representation.getProcessedItemHeatTreatmentBatch() == null ||
           representation.getProcessedItemHeatTreatmentBatch().getId() == null ||
           representation.getProcessedItemHeatTreatmentBatch().getAvailableMachiningBatchPiecesCount() == null;
  }

  private boolean isNullOrEmpty(String value) {
    return value == null || value.isEmpty();
  }

  private boolean isInvalidMachiningBatchPiecesCount(Integer count) {
    return count == null || count == 0;
  }

  private boolean isInvalidDailyMachiningBatchRepresentation(DailyMachiningBatchRepresentation DailyMachiningBatchRepresentation) {
    if (DailyMachiningBatchRepresentation == null ||
        DailyMachiningBatchRepresentation.getOperationDate() == null || DailyMachiningBatchRepresentation.getOperationDate().isEmpty() ||
        DailyMachiningBatchRepresentation.getStartDateTime() == null || DailyMachiningBatchRepresentation.getStartDateTime().isEmpty() ||
        DailyMachiningBatchRepresentation.getEndDateTime() == null || DailyMachiningBatchRepresentation.getEndDateTime().isEmpty()) {
      return true;
    }
    return false;
  }

  private boolean isInvalidMachiningBatchDetailsForEnding(MachiningBatchRepresentation machiningBatchRepresentation) {
    if (machiningBatchRepresentation == null ||
        machiningBatchRepresentation.getEndAt() == null || machiningBatchRepresentation.getEndAt().isEmpty()) {
      return true;
    }
    return false;
  }
}
