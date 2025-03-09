package com.jangid.forging_process_management_service.resource.operator;

import com.jangid.forging_process_management_service.entitiesRepresentation.operator.MachineOperatorListRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.operator.MachineOperatorRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.operator.OperatorRepresentation;
import com.jangid.forging_process_management_service.service.operator.OperatorService;
import com.jangid.forging_process_management_service.utils.GenericResourceUtils;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;
import java.util.stream.Stream;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class OperatorResource {

  private final OperatorService operatorService;

  @PostMapping("tenant/{tenantId}/operator")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @ResponseStatus(HttpStatus.CREATED)
  public ResponseEntity<OperatorRepresentation> createOperator(
      @PathVariable String tenantId,
      @RequestBody OperatorRepresentation operatorRepresentation) {

    if (tenantId == null || tenantId.isBlank() || isInvalidOperatorRepresentation(operatorRepresentation)) {
      log.error("Invalid input for createOperator. TenantId: {}, Operator: {}",
                tenantId, operatorRepresentation);
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid input for createOperator.");
    }

    Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not a valid tenantId!"));

    OperatorRepresentation createdOperatorRepresentation = operatorService.createOperator(tenantIdLongValue, operatorRepresentation);
    return new ResponseEntity<>(createdOperatorRepresentation, HttpStatus.CREATED);
  }

  @PostMapping("tenant/{tenantId}/operator/{operatorId}")
  public ResponseEntity<OperatorRepresentation> updateOperator(
      @PathVariable("tenantId") String tenantId,
      @PathVariable("operatorId") String operatorId,
      @RequestBody OperatorRepresentation operatorRepresentation) {

    if (tenantId == null || tenantId.isEmpty() || operatorId == null || operatorId.isEmpty() ||
        isInvalidOperatorRepresentation(operatorRepresentation)) {
      log.error("Invalid input for updateOperator!");
      throw new RuntimeException("Invalid input for updateOperator!");
    }

    Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
        .orElseThrow(() -> new RuntimeException("Not valid tenant id!"));

    Long operatorIdLongValue = GenericResourceUtils.convertResourceIdToLong(operatorId)
        .orElseThrow(() -> new RuntimeException("Not valid operatorId!"));

    OperatorRepresentation updatedOperator = operatorService.updateOperator(
        operatorIdLongValue, tenantIdLongValue, operatorRepresentation);

    return ResponseEntity.ok(updatedOperator);
  }

  @GetMapping("tenant/{tenantId}/searchOperators")
  public ResponseEntity<MachineOperatorListRepresentation> searchOperators(
      @PathVariable String tenantId,
      @RequestParam String searchType,
      @RequestParam String searchQuery) {

    if (tenantId == null || tenantId.isBlank() || searchType == null || searchQuery == null || searchQuery.isBlank()) {
      log.error("Invalid input for searchOperators. TenantId: {}, SearchType: {}, SearchQuery: {}",
                tenantId, searchType, searchQuery);
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid input for searchOperators.");
    }

    Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not a valid tenantId!"));

    List<MachineOperatorRepresentation> operators = operatorService.searchOperators(tenantIdLongValue, searchType, searchQuery);
    MachineOperatorListRepresentation machineOperatorListRepresentation = MachineOperatorListRepresentation.builder()
        .machineOperators(operators).build();
    return ResponseEntity.ok(machineOperatorListRepresentation);
  }

  private boolean isInvalidOperatorRepresentation(OperatorRepresentation operatorRepresentation) {
    return operatorRepresentation == null ||
           Stream.of(operatorRepresentation.getFullName(),
                     operatorRepresentation.getAddress(),
                     operatorRepresentation.getAadhaarNumber())
               .anyMatch(value -> value == null || value.isBlank()) ||
           operatorRepresentation.getOperatorType() == null ||
           !operatorService.isValidAadhaarNumber(operatorRepresentation.getAadhaarNumber());
  }
}
