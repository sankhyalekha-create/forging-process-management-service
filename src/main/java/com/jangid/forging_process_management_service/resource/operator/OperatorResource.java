package com.jangid.forging_process_management_service.resource.operator;

import com.jangid.forging_process_management_service.entitiesRepresentation.operator.MachineOperatorListRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.operator.MachineOperatorRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.operator.OperatorRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.overview.OperatorPerformanceRepresentation;
import com.jangid.forging_process_management_service.service.operator.OperatorService;
import com.jangid.forging_process_management_service.utils.ConvertorUtils;
import com.jangid.forging_process_management_service.utils.GenericResourceUtils;
import com.jangid.forging_process_management_service.utils.GenericExceptionHandler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
  public ResponseEntity<?> createOperator(
      @PathVariable String tenantId,
      @RequestBody OperatorRepresentation operatorRepresentation) {

    try {
      if (tenantId == null || tenantId.isBlank() || isInvalidOperatorRepresentation(operatorRepresentation)) {
        log.error("Invalid input for createOperator. TenantId: {}, Operator: {}",
                  tenantId, operatorRepresentation);
        throw new IllegalArgumentException("Invalid input for createOperator.");
      }

      Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
          .orElseThrow(() -> new IllegalArgumentException("Not a valid tenantId!"));

      OperatorRepresentation createdOperatorRepresentation = operatorService.createOperator(tenantIdLongValue, operatorRepresentation);
      return new ResponseEntity<>(createdOperatorRepresentation, HttpStatus.CREATED);
    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "createOperator");
    }
  }

  @PostMapping("tenant/{tenantId}/operator/{operatorId}")
  public ResponseEntity<?> updateOperator(
      @PathVariable("tenantId") String tenantId,
      @PathVariable("operatorId") String operatorId,
      @RequestBody OperatorRepresentation operatorRepresentation) {

    try {
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
    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "updateOperator");
    }
  }

  @GetMapping("tenant/{tenantId}/searchOperators")
  public ResponseEntity<?> searchOperators(
      @PathVariable String tenantId,
      @RequestParam String searchType,
      @RequestParam String searchQuery) {

    try {
      if (tenantId == null || tenantId.isBlank() || searchType == null || searchQuery == null || searchQuery.isBlank()) {
        log.error("Invalid input for searchOperators. TenantId: {}, SearchType: {}, SearchQuery: {}",
                  tenantId, searchType, searchQuery);
        throw new IllegalArgumentException("Invalid input for searchOperators.");
      }

      Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
          .orElseThrow(() -> new IllegalArgumentException("Not a valid tenantId!"));

      List<MachineOperatorRepresentation> operators = operatorService.searchOperators(tenantIdLongValue, searchType, searchQuery);
      MachineOperatorListRepresentation machineOperatorListRepresentation = MachineOperatorListRepresentation.builder()
          .machineOperators(operators).build();
      return ResponseEntity.ok(machineOperatorListRepresentation);
    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "searchOperators");
    }
  }

  @DeleteMapping("tenant/{tenantId}/operator/{operatorId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public ResponseEntity<?> deleteOperator(
      @PathVariable("tenantId") String tenantId,
      @PathVariable("operatorId") String operatorId,
      @RequestParam(value = "dateOfLeaving", required = false) String dateOfLeaving) {
    try {
      if (tenantId == null || tenantId.isBlank() || operatorId == null || operatorId.isBlank()) {
        log.error("Invalid input for deleteOperator. TenantId: {}, OperatorId: {}", tenantId, operatorId);
        throw new IllegalArgumentException("Invalid input for deleteOperator.");
      }

      Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
          .orElseThrow(() -> new IllegalArgumentException("Not a valid tenantId!"));

      Long operatorIdLongValue = GenericResourceUtils.convertResourceIdToLong(operatorId)
          .orElseThrow(() -> new IllegalArgumentException("Not a valid operatorId!"));

      // Convert date of leaving string to LocalDate if provided
      java.time.LocalDate dateOfLeavingLocalDate = null;
      if (dateOfLeaving != null && !dateOfLeaving.isBlank()) {
        try {
          dateOfLeavingLocalDate = java.time.LocalDate.parse(dateOfLeaving);
        } catch (Exception e) {
          log.error("Invalid date format for dateOfLeaving: {}", dateOfLeaving);
          throw new IllegalArgumentException("Invalid date format for dateOfLeaving. Use ISO format (YYYY-MM-DD).");
        }
      }

      operatorService.deleteOperator(operatorIdLongValue, tenantIdLongValue, dateOfLeavingLocalDate);
      return ResponseEntity.noContent().build();
    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "deleteOperator");
    }
  }

  @GetMapping(value = "tenant/{tenantId}/operator/{operatorId}/performance", produces = MediaType.APPLICATION_JSON)
  public ResponseEntity<?> getOperatorPerformanceForPeriod(
      @PathVariable("tenantId") String tenantId,
      @PathVariable("operatorId") String operatorId,
      @RequestParam(value = "startTime", required = false) String startTime,
      @RequestParam(value = "endTime", required = false) String endTime) {

      try {
          if (tenantId == null || tenantId.isBlank() || operatorId == null || operatorId.isBlank()) {
              log.error("Invalid input parameters. TenantId: {}, OperatorId: {}", tenantId, operatorId);
              throw new IllegalArgumentException("Invalid input parameters");
          }

          Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
              .orElseThrow(() -> new IllegalArgumentException("Not valid tenantId!"));

          Long operatorIdLongValue = GenericResourceUtils.convertResourceIdToLong(operatorId)
              .orElseThrow(() -> new IllegalArgumentException("Not valid operatorId!"));

          LocalDateTime startLocalDateTime = ConvertorUtils.convertStringToLocalDateTime(startTime);
          LocalDateTime endLocalDateTime = ConvertorUtils.convertStringToLocalDateTime(endTime);

          OperatorPerformanceRepresentation performance = operatorService.getOperatorPerformanceForPeriod(
              tenantIdLongValue,
              operatorIdLongValue,
              startLocalDateTime,
              endLocalDateTime);

          return ResponseEntity.ok(performance);
      } catch (Exception exception) {
          return GenericExceptionHandler.handleException(exception, "getOperatorPerformanceForPeriod");
      }
  }

  private boolean isInvalidOperatorRepresentation(OperatorRepresentation operatorRepresentation) {
    return operatorRepresentation == null ||
           Stream.of(operatorRepresentation.getFullName(),
                     operatorRepresentation.getAddress(),
                     operatorRepresentation.getAadhaarNumber())
               .anyMatch(value -> value == null || value.isBlank()) ||
           operatorRepresentation.getOperatorType() == null ||
           !operatorService.isValidAadhaarNumber(operatorRepresentation.getAadhaarNumber()) ||
           (operatorRepresentation.getHourlyWages() != null && operatorRepresentation.getHourlyWages().compareTo(BigDecimal.ZERO) < 0);
  }
}
