package com.jangid.forging_process_management_service.service.machining;

import com.jangid.forging_process_management_service.assemblers.heating.ProcessedItemHeatTreatmentBatchAssembler;
import com.jangid.forging_process_management_service.assemblers.machining.DailyMachiningBatchAssembler;
import com.jangid.forging_process_management_service.assemblers.machining.MachiningBatchAssembler;
import com.jangid.forging_process_management_service.entities.heating.ProcessedItemHeatTreatmentBatch;
import com.jangid.forging_process_management_service.entities.machining.DailyMachiningBatch;
import com.jangid.forging_process_management_service.entities.machining.MachineSet;
import com.jangid.forging_process_management_service.entities.machining.MachiningBatch;
import com.jangid.forging_process_management_service.entities.machining.ProcessedItemMachiningBatch;
import com.jangid.forging_process_management_service.entities.product.ItemStatus;
import com.jangid.forging_process_management_service.entitiesRepresentation.heating.ProcessedItemHeatTreatmentBatchRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.machining.DailyMachiningBatchRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.machining.MachiningBatchRepresentation;
import com.jangid.forging_process_management_service.exception.ResourceNotFoundException;
import com.jangid.forging_process_management_service.exception.machining.MachiningBatchNotFoundException;
import com.jangid.forging_process_management_service.repositories.machining.MachiningBatchRepository;
import com.jangid.forging_process_management_service.service.TenantService;
import com.jangid.forging_process_management_service.service.heating.ProcessedItemHeatTreatmentBatchService;
import com.jangid.forging_process_management_service.utils.ConvertorUtils;
import com.jangid.forging_process_management_service.utils.MachiningBatchUtil;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MachiningBatchService {

  @Autowired
  private MachiningBatchRepository machiningBatchRepository;

  @Autowired
  private TenantService tenantService;

  @Autowired
  private MachineSetService machineSetService;

  @Autowired
  private MachiningBatchAssembler machiningBatchAssembler;
  @Autowired
  private DailyMachiningBatchAssembler dailyMachiningBatchDetailAssembler;
  @Autowired
  private ProcessedItemHeatTreatmentBatchService processedItemHeatTreatmentBatchService;

  @Autowired
  private ProcessedItemHeatTreatmentBatchAssembler processedItemHeatTreatmentBatchAssembler;

  @Transactional
  public MachiningBatchRepresentation applyMachiningBatch(long tenantId, long machineSetId, MachiningBatchRepresentation representation) {
    tenantService.validateTenantExists(tenantId);
    MachineSet machineSet = getMachineSetUsingTenantIdAndMachineSetId(tenantId, machineSetId);

    if (!MachineSet.MachineSetStatus.MACHINING_NOT_APPLIED.equals(machineSet.getMachineSetStatus())) {
      log.error("MachineSet={} is in a status={}, which is not correct for applying machining batch", machineSetId, machineSet.getMachineSetStatus().name());
      throw new RuntimeException("Cannot apply a new machining batch on this machine set as it is in an incorrect status.");
    }

    if (isMachiningBatchAppliedOnMachineSet(machineSetId)) {
      log.error("MachineSet={} already has a machining batch. Cannot apply a new machining batch.", machineSetId);
      throw new RuntimeException("MachineSet already has a machining batch.");
    }

    ProcessedItemHeatTreatmentBatchRepresentation heatTreatmentBatchRep = representation.getProcessedItemHeatTreatmentBatch();
    ProcessedItemHeatTreatmentBatch heatTreatmentBatch = processedItemHeatTreatmentBatchService.getProcessedItemHeatTreatmentBatchById(heatTreatmentBatchRep.getId());

    MachiningBatch machiningBatch = machiningBatchAssembler.createAssemble(representation);
    machiningBatch.setProcessedItemHeatTreatmentBatch(heatTreatmentBatch);
    machiningBatch.setMachineSet(machineSet);
    machiningBatch.setMachiningBatchType(MachiningBatch.MachiningBatchType.FRESH);

    ProcessedItemMachiningBatch processedItemMachiningBatch = machiningBatch.getProcessedItemMachiningBatch();

    if (processedItemMachiningBatch.getMachiningBatchPiecesCount() > heatTreatmentBatch.getAvailableMachiningBatchPiecesCount()) {
      throw new IllegalArgumentException("Machining batch pieces count exceeds available pieces count.");
    }

    processedItemMachiningBatch.setProcessedItem(heatTreatmentBatch.getProcessedItem());
    processedItemMachiningBatch.setMachiningBatch(machiningBatch);

    // Deduct the pieces from the heat treatment batch
    heatTreatmentBatch.setAvailableMachiningBatchPiecesCount(
        heatTreatmentBatch.getAvailableMachiningBatchPiecesCount() - processedItemMachiningBatch.getMachiningBatchPiecesCount()
    );
    processedItemMachiningBatch.setAvailableMachiningBatchPiecesCount(processedItemMachiningBatch.getMachiningBatchPiecesCount());

    MachiningBatch createdMachiningBatch = machiningBatchRepository.save(machiningBatch);
    machineSetService.updateMachineSetStatus(machineSet, MachineSet.MachineSetStatus.MACHINING_APPLIED);
    processedItemHeatTreatmentBatchService.save(heatTreatmentBatch);

    MachiningBatchRepresentation machiningBatchRepresentation = machiningBatchAssembler.dissemble(createdMachiningBatch);
    machiningBatchRepresentation.setProcessedItemHeatTreatmentBatch(processedItemHeatTreatmentBatchAssembler.dissemble(heatTreatmentBatch));
    return machiningBatchRepresentation;
  }

  public MachineSet getMachineSetUsingTenantIdAndMachineSetId(long tenantId, long machineSetId) {
    boolean isMachineSetOfTenantExists = machineSetService.isMachineSetExistsUsingTenantIdAndMachineSetId(tenantId, machineSetId);
    if (!isMachineSetOfTenantExists) {
      log.error("MachineSet={} for the tenant={} does not exist!", machineSetId, tenantId);
      throw new ResourceNotFoundException("MachineSet for the tenant does not exist!");
    }
    return machineSetService.getMachineSetUsingTenantIdAndMachineSetId(tenantId, machineSetId);
  }

  public boolean isMachiningBatchAppliedOnMachineSet(long machineSetId) {
    Optional<MachiningBatch> machiningBatchOptional = machiningBatchRepository.findAppliedMachiningBatchOnMachineSet(machineSetId);
    if (machiningBatchOptional.isPresent()) {
      log.info("Machining Batch={} already applied on machineSetId={}", machiningBatchOptional.get().getId(), machineSetId);
      return true;
    }
    return false;
  }

  @Transactional
  public MachiningBatchRepresentation startMachiningBatch(long tenantId, long machineSetId, long machiningBatchId, String startAt) {
    tenantService.validateTenantExists(tenantId);
    MachineSet machineSet = getMachineSetUsingTenantIdAndMachineSetId(tenantId, machineSetId);

    if (!MachineSet.MachineSetStatus.MACHINING_APPLIED.equals(machineSet.getMachineSetStatus())) {
      log.error("MachineSet={} is in a status={}, which is not correct for starting machining batch", machineSetId, machineSet.getMachineSetStatus().name());
      throw new RuntimeException("Cannot start a new machining batch on this machine set as machineSet " + machineSetId + " having not correct status " + machineSet.getMachineSetStatus().name());
    }

    boolean isMachineBatchAppliedOnMachineSet = isMachiningBatchAppliedOnMachineSet(machineSetId);

    if (!isMachineBatchAppliedOnMachineSet) {
      log.error("MachineSet={} does not have a machining batch set. Can not start machining batch on this machining set", machineSetId);
      throw new RuntimeException("Machining batch does not exists for MachineSet!");
    }

    MachiningBatch existingMachiningBatch = getMachiningBatchById(machiningBatchId);
    String batchNumber = existingMachiningBatch.getMachiningBatchNumber();

    if (existingMachiningBatch.getStartAt() != null) {
      log.error("The machiningBatch={} having batch number={} has already been started!", machiningBatchId, existingMachiningBatch.getMachiningBatchNumber());
      throw new RuntimeException("machiningBatch=" + machiningBatchId + " , batch number=" + batchNumber + "has already been started!");
    }

    if (!MachiningBatch.MachiningBatchStatus.IDLE.equals(existingMachiningBatch.getMachiningBatchStatus())) {
      log.error("The machiningBatch={} having batch number={} is not in IDLE status to start it!", machiningBatchId, existingMachiningBatch.getMachiningBatchNumber());
      throw new RuntimeException("MachiningBatch=" + machiningBatchId + " ,  batch number=" + batchNumber + "Not in IDLE status to start it!");
    }

    existingMachiningBatch.setMachiningBatchStatus(MachiningBatch.MachiningBatchStatus.IN_PROGRESS);
    existingMachiningBatch.setStartAt(ConvertorUtils.convertStringToLocalDateTime(startAt));

    existingMachiningBatch.getProcessedItemMachiningBatch().setItemStatus(ItemStatus.MACHINING_IN_PROGRESS);

    MachiningBatch startedMachiningBatch = machiningBatchRepository.save(existingMachiningBatch);

    machineSetService.updateMachineSetStatus(machineSet, MachineSet.MachineSetStatus.MACHINING_IN_PROGRESS);

    return machiningBatchAssembler.dissemble(startedMachiningBatch);
  }

  @Transactional
  public MachiningBatchRepresentation endMachiningBatch(long tenantId, long machineSetId, long machiningBatchId, MachiningBatchRepresentation machiningBatchRepresentation) {
    tenantService.validateTenantExists(tenantId);

    MachineSet machineSet = getMachineSetUsingTenantIdAndMachineSetId(tenantId, machineSetId);

    if (!MachineSet.MachineSetStatus.MACHINING_IN_PROGRESS.equals(machineSet.getMachineSetStatus())) {
      log.error("MachineSet={} is in a status={}, which is not correct for ending machining batch", machineSetId, machineSet.getMachineSetStatus().name());
      throw new RuntimeException("Cannot end this machining batch on this machine set as machineSet " + machineSetId + " having not correct status " + machineSet.getMachineSetStatus().name());
    }

    boolean isMachineBatchAppliedOnMachineSet = isMachiningBatchAppliedOnMachineSet(machineSetId);

    if (!isMachineBatchAppliedOnMachineSet) {
      log.error("MachineSet={} does not have a machining batch set. Can not end machining batch on this machine set", machineSetId);
      throw new MachiningBatchNotFoundException("MachiningBatch does not exists for machiningBatchId=" + machiningBatchId);
    }

    MachiningBatch existingMachiningBatch = getMachiningBatchById(machiningBatchId);
    String batchNumber = existingMachiningBatch.getMachiningBatchNumber();

    if (existingMachiningBatch.getEndAt() != null) {
      log.error("The machiningBatch={} having batch number={} has already been ended!", machiningBatchId, existingMachiningBatch.getMachiningBatchNumber());
      throw new RuntimeException("MachiningBatch=" + machiningBatchId + " , batch number=" + batchNumber + "has already been ended!");
    }

    LocalDateTime endAt = ConvertorUtils.convertStringToLocalDateTime(machiningBatchRepresentation.getEndAt());

    if (existingMachiningBatch.getStartAt().compareTo(endAt) >= 0) {
      log.error("The machiningBatch={} having batch number={} end time is before or equal to start time!", machiningBatchId, existingMachiningBatch.getMachiningBatchNumber());
      throw new RuntimeException("MachiningBatch=" + machiningBatchId + " , batch number=" + batchNumber + " end time is before or equal to start time!");
    }

    DailyMachiningBatch latestDailyMachiningBatch = MachiningBatchUtil.getLatestDailyMachiningBatch(existingMachiningBatch);

    if (latestDailyMachiningBatch != null && latestDailyMachiningBatch.getEndDateTime() != null && latestDailyMachiningBatch.getEndDateTime().compareTo(endAt) >= 0) {
      log.error("The end time of machining batch={} having batch number={} is before the last DailyMachiningBatch's end time!", machiningBatchId, existingMachiningBatch.getMachiningBatchNumber());
      throw new RuntimeException(
          "The end time of machining batch=" + machiningBatchId + " having batch number=" + batchNumber + "  is before the last DailyMachiningBatch's end time!");
    }
    ProcessedItemHeatTreatmentBatch processedItemHeatTreatmentBatch = existingMachiningBatch.getProcessedItemHeatTreatmentBatch();
    boolean isFullMachiningBatchCompleted = processedItemHeatTreatmentBatch.getAvailableMachiningBatchPiecesCount() == 0;

    ProcessedItemMachiningBatch processedItemMachiningBatch = existingMachiningBatch.getProcessedItemMachiningBatch();

    if (isFullMachiningBatchCompleted) {
        if (processedItemMachiningBatch.getReworkPiecesCount() == 0) {
          processedItemMachiningBatch.setItemStatus(ItemStatus.MACHINING_COMPLETED);
        } else {
          processedItemMachiningBatch.setItemStatus(ItemStatus.MACHINING_COMPLETED_WITH_REWORK);
        }
      } else {
        if (processedItemMachiningBatch.getReworkPiecesCount() == 0) {
          processedItemMachiningBatch.setItemStatus(ItemStatus.MACHINING_PARTIALLY_COMPLETED_WITHOUT_REWORK);
        } else {
          processedItemMachiningBatch.setItemStatus(ItemStatus.MACHINING_PARTIALLY_COMPLETED_WITH_REWORK);
        }
      }
    int appliedMachiningBatchPiecesCount = processedItemMachiningBatch.getMachiningBatchPiecesCount();
    int totalMachinePiecesCount = processedItemMachiningBatch.getActualMachiningBatchPiecesCount();
    int totalRejectedPiecesCount = processedItemMachiningBatch.getRejectMachiningBatchPiecesCount();
    int totalReworkPiecesCount = processedItemMachiningBatch.getReworkPiecesCount();

    if (totalMachinePiecesCount + totalRejectedPiecesCount + totalReworkPiecesCount < appliedMachiningBatchPiecesCount) {
      log.error("The total provided actual finished, reject, and rework machining piece counts for all daily batches is " +
                "less than the total applied machining pieces count for machining batch={} having batch number={}!",
                machiningBatchId, batchNumber);
      throw new RuntimeException("The total provided actual finished, reject, and rework machining piece counts for all daily batches is " +
                                 "less than the total applied machining pieces count!");

    }

    existingMachiningBatch.setEndAt(endAt);
    existingMachiningBatch.setMachiningBatchStatus(MachiningBatch.MachiningBatchStatus.COMPLETED);

    MachiningBatch completedMachiningBatch = machiningBatchRepository.save(existingMachiningBatch);
    machineSetService.updateMachineSetStatus(machineSet, MachineSet.MachineSetStatus.MACHINING_NOT_APPLIED);

    return machiningBatchAssembler.dissemble(completedMachiningBatch);
  }

  public MachiningBatchRepresentation dailyMachiningBatchUpdate(long tenantId, long machineSetId, long machiningBatchId,
                                                                 DailyMachiningBatchRepresentation dailyMachiningBatchRepresentation) {
    tenantService.validateTenantExists(tenantId);

    MachineSet machineSet = getMachineSetUsingTenantIdAndMachineSetId(tenantId, machineSetId);

    if (!MachineSet.MachineSetStatus.MACHINING_IN_PROGRESS.equals(machineSet.getMachineSetStatus())) {
      log.error("MachineSet={} is in a status={}, which is not correct for applying machining batch", machineSetId, machineSet.getMachineSetStatus().name());
      throw new RuntimeException("Cannot apply a new machining batch on this machine set as machineSet " + machineSetId + " having not correct status " + machineSet.getMachineSetStatus().name());
    }

    boolean isMachineBatchAppliedOnMachineSet = isMachiningBatchAppliedOnMachineSet(machineSetId);

    if (!isMachineBatchAppliedOnMachineSet) {
      log.error("MachineSet={} does not have a machining batch set. Can not end machining batch on this machine set", machineSetId);
      throw new MachiningBatchNotFoundException("MachiningBatch does not exists for machiningBatchId=" + machiningBatchId);
    }

    MachiningBatch existingMachiningBatch = getMachiningBatchById(machiningBatchId);
    String batchNumber = existingMachiningBatch.getMachiningBatchNumber();

    LocalDateTime dailyMachiningBatchStartDateTime = ConvertorUtils.convertStringToLocalDateTime(dailyMachiningBatchRepresentation.getStartDateTime());
    LocalDateTime dailyMachiningBatchEndDateTime = ConvertorUtils.convertStringToLocalDateTime(dailyMachiningBatchRepresentation.getEndDateTime());
    LocalDate operationDate = ConvertorUtils.convertStringToLocalDate(dailyMachiningBatchRepresentation.getOperationDate());

    if (existingMachiningBatch.getStartAt().compareTo(dailyMachiningBatchStartDateTime) > 0) {
      log.error("The dailyMachiningBatchStartDateTime provided is before start time of machining batch={} having batch number={} !", machiningBatchId, batchNumber);
      throw new RuntimeException("MachiningBatch=" + machiningBatchId + " , batch number=" + batchNumber + " start time is after the dailyMachiningBatchStartDateTime!");
    }

    DailyMachiningBatch latestDailyMachiningBatch = MachiningBatchUtil.getLatestDailyMachiningBatch(existingMachiningBatch);

    if (operationDate.compareTo(existingMachiningBatch.getStartAt().toLocalDate()) < 0) {
      log.error("The operationDate provided is before startAt of machiningBatch={}!", machiningBatchId);
      throw new RuntimeException("OperationDate must be greater than or equal to startAt of machining batch.");
    }

    if (latestDailyMachiningBatch != null) {
      if (operationDate.compareTo(latestDailyMachiningBatch.getOperationDate()) <= 0) {
        log.error("The operationDate provided is not greater than the last DailyMachiningBatch operation date for machiningBatchId={}!", machiningBatchId);
        throw new RuntimeException("OperationDate must be greater than the last daily machining batch operation date.");
      }

      if (dailyMachiningBatchStartDateTime.compareTo(latestDailyMachiningBatch.getEndDateTime()) <= 0) {
        log.error("The dailyMachiningBatchStartDateTime provided is not greater than last DailyMachiningBatch endDateTime for machiningBatchId={}!", machiningBatchId);
        throw new RuntimeException("DailyMachiningBatch start dateTime must be greater than the last daily machining batch endDateTime.");
      }

      if (dailyMachiningBatchEndDateTime.compareTo(latestDailyMachiningBatch.getEndDateTime()) <= 0) {
        log.error("The dailyMachiningBatchEndDateTime provided is not greater than last DailyMachiningBatch endDateTime for machiningBatchId={}!", machiningBatchId);
        throw new RuntimeException("DailyMachiningBatch end dateTime must be greater than the last daily machining batch endDateTime.");
      }
    }

    if (dailyMachiningBatchStartDateTime.compareTo(dailyMachiningBatchEndDateTime) >= 0) {
      log.error("The dailyMachiningBatch start dateTime is after or equal to end dateTime for machiningBatchId={} having machiningBatchNumber={}!", machiningBatchId, batchNumber);
      throw new RuntimeException("The dailyMachiningBatch start dateTime must be less than the end dateTime.");
    }

    DailyMachiningBatch dailyMachiningBatch = dailyMachiningBatchDetailAssembler.createAssemble(dailyMachiningBatchRepresentation);
    dailyMachiningBatch.setDailyMachiningBatchStatus(DailyMachiningBatch.DailyMachiningBatchStatus.COMPLETED);

    if (existingMachiningBatch.getDailyMachiningBatch() == null) {
      existingMachiningBatch.setDailyMachiningBatch(new ArrayList<>());
    }
    existingMachiningBatch.getDailyMachiningBatch().add(dailyMachiningBatch);
    dailyMachiningBatch.setMachiningBatch(existingMachiningBatch);

    int dailyActualFinishedMachiningPiecesCount = dailyMachiningBatchRepresentation.getCompletedPiecesCount();

    int dailyRejectedMachiningPiecesCount = dailyMachiningBatchRepresentation.getRejectedPiecesCount();
    int dailyReworkMachiningPiecesCount = dailyMachiningBatchRepresentation.getReworkPiecesCount();

    ProcessedItemMachiningBatch processedItemMachiningBatch = existingMachiningBatch.getProcessedItemMachiningBatch();
    int machiningBatchPiecesCount = processedItemMachiningBatch.getMachiningBatchPiecesCount();

    // Safely retrieve counts, defaulting to 0 if null
    int actualMachiningBatchPiecesCount = processedItemMachiningBatch.getActualMachiningBatchPiecesCount() != null
                                          ? processedItemMachiningBatch.getActualMachiningBatchPiecesCount()
                                          : 0;
    int rejectMachiningBatchPiecesCount = processedItemMachiningBatch.getRejectMachiningBatchPiecesCount() != null
                                          ? processedItemMachiningBatch.getRejectMachiningBatchPiecesCount()
                                          : 0;
    int reworkPiecesCount = processedItemMachiningBatch.getReworkPiecesCount() != null
                            ? processedItemMachiningBatch.getReworkPiecesCount()
                            : 0;

    int availableMachiningBatchPiecesCount = processedItemMachiningBatch.getAvailableMachiningBatchPiecesCount() != null ? processedItemMachiningBatch.getAvailableMachiningBatchPiecesCount() : 0;

    // Perform validation checks
    if (actualMachiningBatchPiecesCount + dailyActualFinishedMachiningPiecesCount
        + rejectMachiningBatchPiecesCount + dailyRejectedMachiningPiecesCount
        + reworkPiecesCount + dailyReworkMachiningPiecesCount > machiningBatchPiecesCount) {
      log.error("The provided daily actual finished, reject, and rework machining piece counts exceeds " +
                "the applied machine count for machining batch={} having batch number={}!",
                machiningBatchId, batchNumber);
      throw new RuntimeException("The provided daily actual finished, reject, and rework machining piece counts " +
                                 "exceeds the applied machine count!");
    }

    if (actualMachiningBatchPiecesCount + dailyActualFinishedMachiningPiecesCount > machiningBatchPiecesCount) {
      log.error("The provided daily actual finished machining piece counts exceeds " +
                "the applied machine count for machining batch={} having batch number={}!",
                machiningBatchId, batchNumber);
      throw new RuntimeException("The provided daily actual finished machining piece counts exceeds " +
                                 "the applied machine count!");
    }

    // Update counts, safely handling nulls
    processedItemMachiningBatch.setActualMachiningBatchPiecesCount(
        actualMachiningBatchPiecesCount + dailyActualFinishedMachiningPiecesCount
    );
    processedItemMachiningBatch.setRejectMachiningBatchPiecesCount(
        rejectMachiningBatchPiecesCount + dailyRejectedMachiningPiecesCount
    );
    processedItemMachiningBatch.setReworkPiecesCount(
        reworkPiecesCount + dailyReworkMachiningPiecesCount
    );
    processedItemMachiningBatch.setInitialInspectionBatchPiecesCount(
        (processedItemMachiningBatch.getInitialInspectionBatchPiecesCount() != null
         ? processedItemMachiningBatch.getInitialInspectionBatchPiecesCount()
         : 0) + dailyActualFinishedMachiningPiecesCount
    );
    processedItemMachiningBatch.setAvailableInspectionBatchPiecesCount(
        (processedItemMachiningBatch.getAvailableInspectionBatchPiecesCount() != null
         ? processedItemMachiningBatch.getAvailableInspectionBatchPiecesCount()
         : 0) + dailyActualFinishedMachiningPiecesCount
    );

    processedItemMachiningBatch.setAvailableMachiningBatchPiecesCount(
        availableMachiningBatchPiecesCount - (dailyActualFinishedMachiningPiecesCount + dailyRejectedMachiningPiecesCount + dailyReworkMachiningPiecesCount));

    MachiningBatch updatedMachiningBatch = machiningBatchRepository.save(existingMachiningBatch);
    return machiningBatchAssembler.dissemble(updatedMachiningBatch);
  }



  public MachiningBatch getMachiningBatchById(long machiningBatchId) {
    Optional<MachiningBatch> machiningBatchOptional = machiningBatchRepository.findByIdAndDeletedFalse(machiningBatchId);
    if (machiningBatchOptional.isEmpty()) {
      log.error("MachiningBatch does not exists for machiningBatchId={}", machiningBatchId);
      throw new MachiningBatchNotFoundException("MachiningBatch does not exists for machiningBatchId=" + machiningBatchId);
    }
    return machiningBatchOptional.get();
  }

  public MachiningBatch getAppliedMachiningBatchByMachineSet(long machineSetId) {
    Optional<MachiningBatch> machiningBatchOptional = machiningBatchRepository.findAppliedMachiningBatchOnMachineSet(machineSetId);
    if (machiningBatchOptional.isEmpty()) {
      log.error("MachiningBatch does not exists for machineSetId={}", machineSetId);
//      throw new RuntimeException("MachiningBatch does not exists for machineSet!");
      return MachiningBatch.builder().build();
    }
    return machiningBatchOptional.get();
  }

  // getAllMachiningBatchByTenantId

  public Page<MachiningBatchRepresentation> getAllMachiningBatchByTenantId(long tenantId, int page, int size) {
    Pageable pageable = PageRequest.of(page, size);

    List<Long> machineSetIds = machineSetService.getAllMachineSetsOfTenant(tenantId)
        .stream()
        .map(MachineSet::getId)
        .collect(Collectors.toList());

    Page<MachiningBatch> machiningBatchPage = machiningBatchRepository
        .findByMachineSetIdInAndDeletedFalseOrderByCreatedAtDesc(machineSetIds, pageable);

    return machiningBatchPage.map(machiningBatchAssembler::dissemble);
  }
}
