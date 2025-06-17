package com.jangid.forging_process_management_service.service.heating;

import com.jangid.forging_process_management_service.assemblers.heating.HeatTreatmentBatchAssembler;
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
import com.jangid.forging_process_management_service.exception.ResourceNotFoundException;
import com.jangid.forging_process_management_service.exception.forging.ForgeNotFoundException;
import com.jangid.forging_process_management_service.exception.heating.FurnaceOccupiedException;
import com.jangid.forging_process_management_service.exception.heating.HeatTreatmentBatchNotFoundException;
import com.jangid.forging_process_management_service.repositories.heating.HeatTreatmentBatchRepository;
import com.jangid.forging_process_management_service.service.TenantService;
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
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Collections;

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
  private com.jangid.forging_process_management_service.assemblers.heating.ProcessedItemHeatTreatmentBatchAssembler processedItemHeatTreatmentBatchAssembler;
  @Autowired
  private ObjectMapper objectMapper;
  @Autowired
  private RawMaterialHeatService rawMaterialHeatService;

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
      log.error("Selected items' total weight for heatTreatment has exceeded the furnace capacity! Can not perform heatTreatment operation. Furnace=" + furnace.getFurnaceName() + " tenantId=" + tenantId);
      throw new RuntimeException("Selected items' total weight for heatTreatment has exceeded the furnace capacity! Can not perform heatTreatment operation. Furnace=" + furnace.getFurnaceName() + " tenantId=" + tenantId);
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

    // For heat treatment, determine the previous operation batch ID based on the previous operation type
    // This will need to be determined based on the specific previous operation and how it stores its batch IDs
    Long previousOperationBatchId = getPreviousOperationBatchId(processedItemHeatTreatmentBatch, previousOperationStep);

    int currentAvailablePiecesForHeat = itemWorkflowService.getAvailablePiecesFromSpecificPreviousOperationOfItemWorkflowStep(
        previousOperationStep, previousOperationBatchId);
    
    if (currentAvailablePiecesForHeat < processedItemHeatTreatmentBatch.getHeatTreatBatchPiecesCount()) {
      throw new IllegalArgumentException("Piece count exceeds available pieces from previous operation " + 
                                        previousOperationBatchId);
    }

    // Update available pieces in the specific previous operation
    itemWorkflowService.updateAvailablePiecesInSpecificPreviousOperation(
        workflow.getId(), 
        WorkflowStep.OperationType.HEAT_TREATMENT,
        previousOperationBatchId,
        processedItemHeatTreatmentBatch.getHeatTreatBatchPiecesCount());

    log.info("Successfully consumed {} pieces from {} operation {} for heat treatment in workflow {}", 
             processedItemHeatTreatmentBatch.getHeatTreatBatchPiecesCount(),
             previousOperationStep.getOperationType(),
             previousOperationBatchId,
             workflow.getId());
  }

  /**
   * Helper method to determine the previous operation batch ID
   * This will need to be implemented based on how different operations store their batch IDs
   */
  private Long getPreviousOperationBatchId(ProcessedItemHeatTreatmentBatch processedItemHeatTreatmentBatch, 
                                          ItemWorkflowStep previousOperationStep) {
    // This is a placeholder implementation that needs to be completed based on your specific requirements
    // For now, returning null as this would need to be determined based on the previous operation type
    // and how it relates to the current heat treatment batch
    log.warn("getPreviousOperationBatchId not fully implemented for operation type: {}", 
             previousOperationStep.getOperationType());
    return null;
  }

  /**
   * Handles heat consumption from inventory when heat treatment is the first operation
   */
  private void handleHeatConsumptionForFirstOperation(HeatTreatmentBatchRepresentation representation,
                                                     ProcessedItemHeatTreatmentBatch processedItemHeatTreatmentBatch,
                                                     ItemWorkflow workflow) {
    // Check if heat treatment heats are provided in the representation
    if (representation.getHeatTreatmentHeats() == null || representation.getHeatTreatmentHeats().isEmpty()) {
      log.warn("No heat consumption data provided for first operation heat treatment batch. This may result in inventory inconsistency.");
      return;
    }

    // Validate that heat consumption matches the required pieces
    int totalPiecesFromHeats = representation.getHeatTreatmentHeats().stream()
        .mapToInt(HeatTreatmentHeatRepresentation::getPiecesUsed)
        .sum();

    if (totalPiecesFromHeats != processedItemHeatTreatmentBatch.getHeatTreatBatchPiecesCount()) {
      throw new IllegalArgumentException("Total pieces from heats (" + totalPiecesFromHeats + 
                                        ") does not match heat treatment batch pieces count (" + 
                                        processedItemHeatTreatmentBatch.getHeatTreatBatchPiecesCount() + ")");
    }

    // Get the heat treatment batch apply time for validation
    LocalDateTime applyAtLocalDateTime = ConvertorUtils.convertStringToLocalDateTime(representation.getApplyAt());

    // Validate heat availability and consume pieces from inventory
    representation.getHeatTreatmentHeats().forEach(heatRepresentation -> {
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
        log.info("Updating AvailablePiecesCount for heat={} from {} to {} for heat treatment batch", 
                 heat.getId(), heat.getAvailablePiecesCount(), newHeatPieces);
        heat.setAvailablePiecesCount(newHeatPieces);
        
        // Persist the updated heat
        rawMaterialHeatService.updateRawMaterialHeat(heat);
        
        log.info("Successfully consumed {} pieces from heat {} for heat treatment batch in workflow {}", 
                 heatRepresentation.getPiecesUsed(),
                 heatRepresentation.getHeat().getId(),
                 workflow.getId());
        
      } catch (Exception e) {
        log.error("Failed to consume pieces from heat {} for heat treatment: {}", 
                  heatRepresentation.getHeat().getId(), e.getMessage());
        throw new RuntimeException("Failed to consume inventory from heat: " + e.getMessage(), e);
      }
    });

    log.info("Successfully consumed inventory from {} heats for heat treatment batch in workflow {}", 
             representation.getHeatTreatmentHeats().size(), workflow.getId());
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
      log.error("Furnace={} for the tenant={} does not exist!", furnaceId, tenantId);
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
          "The provided start at time=" + startAtLocalDateTime + " is before to heat treatment batch=" + existingHeatTreatmentBatch.getHeatTreatmentBatchNumber() + " apply at time=" + applyAtLocalDateTime
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

    furnace.setFurnaceStatus(Furnace.FurnaceStatus.HEAT_TREATMENT_BATCH_IN_PROGRESS);
    furnaceService.saveFurnace(furnace);

    return heatTreatmentBatchAssembler.dissemble(startedHeatTreatmentBatch);
  }

  //End HeatTreatmentBatch (Update End Time, Update Item Status, Update FurnaceStatus)

  @Transactional
  public HeatTreatmentBatchRepresentation endHeatTreatmentBatch(long tenantId, long furnaceId, long heatTreatmentBatchId, HeatTreatmentBatchRepresentation heatTreatmentBatchRepresentation) {
    tenantService.validateTenantExists(tenantId);
    Furnace furnace = getFurnaceUsingTenantIdAndFurnaceId(tenantId, furnaceId);
    boolean isHeatTreatmentBatchAppliedOnFurnace = isHeatTreatmentBatchAppliedOnFurnace(furnace.getId());

    if (!isHeatTreatmentBatchAppliedOnFurnace) {
      log.error("Furnace={} does not have a existing heatTreatmentBatch set. Can not end heatTreatmentBatch on this furnace as it does not have existing heatTreatmentBatch", furnaceId);
      throw new HeatTreatmentBatchNotFoundException("HeatTreatmentBatch does not exists for furnace=" + furnaceId);
    }

    HeatTreatmentBatch existingHeatTreatmentBatch = getHeatTreatmentBatchById(heatTreatmentBatchId);

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

    existingHeatTreatmentBatch.setHeatTreatmentBatchStatus(HeatTreatmentBatch.HeatTreatmentBatchStatus.COMPLETED);
    existingHeatTreatmentBatch.setEndAt(endAt);

    existingHeatTreatmentBatch.getProcessedItemHeatTreatmentBatches().forEach(processedItemHeatTreatmentBatch -> {
      processedItemHeatTreatmentBatch.setItemStatus(ItemStatus.HEAT_TREATMENT_COMPLETED);

      // Check if the processed item exists in the representation and update it
      heatTreatmentBatchRepresentation.getProcessedItemHeatTreatmentBatches().stream()
          .filter(processedItemHeatTreatmentBatchRepresentation -> processedItemHeatTreatmentBatchRepresentation.getId().equals(processedItemHeatTreatmentBatch.getId()))
          .findFirst()
          .ifPresent(processedItemHeatTreatmentBatchRepresentation -> {
                       int actualHeatTreatmentBatchPiecesCount = processedItemHeatTreatmentBatchRepresentation.getActualHeatTreatBatchPiecesCount();
                       processedItemHeatTreatmentBatch.setActualHeatTreatBatchPiecesCount(actualHeatTreatmentBatchPiecesCount);
                       processedItemHeatTreatmentBatch.setInitialMachiningBatchPiecesCount(actualHeatTreatmentBatchPiecesCount);
                       processedItemHeatTreatmentBatch.setAvailableMachiningBatchPiecesCount(actualHeatTreatmentBatchPiecesCount);
                       
                       // Update workflow fields if provided in the representation
                       if (processedItemHeatTreatmentBatchRepresentation.getWorkflowIdentifier() != null) {
                         processedItemHeatTreatmentBatch.setWorkflowIdentifier(processedItemHeatTreatmentBatchRepresentation.getWorkflowIdentifier());
                       }
                       if (processedItemHeatTreatmentBatchRepresentation.getItemWorkflowId() != null) {
                         processedItemHeatTreatmentBatch.setItemWorkflowId(processedItemHeatTreatmentBatchRepresentation.getItemWorkflowId());
                       }
                     }

          );
    });

    HeatTreatmentBatch completedHeatTreatmentBatch = heatTreatmentBatchRepository.save(existingHeatTreatmentBatch);

    furnace.setFurnaceStatus(Furnace.FurnaceStatus.HEAT_TREATMENT_BATCH_NOT_APPLIED);
    furnaceService.saveFurnace(furnace);

    // Complete workflow step with outcome data
    try {
        // Create new batch outcomes from the completed heat treatment batch
        List<OperationOutcomeData.BatchOutcome> newBatchData = completedHeatTreatmentBatch.getProcessedItemHeatTreatmentBatches().stream()
                .map(processedItemHeatTreatmentBatch -> OperationOutcomeData.BatchOutcome.builder()
                        .id(processedItemHeatTreatmentBatch.getId())
                        .initialPiecesCount(processedItemHeatTreatmentBatch.getActualHeatTreatBatchPiecesCount())
                        .piecesAvailableForNext(processedItemHeatTreatmentBatch.getActualHeatTreatBatchPiecesCount())
                        .createdAt(processedItemHeatTreatmentBatch.getCreatedAt())
                        .updatedAt(processedItemHeatTreatmentBatch.getUpdatedAt())
                        .deletedAt(processedItemHeatTreatmentBatch.getDeletedAt())
                        .deleted(processedItemHeatTreatmentBatch.isDeleted())
                        .build())
                .collect(Collectors.toList());
        
        // Use the generic method to merge and update workflow step data
        boolean updateSuccess = itemWorkflowService.updateWorkflowStepWithMergedBatchData(
            completedHeatTreatmentBatch.getProcessedItemHeatTreatmentBatches().get(0).getItemWorkflowId(),
            WorkflowStep.OperationType.HEAT_TREATMENT,
            newBatchData
        );
        
        if (updateSuccess) {
            log.info("Successfully updated workflow step with merged batch data for heat treatment batch {}", 
                     heatTreatmentBatchId);
        } else {
            log.warn("Failed to update workflow step with merged batch data for heat treatment batch {}", 
                     heatTreatmentBatchId);
        }
    } catch (Exception e) {
        log.warn("Could not complete workflow step for item {}: {}", existingHeatTreatmentBatch.getProcessedItemHeatTreatmentBatches().get(0).getItem().getItemName(), e.getMessage());
    }

    return heatTreatmentBatchAssembler.dissemble(completedHeatTreatmentBatch);
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
   * @param tenantId The ID of the tenant
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
   * @param tenantId The ID of the tenant
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

    // 4. Validate no processedItemHeatTreatmentBatch is part of any MachiningBatch
    validateNoMachiningBatchExistsForProcessedItemHeatTreatmentBatches(heatTreatmentBatch);

    // 5. Handle inventory reversal based on whether this was the first operation or not
    LocalDateTime now = LocalDateTime.now();
    for (ProcessedItemHeatTreatmentBatch processedItemHeatTreatmentBatch : heatTreatmentBatch.getProcessedItemHeatTreatmentBatches()) {
      Item item = processedItemHeatTreatmentBatch.getItem();

      // Get the item workflow ID from the ProcessedItemHeatTreatmentBatch entity
      Long itemWorkflowId = processedItemHeatTreatmentBatch.getItemWorkflowId();
      
      if (itemWorkflowId != null) {
        try {
          // Get the workflow to check if heat treatment was the first operation
          ItemWorkflow workflow = itemWorkflowService.getItemWorkflowById(itemWorkflowId);
          boolean wasFirstOperation = WorkflowStep.OperationType.HEAT_TREATMENT.equals(
              workflow.getWorkflowTemplate().getFirstStep().getOperationType());
          
          if (wasFirstOperation) {
            // This was the first operation - return pieces to heat inventory
            revertHeatInventoryConsumption(heatTreatmentBatch);
            log.info("Successfully reverted heat inventory consumption for heat treatment batch {}", heatTreatmentBatchId);
          } else {
            // This was not the first operation - return pieces to previous operation
            // Note: In the new architecture, we need to determine the previous operation batch ID
            // This is a placeholder and needs to be implemented based on your workflow logic
            Long previousOperationBatchId = null; // TODO: Implement logic to get previous operation batch ID
            
            itemWorkflowService.returnPiecesToSpecificPreviousOperation(
                itemWorkflowId,
                WorkflowStep.OperationType.HEAT_TREATMENT,
                previousOperationBatchId,
                processedItemHeatTreatmentBatch.getHeatTreatBatchPiecesCount()
            );
            
            log.info("Successfully returned {} pieces from heat treatment back to previous operation {} in workflow {}", 
                     processedItemHeatTreatmentBatch.getHeatTreatBatchPiecesCount(),
                     previousOperationBatchId,
                     itemWorkflowId);
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

    // 7. Soft delete heat treatment heats if they exist
    if (heatTreatmentBatch.getHeatTreatmentHeats() != null && !heatTreatmentBatch.getHeatTreatmentHeats().isEmpty()) {
      heatTreatmentBatch.getHeatTreatmentHeats().forEach(heatTreatmentHeat -> {
        heatTreatmentHeat.setDeleted(true);
        heatTreatmentHeat.setDeletedAt(now);
      });
    }

    // 8. Store the original batch number and modify the batch number for deletion
    heatTreatmentBatch.setOriginalHeatTreatmentBatchNumber(heatTreatmentBatch.getHeatTreatmentBatchNumber());
    heatTreatmentBatch.setHeatTreatmentBatchNumber(
        heatTreatmentBatch.getHeatTreatmentBatchNumber() + "_deleted_" + heatTreatmentBatch.getId() + "_" + now.toEpochSecond(java.time.ZoneOffset.UTC)
    );

    // 9. Soft delete the HeatTreatmentBatch
    heatTreatmentBatch.setDeleted(true);
    heatTreatmentBatch.setDeletedAt(now);
    heatTreatmentBatchRepository.save(heatTreatmentBatch);

    log.info("Successfully deleted heat treatment batch={}, original batch number={}", 
        heatTreatmentBatchId, heatTreatmentBatch.getOriginalHeatTreatmentBatchNumber());
  }

  /**
   * Reverts heat inventory consumption when deleting a heat treatment batch that was the first operation
   */
  private void revertHeatInventoryConsumption(HeatTreatmentBatch heatTreatmentBatch) {
    if (heatTreatmentBatch.getHeatTreatmentHeats() == null || heatTreatmentBatch.getHeatTreatmentHeats().isEmpty()) {
      log.warn("No heat consumption data found for heat treatment batch {}. Cannot revert inventory.", 
               heatTreatmentBatch.getId());
      return;
    }

    // Revert each heat consumption
    heatTreatmentBatch.getHeatTreatmentHeats().forEach(heatTreatmentHeat -> {
      try {
        // Get the heat entity
        com.jangid.forging_process_management_service.entities.inventory.Heat heat = heatTreatmentHeat.getHeat();
        
        // Calculate new available pieces count after reverting consumption
        int newHeatPieces = heat.getAvailablePiecesCount() + heatTreatmentHeat.getPiecesUsed();
        
        // Update heat available pieces count
        log.info("Reverting inventory: Updating AvailablePiecesCount for heat={} from {} to {} for deleted heat treatment batch {}", 
                 heat.getId(), heat.getAvailablePiecesCount(), newHeatPieces, heatTreatmentBatch.getId());
        heat.setAvailablePiecesCount(newHeatPieces);
        
        // Persist the updated heat
        rawMaterialHeatService.updateRawMaterialHeat(heat);
        
        log.info("Successfully reverted {} pieces back to heat {} for deleted heat treatment batch {}", 
                 heatTreatmentHeat.getPiecesUsed(),
                 heatTreatmentHeat.getHeat().getId(),
                 heatTreatmentBatch.getId());
        
      } catch (Exception e) {
        log.error("Failed to revert inventory for heat {} in heat treatment batch {}: {}", 
                  heatTreatmentHeat.getHeat().getId(), heatTreatmentBatch.getId(), e.getMessage());
        throw new RuntimeException("Failed to revert heat inventory: " + e.getMessage(), e);
      }
    });

    log.info("Successfully reverted inventory consumption for {} heats in heat treatment batch {}", 
             heatTreatmentBatch.getHeatTreatmentHeats().size(), heatTreatmentBatch.getId());
  }

  private void validateNoMachiningBatchExistsForProcessedItemHeatTreatmentBatches(HeatTreatmentBatch heatTreatmentBatch) {
    for (ProcessedItemHeatTreatmentBatch processedItemHeatTreatmentBatch : heatTreatmentBatch.getProcessedItemHeatTreatmentBatches()) {
      // Check if any machining batch uses this processedItemHeatTreatmentBatch
      boolean isAnyMachiningBatchExistsForHeatTreatmentBatch = processedItemHeatTreatmentBatch.getMachiningBatches().stream().anyMatch(mb -> !mb.isDeleted());
      if (isAnyMachiningBatchExistsForHeatTreatmentBatch) {
        log.error("Cannot delete heat treatment batch={} as it has associated machining batches", heatTreatmentBatch.getId());
        throw new IllegalStateException("This heat treatment batch cannot be deleted as a machining batch entry exists for it.");
      }
    }
  }

  public boolean isHeatTreatmentBatchNumberPreviouslyUsed(String heatTreatmentBatchNumber, Long tenantId) {
    return heatTreatmentBatchRepository.existsByHeatTreatmentBatchNumberAndTenantIdAndOriginalHeatTreatmentBatchNumber(
        heatTreatmentBatchNumber, tenantId);
  }

  /**
   * Search for heat treatment batches by various criteria with pagination
   * @param tenantId The tenant ID
   * @param searchType The type of search (ITEM_NAME, FORGE_TRACEABILITY_NUMBER, HEAT_TREATMENT_BATCH_NUMBER, or FURNACE_NAME)
   * @param searchTerm The search term (substring matching for all search types)
   * @param page The page number (0-based)
   * @param size The page size
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
   * @param processedItemHeatTreatmentBatchIds List of processed item heat treatment batch IDs
   * @param tenantId The tenant ID for validation
   * @return List of HeatTreatmentBatchRepresentation
   */
  public List<HeatTreatmentBatchRepresentation> getHeatTreatmentBatchesByProcessedItemHeatTreatmentBatchIds(List<Long> processedItemHeatTreatmentBatchIds, Long tenantId) {
    if (processedItemHeatTreatmentBatchIds == null || processedItemHeatTreatmentBatchIds.isEmpty()) {
      log.info("No processed item heat treatment batch IDs provided, returning empty list");
      return Collections.emptyList();
    }

    log.info("Getting heat treatment batches for {} processed item heat treatment batch IDs for tenant {}", processedItemHeatTreatmentBatchIds.size(), tenantId);
    
    List<HeatTreatmentBatch> heatTreatmentBatches = heatTreatmentBatchRepository.findByProcessedItemHeatTreatmentBatchIdInAndDeletedFalse(processedItemHeatTreatmentBatchIds);
    
    // Filter and validate that all heat treatment batches belong to the tenant
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
          validHeatTreatmentBatches.add(heatTreatmentBatchAssembler.dissemble(heatTreatmentBatch));
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
    
    log.info("Found {} valid heat treatment batches out of {} requested processed item heat treatment batch IDs", validHeatTreatmentBatches.size(), processedItemHeatTreatmentBatchIds.size());
    return validHeatTreatmentBatches;
  }

}
