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
  public ResponseEntity<MachiningBatchRepresentation> applyMachiningBatch(@PathVariable String tenantId, @PathVariable String machineSetId,
                                                                          @RequestBody MachiningBatchRepresentation machiningBatchRepresentation) {
    try {
      if (machineSetId == null || machineSetId.isEmpty() || tenantId == null || tenantId.isEmpty() || isInvalidMachiningBatchDetailsForApplying(machiningBatchRepresentation)) {
        log.error("invalid applyMachiningBatch input!");
        throw new RuntimeException("invalid applyMachiningBatch input!");
      }
      Long tenantIdLongValue = ResourceUtils.convertIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Not valid tenantId!"));
      Long machineSetIdLongValue = ResourceUtils.convertIdToLong(machineSetId)
          .orElseThrow(() -> new RuntimeException("Not valid machineSetId!"));
      MachiningBatchRepresentation createdMachiningBatch = machiningBatchService.applyMachiningBatch(tenantIdLongValue, machineSetIdLongValue, machiningBatchRepresentation);
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
                                                                          @RequestBody MachiningBatchRepresentation machiningBatchRepresentation) {
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
                                                                                                     machiningBatchRepresentation.getStartAt());
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
                                                                        @RequestBody MachiningBatchRepresentation machiningBatchRepresentation) {
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

      MachiningBatchRepresentation endedMachiningBatch = machiningBatchService.endMachiningBatch(tenantIdLongValue, machineSetIdLongValue, machiningBatchIdLongValue, machiningBatchRepresentation);
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


  private boolean isInvalidMachiningBatchDetailsForApplying(MachiningBatchRepresentation representation) {
    if (representation == null ||
        representation.getMachiningBatchNumber() == null || representation.getMachiningBatchNumber().isEmpty() ||
        representation.getProcessedItemMachiningBatches() == null || representation.getProcessedItemMachiningBatches().isEmpty() ||
        representation.getProcessedItemMachiningBatches().stream().anyMatch(
            processedItemMachiningBatchRepresentation -> processedItemMachiningBatchRepresentation.getMachiningBatchPiecesCount() == null
                                                         || processedItemMachiningBatchRepresentation.getMachiningBatchPiecesCount() == 0)) {
      return true;
    }
    return false;
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
        machiningBatchRepresentation.getEndAt() == null || machiningBatchRepresentation.getEndAt().isEmpty() ||
        machiningBatchRepresentation.getProcessedItemMachiningBatches() == null || machiningBatchRepresentation.getProcessedItemMachiningBatches().isEmpty() ||
        machiningBatchRepresentation.getProcessedItemMachiningBatches().stream().anyMatch(
            processedItemMachiningBatchRepresentation ->
                machiningBatchRepresentation.getDailyMachiningBatchDetail() == null || machiningBatchRepresentation.getDailyMachiningBatchDetail()
                    .isEmpty())) {
      return true;
    }
    return false;
  }
}
