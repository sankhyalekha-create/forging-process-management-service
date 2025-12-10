package com.jangid.forging_process_management_service.resource.operator;

import com.jangid.forging_process_management_service.configuration.security.TenantContextHolder;
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

  @PostMapping("operator")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @ResponseStatus(HttpStatus.CREATED)
  public ResponseEntity<?> createOperator(
      @RequestBody OperatorRepresentation operatorRepresentation) {

    try {
      if (isInvalidOperatorRepresentation(operatorRepresentation)) {
        log.error("Invalid input for createOperator. Operator: {}",
                  operatorRepresentation);
        throw new IllegalArgumentException("Invalid input for createOperator.");
      }

      Long tenantIdLongValue = TenantContextHolder.getAuthenticatedTenantId();

      OperatorRepresentation createdOperatorRepresentation = operatorService.createOperator(tenantIdLongValue, operatorRepresentation);
      return new ResponseEntity<>(createdOperatorRepresentation, HttpStatus.CREATED);
    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "createOperator");
    }
  }

  @PostMapping("operator/{operatorId}")
  public ResponseEntity<?> updateOperator(
      @PathVariable("operatorId") String operatorId,
      @RequestBody OperatorRepresentation operatorRepresentation) {

    try {
      if ( operatorId == null || operatorId.isEmpty() ||
          isInvalidOperatorRepresentation(operatorRepresentation)) {
        log.error("Invalid input for updateOperator!");
        throw new RuntimeException("Invalid input for updateOperator!");
      }

      Long tenantIdLongValue = TenantContextHolder.getAuthenticatedTenantId();

      Long operatorIdLongValue = GenericResourceUtils.convertResourceIdToLong(operatorId)
          .orElseThrow(() -> new RuntimeException("Not valid operatorId!"));

      OperatorRepresentation updatedOperator = operatorService.updateOperator(
          operatorIdLongValue, tenantIdLongValue, operatorRepresentation);

      return ResponseEntity.ok(updatedOperator);
    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "updateOperator");
    }
  }

  @GetMapping("searchOperators")
  public ResponseEntity<?> searchOperators(
      @RequestParam String searchType,
      @RequestParam String searchQuery) {

    try {
      if (searchType == null || searchQuery == null || searchQuery.isBlank()) {
        log.error("Invalid input for searchOperators. SearchType: {}, SearchQuery: {}",
                   searchType, searchQuery);
        throw new IllegalArgumentException("Invalid input for searchOperators.");
      }

      Long tenantIdLongValue = TenantContextHolder.getAuthenticatedTenantId();

      List<MachineOperatorRepresentation> operators = operatorService.searchOperators(tenantIdLongValue, searchType, searchQuery);
      MachineOperatorListRepresentation machineOperatorListRepresentation = MachineOperatorListRepresentation.builder()
          .machineOperators(operators).build();
      return ResponseEntity.ok(machineOperatorListRepresentation);
    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "searchOperators");
    }
  }

  @DeleteMapping("operator/{operatorId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public ResponseEntity<?> deleteOperator(
      @PathVariable("operatorId") String operatorId,
      @RequestParam(value = "dateOfLeaving", required = false) String dateOfLeaving) {
    try {
      if (operatorId == null || operatorId.isBlank()) {
        log.error("Invalid input for deleteOperator. OperatorId: {}", operatorId);
        throw new IllegalArgumentException("Invalid input for deleteOperator.");
      }

      Long tenantIdLongValue = TenantContextHolder.getAuthenticatedTenantId();

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

  @GetMapping(value = "operator/{operatorId}/performance", produces = MediaType.APPLICATION_JSON)
  public ResponseEntity<?> getOperatorPerformanceForPeriod(
      @PathVariable("operatorId") String operatorId,
      @RequestParam(value = "startTime", required = false) String startTime,
      @RequestParam(value = "endTime", required = false) String endTime) {

      try {
          if (operatorId == null || operatorId.isBlank()) {
              log.error("Invalid input parameters. OperatorId: {}", operatorId);
              throw new IllegalArgumentException("Invalid input parameters");
          }

          Long tenantIdLongValue = TenantContextHolder.getAuthenticatedTenantId();

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

  @GetMapping("operator/{operatorId}/check-editability")
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<?> checkOperatorEditability(@PathVariable("operatorId") String operatorId) {
    try {
      if (operatorId == null || operatorId.isBlank()) {
        log.error("Invalid operatorId for checkOperatorEditability: {}", operatorId);
        throw new IllegalArgumentException("Invalid operatorId");
      }

      Long tenantIdLongValue = TenantContextHolder.getAuthenticatedTenantId();

      Long operatorIdLongValue = GenericResourceUtils.convertResourceIdToLong(operatorId)
          .orElseThrow(() -> new IllegalArgumentException("Not a valid operatorId!"));

      boolean isFullyEditable = operatorService.isOperatorFullyEditable(operatorIdLongValue, tenantIdLongValue);

      String message = isFullyEditable
          ? "Operator can be fully edited (all fields)."
          : "Operator has been part of machining batches. Only hourly wages can be updated.";

      return ResponseEntity.ok(java.util.Map.of(
          "isFullyEditable", isFullyEditable,
          "message", message
      ));
    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "checkOperatorEditability");
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
