package com.jangid.forging_process_management_service.service.heating;

import com.jangid.forging_process_management_service.assemblers.heating.HeatTreatmentBatchAssembler;
import com.jangid.forging_process_management_service.assemblers.heating.ProcessedItemHeatTreatmentBatchAssembler;
import com.jangid.forging_process_management_service.dto.workflow.OperationOutcomeData;
import com.jangid.forging_process_management_service.entities.ProcessedItem;
import com.jangid.forging_process_management_service.entities.Tenant;
import com.jangid.forging_process_management_service.entities.forging.Furnace;
import com.jangid.forging_process_management_service.entities.heating.HeatTreatmentBatch;
import com.jangid.forging_process_management_service.entities.heating.ProcessedItemHeatTreatmentBatch;
import com.jangid.forging_process_management_service.entities.inventory.Heat;
import com.jangid.forging_process_management_service.entities.product.ItemStatus;
import com.jangid.forging_process_management_service.entities.product.Item;
import com.jangid.forging_process_management_service.entities.workflow.ItemWorkflow;
import com.jangid.forging_process_management_service.entities.workflow.ItemWorkflowStep;
import com.jangid.forging_process_management_service.entities.workflow.WorkflowStep;
import com.jangid.forging_process_management_service.entitiesRepresentation.heating.HeatTreatmentBatchNotInExpectedStatusException;
import com.jangid.forging_process_management_service.entitiesRepresentation.heating.HeatTreatmentBatchRepresentation;
import com.jangid.forging_process_management_service.dto.HeatTreatmentBatchAssociationsDTO;
import com.jangid.forging_process_management_service.dto.MachiningBatchAssociationsDTO;
import com.jangid.forging_process_management_service.entitiesRepresentation.heating.HeatTreatmentHeatRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.heating.ProcessedItemHeatTreatmentBatchRepresentation;
import com.jangid.forging_process_management_service.exception.ResourceNotFoundException;
import com.jangid.forging_process_management_service.exception.forging.ForgeNotFoundException;
import com.jangid.forging_process_management_service.exception.heating.FurnaceOccupiedException;
import com.jangid.forging_process_management_service.exception.heating.HeatTreatmentBatchNotFoundException;
import com.jangid.forging_process_management_service.repositories.heating.HeatTreatmentBatchRepository;
import com.jangid.forging_process_management_service.repositories.heating.ProcessedItemHeatTreatmentBatchRepository;
import com.jangid.forging_process_management_service.service.TenantService;
import com.jangid.forging_process_management_service.service.inventory.RawMaterialService;
import com.jangid.forging_process_management_service.service.machining.MachiningBatchService;
import com.jangid.forging_process_management_service.utils.ConvertorUtils;
import com.jangid.forging_process_management_service.service.workflow.ItemWorkflowService;
import com.jangid.forging_process_management_service.service.inventory.RawMaterialHeatService;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Service
public class HeatTreatmentBatchService {

  @Autowired
  private HeatTreatmentBatchRepository heatTreatmentBatchRepository;
  @Autowired
  private TenantService tenantService;
  @Autowired
  private FurnaceService furnaceService;
  @Autowired
  private HeatTreatmentBatchAssembler heatTreatmentBatchAssembler;
  @Autowired
  private MachiningBatchService machiningBatchService;
  @Autowired
  private ItemWorkflowService itemWorkflowService;
  @Autowired
  private ProcessedItemHeatTreatmentBatchAssembler processedItemHeatTreatmentBatchAssembler;
  @Autowired
  private ObjectMapper objectMapper;
  @Autowired
  private RawMaterialHeatService rawMaterialHeatService;
  @Autowired
  private ProcessedItemHeatTreatmentBatchRepository processedItemHeatTreatmentBatchRepository;

  @Transactional
  public HeatTreatmentBatchRepresentation applyHeatTreatmentBatch(long tenantId, long furnaceId, HeatTreatmentBatchRepresentation representation) {
    // Phase 1: Initial validations
    validateTenantAndBatchNumber(tenantId, representation);

    // Phase 2: Validate pieces and furnace availability
    Furnace furnace = validatePiecesCountAndFurnaceAvailability(tenantId, furnaceId, representation);

    // Phase 3: Setup heat treatment batch
    HeatTreatmentBatch inputHeatTreatmentBatch = setupHeatTreatmentBatch(tenantId, furnace, representation);

    // Phase 4: Process each item in the batch
    processHeatTreatmentBatchItems(representation, inputHeatTreatmentBatch);

    // Phase 5: Finalize and save
    return finalizeHeatTreatmentBatch(inputHeatTreatmentBatch, furnace, representation);
  }

  /**
   * Phase 1: Validate tenant existence and batch number uniqueness
   */
  private void validateTenantAndBatchNumber(long tenantId, HeatTreatmentBatchRepresentation representation) {
    tenantService.validateTenantExists(tenantId);

    boolean exists = heatTreatmentBatchRepository.existsByHeatTreatmentBatchNumberAndTenantIdAndDeletedFalse(
        representation.getHeatTreatmentBatchNumber(), tenantId);
    if (exists) {
      log.error("Heat Treatment Batch with batch number: {} already exists with the tenant: {}!",
                representation.getHeatTreatmentBatchNumber(), tenantId);
      throw new IllegalStateException("Heat Treatment Batch with batch number =" + representation.getHeatTreatmentBatchNumber()
                                      + "with the tenant =" + tenantId);
    }

    // Check if this batch number was previously used and deleted
    if (isHeatTreatmentBatchNumberPreviouslyUsed(representation.getHeatTreatmentBatchNumber(), tenantId)) {
      log.warn("Heat Treatment Batch with batch number: {} was previously used and deleted for tenant: {}",
               representation.getHeatTreatmentBatchNumber(), tenantId);
    }
  }

  /**
   * Phase 2: Validate pieces count and furnace availability
   */
  private Furnace validatePiecesCountAndFurnaceAvailability(long tenantId, long furnaceId, HeatTreatmentBatchRepresentation representation) {
    Furnace furnace = getFurnaceUsingTenantIdAndFurnaceId(tenantId, furnaceId);
    boolean isHeatTreatmentBatchAppliedOnFurnace = isHeatTreatmentBatchAppliedOnFurnace(furnaceId);

    if (isHeatTreatmentBatchAppliedOnFurnace) {
      log.error("Furnace={} is already having a heatTreatmentBatch set. Cannot apply a new heatTreatmentBatch on this furnace", furnaceId);
      throw new FurnaceOccupiedException("Cannot apply a new heatTreatmentBatch on this furnace as Furnace " + furnaceId + " is already occupied");
    }

    return furnace;
  }

  /**
   * Phase 3: Setup heat treatment batch with basic properties
   */
  private HeatTreatmentBatch setupHeatTreatmentBatch(long tenantId, Furnace furnace, HeatTreatmentBatchRepresentation representation) {
    HeatTreatmentBatch inputHeatTreatmentBatch = heatTreatmentBatchAssembler.createAssemble(representation);

    if (inputHeatTreatmentBatch.getTotalWeight() > furnace.getFurnaceCapacity()) {
      log.error(
          "Selected items' total weight for heatTreatment has exceeded the furnace capacity! Can not perform heatTreatment operation. Furnace=" + furnace.getFurnaceName() + " tenantId=" + tenantId);
      throw new RuntimeException(
          "Selected items' total weight for heatTreatment has exceeded the furnace capacity! Can not perform heatTreatment operation. Furnace=" + furnace.getFurnaceName() + " tenantId=" + tenantId);
    }

    inputHeatTreatmentBatch.setFurnace(furnace);
    Tenant tenant = tenantService.getTenantById(tenantId);
    inputHeatTreatmentBatch.setTenant(tenant);

    return inputHeatTreatmentBatch;
  }

  /**
   * Phase 4: Process each heat treatment batch item with workflow integration
   */
  private void processHeatTreatmentBatchItems(HeatTreatmentBatchRepresentation representation,
                                              HeatTreatmentBatch inputHeatTreatmentBatch) {
    // Clear the existing processed items list to avoid duplicates
    // (the assembler may have already populated it from the representation)
    inputHeatTreatmentBatch.getProcessedItemHeatTreatmentBatches().clear();

    representation.getProcessedItemHeatTreatmentBatches().forEach(processedItemHeatTreatmentBatchRepresentation -> {
      ProcessedItemHeatTreatmentBatch processedItemHeatTreatmentBatch =
          processedItemHeatTreatmentBatchAssembler.createAssemble(processedItemHeatTreatmentBatchRepresentation);

      // Finalize processed item setup and add to the batch
      finalizeProcessedItemSetup(processedItemHeatTreatmentBatch, inputHeatTreatmentBatch);

      // Add the processed item to the batch's list
      inputHeatTreatmentBatch.getProcessedItemHeatTreatmentBatches().add(processedItemHeatTreatmentBatch);
    });
  }

  /**
   * Validate timing constraints for processed item
   * Note: This method is deprecated in the flexible workflow architecture
   * since heat treatment can now be the first operation without forging
   */
  @Deprecated
  private void validateProcessedItemTiming(ProcessedItemHeatTreatmentBatch processedItemHeatTreatmentBatch, LocalDateTime applyAt) {
    // This validation is no longer applicable in flexible workflows
    // where heat treatment can be the first operation without forging
    log.warn("validateProcessedItemTiming is deprecated in flexible workflow architecture");
  }

  /**
   * Handle workflow integration including pieces consumption and workflow step updates
   */
  private void handleWorkflowIntegration(HeatTreatmentBatchRepresentation representation,
                                         ProcessedItemHeatTreatmentBatch processedItemHeatTreatmentBatch) {
    try {
      // Extract item directly from ProcessedItemHeatTreatmentBatch
      Item item = processedItemHeatTreatmentBatch.getItem();

      // Get workflow fields from the ProcessedItemHeatTreatmentBatch entity
      String workflowIdentifier = processedItemHeatTreatmentBatch.getWorkflowIdentifier();
      Long itemWorkflowId = processedItemHeatTreatmentBatch.getItemWorkflowId();

      // Use the generic workflow handling method
      ItemWorkflow workflow = itemWorkflowService.handleWorkflowForOperation(
          item,
          WorkflowStep.OperationType.HEAT_TREATMENT,
          workflowIdentifier,
          itemWorkflowId
      );

      // Update the heat treatment batch with workflow ID for future reference
      log.info("Successfully integrated heat treatment batch with workflow. Workflow ID: {}, Workflow Identifier: {}",
               workflow.getId(), workflow.getWorkflowIdentifier());

      if (processedItemHeatTreatmentBatch.getItemWorkflowId() == null) {
        processedItemHeatTreatmentBatch.setItemWorkflowId(workflow.getId());
        processedItemHeatTreatmentBatchRepository.save(processedItemHeatTreatmentBatch);
      }

      // Check if heat treatment is the first operation in the workflow template
      boolean isFirstOperation = WorkflowStep.OperationType.HEAT_TREATMENT.equals(
          workflow.getWorkflowTemplate().getFirstStep().getOperationType());

      if (isFirstOperation) {
        // This is the first operation in workflow - consume inventory from Heat
        log.info("Heat treatment is the first operation in workflow - consuming inventory from heat");
        handleHeatConsumptionForFirstOperation(representation, processedItemHeatTreatmentBatch, workflow);
      } else {
        // This is not the first operation - consume pieces from previous operation
        handlePiecesConsumptionFromPreviousOperation(processedItemHeatTreatmentBatch, workflow);
      }

      // Update workflow step with accumulated batch data
      updateWorkflowStepWithBatchOutcome(processedItemHeatTreatmentBatch, workflow.getId());

      // Update relatedEntityIds for HEAT_TREATMENT operation step with ProcessedItemHeatTreatmentBatch.id
      itemWorkflowService.updateRelatedEntityIds(workflow.getId(), WorkflowStep.OperationType.HEAT_TREATMENT, processedItemHeatTreatmentBatch.getId());

    } catch (Exception e) {
      log.error("Failed to integrate heat treatment batch with workflow for item {}: {}",
                processedItemHeatTreatmentBatch.getItem().getId(), e.getMessage());
      // Re-throw to fail the operation since workflow integration is critical
      throw new RuntimeException("Failed to integrate with workflow system: " + e.getMessage(), e);
    }
  }

  /**
   * Handles pieces consumption from previous operation (if applicable)
   */
  private void handlePiecesConsumptionFromPreviousOperation(ProcessedItemHeatTreatmentBatch processedItemHeatTreatmentBatch,
                                                            ItemWorkflow workflow) {
    // Get previous operation step and validate available pieces
    ItemWorkflowStep previousOperationStep = itemWorkflowService.getPreviousOperationStep(
        workflow.getId(), WorkflowStep.OperationType.HEAT_TREATMENT);

    if (previousOperationStep == null) {
      log.warn("No previous operation step found for heat treatment in workflow {}", workflow.getId());
      return;
    }

    int currentAvailablePiecesForHeat = itemWorkflowService.getAvailablePiecesFromSpecificPreviousOperationOfItemWorkflowStep(
        previousOperationStep, processedItemHeatTreatmentBatch.getPreviousOperationProcessedItemId());

    if (currentAvailablePiecesForHeat < processedItemHeatTreatmentBatch.getHeatTreatBatchPiecesCount()) {
      throw new IllegalArgumentException("Piece count exceeds available pieces from previous operation " +
                                         processedItemHeatTreatmentBatch.getPreviousOperationProcessedItemId());
    }

    // Update available pieces in the specific previous operation
    itemWorkflowService.updateAvailablePiecesInSpecificPreviousOperation(
        workflow.getId(),
        WorkflowStep.OperationType.HEAT_TREATMENT,
        processedItemHeatTreatmentBatch.getPreviousOperationProcessedItemId(),
        processedItemHeatTreatmentBatch.getHeatTreatBatchPiecesCount());

    log.info("Successfully consumed {} pieces from {} operation {} for heat treatment in workflow {}",
             processedItemHeatTreatmentBatch.getHeatTreatBatchPiecesCount(),
             previousOperationStep.getOperationType(),
             processedItemHeatTreatmentBatch.getPreviousOperationProcessedItemId(),
             workflow.getId());
  }

  /**
   * Handles heat consumption from inventory when heat treatment is the first operation
   */
  private void handleHeatConsumptionForFirstOperation(HeatTreatmentBatchRepresentation representation,
                                                      ProcessedItemHeatTreatmentBatch processedItemHeatTreatmentBatch,
                                                      ItemWorkflow workflow) {
    // Get the corresponding processed item representation to access heat data
    ProcessedItemHeatTreatmentBatchRepresentation processedItemRepresentation =
        representation.getProcessedItemHeatTreatmentBatches().stream()
            .filter(piRep -> piRep.getItem() != null &&
                             processedItemHeatTreatmentBatch.getItem() != null &&
                             piRep.getItem().getId().equals(processedItemHeatTreatmentBatch.getItem().getId()))
            .findFirst()
            .orElse(null);

    if (processedItemRepresentation == null ||
        processedItemRepresentation.getHeatTreatmentHeats() == null ||
        processedItemRepresentation.getHeatTreatmentHeats().isEmpty()) {
      log.warn("No heat consumption data provided for first operation heat treatment batch processed item {}. This may result in inventory inconsistency.",
               processedItemHeatTreatmentBatch.getId());
      return;
    }

    // Validate that heat consumption matches the required pieces
    int totalPiecesFromHeats = processedItemRepresentation.getHeatTreatmentHeats().stream()
        .mapToInt(HeatTreatmentHeatRepresentation::getPiecesUsed)
        .sum();

    if (totalPiecesFromHeats != processedItemHeatTreatmentBatch.getHeatTreatBatchPiecesCount()) {
      throw new IllegalArgumentException("Total pieces from heats (" + totalPiecesFromHeats +
                                         ") does not match heat treatment batch pieces count (" +
                                         processedItemHeatTreatmentBatch.getHeatTreatBatchPiecesCount() +
                                         ") for processed item " + processedItemHeatTreatmentBatch.getId());
    }

    // Get the heat treatment batch apply time for validation
    LocalDateTime applyAtLocalDateTime = ConvertorUtils.convertStringToLocalDateTime(representation.getApplyAt());

    // Validate heat availability and consume pieces from inventory
    processedItemRepresentation.getHeatTreatmentHeats().forEach(heatRepresentation -> {
      try {
        // Get the heat entity
        Heat heat =
            rawMaterialHeatService.getRawMaterialHeatById(heatRepresentation.getHeat().getId());

        // Calculate new available pieces count after consumption
        int newHeatPieces = heat.getAvailablePiecesCount() - heatRepresentation.getPiecesUsed();

        // Validate sufficient pieces are available
        if (newHeatPieces < 0) {
          log.error("Insufficient heat pieces for heat={} on workflow={}", heat.getId(), workflow.getId());
          throw new IllegalArgumentException("Insufficient heat pieces for heat " + heat.getId());
        }

        // Validate timing - heat should be created before the heat treatment apply time
        if (heat.getCreatedAt().compareTo(applyAtLocalDateTime) > 0) {
          log.error("The provided apply at time={} is before to heat={} created at time={} !",
                    applyAtLocalDateTime, heat.getHeatNumber(), heat.getCreatedAt());
          throw new RuntimeException("The provided apply at time=" + applyAtLocalDateTime +
                                     " is before to heat=" + heat.getHeatNumber() +
                                     " created at time=" + heat.getCreatedAt() + " !");
        }

        // Update heat available pieces count
        log.info("Updating AvailablePiecesCount for heat={} from {} to {} for heat treatment batch processed item {}",
                 heat.getId(), heat.getAvailablePiecesCount(), newHeatPieces, processedItemHeatTreatmentBatch.getId());
        heat.setAvailablePiecesCount(newHeatPieces);

        // Persist the updated heat
        rawMaterialHeatService.updateRawMaterialHeat(heat);

        log.info("Successfully consumed {} pieces from heat {} for heat treatment batch processed item {} in workflow {}",
                 heatRepresentation.getPiecesUsed(),
                 heatRepresentation.getHeat().getId(),
                 processedItemHeatTreatmentBatch.getId(),
                 workflow.getId());

      } catch (Exception e) {
        log.error("Failed to consume pieces from heat {} for heat treatment processed item {}: {}",
                  heatRepresentation.getHeat().getId(), processedItemHeatTreatmentBatch.getId(), e.getMessage());
        throw new RuntimeException("Failed to consume inventory from heat: " + e.getMessage(), e);
      }
    });

    log.info("Successfully consumed inventory from {} heats for heat treatment batch processed item {} in workflow {}",
             processedItemRepresentation.getHeatTreatmentHeats().size(), processedItemHeatTreatmentBatch.getId(), workflow.getId());
  }

  /**
   * Update workflow step with accumulated batch outcome data
   */
  private void updateWorkflowStepWithBatchOutcome(ProcessedItemHeatTreatmentBatch processedItemHeatTreatmentBatch,
                                                  Long workflowId) {
    // Create heatTreatmentBatchOutcome object with available data from ProcessedItemHeatTreatmentBatch
    OperationOutcomeData.BatchOutcome heatTreatmentBatchOutcome = OperationOutcomeData.BatchOutcome.builder()
        .id(processedItemHeatTreatmentBatch.getId())
        .initialPiecesCount(0)
        .piecesAvailableForNext(0)
        .createdAt(processedItemHeatTreatmentBatch.getCreatedAt())
        .updatedAt(LocalDateTime.now())
        .deletedAt(processedItemHeatTreatmentBatch.getDeletedAt())
        .deleted(processedItemHeatTreatmentBatch.isDeleted())
        .build();

    // Get existing workflow step data and accumulate batch outcomes
    List<OperationOutcomeData.BatchOutcome> accumulatedBatchData = getAccumulatedBatchData(processedItemHeatTreatmentBatch, workflowId);

    // Add the current batch outcome to the accumulated list
    accumulatedBatchData.add(heatTreatmentBatchOutcome);

    // Update workflow step with accumulated batch data
    itemWorkflowService.updateWorkflowStepForOperation(
        workflowId,
        WorkflowStep.OperationType.HEAT_TREATMENT,
        OperationOutcomeData.forHeatTreatmentOperation(accumulatedBatchData, LocalDateTime.now()));
  }

  /**
   * Get accumulated batch data from existing workflow step
   */
  private List<OperationOutcomeData.BatchOutcome> getAccumulatedBatchData(ProcessedItemHeatTreatmentBatch processedItemHeatTreatmentBatch, Long workflowId) {
    List<OperationOutcomeData.BatchOutcome> accumulatedBatchData = new ArrayList<>();

    try {
      ItemWorkflowStep existingHeatTreatmentStep = itemWorkflowService.getWorkflowStepByOperation(
          workflowId, WorkflowStep.OperationType.HEAT_TREATMENT);

      if (existingHeatTreatmentStep != null &&
          existingHeatTreatmentStep.getOperationOutcomeData() != null &&
          !existingHeatTreatmentStep.getOperationOutcomeData().trim().isEmpty()) {

        // Parse existing outcome data and get existing batch data
        OperationOutcomeData existingOutcomeData = objectMapper.readValue(
            existingHeatTreatmentStep.getOperationOutcomeData(), OperationOutcomeData.class);

        if (existingOutcomeData.getBatchData() != null) {
          accumulatedBatchData.addAll(existingOutcomeData.getBatchData());
        }
      }
    } catch (Exception e) {
      log.warn("Failed to parse existing workflow outcome data for heat treatment step in workflow {}: {}",
               workflowId, e.getMessage());
    }

    return accumulatedBatchData;
  }

  /**
   * Finalize processed item setup
   */
  private void finalizeProcessedItemSetup(ProcessedItemHeatTreatmentBatch processedItemHeatTreatmentBatch,
                                          HeatTreatmentBatch inputHeatTreatmentBatch) {
    processedItemHeatTreatmentBatch.setItemStatus(ItemStatus.HEAT_TREATMENT_NOT_STARTED);
    processedItemHeatTreatmentBatch.setHeatTreatmentBatch(inputHeatTreatmentBatch);
  }

  /**
   * Phase 5: Finalize and save heat treatment batch
   */
  private HeatTreatmentBatchRepresentation finalizeHeatTreatmentBatch(HeatTreatmentBatch inputHeatTreatmentBatch, Furnace furnace, HeatTreatmentBatchRepresentation heatTreatmentBatchRepresentation) {
    HeatTreatmentBatch createdHeatTreatmentBatch = heatTreatmentBatchRepository.save(inputHeatTreatmentBatch);
    furnace.setFurnaceStatus(Furnace.FurnaceStatus.HEAT_TREATMENT_BATCH_APPLIED);
    furnaceService.saveFurnace(furnace);

    // Handle workflow integration and pieces consumption
    createdHeatTreatmentBatch.getProcessedItemHeatTreatmentBatches().forEach(processedItemHeatTreatmentBatch -> {
      handleWorkflowIntegration(heatTreatmentBatchRepresentation, processedItemHeatTreatmentBatch);
    });

    // Return the created HeatTreatmentBatch
    HeatTreatmentBatchRepresentation createdRepresentation = heatTreatmentBatchAssembler.dissemble(createdHeatTreatmentBatch);
    createdRepresentation.setHeatTreatmentBatchStatus(HeatTreatmentBatch.HeatTreatmentBatchStatus.IDLE.name());

    return createdRepresentation;
  }

  public Furnace getFurnaceUsingTenantIdAndFurnaceId(long tenantId, long furnaceId) {
    boolean isFurnaceOfTenantExists = furnaceService.isFurnaceByTenantExists(tenantId);
    if (!isFurnaceOfTenantExists) {
      log.error("Furnace={} does not exist!", furnaceId);
      throw new ResourceNotFoundException("Furnace for the tenant does not exist!");
    }
    return furnaceService.getFurnaceByIdAndTenantId(furnaceId, tenantId);
  }

  public boolean isHeatTreatmentBatchAppliedOnFurnace(long furnaceId) {
    Optional<HeatTreatmentBatch> heatTreatmentBatchOptional = heatTreatmentBatchRepository.findAppliedHeatTreatmentBatchOnFurnace(furnaceId);
    if (heatTreatmentBatchOptional.isPresent()) {
      log.info("HeatTreatmentBatch={} already applied on furnaceId={}", heatTreatmentBatchOptional.get().getId(), furnaceId);
      return true;
    }
    return false;
  }

  //Start HeatTreatmentBatch (Update start Time, Update Item Status, Update FurnaceStatus)
  @Transactional
  public HeatTreatmentBatchRepresentation startHeatTreatmentBatch(long tenantId, long furnaceId, long heatTreatmentBatchId, String startAt) {
    tenantService.validateTenantExists(tenantId);
    Furnace furnace = getFurnaceUsingTenantIdAndFurnaceId(tenantId, furnaceId);
    boolean isHeatTreatmentBatchAppliedOnFurnace = isHeatTreatmentBatchAppliedOnFurnace(furnaceId);

    if (!isHeatTreatmentBatchAppliedOnFurnace) {
      log.error("Furnace={} does not have a heatTreatmentBatch set. Can not start forge existingHeatTreatmentBatch on this furnace as it does not have heatTreatmentBatch", furnaceId);
      throw new ForgeNotFoundException("heatTreatmentBatch does not exists for furnace!");
    }
    HeatTreatmentBatch existingHeatTreatmentBatch = getHeatTreatmentBatchById(heatTreatmentBatchId);

    if (existingHeatTreatmentBatch.getStartAt() != null) {
      log.error("The heatTreatmentBatch={} for furnace={} has already been started!", heatTreatmentBatchId, furnace.getFurnaceName());
      throw new HeatTreatmentBatchNotInExpectedStatusException("HeatTreatmentBatch=" + heatTreatmentBatchId + " , for furnace=" + furnace.getFurnaceName() + "has already been started!");
    }

    LocalDateTime applyAtLocalDateTime = existingHeatTreatmentBatch.getApplyAt();
    LocalDateTime startAtLocalDateTime = ConvertorUtils.convertStringToLocalDateTime(startAt);

    if (applyAtLocalDateTime.compareTo(startAtLocalDateTime) > 0) {
      log.error("The provided start at time={} is before to heat treatment batch={} apply at time={} !", startAtLocalDateTime,
                existingHeatTreatmentBatch.getHeatTreatmentBatchNumber(), applyAtLocalDateTime);
      throw new RuntimeException(
          "The provided start at time=" + startAtLocalDateTime + " is before to heat treatment batch=" + existingHeatTreatmentBatch.getHeatTreatmentBatchNumber() + " apply at time="
          + applyAtLocalDateTime
          + " !");
    }

    if (!HeatTreatmentBatch.HeatTreatmentBatchStatus.IDLE.equals(existingHeatTreatmentBatch.getHeatTreatmentBatchStatus())) {
      log.error("The heatTreatmentBatch={} for furnace={} is not in IDLE status to start it!", heatTreatmentBatchId, furnace.getFurnaceName());
      throw new HeatTreatmentBatchNotInExpectedStatusException("HeatTreatmentBatch=" + heatTreatmentBatchId + " , for furnace=" + furnace.getFurnaceName() + "Not in IDLE status to start it!");
    }

    existingHeatTreatmentBatch.setHeatTreatmentBatchStatus(HeatTreatmentBatch.HeatTreatmentBatchStatus.IN_PROGRESS);
    existingHeatTreatmentBatch.setStartAt(ConvertorUtils.convertStringToLocalDateTime(startAt));
    existingHeatTreatmentBatch.getProcessedItemHeatTreatmentBatches().forEach(processedItemHeatTreatmentBatch -> {
      processedItemHeatTreatmentBatch.setItemStatus(ItemStatus.HEAT_TREATMENT_IN_PROGRESS);
    });

    HeatTreatmentBatch startedHeatTreatmentBatch = heatTreatmentBatchRepository.save(existingHeatTreatmentBatch);

    // Update workflow steps for all processed items with start time
    updateWorkflowForHeatTreatmentStart(startedHeatTreatmentBatch, startAtLocalDateTime);

    furnace.setFurnaceStatus(Furnace.FurnaceStatus.HEAT_TREATMENT_BATCH_IN_PROGRESS);
    furnaceService.saveFurnace(furnace);

    return heatTreatmentBatchAssembler.dissemble(startedHeatTreatmentBatch);
  }

  /**
   * Updates workflow when heat treatment is started
   * Updates existing BatchOutcome entries with startedAt time instead of overwriting existing data
   */
  private void updateWorkflowForHeatTreatmentStart(HeatTreatmentBatch heatTreatmentBatch, LocalDateTime startedAt) {
    try {
      // Group processed items by itemWorkflowId since each can belong to different workflows
      Map<Long, List<ProcessedItemHeatTreatmentBatch>> workflowGroups = heatTreatmentBatch
          .getProcessedItemHeatTreatmentBatches().stream()
          .filter(processedItem -> processedItem.getItemWorkflowId() != null)
          .collect(Collectors.groupingBy(ProcessedItemHeatTreatmentBatch::getItemWorkflowId));

      // Update each workflow separately
      for (Map.Entry<Long, List<ProcessedItemHeatTreatmentBatch>> workflowGroup : workflowGroups.entrySet()) {
        Long itemWorkflowId = workflowGroup.getKey();
        List<ProcessedItemHeatTreatmentBatch> processedItemsForWorkflow = workflowGroup.getValue();

        // Validate workflow and get ItemWorkflow
        ItemWorkflow itemWorkflow = validateAndGetWorkflow(itemWorkflowId, processedItemsForWorkflow.get(0));

        // Find the HEAT_TREATMENT workflow step for this workflow
        Optional<ItemWorkflowStep> heatTreatmentStepOpt = itemWorkflow.getItemWorkflowSteps().stream()
            .filter(step -> step.getWorkflowStep().getOperationType() == WorkflowStep.OperationType.HEAT_TREATMENT)
            .findFirst();

        if (heatTreatmentStepOpt.isEmpty()) {
          log.warn("No HEAT_TREATMENT step found for itemWorkflowId: {}", itemWorkflowId);
          throw new RuntimeException("No HEAT_TREATMENT step found for itemWorkflowId: " + itemWorkflowId);
        }

        ItemWorkflowStep heatTreatmentStep = heatTreatmentStepOpt.get();
        
        // Get existing operation outcome data from JSON string
        List<OperationOutcomeData.BatchOutcome> existingBatchData = new ArrayList<>();
        
        if (heatTreatmentStep.getOperationOutcomeData() != null && 
            !heatTreatmentStep.getOperationOutcomeData().trim().isEmpty()) {
          
          try {
            OperationOutcomeData existingOutcomeData = objectMapper.readValue(
                heatTreatmentStep.getOperationOutcomeData(), OperationOutcomeData.class);
            
            if (existingOutcomeData.getBatchData() != null) {
              existingBatchData.addAll(existingOutcomeData.getBatchData());
            }
          } catch (Exception e) {
            log.warn("Failed to parse existing workflow outcome data for heat treatment step in workflow {}: {}", 
                     itemWorkflowId, e.getMessage());
            throw e;
          }
        }


        // Update existing BatchOutcome entries or create new ones
        for (ProcessedItemHeatTreatmentBatch processedItem : processedItemsForWorkflow) {
          Long processedItemId = processedItem.getId();
          
          // Find existing batch outcome by ID
          boolean batchFound = false;
          for (OperationOutcomeData.BatchOutcome batchOutcome : existingBatchData) {
            if (Objects.equals(batchOutcome.getId(), processedItemId)) {
              // Update existing BatchOutcome with startedAt time
              batchOutcome.setStartedAt(startedAt);
              batchOutcome.setUpdatedAt(LocalDateTime.now());
              batchFound = true;
              
              log.debug("Updated existing BatchOutcome for processedItemId: {} with startedAt: {}", 
                       processedItemId, startedAt);
              break;
            }
          }
          
          if (!batchFound) {
            log.error("BatchOutcome for processedItemId: " + processedItemId + " not found!");
            throw new RuntimeException("BatchOutcome for processedItemId: " + processedItemId + " not found!");
          }
        }

        // Update workflow step with all batch data (preserving existing batches)
        itemWorkflowService.updateWorkflowStepForOperation(
            itemWorkflowId,
            WorkflowStep.OperationType.HEAT_TREATMENT,
            OperationOutcomeData.forHeatTreatmentOperation(existingBatchData, LocalDateTime.now()));

        log.info("Successfully updated workflow {} heat treatment step with start data for {} processed items, startedAt: {}",
                 itemWorkflowId, processedItemsForWorkflow.size(), startedAt);
      }

      log.info("Updated {} different workflows for heat treatment batch start {}", workflowGroups.size(), heatTreatmentBatch.getId());

    } catch (Exception e) {
      // Re-throw the exception to fail the heat treatment start since workflow integration is now mandatory
      log.error("Failed to update workflow for heat treatment start on batch ID={}: {}. Failing heat treatment start.",
                heatTreatmentBatch.getId(), e.getMessage());
      throw new RuntimeException("Failed to update workflow for heat treatment start: " + e.getMessage(), e);
    }
  }

  /**
   * Validates workflow requirements and returns the ItemWorkflow
   * Extracted common validation logic used by workflow updates
   */
  private ItemWorkflow validateAndGetWorkflow(Long itemWorkflowId, ProcessedItemHeatTreatmentBatch processedItemHeatTreatmentBatch) {
    if (itemWorkflowId == null) {
      log.error("itemWorkflowId is mandatory for heat treatment operations as it's required for workflow integration");
      throw new IllegalArgumentException("itemWorkflowId is mandatory for heat treatment operations. " +
                                         "This is required to update the workflow step so that next operations can consume the produced pieces.");
    }

    // Get the specific workflow by ID and validate it
    ItemWorkflow workflow = itemWorkflowService.getItemWorkflowById(itemWorkflowId);
    Long itemId = workflow.getItem().getId();

    // Validate that the workflow belongs to the same item as the heat treatment batch
    if (!itemId.equals(processedItemHeatTreatmentBatch.getItem().getId())) {
      log.error("ItemWorkflow {} does not belong to the same item as heat treatment batch {}", itemWorkflowId, processedItemHeatTreatmentBatch.getId());
      throw new IllegalArgumentException("ItemWorkflow does not belong to the same item as the heat treatment batch");
    }

    return workflow;
  }



  //End HeatTreatmentBatch (Update End Time, Update Item Status, Update FurnaceStatus)

  @Transactional
  public HeatTreatmentBatchRepresentation endHeatTreatmentBatch(long tenantId, long furnaceId, long heatTreatmentBatchId, HeatTreatmentBatchRepresentation heatTreatmentBatchRepresentation) {
    // Phase 1: Initial validations
    validateEndHeatTreatmentBatchPreconditions(tenantId, furnaceId, heatTreatmentBatchId);

    // Phase 2: Get entities and validate timing
    Furnace furnace = getFurnaceUsingTenantIdAndFurnaceId(tenantId, furnaceId);
    HeatTreatmentBatch existingHeatTreatmentBatch = getHeatTreatmentBatchById(heatTreatmentBatchId);
    LocalDateTime endAt = validateAndGetEndTime(existingHeatTreatmentBatch, heatTreatmentBatchRepresentation);

    // Phase 3: Update heat treatment batch status and processed items
    updateHeatTreatmentBatchForCompletion(existingHeatTreatmentBatch, endAt, heatTreatmentBatchRepresentation);

    // Phase 4: Save and update furnace status
    HeatTreatmentBatch completedHeatTreatmentBatch = heatTreatmentBatchRepository.save(existingHeatTreatmentBatch);
    updateFurnaceStatusAfterCompletion(furnace);

    // Phase 5: Update workflow steps
    updateWorkflowStepsForCompletion(completedHeatTreatmentBatch, endAt, heatTreatmentBatchId);

    return heatTreatmentBatchAssembler.dissemble(completedHeatTreatmentBatch);
  }

  /**
   * Phase 1: Validate all preconditions for ending heat treatment batch
   */
  private void validateEndHeatTreatmentBatchPreconditions(long tenantId, long furnaceId, long heatTreatmentBatchId) {
    tenantService.validateTenantExists(tenantId);

    boolean isHeatTreatmentBatchAppliedOnFurnace = isHeatTreatmentBatchAppliedOnFurnace(furnaceId);
    if (!isHeatTreatmentBatchAppliedOnFurnace) {
      log.error("Furnace={} does not have a existing heatTreatmentBatch set. Can not end heatTreatmentBatch on this furnace as it does not have existing heatTreatmentBatch", furnaceId);
      throw new HeatTreatmentBatchNotFoundException("HeatTreatmentBatch does not exists for furnace=" + furnaceId);
    }
  }

  /**
   * Phase 2: Validate timing and get end time
   */
  private LocalDateTime validateAndGetEndTime(HeatTreatmentBatch existingHeatTreatmentBatch, HeatTreatmentBatchRepresentation heatTreatmentBatchRepresentation) {
    Furnace furnace = existingHeatTreatmentBatch.getFurnace();
    long heatTreatmentBatchId = existingHeatTreatmentBatch.getId();

    if (existingHeatTreatmentBatch.getEndAt() != null) {
      log.error("The heatTreatmentBatch={} for furnace={} has already been ended!", heatTreatmentBatchId, furnace.getFurnaceName());
      throw new HeatTreatmentBatchNotInExpectedStatusException("HeatTreatmentBatch=" + heatTreatmentBatchId + " , for furnace=" + furnace.getFurnaceName() + "has already been ended!");
    }

    if (!HeatTreatmentBatch.HeatTreatmentBatchStatus.IN_PROGRESS.equals(existingHeatTreatmentBatch.getHeatTreatmentBatchStatus())) {
      log.error("The heatTreatmentBatch={} for furnace={} is not in IN_PROGRESS status to end it!", heatTreatmentBatchId, furnace.getFurnaceName());
      throw new HeatTreatmentBatchNotInExpectedStatusException("HeatTreatmentBatch=" + heatTreatmentBatchId + " , for furnace=" + furnace.getFurnaceName() + "Not in IN_PROGRESS status to end it!");
    }

    LocalDateTime endAt = ConvertorUtils.convertStringToLocalDateTime(heatTreatmentBatchRepresentation.getEndAt());
    if (existingHeatTreatmentBatch.getStartAt().compareTo(endAt) > 0) {
      log.error("The heatTreatmentBatch={} for furnace={} end time is before the start time!", heatTreatmentBatchId, furnace.getFurnaceName());
      throw new RuntimeException("HeatTreatmentBatch=" + heatTreatmentBatchId + " , for furnace=" + furnace.getFurnaceName() + " end time is before the start time!");
    }

    return endAt;
  }

  /**
   * Phase 3: Update heat treatment batch and processed items for completion
   */
  private void updateHeatTreatmentBatchForCompletion(HeatTreatmentBatch existingHeatTreatmentBatch, LocalDateTime endAt, HeatTreatmentBatchRepresentation heatTreatmentBatchRepresentation) {
    existingHeatTreatmentBatch.setHeatTreatmentBatchStatus(HeatTreatmentBatch.HeatTreatmentBatchStatus.COMPLETED);
    existingHeatTreatmentBatch.setEndAt(endAt);

    existingHeatTreatmentBatch.getProcessedItemHeatTreatmentBatches().forEach(processedItemHeatTreatmentBatch -> {
      updateProcessedItemForCompletion(processedItemHeatTreatmentBatch, heatTreatmentBatchRepresentation);
    });
  }

  /**
   * Update individual processed item for completion
   */
  private void updateProcessedItemForCompletion(ProcessedItemHeatTreatmentBatch processedItemHeatTreatmentBatch, HeatTreatmentBatchRepresentation heatTreatmentBatchRepresentation) {
    processedItemHeatTreatmentBatch.setItemStatus(ItemStatus.HEAT_TREATMENT_COMPLETED);

    // Check if the processed item exists in the representation and update it
    heatTreatmentBatchRepresentation.getProcessedItemHeatTreatmentBatches().stream()
        .filter(processedItemHeatTreatmentBatchRepresentation -> processedItemHeatTreatmentBatchRepresentation.getId().equals(processedItemHeatTreatmentBatch.getId()))
        .findFirst()
        .ifPresent(processedItemHeatTreatmentBatchRepresentation -> {
          int actualHeatTreatmentBatchPiecesCount = processedItemHeatTreatmentBatchRepresentation.getActualHeatTreatBatchPiecesCount();
          processedItemHeatTreatmentBatch.setActualHeatTreatBatchPiecesCount(actualHeatTreatmentBatchPiecesCount);

          if (processedItemHeatTreatmentBatchRepresentation.getWorkflowIdentifier() != null) {
            processedItemHeatTreatmentBatch.setWorkflowIdentifier(processedItemHeatTreatmentBatchRepresentation.getWorkflowIdentifier());
          }
          if (processedItemHeatTreatmentBatchRepresentation.getItemWorkflowId() != null) {
            processedItemHeatTreatmentBatch.setItemWorkflowId(processedItemHeatTreatmentBatchRepresentation.getItemWorkflowId());
          }
        });
  }

  /**
   * Phase 4: Update furnace status after completion
   */
  private void updateFurnaceStatusAfterCompletion(Furnace furnace) {
    furnace.setFurnaceStatus(Furnace.FurnaceStatus.HEAT_TREATMENT_BATCH_NOT_APPLIED);
    furnaceService.saveFurnace(furnace);
  }

  /**
   * Phase 5: Update workflow steps for completion
   */
  private void updateWorkflowStepsForCompletion(HeatTreatmentBatch completedHeatTreatmentBatch, LocalDateTime endAt, long heatTreatmentBatchId) {
    try {
      // Group processed items by itemWorkflowId since each can belong to different workflows
      Map<Long, List<ProcessedItemHeatTreatmentBatch>> workflowGroups = completedHeatTreatmentBatch
          .getProcessedItemHeatTreatmentBatches().stream()
          .filter(processedItem -> processedItem.getItemWorkflowId() != null)
          .collect(Collectors.groupingBy(ProcessedItemHeatTreatmentBatch::getItemWorkflowId));

      // Update each workflow separately
      for (Map.Entry<Long, List<ProcessedItemHeatTreatmentBatch>> workflowGroup : workflowGroups.entrySet()) {
        Long itemWorkflowId = workflowGroup.getKey();
        List<ProcessedItemHeatTreatmentBatch> processedItemsForWorkflow = workflowGroup.getValue();

        updateWorkflowForSpecificWorkflowGroup(itemWorkflowId, processedItemsForWorkflow, endAt, heatTreatmentBatchId);
      }

      // Log summary
      log.info("Updated {} different workflows for heat treatment batch {}", workflowGroups.size(), heatTreatmentBatchId);

    } catch (Exception e) {
      log.warn("Could not complete workflow steps for heat treatment batch {}: {}", heatTreatmentBatchId, e.getMessage());
    }
  }

  /**
   * Update workflow for a specific workflow group
   */
  private void updateWorkflowForSpecificWorkflowGroup(Long itemWorkflowId, List<ProcessedItemHeatTreatmentBatch> processedItemsForWorkflow, LocalDateTime endAt, long heatTreatmentBatchId) {
    // Get existing workflow step data to preserve other batch outcomes
    ItemWorkflowStep heatTreatmentItemWorkflowStep = itemWorkflowService.getWorkflowStepByOperation(
        itemWorkflowId, WorkflowStep.OperationType.HEAT_TREATMENT);

    List<OperationOutcomeData.BatchOutcome> existingBatchData = new ArrayList<>();

    // Parse existing batch data if it exists
    if (heatTreatmentItemWorkflowStep != null &&
        heatTreatmentItemWorkflowStep.getOperationOutcomeData() != null &&
        !heatTreatmentItemWorkflowStep.getOperationOutcomeData().trim().isEmpty()) {

      try {
        OperationOutcomeData existingOutcomeData = objectMapper.readValue(
            heatTreatmentItemWorkflowStep.getOperationOutcomeData(), OperationOutcomeData.class);

        if (existingOutcomeData.getBatchData() != null) {
          existingBatchData.addAll(existingOutcomeData.getBatchData());
        }
      } catch (Exception e) {
        log.warn("Failed to parse existing workflow outcome data for heat treatment step in workflow {}: {}",
                 itemWorkflowId, e.getMessage());
      }
    }

    // Update completion data for each processed item in this workflow group
    for (ProcessedItemHeatTreatmentBatch processedItemHeatTreatmentBatch : processedItemsForWorkflow) {
      boolean batchFound = false;

      // Find and update the specific batch outcome for this heat treatment batch
      for (OperationOutcomeData.BatchOutcome batchOutcome : existingBatchData) {
        if (Objects.equals(batchOutcome.getId(), processedItemHeatTreatmentBatch.getId())) {
          // Only update the completedAt field and final piece counts, preserve all other data
          batchOutcome.setCompletedAt(endAt);
          batchOutcome.setInitialPiecesCount(processedItemHeatTreatmentBatch.getActualHeatTreatBatchPiecesCount());
          batchOutcome.setPiecesAvailableForNext(processedItemHeatTreatmentBatch.getActualHeatTreatBatchPiecesCount());
          batchOutcome.setUpdatedAt(LocalDateTime.now());
          batchFound = true;

          log.info("Updated heat treatment batch outcome completion: ID={}, completedAt={}, finalPieces={}",
                   processedItemHeatTreatmentBatch.getId(), endAt, processedItemHeatTreatmentBatch.getActualHeatTreatBatchPiecesCount());
          break;
        }
      }

      // If batch outcome doesn't exist, create a new one (this shouldn't normally happen)
      if (!batchFound) {
        log.warn("Batch outcome for heat treatment batch {} not found in existing data. Creating new batch outcome.",
                 processedItemHeatTreatmentBatch.getId());
        OperationOutcomeData.BatchOutcome newBatchOutcome = OperationOutcomeData.BatchOutcome.builder()
            .id(processedItemHeatTreatmentBatch.getId())
            .initialPiecesCount(processedItemHeatTreatmentBatch.getActualHeatTreatBatchPiecesCount())
            .piecesAvailableForNext(processedItemHeatTreatmentBatch.getActualHeatTreatBatchPiecesCount())
            .completedAt(endAt)
            .createdAt(processedItemHeatTreatmentBatch.getCreatedAt())
            .updatedAt(LocalDateTime.now())
            .deletedAt(processedItemHeatTreatmentBatch.getDeletedAt())
            .deleted(processedItemHeatTreatmentBatch.isDeleted())
            .build();
        existingBatchData.add(newBatchOutcome);
      }
    }

    // Update workflow step with all batch data (preserving existing batches)
    itemWorkflowService.updateWorkflowStepForOperation(
        itemWorkflowId,
        WorkflowStep.OperationType.HEAT_TREATMENT,
        OperationOutcomeData.forHeatTreatmentOperation(existingBatchData, LocalDateTime.now())
    );

    log.info("Successfully updated workflow step for itemWorkflowId {} with {} processed items for heat treatment batch {}",
             itemWorkflowId, processedItemsForWorkflow.size(), heatTreatmentBatchId);
  }

  public HeatTreatmentBatch getHeatTreatmentBatchById(long heatTreatmentBatchId) {
    Optional<HeatTreatmentBatch> heatTreatmentBatchOptional = heatTreatmentBatchRepository.findByIdAndDeletedFalse(heatTreatmentBatchId);
    if (heatTreatmentBatchOptional.isEmpty()) {
      log.error("HeatTreatmentBatch does not exists for heatTreatmentBatchId={}", heatTreatmentBatchId);
      throw new HeatTreatmentBatchNotFoundException("HeatTreatmentBatch does not exists for heatTreatmentBatchId=" + heatTreatmentBatchId);
    }
    return heatTreatmentBatchOptional.get();
  }

  //getAppliedHeatTreatmentByFurnace

  public HeatTreatmentBatch getAppliedHeatTreatmentByFurnace(long furnaceId) {
    Optional<HeatTreatmentBatch> heatTreatmentBatchOptional = heatTreatmentBatchRepository.findAppliedHeatTreatmentBatchOnFurnace(furnaceId);
    if (heatTreatmentBatchOptional.isEmpty()) {
      log.error("HeatTreatmentBatch does not exists for furnace={}", furnaceId);
      throw new ForgeNotFoundException("HeatTreatmentBatch does not exists for furnace!");
    }
    return heatTreatmentBatchOptional.get();
  }

//  getAllHeatTreatmentBatchByTenantId

  public Page<HeatTreatmentBatchRepresentation> getAllHeatTreatmentBatchByTenantId(long tenantId, int page, int size) {
    Pageable pageable = PageRequest.of(page, size);

//    List<Long> furnaceIds = furnaceService.getAllFurnacesOfTenant(tenantId)
//        .stream()
//        .map(Furnace::getId)
//        .collect(Collectors.toList());

    Page<HeatTreatmentBatch> heatTreatmentBatchPage = heatTreatmentBatchRepository
        .findByTenantIdAndDeletedFalseOrderByUpdatedAtDesc(tenantId, pageable);

    return heatTreatmentBatchPage.map(heatTreatmentBatchAssembler::dissemble);
  }

  /**
   * Get all associations for a heat treatment batch
   *
   * @param heatTreatmentBatchId The ID of the heat treatment batch
   * @param tenantId             The ID of the tenant
   * @return DTO containing heat treatment batch details and all associated machining batches
   */
  @Transactional(readOnly = true)
  public HeatTreatmentBatchAssociationsDTO getHeatTreatmentBatchAssociations(Long heatTreatmentBatchId, Long tenantId) {
    tenantService.validateTenantExists(tenantId);

    // Get the heat treatment batch
    HeatTreatmentBatch heatTreatmentBatch = getHeatTreatmentBatchById(heatTreatmentBatchId);

    if (heatTreatmentBatch == null || heatTreatmentBatch.getTenant().getId() != tenantId) {
      log.error("Heat treatment batch not found or doesn't belong to tenant. ID: {}, Tenant: {}",
                heatTreatmentBatchId, tenantId);
      throw new HeatTreatmentBatchNotFoundException("Heat treatment batch not found");
    }

    // Convert to representation
    HeatTreatmentBatchRepresentation heatTreatmentBatchRepresentation =
        heatTreatmentBatchAssembler.dissemble(heatTreatmentBatch);

    // Get all machining batches associated with this heat treatment batch
    List<MachiningBatchAssociationsDTO> machiningBatchesWithAssociations =
        getMachiningBatchesWithAssociationsForHeatTreatmentBatch(heatTreatmentBatchId, tenantId);

    // Create and return the associations DTO
    return HeatTreatmentBatchAssociationsDTO.builder()
        .heatTreatmentBatchId(heatTreatmentBatchId)
        .heatTreatmentBatch(heatTreatmentBatchRepresentation)
        .machiningBatches(machiningBatchesWithAssociations)
        .build();
  }

  /**
   * Helper method to fetch all machining batches with their associations for a heat treatment batch
   *
   * @param heatTreatmentBatchId The ID of the heat treatment batch
   * @param tenantId             The ID of the tenant
   * @return List of machining batch association DTOs
   */
  private List<MachiningBatchAssociationsDTO> getMachiningBatchesWithAssociationsForHeatTreatmentBatch(Long heatTreatmentBatchId, Long tenantId) {
    List<com.jangid.forging_process_management_service.entities.machining.MachiningBatch> machiningBatches =
        heatTreatmentBatchRepository.findMachiningBatchesByHeatTreatmentBatchId(heatTreatmentBatchId);

    return machiningBatches.stream()
        .map(machiningBatch -> {
          // For each machining batch, get the full associations with inspection and dispatch batches
          return machiningBatchService.getMachiningBatchAssociations(machiningBatch.getId(), tenantId);
        })
        .collect(Collectors.toList());
  }

  @Transactional
  public void deleteHeatTreatmentBatch(long tenantId, long heatTreatmentBatchId) {
    // 1. Validate tenant exists
    tenantService.validateTenantExists(tenantId);

    // 2. Validate heat treatment batch exists
    HeatTreatmentBatch heatTreatmentBatch = getHeatTreatmentBatchById(heatTreatmentBatchId);

    // 3. Validate heat treatment batch status is COMPLETED
    if (!HeatTreatmentBatch.HeatTreatmentBatchStatus.COMPLETED.equals(heatTreatmentBatch.getHeatTreatmentBatchStatus())) {
      log.error("Cannot delete heat treatment batch={} as it is not in COMPLETED status!", heatTreatmentBatchId);
      throw new IllegalStateException("This heat treatment batch cannot be deleted as it is not in the COMPLETED status.");
    }

    // 5. Handle inventory reversal based on whether this was the first operation or not
    LocalDateTime now = LocalDateTime.now();
    for (ProcessedItemHeatTreatmentBatch processedItemHeatTreatmentBatch : heatTreatmentBatch.getProcessedItemHeatTreatmentBatches()) {
      Item item = processedItemHeatTreatmentBatch.getItem();

      // Get the item workflow ID from the ProcessedItemHeatTreatmentBatch entity
      Long itemWorkflowId = processedItemHeatTreatmentBatch.getItemWorkflowId();

      if (itemWorkflowId != null) {
        try {

          // Use workflow-based validation: check if all entries in next operation are marked deleted
          boolean canDeleteHeatTreatment = itemWorkflowService.areAllNextOperationBatchesDeleted(itemWorkflowId, WorkflowStep.OperationType.HEAT_TREATMENT);

          if (!canDeleteHeatTreatment) {
            log.error("Cannot delete heatTreatment id={} as the next operation has active (non-deleted) batches", heatTreatmentBatchId);
            throw new IllegalStateException("This heatTreatment cannot be deleted as the next operation has active batch entries.");
          }

          log.info("HeatTreatment id={} is eligible for deletion - all next operation batches are deleted", heatTreatmentBatchId);

          // Get the workflow to check if heat treatment was the first operation
          ItemWorkflow workflow = itemWorkflowService.getItemWorkflowById(itemWorkflowId);
          boolean wasFirstOperation = WorkflowStep.OperationType.HEAT_TREATMENT.equals(
              workflow.getWorkflowTemplate().getFirstStep().getOperationType());

          if (wasFirstOperation) {
            // This was the first operation - heat quantities will be returned to heat inventory
            log.info("Heat treatment batch {} was first operation for item {}, heat inventory will be reverted",
                     heatTreatmentBatchId, item.getId());

            // Update workflow step to mark heat treatment batch as deleted and adjust piece counts
            Integer actualHeatTreatBatchPiecesCount = processedItemHeatTreatmentBatch.getActualHeatTreatBatchPiecesCount();
            if (actualHeatTreatBatchPiecesCount != null && actualHeatTreatBatchPiecesCount > 0) {
              try {
                itemWorkflowService.markOperationAsDeletedAndUpdatePieceCounts(
                    itemWorkflowId, 
                    WorkflowStep.OperationType.HEAT_TREATMENT, 
                    actualHeatTreatBatchPiecesCount,
                    processedItemHeatTreatmentBatch.getId()
                );
                log.info("Successfully marked heat treatment operation as deleted and updated workflow step for processed item {}, subtracted {} pieces", 
                         processedItemHeatTreatmentBatch.getId(), actualHeatTreatBatchPiecesCount);
              } catch (Exception e) {
                log.error("Failed to update workflow step for deleted heat treatment processed item {}: {}", 
                         processedItemHeatTreatmentBatch.getId(), e.getMessage());
                throw new RuntimeException("Failed to update workflow step for heat treatment deletion: " + e.getMessage(), e);
              }
            } else {
              log.info("No actual heat treatment pieces to subtract for deleted processed item {}", processedItemHeatTreatmentBatch.getId());
            }

            // Return heat quantities to original heats (similar to ForgeService.deleteForge method)
            LocalDateTime currentTime = LocalDateTime.now();
            if (processedItemHeatTreatmentBatch.getHeatTreatmentHeats() != null &&
                !processedItemHeatTreatmentBatch.getHeatTreatmentHeats().isEmpty()) {

              processedItemHeatTreatmentBatch.getHeatTreatmentHeats().forEach(heatTreatmentHeat -> {
                Heat heat = heatTreatmentHeat.getHeat();
                int piecesToReturn = heatTreatmentHeat.getPiecesUsed();

                // Return pieces to heat inventory based on heat's unit of measurement
                if (heat.getIsInPieces()) {
                  // Heat is managed in pieces - return to availablePiecesCount
                  int newAvailablePieces = heat.getAvailablePiecesCount() + piecesToReturn;
                  heat.setAvailablePiecesCount(newAvailablePieces);
                  log.info("Returned {} pieces to heat {} (pieces-based), new available pieces: {}",
                           piecesToReturn, heat.getId(), newAvailablePieces);
                } else {
                  throw new IllegalStateException("Heat treatment batch has no pieces!");
                }

                // Persist the updated heat
                rawMaterialHeatService.updateRawMaterialHeat(heat);

                // Soft delete heat treatment heat record
                heatTreatmentHeat.setDeleted(true);
                heatTreatmentHeat.setDeletedAt(currentTime);

                log.info("Successfully returned {} pieces from heat {} for deleted heat treatment batch processed item {}",
                         piecesToReturn, heat.getId(), processedItemHeatTreatmentBatch.getId());
              });
            }
          } else {
            // This was not the first operation - return pieces to previous operation
            Long previousOperationBatchId = itemWorkflowService.getPreviousOperationBatchId(
                itemWorkflowId, 
                WorkflowStep.OperationType.HEAT_TREATMENT,
                processedItemHeatTreatmentBatch.getPreviousOperationProcessedItemId()
            );

            if (previousOperationBatchId != null) {
              itemWorkflowService.returnPiecesToSpecificPreviousOperation(
                  itemWorkflowId,
                  WorkflowStep.OperationType.HEAT_TREATMENT,
                  previousOperationBatchId,
                  processedItemHeatTreatmentBatch.getHeatTreatBatchPiecesCount(),
                  processedItemHeatTreatmentBatch.getId()
              );

              log.info("Successfully returned {} pieces from heat treatment back to previous operation {} in workflow {}",
                       processedItemHeatTreatmentBatch.getHeatTreatBatchPiecesCount(),
                       previousOperationBatchId,
                       itemWorkflowId);
            } else {
              log.warn("Could not determine previous operation batch ID for heat treatment batch processed item {}. " +
                       "Pieces may not be properly returned to previous operation.", processedItemHeatTreatmentBatch.getId());
            }
          }
        } catch (Exception e) {
          log.warn("Failed to handle workflow pieces reversion for item {}: {}. This may indicate workflow data inconsistency.",
                   item.getId(), e.getMessage());
          throw e;
        }
      } else {
        log.warn("No workflow ID found for item {} during heat treatment batch deletion. " +
                 "This may be a legacy record before workflow integration.", item.getId());
        throw new IllegalStateException("No workflow ID found for item " + item.getId());
      }

      // 6. Soft delete each ProcessedItemHeatTreatmentBatch
      processedItemHeatTreatmentBatch.setDeleted(true);
      processedItemHeatTreatmentBatch.setDeletedAt(now);
    }

    // 7. Heat inventory reversal is now handled in the main deletion loop above
    // No additional processing needed as first operation heat reversal is done inline
    log.info("Heat inventory reversal completed as part of processed item deletion loop");

    // 8. Soft delete heat consumption records for all processed items
    heatTreatmentBatch.getProcessedItemHeatTreatmentBatches().forEach(processedItemHeatTreatmentBatch -> {
      if (processedItemHeatTreatmentBatch.getHeatTreatmentHeats() != null &&
          !processedItemHeatTreatmentBatch.getHeatTreatmentHeats().isEmpty()) {
        processedItemHeatTreatmentBatch.getHeatTreatmentHeats().forEach(heatTreatmentHeat -> {
          heatTreatmentHeat.setDeleted(true);
          heatTreatmentHeat.setDeletedAt(now);
        });
      }
    });

    // 9. Store the original batch number and modify the batch number for deletion
    heatTreatmentBatch.setOriginalHeatTreatmentBatchNumber(heatTreatmentBatch.getHeatTreatmentBatchNumber());
    heatTreatmentBatch.setHeatTreatmentBatchNumber(
        heatTreatmentBatch.getHeatTreatmentBatchNumber() + "_deleted_" + heatTreatmentBatch.getId() + "_" + now.toEpochSecond(java.time.ZoneOffset.UTC)
    );

    // 10. Soft delete the HeatTreatmentBatch
    heatTreatmentBatch.setDeleted(true);
    heatTreatmentBatch.setDeletedAt(now);
    heatTreatmentBatchRepository.save(heatTreatmentBatch);

    log.info("Successfully deleted heat treatment batch={}, original batch number={}",
             heatTreatmentBatchId, heatTreatmentBatch.getOriginalHeatTreatmentBatchNumber());
  }

  public boolean isHeatTreatmentBatchNumberPreviouslyUsed(String heatTreatmentBatchNumber, Long tenantId) {
    return heatTreatmentBatchRepository.existsByHeatTreatmentBatchNumberAndTenantIdAndOriginalHeatTreatmentBatchNumber(
        heatTreatmentBatchNumber, tenantId);
  }

  /**
   * Search for heat treatment batches by various criteria with pagination
   *
   * @param tenantId   The tenant ID
   * @param searchType The type of search (ITEM_NAME, FORGE_TRACEABILITY_NUMBER, HEAT_TREATMENT_BATCH_NUMBER, or FURNACE_NAME)
   * @param searchTerm The search term (substring matching for all search types)
   * @param page       The page number (0-based)
   * @param size       The page size
   * @return Page of HeatTreatmentBatchRepresentation containing the search results
   */
  @Transactional(readOnly = true)
  public Page<HeatTreatmentBatchRepresentation> searchHeatTreatmentBatches(Long tenantId, String searchType, String searchTerm, int page, int size) {
    if (searchTerm == null || searchTerm.trim().isEmpty() || searchType == null || searchType.trim().isEmpty()) {
      Pageable pageable = PageRequest.of(page, size);
      return Page.empty(pageable);
    }

    tenantService.validateTenantExists(tenantId);
    Pageable pageable = PageRequest.of(page, size);
    Page<HeatTreatmentBatch> heatTreatmentBatchPage;

    switch (searchType.toUpperCase()) {
      case "ITEM_NAME":
        heatTreatmentBatchPage = heatTreatmentBatchRepository.findHeatTreatmentBatchesByItemNameContainingIgnoreCase(tenantId, searchTerm.trim(), pageable);
        break;
      case "FORGE_TRACEABILITY_NUMBER":
        heatTreatmentBatchPage = heatTreatmentBatchRepository.findHeatTreatmentBatchesByForgeTraceabilityNumberContainingIgnoreCase(tenantId, searchTerm.trim(), pageable);
        break;
      case "HEAT_TREATMENT_BATCH_NUMBER":
        heatTreatmentBatchPage = heatTreatmentBatchRepository.findHeatTreatmentBatchesByHeatTreatmentBatchNumberContainingIgnoreCase(tenantId, searchTerm.trim(), pageable);
        break;
      case "FURNACE_NAME":
        heatTreatmentBatchPage = heatTreatmentBatchRepository.findHeatTreatmentBatchesByFurnaceNameContainingIgnoreCase(tenantId, searchTerm.trim(), pageable);
        break;
      default:
        log.error("Invalid search type: {}", searchType);
        throw new IllegalArgumentException("Invalid search type: " + searchType + ". Valid types are: ITEM_NAME, FORGE_TRACEABILITY_NUMBER, HEAT_TREATMENT_BATCH_NUMBER, FURNACE_NAME");
    }

    return heatTreatmentBatchPage.map(heatTreatmentBatchAssembler::dissemble);
  }

  private Long extractItemIdFromRepresentation(HeatTreatmentBatchRepresentation representation) {
    if (representation == null ||
        representation.getProcessedItemHeatTreatmentBatches() == null ||
        representation.getProcessedItemHeatTreatmentBatches().isEmpty() ||
        representation.getProcessedItemHeatTreatmentBatches().get(0).getItem() == null) {
      return null;
    }

    return representation.getProcessedItemHeatTreatmentBatches().get(0)
        .getItem().getId();
  }

  /**
   * Get HeatTreatmentBatch by ProcessedItemHeatTreatmentBatch ID
   *
   * @param processedItemHeatTreatmentBatchId The ID of the ProcessedItemHeatTreatmentBatch
   * @return The HeatTreatmentBatch associated with the ProcessedItemHeatTreatmentBatch
   * @throws HeatTreatmentBatchNotFoundException if no HeatTreatmentBatch is found for the given ID
   */
  public HeatTreatmentBatch getHeatTreatmentBatchByProcessedItemHeatTreatmentBatchId(long processedItemHeatTreatmentBatchId) {
    Optional<HeatTreatmentBatch> heatTreatmentBatchOptional = heatTreatmentBatchRepository
        .findByProcessedItemHeatTreatmentBatchIdAndDeletedFalse(processedItemHeatTreatmentBatchId);

    if (heatTreatmentBatchOptional.isEmpty()) {
      log.error("HeatTreatmentBatch does not exist for ProcessedItemHeatTreatmentBatchId={}", processedItemHeatTreatmentBatchId);
      throw new HeatTreatmentBatchNotFoundException("HeatTreatmentBatch does not exist for ProcessedItemHeatTreatmentBatchId=" + processedItemHeatTreatmentBatchId);
    }

    return heatTreatmentBatchOptional.get();
  }

  /**
   * Retrieves heat treatment batches by multiple processed item heat treatment batch IDs and validates they belong to the tenant
   *
   * @param processedItemHeatTreatmentBatchIds List of processed item heat treatment batch IDs
   * @param tenantId                           The tenant ID for validation
   * @return List of HeatTreatmentBatchRepresentation (distinct heat treatment batches)
   */
  public List<HeatTreatmentBatchRepresentation> getHeatTreatmentBatchesByProcessedItemHeatTreatmentBatchIds(List<Long> processedItemHeatTreatmentBatchIds, Long tenantId) {
    if (processedItemHeatTreatmentBatchIds == null || processedItemHeatTreatmentBatchIds.isEmpty()) {
      log.info("No processed item heat treatment batch IDs provided, returning empty list");
      return Collections.emptyList();
    }

    log.info("Getting heat treatment batches for {} processed item heat treatment batch IDs for tenant {}", processedItemHeatTreatmentBatchIds.size(), tenantId);

    List<HeatTreatmentBatch> heatTreatmentBatches = heatTreatmentBatchRepository.findByProcessedItemHeatTreatmentBatchIdInAndDeletedFalse(processedItemHeatTreatmentBatchIds);

    // Use a Set to track processed heat treatment batch IDs to avoid duplicates
    Set<Long> processedHeatTreatmentBatchIds = new HashSet<>();
    List<HeatTreatmentBatchRepresentation> validHeatTreatmentBatches = new ArrayList<>();
    List<Long> invalidProcessedItemHeatTreatmentBatchIds = new ArrayList<>();

    for (Long processedItemHeatTreatmentBatchId : processedItemHeatTreatmentBatchIds) {
      Optional<HeatTreatmentBatch> heatTreatmentBatchOpt = heatTreatmentBatches.stream()
          .filter(htb -> htb.getProcessedItemHeatTreatmentBatches().stream()
              .anyMatch(pihtb -> pihtb.getId().equals(processedItemHeatTreatmentBatchId)))
          .findFirst();

      if (heatTreatmentBatchOpt.isPresent()) {
        HeatTreatmentBatch heatTreatmentBatch = heatTreatmentBatchOpt.get();
        if (Long.valueOf(heatTreatmentBatch.getTenant().getId()).equals(tenantId)) {
          // Only add if we haven't already processed this heat treatment batch
          if (!processedHeatTreatmentBatchIds.contains(heatTreatmentBatch.getId())) {
            validHeatTreatmentBatches.add(heatTreatmentBatchAssembler.dissemble(heatTreatmentBatch));
            processedHeatTreatmentBatchIds.add(heatTreatmentBatch.getId());
          }
        } else {
          log.warn("HeatTreatmentBatch for processedItemHeatTreatmentBatchId={} does not belong to tenant={}", processedItemHeatTreatmentBatchId, tenantId);
          invalidProcessedItemHeatTreatmentBatchIds.add(processedItemHeatTreatmentBatchId);
        }
      } else {
        log.warn("No heat treatment batch found for processedItemHeatTreatmentBatchId={}", processedItemHeatTreatmentBatchId);
        invalidProcessedItemHeatTreatmentBatchIds.add(processedItemHeatTreatmentBatchId);
      }
    }

    if (!invalidProcessedItemHeatTreatmentBatchIds.isEmpty()) {
      log.warn("The following processed item heat treatment batch IDs did not have valid heat treatment batches for tenant {}: {}",
               tenantId, invalidProcessedItemHeatTreatmentBatchIds);
    }

    log.info("Found {} distinct valid heat treatment batches out of {} requested processed item heat treatment batch IDs", validHeatTreatmentBatches.size(), processedItemHeatTreatmentBatchIds.size());
    return validHeatTreatmentBatches;
  }

}
