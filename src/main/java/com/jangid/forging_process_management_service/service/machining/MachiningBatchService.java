package com.jangid.forging_process_management_service.service.machining;

import com.jangid.forging_process_management_service.assemblers.heating.ProcessedItemHeatTreatmentBatchAssembler;
import com.jangid.forging_process_management_service.assemblers.machining.DailyMachiningBatchAssembler;
import com.jangid.forging_process_management_service.assemblers.machining.MachiningBatchAssembler;
import com.jangid.forging_process_management_service.assemblers.machining.ProcessedItemMachiningBatchAssembler;
import com.jangid.forging_process_management_service.entities.ProcessedItem;
import com.jangid.forging_process_management_service.entities.Tenant;
import com.jangid.forging_process_management_service.entities.heating.ProcessedItemHeatTreatmentBatch;
import com.jangid.forging_process_management_service.entities.inventory.Heat;
import com.jangid.forging_process_management_service.entities.machining.DailyMachiningBatch;
import com.jangid.forging_process_management_service.entities.machining.MachineSet;
import com.jangid.forging_process_management_service.entities.machining.MachiningBatch;
import com.jangid.forging_process_management_service.entities.machining.ProcessedItemMachiningBatch;
import com.jangid.forging_process_management_service.entities.operator.MachineOperator;
import com.jangid.forging_process_management_service.entities.product.ItemStatus;
import com.jangid.forging_process_management_service.entitiesRepresentation.heating.ProcessedItemHeatTreatmentBatchRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.machining.DailyMachiningBatchRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.machining.MachiningBatchRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.machining.ProcessedItemMachiningBatchRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.machining.MachiningBatchDetailRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.machining.MachiningBatchStatisticsRepresentation;
import com.jangid.forging_process_management_service.exception.ResourceNotFoundException;
import com.jangid.forging_process_management_service.exception.machining.DailyMachiningBatchOverlapException;
import com.jangid.forging_process_management_service.exception.machining.MachiningBatchNotFoundException;
import com.jangid.forging_process_management_service.repositories.machining.MachiningBatchRepository;
import com.jangid.forging_process_management_service.repositories.quality.InspectionBatchRepository;
import com.jangid.forging_process_management_service.service.ProcessedItemService;
import com.jangid.forging_process_management_service.service.TenantService;
import com.jangid.forging_process_management_service.service.heating.ProcessedItemHeatTreatmentBatchService;
import com.jangid.forging_process_management_service.service.inventory.RawMaterialHeatService;
import com.jangid.forging_process_management_service.service.operator.MachineOperatorService;
import com.jangid.forging_process_management_service.utils.ConvertorUtils;
import com.jangid.forging_process_management_service.utils.MachiningBatchUtil;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MachiningBatchService {

  @Autowired
  private MachiningBatchRepository machiningBatchRepository;
  @Autowired
  private InspectionBatchRepository inspectionBatchRepository;

  @Autowired
  private TenantService tenantService;

  @Autowired
  private MachineSetService machineSetService;
  @Autowired
  private DailyMachiningBatchService dailyMachiningBatchService;

  @Autowired
  private MachiningBatchAssembler machiningBatchAssembler;
  @Autowired
  private DailyMachiningBatchAssembler dailyMachiningBatchDetailAssembler;
  @Autowired
  private ProcessedItemHeatTreatmentBatchService processedItemHeatTreatmentBatchService;
  @Autowired
  private ProcessedItemMachiningBatchService processedItemMachiningBatchService;

  @Autowired
  private ProcessedItemHeatTreatmentBatchAssembler processedItemHeatTreatmentBatchAssembler;
  @Autowired
  private ProcessedItemMachiningBatchAssembler processedItemMachiningBatchAssembler;
  @Autowired
  private MachineOperatorService machineOperatorService;

  @Autowired
  private RawMaterialHeatService rawMaterialHeatService;

  @Autowired
  private ProcessedItemService processedItemService;


  @Transactional
  public MachiningBatchRepresentation applyMachiningBatch(long tenantId, long machineSetId, MachiningBatchRepresentation representation, boolean rework) {
    tenantService.validateTenantExists(tenantId);
    MachineSet machineSet = getMachineSetUsingTenantIdAndMachineSetId(tenantId, machineSetId);

    if (!MachineSet.MachineSetStatus.MACHINING_NOT_APPLIED.equals(machineSet.getMachineSetStatus())) {
      log.error("MachineSet={} is in a status={}, which is not correct for applying machining batch", machineSetId, machineSet.getMachineSetStatus().name());
      throw new RuntimeException("Cannot apply a new machining batch on this machine set as it is in an incorrect status.");
    }

    if (!MachineSet.MachineSetRunningJobType.NONE.equals(machineSet.getMachineSetRunningJobType())) {
      log.error("MachineSet={} is in runningJobType={}, which is not correct for applying machining batch", machineSetId, machineSet.getMachineSetStatus().name());
      throw new RuntimeException("Cannot apply a new machining batch on this machine set as it is having an incorrect runningJobType.");
    }

    if (isMachiningBatchAppliedOnMachineSet(machineSetId)) {
      log.error("MachineSet={} already has a machining batch. Cannot apply a new machining batch.", machineSetId);
      throw new RuntimeException("MachineSet already has a machining batch.");
    }

    boolean exists = machiningBatchRepository.existsByMachiningBatchNumberAndTenantIdAndDeletedFalse(representation.getMachiningBatchNumber(), tenantId);
    if (exists) {
      log.error("Machining Batch with batch number: {} already exists with the tenant: {}!", representation.getMachiningBatchNumber(), tenantId);
      throw new IllegalStateException("Machining Batch with batch number =" + representation.getMachiningBatchNumber() + "with the tenant =" + tenantId);
    }

    MachiningBatch machiningBatch = machiningBatchAssembler.createAssemble(representation);

    ProcessedItemHeatTreatmentBatch heatTreatmentBatch = null;
    ProcessedItemMachiningBatch inputProcessedItemMachiningBatch = null;
    ProcessedItemMachiningBatch outputProcessedItemMachiningBatch = null;
    LocalDateTime applyAtLocalDateTime = ConvertorUtils.convertStringToLocalDateTime(representation.getApplyAt());

    if (!rework) {
      // Direct Machining without forging
      if (!CollectionUtils.isEmpty(machiningBatch.getMachiningHeats())) {
        // Update heat pieces
        machiningBatch.getMachiningHeats().forEach(machiningHeat -> {

          Heat heat = machiningHeat.getHeat();
          int newHeatPieces = heat.getAvailablePiecesCount() - machiningHeat.getPiecesUsed();
          if (newHeatPieces < 0) {
            log.error("Insufficient heat pieces for heat={} on tenantId={}", heat.getId(), tenantId);
            throw new IllegalArgumentException("Insufficient heat pieces for heat " + heat.getId());
          }
          if (heat.getCreatedAt().compareTo(applyAtLocalDateTime) > 0) {
            log.error("The provided apply at time={} is before to heat={} created at time={} !", applyAtLocalDateTime,
                      heat.getHeatNumber(), heat.getCreatedAt());
            throw new RuntimeException(
                "The provided apply at time=" + applyAtLocalDateTime + " is before to heat=" + heat.getHeatNumber() + " created at time=" + heat.getCreatedAt()
                + " !");
          }
          log.info("Updating AvailablePiecesCount for heat={} to {}", heat.getId(), newHeatPieces);
          heat.setAvailablePiecesCount(newHeatPieces);
          rawMaterialHeatService.updateRawMaterialHeat(heat); // Persist the updated heat
          machiningHeat.setMachiningBatch(machiningBatch);
        });
        machiningBatch.setMachiningBatchType(MachiningBatch.MachiningBatchType.FRESH);
        outputProcessedItemMachiningBatch = machiningBatch.getProcessedItemMachiningBatch();
//        DirectMachiningProcessedItem directMachiningProcessedItem = machiningBatch.getProcessedItemMachiningBatch().getDirectMachiningProcessedItem();
//        directMachiningProcessedItem.setCreatedAt(LocalDateTime.now());
//        DirectMachiningProcessedItem savedDirectMachiningProcessedItem = directMachiningProcessedItemService.save(directMachiningProcessedItem);
//        outputProcessedItemMachiningBatch.setDirectMachiningProcessedItem(savedDirectMachiningProcessedItem);
        ProcessedItem processedItem = outputProcessedItemMachiningBatch.getProcessedItem();
//        processedItem.setItem(representation.getItem());
        ProcessedItem savedPI = processedItemService.save(processedItem);
        outputProcessedItemMachiningBatch.setProcessedItem(savedPI);
        outputProcessedItemMachiningBatch.setMachiningBatch(machiningBatch);

        outputProcessedItemMachiningBatch.setAvailableMachiningBatchPiecesCount(outputProcessedItemMachiningBatch.getMachiningBatchPiecesCount());
        machiningBatch.setProcessedItemMachiningBatch(outputProcessedItemMachiningBatch);
      } else {
        // Machining after forging
        LocalDateTime inputHeatTreatmentBatchCompleteAt = ConvertorUtils.convertStringToLocalDateTime(representation.getProcessedItemHeatTreatmentBatch().getHeatTreatmentBatch().getEndAt());

        if (inputHeatTreatmentBatchCompleteAt.compareTo(applyAtLocalDateTime) >= 0) {
          log.error("The provided apply at time={} is before or equal to input heat treatment batch={} complete at time={} !", applyAtLocalDateTime,
                    representation.getProcessedItemHeatTreatmentBatch().getHeatTreatmentBatch().getHeatTreatmentBatchNumber(), inputHeatTreatmentBatchCompleteAt);
          throw new RuntimeException(
              "The provided apply at time=" + applyAtLocalDateTime + " is before or equal to input heat treatment batch=" + representation.getProcessedItemHeatTreatmentBatch().getHeatTreatmentBatch()
                  .getHeatTreatmentBatchNumber() + " complete at time=" + inputHeatTreatmentBatchCompleteAt + " !");
        }
        ProcessedItemHeatTreatmentBatchRepresentation heatTreatmentBatchRep = representation.getProcessedItemHeatTreatmentBatch();
        heatTreatmentBatch = processedItemHeatTreatmentBatchService.getProcessedItemHeatTreatmentBatchById(heatTreatmentBatchRep.getId());
        machiningBatch.setProcessedItemHeatTreatmentBatch(heatTreatmentBatch);
        machiningBatch.setMachiningBatchType(MachiningBatch.MachiningBatchType.FRESH);
        outputProcessedItemMachiningBatch = machiningBatch.getProcessedItemMachiningBatch();
        if (outputProcessedItemMachiningBatch.getMachiningBatchPiecesCount() > heatTreatmentBatch.getAvailableMachiningBatchPiecesCount()) {
          throw new IllegalArgumentException("Machining batch pieces count exceeds available pieces count.");
        }
        outputProcessedItemMachiningBatch.setProcessedItem(heatTreatmentBatch.getProcessedItem());
        outputProcessedItemMachiningBatch.setMachiningBatch(machiningBatch);

        // Deduct the pieces from the heat treatment batch
        heatTreatmentBatch.setAvailableMachiningBatchPiecesCount(
            heatTreatmentBatch.getAvailableMachiningBatchPiecesCount() - outputProcessedItemMachiningBatch.getMachiningBatchPiecesCount()
        );
        outputProcessedItemMachiningBatch.setAvailableMachiningBatchPiecesCount(outputProcessedItemMachiningBatch.getMachiningBatchPiecesCount());
        machiningBatch.setProcessedItemMachiningBatch(outputProcessedItemMachiningBatch);
      }
    } else {
      // rework
      LocalDateTime inputMatchBatchStartAt = ConvertorUtils.convertStringToLocalDateTime(representation.getInputProcessedItemMachiningBatch().getMachiningBatch().getStartAt());

      if (inputMatchBatchStartAt.compareTo(applyAtLocalDateTime) >= 0) {
        log.error("The provided apply at time={} is before or equal to input machine treatment batch={} start at time={} !", applyAtLocalDateTime,
                  representation.getInputProcessedItemMachiningBatch().getMachiningBatch().getMachiningBatchNumber(), inputMatchBatchStartAt);
        throw new RuntimeException(
            "The provided apply at time=" + applyAtLocalDateTime + " is before or equal to input machine treatment batch=" + representation.getInputProcessedItemMachiningBatch().getMachiningBatch()
                .getMachiningBatchNumber() + " start at time=" + inputMatchBatchStartAt
            + " !");
      }
      ProcessedItemMachiningBatchRepresentation inputProcessedItemMachiningBatchRep = representation.getInputProcessedItemMachiningBatch();
      inputProcessedItemMachiningBatch = processedItemMachiningBatchService.getProcessedItemMachiningBatchById(inputProcessedItemMachiningBatchRep.getId());
      machiningBatch.setInputProcessedItemMachiningBatch(inputProcessedItemMachiningBatch);
      machiningBatch.setMachiningBatchType(MachiningBatch.MachiningBatchType.REWORK);
      outputProcessedItemMachiningBatch = machiningBatch.getProcessedItemMachiningBatch();
      if (outputProcessedItemMachiningBatch.getMachiningBatchPiecesCount() > inputProcessedItemMachiningBatch.getReworkPiecesCountAvailableForRework()) {
        throw new IllegalArgumentException("Machining batch pieces count exceeds available rework pieces count.");
      }
      outputProcessedItemMachiningBatch.setProcessedItem(inputProcessedItemMachiningBatch.getProcessedItem());
      outputProcessedItemMachiningBatch.setMachiningBatch(machiningBatch);

      // Deduct the pieces from the input processedItemMachinig batch
      inputProcessedItemMachiningBatch.setReworkPiecesCountAvailableForRework(
          inputProcessedItemMachiningBatch.getReworkPiecesCountAvailableForRework() - outputProcessedItemMachiningBatch.getMachiningBatchPiecesCount()
      );
      outputProcessedItemMachiningBatch.setAvailableMachiningBatchPiecesCount(outputProcessedItemMachiningBatch.getMachiningBatchPiecesCount());
      machiningBatch.setInputProcessedItemMachiningBatch(inputProcessedItemMachiningBatch);
      machiningBatch.setProcessedItemMachiningBatch(outputProcessedItemMachiningBatch);
    }

    machiningBatch.setMachineSet(machineSet);

    Tenant tenant = tenantService.getTenantById(tenantId);
    machiningBatch.setTenant(tenant);
    MachiningBatch createdMachiningBatch = machiningBatchRepository.save(machiningBatch);
    machineSetService.updateMachineSetStatus(machineSet, MachineSet.MachineSetStatus.MACHINING_APPLIED);

    if (!rework) {
      // Machining after forging
      if (CollectionUtils.isEmpty(machiningBatch.getMachiningHeats())) {
        processedItemHeatTreatmentBatchService.save(heatTreatmentBatch);
        machineSetService.updateMachineSetRunningJobType(machineSet, MachineSet.MachineSetRunningJobType.FRESH);
      } else {
        processedItemMachiningBatchService.save(outputProcessedItemMachiningBatch);
      }
    } else {
      processedItemMachiningBatchService.save(inputProcessedItemMachiningBatch);
      machineSetService.updateMachineSetRunningJobType(machineSet, MachineSet.MachineSetRunningJobType.REWORK);
    }

    MachiningBatchRepresentation machiningBatchRepresentation = machiningBatchAssembler.dissemble(createdMachiningBatch);

    if (!rework) {
      // Machining after forging
      if (CollectionUtils.isEmpty(machiningBatch.getMachiningHeats())) {
        machiningBatchRepresentation.setProcessedItemHeatTreatmentBatch(processedItemHeatTreatmentBatchAssembler.dissemble(heatTreatmentBatch));
      }
    } else {
      machiningBatchRepresentation.setInputProcessedItemMachiningBatch(processedItemMachiningBatchAssembler.dissemble(inputProcessedItemMachiningBatch));
    }
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
  public MachiningBatchRepresentation startMachiningBatch(long tenantId, long machineSetId, long machiningBatchId, String startAt, boolean rework) {
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
    LocalDateTime startTimeLocalDateTime = ConvertorUtils.convertStringToLocalDateTime(startAt);

    if (existingMachiningBatch.getApplyAt().compareTo(startTimeLocalDateTime) > 0) {
      log.error("The machiningBatch={} having batch number={} provided start time is before apply at time!", machiningBatchId, existingMachiningBatch.getMachiningBatchNumber());
      throw new RuntimeException("MachiningBatch=" + machiningBatchId + " , batch number=" + batchNumber + " provided start time is before apply at time!");
    }

    if (existingMachiningBatch.getStartAt() != null) {
      log.error("The machiningBatch={} having batch number={} has already been started!", machiningBatchId, existingMachiningBatch.getMachiningBatchNumber());
      throw new RuntimeException("machiningBatch=" + machiningBatchId + " , batch number=" + batchNumber + "has already been started!");
    }

    if (!MachiningBatch.MachiningBatchStatus.IDLE.equals(existingMachiningBatch.getMachiningBatchStatus())) {
      log.error("The machiningBatch={} having batch number={} is not in IDLE status to start it!", machiningBatchId, existingMachiningBatch.getMachiningBatchNumber());
      throw new RuntimeException("MachiningBatch=" + machiningBatchId + " ,  batch number=" + batchNumber + "Not in IDLE status to start it!");
    }

    existingMachiningBatch.setMachiningBatchStatus(MachiningBatch.MachiningBatchStatus.IN_PROGRESS);
    existingMachiningBatch.setStartAt(startTimeLocalDateTime);

    existingMachiningBatch.getProcessedItemMachiningBatch().setItemStatus(ItemStatus.MACHINING_IN_PROGRESS);

    MachiningBatch startedMachiningBatch = machiningBatchRepository.save(existingMachiningBatch);

    machineSetService.updateMachineSetStatus(machineSet, MachineSet.MachineSetStatus.MACHINING_IN_PROGRESS);
    if (rework) {
      machineSetService.updateMachineSetRunningJobType(machineSet, MachineSet.MachineSetRunningJobType.REWORK);
    } else {
      machineSetService.updateMachineSetRunningJobType(machineSet, MachineSet.MachineSetRunningJobType.FRESH);
    }

    return machiningBatchAssembler.dissemble(startedMachiningBatch);
  }

  @Transactional
  public MachiningBatchRepresentation endMachiningBatch(long tenantId, long machineSetId, long machiningBatchId, MachiningBatchRepresentation machiningBatchRepresentation, boolean rework) {
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

    DailyMachiningBatch lastDailyMachiningBatch = MachiningBatchUtil.getLatestDailyMachiningBatch(existingMachiningBatch);

    if (lastDailyMachiningBatch != null && lastDailyMachiningBatch.getEndDateTime() != null && lastDailyMachiningBatch.getEndDateTime().compareTo(endAt) >= 0) {
      log.error("The end time of machining batch={} having batch number={} is before the last DailyMachiningBatch's end time!", machiningBatchId, existingMachiningBatch.getMachiningBatchNumber());
      throw new RuntimeException(
          "The end time of machining batch=" + machiningBatchId + " having batch number=" + batchNumber + "  is before the last DailyMachiningBatch's end time!");
    }

    boolean isFullMachiningBatchCompleted;
    if (!rework) {
      if (CollectionUtils.isEmpty(existingMachiningBatch.getMachiningHeats())) {
        ProcessedItemHeatTreatmentBatch processedItemHeatTreatmentBatch = existingMachiningBatch.getProcessedItemHeatTreatmentBatch();
        isFullMachiningBatchCompleted = processedItemHeatTreatmentBatch.getAvailableMachiningBatchPiecesCount() == 0;
      } else {
        ProcessedItemMachiningBatch processedItemMachiningBatch = existingMachiningBatch.getProcessedItemMachiningBatch();
        isFullMachiningBatchCompleted = processedItemMachiningBatch.getAvailableMachiningBatchPiecesCount() == 0;
      }
    } else {
      ProcessedItemMachiningBatch inputProcessedItemMachiningBatch = existingMachiningBatch.getInputProcessedItemMachiningBatch();
      isFullMachiningBatchCompleted = inputProcessedItemMachiningBatch.getReworkPiecesCountAvailableForRework() == 0;
    }

    ProcessedItemMachiningBatch processedItemMachiningBatch = existingMachiningBatch.getProcessedItemMachiningBatch();

    if (isFullMachiningBatchCompleted) {
      if (processedItemMachiningBatch.getReworkPiecesCountAvailableForRework() == 0) {
        processedItemMachiningBatch.setItemStatus(ItemStatus.MACHINING_COMPLETED);
      } else {
        processedItemMachiningBatch.setItemStatus(ItemStatus.MACHINING_COMPLETED_WITH_REWORK);
      }
    } else {
      if (processedItemMachiningBatch.getReworkPiecesCountAvailableForRework() == 0) {
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
    machineSetService.updateMachineSetRunningJobType(machineSet, MachineSet.MachineSetRunningJobType.NONE);

    return machiningBatchAssembler.dissemble(completedMachiningBatch);
  }

  @Transactional
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

    if (existingMachiningBatch.getStartAt().compareTo(dailyMachiningBatchStartDateTime) > 0) {
      log.error("The dailyMachiningBatchStartDateTime provided is before start time of machining batch={} having batch number={} !", machiningBatchId, batchNumber);
      throw new RuntimeException("MachiningBatch=" + machiningBatchId + " , batch number=" + batchNumber + " start time is after the dailyMachiningBatchStartDateTime!");
    }

    DailyMachiningBatch lastDailyMachiningBatch = MachiningBatchUtil.getLatestDailyMachiningBatch(existingMachiningBatch);

    if (lastDailyMachiningBatch != null) {
      if (dailyMachiningBatchStartDateTime.compareTo(lastDailyMachiningBatch.getEndDateTime()) <= 0) {
        log.error("The dailyMachiningBatchStartDateTime provided is not greater than last DailyMachiningBatch endDateTime for machiningBatchId={}!", machiningBatchId);
        throw new RuntimeException("DailyMachiningBatch start dateTime must be greater than the last daily machining batch endDateTime.");
      }

      if (dailyMachiningBatchEndDateTime.compareTo(lastDailyMachiningBatch.getEndDateTime()) <= 0) {
        log.error("The dailyMachiningBatchEndDateTime provided is not greater than last DailyMachiningBatch endDateTime for machiningBatchId={}!", machiningBatchId);
        throw new RuntimeException("DailyMachiningBatch end dateTime must be greater than the last daily machining batch endDateTime.");
      }
    }

    if (dailyMachiningBatchStartDateTime.compareTo(dailyMachiningBatchEndDateTime) >= 0) {
      log.error("The dailyMachiningBatch start dateTime is after or equal to end dateTime for machiningBatchId={} having machiningBatchNumber={}!", machiningBatchId, batchNumber);
      throw new RuntimeException("The dailyMachiningBatch start dateTime must be less than the end dateTime.");
    }

    if (dailyMachiningBatchService.existsOverlappingBatchForOperator(dailyMachiningBatchRepresentation.getMachineOperator().getOperator().getId(),
                                                                     ConvertorUtils.convertStringToLocalDateTime(dailyMachiningBatchRepresentation.getStartDateTime()),
                                                                     ConvertorUtils.convertStringToLocalDateTime(dailyMachiningBatchRepresentation.getEndDateTime()))) {
      log.error("There exists an overlap between the startTime={} and endTime={} for the operator having id={}",
                dailyMachiningBatchRepresentation.getStartDateTime(), dailyMachiningBatchRepresentation.getEndDateTime(), dailyMachiningBatchRepresentation.getMachineOperator().getOperator().getId());
      throw new DailyMachiningBatchOverlapException(
          "There exists an overlap between the startTime=" + dailyMachiningBatchRepresentation.getStartDateTime() + " and endTime=" + dailyMachiningBatchRepresentation.getEndDateTime()
          + " for the operator having id=" + dailyMachiningBatchRepresentation.getMachineOperator().getOperator().getId());
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

    processedItemMachiningBatch.setReworkPiecesCountAvailableForRework(
        (processedItemMachiningBatch.getReworkPiecesCountAvailableForRework() != null
        ? processedItemMachiningBatch.getReworkPiecesCountAvailableForRework()
        : 0) + dailyReworkMachiningPiecesCount
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

    MachineOperator machineOperator = machineOperatorService.getMachineOperatorById(dailyMachiningBatchRepresentation.getMachineOperator().getOperator().getId());
    machineOperator.getDailyMachiningBatches().add(dailyMachiningBatch);
    MachineOperator savedMachineOperator = machineOperatorService.save(machineOperator);
    dailyMachiningBatch.setMachineOperator(savedMachineOperator);
    dailyMachiningBatchService.save(dailyMachiningBatch);
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

  @Transactional
  public void deleteMachiningBatch(long tenantId, long machiningBatchId) {
    // 1. Validate tenant exists
    tenantService.validateTenantExists(tenantId);

    // 2. Validate machining batch exists
    MachiningBatch machiningBatch = getMachiningBatchById(machiningBatchId);

    // 3. Validate machining batch status is COMPLETED
    if (!MachiningBatch.MachiningBatchStatus.COMPLETED.equals(machiningBatch.getMachiningBatchStatus())) {
      log.error("Cannot delete machining batch={} as it is not in COMPLETED status", machiningBatchId);
      throw new IllegalStateException("Cannot delete machining batch that is not in COMPLETED status");
    }

    validateIfAnyInspectionBatchExistsForMachiningBatch(machiningBatch);

    ProcessedItemMachiningBatch outputProcessedItemMachiningBatch = machiningBatch.getProcessedItemMachiningBatch();

    // 4. Handle non-rework case
    if (!MachiningBatch.MachiningBatchType.REWORK.equals(machiningBatch.getMachiningBatchType())) {
      ProcessedItemHeatTreatmentBatch heatTreatmentBatch = machiningBatch.getProcessedItemHeatTreatmentBatch();
      // Revert the pieces count deduction
      heatTreatmentBatch.setAvailableMachiningBatchPiecesCount(
          heatTreatmentBatch.getAvailableMachiningBatchPiecesCount() + outputProcessedItemMachiningBatch.getMachiningBatchPiecesCount()
      );
      processedItemHeatTreatmentBatchService.save(heatTreatmentBatch);
    }
    // 5. Handle rework case
    else {
      ProcessedItemMachiningBatch inputProcessedItemMachiningBatch = machiningBatch.getInputProcessedItemMachiningBatch();
      // Revert the rework pieces count deduction
      inputProcessedItemMachiningBatch.setReworkPiecesCountAvailableForRework(
          inputProcessedItemMachiningBatch.getReworkPiecesCountAvailableForRework() + outputProcessedItemMachiningBatch.getMachiningBatchPiecesCount()
      );
      processedItemMachiningBatchService.save(inputProcessedItemMachiningBatch);
    }

    // 6. Soft delete processedItemMachiningBatch
    LocalDateTime now = LocalDateTime.now();
    outputProcessedItemMachiningBatch.setDeleted(true);
    outputProcessedItemMachiningBatch.setDeletedAt(now);
    processedItemMachiningBatchService.save(outputProcessedItemMachiningBatch);

    // 7. Soft delete MachiningBatch
    machiningBatch.setDeleted(true);
    machiningBatch.setDeletedAt(now);
    machiningBatchRepository.save(machiningBatch);

    log.info("Successfully deleted machining batch={}", machiningBatchId);
  }

  private void validateIfAnyInspectionBatchExistsForMachiningBatch(MachiningBatch machiningBatch) {
    boolean isExists = inspectionBatchRepository.existsByInputProcessedItemMachiningBatchIdAndDeletedFalse(machiningBatch.getProcessedItemMachiningBatch().getId());
    if (isExists) {
      log.error("There exists inspection batch entry for the machiningBatchId={} for the tenant={}!", machiningBatch.getId(), machiningBatch.getTenant().getId());
      throw new IllegalStateException("There exists inspection batch entry for the machiningBatchId=" + machiningBatch.getId() + " for the tenant=" + machiningBatch.getTenant().getId());
    }

  }

  public MachiningBatchStatisticsRepresentation getMachiningBatchStatistics(Long tenantId) {
    List<MachiningBatch> inProgressBatches = machiningBatchRepository
        .findByTenantIdAndMachiningBatchStatusInProgressAndDeletedFalse(tenantId);

    if (inProgressBatches.isEmpty()) {
      return MachiningBatchStatisticsRepresentation.builder()
          .totalInProgressBatches(0)
          .totalPiecesInProgress(0)
          .totalMachineSetsInUse(0)
          .totalOperatorsAssigned(0)
          .averageProcessingTimeInHours(0.0)
          .totalReworkBatches(0)
          .totalFreshBatches(0)
          .batchDetails(List.of())
          .build();
    }

    int totalPieces = 0;
    Set<Long> machineSetIds = new HashSet<>();
    Set<Long> operatorIds = new HashSet<>();
    long totalProcessingTimeInHours = 0;
    int reworkBatches = 0;
    int freshBatches = 0;
    List<MachiningBatchDetailRepresentation> batchDetails = new ArrayList<>();

    for (MachiningBatch batch : inProgressBatches) {
      ProcessedItemMachiningBatch processedItem = batch.getProcessedItemMachiningBatch();

      // Count pieces
      if (processedItem != null) {
        totalPieces += processedItem.getMachiningBatchPiecesCount();
      }

      // Count unique machine sets
      if (batch.getMachineSet() != null) {
        machineSetIds.add(batch.getMachineSet().getId());
      }

      // Count unique operators from daily machining batches
      Set<Long> batchOperatorIds = new HashSet<>();
      for (DailyMachiningBatch dailyBatch : batch.getDailyMachiningBatch()) {
        if (dailyBatch.getMachineOperator() != null && dailyBatch.getMachineOperator().getId() != null) {
          Long operatorId = dailyBatch.getMachineOperator().getId();
          operatorIds.add(operatorId);
          batchOperatorIds.add(operatorId);
        }
      }

      // Calculate processing time
      double processingTimeInHours = 0.0;
      if (batch.getStartAt() != null) {
        processingTimeInHours = ChronoUnit.HOURS.between(batch.getStartAt(), LocalDateTime.now());
        totalProcessingTimeInHours += processingTimeInHours;
      }

      // Count batch types
      if (batch.getMachiningBatchType() == MachiningBatch.MachiningBatchType.REWORK) {
        reworkBatches++;
      } else {
        freshBatches++;
      }

      // Create batch detail
      MachiningBatchDetailRepresentation detail = MachiningBatchDetailRepresentation.builder()
          .id(batch.getId())
          .machiningBatchNumber(batch.getMachiningBatchNumber())
          .machineSetName(batch.getMachineSet() != null ? batch.getMachineSet().getMachineSetName() : null)
          .totalPieces(processedItem != null ? processedItem.getMachiningBatchPiecesCount() : 0)
          .completedPieces(processedItem != null ? processedItem.getActualMachiningBatchPiecesCount() : 0)
          .rejectedPieces(processedItem != null ? processedItem.getRejectMachiningBatchPiecesCount() : 0)
          .reworkPieces(processedItem != null ? processedItem.getReworkPiecesCount() : 0)
          .reworkPiecesAvailableForRework(processedItem != null ? processedItem.getReworkPiecesCountAvailableForRework() : 0)
          .availablePieces(processedItem != null ? processedItem.getAvailableMachiningBatchPiecesCount() : 0)
          .startAt(batch.getStartAt() != null ? batch.getStartAt().toString() : null)
          .processingTimeInHours(processingTimeInHours)
          .machiningBatchType(batch.getMachiningBatchType().name())
          .machiningBatchStatus(batch.getMachiningBatchStatus().name())
          .totalOperatorsAssigned(batchOperatorIds.size())
          .build();

      batchDetails.add(detail);
    }

    double averageProcessingTime = inProgressBatches.size() > 0
                                   ? (double) totalProcessingTimeInHours / inProgressBatches.size()
                                   : 0.0;

    return MachiningBatchStatisticsRepresentation.builder()
        .totalInProgressBatches(inProgressBatches.size())
        .totalPiecesInProgress(totalPieces)
        .totalMachineSetsInUse(machineSetIds.size())
        .totalOperatorsAssigned(operatorIds.size())
        .averageProcessingTimeInHours(averageProcessingTime)
        .totalReworkBatches(reworkBatches)
        .totalFreshBatches(freshBatches)
        .batchDetails(batchDetails)
        .build();
  }
}
