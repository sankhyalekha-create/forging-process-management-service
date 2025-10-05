package com.jangid.forging_process_management_service.service.machining;

import com.jangid.forging_process_management_service.assemblers.machining.DailyMachiningBatchAssembler;
import com.jangid.forging_process_management_service.assemblers.machining.MachiningBatchAssembler;
import com.jangid.forging_process_management_service.assemblers.machining.ProcessedItemMachiningBatchAssembler;
import com.jangid.forging_process_management_service.dto.workflow.OperationOutcomeData;
import com.jangid.forging_process_management_service.dto.workflow.WorkflowOperationContext;
import com.jangid.forging_process_management_service.entities.Tenant;
import com.jangid.forging_process_management_service.entities.inventory.Heat;
import com.jangid.forging_process_management_service.entities.machining.DailyMachiningBatch;
import com.jangid.forging_process_management_service.entities.machining.MachineSet;
import com.jangid.forging_process_management_service.entities.machining.MachiningBatch;
import com.jangid.forging_process_management_service.entities.machining.ProcessedItemMachiningBatch;
import com.jangid.forging_process_management_service.entities.operator.MachineOperator;
import com.jangid.forging_process_management_service.entities.product.Item;
import com.jangid.forging_process_management_service.entities.product.ItemStatus;
import com.jangid.forging_process_management_service.entities.workflow.ItemWorkflow;
import com.jangid.forging_process_management_service.entities.workflow.ItemWorkflowStep;
import com.jangid.forging_process_management_service.entities.workflow.WorkflowStep;
import com.jangid.forging_process_management_service.entitiesRepresentation.machining.DailyMachiningBatchRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.machining.MachiningBatchRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.machining.ProcessedItemMachiningBatchRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.machining.MachiningBatchDetailRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.machining.MachiningBatchStatisticsRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.machining.MonthlyMachiningStatisticsRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.machining.MachiningHeatRepresentation;
import com.jangid.forging_process_management_service.exception.ResourceNotFoundException;
import com.jangid.forging_process_management_service.exception.machining.DailyMachiningBatchOverlapException;
import com.jangid.forging_process_management_service.exception.machining.MachiningBatchNotFoundException;
import com.jangid.forging_process_management_service.repositories.machining.MachiningBatchRepository;
import com.jangid.forging_process_management_service.repositories.quality.InspectionBatchRepository;
import com.jangid.forging_process_management_service.service.TenantService;
import com.jangid.forging_process_management_service.service.inventory.RawMaterialHeatService;
import com.jangid.forging_process_management_service.service.operator.MachineOperatorService;
import com.jangid.forging_process_management_service.service.workflow.ItemWorkflowService;
import com.jangid.forging_process_management_service.utils.ConvertorUtils;
import com.jangid.forging_process_management_service.utils.MachiningBatchUtil;
import com.jangid.forging_process_management_service.dto.MachiningBatchAssociationsDTO;
import com.jangid.forging_process_management_service.entitiesRepresentation.dispatch.DispatchBatchRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.quality.InspectionBatchRepresentation;
import com.jangid.forging_process_management_service.service.quality.InspectionBatchService;
import com.jangid.forging_process_management_service.service.dispatch.DispatchBatchService;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataAccessException;

import com.jangid.forging_process_management_service.exception.document.DocumentDeletionException;
import com.jangid.forging_process_management_service.entities.document.DocumentLink;
import com.jangid.forging_process_management_service.service.document.DocumentService;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
  private ProcessedItemMachiningBatchService processedItemMachiningBatchService;

  @Autowired
  private ProcessedItemMachiningBatchAssembler processedItemMachiningBatchAssembler;
  @Autowired
  private MachineOperatorService machineOperatorService;

  @Autowired
  private RawMaterialHeatService rawMaterialHeatService;

  @Autowired
  private InspectionBatchService inspectionBatchService;

  @Autowired
  private DispatchBatchService dispatchBatchService;

  @Autowired
  private ItemWorkflowService itemWorkflowService;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private DocumentService documentService;

  @Transactional(rollbackFor = Exception.class)
  public MachiningBatchRepresentation createMachiningBatch(long tenantId, MachiningBatchRepresentation representation) {
    log.info("Starting machining batch creation transaction for tenant: {}, batch: {}",
             tenantId, representation.getMachiningBatchNumber());

    MachiningBatch savedMachiningBatch = null;
    try {
      // Phase 1: Initial validations
      validateTenantAndBatchNumber(tenantId, representation);

      // Phase 2: Setup machining batch
      MachiningBatch inputMachiningBatch = setupMachiningBatch(tenantId, representation);

      // Phase 3: Process the machining batch item with workflow integration
      processMachiningBatchItem(representation, inputMachiningBatch);

      // Phase 4: Finalize and save - this generates the required ID for workflow integration
      MachiningBatchRepresentation result = finalizeMachiningBatch(inputMachiningBatch, representation);
      savedMachiningBatch = machiningBatchRepository.findById(inputMachiningBatch.getId()).orElse(null);

      log.info("Successfully completed machining batch creation transaction for batch ID: {}",
               inputMachiningBatch.getId());
      return result;

    } catch (Exception e) {
      log.error("Machining batch creation transaction failed for tenant: {}, batch: {}. " +
                "All changes will be rolled back. Error: {}",
                tenantId, representation.getMachiningBatchNumber(), e.getMessage());

      if (savedMachiningBatch != null) {
        log.error("Machining batch with ID {} was persisted but workflow integration failed. " +
                  "Transaction rollback will restore database consistency.", savedMachiningBatch.getId());
      }

      // Re-throw to ensure transaction rollback
      throw e;
    }
  }

  /**
   * Phase 1: Validate tenant existence and batch number uniqueness
   */
  private void validateTenantAndBatchNumber(long tenantId, MachiningBatchRepresentation representation) {
    tenantService.validateTenantExists(tenantId);

    boolean exists = machiningBatchRepository.existsByMachiningBatchNumberAndTenantIdAndDeletedFalse(
        representation.getMachiningBatchNumber(), tenantId);
    if (exists) {
      log.error("Machining Batch with batch number: {} already exists with the tenant: {}!",
                representation.getMachiningBatchNumber(), tenantId);
      throw new IllegalStateException("Machining Batch with batch number =" + representation.getMachiningBatchNumber()
                                      + " with the tenant =" + tenantId);
    }

    // Check if this batch number was previously used and deleted
    if (isMachiningBatchNumberPreviouslyUsed(representation.getMachiningBatchNumber(), tenantId)) {
      log.warn("Machining Batch with batch number: {} was previously used and deleted for tenant: {}",
               representation.getMachiningBatchNumber(), tenantId);
    }
  }

  /**
   * Phase 2: Setup machining batch with basic properties
   */
  private MachiningBatch setupMachiningBatch(long tenantId, MachiningBatchRepresentation representation) {
    MachiningBatch inputMachiningBatch = machiningBatchAssembler.createAssemble(representation);

    Tenant tenant = tenantService.getTenantById(tenantId);
    inputMachiningBatch.setTenant(tenant);
    inputMachiningBatch.setMachiningBatchType(MachiningBatch.MachiningBatchType.FRESH);

    return inputMachiningBatch;
  }

  /**
   * Phase 3: Process the machining batch item with workflow integration
   */
  private void processMachiningBatchItem(MachiningBatchRepresentation representation,
                                         MachiningBatch inputMachiningBatch) {
    // Process the single processed item machining batch from the representation
    if (representation.getProcessedItemMachiningBatch() != null) {
      ProcessedItemMachiningBatch processedItemMachiningBatch =
          processedItemMachiningBatchAssembler.createAssemble(representation.getProcessedItemMachiningBatch());

      // Finalize processed item setup and add to the batch
      finalizeProcessedItemSetup(processedItemMachiningBatch, inputMachiningBatch);

      // Set the processed item to the batch
      inputMachiningBatch.setProcessedItemMachiningBatch(processedItemMachiningBatch);
    }
  }

  /**
   * Finalize processed item setup
   */
  private void finalizeProcessedItemSetup(ProcessedItemMachiningBatch processedItemMachiningBatch,
                                          MachiningBatch inputMachiningBatch) {
    processedItemMachiningBatch.setItemStatus(ItemStatus.MACHINING_NOT_STARTED);
    processedItemMachiningBatch.setMachiningBatch(inputMachiningBatch);

    // Set available pieces count equal to the machining batch pieces count initially
    processedItemMachiningBatch.setAvailableMachiningBatchPiecesCount(
        processedItemMachiningBatch.getMachiningBatchPiecesCount());
  }

  /**
   * Phase 4: Finalize and save machining batch
   */
  private MachiningBatchRepresentation finalizeMachiningBatch(MachiningBatch inputMachiningBatch,
                                                              MachiningBatchRepresentation machiningBatchRepresentation) {
    MachiningBatch createdMachiningBatch = machiningBatchRepository.save(inputMachiningBatch);

    // Handle workflow integration and pieces consumption
    if (createdMachiningBatch.getProcessedItemMachiningBatch() != null) {
      handleWorkflowIntegration(machiningBatchRepresentation, createdMachiningBatch.getProcessedItemMachiningBatch());
    }

    // Return the created MachiningBatch
    MachiningBatchRepresentation createdRepresentation = machiningBatchAssembler.dissemble(createdMachiningBatch);
    createdRepresentation.setMachiningBatchStatus(MachiningBatch.MachiningBatchStatus.IDLE.name());

    return createdRepresentation;
  }

  /**
   * Handle workflow integration including pieces consumption and workflow step updates
   */
  private void handleWorkflowIntegration(MachiningBatchRepresentation representation,
                                         ProcessedItemMachiningBatch processedItemMachiningBatch) {
    try {
      // Get or validate workflow
      ItemWorkflow workflow = getOrValidateWorkflow(processedItemMachiningBatch);

      // Determine operation position and get workflow step
      WorkflowOperationContext operationContext = createOperationContext(processedItemMachiningBatch, workflow);

      // Start workflow step operation
      itemWorkflowService.startItemWorkflowStepOperation(operationContext.getTargetWorkflowStep());

      // Handle inventory/pieces consumption based on operation position
      handleInventoryConsumption(representation, processedItemMachiningBatch, workflow, operationContext.isFirstOperation());

      // Update workflow step with batch outcome data
      updateWorkflowStepWithBatchOutcome(processedItemMachiningBatch, operationContext.isFirstOperation(), operationContext.getTargetWorkflowStep());

      // Update related entity IDs
      updateRelatedEntityIds(operationContext.getTargetWorkflowStep(), processedItemMachiningBatch.getId());

    } catch (Exception e) {
      log.error("Failed to integrate machining batch with workflow for item {}: {}",
                processedItemMachiningBatch.getItem().getId(), e.getMessage());
      throw new RuntimeException("Failed to integrate with workflow system: " + e.getMessage(), e);
    }
  }

  /**
   * Get or validate workflow for the processed item
   */
  private ItemWorkflow getOrValidateWorkflow(ProcessedItemMachiningBatch processedItemMachiningBatch) {
    Long itemWorkflowId = processedItemMachiningBatch.getItemWorkflowId();
    if (itemWorkflowId == null) {
      throw new IllegalStateException("ItemWorkflowId is required for machining batch integration");
    }

    ItemWorkflow workflow = itemWorkflowService.getItemWorkflowById(itemWorkflowId);

    // Ensure workflow ID is set (defensive programming)
    if (processedItemMachiningBatch.getItemWorkflowId() == null) {
      processedItemMachiningBatch.setItemWorkflowId(workflow.getId());
      processedItemMachiningBatchService.save(processedItemMachiningBatch);
    }

    return workflow;
  }

  /**
   * Create operation context with workflow step information
   */
  private WorkflowOperationContext createOperationContext(ProcessedItemMachiningBatch processedItemMachiningBatch, ItemWorkflow workflow) {
    Long previousOperationProcessedItemId = processedItemMachiningBatch.getPreviousOperationProcessedItemId();

    // Determine if this is the first operation
    boolean isFirstOperation = isFirstWorkflowOperation(previousOperationProcessedItemId, workflow);

    // Find the appropriate workflow step
    ItemWorkflowStep targetMachiningStep = findTargetMachiningStep(workflow, previousOperationProcessedItemId, isFirstOperation);

    return new WorkflowOperationContext(isFirstOperation, targetMachiningStep, previousOperationProcessedItemId);
  }

  /**
   * Determine if this is the first operation in the workflow
   */
  private boolean isFirstWorkflowOperation(Long previousOperationProcessedItemId, ItemWorkflow workflow) {
    return previousOperationProcessedItemId == null ||
           workflow.getWorkflowTemplate().isFirstOperationType(WorkflowStep.OperationType.MACHINING);
  }

  /**
   * Find the target machining workflow step based on operation position
   */
  private ItemWorkflowStep findTargetMachiningStep(ItemWorkflow workflow, Long previousOperationProcessedItemId, boolean isFirstOperation) {
    if (isFirstOperation) {
      return workflow.getFirstRootStep(WorkflowStep.OperationType.MACHINING);
    } else {
      return itemWorkflowService.findItemWorkflowStepByParentEntityId(
          workflow.getId(),
          previousOperationProcessedItemId,
          WorkflowStep.OperationType.MACHINING);
    }
  }

  /**
   * Handle inventory/pieces consumption based on operation position
   */
  private void handleInventoryConsumption(MachiningBatchRepresentation representation,
                                          ProcessedItemMachiningBatch processedItemMachiningBatch,
                                          ItemWorkflow workflow,
                                          boolean isFirstOperation) {
    if (isFirstOperation) {
      log.info("Machining is the first operation in workflow - consuming inventory from heat");
      handleHeatConsumptionForFirstOperation(representation, processedItemMachiningBatch, workflow);
    } else {
      handlePiecesConsumptionFromPreviousOperation(processedItemMachiningBatch, workflow);
    }
  }

  /**
   * Update related entity IDs for the workflow step
   */
  private void updateRelatedEntityIds(ItemWorkflowStep targetMachiningStep, Long processedItemId) {
    if (targetMachiningStep != null) {
      itemWorkflowService.updateRelatedEntityIdsForSpecificStep(targetMachiningStep, processedItemId);
    } else {
      log.warn("Could not find target MACHINING ItemWorkflowStep for machining batch {}", processedItemId);
    }
  }

  /**
   * Handles pieces consumption from previous operation (if applicable)
   * Uses optimized single-call method to improve performance
   */
  private void handlePiecesConsumptionFromPreviousOperation(ProcessedItemMachiningBatch processedItemMachiningBatch,
                                                            ItemWorkflow workflow) {
    // Use the optimized method that combines find + validate + consume in a single efficient call
    try {
      ItemWorkflowStep parentOperationStep = itemWorkflowService.validateAndConsumePiecesFromParentOperation(
          workflow.getId(),
          WorkflowStep.OperationType.MACHINING,
          processedItemMachiningBatch.getPreviousOperationProcessedItemId(),
          processedItemMachiningBatch.getMachiningBatchPiecesCount()
      );

      log.info("Efficiently consumed {} pieces from {} operation {} for machining in workflow {}",
               processedItemMachiningBatch.getMachiningBatchPiecesCount(),
               parentOperationStep.getOperationType(),
               processedItemMachiningBatch.getPreviousOperationProcessedItemId(),
               workflow.getId());

    } catch (IllegalArgumentException e) {
      // Re-throw with context for machining batch
      log.error("Failed to consume pieces for machining batch: {}", e.getMessage());
      throw e;
    }
  }

  /**
   * Handles heat consumption from inventory when machining is the first operation
   */
  private void handleHeatConsumptionForFirstOperation(MachiningBatchRepresentation representation,
                                                      ProcessedItemMachiningBatch processedItemMachiningBatch,
                                                      ItemWorkflow workflow) {
    // Get the corresponding processed item representation to access heat data
    ProcessedItemMachiningBatchRepresentation processedItemRepresentation = representation.getProcessedItemMachiningBatch();

    if (processedItemRepresentation == null ||
        processedItemRepresentation.getMachiningHeats() == null ||
        processedItemRepresentation.getMachiningHeats().isEmpty()) {
      log.warn("No heat consumption data provided for first operation machining batch processed item {}. This may result in inventory inconsistency.",
               processedItemMachiningBatch.getId());
      return;
    }

    // Validate that heat consumption matches the required pieces
    int totalPiecesFromHeats = processedItemRepresentation.getMachiningHeats().stream()
        .mapToInt(MachiningHeatRepresentation::getPiecesUsed)
        .sum();

    if (totalPiecesFromHeats != processedItemMachiningBatch.getMachiningBatchPiecesCount()) {
      throw new IllegalArgumentException("Total pieces from heats (" + totalPiecesFromHeats +
                                         ") does not match machining batch pieces count (" +
                                         processedItemMachiningBatch.getMachiningBatchPiecesCount() +
                                         ") for processed item " + processedItemMachiningBatch.getId());
    }

    // Get the machining batch create time for validation
    LocalDateTime createAtLocalDateTime = ConvertorUtils.convertStringToLocalDateTime(representation.getCreateAt());

    // Validate heat availability and consume pieces from inventory
    processedItemRepresentation.getMachiningHeats().forEach(heatRepresentation -> {
      try {
        // Get the heat entity
        Heat heat = rawMaterialHeatService.getRawMaterialHeatById(heatRepresentation.getHeat().getId());

        // Calculate new available pieces count after consumption
        int newHeatPieces = heat.getAvailablePiecesCount() - heatRepresentation.getPiecesUsed();

        // Validate sufficient pieces are available
        if (newHeatPieces < 0) {
          log.error("Insufficient heat pieces for heat={} on workflow={}", heat.getId(), workflow.getId());
          throw new IllegalArgumentException("Insufficient heat pieces for heat " + heat.getId());
        }

        // Validate timing - heat should be received before the machining create time
        LocalDateTime rawMaterialReceivingDate = heat.getRawMaterialProduct().getRawMaterial().getRawMaterialReceivingDate();
        if (rawMaterialReceivingDate != null && rawMaterialReceivingDate.compareTo(createAtLocalDateTime) > 0) {
          log.error("The provided create at time={} is before raw material receiving date={} for heat={} !",
                    createAtLocalDateTime, rawMaterialReceivingDate, heat.getHeatNumber());
          throw new RuntimeException("The provided create at time=" + createAtLocalDateTime +
                                     " is before raw material receiving date=" + rawMaterialReceivingDate +
                                     " for heat=" + heat.getHeatNumber() + " !");
        }

        // Update heat available pieces count
        log.info("Updating AvailablePiecesCount for heat={} from {} to {} for machining batch processed item {}",
                 heat.getId(), heat.getAvailablePiecesCount(), newHeatPieces, processedItemMachiningBatch.getId());
        heat.setAvailablePiecesCount(newHeatPieces);

        // Persist the updated heat
        rawMaterialHeatService.updateRawMaterialHeat(heat);

        log.info("Successfully consumed {} pieces from heat {} for machining batch processed item {} in workflow {}",
                 heatRepresentation.getPiecesUsed(),
                 heatRepresentation.getHeat().getId(),
                 processedItemMachiningBatch.getId(),
                 workflow.getId());

      } catch (Exception e) {
        log.error("Failed to consume pieces from heat {} for machining processed item {}: {}",
                  heatRepresentation.getHeat().getId(), processedItemMachiningBatch.getId(), e.getMessage());
        throw new RuntimeException("Failed to consume inventory from heat: " + e.getMessage(), e);
      }
    });

    log.info("Successfully consumed inventory from {} heats for machining batch processed item {} in workflow {}",
             processedItemRepresentation.getMachiningHeats().size(), processedItemMachiningBatch.getId(), workflow.getId());
  }

  /**
   * Update workflow step with accumulated batch outcome data
   */
  private void updateWorkflowStepWithBatchOutcome(ProcessedItemMachiningBatch processedItemMachiningBatch, boolean isFirstOperation, ItemWorkflowStep operationStep) {
    // Create machiningBatchOutcome object with available data from ProcessedItemMachiningBatch
    OperationOutcomeData.BatchOutcome machiningBatchOutcome = OperationOutcomeData.BatchOutcome.builder()
        .id(processedItemMachiningBatch.getId())
        .initialPiecesCount(0)
        .piecesAvailableForNext(0)
        .createdAt(processedItemMachiningBatch.getCreatedAt())
        .updatedAt(LocalDateTime.now())
        .deletedAt(processedItemMachiningBatch.getDeletedAt())
        .deleted(processedItemMachiningBatch.isDeleted())
        .build();

    List<OperationOutcomeData.BatchOutcome> accumulatedBatchData = new ArrayList<>();

    if (!isFirstOperation) {
      accumulatedBatchData = itemWorkflowService.getAccumulatedBatchOutcomeData(operationStep);
    }
    accumulatedBatchData.add(machiningBatchOutcome);
    itemWorkflowService.updateWorkflowStepForOperation(operationStep, OperationOutcomeData.forMachiningOperation(accumulatedBatchData, LocalDateTime.now()));
  }


  public MachineSet getMachineSetUsingTenantIdAndMachineSetId(long tenantId, long machineSetId) {
    boolean isMachineSetOfTenantExists = machineSetService.isMachineSetExistsUsingTenantIdAndMachineSetId(tenantId, machineSetId);
    if (!isMachineSetOfTenantExists) {
      log.error("MachineSet={} does not exist!", machineSetId);
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

  @Transactional(rollbackFor = Exception.class)
  public MachiningBatchRepresentation startMachiningBatch(long tenantId, long machineSetId, long machiningBatchId, String startAt, boolean rework) {
    log.info("Starting machining batch start transaction for tenant: {}, machineSet: {}, batch: {}",
             tenantId, machineSetId, machiningBatchId);

    MachiningBatch startedMachiningBatch = null;
    try {
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

      if (existingMachiningBatch.getCreateAt().compareTo(startTimeLocalDateTime) > 0) {
        log.error("The machiningBatch={} having batch number={} provided start time is before create at time!", machiningBatchId, existingMachiningBatch.getMachiningBatchNumber());
        throw new RuntimeException("MachiningBatch=" + machiningBatchId + " , batch number=" + batchNumber + " provided start time is before create at time!");
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

      startedMachiningBatch = machiningBatchRepository.save(existingMachiningBatch);
      log.info("Successfully persisted started machining batch with ID: {}", startedMachiningBatch.getId());

      machineSetService.updateMachineSetStatus(machineSet, MachineSet.MachineSetStatus.MACHINING_IN_PROGRESS);
      if (rework) {
        machineSetService.updateMachineSetRunningJobType(machineSet, MachineSet.MachineSetRunningJobType.REWORK);
      } else {
        machineSetService.updateMachineSetRunningJobType(machineSet, MachineSet.MachineSetRunningJobType.FRESH);
      }

      // Update workflow - if this fails, entire transaction will rollback
      updateWorkflowForMachiningBatchStart(startedMachiningBatch, startTimeLocalDateTime);

      log.info("Successfully completed machining batch start transaction for ID: {}", startedMachiningBatch.getId());
      return machiningBatchAssembler.dissemble(startedMachiningBatch);

    } catch (Exception e) {
      log.error("Machining batch start transaction failed for tenant: {}, batch: {}. " +
                "All changes will be rolled back. Error: {}",
                tenantId, machiningBatchId, e.getMessage());

      if (startedMachiningBatch != null) {
        log.error("Machining batch with ID {} was started but workflow integration failed. " +
                  "Transaction rollback will restore database consistency.", startedMachiningBatch.getId());
      }

      // Re-throw to ensure transaction rollback
      throw e;
    }
  }

  private void updateWorkflowForMachiningBatchStart(MachiningBatch machiningBatch, LocalDateTime startedAt) {
    try {
      // Validate workflow and get ItemWorkflow
      ItemWorkflow workflow = validateAndGetWorkflow(machiningBatch);
      ProcessedItemMachiningBatch processedItemMachiningBatch = machiningBatch.getProcessedItemMachiningBatch();

      // Get existing workflow step data to preserve other batch outcomes
      ItemWorkflowStep machiningItemWorkflowStep = getMachiningWorkflowStep(
          workflow.getId(), processedItemMachiningBatch.getId());

      List<OperationOutcomeData.BatchOutcome> existingBatchData = itemWorkflowService.extractExistingBatchData(machiningItemWorkflowStep);

      // Find and update the specific batch outcome for this machining batch
      boolean batchFound = false;
      for (OperationOutcomeData.BatchOutcome batchOutcome : existingBatchData) {
        if (Objects.equals(batchOutcome.getId(), processedItemMachiningBatch.getId())) {
          batchOutcome.setStartedAt(startedAt);
          batchFound = true;
          break;
        }
      }

      itemWorkflowService.updateWorkflowStepForOperation(machiningItemWorkflowStep, OperationOutcomeData.forMachiningOperation(existingBatchData, LocalDateTime.now()));

      if (batchFound) {
        log.info("Successfully updated workflow {} with machining batch start data, startedAt: {}",
                 workflow.getId(), startedAt);
      } else {
        log.warn("Failed to find existing batch data for workflow {} for machining batch start {}",
                 workflow.getId(), machiningBatch.getId());
      }

    } catch (Exception e) {
      // Re-throw the exception to fail the machining batch start since workflow integration is now mandatory
      log.error("Failed to update workflow for machining batch start on machining batch ID={}: {}. Failing machining batch start.",
                machiningBatch.getId(), e.getMessage());
      throw new RuntimeException("Failed to update workflow for machining batch start: " + e.getMessage(), e);
    }
  }


  private ItemWorkflow validateAndGetWorkflow(MachiningBatch machiningBatch) {
    Long itemWorkflowId = machiningBatch.getProcessedItemMachiningBatch().getItemWorkflowId();
    if (itemWorkflowId == null) {
      log.error("itemWorkflowId is mandatory for machiningBatch operations as it's required for workflow integration");
      throw new IllegalArgumentException("itemWorkflowId is mandatory for machiningBatch operations. " +
                                         "This is required to update the workflow step so that next operations can consume the produced pieces.");
    }

    // Get the specific workflow by ID and validate it
    ItemWorkflow workflow = itemWorkflowService.getItemWorkflowById(itemWorkflowId);
    Long itemId = workflow.getItem().getId();

    // Validate that the workflow belongs to the same item as the machiningBatch
    if (!itemId.equals(machiningBatch.getProcessedItemMachiningBatch().getItem().getId())) {
      log.error("ItemWorkflow {} does not belong to the same item as machiningBatch {}", itemWorkflowId, machiningBatch.getId());
      throw new IllegalArgumentException("ItemWorkflow does not belong to the same item as the machiningBatch");
    }

    return workflow;
  }

  @Transactional(rollbackFor = Exception.class)
  public MachiningBatchRepresentation endMachiningBatch(long tenantId, long machiningBatchId, MachiningBatchRepresentation machiningBatchRepresentation, boolean rework) throws Exception {
    log.info("Starting machining batch completion transaction for tenant: {}, batch: {}",
             tenantId, machiningBatchId);

    MachiningBatch completedMachiningBatch = null;
    try {
      // Phase 1: Initial validations
      tenantService.validateTenantExists(tenantId);
      MachiningBatch existingMachiningBatch = getMachiningBatchById(machiningBatchId);

      // Phase 2: Validate batch end conditions
      LocalDateTime endAt = validateBatchEndConditions(existingMachiningBatch, machiningBatchRepresentation, machiningBatchId);

      // Phase 3: Validate piece counts
      validatePieceCounts(existingMachiningBatch, machiningBatchId);

      // Phase 4: Complete the batch
      completedMachiningBatch = completeMachiningBatch(existingMachiningBatch, endAt);
      log.info("Successfully persisted completed machining batch with ID: {}", completedMachiningBatch.getId());

      // Phase 5: Update workflow for completion - if this fails, entire transaction will rollback
      updateWorkflowStepsForCompletion(completedMachiningBatch, endAt, machiningBatchId);

      log.info("Successfully completed machining batch completion transaction for ID: {}", machiningBatchId);
      return machiningBatchAssembler.dissemble(completedMachiningBatch);

    } catch (Exception e) {
      log.error("Machining batch completion transaction failed for tenant: {}, batch: {}. " +
                "All changes will be rolled back. Error: {}",
                tenantId, machiningBatchId, e.getMessage());

      if (completedMachiningBatch != null) {
        log.error("Machining batch with ID {} was completed but workflow integration failed. " +
                  "Transaction rollback will restore database consistency.", completedMachiningBatch.getId());
      }

      // Re-throw to ensure transaction rollback
      throw e;
    }
  }

  /**
   * Validates batch end conditions including timing and status checks
   */
  private LocalDateTime validateBatchEndConditions(MachiningBatch existingMachiningBatch,
                                                   MachiningBatchRepresentation machiningBatchRepresentation,
                                                   long machiningBatchId) {
    String batchNumber = existingMachiningBatch.getMachiningBatchNumber();

    // Check if batch is already ended
    if (existingMachiningBatch.getEndAt() != null) {
      log.error("The machiningBatch={} having batch number={} has already been ended!",
                machiningBatchId, batchNumber);
      throw new RuntimeException("MachiningBatch=" + machiningBatchId + " , batch number=" + batchNumber +
                                 " has already been ended!");
    }

    LocalDateTime endAt = ConvertorUtils.convertStringToLocalDateTime(machiningBatchRepresentation.getEndAt());

    // Validate end time is after start time
    if (existingMachiningBatch.getStartAt().compareTo(endAt) >= 0) {
      log.error("The machiningBatch={} having batch number={} end time is before or equal to start time!",
                machiningBatchId, batchNumber);
      throw new RuntimeException("MachiningBatch=" + machiningBatchId + " , batch number=" + batchNumber +
                                 " end time is before or equal to start time!");
    }

    // Validate end time is after last daily batch end time
    validateEndTimeAgainstDailyBatches(existingMachiningBatch, endAt, machiningBatchId, batchNumber);

    return endAt;
  }

  /**
   * Validates that the batch end time is after the last daily machining batch end time
   */
  private void validateEndTimeAgainstDailyBatches(MachiningBatch existingMachiningBatch,
                                                  LocalDateTime endAt,
                                                  long machiningBatchId,
                                                  String batchNumber) {
    DailyMachiningBatch lastDailyMachiningBatch = MachiningBatchUtil.getLatestDailyMachiningBatch(existingMachiningBatch);

    if (lastDailyMachiningBatch != null &&
        lastDailyMachiningBatch.getEndDateTime() != null &&
        lastDailyMachiningBatch.getEndDateTime().compareTo(endAt) > 0) {

      log.error("The end time of machining batch={} having batch number={} is before the last DailyMachiningBatch's end time!",
                machiningBatchId, batchNumber);
      throw new RuntimeException("The end time of machining batch=" + machiningBatchId +
                                 " having batch number=" + batchNumber +
                                 " is before the last DailyMachiningBatch's end time!");
    }
  }

  /**
   * Validates that piece counts are consistent and complete
   */
  private void validatePieceCounts(MachiningBatch existingMachiningBatch, long machiningBatchId) {
    ProcessedItemMachiningBatch processedItemMachiningBatch = existingMachiningBatch.getProcessedItemMachiningBatch();
    String batchNumber = existingMachiningBatch.getMachiningBatchNumber();

    int appliedMachiningBatchPiecesCount = processedItemMachiningBatch.getMachiningBatchPiecesCount();
    int totalMachinePiecesCount = processedItemMachiningBatch.getActualMachiningBatchPiecesCount() != null ?
                                  processedItemMachiningBatch.getActualMachiningBatchPiecesCount() : 0;
    int totalRejectedPiecesCount = processedItemMachiningBatch.getRejectMachiningBatchPiecesCount() != null ?
                                   processedItemMachiningBatch.getRejectMachiningBatchPiecesCount() : 0;
    int totalReworkPiecesCount = processedItemMachiningBatch.getReworkPiecesCount() != null ?
                                 processedItemMachiningBatch.getReworkPiecesCount() : 0;

    int totalProcessedPieces = totalMachinePiecesCount + totalRejectedPiecesCount + totalReworkPiecesCount;

    if (totalProcessedPieces < appliedMachiningBatchPiecesCount) {
      log.error("The total provided actual finished, reject, and rework machining piece counts for all daily batches is " +
                "less than the total applied machining pieces count for machining batch={} having batch number={}! " +
                "Applied: {}, Processed: {} (Finished: {}, Rejected: {}, Rework: {})",
                machiningBatchId, batchNumber, appliedMachiningBatchPiecesCount, totalProcessedPieces,
                totalMachinePiecesCount, totalRejectedPiecesCount, totalReworkPiecesCount);

      throw new RuntimeException("The total provided actual finished, reject, and rework machining piece counts " +
                                 "for all daily batches is less than the total applied machining pieces count! " +
                                 "Applied: " + appliedMachiningBatchPiecesCount + ", Processed: " + totalProcessedPieces);
    }

    log.info("Piece count validation passed for machining batch={}: Applied={}, Processed={} (Finished={}, Rejected={}, Rework={})",
             machiningBatchId, appliedMachiningBatchPiecesCount, totalProcessedPieces,
             totalMachinePiecesCount, totalRejectedPiecesCount, totalReworkPiecesCount);
  }

  /**
   * Completes the machining batch by setting end time and status
   */
  private MachiningBatch completeMachiningBatch(MachiningBatch existingMachiningBatch, LocalDateTime endAt) {
    existingMachiningBatch.setEndAt(endAt);
    existingMachiningBatch.setMachiningBatchStatus(MachiningBatch.MachiningBatchStatus.COMPLETED);

    MachiningBatch completedMachiningBatch = machiningBatchRepository.save(existingMachiningBatch);

    log.info("Successfully completed machining batch={} with batch number={} at {}",
             completedMachiningBatch.getId(),
             completedMachiningBatch.getMachiningBatchNumber(),
             endAt);

    return completedMachiningBatch;
  }

  @Transactional(rollbackFor = Exception.class)
  public MachiningBatchRepresentation dailyMachiningBatchUpdate(long tenantId, long machiningBatchId,
                                                                DailyMachiningBatchRepresentation dailyMachiningBatchRepresentation) {
    log.info("Starting daily machining batch update transaction for tenant: {}, batch: {}, daily batch: {}",
             tenantId, machiningBatchId, dailyMachiningBatchRepresentation.getDailyMachiningBatchNumber());

    MachiningBatch updatedMachiningBatch = null;
    try {
      // Phase 1: Initial validations
      tenantService.validateTenantExists(tenantId);
      MachiningBatch existingMachiningBatch = getMachiningBatchById(machiningBatchId);

      // Phase 2: Validate daily batch number uniqueness
      validateDailyBatchNumberUniqueness(dailyMachiningBatchRepresentation, machiningBatchId, existingMachiningBatch.getMachiningBatchNumber(), tenantId);

      // Phase 3: Parse and validate datetime inputs
      LocalDateTime dailyMachiningBatchStartDateTime = ConvertorUtils.convertStringToLocalDateTime(dailyMachiningBatchRepresentation.getStartDateTime());
      LocalDateTime dailyMachiningBatchEndDateTime = ConvertorUtils.convertStringToLocalDateTime(dailyMachiningBatchRepresentation.getEndDateTime());

      // Phase 4: Handle first daily batch logic
      handleFirstDailyBatchLogic(existingMachiningBatch, dailyMachiningBatchStartDateTime, machiningBatchId);

      // Phase 5: Validate timing and overlaps
      validateDailyBatchTiming(dailyMachiningBatchStartDateTime, dailyMachiningBatchEndDateTime, machiningBatchId, existingMachiningBatch.getMachiningBatchNumber());
      validateOperatorAndMachineSetOverlaps(dailyMachiningBatchRepresentation);

      // Phase 6: Create and setup daily batch
      DailyMachiningBatch dailyMachiningBatch = createAndSetupDailyBatch(dailyMachiningBatchRepresentation, existingMachiningBatch);

      // Phase 7: Validate and update piece counts
      int dailyActualFinishedMachiningPiecesCount = validateAndUpdatePieceCounts(dailyMachiningBatchRepresentation, existingMachiningBatch, machiningBatchId);

      // Phase 8: Setup machine operator relationships
      setupMachineOperatorRelationships(dailyMachiningBatch, dailyMachiningBatchRepresentation);

      // Phase 9: Save and finalize - this generates the required ID for workflow integration
      updatedMachiningBatch = machiningBatchRepository.save(existingMachiningBatch);
      log.info("Successfully persisted updated machining batch with ID: {}", updatedMachiningBatch.getId());

      // Phase 10: Update workflow - if this fails, entire transaction will rollback
      updateWorkflowForDailyMachiningBatchUpdate(updatedMachiningBatch, dailyActualFinishedMachiningPiecesCount);

      log.info("Successfully completed daily machining batch update transaction for ID: {}", updatedMachiningBatch.getId());
      return machiningBatchAssembler.dissemble(updatedMachiningBatch);

    } catch (Exception e) {
      log.error("Daily machining batch update transaction failed for tenant: {}, batch: {}, daily batch: {}. " +
                "All changes will be rolled back. Error: {}",
                tenantId, machiningBatchId, dailyMachiningBatchRepresentation.getDailyMachiningBatchNumber(), e.getMessage());

      if (updatedMachiningBatch != null) {
        log.error("Machining batch with ID {} was updated but workflow integration failed. " +
                  "Transaction rollback will restore database consistency.", updatedMachiningBatch.getId());
      }

      // Re-throw to ensure transaction rollback
      throw e;
    }
  }

  /**
   * Validates that the daily batch number is unique within the machining batch
   */
  private void validateDailyBatchNumberUniqueness(DailyMachiningBatchRepresentation dailyMachiningBatchRepresentation,
                                                  long machiningBatchId,
                                                  String batchNumber,
                                                  long tenantId) {
    if (dailyMachiningBatchRepresentation.getDailyMachiningBatchNumber() != null &&
        !dailyMachiningBatchRepresentation.getDailyMachiningBatchNumber().isEmpty()) {

      boolean exists = dailyMachiningBatchService.existsByDailyMachiningBatchNumberAndMachiningBatchIdAndDeletedFalse(
          dailyMachiningBatchRepresentation.getDailyMachiningBatchNumber(), machiningBatchId);

      if (exists) {
        log.error("Machining Shift with batch number: {} already exists within machining batch: {} for tenant: {}!",
                  dailyMachiningBatchRepresentation.getDailyMachiningBatchNumber(), batchNumber, tenantId);
        throw new IllegalStateException("Machining Shift with batch number=" +
                                        dailyMachiningBatchRepresentation.getDailyMachiningBatchNumber() +
                                        " already exists within machining batch=" + batchNumber);
      }
    }
  }

  /**
   * Handles first daily batch specific logic including timing validation and status updates
   */
  private boolean handleFirstDailyBatchLogic(MachiningBatch existingMachiningBatch,
                                             LocalDateTime dailyMachiningBatchStartDateTime,
                                             long machiningBatchId) {
    // Check if this is the first active daily batch by filtering out deleted ones
    List<DailyMachiningBatch> activeDailyBatches = existingMachiningBatch.getDailyMachiningBatch() == null ?
                                                   Collections.emptyList() :
                                                   existingMachiningBatch.getDailyMachiningBatch().stream()
                                                       .filter(batch -> !batch.isDeleted())
                                                       .collect(Collectors.toList());

    boolean isFirstDailyBatch = activeDailyBatches.isEmpty();

    if (isFirstDailyBatch) {
      String batchNumber = existingMachiningBatch.getMachiningBatchNumber();

      // Validate create time vs daily batch start time
      if (existingMachiningBatch.getCreateAt().compareTo(dailyMachiningBatchStartDateTime) > 0) {
        log.error("The dailyMachiningBatchStartDateTime provided is before create time of machining batch={} having batch number={} !",
                  machiningBatchId, batchNumber);
        throw new RuntimeException("MachiningBatch=" + machiningBatchId + " , batch number=" + batchNumber +
                                   " create time is after the dailyMachiningBatchStartDateTime!");
      }

      // Validate start time consistency for first daily batch
      if (existingMachiningBatch.getStartAt() != null &&
          !existingMachiningBatch.getStartAt().equals(dailyMachiningBatchStartDateTime)) {
        log.error("The dailyMachiningBatchStartDateTime must equal the machining batch start time for the first daily batch. MachiningBatch={} having batch number={} !",
                  machiningBatchId, batchNumber);
        throw new RuntimeException("MachiningBatch=" + machiningBatchId + " , batch number=" + batchNumber +
                                   " start time must equal the dailyMachiningBatchStartDateTime for the first daily batch!");
      }

      // Set the startAt of the machining batch to the first daily batch start time
      if (existingMachiningBatch.getStartAt() == null) {
        existingMachiningBatch.setStartAt(dailyMachiningBatchStartDateTime);
        log.info("Setting startAt of machining batch={} to first daily batch start time={}",
                 machiningBatchId, dailyMachiningBatchStartDateTime);
      }

      // Set the machining batch status to IN_PROGRESS for the first daily batch
      if (existingMachiningBatch.getMachiningBatchStatus() != MachiningBatch.MachiningBatchStatus.IN_PROGRESS) {
        existingMachiningBatch.setMachiningBatchStatus(MachiningBatch.MachiningBatchStatus.IN_PROGRESS);
        log.info("Setting machining batch={} status to IN_PROGRESS for first daily batch", machiningBatchId);
      }

      log.info("Handling first daily batch logic for machining batch {}: activeDailyBatches={}, isFirstDailyBatch={}",
               machiningBatchId, activeDailyBatches.size(), isFirstDailyBatch);
    }

    return isFirstDailyBatch;
  }

  /**
   * Validates daily batch timing constraints
   */
  private void validateDailyBatchTiming(LocalDateTime dailyMachiningBatchStartDateTime,
                                        LocalDateTime dailyMachiningBatchEndDateTime,
                                        long machiningBatchId,
                                        String batchNumber) {
    if (dailyMachiningBatchStartDateTime.compareTo(dailyMachiningBatchEndDateTime) >= 0) {
      log.error("The dailyMachiningBatch start dateTime is after or equal to end dateTime for machiningBatchId={} having machiningBatchNumber={}!",
                machiningBatchId, batchNumber);
      throw new RuntimeException("The dailyMachiningBatch start dateTime must be less than the end dateTime.");
    }
  }

  /**
   * Validates operator and machine set overlaps
   */
  private void validateOperatorAndMachineSetOverlaps(DailyMachiningBatchRepresentation dailyMachiningBatchRepresentation) {
    LocalDateTime startDateTime = ConvertorUtils.convertStringToLocalDateTime(dailyMachiningBatchRepresentation.getStartDateTime());
    LocalDateTime endDateTime = ConvertorUtils.convertStringToLocalDateTime(dailyMachiningBatchRepresentation.getEndDateTime());

    // Validate operator overlap
    if (dailyMachiningBatchService.existsOverlappingBatchForOperator(
        dailyMachiningBatchRepresentation.getMachineOperator().getOperator().getId(),
        startDateTime, endDateTime)) {

      log.error("There exists an overlap between the startTime={} and endTime={} for the operator having id={}",
                dailyMachiningBatchRepresentation.getStartDateTime(),
                dailyMachiningBatchRepresentation.getEndDateTime(),
                dailyMachiningBatchRepresentation.getMachineOperator().getOperator().getId());

      throw new DailyMachiningBatchOverlapException(
          "There exists an overlap between the startTime=" + dailyMachiningBatchRepresentation.getStartDateTime() +
          " and endTime=" + dailyMachiningBatchRepresentation.getEndDateTime() +
          " for the operator having id=" + dailyMachiningBatchRepresentation.getMachineOperator().getOperator().getId());
    }

    // Validate machine set overlap
    if (dailyMachiningBatchRepresentation.getMachineSet() != null &&
        dailyMachiningBatchRepresentation.getMachineSet().getId() != null) {

      if (dailyMachiningBatchService.existsOverlappingBatchForMachineSet(
          dailyMachiningBatchRepresentation.getMachineSet().getId(), startDateTime, endDateTime)) {

        log.error("There exists an overlap between the startTime={} and endTime={} for the machineSet having id={}",
                  dailyMachiningBatchRepresentation.getStartDateTime(),
                  dailyMachiningBatchRepresentation.getEndDateTime(),
                  dailyMachiningBatchRepresentation.getMachineSet().getId());

        throw new DailyMachiningBatchOverlapException(
            "There exists an overlap between the startTime=" + dailyMachiningBatchRepresentation.getStartDateTime() +
            " and endTime=" + dailyMachiningBatchRepresentation.getEndDateTime() +
            " for the machineSet having id=" + dailyMachiningBatchRepresentation.getMachineSet().getId());
      }
    }
  }

  /**
   * Creates and sets up the daily machining batch entity
   */
  private DailyMachiningBatch createAndSetupDailyBatch(DailyMachiningBatchRepresentation dailyMachiningBatchRepresentation,
                                                       MachiningBatch existingMachiningBatch) {
    DailyMachiningBatch dailyMachiningBatch = dailyMachiningBatchDetailAssembler.createAssemble(dailyMachiningBatchRepresentation);
    dailyMachiningBatch.setDailyMachiningBatchStatus(DailyMachiningBatch.DailyMachiningBatchStatus.COMPLETED);

    if (existingMachiningBatch.getDailyMachiningBatch() == null) {
      existingMachiningBatch.setDailyMachiningBatch(new ArrayList<>());
    }
    existingMachiningBatch.getDailyMachiningBatch().add(dailyMachiningBatch);
    dailyMachiningBatch.setMachiningBatch(existingMachiningBatch);

    return dailyMachiningBatch;
  }

  /**
   * Validates piece counts and updates the processed item machining batch
   */
  private int validateAndUpdatePieceCounts(DailyMachiningBatchRepresentation dailyMachiningBatchRepresentation,
                                           MachiningBatch existingMachiningBatch,
                                           long machiningBatchId) {
    int dailyActualFinishedMachiningPiecesCount = dailyMachiningBatchRepresentation.getCompletedPiecesCount();
    int dailyRejectedMachiningPiecesCount = dailyMachiningBatchRepresentation.getRejectedPiecesCount();
    int dailyReworkMachiningPiecesCount = dailyMachiningBatchRepresentation.getReworkPiecesCount();

    ProcessedItemMachiningBatch processedItemMachiningBatch = existingMachiningBatch.getProcessedItemMachiningBatch();
    int machiningBatchPiecesCount = processedItemMachiningBatch.getMachiningBatchPiecesCount();
    String batchNumber = existingMachiningBatch.getMachiningBatchNumber();

    // Safely retrieve current counts, defaulting to 0 if null
    int actualMachiningBatchPiecesCount = processedItemMachiningBatch.getActualMachiningBatchPiecesCount() != null
                                          ? processedItemMachiningBatch.getActualMachiningBatchPiecesCount() : 0;
    int rejectMachiningBatchPiecesCount = processedItemMachiningBatch.getRejectMachiningBatchPiecesCount() != null
                                          ? processedItemMachiningBatch.getRejectMachiningBatchPiecesCount() : 0;
    int reworkPiecesCount = processedItemMachiningBatch.getReworkPiecesCount() != null
                            ? processedItemMachiningBatch.getReworkPiecesCount() : 0;
    int availableMachiningBatchPiecesCount = processedItemMachiningBatch.getAvailableMachiningBatchPiecesCount() != null
                                             ? processedItemMachiningBatch.getAvailableMachiningBatchPiecesCount() : 0;

    // Validate piece count constraints
    validateDailyPieceCountConstraints(actualMachiningBatchPiecesCount, rejectMachiningBatchPiecesCount, reworkPiecesCount,
                                       dailyActualFinishedMachiningPiecesCount, dailyRejectedMachiningPiecesCount,
                                       dailyReworkMachiningPiecesCount, machiningBatchPiecesCount, machiningBatchId, batchNumber);

    // Update piece counts
    updateProcessedItemPieceCounts(processedItemMachiningBatch, actualMachiningBatchPiecesCount, rejectMachiningBatchPiecesCount,
                                   reworkPiecesCount, availableMachiningBatchPiecesCount, dailyActualFinishedMachiningPiecesCount,
                                   dailyRejectedMachiningPiecesCount, dailyReworkMachiningPiecesCount);

    return dailyActualFinishedMachiningPiecesCount;
  }

  /**
   * Validates daily piece count constraints against total batch limits
   */
  private void validateDailyPieceCountConstraints(int actualMachiningBatchPiecesCount, int rejectMachiningBatchPiecesCount,
                                                  int reworkPiecesCount, int dailyActualFinishedMachiningPiecesCount,
                                                  int dailyRejectedMachiningPiecesCount, int dailyReworkMachiningPiecesCount,
                                                  int machiningBatchPiecesCount, long machiningBatchId, String batchNumber) {
    // Validate total pieces don't exceed applied count
    if (actualMachiningBatchPiecesCount + dailyActualFinishedMachiningPiecesCount
        + rejectMachiningBatchPiecesCount + dailyRejectedMachiningPiecesCount
        + reworkPiecesCount + dailyReworkMachiningPiecesCount > machiningBatchPiecesCount) {

      log.error("The provided daily actual finished, reject, and rework machining piece counts exceeds " +
                "the applied machine count for machining batch={} having batch number={}!",
                machiningBatchId, batchNumber);
      throw new RuntimeException("The provided daily actual finished, reject, and rework machining piece counts " +
                                 "exceeds the applied machine count!");
    }

    // Validate finished pieces don't exceed applied count
    if (actualMachiningBatchPiecesCount + dailyActualFinishedMachiningPiecesCount > machiningBatchPiecesCount) {
      log.error("The provided daily actual finished machining piece counts exceeds " +
                "the applied machine count for machining batch={} having batch number={}!",
                machiningBatchId, batchNumber);
      throw new RuntimeException("The provided daily actual finished machining piece counts exceeds " +
                                 "the applied machine count!");
    }
  }

  /**
   * Updates all piece counts in the processed item machining batch
   */
  private void updateProcessedItemPieceCounts(ProcessedItemMachiningBatch processedItemMachiningBatch,
                                              int actualMachiningBatchPiecesCount, int rejectMachiningBatchPiecesCount,
                                              int reworkPiecesCount, int availableMachiningBatchPiecesCount,
                                              int dailyActualFinishedMachiningPiecesCount, int dailyRejectedMachiningPiecesCount,
                                              int dailyReworkMachiningPiecesCount) {
    // Update counts, safely handling nulls
    processedItemMachiningBatch.setActualMachiningBatchPiecesCount(
        actualMachiningBatchPiecesCount + dailyActualFinishedMachiningPiecesCount);

    processedItemMachiningBatch.setRejectMachiningBatchPiecesCount(
        rejectMachiningBatchPiecesCount + dailyRejectedMachiningPiecesCount);

    processedItemMachiningBatch.setReworkPiecesCount(
        reworkPiecesCount + dailyReworkMachiningPiecesCount);

    processedItemMachiningBatch.setReworkPiecesCountAvailableForRework(
        (processedItemMachiningBatch.getReworkPiecesCountAvailableForRework() != null
         ? processedItemMachiningBatch.getReworkPiecesCountAvailableForRework() : 0) + dailyReworkMachiningPiecesCount);

    // Note: We no longer set initialInspectionBatchPiecesCount and availableInspectionBatchPiecesCount
    // These values are now managed through workflow system
    processedItemMachiningBatch.setAvailableMachiningBatchPiecesCount(
        availableMachiningBatchPiecesCount - (dailyActualFinishedMachiningPiecesCount + dailyRejectedMachiningPiecesCount + dailyReworkMachiningPiecesCount));
  }

  /**
   * Sets up machine operator relationships for the daily batch
   */
  private void setupMachineOperatorRelationships(DailyMachiningBatch dailyMachiningBatch,
                                                 DailyMachiningBatchRepresentation dailyMachiningBatchRepresentation) {
    MachineOperator machineOperator = machineOperatorService.getMachineOperatorById(
        dailyMachiningBatchRepresentation.getMachineOperator().getOperator().getId());

    machineOperator.getDailyMachiningBatches().add(dailyMachiningBatch);
    MachineOperator savedMachineOperator = machineOperatorService.save(machineOperator);

    dailyMachiningBatch.setMachineOperator(savedMachineOperator);
    dailyMachiningBatchService.save(dailyMachiningBatch);
  }

  /**
   * Update workflow steps for completion
   * Similar to updateWorkflowStepsForCompletion in HeatTreatmentBatchService
   */
  private void updateWorkflowStepsForCompletion(MachiningBatch completedMachiningBatch, LocalDateTime endAt, long machiningBatchId) throws Exception {
    try {
      ProcessedItemMachiningBatch processedItemMachiningBatch = completedMachiningBatch.getProcessedItemMachiningBatch();

      if (processedItemMachiningBatch == null || processedItemMachiningBatch.getItemWorkflowId() == null) {
        log.warn("No workflow ID found for machining batch {}. Skipping workflow completion update.", machiningBatchId);
        return;
      }

      Long itemWorkflowId = processedItemMachiningBatch.getItemWorkflowId();

      updateWorkflowForSpecificMachiningBatch(itemWorkflowId, processedItemMachiningBatch, endAt, machiningBatchId);

      log.info("Updated workflow for machining batch completion {}", machiningBatchId);

    } catch (Exception e) {
      log.error("Could not complete workflow steps for machining batch {}: {}", machiningBatchId, e.getMessage());
      throw e;
    }
  }

  /**
   * Update workflow for a specific machining batch completion
   */
  private void updateWorkflowForSpecificMachiningBatch(Long itemWorkflowId, ProcessedItemMachiningBatch processedItemMachiningBatch, LocalDateTime endAt, long machiningBatchId) throws Exception {
    // Get existing workflow step data to preserve other batch outcomes
    ItemWorkflowStep machiningItemWorkflowStep = getMachiningWorkflowStep(
        itemWorkflowId, processedItemMachiningBatch.getId());

    List<OperationOutcomeData.BatchOutcome> existingBatchData = itemWorkflowService.extractExistingBatchData(machiningItemWorkflowStep);

    // Find and update the specific batch outcome for this machining batch
    boolean batchFound = false;
    for (OperationOutcomeData.BatchOutcome batchOutcome : existingBatchData) {
      if (Objects.equals(batchOutcome.getId(), processedItemMachiningBatch.getId())) {
        // Only update the completedAt field, preserve all other data
        batchOutcome.setCompletedAt(endAt);
        batchOutcome.setUpdatedAt(LocalDateTime.now());
        batchFound = true;

        log.info("Updated machining batch outcome completion: ID={}, completedAt={}",
                 processedItemMachiningBatch.getId(), endAt);
        break;
      }
    }

    itemWorkflowService.updateWorkflowStepForOperation(machiningItemWorkflowStep, OperationOutcomeData.forMachiningOperation(existingBatchData, LocalDateTime.now()));

    if (batchFound) {
      log.info("Successfully updated workflow step for itemWorkflowId {} with machining batch completion {}",
               itemWorkflowId, machiningBatchId);
    } else {
      log.warn("Failed to find existing batch data for itemWorkflowId {} for machining batch completion {}",
               itemWorkflowId, machiningBatchId);
    }
  }

  /*
   * Helper method to retrieve the MACHINING ItemWorkflowStep for a given workflow & processed item
   */
  private ItemWorkflowStep getMachiningWorkflowStep(Long workflowId, Long processedItemId) {
    return itemWorkflowService.findItemWorkflowStepByRelatedEntityId(
        workflowId,
        processedItemId,
        WorkflowStep.OperationType.MACHINING);
  }


  public MachiningBatch getMachiningBatchById(long machiningBatchId) {
    Optional<MachiningBatch> machiningBatchOptional = machiningBatchRepository.findByIdAndDeletedFalse(machiningBatchId);
    if (machiningBatchOptional.isEmpty()) {
      log.error("MachiningBatch does not exists for machiningBatchId={}", machiningBatchId);
      throw new MachiningBatchNotFoundException("MachiningBatch does not exists for machiningBatchId=" + machiningBatchId);
    }
    return machiningBatchOptional.get();
  }

  public MachiningBatch getMachiningBatchByIdAndTenantId(long machiningBatchId, long tenantId) {
    Optional<MachiningBatch> machiningBatchOptional = machiningBatchRepository.findByIdAndTenantIdAndDeletedFalse(machiningBatchId, tenantId);
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

    Page<MachiningBatch> machiningBatchPage = machiningBatchRepository
        .findByTenantIdAndDeletedFalseOrderByUpdatedAtDesc(tenantId, pageable);

    return machiningBatchPage.map(machiningBatchAssembler::dissemble);
  }

  @Transactional(rollbackFor = Exception.class)
  public void deleteMachiningBatch(long tenantId, long machiningBatchId) throws DocumentDeletionException {
    log.info("Starting machining batch deletion transaction for tenant: {}, batch: {}",
             tenantId, machiningBatchId);

    try {
      // Phase 1: Validate all deletion preconditions
      MachiningBatch machiningBatch = validateMachiningBatchDeletionPreconditions(tenantId, machiningBatchId);

      // Phase 2: Delete all documents attached to this machining batch using bulk delete for efficiency
      try {
        // Use bulk delete method from DocumentService for better performance
        documentService.deleteDocumentsForEntity(tenantId, DocumentLink.EntityType.MACHINING_BATCH, machiningBatchId);
        log.info("Successfully bulk deleted all documents attached to machining batch {} for tenant {}", machiningBatchId, tenantId);
      } catch (DataAccessException e) {
        log.error("Database error while deleting documents attached to machining batch {}: {}", machiningBatchId, e.getMessage(), e);
        throw new DocumentDeletionException("Database error occurred while deleting attached documents for machining batch " + machiningBatchId, e);
      } catch (RuntimeException e) {
        // Handle document service specific runtime exceptions (storage, file system errors, etc.)
        log.error("Document service error while deleting documents attached to machining batch {}: {}", machiningBatchId, e.getMessage(), e);
        throw new DocumentDeletionException("Document service error occurred while deleting attached documents for machining batch " + machiningBatchId + ": " + e.getMessage(), e);
      } catch (Exception e) {
        // Handle any other unexpected exceptions
        log.error("Unexpected error while deleting documents attached to machining batch {}: {}", machiningBatchId, e.getMessage(), e);
        throw new DocumentDeletionException("Unexpected error occurred while deleting attached documents for machining batch " + machiningBatchId, e);
      }

      // Phase 3: Process inventory reversal for processed item - CRITICAL: Workflow and heat inventory operations
      ProcessedItemMachiningBatch processedItemMachiningBatch = machiningBatch.getProcessedItemMachiningBatch();
      processInventoryReversalForProcessedItem(processedItemMachiningBatch, machiningBatchId);

      // Phase 4: Soft delete processed item and associated records
      softDeleteProcessedItemAndAssociatedRecords(machiningBatch, processedItemMachiningBatch);

      // Phase 5: Finalize machining batch deletion
      finalizeMachiningBatchDeletion(machiningBatch, machiningBatchId);
      log.info("Successfully persisted machining batch deletion with ID: {}", machiningBatchId);

      log.info("Successfully completed machining batch deletion transaction for ID: {}", machiningBatchId);

    } catch (Exception e) {
      log.error("Machining batch deletion transaction failed for tenant: {}, batch: {}. " +
                "All changes will be rolled back. Error: {}",
                tenantId, machiningBatchId, e.getMessage());

      log.error("Machining batch deletion failed - workflow updates, heat inventory reversals, and entity deletions will be rolled back.");

      // Re-throw to ensure transaction rollback
      throw e;
    }
  }

  /**
   * Phase 1: Validate all deletion preconditions
   */
  private MachiningBatch validateMachiningBatchDeletionPreconditions(long tenantId, long machiningBatchId) {
    // 1. Validate tenant exists
    tenantService.validateTenantExists(tenantId);

    // 2. Validate machining batch exists
    MachiningBatch machiningBatch = getMachiningBatchById(machiningBatchId);

    // 3. Validate machining batch status is COMPLETED
    if (!MachiningBatch.MachiningBatchStatus.COMPLETED.equals(machiningBatch.getMachiningBatchStatus())) {
      log.error("Cannot delete machining batch={} as it is not in COMPLETED status", machiningBatchId);
      throw new IllegalStateException("Cannot delete machining batch that is not in COMPLETED status");
    }

    // 4. Validate no inspection batches exist for this machining batch
    validateIfAnyInspectionBatchExistsForMachiningBatch(machiningBatch);

    return machiningBatch;
  }

  /**
   * Phase 2: Process inventory reversal for processed item
   */
  private void processInventoryReversalForProcessedItem(ProcessedItemMachiningBatch processedItemMachiningBatch, long machiningBatchId) {
    Item item = processedItemMachiningBatch.getItem();
    Long itemWorkflowId = processedItemMachiningBatch.getItemWorkflowId();

    if (itemWorkflowId != null) {
      try {
        validateWorkflowDeletionEligibility(itemWorkflowId, processedItemMachiningBatch.getId(), machiningBatchId);
        handleInventoryReversalBasedOnWorkflowPosition(processedItemMachiningBatch, itemWorkflowId, machiningBatchId, item);
      } catch (Exception e) {
        log.warn("Failed to handle workflow pieces reversion for item {}: {}. This may indicate workflow data inconsistency.",
                 item.getId(), e.getMessage());
        throw e;
      }
    } else {
      log.warn("No workflow ID found for item {} during machining batch deletion. " +
               "This may be a legacy record before workflow integration.", item.getId());
      throw new IllegalStateException("No workflow ID found for item " + item.getId());
    }
  }

  /**
   * Validate workflow deletion eligibility
   */
  private void validateWorkflowDeletionEligibility(Long itemWorkflowId, Long entityId, Long machiningBatchId) {
    // Use workflow-based validation: check if all entries in next operation are marked deleted
    boolean canDeleteMachining = itemWorkflowService.areAllNextOperationBatchesDeleted(itemWorkflowId, entityId, WorkflowStep.OperationType.MACHINING);

    if (!canDeleteMachining) {
      log.error("Cannot delete machining id={} as the next operation has active (non-deleted) batches", machiningBatchId);
      throw new IllegalStateException("This machining cannot be deleted as the next operation has active batch entries.");
    }

    log.info("Machining id={} is eligible for deletion - all next operation batches are deleted", machiningBatchId);
  }

  /**
   * Handle inventory reversal based on workflow position (first operation vs subsequent operations)
   */
  private void handleInventoryReversalBasedOnWorkflowPosition(ProcessedItemMachiningBatch processedItemMachiningBatch,
                                                              Long itemWorkflowId, long machiningBatchId, Item item) {
    // Get the workflow to check if machining was the first operation
    ItemWorkflow workflow = itemWorkflowService.getItemWorkflowById(itemWorkflowId);
    boolean wasFirstOperation = workflow.getWorkflowTemplate().isFirstOperationType(WorkflowStep.OperationType.MACHINING);

    if (wasFirstOperation) {
      handleHeatInventoryReversalForFirstOperation(processedItemMachiningBatch, machiningBatchId, item, itemWorkflowId);
    } else {
      handlePiecesReturnToPreviousOperation(processedItemMachiningBatch, itemWorkflowId);
    }
  }

  /**
   * Handle heat inventory reversal when machining was the first operation
   */
  private void handleHeatInventoryReversalForFirstOperation(ProcessedItemMachiningBatch processedItemMachiningBatch,
                                                            long machiningBatchId, Item item, Long itemWorkflowId) {
    // This was the first operation - heat quantities will be returned to heat inventory
    log.info("Machining batch {} was first operation for item {}, heat inventory will be reverted",
             machiningBatchId, item.getId());

    // Update workflow step to mark machining batch as deleted and adjust piece counts
    Integer actualMachiningBatchPiecesCount = processedItemMachiningBatch.getActualMachiningBatchPiecesCount();
    if (actualMachiningBatchPiecesCount != null && actualMachiningBatchPiecesCount > 0) {
      try {
        itemWorkflowService.updateCurrentOperationStepForReturnedPieces(
            itemWorkflowId,
            WorkflowStep.OperationType.MACHINING,
            actualMachiningBatchPiecesCount,
            processedItemMachiningBatch.getId()
        );
        log.info("Successfully marked machining operation as deleted and updated workflow step for processed item {}, subtracted {} pieces",
                 processedItemMachiningBatch.getId(), actualMachiningBatchPiecesCount);
      } catch (Exception e) {
        log.error("Failed to update workflow step for deleted machining processed item {}: {}",
                  processedItemMachiningBatch.getId(), e.getMessage());
        throw new RuntimeException("Failed to update workflow step for machining deletion: " + e.getMessage(), e);
      }
    } else {
      log.info("No actual machining pieces to subtract for deleted processed item {}", processedItemMachiningBatch.getId());
    }

    // Return heat quantities to original heats (similar to HeatTreatmentBatchService.deleteHeatTreatmentBatch method)
    LocalDateTime currentTime = LocalDateTime.now();
    if (processedItemMachiningBatch.getMachiningHeats() != null &&
        !processedItemMachiningBatch.getMachiningHeats().isEmpty()) {

      processedItemMachiningBatch.getMachiningHeats().forEach(machiningHeat -> {
        Heat heat = machiningHeat.getHeat();
        int piecesToReturn = machiningHeat.getPiecesUsed();

        // Return pieces to heat inventory based on heat's unit of measurement
        if (heat.getIsInPieces()) {
          // Heat is managed in pieces - return to availablePiecesCount
          int newAvailablePieces = heat.getAvailablePiecesCount() + piecesToReturn;
          heat.setAvailablePiecesCount(newAvailablePieces);
          log.info("Returned {} pieces to heat {} (pieces-based), new available pieces: {}",
                   piecesToReturn, heat.getId(), newAvailablePieces);
        } else {
          throw new IllegalStateException("Machining batch has no pieces!");
        }

        // Persist the updated heat
        rawMaterialHeatService.updateRawMaterialHeat(heat);

        // Soft delete machining heat record
        machiningHeat.setDeleted(true);
        machiningHeat.setDeletedAt(currentTime);

        log.info("Successfully returned {} pieces from heat {} for deleted machining batch processed item {}",
                 piecesToReturn, heat.getId(), processedItemMachiningBatch.getId());
      });
    }
  }

  /**
   * Handle pieces return to previous operation when machining was not the first operation
   */
  private void handlePiecesReturnToPreviousOperation(ProcessedItemMachiningBatch processedItemMachiningBatch,
                                                     Long itemWorkflowId) {
    // This was not the first operation - return pieces to previous operation
    Long previousOperationBatchId = itemWorkflowService.getPreviousOperationBatchId(
        itemWorkflowId,
        WorkflowStep.OperationType.MACHINING,
        processedItemMachiningBatch.getPreviousOperationProcessedItemId()
    );

    if (previousOperationBatchId != null) {
      itemWorkflowService.returnPiecesToSpecificPreviousOperation(
          itemWorkflowId,
          WorkflowStep.OperationType.MACHINING,
          previousOperationBatchId,
          processedItemMachiningBatch.getMachiningBatchPiecesCount(),
          processedItemMachiningBatch.getId()
      );

      log.info("Successfully returned {} pieces from machining back to previous operation {} in workflow {}",
               processedItemMachiningBatch.getMachiningBatchPiecesCount(),
               previousOperationBatchId,
               itemWorkflowId);
    } else {
      log.warn("Could not determine previous operation batch ID for machining batch processed item {}. " +
               "Pieces may not be properly returned to previous operation.", processedItemMachiningBatch.getId());
    }
  }

  /**
   * Phase 3: Soft delete processed item and associated records
   */
  private void softDeleteProcessedItemAndAssociatedRecords(MachiningBatch machiningBatch,
                                                           ProcessedItemMachiningBatch processedItemMachiningBatch) {
    LocalDateTime now = LocalDateTime.now();

    // Soft delete machining heats at processed item level
    softDeleteMachiningHeats(processedItemMachiningBatch, now);

    // Soft delete all associated daily machining batches
    softDeleteDailyMachiningBatches(machiningBatch, now);

    // Soft delete processedItemMachiningBatch
    processedItemMachiningBatch.setDeleted(true);
    processedItemMachiningBatch.setDeletedAt(now);
    processedItemMachiningBatchService.save(processedItemMachiningBatch);
  }

  /**
   * Soft delete machining heats associated with the processed item
   */
  private void softDeleteMachiningHeats(ProcessedItemMachiningBatch processedItemMachiningBatch, LocalDateTime now) {
    if (processedItemMachiningBatch.getMachiningHeats() != null &&
        !processedItemMachiningBatch.getMachiningHeats().isEmpty()) {
      processedItemMachiningBatch.getMachiningHeats().forEach(machiningHeat -> {
        machiningHeat.setDeleted(true);
        machiningHeat.setDeletedAt(now);
      });
    }
  }

  /**
   * Soft delete daily machining batches associated with the machining batch
   */
  private void softDeleteDailyMachiningBatches(MachiningBatch machiningBatch, LocalDateTime now) {
    if (machiningBatch.getDailyMachiningBatch() != null) {
      machiningBatch.getDailyMachiningBatch().forEach(dailyBatch -> {
        dailyBatch.setDeleted(true);
        dailyBatch.setDeletedAt(now);
        dailyMachiningBatchService.save(dailyBatch);
      });
    }
  }

  /**
   * Phase 4: Finalize machining batch deletion
   */
  private void finalizeMachiningBatchDeletion(MachiningBatch machiningBatch, long machiningBatchId) {
    LocalDateTime now = LocalDateTime.now();

    // Store the original batch number and modify the batch number for deletion
    machiningBatch.setOriginalMachiningBatchNumber(machiningBatch.getMachiningBatchNumber());
    machiningBatch.setMachiningBatchNumber(machiningBatch.getMachiningBatchNumber() + "_deleted_" + machiningBatch.getId() + "_" + now.toEpochSecond(java.time.ZoneOffset.UTC));

    // Soft delete MachiningBatch
    machiningBatch.setDeleted(true);
    machiningBatch.setDeletedAt(now);
    machiningBatchRepository.save(machiningBatch);

    log.info("Successfully deleted machining batch={}, original batch number={}", machiningBatchId, machiningBatch.getOriginalMachiningBatchNumber());
  }


  private void validateIfAnyInspectionBatchExistsForMachiningBatch(MachiningBatch machiningBatch) {
    boolean isExists = inspectionBatchRepository.existsByInputProcessedItemMachiningBatchIdAndDeletedFalse(machiningBatch.getProcessedItemMachiningBatch().getId());
    if (isExists) {
      log.error("There exists inspection batch entry for the machiningBatchId={}!", machiningBatch.getId());
      throw new IllegalStateException("There exists inspection batch entry for the machiningBatchId=" + machiningBatch.getId());
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

      // Count unique machine sets and operators from daily machining batches
      Set<Long> batchOperatorIds = new HashSet<>();
      for (DailyMachiningBatch dailyBatch : batch.getDailyMachiningBatch()) {
        // Count unique machine sets
        if (dailyBatch.getMachineSet() != null) {
          machineSetIds.add(dailyBatch.getMachineSet().getId());
        }

        // Count unique operators
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

      // Get machine set name from daily batches (use the first one if multiple)
      String machineSetName = null;
      if (!batch.getDailyMachiningBatch().isEmpty()) {
        DailyMachiningBatch firstDailyBatch = batch.getDailyMachiningBatch().get(0);
        if (firstDailyBatch.getMachineSet() != null) {
          machineSetName = firstDailyBatch.getMachineSet().getMachineSetName();
        }
      }

      // Create batch detail
      MachiningBatchDetailRepresentation detail = MachiningBatchDetailRepresentation.builder()
          .id(batch.getId())
          .machiningBatchNumber(batch.getMachiningBatchNumber())
          .machineSetName(machineSetName)
          .totalPieces(processedItem != null && processedItem.getMachiningBatchPiecesCount() != null ? processedItem.getMachiningBatchPiecesCount() : 0)
          .completedPieces(processedItem != null && processedItem.getActualMachiningBatchPiecesCount() != null ? processedItem.getActualMachiningBatchPiecesCount() : 0)
          .rejectedPieces(processedItem != null && processedItem.getRejectMachiningBatchPiecesCount() != null ? processedItem.getRejectMachiningBatchPiecesCount() : 0)
          .reworkPieces(processedItem != null && processedItem.getReworkPiecesCount() != null ? processedItem.getReworkPiecesCount() : 0)
          .reworkPiecesAvailableForRework(processedItem != null && processedItem.getReworkPiecesCountAvailableForRework() != null ? processedItem.getReworkPiecesCountAvailableForRework() : 0)
          .availablePieces(processedItem != null && processedItem.getAvailableMachiningBatchPiecesCount() != null ? processedItem.getAvailableMachiningBatchPiecesCount() : 0)
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

  /**
   * Get all inspection batches and dispatch batches associated with a machining batch
   *
   * @param machiningBatchId ID of the machining batch
   * @param tenantId         ID of the tenant
   * @return DTO containing both inspection and dispatch batch representations
   */
  public MachiningBatchAssociationsDTO getMachiningBatchAssociations(Long machiningBatchId, Long tenantId) {
    log.info("Getting associations for machining batch ID: {}, tenant ID: {}", machiningBatchId, tenantId);

    // Verify the machining batch exists and belongs to the tenant
    MachiningBatch machiningBatch = getMachiningBatchByIdAndTenantId(machiningBatchId, tenantId);

    // Get machining batch representation
    MachiningBatchRepresentation machiningBatchRepresentation = machiningBatchAssembler.dissemble(machiningBatch);

    // Get inspection batches
    List<InspectionBatchRepresentation> inspectionBatches =
        inspectionBatchService.getInspectionBatchesByMachiningBatchId(machiningBatchId);

    // Get dispatch batches
    List<DispatchBatchRepresentation> dispatchBatches =
        dispatchBatchService.getDispatchBatchesByMachiningBatchId(machiningBatchId);

    return MachiningBatchAssociationsDTO.builder()
        .machiningBatchId(machiningBatchId)
        .machiningBatch(machiningBatchRepresentation)
        .inspectionBatches(inspectionBatches)
        .dispatchBatches(dispatchBatches)
        .build();
  }

  /**
   * Get monthly statistics for machining batches within a date range
   *
   * @param tenantId  The tenant ID
   * @param fromMonth Starting month (1-12)
   * @param fromYear  Starting year
   * @param toMonth   Ending month (1-12)
   * @param toYear    Ending year
   * @return Monthly statistics representation
   */
  public MonthlyMachiningStatisticsRepresentation getMachiningBatchMonthlyStatistics(
      Long tenantId, int fromMonth, int fromYear, int toMonth, int toYear) {

    log.info("Fetching monthly machining statistics for tenant={}, from={}-{}, to={}-{}",
             tenantId, fromYear, fromMonth, toYear, toMonth);

    // Create start and end date time boundaries
    LocalDateTime startDateTime = LocalDateTime.of(fromYear, fromMonth, 1, 0, 0, 0);

    // Set end date to the last day of the month
    LocalDateTime endDateTime = LocalDateTime.of(toYear, toMonth, 1, 23, 59, 59)
        .plusMonths(1).minusDays(1);

    // Validate tenant exists
    tenantService.validateTenantExists(tenantId);

    // Get all machine sets for the tenant
    List<Long> machineSetIds = machineSetService.getAllMachineSetsOfTenant(tenantId)
        .stream()
        .map(MachineSet::getId)
        .collect(Collectors.toList());

    // Get all completed machining batches within the date range
    List<MachiningBatch> completedBatches = machiningBatchRepository
        .findCompletedBatchesInDateRange(machineSetIds, startDateTime, endDateTime);

    // Initialize statistics
    int totalFinished = 0;
    int totalRework = 0;
    int totalRejected = 0;

    // Map to store monthly breakdown
    Map<String, MonthlyMachiningStatisticsRepresentation.MonthlyStatistics> monthlyBreakdown = new HashMap<>();

    // Process each batch
    for (MachiningBatch batch : completedBatches) {
      // Get the month key (YYYY-MM)
      String monthKey = getMonthKey(batch.getEndAt());

      // Initialize monthly statistics if not exists
      if (!monthlyBreakdown.containsKey(monthKey)) {
        monthlyBreakdown.put(monthKey,
                             MonthlyMachiningStatisticsRepresentation.MonthlyStatistics.builder()
                                 .finished(0)
                                 .rework(0)
                                 .rejected(0)
                                 .build());
      }

      // Get current statistics
      MonthlyMachiningStatisticsRepresentation.MonthlyStatistics monthStats = monthlyBreakdown.get(monthKey);

      // Get processed item machining batch
      ProcessedItemMachiningBatch processedItem = batch.getProcessedItemMachiningBatch();
      if (processedItem != null) {
        // Get counts (safely handle nulls)
        int actualFinished = processedItem.getActualMachiningBatchPiecesCount() != null
                             ? processedItem.getActualMachiningBatchPiecesCount() : 0;
        int rejected = processedItem.getRejectMachiningBatchPiecesCount() != null
                       ? processedItem.getRejectMachiningBatchPiecesCount() : 0;
        int rework = processedItem.getReworkPiecesCount() != null
                     ? processedItem.getReworkPiecesCount() : 0;

        // Update monthly statistics
        monthStats.setFinished(monthStats.getFinished() + actualFinished);
        monthStats.setRework(monthStats.getRework() + rework);
        monthStats.setRejected(monthStats.getRejected() + rejected);

        // Update totals
        totalFinished += actualFinished;
        totalRework += rework;
        totalRejected += rejected;
      }
    }

    // Build and return the response
    return MonthlyMachiningStatisticsRepresentation.builder()
        .totalFinished(totalFinished)
        .totalRework(totalRework)
        .totalRejected(totalRejected)
        .monthlyBreakdown(monthlyBreakdown)
        .build();
  }

  /**
   * Helper method to get month key in YYYY-MM format
   */
  private String getMonthKey(LocalDateTime dateTime) {
    return String.format("%d-%02d", dateTime.getYear(), dateTime.getMonthValue());
  }

  public boolean isMachiningBatchNumberPreviouslyUsed(String machiningBatchNumber, Long tenantId) {
    return machiningBatchRepository.existsByMachiningBatchNumberAndTenantIdAndOriginalMachiningBatchNumber(
        machiningBatchNumber, tenantId);
  }

  /**
   * Search for machining batches by various criteria with pagination
   *
   * @param tenantId   The tenant ID
   * @param searchType The type of search (ITEM_NAME, FORGE_TRACEABILITY_NUMBER, or MACHINING_BATCH_NUMBER)
   * @param searchTerm The search term (substring matching for all search types)
   * @param page       The page number (0-based)
   * @param size       The page size
   * @return Page of MachiningBatchRepresentation containing the search results
   */
  @Transactional(readOnly = true)
  public Page<MachiningBatchRepresentation> searchMachiningBatches(Long tenantId, String searchType, String searchTerm, int page, int size) {
    if (searchTerm == null || searchTerm.trim().isEmpty() || searchType == null || searchType.trim().isEmpty()) {
      Pageable pageable = PageRequest.of(page, size);
      return Page.empty(pageable);
    }

    tenantService.validateTenantExists(tenantId);
    Pageable pageable = PageRequest.of(page, size);
    Page<MachiningBatch> machiningBatchPage;

    switch (searchType.toUpperCase()) {
      case "ITEM_NAME":
        machiningBatchPage = machiningBatchRepository.findMachiningBatchesByItemNameContainingIgnoreCase(tenantId, searchTerm.trim(), pageable);
        break;
      case "FORGE_TRACEABILITY_NUMBER":
        machiningBatchPage = machiningBatchRepository.findMachiningBatchesByForgeTraceabilityNumberContainingIgnoreCase(tenantId, searchTerm.trim(), pageable);
        break;
      case "MACHINING_BATCH_NUMBER":
        machiningBatchPage = machiningBatchRepository.findMachiningBatchesByMachiningBatchNumberContainingIgnoreCase(tenantId, searchTerm.trim(), pageable);
        break;
      default:
        log.error("Invalid search type: {}", searchType);
        throw new IllegalArgumentException("Invalid search type: " + searchType + ". Valid types are: ITEM_NAME, FORGE_TRACEABILITY_NUMBER, MACHINING_BATCH_NUMBER");
    }

    return machiningBatchPage.map(machiningBatchAssembler::dissemble);
  }

  /**
   * Retrieves machining batches by multiple processed item machining batch IDs and validates they belong to the tenant
   *
   * @param processedItemMachiningBatchIds List of processed item machining batch IDs
   * @param tenantId                       The tenant ID for validation
   * @return List of MachiningBatchRepresentation (distinct machining batches)
   */
  public List<MachiningBatchRepresentation> getMachiningBatchesByProcessedItemMachiningBatchIds(List<Long> processedItemMachiningBatchIds, Long tenantId) {
    if (processedItemMachiningBatchIds == null || processedItemMachiningBatchIds.isEmpty()) {
      log.info("No processed item machining batch IDs provided, returning empty list");
      return Collections.emptyList();
    }

    log.info("Getting machining batches for {} processed item machining batch IDs for tenant {}", processedItemMachiningBatchIds.size(), tenantId);

    List<MachiningBatch> machiningBatches = machiningBatchRepository.findByProcessedItemMachiningBatchIdInAndDeletedFalse(processedItemMachiningBatchIds);

    // Use a Set to track processed machining batch IDs to avoid duplicates
    Set<Long> processedMachiningBatchIds = new HashSet<>();
    List<MachiningBatchRepresentation> validMachiningBatches = new ArrayList<>();
    List<Long> invalidProcessedItemMachiningBatchIds = new ArrayList<>();

    for (Long processedItemMachiningBatchId : processedItemMachiningBatchIds) {
      Optional<MachiningBatch> machiningBatchOpt = machiningBatches.stream()
          .filter(mb -> mb.getProcessedItemMachiningBatch() != null &&
                        mb.getProcessedItemMachiningBatch().getId().equals(processedItemMachiningBatchId))
          .findFirst();

      if (machiningBatchOpt.isPresent()) {
        MachiningBatch machiningBatch = machiningBatchOpt.get();
        if (Long.valueOf(machiningBatch.getTenant().getId()).equals(tenantId)) {
          // Only add if we haven't already processed this machining batch
          if (!processedMachiningBatchIds.contains(machiningBatch.getId())) {
            validMachiningBatches.add(machiningBatchAssembler.dissemble(machiningBatch));
            processedMachiningBatchIds.add(machiningBatch.getId());
          }
        } else {
          log.warn("MachiningBatch for processedItemMachiningBatchId={} does not belong to tenant={}", processedItemMachiningBatchId, tenantId);
          invalidProcessedItemMachiningBatchIds.add(processedItemMachiningBatchId);
        }
      } else {
        log.warn("No machining batch found for processedItemMachiningBatchId={}", processedItemMachiningBatchId);
        invalidProcessedItemMachiningBatchIds.add(processedItemMachiningBatchId);
      }
    }

    if (!invalidProcessedItemMachiningBatchIds.isEmpty()) {
      log.warn("The following processed item machining batch IDs did not have valid machining batches for tenant {}: {}",
               tenantId, invalidProcessedItemMachiningBatchIds);
    }

    log.info("Found {} distinct valid machining batches out of {} requested processed item machining batch IDs", validMachiningBatches.size(), processedItemMachiningBatchIds.size());
    return validMachiningBatches;
  }


  public MachiningBatchRepresentation getMachiningBatchByProcessedItemMachiningBatchId(Long processedItemMachiningBatchId, Long tenantId) {
    if (processedItemMachiningBatchId == null) {
      log.info("No processed item machining batch ID provided, returning null");
      return null;
    }

    Optional<MachiningBatch> machiningBatchOptional = machiningBatchRepository.findByProcessedItemMachiningBatchIdAndDeletedFalse(processedItemMachiningBatchId);

    if (machiningBatchOptional.isPresent()) {
      MachiningBatch machiningBatch = machiningBatchOptional.get();
      return machiningBatchAssembler.dissemble(machiningBatch);
    } else {
      log.error("No machining batch found for processedItemMachiningBatchId={}", processedItemMachiningBatchId);
      throw new RuntimeException("No machining batch found for processedItemMachiningBatchId=" + processedItemMachiningBatchId);
    }

  }


  /**
   * Deletes the last daily machining batch from a machining batch with comprehensive validation and reversion
   *
   * @param tenantId              The tenant ID for validation
   * @param machiningBatchId      The machining batch ID
   * @param dailyMachiningBatchId The daily machining batch ID to delete
   * @throws Exception if deletion is not allowed or fails
   */
  @Transactional(rollbackFor = Exception.class)
  public void deleteDailyMachiningBatch(long tenantId, long machiningBatchId, long dailyMachiningBatchId) throws Exception {
    log.info("Starting daily machining batch deletion transaction for tenant: {}, machining batch: {}, daily batch: {}",
             tenantId, machiningBatchId, dailyMachiningBatchId);

    try {
      // Phase 1: Initial validations
      validateTenantAndMachiningBatch(tenantId, machiningBatchId);

      // Phase 2: Get and validate daily machining batch
      DailyMachiningBatch dailyMachiningBatchToDelete = validateDailyMachiningBatchForDeletion(
          machiningBatchId, dailyMachiningBatchId);

      MachiningBatch machiningBatch = dailyMachiningBatchToDelete.getMachiningBatch();

      // Phase 3: Validate deletion eligibility
      validateDeletionEligibility(machiningBatch, dailyMachiningBatchToDelete);

      // Phase 4: Revert all operations performed during dailyMachiningBatchUpdate
      revertDailyMachiningBatchOperations(machiningBatch, dailyMachiningBatchToDelete);

      log.info("Successfully completed daily machining batch deletion transaction for ID: {}", dailyMachiningBatchId);

    } catch (Exception e) {
      log.error("Daily machining batch deletion transaction failed for tenant: {}, batch: {}, daily batch: {}. " +
                "All changes will be rolled back. Error: {}",
                tenantId, machiningBatchId, dailyMachiningBatchId, e.getMessage());

      // Re-throw to ensure transaction rollback
      throw e;
    }
  }

  /**
   * Phase 1: Validate tenant and machining batch existence
   */
  private void validateTenantAndMachiningBatch(long tenantId, long machiningBatchId) {
    tenantService.validateTenantExists(tenantId);

    // Validate machining batch exists and belongs to tenant
    MachiningBatch machiningBatch = getMachiningBatchByIdAndTenantId(machiningBatchId, tenantId);
    if (machiningBatch == null) {
      throw new MachiningBatchNotFoundException("MachiningBatch does not exist for machiningBatchId=" + machiningBatchId);
    }
  }

  /**
   * Phase 2: Get and validate daily machining batch for deletion
   */
  private DailyMachiningBatch validateDailyMachiningBatchForDeletion(long machiningBatchId, long dailyMachiningBatchId) {
    DailyMachiningBatch dailyMachiningBatch = dailyMachiningBatchService.getDailyMachiningBatchById(dailyMachiningBatchId);

    // Validate that the daily batch belongs to the specified machining batch
    if (!dailyMachiningBatch.getMachiningBatch().getId().equals(machiningBatchId)) {
      log.error("Daily machining batch {} does not belong to machining batch {}",
                dailyMachiningBatchId, machiningBatchId);
      throw new IllegalArgumentException("Daily machining batch does not belong to the specified machining batch");
    }

    return dailyMachiningBatch;
  }

  /**
   * Phase 3: Validate deletion eligibility based on business rules
   */
  private void validateDeletionEligibility(MachiningBatch machiningBatch, DailyMachiningBatch dailyMachiningBatchToDelete) {
    // Rule 1: Must be the last daily machining batch
    validateIsLastDailyMachiningBatch(machiningBatch, dailyMachiningBatchToDelete);

    // Rule 2: Machining batch must not be completed
    validateMachiningBatchNotCompleted(machiningBatch);

    // Rule 3: Next operation must not have consumed pieces from this machining batch
    validateNextOperationNotStarted(machiningBatch);
  }

  /**
   * Rule 1: Validate that this is the last daily machining batch
   */
  private void validateIsLastDailyMachiningBatch(MachiningBatch machiningBatch, DailyMachiningBatch dailyMachiningBatchToDelete) {
    List<DailyMachiningBatch> dailyBatches = machiningBatch.getDailyMachiningBatch();

    if (dailyBatches == null || dailyBatches.isEmpty()) {
      throw new IllegalStateException("No daily machining batches found for machining batch " + machiningBatch.getId());
    }

    // Sort by creation time to get the last one
    DailyMachiningBatch lastDailyBatch = dailyBatches.stream()
        .filter(batch -> !batch.isDeleted())
        .max((batch1, batch2) -> batch1.getCreatedAt().compareTo(batch2.getCreatedAt()))
        .orElse(null);

    if (lastDailyBatch == null || !lastDailyBatch.getId().equals(dailyMachiningBatchToDelete.getId())) {
      log.error("Cannot delete daily machining batch {} as it is not the last one. Last batch ID: {}",
                dailyMachiningBatchToDelete.getId(),
                lastDailyBatch != null ? lastDailyBatch.getId() : "none");
      throw new IllegalStateException("Only the last daily machining batch can be deleted. " +
                                      "This daily batch is not the most recent one.");
    }
  }

  /**
   * Rule 2: Validate that machining batch is not completed
   */
  private void validateMachiningBatchNotCompleted(MachiningBatch machiningBatch) {
    if (MachiningBatch.MachiningBatchStatus.COMPLETED.equals(machiningBatch.getMachiningBatchStatus())) {
      log.error("Cannot delete daily machining batch from completed machining batch {}", machiningBatch.getId());
      throw new IllegalStateException("Cannot delete daily machining batch from a completed machining batch. " +
                                      "Machining batch status: " + machiningBatch.getMachiningBatchStatus());
    }
  }

  /**
   * Rule 3: Validate that next operation has not consumed pieces from this machining batch
   */
  private void validateNextOperationNotStarted(MachiningBatch machiningBatch) {
    ProcessedItemMachiningBatch processedItem = machiningBatch.getProcessedItemMachiningBatch();
    Long itemWorkflowId = processedItem.getItemWorkflowId();

    if (itemWorkflowId == null) {
      log.warn("No workflow ID found for machining batch {}. Skipping next operation validation.", machiningBatch.getId());
      return;
    }

    try {
      // Check if next operation has consumed pieces from this machining batch
      boolean nextOperationStarted = itemWorkflowService.areAllNextOperationBatchesDeleted(
          itemWorkflowId, processedItem.getId(), WorkflowStep.OperationType.MACHINING);

      if (!nextOperationStarted) {
        log.error("Cannot delete daily machining batch from machining batch {} as next operation has consumed pieces",
                  machiningBatch.getId());
        throw new IllegalStateException("Cannot delete Machining Shift as the next operation in the workflow " +
                                        "has already started and consumed pieces from this machining batch. " +
                                        "Delete the next operation first.");
      }
    } catch (Exception e) {
      log.error("Error validating next operation for machining batch {}: {}", machiningBatch.getId(), e.getMessage());
      throw new RuntimeException("Failed to validate next operation status: " + e.getMessage(), e);
    }
  }

  /**
   * Phase 4: Revert all operations performed during dailyMachiningBatchUpdate
   */
  private void revertDailyMachiningBatchOperations(MachiningBatch machiningBatch, DailyMachiningBatch dailyMachiningBatchToDelete) {
    log.info("Reverting daily machining batch operations for daily batch ID: {}", dailyMachiningBatchToDelete.getId());

    // Step 1: Revert piece counts in ProcessedItemMachiningBatch
    revertProcessedItemPieceCounts(machiningBatch, dailyMachiningBatchToDelete);

    // Step 2: Handle first daily batch specific reversions
    boolean wasFirstDailyBatch = handleFirstDailyBatchReversion(machiningBatch, dailyMachiningBatchToDelete);

    // Step 3: Revert machine operator relationships
    revertMachineOperatorRelationships(dailyMachiningBatchToDelete);

    // Step 4: Revert machine set relationships and availability
    revertMachineSetRelationships(dailyMachiningBatchToDelete, machiningBatch, wasFirstDailyBatch);

    // Step 5: Revert workflow integration
    revertWorkflowIntegration(machiningBatch, dailyMachiningBatchToDelete, wasFirstDailyBatch);

    // Step 6: Remove daily batch from machining batch and soft delete it
    removeDailyBatchFromMachiningBatch(dailyMachiningBatchToDelete);

    // Step 7: Save the updated machining batch
    machiningBatchRepository.save(machiningBatch);

    log.info("Successfully reverted all daily machining batch operations for daily batch ID: {}",
             dailyMachiningBatchToDelete.getId());
  }

  /**
   * Step 1: Revert piece counts in ProcessedItemMachiningBatch
   */
  private void revertProcessedItemPieceCounts(MachiningBatch machiningBatch, DailyMachiningBatch dailyMachiningBatchToDelete) {
    ProcessedItemMachiningBatch processedItem = machiningBatch.getProcessedItemMachiningBatch();

    int dailyCompletedPieces = dailyMachiningBatchToDelete.getCompletedPiecesCount();
    int dailyRejectedPieces = dailyMachiningBatchToDelete.getRejectedPiecesCount();
    int dailyReworkPieces = dailyMachiningBatchToDelete.getReworkPiecesCount();

    // Revert actual machining batch pieces count
    int currentActualPieces = processedItem.getActualMachiningBatchPiecesCount() != null
                              ? processedItem.getActualMachiningBatchPiecesCount() : 0;
    processedItem.setActualMachiningBatchPiecesCount(Math.max(0, currentActualPieces - dailyCompletedPieces));

    // Revert reject pieces count
    int currentRejectPieces = processedItem.getRejectMachiningBatchPiecesCount() != null
                              ? processedItem.getRejectMachiningBatchPiecesCount() : 0;
    processedItem.setRejectMachiningBatchPiecesCount(Math.max(0, currentRejectPieces - dailyRejectedPieces));

    // Revert rework pieces count
    int currentReworkPieces = processedItem.getReworkPiecesCount() != null
                              ? processedItem.getReworkPiecesCount() : 0;
    processedItem.setReworkPiecesCount(Math.max(0, currentReworkPieces - dailyReworkPieces));

    // Revert rework pieces available for rework
    int currentReworkAvailable = processedItem.getReworkPiecesCountAvailableForRework() != null
                                 ? processedItem.getReworkPiecesCountAvailableForRework() : 0;
    processedItem.setReworkPiecesCountAvailableForRework(Math.max(0, currentReworkAvailable - dailyReworkPieces));

    // Revert available machining batch pieces count
    int currentAvailable = processedItem.getAvailableMachiningBatchPiecesCount() != null
                           ? processedItem.getAvailableMachiningBatchPiecesCount() : 0;
    int totalDailyPieces = dailyCompletedPieces + dailyRejectedPieces + dailyReworkPieces;
    processedItem.setAvailableMachiningBatchPiecesCount(currentAvailable + totalDailyPieces);

    log.info("Reverted piece counts: completed={}, rejected={}, rework={}, total reverted={}",
             dailyCompletedPieces, dailyRejectedPieces, dailyReworkPieces, totalDailyPieces);
  }

  /**
   * Step 2: Handle first daily batch specific reversions
   */
  private boolean handleFirstDailyBatchReversion(MachiningBatch machiningBatch, DailyMachiningBatch dailyMachiningBatchToDelete) {
    List<DailyMachiningBatch> activeDailyBatches = machiningBatch.getDailyMachiningBatch().stream()
        .filter(batch -> !batch.isDeleted())
        .sorted((batch1, batch2) -> batch1.getCreatedAt().compareTo(batch2.getCreatedAt()))
        .collect(Collectors.toList());

    boolean wasFirstDailyBatch = !activeDailyBatches.isEmpty() &&
                                 activeDailyBatches.get(0).getId().equals(dailyMachiningBatchToDelete.getId());

    if (wasFirstDailyBatch) {
      log.info("Reverting first daily batch operations for machining batch {}", machiningBatch.getId());

      // Check if this was the only daily batch
      if (activeDailyBatches.size() == 1) {
        // This was the only daily batch - revert machining batch to IDLE status and clear startAt
        machiningBatch.setMachiningBatchStatus(MachiningBatch.MachiningBatchStatus.IDLE);
        machiningBatch.setStartAt(null);

        // Revert processed item status
        machiningBatch.getProcessedItemMachiningBatch().setItemStatus(ItemStatus.MACHINING_NOT_STARTED);

        log.info("Reverted machining batch {} to IDLE status as this was the only daily batch", machiningBatch.getId());
      }
    } else {
      // This is not the first daily batch - no changes needed to machining batch startAt
      // The startAt time was set by the first daily batch and should remain unchanged
      log.info("Daily batch {} is not the first daily batch for machining batch {}. " +
               "No changes needed to machining batch startAt time.",
               dailyMachiningBatchToDelete.getId(), machiningBatch.getId());
    }

    return wasFirstDailyBatch;
  }

  /**
   * Step 3: Revert machine operator relationships
   */
  private void revertMachineOperatorRelationships(DailyMachiningBatch dailyMachiningBatchToDelete) {
    MachineOperator machineOperator = dailyMachiningBatchToDelete.getMachineOperator();

    if (machineOperator != null) {
      // Remove this daily batch from machine operator's list
      machineOperator.getDailyMachiningBatches().removeIf(batch ->
                                                              batch.getId().equals(dailyMachiningBatchToDelete.getId()));

      machineOperatorService.save(machineOperator);

      log.info("Removed daily batch {} from machine operator {} relationships",
               dailyMachiningBatchToDelete.getId(), machineOperator.getId());
    }
  }

  /**
   * Step 4: Revert machine set relationships and make it available for other machining batches
   * When a daily machining batch is deleted, its associated machine set should become available
   * for other machining batches to use during the same time period.
   */
  private void revertMachineSetRelationships(DailyMachiningBatch dailyMachiningBatchToDelete,
                                             MachiningBatch machiningBatch,
                                             boolean wasFirstDailyBatch) {
    MachineSet machineSet = dailyMachiningBatchToDelete.getMachineSet();

    if (machineSet == null) {
      log.warn("No machine set associated with daily machining batch {}", dailyMachiningBatchToDelete.getId());
      return;
    }
    machineSetService.updateMachineSetRunningJobType(machineSet, MachineSet.MachineSetRunningJobType.NONE);

    log.info("Successfully handled machine set {} relationships for deleted daily batch {}",
             machineSet.getId(), dailyMachiningBatchToDelete.getId());
  }

  /**
   * Step 5: Revert workflow integration
   */
  private void revertWorkflowIntegration(MachiningBatch machiningBatch, DailyMachiningBatch dailyMachiningBatchToDelete, boolean wasFirstDailyBatch) {
    ProcessedItemMachiningBatch processedItem = machiningBatch.getProcessedItemMachiningBatch();
    Long itemWorkflowId = processedItem.getItemWorkflowId();

    if (itemWorkflowId == null) {
      log.warn("No workflow ID found for machining batch {}. Skipping workflow reversion.", machiningBatch.getId());
      return;
    }

    try {
      ItemWorkflowStep machiningItemWorkflowStep = getMachiningWorkflowStep(itemWorkflowId, processedItem.getId());
      List<OperationOutcomeData.BatchOutcome> existingBatchData = itemWorkflowService.extractExistingBatchData(machiningItemWorkflowStep);

      int dailyCompletedPieces = dailyMachiningBatchToDelete.getCompletedPiecesCount();

      // Find and update the specific batch outcome for this machining batch
      boolean batchFound = false;
      for (OperationOutcomeData.BatchOutcome batchOutcome : existingBatchData) {
        if (Objects.equals(batchOutcome.getId(), processedItem.getId())) {
          // Revert batch outcome by subtracting the daily finished pieces
          int currentInitialPieces = batchOutcome.getInitialPiecesCount() != null ? batchOutcome.getInitialPiecesCount() : 0;
          int currentAvailablePieces = batchOutcome.getPiecesAvailableForNext() != null ? batchOutcome.getPiecesAvailableForNext() : 0;

          // Subtract the daily finished pieces
          batchOutcome.setInitialPiecesCount(Math.max(0, currentInitialPieces - dailyCompletedPieces));
          batchOutcome.setPiecesAvailableForNext(Math.max(0, currentAvailablePieces - dailyCompletedPieces));

          // If this was the first daily batch and now there are no more daily batches, clear startedAt
          if (wasFirstDailyBatch) {
            List<DailyMachiningBatch> remainingBatches = machiningBatch.getDailyMachiningBatch().stream()
                .filter(batch -> !batch.isDeleted() && !batch.getId().equals(dailyMachiningBatchToDelete.getId()))
                .toList();

            if (remainingBatches.isEmpty()) {
              batchOutcome.setStartedAt(null);
              log.info("Cleared startedAt for machining batch outcome as this was the only daily batch");
            }
          }

          batchOutcome.setUpdatedAt(LocalDateTime.now());
          batchFound = true;

          log.info("Reverted machining batch outcome: ID={}, initialPieces={}, availablePieces={}, dailyDecrement={}",
                   processedItem.getId(), batchOutcome.getInitialPiecesCount(),
                   batchOutcome.getPiecesAvailableForNext(), dailyCompletedPieces);
          break;
        }
      }

      if (batchFound) {
        // Update workflow step with reverted batch data
        itemWorkflowService.updateWorkflowStepForOperation(machiningItemWorkflowStep,
                                                           OperationOutcomeData.forMachiningOperation(existingBatchData, LocalDateTime.now()));

        log.info("Successfully reverted workflow integration for machining batch {}", machiningBatch.getId());
      } else {
        log.warn("Failed to find batch data for workflow {} during daily machining batch deletion {}",
                 itemWorkflowId, dailyMachiningBatchToDelete.getId());
      }

    } catch (Exception e) {
      log.error("Failed to revert workflow integration for daily machining batch deletion {}: {}",
                dailyMachiningBatchToDelete.getId(), e.getMessage());
      throw new RuntimeException("Failed to revert workflow integration: " + e.getMessage(), e);
    }
  }

  /**
   * Step 6: Remove daily batch from machining batch and soft delete it
   */
  private void removeDailyBatchFromMachiningBatch(DailyMachiningBatch dailyMachiningBatchToDelete) {
    LocalDateTime now = LocalDateTime.now();

    // Store the original daily batch number and modify it for deletion to avoid unique constraint conflicts
    String originalDailyBatchNumber = dailyMachiningBatchToDelete.getDailyMachiningBatchNumber();
    String deletedDailyBatchNumber = originalDailyBatchNumber + "_deleted_" + dailyMachiningBatchToDelete.getId() + "_" + now.toEpochSecond(java.time.ZoneOffset.UTC);

    dailyMachiningBatchToDelete.setDailyMachiningBatchNumber(deletedDailyBatchNumber);

    // Soft delete the daily machining batch
    dailyMachiningBatchToDelete.setDeleted(true);
    dailyMachiningBatchToDelete.setDeletedAt(now);

    // Save the daily batch
    dailyMachiningBatchService.save(dailyMachiningBatchToDelete);

    log.info("Soft deleted daily machining batch {} (original number: {}, deleted number: {})",
             dailyMachiningBatchToDelete.getId(), originalDailyBatchNumber, deletedDailyBatchNumber);
  }

  /**
   * Updates workflow when daily machining batch is completed
   * Similar to updateWorkflowForForgeShift in ForgeService
   */
  private void updateWorkflowForDailyMachiningBatchUpdate(MachiningBatch machiningBatch, int dailyActualFinishedPieces) {
    try {
      // Validate workflow and get ItemWorkflow
      ItemWorkflow workflow = validateAndGetWorkflow(machiningBatch);

      ProcessedItemMachiningBatch processedItemMachiningBatch = machiningBatch.getProcessedItemMachiningBatch();

      // Check if this is the first daily machining batch by looking at existing active daily batches
      boolean isFirstDailyBatch = false;
      LocalDateTime firstDailyBatchStartTime = null;

      if (machiningBatch.getDailyMachiningBatch() != null && !machiningBatch.getDailyMachiningBatch().isEmpty()) {
        // Filter out deleted daily batches to get only active ones
        List<DailyMachiningBatch> activeDailyBatches = machiningBatch.getDailyMachiningBatch().stream()
            .filter(batch -> !batch.isDeleted())
            .sorted((batch1, batch2) -> batch1.getCreatedAt().compareTo(batch2.getCreatedAt()))
            .collect(Collectors.toList());

        if (!activeDailyBatches.isEmpty()) {
          // Get the first active daily machining batch start time
          DailyMachiningBatch firstDailyBatch = activeDailyBatches.get(0);
          firstDailyBatchStartTime = firstDailyBatch.getStartDateTime();

          // This is the first daily batch if we only have one active daily batch
          isFirstDailyBatch = activeDailyBatches.size() == 1;

          log.info("Workflow update for machining batch {}: activeDailyBatches={}, isFirstDailyBatch={}, firstDailyBatchStartTime={}",
                   machiningBatch.getId(), activeDailyBatches.size(), isFirstDailyBatch, firstDailyBatchStartTime);
        }
      }

      // Get existing machining outcome data
      ItemWorkflowStep machiningItemWorkflowStep = getMachiningWorkflowStep(
          workflow.getId(), processedItemMachiningBatch.getId());

      List<OperationOutcomeData.BatchOutcome> existingBatchData = itemWorkflowService.extractExistingBatchData(machiningItemWorkflowStep);

      // Find and update the specific batch outcome for this machining batch
      boolean batchFound = false;
      for (OperationOutcomeData.BatchOutcome batchOutcome : existingBatchData) {
        if (Objects.equals(batchOutcome.getId(), processedItemMachiningBatch.getId())) {
          // Update batch outcome with incremental pieces
          int currentInitialPieces = batchOutcome.getInitialPiecesCount() != null ? batchOutcome.getInitialPiecesCount() : 0;
          int currentAvailablePieces = batchOutcome.getPiecesAvailableForNext() != null ? batchOutcome.getPiecesAvailableForNext() : 0;

          // Increment initial pieces by the daily finished pieces
          batchOutcome.setInitialPiecesCount(currentInitialPieces + dailyActualFinishedPieces);

          // Increment available pieces by the daily finished pieces
          batchOutcome.setPiecesAvailableForNext(currentAvailablePieces + dailyActualFinishedPieces);

          // Set startedAt ONLY if this is the first daily machining batch AND startedAt is not already set
          if (isFirstDailyBatch && batchOutcome.getStartedAt() == null && firstDailyBatchStartTime != null) {
            batchOutcome.setStartedAt(firstDailyBatchStartTime);
            log.info("Setting startedAt for first daily machining batch outcome: ID={}, startedAt={}",
                     processedItemMachiningBatch.getId(), firstDailyBatchStartTime);
          }

          batchOutcome.setUpdatedAt(LocalDateTime.now());
          batchFound = true;

          log.info("Updated machining batch outcome: ID={}, initialPieces={}, availablePieces={}, dailyIncrement={}, isFirstDaily={}, startedAt={}",
                   processedItemMachiningBatch.getId(), batchOutcome.getInitialPiecesCount(),
                   batchOutcome.getPiecesAvailableForNext(), dailyActualFinishedPieces, isFirstDailyBatch, batchOutcome.getStartedAt());
          break;
        }
      }

      // Update workflow step with all batch data (preserving existing batches)
      itemWorkflowService.updateWorkflowStepForOperation(machiningItemWorkflowStep, OperationOutcomeData.forMachiningOperation(existingBatchData, LocalDateTime.now()));

      if (batchFound) {
        log.info("Successfully updated workflow {} with daily machining batch data: {} total pieces, {} daily increment, isFirstDaily={}",
                 workflow.getId(), dailyActualFinishedPieces, dailyActualFinishedPieces, isFirstDailyBatch);
      } else {
        log.warn("Failed to find existing batch data for workflow {} for daily machining batch update {}",
                 workflow.getId(), machiningBatch.getId());
      }

    } catch (Exception e) {
      // Re-throw the exception to fail the daily machining batch update since workflow integration is now mandatory
      log.error("Failed to update workflow for daily machining batch update on machining batch ID={}: {}. Failing daily machining batch update.",
                machiningBatch.getId(), e.getMessage());
      throw new RuntimeException("Failed to update workflow for daily machining batch update: " + e.getMessage(), e);
    }
  }
}
