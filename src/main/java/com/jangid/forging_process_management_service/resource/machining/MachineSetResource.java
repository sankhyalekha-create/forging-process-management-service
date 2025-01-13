package com.jangid.forging_process_management_service.resource.machining;

import com.jangid.forging_process_management_service.entitiesRepresentation.machining.MachineSetRepresentation;
import com.jangid.forging_process_management_service.exception.TenantNotFoundException;
import com.jangid.forging_process_management_service.service.machining.MachineSetService;
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
public class MachineSetResource {

  @Autowired
  private MachineSetService machineSetService;

  @PostMapping("tenant/{tenantId}/machineSet")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<MachineSetRepresentation> createMachineSet(@PathVariable String tenantId, @RequestBody MachineSetRepresentation machineSetRepresentation) {
    try {
      if (tenantId == null || tenantId.isEmpty() || isInvalidMachineSetRepresentation(machineSetRepresentation)) {
        log.error("invalid createMachine input!");
        throw new RuntimeException("invalid createMachine input!");
      }
      Long tenantIdLongValue = ResourceUtils.convertIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Not valid tenantId!"));
      MachineSetRepresentation createdMachineSet = machineSetService.createMachineSet(tenantIdLongValue, machineSetRepresentation);
      return new ResponseEntity<>(createdMachineSet, HttpStatus.CREATED);
    } catch (Exception exception) {
      return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @GetMapping("tenant/{tenantId}/machineSets")
  public ResponseEntity<Page<MachineSetRepresentation>> getAllMachineSetsOfTenant(@ApiParam(value = "Identifier of the tenant", required = true) @PathVariable String tenantId,
                                                                            @RequestParam(value = "page", defaultValue = "0") String page,
                                                                            @RequestParam(value = "size", defaultValue = "5") String size) {
    Long tId = ResourceUtils.convertIdToLong(tenantId)
        .orElseThrow(() -> new TenantNotFoundException(tenantId));

    int pageNumber = ResourceUtils.convertIdToInt(page)
        .orElseThrow(() -> new RuntimeException("Invalid page="+page));

    int sizeNumber = ResourceUtils.convertIdToInt(size)
        .orElseThrow(() -> new RuntimeException("Invalid size="+size));

    Page<MachineSetRepresentation> machines = machineSetService.getAllMachineSetPagesOfTenant(tId, pageNumber, sizeNumber);
    return ResponseEntity.ok(machines);
  }

  @PostMapping("tenant/{tenantId}/machineSet/{machineSetId}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<MachineSetRepresentation> updateMachineSet(
      @PathVariable("tenantId") String tenantId, @PathVariable("machineSetId") String machineSetId,
      @RequestBody MachineSetRepresentation machineSetRepresentation) {
    if (tenantId == null || tenantId.isEmpty() || machineSetId == null || machineSetId.isEmpty() || isInvalidMachineSetRepresentation(machineSetRepresentation)) {
      log.error("invalid input for updateMachineSet!");
      throw new RuntimeException("invalid input for updateMachineSet!");
    }
    Long tenantIdLongValue = ResourceUtils.convertIdToLong(tenantId)
        .orElseThrow(() -> new RuntimeException("Not valid tenant id!"));

    Long machineSetIdLongValue = ResourceUtils.convertIdToLong(machineSetId)
        .orElseThrow(() -> new RuntimeException("Not valid machineSetId!"));

    MachineSetRepresentation updatedMachineSet = machineSetService.updateMachineSet(machineSetIdLongValue, tenantIdLongValue, machineSetRepresentation);
    return ResponseEntity.ok(updatedMachineSet);
  }


  private boolean isInvalidMachineSetRepresentation(MachineSetRepresentation machineSetRepresentation) {
    if (machineSetRepresentation == null ||
        machineSetRepresentation.getMachineSetName() == null || machineSetRepresentation.getMachineSetName().isEmpty() ||
        machineSetRepresentation.getMachines() == null || machineSetRepresentation.getMachines().isEmpty()) {
      return true;
    }
    return false;
  }
}
