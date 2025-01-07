package com.jangid.forging_process_management_service.service.machining;

import com.jangid.forging_process_management_service.assemblers.machining.DailyMachiningBatchDetailAssembler;
import com.jangid.forging_process_management_service.assemblers.machining.MachiningBatchAssembler;
import com.jangid.forging_process_management_service.entities.ProcessedItem;
import com.jangid.forging_process_management_service.entities.machining.DailyMachiningBatchDetail;
import com.jangid.forging_process_management_service.entities.machining.MachineSet;
import com.jangid.forging_process_management_service.entities.machining.MachiningBatch;
import com.jangid.forging_process_management_service.entities.product.ItemStatus;
import com.jangid.forging_process_management_service.entitiesRepresentation.machining.DailyMachiningBatchDetailRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.machining.MachiningBatchRepresentation;
import com.jangid.forging_process_management_service.exception.ResourceNotFoundException;
import com.jangid.forging_process_management_service.exception.machining.MachiningBatchNotFoundException;
import com.jangid.forging_process_management_service.repositories.machining.MachiningBatchRepository;
import com.jangid.forging_process_management_service.service.ProcessedItemService;
import com.jangid.forging_process_management_service.service.TenantService;
import com.jangid.forging_process_management_service.utils.ConvertorUtils;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
  private DailyMachiningBatchDetailAssembler dailyMachiningBatchDetailAssembler;
  @Autowired
  private ProcessedItemService processedItemService;

  @Transactional
  public MachiningBatchRepresentation applyMachiningBatch(long tenantId, long machineSetId, MachiningBatchRepresentation representation) {
    tenantService.validateTenantExists(tenantId);
    MachineSet machineSet = getMachineSetUsingTenantIdAndMachineSetId(tenantId, machineSetId);

    if (!MachineSet.MachineSetStatus.MACHINING_NOT_APPLIED.equals(machineSet.getMachineSetStatus())) {
      log.error("MachineSet={} is in a status={}, which is not correct for applying machining batch", machineSetId, machineSet.getMachineSetStatus().name());
      throw new RuntimeException("Cannot apply a new machining batch on this machine set as machineSet " + machineSetId + " having not correct status " + machineSet.getMachineSetStatus().name());
    }

    boolean isMachineBatchAppliedOnMachineSet = isMachiningBatchAppliedOnMachineSet(machineSetId);

    if (isMachineBatchAppliedOnMachineSet) {
      log.error("MachineSet={} is already having a machining batch set. Cannot apply a new machining batch on this machining set", machineSetId);
      throw new RuntimeException("Cannot apply a new machining batch on this machine set as machineSet " + machineSetId + " is already occupied");
    }
    ProcessedItem processedItem = processedItemService.getProcessedItemById(representation.getProcessedItem().getId());
    if (processedItem.getItemStatus() != ItemStatus.HEAT_TREATMENT_PARTIALLY_COMPLETED &&
        processedItem.getItemStatus() != ItemStatus.HEAT_TREATMENT_COMPLETED &&
        processedItem.getItemStatus() != ItemStatus.MACHINING_NOT_STARTED &&
        processedItem.getItemStatus() != ItemStatus.MACHINING_IN_PROGRESS &&
        processedItem.getItemStatus() != ItemStatus.MACHINING_PARTIALLY_COMPLETED_WITHOUT_REWORK &&
        processedItem.getItemStatus() != ItemStatus.MACHINING_PARTIALLY_COMPLETED_WITH_REWORK
    ) {
      log.error("The processedItem={} is in ItemStatus={} which is not correct ItemStatus for machining.", processedItem.getId(), processedItem.getItemStatus());
      throw new RuntimeException("The processedItem is not in correct ItemStatus for machining. " + processedItem.getItemStatus() + " is not correct ItemStatus for machining.");
    }

    MachiningBatch machiningBatch = machiningBatchAssembler.createAssemble(representation);
    machiningBatch.setMachineSet(machineSet);
    machiningBatch.setAppliedMachiningBatchPiecesCount(Integer.valueOf(representation.getAppliedMachiningBatchPiecesCount()));
    machiningBatch.setMachiningBatchType(MachiningBatch.MachiningBatchType.FRESH);

    processedItem.setInitialMachiningBatchPiecesCount(processedItem.getActualHeatTreatBatchPiecesCount());
    processedItem.setAvailableMachiningBatchPiecesCount(processedItem.getActualHeatTreatBatchPiecesCount() - Integer.valueOf(representation.getAppliedMachiningBatchPiecesCount()));
    processedItem.setItemStatus(ItemStatus.MACHINING_NOT_STARTED);
    machiningBatch.setProcessedItem(processedItem);

    MachiningBatch createdMachiningBatch = machiningBatchRepository.save(machiningBatch);
    machineSetService.updateMachineSetStatus(machineSet, MachineSet.MachineSetStatus.MACHINING_APPLIED);
    return machiningBatchAssembler.dissemble(createdMachiningBatch);
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

    if (existingMachiningBatch.getStartAt() != null) {
      log.error("The machiningBatch={} having batch number={} has already been started!", machiningBatchId, existingMachiningBatch.getMachiningBatchNumber());
      throw new RuntimeException("machiningBatch=" + machiningBatchId + " , batch number=" + existingMachiningBatch.getMachiningBatchNumber() + "has already been started!");
    }

    if (!MachiningBatch.MachiningBatchStatus.IDLE.equals(existingMachiningBatch.getMachiningBatchStatus())) {
      log.error("The machiningBatch={} having batch number={} is not in IDLE status to start it!", machiningBatchId, existingMachiningBatch.getMachiningBatchNumber());
      throw new RuntimeException("MachiningBatch=" + machiningBatchId + " ,  batch number=" + existingMachiningBatch.getMachiningBatchNumber() + "Not in IDLE status to start it!");
    }

    existingMachiningBatch.setMachiningBatchStatus(MachiningBatch.MachiningBatchStatus.IN_PROGRESS);
    existingMachiningBatch.setStartAt(ConvertorUtils.convertStringToLocalDateTime(startAt));
    existingMachiningBatch.getProcessedItem().setItemStatus(ItemStatus.MACHINING_IN_PROGRESS);

    MachiningBatch startedMachiningBatch = machiningBatchRepository.save(existingMachiningBatch);

    machineSetService.updateMachineSetStatus(machineSet, MachineSet.MachineSetStatus.MACHINING_IN_PROGRESS);

    return machiningBatchAssembler.dissemble(startedMachiningBatch);
  }

  @Transactional
  public MachiningBatchRepresentation endMachiningBatch(long tenantId, long machineSetId, long machiningBatchId, MachiningBatchRepresentation representation) {
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

    if (existingMachiningBatch.getEndAt() != null) {
      log.error("The machiningBatch={} having batch number={} has already been ended!", machiningBatchId, existingMachiningBatch.getMachiningBatchNumber());
      throw new RuntimeException("MachiningBatch=" + machiningBatchId + " , batch number=" + existingMachiningBatch.getMachiningBatchNumber() + "has already been ended!");
    }

    LocalDateTime endAt = ConvertorUtils.convertStringToLocalDateTime(representation.getEndAt());
    if (existingMachiningBatch.getStartAt().compareTo(endAt) >= 0) {
      log.error("The machiningBatch={} having batch number={} end time is before or equal to start time!", machiningBatchId, existingMachiningBatch.getMachiningBatchNumber());
      throw new RuntimeException("MachiningBatch=" + machiningBatchId + " , batch number=" + existingMachiningBatch.getMachiningBatchNumber() + " end time is before or equal to start time!");
    }

    ProcessedItem existingMachiningBatchProcessedItem = existingMachiningBatch.getProcessedItem();

    int actualReworkedMachiningPieces = getActualReworkedMachiningPieces(representation.getDailyMachiningBatchDetail());

    if (existingMachiningBatchProcessedItem.getAvailableMachiningBatchPiecesCount() == 0) {
      if (actualReworkedMachiningPieces == 0) {
        existingMachiningBatchProcessedItem.setItemStatus(ItemStatus.MACHINING_COMPLETED);
//        existingMachiningBatchProcessedItem.setInitialReworkMachiningBatchPiecesCount(0);
//        existingMachiningBatchProcessedItem.setAvailableReworkMachiningBatchPiecesCount(0);
      } else {
        existingMachiningBatchProcessedItem.setItemStatus(ItemStatus.MACHINING_PARTIALLY_COMPLETED_WITH_REWORK);
//        existingMachiningBatchProcessedItem.setInitialMachiningBatchPiecesCount(actualReworkedMachiningPieces);
//        existingMachiningBatchProcessedItem.setAvailableReworkMachiningBatchPiecesCount(actualReworkedMachiningPieces);
//        existingMachiningBatchProcessedItem.setInitialReworkMachiningBatchPiecesCount(actualReworkedMachiningPieces);
//        existingMachiningBatchProcessedItem.setAvailableReworkMachiningBatchPiecesCount(actualReworkedMachiningPieces);
      }
    } else {
      if (actualReworkedMachiningPieces == 0) {
        existingMachiningBatchProcessedItem.setItemStatus(ItemStatus.MACHINING_PARTIALLY_COMPLETED_WITHOUT_REWORK);
      } else {
        existingMachiningBatchProcessedItem.setItemStatus(ItemStatus.MACHINING_PARTIALLY_COMPLETED_WITH_REWORK);
//        existingMachiningBatchProcessedItem.setInitialMachiningBatchPiecesCount(actualReworkedMachiningPieces);
//        existingMachiningBatchProcessedItem.setAvailableReworkMachiningBatchPiecesCount(actualReworkedMachiningPieces);
//        existingMachiningBatchProcessedItem.setInitialReworkMachiningBatchPiecesCount(actualReworkedMachiningPieces);
//        existingMachiningBatchProcessedItem.setAvailableReworkMachiningBatchPiecesCount(actualReworkedMachiningPieces);
      }
    }
    existingMachiningBatch.setProcessedItem(existingMachiningBatchProcessedItem);

//    int actualFinishedMachiningPieces = getActualFinishedMachiningPieces(representation.getDailyMachiningBatchDetail());
//    int actualRejectedMachiningPieces = getActualRejectedMachiningPieces(representation.getDailyMachiningBatchDetail());
//
//    existingMachiningBatch.setActualMachiningBatchPiecesCount(actualFinishedMachiningPieces);
//    existingMachiningBatch.setRejectMachiningBatchPiecesCount(actualRejectedMachiningPieces);
//    existingMachiningBatch.setReworkPiecesCount(actualReworkedMachiningPieces);

    existingMachiningBatch.setEndAt(endAt);
    existingMachiningBatch.setMachiningBatchStatus(MachiningBatch.MachiningBatchStatus.COMPLETED);

    MachiningBatch completedMachiningBatch = machiningBatchRepository.save(existingMachiningBatch);
    machineSetService.updateMachineSetStatus(machineSet, MachineSet.MachineSetStatus.MACHINING_NOT_APPLIED);

    return machiningBatchAssembler.dissemble(completedMachiningBatch);
  }

  public MachiningBatchRepresentation dailyMachiningBatchUpdate(long tenantId, long machineSetId, long machiningBatchId,
                                                                DailyMachiningBatchDetailRepresentation dailyMachiningBatchDetailRepresentation) {
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

    if (existingMachiningBatch.getDailyMachiningBatchDetail() == null) {
      existingMachiningBatch.setDailyMachiningBatchDetail(new ArrayList<>());
    }

    DailyMachiningBatchDetail dailyMachiningBatchDetail = dailyMachiningBatchDetailAssembler.createAssemble(dailyMachiningBatchDetailRepresentation);
    existingMachiningBatch.getDailyMachiningBatchDetail().add(dailyMachiningBatchDetail);
    dailyMachiningBatchDetail.setMachiningBatch(existingMachiningBatch);

    int dailyActualFinishedMachiningPiecesCount = dailyMachiningBatchDetailRepresentation.getCompletedPiecesCount();
    int dailyRejectedMachiningPiecesCount = dailyMachiningBatchDetailRepresentation.getRejectedPiecesCount();
    int dailyReworkMachiningPiecesCount = dailyMachiningBatchDetailRepresentation.getReworkPiecesCount();
    existingMachiningBatch.setActualMachiningBatchPiecesCount(existingMachiningBatch.getActualMachiningBatchPiecesCount() == null ? dailyActualFinishedMachiningPiecesCount
                                                                                                                                  : existingMachiningBatch.getActualMachiningBatchPiecesCount()
                                                                                                                                    + dailyActualFinishedMachiningPiecesCount);
    existingMachiningBatch.setRejectMachiningBatchPiecesCount(existingMachiningBatch.getRejectMachiningBatchPiecesCount() == null ? dailyRejectedMachiningPiecesCount
                                                                                                                                  : existingMachiningBatch.getRejectMachiningBatchPiecesCount()
                                                                                                                                    + dailyRejectedMachiningPiecesCount);
    existingMachiningBatch.setReworkPiecesCount(
        existingMachiningBatch.getReworkPiecesCount() == null ? dailyReworkMachiningPiecesCount : existingMachiningBatch.getReworkPiecesCount() + dailyReworkMachiningPiecesCount);

    ProcessedItem processedItem = existingMachiningBatch.getProcessedItem();
    processedItem.setInitialReworkMachiningBatchPiecesCount(processedItem.getInitialReworkMachiningBatchPiecesCount()==null?dailyReworkMachiningPiecesCount:processedItem.getInitialReworkMachiningBatchPiecesCount() + dailyReworkMachiningPiecesCount);
    processedItem.setAvailableReworkMachiningBatchPiecesCount(processedItem.getAvailableReworkMachiningBatchPiecesCount()==null?dailyReworkMachiningPiecesCount: processedItem.getAvailableReworkMachiningBatchPiecesCount() + dailyReworkMachiningPiecesCount);

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

  private int getActualFinishedMachiningPieces(List<DailyMachiningBatchDetailRepresentation> dailyMachiningBatchDetailRepresentations) {
    return dailyMachiningBatchDetailRepresentations.stream()
        .mapToInt(DailyMachiningBatchDetailRepresentation::getCompletedPiecesCount)
        .sum();
  }

  private int getActualRejectedMachiningPieces(List<DailyMachiningBatchDetailRepresentation> dailyMachiningBatchDetailRepresentations) {
    return dailyMachiningBatchDetailRepresentations.stream()
        .mapToInt(DailyMachiningBatchDetailRepresentation::getRejectedPiecesCount)
        .sum();
  }

  private int getActualReworkedMachiningPieces(List<DailyMachiningBatchDetailRepresentation> dailyMachiningBatchDetailRepresentations) {
    return dailyMachiningBatchDetailRepresentations.stream()
        .mapToInt(DailyMachiningBatchDetailRepresentation::getReworkPiecesCount)
        .sum();
  }
}
