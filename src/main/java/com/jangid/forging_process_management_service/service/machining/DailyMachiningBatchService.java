package com.jangid.forging_process_management_service.service.machining;

import com.jangid.forging_process_management_service.entities.machining.DailyMachiningBatch;
import com.jangid.forging_process_management_service.entities.operator.Operator;
import com.jangid.forging_process_management_service.entitiesRepresentation.operator.OperatorMachiningDetailsRepresentation;
import com.jangid.forging_process_management_service.exception.operator.MachineOperatorNotFoundException;
import com.jangid.forging_process_management_service.repositories.machining.DailyMachiningBatchRepository;
import com.jangid.forging_process_management_service.service.operator.OperatorService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class DailyMachiningBatchService {

  @Autowired
  private DailyMachiningBatchRepository dailyMachiningBatchRepository;

  @Autowired
  private OperatorService operatorService;

  public DailyMachiningBatch getDailyMachiningBatchById(long id) {
    Optional<DailyMachiningBatch> dailyMachiningBatchOptional = dailyMachiningBatchRepository.findByIdAndDeletedFalse(id);

    if (dailyMachiningBatchOptional.isEmpty()) {
      log.error("DailyMachiningBatch does not exists for id={}", id);
      throw new MachineOperatorNotFoundException("DailyMachiningBatch does not exists for id=" + id);
    }
    return dailyMachiningBatchOptional.get();
  }

  public boolean existsOverlappingBatchForOperator(long operatorId, LocalDateTime startTime, LocalDateTime endTime){
    boolean existsOverlappingBatchForOperator = dailyMachiningBatchRepository.existsOverlappingBatchForOperator(operatorId, startTime, endTime);
    if(existsOverlappingBatchForOperator){
      List<DailyMachiningBatch> dmbs =  dailyMachiningBatchRepository.findOverlappingBatchesForOperator(operatorId, startTime, endTime);
      List<Long> batchIds = dmbs.stream()
          .map(DailyMachiningBatch::getId)
          .toList();

      log.error("There exists an overlap with dailyMachiningBatch ids={} between the startTime={} and endTime={} for the operator having id={}",
                batchIds, startTime, endTime, operatorId);
    }
    return existsOverlappingBatchForOperator;
  }

  public boolean existsOverlappingBatchForMachineSet(long machineSetId, LocalDateTime startTime, LocalDateTime endTime){
    boolean existsOverlappingBatchForMachineSet = dailyMachiningBatchRepository.existsOverlappingBatchForMachineSet(machineSetId, startTime, endTime);
    if(existsOverlappingBatchForMachineSet){
      List<DailyMachiningBatch> dmbs = dailyMachiningBatchRepository.findOverlappingBatchesForMachineSet(machineSetId, startTime, endTime);
      List<Long> batchIds = dmbs.stream()
          .map(DailyMachiningBatch::getId)
          .toList();

      log.error("There exists an overlap with dailyMachiningBatch ids={} between the startTime={} and endTime={} for the machineSet having id={}",
                batchIds, startTime, endTime, machineSetId);
    }
    return existsOverlappingBatchForMachineSet;
  }

  @Transactional
  public DailyMachiningBatch save(DailyMachiningBatch dailyMachiningBatch){
    return dailyMachiningBatchRepository.save(dailyMachiningBatch);
  }

  public boolean existsByDailyMachiningBatchNumberAndMachiningBatchIdAndDeletedFalse(String dailyMachiningBatchNumber, Long machiningBatchId) {
    return dailyMachiningBatchRepository.existsByDailyMachiningBatchNumberAndMachiningBatchIdAndDeletedFalse(
        dailyMachiningBatchNumber, machiningBatchId);
  }

  public OperatorMachiningDetailsRepresentation getMachiningDetailsForOperatorForPeriod(long tenantId, long operatorId, LocalDateTime startTime, LocalDateTime endTime) {
    boolean isExists = operatorService.isOperatorExistsForTenant(operatorId, tenantId);
    if (!isExists) {
      log.error("Operator with id={} does not exist for tenant with id={}", operatorId, tenantId);
      throw new MachineOperatorNotFoundException("Operator with id=" + operatorId + " does not exist for tenant with id=" + tenantId);
    }

    List<DailyMachiningBatch> batches = dailyMachiningBatchRepository.findOverlappingBatchesForOperator(operatorId, startTime, endTime);

    double totalMachiningHours = batches.stream()
        .mapToDouble(batch -> {
          LocalDateTime effectiveStart = batch.getStartDateTime().isBefore(startTime) ? startTime : batch.getStartDateTime();
          LocalDateTime effectiveEnd = batch.getEndDateTime().isAfter(endTime) ? endTime : batch.getEndDateTime();
          return Duration.between(effectiveStart, effectiveEnd).toMinutes() / 60.0; // Convert minutes to hours
        })
        .sum();

    int totalCompletedPieces = batches.stream()
        .mapToInt(DailyMachiningBatch::getCompletedPiecesCount)
        .sum();

    int totalRejectedPieces = batches.stream()
        .mapToInt(DailyMachiningBatch::getRejectedPiecesCount)
        .sum();

    int totalReworkPieces = batches.stream()
        .mapToInt(DailyMachiningBatch::getReworkPiecesCount)
        .sum();

    Operator operator = operatorService.getOperatorById(operatorId);

    return OperatorMachiningDetailsRepresentation.builder()
        .totalMachiningHours(totalMachiningHours)
        .totalCompletedPieces(totalCompletedPieces)
        .totalRejectedPieces(totalRejectedPieces)
        .totalReworkPieces(totalReworkPieces)
        .id(operatorId)
        .fullName(operator.getFullName())
        .aadhaarNumber(operator.getAadhaarNumber())
        .startTime(startTime.toString())
        .endTime(endTime.toString())
        .build();
  }


}
