package com.jangid.forging_process_management_service.service.operator;

import com.jangid.forging_process_management_service.assemblers.operator.MachineOperatorAssembler;
import com.jangid.forging_process_management_service.assemblers.operator.OperatorAssembler;
import com.jangid.forging_process_management_service.entities.Tenant;
import com.jangid.forging_process_management_service.entities.machining.DailyMachiningBatch;
import com.jangid.forging_process_management_service.entities.operator.MachineOperator;
import com.jangid.forging_process_management_service.entities.operator.Operator;
import com.jangid.forging_process_management_service.entitiesRepresentation.operator.MachineOperatorRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.operator.OperatorRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.operator.OperatorType;
import com.jangid.forging_process_management_service.entitiesRepresentation.overview.OperatorPerformanceRepresentation;
import com.jangid.forging_process_management_service.exception.operator.MachineOperatorNotFoundException;
import com.jangid.forging_process_management_service.exception.operator.OperatorNotFoundException;
import com.jangid.forging_process_management_service.repositories.machining.DailyMachiningBatchRepository;
import com.jangid.forging_process_management_service.repositories.operator.MachineOperatorRepository;
import com.jangid.forging_process_management_service.repositories.operator.OperatorRepository;
import com.jangid.forging_process_management_service.service.TenantService;
import com.jangid.forging_process_management_service.utils.ValidationUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OperatorService {

  private final OperatorRepository operatorRepository;
  private final MachineOperatorRepository machineOperatorRepository;
  private final TenantService tenantService;
  private final OperatorAssembler operatorAssembler;
  private final MachineOperatorAssembler machineOperatorAssembler;
  private final DailyMachiningBatchRepository dailyMachiningBatchRepository;

  public OperatorRepresentation createOperator(Long tenantId, OperatorRepresentation operatorRepresentation) {
    String aadhaarNumber = operatorRepresentation.getAadhaarNumber();
    if (!isValidAadhaarNumber(aadhaarNumber)) {
      log.error("Invalid Aadhaar number: {}", aadhaarNumber);
      throw new IllegalArgumentException("Invalid Aadhaar number. It must be a 12-digit numeric value.");
    }

    String phoneNumber = operatorRepresentation.getPhoneNumber();
    if (phoneNumber != null && !phoneNumber.trim().isEmpty() && !ValidationUtils.isValidPhoneNumber(phoneNumber)) {
      log.error("Invalid phone number format: {}", phoneNumber);
      throw new IllegalArgumentException("Invalid phone number format. Please provide a valid phone number (e.g., +919876543210).");
    }
    
    // Validate hourly wages if provided
    if (operatorRepresentation.getHourlyWages() != null && operatorRepresentation.getHourlyWages().compareTo(java.math.BigDecimal.ZERO) < 0) {
      log.error("Invalid hourly wages: {}", operatorRepresentation.getHourlyWages());
      throw new IllegalArgumentException("Hourly wages cannot be negative.");
    }
    
    // Set date of joining to current date if not provided
    if (operatorRepresentation.getDateOfJoining() == null) {
      operatorRepresentation.setDateOfJoining(java.time.LocalDate.now());
    }

    boolean exists = operatorRepository.existsByAadhaarNumberAndDeletedFalse(aadhaarNumber);
    if (exists) {
      Operator operator = operatorRepository.findByAadhaarNumberAndDeletedFalse(aadhaarNumber).get();
      log.error("Operator with Aadhaar number {} already associated with the tenant {}!", aadhaarNumber, operator.getTenant().getTenantName());
      throw new IllegalStateException("Operator with this aadhaar number already associated with the tenant=" + operator.getTenant().getTenantName());
    }

    if (OperatorType.MACHINING.equals(operatorRepresentation.getOperatorType())) {
      MachineOperator machineOperator = machineOperatorAssembler.createAssemble(operatorRepresentation);
      machineOperator.setTenant(tenantService.getTenantById(tenantId));

      MachineOperator savedMachineOperator = machineOperatorRepository.save(machineOperator);
      log.info("MachineOperator created successfully with ID {}", savedMachineOperator.getId());

      OperatorRepresentation createdOperatorRepresentation = operatorAssembler.dissemble(savedMachineOperator);
      createdOperatorRepresentation.setOperatorType(OperatorType.MACHINING);
      return createdOperatorRepresentation;
    }
    return null;
  }


  public boolean isValidAadhaarNumber(String aadhaarNumber) {
    return aadhaarNumber != null && aadhaarNumber.matches("\\d{12}");
  }

  public Operator getOperatorById(long id) {
    Optional<Operator> operatorOptional = operatorRepository.findByIdAndDeletedFalse(id);
    if (operatorOptional.isEmpty()) {
      log.error("Operator does not exists for id={}", id);
      throw new MachineOperatorNotFoundException("Operator does not exists for id=" + id);
    }
    return operatorOptional.get();
  }

  public OperatorRepresentation updateOperator(Long id, Long tenantId, OperatorRepresentation operatorRepresentation) {
    Operator operator = getOperatorByIdAndTenantId(id, tenantId);
    boolean isUpdated = false; // Flag to track changes

    // Update fields only if changed
    if (!operator.getFullName().equals(operatorRepresentation.getFullName())) {
      operator.setFullName(operatorRepresentation.getFullName());
      isUpdated = true;
    }

    if (!operator.getAddress().equals(operatorRepresentation.getAddress())) {
      operator.setAddress(operatorRepresentation.getAddress());
      isUpdated = true;
    }

    if (!operator.getAadhaarNumber().equals(operatorRepresentation.getAadhaarNumber())) {
      operator.setAadhaarNumber(operatorRepresentation.getAadhaarNumber());
      isUpdated = true;
    }
    
    // Update new fields if they differ
    if ((operatorRepresentation.getDateOfBirth() != null && 
         !operatorRepresentation.getDateOfBirth().equals(operator.getDateOfBirth())) ||
        (operator.getDateOfBirth() != null && operatorRepresentation.getDateOfBirth() == null)) {
      operator.setDateOfBirth(operatorRepresentation.getDateOfBirth());
      isUpdated = true;
    }
    
    if ((operatorRepresentation.getDateOfJoining() != null && 
         !operatorRepresentation.getDateOfJoining().equals(operator.getDateOfJoining())) ||
        (operator.getDateOfJoining() != null && operatorRepresentation.getDateOfJoining() == null)) {
      operator.setDateOfJoining(operatorRepresentation.getDateOfJoining());
      isUpdated = true;
    }
    
    if ((operatorRepresentation.getDateOfLeaving() != null && 
         !operatorRepresentation.getDateOfLeaving().equals(operator.getDateOfLeaving())) ||
        (operator.getDateOfLeaving() != null && operatorRepresentation.getDateOfLeaving() == null)) {
      operator.setDateOfLeaving(operatorRepresentation.getDateOfLeaving());
      isUpdated = true;
    }
    
    if ((operatorRepresentation.getHourlyWages() != null && 
         !operatorRepresentation.getHourlyWages().equals(operator.getHourlyWages())) ||
        (operator.getHourlyWages() != null && operatorRepresentation.getHourlyWages() == null)) {
      operator.setHourlyWages(operatorRepresentation.getHourlyWages());
      isUpdated = true;
    }

    // Check if no updates were made; return existing representation
    if (!isUpdated) {
      log.info("No changes detected for Operator ID {}. Skipping update.", id);
      return operatorAssembler.dissemble(operator); // Return existing representation
    }

    // Save only if changes were detected
    if (OperatorType.MACHINING.equals(operatorRepresentation.getOperatorType())) {
      MachineOperator updatedMachineOperator = machineOperatorRepository.save((MachineOperator) operator);
      log.info("MachineOperator updated successfully with ID {}", updatedMachineOperator.getId());

      OperatorRepresentation updatedOperatorRepresentation = operatorAssembler.dissemble(updatedMachineOperator);
      updatedOperatorRepresentation.setOperatorType(OperatorType.MACHINING);
      return updatedOperatorRepresentation;
    }

    return null;
  }

  private Operator getOperatorByIdAndTenantId(Long id, Long tenantId) {
    return operatorRepository.findByIdAndTenantId(id, tenantId)
        .orElseThrow(() -> new RuntimeException("Operator not found for the given tenant!"));
  }

  // is operator exists for the given tenant
  public boolean isOperatorExistsForTenant(Long operatorId, Long tenantId) {
    return operatorRepository.existsByIdAndTenantId(operatorId, tenantId);
  }

  public List<MachineOperatorRepresentation> searchOperators(Long tenantId, String searchType, String searchQuery) {
    List<Operator> operators;

    switch (searchType.toLowerCase()) {
      case "name":
        operators = operatorRepository.findByFullNameContainingIgnoreCaseAndTenantId(searchQuery, tenantId);
        break;
      case "aadhaar":
        operators = operatorRepository.findByAadhaarNumberAndTenantId(searchQuery, tenantId);
        break;
      default:
        throw new IllegalArgumentException("Invalid search type. Must be 'name' or 'aadhaar'.");
    }

    return operators.stream()
        .map(this::mapToRepresentation)
        .collect(Collectors.toList());
  }

  private MachineOperatorRepresentation mapToRepresentation(Operator operator) {
    return machineOperatorAssembler.dissemble((MachineOperator) operator);
  }

  public void deleteOperator(Long operatorId, Long tenantId, java.time.LocalDate dateOfLeaving) {
    // Validate tenant exists
    Tenant tenant = tenantService.getTenantById(tenantId); // This will throw exception if tenant doesn't exist

    // Get operator and validate it exists for the given tenant
    MachineOperator operator = (MachineOperator) getOperatorByIdAndTenantId(operatorId, tenantId);
    if (operator == null) {
      log.error("Operator not found with id {} for tenant {}", operatorId, tenantId);
      throw new OperatorNotFoundException("Operator not found with id=" + operatorId);
    }

    // Check if operator is associated with any future DailyMachiningBatch
    LocalDateTime now = LocalDateTime.now();
    boolean hasFutureBatches = operator.getDailyMachiningBatches().stream()
        .anyMatch(batch -> !batch.isDeleted() &&
                           batch.getStartDateTime().isAfter(now) &&
                           batch.getEndDateTime().isAfter(now));

    if (hasFutureBatches) {
      log.error("Cannot delete operator {} as they are assigned to future machining batches", operatorId);
      throw new IllegalStateException("Cannot delete operator as they are assigned to future machining batches.");
    }

    // Set date of leaving if provided, otherwise use current date
    if (dateOfLeaving != null) {
      operator.setDateOfLeaving(dateOfLeaving);
    } else {
      operator.setDateOfLeaving(java.time.LocalDate.now());
    }

    // Perform soft delete
    operator.setDeleted(true);
    operator.setDeletedAt(now);
    operator.updateTenant(tenant);
    operatorRepository.save(operator);

    log.info("Operator {} successfully deleted with date of leaving: {}", operatorId, operator.getDateOfLeaving());
  }

  // Keep old method for backward compatibility
  public void deleteOperator(Long operatorId, Long tenantId) {
    deleteOperator(operatorId, tenantId, null);
  }

  public Page<OperatorPerformanceRepresentation> getOperatorsPerformanceForPeriod(
      Long tenantId,
      LocalDateTime startDate,
      LocalDateTime endDate,
      PageRequest pageRequest) {

    // First, get all operators and their batch data
    List<MachineOperator> allOperators = operatorRepository.findAllByTenantIdAndDeletedFalse(tenantId);
    
    // Calculate performance for all operators and sort
    List<OperatorPerformanceRepresentation> allPerformances = allOperators.stream()
        .map(operator -> {
            List<DailyMachiningBatch> batches = dailyMachiningBatchRepository
                .findByMachineOperatorAndStartDateTimeBetween(operator, startDate, endDate);
            return buildOperatorPerformance(operator, batches, startDate, endDate);
        })
        .sorted(Comparator.comparingInt(OperatorPerformanceRepresentation::getTotalPiecesCompleted))
        .collect(Collectors.toList());

    // Manual pagination
    int start = (int) pageRequest.getOffset();
    int end = Math.min((start + pageRequest.getPageSize()), allPerformances.size());
    
    // Create a new page with the properly sorted and paginated content
    return new PageImpl<>(
        allPerformances.subList(start, end),
        pageRequest,
        allPerformances.size()
    );
  }

  private OperatorPerformanceRepresentation buildOperatorPerformance(
      MachineOperator operator,
      List<DailyMachiningBatch> batches,
      LocalDateTime startPeriod,
      LocalDateTime endPeriod) {

    int totalBatches = batches.size();
    int totalCompleted = batches.stream().mapToInt(DailyMachiningBatch::getCompletedPiecesCount).sum();
    int totalRejected = batches.stream().mapToInt(DailyMachiningBatch::getRejectedPiecesCount).sum();
    int totalReworked = batches.stream().mapToInt(DailyMachiningBatch::getReworkPiecesCount).sum();
    int totalPieces = totalCompleted + totalRejected + totalReworked;

    // Calculate rates with rounding
    double completionRate = totalPieces > 0 ?
        Math.round((totalCompleted * 100.0) / totalPieces * 100.0) / 100.0 : 0;
    double rejectionRate = totalPieces > 0 ?
        Math.round((totalRejected * 100.0) / totalPieces * 100.0) / 100.0 : 0;
    double reworkRate = totalPieces > 0 ?
        Math.round((totalReworked * 100.0) / totalPieces * 100.0) / 100.0 : 0;

    // Calculate time-based metrics
    long totalWorkingHours = batches.stream()
        .mapToLong(batch ->
            Duration.between(batch.getStartDateTime(), batch.getEndDateTime()).toHours())
        .sum();

    // Calculate average production rate with rounding
    double avgProductionRate = totalWorkingHours > 0 ?
        Math.round((double) totalCompleted / totalWorkingHours * 100.0) / 100.0 : 0;

    // Calculate average pieces per batch with rounding
    double avgPiecesPerBatch = totalBatches > 0 ?
        Math.round((double) totalCompleted / totalBatches * 100.0) / 100.0 : 0;

    // Calculate total wages for the period based on hourly wages
    BigDecimal totalWages = BigDecimal.ZERO;
    if (operator.getHourlyWages() != null) {
        totalWages = operator.getHourlyWages().multiply(BigDecimal.valueOf(totalWorkingHours));
    }

    // Get latest batch status
    String currentStatus = batches.stream()
        .max(Comparator.comparing(DailyMachiningBatch::getEndDateTime))
        .map(batch -> batch.getDailyMachiningBatchStatus().name())
        .orElse("NO_ACTIVITY");

    LocalDateTime lastActive = batches.stream()
        .map(DailyMachiningBatch::getEndDateTime)
        .max(LocalDateTime::compareTo)
        .orElse(null);

    return OperatorPerformanceRepresentation.builder()
        .operatorId(operator.getId())
        .fullName(operator.getFullName())
        .startPeriod(startPeriod)
        .endPeriod(endPeriod)
        .totalBatchesCompleted(totalBatches)
        .totalPiecesCompleted(totalCompleted)
        .totalPiecesRejected(totalRejected)
        .totalPiecesReworked(totalReworked)
        .completionRate(completionRate)
        .rejectionRate(rejectionRate)
        .reworkRate(reworkRate)
        .averagePiecesPerBatch(avgPiecesPerBatch)
        .totalWorkingHours(totalWorkingHours)
        .averageProductionRatePerHour(avgProductionRate)
        .hourlyWages(operator.getHourlyWages())
        .totalWages(totalWages)
        .currentBatchStatus(currentStatus)
        .lastActive(lastActive)
        .build();
  }

  public OperatorPerformanceRepresentation getOperatorPerformanceForPeriod(
      Long tenantId,
      Long operatorId,
      LocalDateTime startDate,
      LocalDateTime endDate) {

      // Get the operator and validate
      MachineOperator operator = (MachineOperator) getOperatorByIdAndTenantId(operatorId, tenantId);
      if (operator == null) {
          throw new OperatorNotFoundException("Operator not found with id=" + operatorId);
      }

      // Get batches for the period
      List<DailyMachiningBatch> batches = dailyMachiningBatchRepository
          .findByMachineOperatorAndStartDateTimeBetween(operator, startDate, endDate);

      // Build and return performance metrics
      return buildOperatorPerformance(operator, batches, startDate, endDate);
  }
}
