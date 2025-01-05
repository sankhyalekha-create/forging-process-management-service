package com.jangid.forging_process_management_service.resource.machining;

import com.jangid.forging_process_management_service.entitiesRepresentation.machining.DailyMachiningBatchDetailRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.machining.MachiningBatchRepresentation;
import com.jangid.forging_process_management_service.exception.machining.MachiningBatchNotFoundException;
import com.jangid.forging_process_management_service.service.machining.MachiningBatchService;
import com.jangid.forging_process_management_service.utils.ResourceUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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

  @PostMapping("tenant/{machineSetId}/machine-set/{machineSetId}/machining-batch/{machiningBatchId}/start-matchining-batch")
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

  @PostMapping("tenant/{machineSetId}/machine-set/{machineSetId}/machining-batch/{machiningBatchId}/end-matchining-batch")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<MachiningBatchRepresentation> endMachiningBatch(@PathVariable String tenantId, @PathVariable String machineSetId, @PathVariable String machiningBatchId,
                                                                        @RequestBody MachiningBatchRepresentation machiningBatchRepresentation) {
    try {
      if (machineSetId == null || machineSetId.isEmpty() || tenantId == null || tenantId.isEmpty() || machiningBatchId == null || machiningBatchId.isEmpty() || isInvalidMachiningBatchDetailsForEnding(
          machiningBatchRepresentation)) {
        log.error("invalid encMachiningBatch input!");
        throw new RuntimeException("invalid encMachiningBatch input!");
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


  @PostMapping("tenant/{machineSetId}/machine-set/{machineSetId}/machining-batch/{machiningBatchId}/daily-matchining-batch-update")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<MachiningBatchRepresentation> dailyMachiningBatchUpdate(@PathVariable String tenantId, @PathVariable String machineSetId, @PathVariable String machiningBatchId,
                                                                                @RequestBody DailyMachiningBatchDetailRepresentation dailyMachiningBatchDetailRepresentation) {
    try {
      if (machineSetId == null || machineSetId.isEmpty() || tenantId == null || tenantId.isEmpty() || machiningBatchId == null || machiningBatchId.isEmpty()
          || isInvalidDailyMachiningBatchDetailRepresentation(dailyMachiningBatchDetailRepresentation)) {
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
                                                                                                               dailyMachiningBatchDetailRepresentation);
      return new ResponseEntity<>(dailyMachiningBatchUpdate, HttpStatus.ACCEPTED);
    } catch (Exception exception) {
      if (exception instanceof MachiningBatchNotFoundException) {
        return ResponseEntity.notFound().build();
      }
      return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }


  private boolean isInvalidMachiningBatchDetailsForApplying(MachiningBatchRepresentation representation) {
    if (representation == null ||
        representation.getMachiningBatchNumber() == null || representation.getMachiningBatchNumber().isEmpty() ||
        representation.getAppliedMachiningBatchPiecesCount() == null || representation.getAppliedMachiningBatchPiecesCount().isEmpty() ||
        representation.getProcessedItem() == null ||
        representation.getProcessedItem().getAvailableMachiningBatchPiecesCount() == null || representation.getProcessedItem().getAvailableMachiningBatchPiecesCount().isEmpty()
        || representation.getProcessedItem().getAvailableMachiningBatchPiecesCount().equals("0")) {
      return true;
    }
    return false;
  }

  private boolean isInvalidDailyMachiningBatchDetailRepresentation(DailyMachiningBatchDetailRepresentation dailyMachiningBatchDetailRepresentation) {
    if (dailyMachiningBatchDetailRepresentation == null ||
        dailyMachiningBatchDetailRepresentation.getOperationDate() == null || dailyMachiningBatchDetailRepresentation.getOperationDate().isEmpty() ||
        dailyMachiningBatchDetailRepresentation.getStartDateTime() == null || dailyMachiningBatchDetailRepresentation.getStartDateTime().isEmpty() ||
        dailyMachiningBatchDetailRepresentation.getEndDateTime() == null || dailyMachiningBatchDetailRepresentation.getEndDateTime().isEmpty()) {
      return false;
    }
    return true;
  }

  private boolean isInvalidMachiningBatchDetailsForEnding(MachiningBatchRepresentation machiningBatchRepresentation) {
    if (machiningBatchRepresentation == null ||
        machiningBatchRepresentation.getEndAt() == null || machiningBatchRepresentation.getEndAt().isEmpty() ||
        machiningBatchRepresentation.getDailyMachiningBatchDetail() == null || machiningBatchRepresentation.getDailyMachiningBatchDetail().isEmpty()) {
      return false;
    }
    return true;
  }
}
