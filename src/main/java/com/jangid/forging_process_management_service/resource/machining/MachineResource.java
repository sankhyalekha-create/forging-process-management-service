package com.jangid.forging_process_management_service.resource.machining;

import com.jangid.forging_process_management_service.entitiesRepresentation.error.ErrorResponse;
import com.jangid.forging_process_management_service.entitiesRepresentation.machining.MachineListRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.machining.MachineRepresentation;
import com.jangid.forging_process_management_service.exception.TenantNotFoundException;
import com.jangid.forging_process_management_service.exception.machining.MachineNotFoundException;
import com.jangid.forging_process_management_service.service.machining.MachineService;
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
public class MachineResource {

  @Autowired
  private MachineService machineService;

  @PostMapping("tenant/{tenantId}/machine")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<MachineRepresentation> createMachine(@PathVariable String tenantId, @RequestBody MachineRepresentation machineRepresentation) {
    try {
      if (tenantId == null || tenantId.isEmpty() || isInvalidMachineRepresentation(machineRepresentation)) {
        log.error("invalid createMachine input!");
        throw new RuntimeException("invalid createMachine input!");
      }
      Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Not valid tenantId!"));
      MachineRepresentation createdMachine = machineService.createMachine(tenantIdLongValue, machineRepresentation);
      return new ResponseEntity<>(createdMachine, HttpStatus.CREATED);
    } catch (Exception exception) {
      return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @GetMapping("tenant/{tenantId}/machines")
  public ResponseEntity<?> getAllMachinesOfTenant(@ApiParam(value = "Identifier of the tenant", required = true) @PathVariable String tenantId,
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
      MachineListRepresentation machineListRepresentation = machineService.getAllMachinesOfTenantWithoutPagination(tId);
      return ResponseEntity.ok(machineListRepresentation);
    }
      Page<MachineRepresentation> machines = machineService.getAllMachinesOfTenant(tId, pageNumber, sizeNumber);
      return ResponseEntity.ok(machines);
    }

  @GetMapping("tenant/{tenantId}/available-machines")
  public ResponseEntity<MachineListRepresentation> getAllMachinesAvailableForMachineSetOfTenant(@ApiParam(value = "Identifier of the tenant", required = true) @PathVariable String tenantId) {
    Long tId = GenericResourceUtils.convertResourceIdToLong(tenantId)
        .orElseThrow(() -> new TenantNotFoundException(tenantId));

      MachineListRepresentation machineListRepresentation = machineService.getAllMachinesAvailableForMachineSetOfTenant(tId);
      return ResponseEntity.ok(machineListRepresentation);
  }

  @PostMapping("tenant/{tenantId}/machine/{machineId}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<MachineRepresentation> updateMachine(
      @PathVariable("tenantId") String tenantId, @PathVariable("machineId") String machineId,
      @RequestBody MachineRepresentation machineRepresentation) {
    if (tenantId == null || tenantId.isEmpty() || machineId == null || machineId.isEmpty() || isInvalidMachineRepresentation(machineRepresentation)) {
      log.error("invalid input for updateMachine!");
      throw new RuntimeException("invalid input for updateMachine!");
    }
    Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
        .orElseThrow(() -> new RuntimeException("Not valid tenant id!"));

    Long machineIdLongValue = GenericResourceUtils.convertResourceIdToLong(machineId)
        .orElseThrow(() -> new RuntimeException("Not valid machineId!"));

    MachineRepresentation updatedMachine = machineService.updateMachine(machineIdLongValue, tenantIdLongValue, machineRepresentation);
    return ResponseEntity.ok(updatedMachine);
  }

  @DeleteMapping("tenant/{tenantId}/machine/{machineId}")
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<?> deleteMachine(
      @PathVariable("tenantId") String tenantId, @PathVariable("machineId") String machineId) {
    try {
      if (tenantId == null || tenantId.isEmpty() || machineId == null || machineId.isEmpty()) {
        log.error("invalid input for deleteMachine!");
        throw new RuntimeException("invalid input for deleteMachine!");
      }
      Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Not valid tenant id!"));

      Long machineIdLongValue = GenericResourceUtils.convertResourceIdToLong(machineId)
          .orElseThrow(() -> new RuntimeException("Not valid machineId!"));

      machineService.deleteMachine(machineIdLongValue, tenantIdLongValue);

      return ResponseEntity.ok().build();
    } catch (Exception exception) {
      if (exception instanceof MachineNotFoundException) {
        return ResponseEntity.notFound().build();
      }
      if (exception instanceof IllegalStateException) {
        if (exception.getMessage().contains("part of a MachineSet")) {
          log.error("The machine is part of a MachineSet. Remove it from the MachineSet first.");
          return new ResponseEntity<>(new ErrorResponse("The machine is part of a MachineSet. Remove it from the MachineSet first."), HttpStatus.CONFLICT);
        }
      }
      log.error("Error while deleting machine: {}", exception.getMessage());
      return new ResponseEntity<>(new ErrorResponse("Error while deleting machine"),
                                  HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  private boolean isInvalidMachineRepresentation(MachineRepresentation machineRepresentation) {
    if (machineRepresentation == null ||
        machineRepresentation.getMachineName() == null || machineRepresentation.getMachineName().isEmpty()) {
      return true;
    }
    return false;
  }
}
