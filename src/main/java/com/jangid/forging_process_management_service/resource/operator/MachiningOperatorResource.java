package com.jangid.forging_process_management_service.resource.operator;

import com.jangid.forging_process_management_service.entitiesRepresentation.operator.MachineOperatorRepresentation;
import com.jangid.forging_process_management_service.service.operator.MachineOperatorService;
import com.jangid.forging_process_management_service.utils.ResourceUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.stream.Stream;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MachiningOperatorResource {

  private final MachineOperatorService machineOperatorService;

  @PostMapping("tenant/{tenantId}/machine-operator")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @ResponseStatus(HttpStatus.CREATED)
  public ResponseEntity<MachineOperatorRepresentation> createMachineOperator(
      @PathVariable String tenantId,
      @RequestBody MachineOperatorRepresentation machineOperatorRepresentation) {

    if (tenantId == null || tenantId.isBlank() || isInvalidMachineOperatorRepresentation(machineOperatorRepresentation)) {
      log.error("Invalid input for createMachineOperator. TenantId: {}, MachineOperator: {}",
                tenantId, machineOperatorRepresentation);
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid input for createMachineOperator.");
    }

    Long tenantIdLongValue = ResourceUtils.convertIdToLong(tenantId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not a valid tenantId!"));

    MachineOperatorRepresentation createdMachineOperatorRepresentation = machineOperatorService.createMachineOperator(tenantIdLongValue, machineOperatorRepresentation);
    return new ResponseEntity<>(createdMachineOperatorRepresentation, HttpStatus.CREATED);
  }

  private boolean isInvalidMachineOperatorRepresentation(MachineOperatorRepresentation machineOperatorRepresentation) {
    return machineOperatorRepresentation == null ||
           Stream.of(machineOperatorRepresentation.getFullName(),
                     machineOperatorRepresentation.getAddress(),
                     machineOperatorRepresentation.getAadhaarNumber())
               .anyMatch(value -> value == null || value.isBlank()) ||
           !machineOperatorService.isValidAadhaarNumber(machineOperatorRepresentation.getAadhaarNumber());
  }
}
