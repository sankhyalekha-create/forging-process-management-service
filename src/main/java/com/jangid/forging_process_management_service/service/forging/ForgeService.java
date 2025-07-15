package com.jangid.forging_process_management_service.service.forging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jangid.forging_process_management_service.assemblers.forging.ForgeAssembler;
import com.jangid.forging_process_management_service.assemblers.workflow.ItemWorkflowStepAssembler;
import com.jangid.forging_process_management_service.entities.Tenant;
import com.jangid.forging_process_management_service.entities.forging.Forge;
import com.jangid.forging_process_management_service.entities.forging.ForgeHeat;
import com.jangid.forging_process_management_service.entities.forging.ForgingLine;
import com.jangid.forging_process_management_service.entities.forging.ItemWeightType;
import com.jangid.forging_process_management_service.entities.ProcessedItem;
import com.jangid.forging_process_management_service.entities.inventory.Heat;
import com.jangid.forging_process_management_service.entities.product.Item;
import com.jangid.forging_process_management_service.entitiesRepresentation.dispatch.DispatchBatchRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.forging.ForgeRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.heating.HeatTreatmentBatchRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.machining.MachiningBatchRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.quality.InspectionBatchRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.workflow.ItemWorkflowStepRepresentation;
import com.jangid.forging_process_management_service.exception.forging.ForgeNotFoundException;
import com.jangid.forging_process_management_service.exception.forging.ForgeNotInExpectedStatusException;
import com.jangid.forging_process_management_service.exception.forging.ForgingLineOccupiedException;
import com.jangid.forging_process_management_service.repositories.forging.ForgeRepository;
import com.jangid.forging_process_management_service.exception.ResourceNotFoundException;
import com.jangid.forging_process_management_service.service.TenantService;
import com.jangid.forging_process_management_service.service.inventory.RawMaterialHeatService;
import com.jangid.forging_process_management_service.service.product.ItemService;
import com.jangid.forging_process_management_service.utils.ConvertorUtils;
import com.jangid.forging_process_management_service.dto.ForgeTraceabilitySearchResultDTO;
import com.jangid.forging_process_management_service.assemblers.dispatch.DispatchBatchAssembler;
import com.jangid.forging_process_management_service.assemblers.heating.HeatTreatmentBatchAssembler;
import com.jangid.forging_process_management_service.assemblers.machining.MachiningBatchAssembler;
import com.jangid.forging_process_management_service.assemblers.quality.InspectionBatchAssembler;
import com.jangid.forging_process_management_service.repositories.heating.HeatTreatmentBatchRepository;
import com.jangid.forging_process_management_service.repositories.machining.MachiningBatchRepository;
import com.jangid.forging_process_management_service.repositories.quality.InspectionBatchRepository;
import com.jangid.forging_process_management_service.repositories.dispatch.DispatchBatchRepository;
import com.jangid.forging_process_management_service.entities.forging.ForgeShift;
import com.jangid.forging_process_management_service.entities.forging.ForgeShiftHeat;
import com.jangid.forging_process_management_service.entitiesRepresentation.forging.ForgeShiftRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.forging.ForgeShiftHeatRepresentation;
import com.jangid.forging_process_management_service.repositories.forging.ForgeShiftRepository;
import com.jangid.forging_process_management_service.assemblers.forging.ForgeShiftAssembler;
import com.jangid.forging_process_management_service.assemblers.forging.ForgeShiftHeatAssembler;
import com.jangid.forging_process_management_service.service.workflow.ItemWorkflowService;
import com.jangid.forging_process_management_service.entities.workflow.WorkflowStep;
import com.jangid.forging_process_management_service.entities.workflow.ItemWorkflow;
import com.jangid.forging_process_management_service.entities.workflow.ItemWorkflowStep;
import com.jangid.forging_process_management_service.dto.workflow.OperationOutcomeData;
import com.jangid.forging_process_management_service.repositories.workflow.ItemWorkflowRepository;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Collections;
import java.util.Set;

@Slf4j
@Service
public class ForgeService {

  @Autowired
  private ForgeRepository forgeRepository;
  @Autowired
  private TenantService tenantService;

  @Autowired
  private RawMaterialHeatService rawMaterialHeatService;

  @Autowired
  private ItemService itemService;

  @Autowired
  private ForgingLineService forgingLineService;

  @Autowired
  private ForgeAssembler forgeAssembler;

  @Autowired
  private HeatTreatmentBatchAssembler heatTreatmentBatchAssembler;
  
  @Autowired
  private MachiningBatchAssembler machiningBatchAssembler;
  
  @Autowired
  private InspectionBatchAssembler inspectionBatchAssembler;
  
  @Autowired
  private DispatchBatchAssembler dispatchBatchAssembler;

  @Autowired
  private HeatTreatmentBatchRepository heatTreatmentBatchRepository;
  
  @Autowired
  private MachiningBatchRepository machiningBatchRepository;
  
  @Autowired
  private InspectionBatchRepository inspectionBatchRepository;
  
  @Autowired
  private DispatchBatchRepository dispatchBatchRepository;

  @Autowired
  private ForgeShiftRepository forgeShiftRepository;

  @Autowired
  private ForgeShiftAssembler forgeShiftAssembler;

  @Autowired
  private ForgeShiftHeatAssembler forgeShiftHeatAssembler;

  @Autowired
  private ItemWorkflowService itemWorkflowService;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private ItemWorkflowRepository itemWorkflowRepository;

  @Autowired
  private ItemWorkflowStepAssembler itemWorkflowStepAssembler;

  public Page<ForgeRepresentation> getAllForges(long tenantId, int page, int size) {
    Pageable pageable = PageRequest.of(page, size);

    List<Long> forgingLineIds = forgingLineService.getAllForgingLinesByTenant(tenantId)
        .stream()
        .map(ForgingLine::getId)
        .collect(Collectors.toList());

    if (forgingLineIds.isEmpty()) {
      return Page.empty(pageable);
    }

    Page<Forge> forgePage = forgeRepository.findByForgingLineIdInAndDeletedFalseOrderByUpdatedAtDesc(
        forgingLineIds, pageable);

    return forgePage.map(forgeAssembler::dissemble);
  }

  public Forge getForgeByIdAndForgingLineId(Long id, Long forgingLineId) {
    Optional<Forge> forgeOptional = forgeRepository.findByIdAndAndForgingLineIdAndDeletedFalse(id, forgingLineId);
    if (forgeOptional.isEmpty()) {
      log.error("Forge does not exists for forgeId={} having forgingLineId={}", id, forgingLineId);
      throw new ForgeNotFoundException("Forge does not exists for forgeId=" + id + " having forgingLineId=" + forgingLineId);
    }
    return forgeOptional.get();
  }

  @Transactional // Ensures all database operations succeed or roll back
  public ForgeRepresentation applyForge(long tenantId, long forgingLineId, ForgeRepresentation representation) {
    // 1. Validate inputs and preconditions
    validateApplyForgeInputs(tenantId, forgingLineId, representation);
    
    // 2. Process heat quantity updates
    LocalDateTime applyAtLocalDateTime = processHeatQuantityUpdates(representation, tenantId);
    
    // 3. Create and persist forge with processed item
    Forge createdForge = createAndPersistForge(representation, tenantId, forgingLineId, applyAtLocalDateTime);
    
    // 4. Update forging line status
    updateForgingLineStatus(forgingLineId, tenantId);
    
    // 5. Integrate with workflow system
    integrateWithWorkflowSystem(createdForge, representation);
    
    // 6. Return the created forge representation
    return forgeAssembler.dissemble(createdForge);
  }

  /**
   * Validates all inputs and preconditions for applying a forge
   */
  private void validateApplyForgeInputs(long tenantId, long forgingLineId, ForgeRepresentation representation) {
    tenantService.validateTenantExists(tenantId);
    ForgingLine forgingLine = getForgingLineUsingTenantIdAndForgingLineId(tenantId, forgingLineId);
    boolean isForgeAppliedOnForgingLine = isForgeAppliedOnForgingLine(forgingLineId);

    if (isForgeAppliedOnForgingLine) {
      log.error("ForgingLine={} is already having a forge set. Cannot create a new forge on this forging line", forgingLineId);
      throw new ForgingLineOccupiedException("Cannot create a new forge on this forging line as ForgingLine " + forgingLineId + " is already occupied");
    }

    // Validate workflow before processing
    Long itemId = representation.getProcessedItem().getItem().getId();
    
    // Validate workflow identifier is provided in ProcessedItem
    String workflowIdentifier = representation.getProcessedItem().getWorkflowIdentifier();
    if (workflowIdentifier == null || workflowIdentifier.trim().isEmpty()) {
      log.error("Workflow identifier is required for forge operation");
      throw new IllegalArgumentException("Workflow identifier is required for forge operation");
    }
    
    // Check if workflow identifier is already in use
    List<ItemWorkflow> existingWorkflows = itemWorkflowService.getAllWorkflowsForItem(itemId);
    boolean workflowIdentifierExists = existingWorkflows.stream()
        .anyMatch(w -> workflowIdentifier.equals(w.getWorkflowIdentifier()));
    
    if (workflowIdentifierExists) {
      log.error("Workflow identifier {} is already in use", workflowIdentifier);
      throw new IllegalArgumentException("Workflow identifier " + workflowIdentifier + " is already in use");
    }
  }

  /**
   * Processes heat quantity updates and validations
   * @return the parsed applyAt LocalDateTime
   */
  private LocalDateTime processHeatQuantityUpdates(ForgeRepresentation representation, long tenantId) {
    LocalDateTime applyAtLocalDateTime = ConvertorUtils.convertStringToLocalDateTime(representation.getApplyAt());

    // Update heat quantities - CRITICAL BUSINESS LOGIC
    representation.getForgeHeats().forEach(forgeHeat -> {
      Heat heat = rawMaterialHeatService.getRawMaterialHeatById(forgeHeat.getHeat().getId());
      double newHeatQuantity = heat.getAvailableHeatQuantity() - roundToGramLevel(Double.parseDouble(forgeHeat.getHeatQuantityUsed()));
      if (newHeatQuantity < 0) {
        log.error("Insufficient heat quantity for heat={} on tenantId={}", heat.getId(), tenantId);
        throw new IllegalArgumentException("Insufficient heat quantity for heat " + heat.getId());
      }
      LocalDateTime heatReceivingDateTime = heat.getRawMaterialProduct().getRawMaterial().getRawMaterialReceivingDate();
      if (heatReceivingDateTime.compareTo(applyAtLocalDateTime) > 0) {
        log.error("The provided apply at time={} is before to heat={} received at time={} !", applyAtLocalDateTime,
                  heat.getHeatNumber(), heatReceivingDateTime);
        throw new RuntimeException(
            "The provided apply at time=" + applyAtLocalDateTime + " is before to heat=" + heat.getHeatNumber() + " received at time=" + heatReceivingDateTime
            + " !");
      }
      log.info("Updating AvailableHeatQuantity for heat={} to {}", heat.getId(), newHeatQuantity);
      heat.setAvailableHeatQuantity(newHeatQuantity);
      rawMaterialHeatService.updateRawMaterialHeat(heat); // Persist the updated heat
    });
    
    return applyAtLocalDateTime;
  }

  /**
   * Creates and persists the forge entity with its processed item
   */
  private Forge createAndPersistForge(ForgeRepresentation representation, long tenantId, long forgingLineId, LocalDateTime applyAtLocalDateTime) {
    // Create the forge and processed item
    Forge inputForge = forgeAssembler.createAssemble(representation);
    inputForge.getForgeHeats().forEach(forgeHeat -> forgeHeat.setForge(inputForge));
    
    ForgingLine forgingLine = getForgingLineUsingTenantIdAndForgingLineId(tenantId, forgingLineId);
    inputForge.setForgingLine(forgingLine);
    inputForge.setCreatedAt(LocalDateTime.now());
    inputForge.setApplyAt(applyAtLocalDateTime);

    Item item = itemService.getItemByIdAndTenantId(representation.getProcessedItem().getItem().getId(), tenantId);
    
    // Get the itemWeightType from the inputForge entity
    ItemWeightType weightType = inputForge.getItemWeightType();
    
    // Determine which weight to use based on itemWeightType
    Double itemWeight = determineItemWeight(item, weightType);
    
    Double totalHeatReserved = representation.getForgeHeats().stream()
        .mapToDouble(forgeHeat -> roundToGramLevel(Double.parseDouble(forgeHeat.getHeatQuantityUsed())))
        .sum();

    ProcessedItem processedItem = ProcessedItem.builder()
        .item(item)
        .forge(inputForge)
        .expectedForgePiecesCount((int) Math.floor(totalHeatReserved / itemWeight))
        .workflowIdentifier(representation.getProcessedItem().getWorkflowIdentifier())
        .createdAt(LocalDateTime.now())
        .build();

    inputForge.setProcessedItem(processedItem);
    Tenant tenant = tenantService.getTenantById(tenantId);
    inputForge.setTenant(tenant);

    return forgeRepository.save(inputForge); // Save forge entity
  }

  /**
   * Updates the forging line status after forge application
   */
  private void updateForgingLineStatus(long forgingLineId, long tenantId) {
    ForgingLine forgingLine = forgingLineService.getForgingLineByIdAndTenantId(forgingLineId, tenantId);
    forgingLine.setForgingLineStatus(ForgingLine.ForgingLineStatus.FORGE_APPLIED);
    forgingLineService.saveForgingLine(forgingLine);
  }

  /**
   * Integrates the created forge with the workflow system
   */
  private void integrateWithWorkflowSystem(Forge createdForge, ForgeRepresentation representation) {
    try {
      Long itemId = createdForge.getProcessedItem().getItem().getId();
      String workflowIdentifier = createdForge.getProcessedItem().getWorkflowIdentifier();
      
      log.info("Starting workflow integration for forging operation on item {}", itemId);
      
      // Get itemWorkflowId from ProcessedItem (stored as Long)
      Long itemWorkflowId = createdForge.getProcessedItem().getItemWorkflowId();

      // Use the generic workflow handling method
      Item item = createdForge.getProcessedItem().getItem();
      ItemWorkflow workflow = itemWorkflowService.handleWorkflowForOperation(
          item,
          WorkflowStep.OperationType.FORGING,
          workflowIdentifier,
          itemWorkflowId
      );
      
      // Update the ProcessedItem with the workflow ID (store as Long)
      createdForge.getProcessedItem().setItemWorkflowId(workflow.getId());
      forgeRepository.save(createdForge);
      
      log.info("Successfully integrated forge creation with workflow system. " +
               "Forge ID: {}, Workflow ID: {}, WorkflowIdentifier: {}",
               createdForge.getId(), workflow.getId(), workflow.getWorkflowIdentifier());
      
      // Update relatedEntityIds for FORGING operation step with ProcessedItem.id
      itemWorkflowService.updateRelatedEntityIds(workflow.getId(), WorkflowStep.OperationType.FORGING, createdForge.getProcessedItem().getId());
      
    } catch (Exception e) {
      log.error("Failed to update workflow for forging on item {}: {}", 
                createdForge.getProcessedItem().getItem().getId(), e.getMessage());
      // Don't fail the entire operation, but log the error for monitoring
    }
  }

  private String getForgeTraceabilityNumber(long tenantId, String forgingLineName, long forgingLineId, LocalDateTime startAt) {
    Tenant tenant = tenantService.getTenantById(tenantId);
    String initialsOfTenant = getInitialsOfTenant(tenant.getTenantName());
    String localDate = startAt.toLocalDate().toString();

    // Construct the prefix for the forge traceability number
    String forgePrefix = initialsOfTenant + forgingLineName + localDate;

    // Fetch the list of forge entries for this forging line on the current day
    List<Forge> forgesForTheDay = forgeRepository.findLastForgeForTheDay(forgingLineId, forgePrefix);

    // Determine the counter value based on the latest entry
    int counter = forgesForTheDay.stream()
        .findFirst()
        .map(forge -> Integer.parseInt(forge.getForgeTraceabilityNumber()
                                           .substring(forge.getForgeTraceabilityNumber().lastIndexOf("-") + 1)) + 1)
        .orElse(1);

    return forgePrefix + "-" + counter;
  }


  private String getInitialsOfTenant(String tenantName) {
    if (tenantName == null || tenantName.isEmpty()) {
      return "";
    }
    return Arrays.stream(tenantName.trim().split("\\s+"))
        .map(word -> String.valueOf(Character.toUpperCase(word.charAt(0))))
        .collect(Collectors.joining());
  }

  @Transactional
  public ForgeRepresentation startForge(long tenantId, long forgingLineId, long forgeId, String startAt) {
    tenantService.validateTenantExists(tenantId);
    ForgingLine forgingLine = getForgingLineUsingTenantIdAndForgingLineId(tenantId, forgingLineId);
    boolean isForgeAppliedOnForgingLine = isForgeAppliedOnForgingLine(forgingLine.getId());

    if (!isForgeAppliedOnForgingLine) {
      log.error("ForgingLine={} does not have a forge existingForge set. Can not start forge existingForge on this forging line as it does not have forge existingForge", forgingLineId);
      throw new ForgeNotFoundException("Forge does not exists for forgingLine!");
    }
    Forge existingForge = getForgeById(forgeId);

    if (existingForge.getStartAt() != null) {
      log.error("The forge={} having traceability={} has already been started!", forgeId, existingForge.getForgeTraceabilityNumber());
      throw new ForgeNotInExpectedStatusException("Forge=" + forgeId + " , traceability=" + existingForge.getForgeTraceabilityNumber() + "has already been started!");
    }

    LocalDateTime startTimeLocalDateTime = ConvertorUtils.convertStringToLocalDateTime(startAt);

    if (existingForge.getApplyAt().compareTo(startTimeLocalDateTime) > 0) {
      log.error("The forge having forge traceability number={} provided start time={} is before apply at time={} !", existingForge.getForgeTraceabilityNumber(), startTimeLocalDateTime,
                existingForge.getApplyAt());
      throw new RuntimeException(
          "The forge having forge traceability number=" + existingForge.getForgeTraceabilityNumber() + " provided start time=" + startTimeLocalDateTime + " is before apply at time="
          + existingForge.getApplyAt());
    }

    if (!Forge.ForgeStatus.IDLE.equals(existingForge.getForgingStatus())) {
      log.error("The forge={} having traceability={} is not in IDLE status to start it!", forgeId, existingForge.getForgeTraceabilityNumber());
      throw new ForgeNotInExpectedStatusException("Forge=" + forgeId + " , traceability=" + existingForge.getForgeTraceabilityNumber() + "Not in IDLE status to start it!");
    }

    existingForge.setForgingStatus(Forge.ForgeStatus.IN_PROGRESS);
    existingForge.setStartAt(ConvertorUtils.convertStringToLocalDateTime(startAt));
    existingForge.setForgeTraceabilityNumber(getForgeTraceabilityNumber(tenantId, forgingLine.getForgingLineName(), forgingLineId, startTimeLocalDateTime));

    Forge startedForge = forgeRepository.save(existingForge);

    updateWorkflowForForgeStart(startedForge, startTimeLocalDateTime);

    forgingLine.setForgingLineStatus(ForgingLine.ForgingLineStatus.FORGE_IN_PROGRESS);
    forgingLineService.saveForgingLine(forgingLine);

    return forgeAssembler.dissemble(startedForge);
  }

  @Transactional
  public ForgeRepresentation endForge(long tenantId, long forgingLineId, long forgeId, String endAt, Long itemWorkflowId) {
    tenantService.validateTenantExists(tenantId);
    ForgingLine forgingLine = getForgingLineUsingTenantIdAndForgingLineId(tenantId, forgingLineId);
    boolean isForgeAppliedOnForgingLine = isForgeAppliedOnForgingLine(forgingLine.getId());

    if (!isForgeAppliedOnForgingLine) {
      log.error("ForgingLine={} does not have a forge existingForge set. Can not end forge existingForge on this forging line as it does not have forge existingForge", forgingLineId);
      throw new ForgeNotFoundException("Forge does not exists for forgingLine!");
    }
    Forge existingForge = getForgeById(forgeId);

    if (existingForge.getEndAt() != null) {
      log.error("The forge={} having traceability={} has already been ended!", forgeId, existingForge.getForgeTraceabilityNumber());
      throw new ForgeNotInExpectedStatusException("Forge=" + forgeId + " , traceability=" + existingForge.getForgeTraceabilityNumber() + "has already been ended!");
    }

    if (!Forge.ForgeStatus.IN_PROGRESS.equals(existingForge.getForgingStatus())) {
      log.error("The forge={} having traceability={} is not in IN_PROGRESS status to end it!", forgeId, existingForge.getForgeTraceabilityNumber());
      throw new ForgeNotInExpectedStatusException("Forge=" + forgeId + " , traceability=" + existingForge.getForgeTraceabilityNumber() + "Not in IN_PROGRESS status to end it!");
    }
    
    // 5. Validate end time
    LocalDateTime endDateTime = ConvertorUtils.convertStringToLocalDateTime(endAt);
    
    if (existingForge.getStartAt().compareTo(endDateTime) > 0) {
      log.error("The forge={} having traceability={} end time is before the forge start time!", forgeId, existingForge.getForgeTraceabilityNumber());
      throw new RuntimeException("Forge=" + forgeId + " , traceability=" + existingForge.getForgeTraceabilityNumber() + " end time is before the forge start time!");
    }
    
    // Validate end time is after the last forge shift end time
    if (existingForge.getForgeShifts() != null && !existingForge.getForgeShifts().isEmpty()) {
      ForgeShift latestShift = existingForge.getLatestForgeShift();
      if (latestShift != null && endDateTime.compareTo(latestShift.getEndDateTime()) < 0) {
        log.error("Forge end time {} is before the latest forge shift end time {}", endDateTime, latestShift.getEndDateTime());
        throw new IllegalArgumentException("Forge end time must be equal to or after the latest forge shift end time");
      }
    }
    
    // 6. Process heat quantity adjustments based on forge shifts usage
    processHeatQuantityAdjustmentsOnForgeCompletion(existingForge);
    
    // 7. Update processedItem fields based on forge shifts totals
    existingForge.setForgingStatus(Forge.ForgeStatus.COMPLETED);
    existingForge.setEndAt(endDateTime);
    
    // 8. Save the completed forge
    Forge endedForge = forgeRepository.save(existingForge);

    // 9. Update forging line status
    forgingLine.setForgingLineStatus(ForgingLine.ForgingLineStatus.FORGE_NOT_APPLIED);
    forgingLineService.saveForgingLine(forgingLine);
    
    // 10. Update ItemWorkflowStep entities for the provided itemWorkflowId
    updateItemWorkflowStepsForForgeCompletion(itemWorkflowId, endedForge, endDateTime);
    
    log.info("Forge ID={} completed successfully at {}", forgeId, endDateTime);

    return forgeAssembler.dissemble(endedForge);
  }

  /**
   * Updates the ItemWorkflowStep entities associated with the provided itemWorkflowId
   * Sets the FORGING step status to COMPLETED and sets completedAt timestamp
   * 
   * @param itemWorkflowId The workflow ID to update
   * @param completedForge The completed forge entity
   * @param completedAt The completion timestamp
   */
  private void updateItemWorkflowStepsForForgeCompletion(Long itemWorkflowId, Forge completedForge, LocalDateTime completedAt) {
    try {
      if (itemWorkflowId == null) {
        log.warn("itemWorkflowId is null, skipping ItemWorkflowStep updates for forge completion");
        return;
      }
      
      log.info("Updating ItemWorkflowStep entities for itemWorkflowId: {} after forge completion", itemWorkflowId);
      
      // Get the ItemWorkflow
      ItemWorkflow itemWorkflow = itemWorkflowService.getItemWorkflowById(itemWorkflowId);
      
      // Validate that the workflow belongs to the same item as the forge
      Long forgeItemId = completedForge.getProcessedItem().getItem().getId();
      Long workflowItemId = itemWorkflow.getItem().getId();
      
      if (!forgeItemId.equals(workflowItemId)) {
        log.error("ItemWorkflow {} does not belong to the same item as forge {}. Forge item ID: {}, Workflow item ID: {}", 
                 itemWorkflowId, completedForge.getId(), forgeItemId, workflowItemId);
        throw new IllegalArgumentException("ItemWorkflow does not belong to the same item as the forge");
      }
      
      // Find the FORGING operation step using the new operationType column for efficient filtering
      ItemWorkflowStep forgingStep = itemWorkflow.getItemWorkflowSteps().stream()
          .filter(step -> step.getOperationType() == WorkflowStep.OperationType.FORGING)
          .findFirst()
          .orElse(null);
      
      if (forgingStep != null) {

        // Deserialize, update, and serialize back the operation outcome data
        try {
          String existingOutcomeDataJson = forgingStep.getOperationOutcomeData();
          if (existingOutcomeDataJson != null && !existingOutcomeDataJson.trim().isEmpty()) {
            OperationOutcomeData operationOutcomeData = objectMapper.readValue(existingOutcomeDataJson, OperationOutcomeData.class);
            operationOutcomeData.setOperationLastUpdatedAt(completedAt);
            OperationOutcomeData.ForgingOutcome forgingOutcome = operationOutcomeData.getForgingData();
            forgingOutcome.setCompletedAt(completedAt);
            forgingStep.setOperationOutcomeData(objectMapper.writeValueAsString(operationOutcomeData));
          } else {
            log.warn("No operation outcome data found for FORGING step in workflow for itemWorkflowId: {}", itemWorkflowId);
          }
        } catch (Exception e) {
          log.error("Failed to update operation outcome data for FORGING step: {}", e.getMessage());
          throw new RuntimeException("Failed to update operation outcome data: " + e.getMessage(), e);
        }
      } else {
        log.warn("No FORGING step found in workflow for itemWorkflowId: {}", itemWorkflowId);
      }
      
      // Save the updated workflow
      itemWorkflowRepository.save(itemWorkflow);
      
    } catch (Exception e) {
      log.error("Failed to update ItemWorkflowStep entities for forge completion on itemWorkflowId: {} - {}", itemWorkflowId, e.getMessage());
      // Re-throw the exception to fail the forge completion since workflow integration is now mandatory
      throw new RuntimeException("Failed to update workflow step for forge completion: " + e.getMessage(), e);
    }
  }

  /**
   * Processes heat quantity adjustments when completing a forge
   * Returns unused heat quantities back to inventory if total forge shifts usage is less than original allocation
   */
  private void processHeatQuantityAdjustmentsOnForgeCompletion(Forge forge) {
    log.info("Processing heat quantity adjustments for forge completion, forge ID={}", forge.getId());
    
    // Get item weight for calculations
    ItemWeightType weightType = forge.getItemWeightType();
    Double itemWeight = determineItemWeight(forge.getProcessedItem().getItem(), weightType);
    
    // Create a map of original forge heat allocations (heatId -> quantity allocated during applyForge)
    Map<Long, Double> originalHeatAllocations = forge.getForgeHeats().stream()
        .collect(Collectors.toMap(
            fh -> fh.getHeat().getId(),
            ForgeHeat::getHeatQuantityUsed
        ));
    
    log.info("Original heat allocations: {}", originalHeatAllocations);
    
    // Calculate total usage of each heat across all forge shifts
    Map<Long, Double> totalUsageByHeat = new HashMap<>();
    
    if (forge.getForgeShifts() != null && !forge.getForgeShifts().isEmpty()) {
      for (ForgeShift forgeShift : forge.getForgeShifts()) {
        if (!forgeShift.isDeleted()) {
          for (ForgeShiftHeat shiftHeat : forgeShift.getForgeShiftHeats()) {
            if (!shiftHeat.isDeleted()) {
              Long heatId = shiftHeat.getHeat().getId();
              double usage = shiftHeat.getHeatQuantityUsed();
              totalUsageByHeat.merge(heatId, usage, Double::sum);
            }
          }
        }
      }
    }
    
    log.info("Total usage by heat across all forge shifts: {}", totalUsageByHeat);
    
    // Process each original heat allocation
    for (Map.Entry<Long, Double> entry : originalHeatAllocations.entrySet()) {
      Long heatId = entry.getKey();
      double originalAllocation = entry.getValue();
      double totalUsage = totalUsageByHeat.getOrDefault(heatId, 0.0);
      
      // Find the corresponding ForgeHeat entity to update
      ForgeHeat forgeHeat = forge.getForgeHeats().stream()
          .filter(fh -> fh.getHeat().getId().equals(heatId))
          .findFirst()
          .orElse(null);
      
      if (totalUsage < originalAllocation) {
        // Return unused quantity back to heat inventory
        double unusedQuantity = originalAllocation - totalUsage;
        
        Heat heat = rawMaterialHeatService.getRawMaterialHeatById(heatId);
        double newAvailableQuantity = heat.getAvailableHeatQuantity() + unusedQuantity;
        heat.setAvailableHeatQuantity(newAvailableQuantity);
        rawMaterialHeatService.updateRawMaterialHeat(heat);
        
        // Record the returned quantity in ForgeHeat for audit trail
        if (forgeHeat != null) {
          forgeHeat.setHeatQuantityReturned(unusedQuantity);
        }
        
        log.info("Returned unused heat quantity for heat ID={}: original allocation={}, total usage={}, returned={}",
                heatId, originalAllocation, totalUsage, unusedQuantity);
      } else if (totalUsage > originalAllocation) {
        // This should not happen as forge shifts should have already deducted extra usage
        log.warn("Total usage ({}) exceeds original allocation ({}) for heat ID={}. This should have been handled during forge shifts.",
                totalUsage, originalAllocation, heatId);
      } else {
        log.info("Heat ID={}: Total usage matches original allocation ({}), no adjustment needed", heatId, originalAllocation);
        
        // Set returned quantity to 0 for clarity in audit trail
        if (forgeHeat != null) {
          forgeHeat.setHeatQuantityReturned(0.0);
        }
      }
    }
  }

  public ForgingLine getForgingLineUsingTenantIdAndForgingLineId(long tenantId, long forgingLineId) {
    boolean isForgingLineOfTenantExists = forgingLineService.isForgingLineByTenantExists(tenantId);
    if (!isForgingLineOfTenantExists) {
      log.error("Forging Line={} does not exist!", forgingLineId);
      throw new ResourceNotFoundException("Forging Line for the tenant does not exist!");
    }
    return forgingLineService.getForgingLineByIdAndTenantId(forgingLineId, tenantId);
  }

  public boolean isForgeAppliedOnForgingLine(long forgingLineId) {
    Optional<Forge> forgeOptional = forgeRepository.findAppliedForgeOnForgingLine(forgingLineId);
    if (forgeOptional.isPresent()) {
      log.info("Forge={} already applied on forgingLineId={}", forgeOptional.get().getId(), forgingLineId);
      return true;
    }
    return false;
  }

  public Forge getAppliedForgeByForgingLine(long forgingLineId) {
    Optional<Forge> forgeOptional = forgeRepository.findAppliedForgeOnForgingLine(forgingLineId);
    if (forgeOptional.isEmpty()) {
      log.error("Forge does not exists for forgingLineId={}", forgingLineId);
      throw new ForgeNotFoundException("Forge does not exists for forgingLine!");
    }
    return forgeOptional.get();
  }

  @Transactional
  public void deleteForge(Long tenantId, Long forgeId) {
    // 1. Validate tenant exists
    tenantService.validateTenantExists(tenantId);

    // 2. Validate forge exists and belongs to tenant
    Forge forge = getForgeByIdAndTenantId(forgeId, tenantId);

    // 3. Validate forge status is COMPLETED
    if (Forge.ForgeStatus.COMPLETED != forge.getForgingStatus()) {
        log.error("Cannot delete forge as it is not in COMPLETED status. Current status: {}", forge.getForgingStatus());
        throw new IllegalStateException("This forging cannot be deleted as it is not in the COMPLETED status.");
    }

    // 4. Check if forge can be deleted by validating next operation batches are all deleted
    ProcessedItem processedItem = forge.getProcessedItem();
    
    // Get the ItemWorkflowStep using the processedItem associated to the forge
    // The processedItem ID is the single entry in relatedEntityIds column for FORGING operation
    Long itemWorkflowId = processedItem.getItemWorkflowId();
    
    if (itemWorkflowId != null) {
        // Use workflow-based validation: check if all entries in next operation are marked deleted
        boolean canDeleteForge = itemWorkflowService.areAllNextOperationBatchesDeleted(itemWorkflowId, WorkflowStep.OperationType.FORGING);
        
        if (!canDeleteForge) {
            log.error("Cannot delete forge id={} as the next operation has active (non-deleted) batches", forgeId);
            throw new IllegalStateException("This forging cannot be deleted as the next operation has active batch entries.");
        }
        
        log.info("Forge id={} is eligible for deletion - all next operation batches are deleted", forgeId);
    } else {
        // No workflow found - allowing deletion as this may be a legacy forge without workflow
        log.warn("No workflow found for forge id={}, allowing deletion (legacy forge without workflow)", forgeId);
    }

    // 5. Update workflow step to mark forging operation as deleted and adjust piece counts
    if (itemWorkflowId != null) {
        Integer actualForgePiecesCount = processedItem.getActualForgePiecesCount();
        if (actualForgePiecesCount != null && actualForgePiecesCount > 0) {
            try {
                itemWorkflowService.markOperationAsDeletedAndUpdatePieceCounts(
                    itemWorkflowId, 
                    WorkflowStep.OperationType.FORGING, 
                    actualForgePiecesCount
                );
                log.info("Successfully marked forge operation as deleted and updated workflow step for forge id={}, subtracted {} pieces", 
                         forgeId, actualForgePiecesCount);
            } catch (Exception e) {
                log.error("Failed to update workflow step for deleted forge id={}: {}", forgeId, e.getMessage());
                throw new RuntimeException("Failed to update workflow step for forge deletion: " + e.getMessage(), e);
            }
        } else {
            log.info("No actual forged pieces to subtract for deleted forge id={}", forgeId);
        }
    }

    // 6. Return heat quantities to original heats
    LocalDateTime currentTime = LocalDateTime.now();
    forge.getForgeHeats().forEach(forgeHeat -> {
        Heat heat = forgeHeat.getHeat();
        double quantityToReturn = forgeHeat.getHeatQuantityUsed();
        double newAvailableQuantity = heat.getAvailableHeatQuantity() + quantityToReturn;
        heat.setAvailableHeatQuantity(newAvailableQuantity);
        rawMaterialHeatService.updateRawMaterialHeat(heat); // Persist the updated heat

        // Soft delete forge heat
        forgeHeat.setDeleted(true);
        forgeHeat.setDeletedAt(currentTime);
    });

    // 7. Soft delete ProcessedItem
    processedItem.setDeleted(true);
    processedItem.setDeletedAt(currentTime);

    // 8. Soft delete Forge
    forge.setDeleted(true);
    forge.setDeletedAt(currentTime);

    // Save the updated forge which will cascade to processed item and forge heats
    forgeRepository.save(forge);
  }

  public Forge getForgeById(long forgeId) {
    Optional<Forge> forgeOptional = forgeRepository.findByIdAndDeletedFalse(forgeId);
    if (forgeOptional.isEmpty()) {
      log.error("Forge does not exists for forgeId={}", forgeId);
      throw new ForgeNotFoundException("Forge does not exists for forgeId=" + forgeId);
    }
    return forgeOptional.get();
  }

  public Forge getForgeByIdAndTenantId(long forgeId, long tenantId) {
    Optional<Forge> forgeOptional = forgeRepository.findByIdAndTenantIdAndDeletedFalse(forgeId, tenantId);
    if (forgeOptional.isEmpty()) {
      log.error("Forge does not exists for forgeId={}, tenantId={}", forgeId, tenantId);
      throw new ForgeNotFoundException("Forge does not exists for forgeId=" + forgeId + ", tenantId=" + tenantId);
    }
    return forgeOptional.get();
  }

  public Forge getForgeByForgeTraceabilityNumber(String forgeTraceabilityNumber) {
    Optional<Forge> forgeOptional = forgeRepository.findByForgeTraceabilityNumberAndDeletedFalse(forgeTraceabilityNumber);
    if (forgeOptional.isEmpty()) {
      log.error("Forge does not exists for forgeTraceabilityNumber={}", forgeTraceabilityNumber);
      throw new ForgeNotFoundException("Forge does not exists for forgeTraceabilityNumber=" + forgeTraceabilityNumber);
    }
    return forgeOptional.get();
  }

  public List<Forge> getForgeTraceabilitiesByHeatNumber(long tenantId, String heatNumber) {
//    return rawMaterialService.getRawMaterialByHeatNumber(tenantId, heatNumber).stream()
//        .flatMap(rm -> rm.getHeats().stream())
//        .filter(rmh -> heatNumber.equals(rmh.getHeatNumber()))
//        .flatMap(rmh -> getForgeTraceabilitiesByHeatId(rmh.getId()).stream())
//        .collect(Collectors.toList());
    return null;
  }

  //  public List<Forge> getForgeTraceabilitiesByHeatId(long heatId){
//    return forgeRepository.findByHeatIdAndDeletedFalse(heatId);
//  }
  @Transactional
  public Forge saveForge(Forge forge) {
    return forgeRepository.save(forge);
  }

  /**
   * Search for a forge and all its related entities by forge traceability number
   * @param forgeTraceabilityNumber The forge traceability number to search for
   * @return A DTO containing the forge and related entities information
   */
  @Transactional(readOnly = true)
  public ForgeTraceabilitySearchResultDTO searchByForgeTraceabilityNumber(String forgeTraceabilityNumber) {
    Forge forge = getForgeByForgeTraceabilityNumber(forgeTraceabilityNumber);
    ForgeRepresentation forgeRepresentation = forgeAssembler.dissemble(forge);
    
    // Find related heat treatment batches using repository
    List<HeatTreatmentBatchRepresentation> heatTreatmentBatchRepresentations = heatTreatmentBatchRepository
        .findByForgeTraceabilityNumber(forgeTraceabilityNumber)
        .stream()
        .map(heatTreatmentBatchAssembler::dissemble)
        .collect(Collectors.toList());
    
    // Find related machining batches using repository
    List<MachiningBatchRepresentation> machiningBatchRepresentations = machiningBatchRepository
        .findByForgeTraceabilityNumber(forgeTraceabilityNumber)
        .stream()
        .map(machiningBatchAssembler::dissemble)
        .collect(Collectors.toList());
    
    // Find related inspection batches using repository
    List<InspectionBatchRepresentation> inspectionBatchRepresentations = inspectionBatchRepository
        .findByForgeTraceabilityNumber(forgeTraceabilityNumber)
        .stream()
        .map(inspectionBatchAssembler::dissemble)
        .collect(Collectors.toList());
    
    // Find related dispatch batches using repository
    List<DispatchBatchRepresentation> dispatchBatchRepresentations = dispatchBatchRepository
        .findByForgeTraceabilityNumber(forgeTraceabilityNumber)
        .stream()
        .map(dispatchBatchAssembler::dissemble)
        .collect(Collectors.toList());
    
    return ForgeTraceabilitySearchResultDTO.builder()
        .forge(forgeRepresentation)
        .heatTreatmentBatches(heatTreatmentBatchRepresentations)
        .machiningBatches(machiningBatchRepresentations)
        .inspectionBatches(inspectionBatchRepresentations)
        .dispatchBatches(dispatchBatchRepresentations)
        .build();
  }

  /**
   * Determines which weight value to use based on the specified weight type
   * @param item The item from which to extract the weight
   * @param weightType The type of weight to use from the ItemWeightType enum
   * @return The appropriate weight value based on the weight type
   * @throws IllegalArgumentException if the selected weight type is null for the item
   */
  private Double determineItemWeight(Item item, ItemWeightType weightType) {
    Double itemWeight;
    
    if (weightType == null) {
        weightType = ItemWeightType.getDefault();
    }
    
    switch (weightType) {
        case ITEM_SLUG_WEIGHT:
            itemWeight = item.getItemSlugWeight();
            log.info("Using item slug weight: {}", itemWeight);
            break;
        case ITEM_FORGED_WEIGHT:
            itemWeight = item.getItemForgedWeight();
            log.info("Using item forged weight: {}", itemWeight);
            break;
        case ITEM_FINISHED_WEIGHT:
            itemWeight = item.getItemFinishedWeight();
            log.info("Using item finished weight: {}", itemWeight);
            break;
        case ITEM_WEIGHT:
        default:
            itemWeight = item.getItemWeight();
            log.info("Using item weight: {}", itemWeight);
            break;
    }
    
    if (itemWeight == null) {
        log.error("Selected weight type {} is null for item {}", weightType, item.getId());
        throw new IllegalArgumentException("Selected weight type " + weightType + " is null for item " + item.getId());
    }
    
    return itemWeight;
  }

  /**
   * Search for forges by item name, forge traceability number, or forging line name with pagination
   * @param tenantId The tenant ID
   * @param searchType The type of search (ITEM_NAME, FORGE_TRACEABILITY_NUMBER, or FORGING_LINE_NAME)
   * @param searchTerm The search term (substring matching for all search types)
   * @param page The page number (0-based)
   * @param size The page size
   * @return Page of ForgeRepresentation containing the search results
   */
  @Transactional(readOnly = true)
  public Page<ForgeRepresentation> searchForges(Long tenantId, String searchType, String searchTerm, int page, int size) {
    if (searchTerm == null || searchTerm.trim().isEmpty() || searchType == null || searchType.trim().isEmpty()) {
      Pageable pageable = PageRequest.of(page, size);
      return Page.empty(pageable);
    }

    Pageable pageable = PageRequest.of(page, size);
    Page<Forge> forgePage;

    switch (searchType.toUpperCase()) {
      case "ITEM_NAME":
        forgePage = forgeRepository.findForgesByItemNameContainingIgnoreCase(tenantId, searchTerm.trim(), pageable);
        break;
      case "FORGE_TRACEABILITY_NUMBER":
        forgePage = forgeRepository.findForgesByForgeTraceabilityNumberContainingIgnoreCase(tenantId, searchTerm.trim(), pageable);
        break;
      case "FORGING_LINE_NAME":
        forgePage = forgeRepository.findForgesByForgingLineNameContainingIgnoreCase(tenantId, searchTerm.trim(), pageable);
        break;
      default:
        log.error("Invalid search type: {}", searchType);
        throw new IllegalArgumentException("Invalid search type: " + searchType + ". Valid types are: ITEM_NAME, FORGE_TRACEABILITY_NUMBER, FORGING_LINE_NAME");
    }

    return forgePage.map(forgeAssembler::dissemble);
  }

  /**
   * Creates a new forge shift for an existing forge in IN_PROGRESS status
   * @param tenantId The tenant ID
   * @param forgingLineId The forging line ID
   * @param forgeId The forge ID
   * @param forgeShiftRepresentation The forge shift data
   * @return The created forge shift representation
   */
  @Transactional
  public ForgeShiftRepresentation createForgeShift(long tenantId, long forgingLineId, long forgeId, 
                                                   ForgeShiftRepresentation forgeShiftRepresentation) {
    // 1. Validate tenant exists
    tenantService.validateTenantExists(tenantId);
    
    // 2. Validate forging line exists for tenant
    ForgingLine forgingLine = getForgingLineUsingTenantIdAndForgingLineId(tenantId, forgingLineId);
    
    // 3. Validate forge exists and belongs to the forging line
    Forge forge = getForgeByIdAndForgingLineId(forgeId, forgingLine.getId());
    
    // 4. Validate forge is in IN_PROGRESS status
    if (Forge.ForgeStatus.IN_PROGRESS != forge.getForgingStatus()) {
      log.error("Forge={} is not in IN_PROGRESS status to add forge shift. Current status: {}", 
               forgeId, forge.getForgingStatus());
      throw new ForgeNotInExpectedStatusException("Forge must be in IN_PROGRESS status to add forge shift");
    }
    
    // 5. Parse and validate start and end times
    LocalDateTime startDateTime = ConvertorUtils.convertStringToLocalDateTime(forgeShiftRepresentation.getStartDateTime());
    LocalDateTime endDateTime = ConvertorUtils.convertStringToLocalDateTime(forgeShiftRepresentation.getEndDateTime());
    
    if (startDateTime.compareTo(endDateTime) >= 0) {
      log.error("Forge shift start time={} must be before end time={}", startDateTime, endDateTime);
      throw new IllegalArgumentException("Forge shift start time must be before end time");
    }
    
    // 6. Validate sequential shift timing
    validateForgeShiftSequencing(forge, startDateTime);
    
    // 7. Parse actual forged pieces count
    int actualForgedPiecesCount = Integer.parseInt(forgeShiftRepresentation.getActualForgedPiecesCount());
    
    // 8. Parse rejection data if applicable
    int rejectedPiecesCount = 0;
    double otherRejectionsKg = 0.0;
    boolean hasRejections = forgeShiftRepresentation.getRejection() != null && forgeShiftRepresentation.getRejection();
    
    if (hasRejections) {
      rejectedPiecesCount = Integer.parseInt(forgeShiftRepresentation.getRejectedForgePiecesCount());
      if (forgeShiftRepresentation.getOtherForgeRejectionsKg() != null && 
          !forgeShiftRepresentation.getOtherForgeRejectionsKg().isEmpty()) {
        otherRejectionsKg = roundToGramLevel(Double.parseDouble(forgeShiftRepresentation.getOtherForgeRejectionsKg()));
      }
    }
    
    // 9. Get item weight for calculations
    ItemWeightType weightType = forge.getItemWeightType();
    Double itemWeight = determineItemWeight(forge.getProcessedItem().getItem(), weightType);
    
    // 10. Validate forge shift heat data and totals
    validateForgeShiftHeatsAndTotals(forgeShiftRepresentation, actualForgedPiecesCount, 
                                     rejectedPiecesCount, otherRejectionsKg, itemWeight, hasRejections);
    
    // 11. Process heat allocations and create forge shift
    ForgeShift forgeShift = processForgeShiftWithProperHeatAllocation(forge, forgeShiftRepresentation, 
                                                                     startDateTime, endDateTime, 
                                                                     actualForgedPiecesCount, rejectedPiecesCount, 
                                                                     otherRejectionsKg, hasRejections, itemWeight);
    
    // 12. Save forge shift
    ForgeShift savedForgeShift = forgeShiftRepository.save(forgeShift);
    
    // 13. Return representation (workflow update is handled in updateProcessedItemFromForgeShifts)
    return forgeShiftAssembler.dissemble(savedForgeShift);
  }

  private void validateForgeShiftSequencing(Forge forge, LocalDateTime startDateTime) {
    // First forge shift must start after forge start time
    if (forge.getForgeShifts() == null || forge.getForgeShifts().isEmpty()) {
      if (forge.getStartAt() == null) {
        log.error("Cannot create forge shift before forge has been started");
        throw new IllegalStateException("Cannot create forge shift before forge has been started");
      }
      if (startDateTime.compareTo(forge.getStartAt()) <= 0) {
        log.error("First forge shift start time={} must be after forge start time={}", 
                 startDateTime, forge.getStartAt());
        throw new IllegalArgumentException("First forge shift must start after forge start time");
      }
    } else {
      // Subsequent shifts must start after the previous shift end time
      ForgeShift latestShift = forge.getLatestForgeShift();
      if (latestShift != null && startDateTime.compareTo(latestShift.getEndDateTime()) <= 0) {
        log.error("Forge shift start time={} must be after previous shift end time={}", 
                 startDateTime, latestShift.getEndDateTime());
        throw new IllegalArgumentException("Forge shift must start after previous shift end time");
      }
    }
  }

  /**
   * Validates forge shift heats and totals according to the new requirements
   */
  private void validateForgeShiftHeatsAndTotals(ForgeShiftRepresentation representation,
                                                int actualForgedPiecesCount, int rejectedPiecesCount, 
                                                double otherRejectionsKg, Double itemWeight, 
                                                boolean hasRejections) {
    if (representation.getForgeShiftHeats() == null || representation.getForgeShiftHeats().isEmpty()) {
      log.error("Forge shift must have at least one heat material");
      throw new IllegalArgumentException("Forge shift must have at least one heat material");
    }
    
    // Extract heatPieces from heatQuantityUsed for validation
    int totalHeatPieces = 0;
    int totalRejectedPieces = 0;
    double totalOtherRejections = 0.0;
    
    for (ForgeShiftHeatRepresentation heatRep : representation.getForgeShiftHeats()) {
      // Validate heat selection
      if (heatRep.getHeatId() == null && (heatRep.getHeat() == null || heatRep.getHeat().getId() == null)) {
        log.error("Heat selection is required for each forge shift heat");
        throw new IllegalArgumentException("Heat selection is required for each forge shift heat");
      }
      
      // Validate heat quantity used is provided
      if (heatRep.getHeatQuantityUsed() == null || heatRep.getHeatQuantityUsed().isEmpty()) {
        log.error("Heat quantity used is required for each forge shift heat");
        throw new IllegalArgumentException("Heat quantity used is required for each forge shift heat");
      }
      
      // Calculate pieces from the heat quantity and item weight
      // Round all quantities to gram level to eliminate floating-point precision errors
      double heatQuantityUsed = roundToGramLevel(Double.parseDouble(heatRep.getHeatQuantityUsed()));
      double rejectedPiecesQuantity = 0.0;
      double otherRejectionsQuantity = 0.0;
      int rejectedPiecesFromHeat = 0;
      
      if (hasRejections) {
        // Parse rejection data for this heat and round to gram level
        if (heatRep.getHeatQuantityUsedInRejectedPieces() != null && 
            !heatRep.getHeatQuantityUsedInRejectedPieces().isEmpty()) {
          rejectedPiecesQuantity = roundToGramLevel(Double.parseDouble(heatRep.getHeatQuantityUsedInRejectedPieces()));
        }
        
        if (heatRep.getHeatQuantityUsedInOtherRejections() != null && 
            !heatRep.getHeatQuantityUsedInOtherRejections().isEmpty()) {
          otherRejectionsQuantity = roundToGramLevel(Double.parseDouble(heatRep.getHeatQuantityUsedInOtherRejections()));
        }
        
        if (heatRep.getRejectedPieces() != null && !heatRep.getRejectedPieces().isEmpty()) {
          rejectedPiecesFromHeat = Integer.parseInt(heatRep.getRejectedPieces());
        }
        
        // Validate rejected pieces quantity matches pieces count * item weight
        double expectedRejectedPiecesQuantity = rejectedPiecesFromHeat * itemWeight;
        if (Math.abs(rejectedPiecesQuantity - expectedRejectedPiecesQuantity) > 0.0001) {
          log.error("Heat rejected pieces quantity ({}) does not match pieces count ({}) * item weight ({})",
                   rejectedPiecesQuantity, rejectedPiecesFromHeat, itemWeight);
          throw new IllegalArgumentException(
              String.format("Heat rejected pieces quantity (%.2f) must equal pieces count (%d) * item weight (%.2f)",
                          rejectedPiecesQuantity, rejectedPiecesFromHeat, itemWeight)
          );
        }
        
        totalRejectedPieces += rejectedPiecesFromHeat;
        totalOtherRejections += otherRejectionsQuantity;
      }
      
      // Calculate heat pieces from total heat quantity
      double expectedHeatQuantity = rejectedPiecesQuantity + otherRejectionsQuantity;
      double heatPiecesQuantity = roundToGramLevel(heatQuantityUsed - expectedHeatQuantity);
      
      // Validate heat pieces calculation (precision errors eliminated by rounding to gram level)
      if (heatPiecesQuantity < 0 || Math.abs(heatPiecesQuantity % itemWeight) > 0.0001) {
        log.error("Invalid heat quantity calculation for heat. Total quantity: {}, rejections: {}, other: {}, heatPiecesQuantity: {}, itemWeight: {}, remainder: {}",
                 heatQuantityUsed, rejectedPiecesQuantity, otherRejectionsQuantity, heatPiecesQuantity, itemWeight, heatPiecesQuantity % itemWeight);
        throw new IllegalArgumentException("Heat quantity calculation is invalid - check pieces and rejection quantities");
      }
      
      int heatPieces = (int) Math.round(heatPiecesQuantity / itemWeight);
      totalHeatPieces += heatPieces;
      
        // Set the calculated heatPieces in the representation for later use
      heatRep.setHeatPieces(String.valueOf(heatPieces));
      
      // Validate the total heat quantity matches the formula (round to eliminate precision errors)
      double calculatedTotalQuantity = roundToGramLevel((heatPieces + rejectedPiecesFromHeat) * itemWeight + otherRejectionsQuantity);
      if (Math.abs(calculatedTotalQuantity - heatQuantityUsed) > 0.0001) {
        log.error("Heat quantity used ({}) does not match calculated quantity ({}) for heat pieces: {}, rejected: {}, other: {}",
                 heatQuantityUsed, calculatedTotalQuantity, heatPieces, rejectedPiecesFromHeat, otherRejectionsQuantity);
        throw new IllegalArgumentException(
            String.format("Heat quantity used (%.2f) must match calculated quantity (%.2f) based on pieces",
                        heatQuantityUsed, calculatedTotalQuantity)
        );
      }
    }
    
    // Validate totals match expected values
    if (totalHeatPieces != actualForgedPiecesCount) {
      log.error("Sum of heat pieces ({}) does not match actual forged pieces count ({})",
               totalHeatPieces, actualForgedPiecesCount);
      throw new IllegalArgumentException(
          String.format("Sum of heat pieces (%d) must equal actual forged pieces count (%d)",
                      totalHeatPieces, actualForgedPiecesCount)
      );
    }
    
    if (hasRejections) {
      if (totalRejectedPieces != rejectedPiecesCount) {
        log.error("Sum of heat rejected pieces ({}) does not match total rejected pieces count ({})",
                 totalRejectedPieces, rejectedPiecesCount);
        throw new IllegalArgumentException(
            String.format("Sum of heat rejected pieces (%d) must equal total rejected pieces count (%d)",
                        totalRejectedPieces, rejectedPiecesCount)
        );
      }
      
      if (Math.abs(totalOtherRejections - otherRejectionsKg) > 0.0001) {
        log.error("Sum of heat other rejections ({}) does not match total other rejections ({})",
                 totalOtherRejections, otherRejectionsKg);
        throw new IllegalArgumentException(
            String.format("Sum of heat other rejections (%.2f) must equal total other rejections (%.2f)",
                        totalOtherRejections, otherRejectionsKg)
        );
      }
    }
  }

  /**
   * Processes forge shift with proper heat allocation logic based on original applyForge quantities
   */
  private ForgeShift processForgeShiftWithProperHeatAllocation(Forge forge, ForgeShiftRepresentation representation,
                                                               LocalDateTime startDateTime, LocalDateTime endDateTime,
                                                               int actualForgedPiecesCount, int rejectedPiecesCount,
                                                               double otherRejectionsKg, boolean hasRejections,
                                                               Double itemWeight) {
    // Create a map of original forge heat allocations (heatId -> quantity allocated during applyForge)
    Map<Long, Double> originalHeatAllocations = forge.getForgeHeats().stream()
        .collect(Collectors.toMap(
            fh -> fh.getHeat().getId(),
            ForgeHeat::getHeatQuantityUsed
        ));
    
    log.info("Original heat allocations from applyForge: {}", originalHeatAllocations);
    
    // Calculate total usage of each heat across all existing forge shifts
    Map<Long, Double> totalExistingUsageByHeat = new HashMap<>();
    
    if (forge.getForgeShifts() != null && !forge.getForgeShifts().isEmpty()) {
      for (ForgeShift existingShift : forge.getForgeShifts()) {
        if (!existingShift.isDeleted()) {
          for (ForgeShiftHeat existingShiftHeat : existingShift.getForgeShiftHeats()) {
            if (!existingShiftHeat.isDeleted()) {
              Long heatId = existingShiftHeat.getHeat().getId();
              double heatUsage = existingShiftHeat.getHeatQuantityUsed();
              totalExistingUsageByHeat.merge(heatId, heatUsage, Double::sum);
            }
          }
        }
      }
    }
    
    log.info("Total existing usage by heat across all previous forge shifts: {}", totalExistingUsageByHeat);
    
    // Process each forge shift heat
    List<ForgeShiftHeat> forgeShiftHeats = new ArrayList<>();
    
    for (ForgeShiftHeatRepresentation heatRep : representation.getForgeShiftHeats()) {
      Long heatId = heatRep.getHeatId() != null ? heatRep.getHeatId() : heatRep.getHeat().getId();
      double currentShiftUsage = roundToGramLevel(Double.parseDouble(heatRep.getHeatQuantityUsed()));
      
      // Get the heat entity
      Heat heat = rawMaterialHeatService.getRawMaterialHeatById(heatId);
      
      // Calculate total usage including current shift
      double existingUsage = totalExistingUsageByHeat.getOrDefault(heatId, 0.0);
      double totalUsageWithCurrentShift = existingUsage + currentShiftUsage;
      
      // Check if this heat was part of the original applyForge allocation
      Double originalAllocation = originalHeatAllocations.get(heatId);
      
      if (originalAllocation != null) {
        // This heat was part of original allocation
        log.info("Heat ID={}: Current shift usage={}, Existing usage={}, Total usage={}, Original allocation={}", 
                heatId, currentShiftUsage, existingUsage, totalUsageWithCurrentShift, originalAllocation);
        
        if (totalUsageWithCurrentShift > originalAllocation) {
          // Total usage exceeds original allocation, need to deduct extra from inventory
          double totalExcess = totalUsageWithCurrentShift - originalAllocation;
          
          // However, we only need to deduct what hasn't been deducted yet in previous shifts
          double previousExcess = Math.max(0, existingUsage - originalAllocation);
          double additionalDeduction = totalExcess - previousExcess;
          
          if (additionalDeduction > 0) {
            // Validate heat has sufficient quantity for additional deduction
            if (heat.getAvailableHeatQuantity() < additionalDeduction) {
              log.error("Insufficient heat quantity for additional deduction. Heat ID={}, required additional={}, available={}",
                       heatId, additionalDeduction, heat.getAvailableHeatQuantity());
              throw new IllegalArgumentException(
                  String.format("Insufficient heat quantity for additional deduction. Heat %d requires %.2f kg additional but only %.2f kg available",
                              heatId, additionalDeduction, heat.getAvailableHeatQuantity())
              );
            }
            
            // Deduct additional usage from heat inventory
            double newAvailableQuantity = heat.getAvailableHeatQuantity() - additionalDeduction;
            heat.setAvailableHeatQuantity(newAvailableQuantity);
            rawMaterialHeatService.updateRawMaterialHeat(heat);
            
            log.info("Deducted additional usage from heat ID={}: total excess={}, previous excess={}, additional deduction={}, new available={}",
                    heatId, totalExcess, previousExcess, additionalDeduction, newAvailableQuantity);
          } else {
            log.info("Heat ID={}: No additional deduction needed, excess was already deducted in previous shifts", heatId);
          }
        } else {
          // Total usage is within original allocation, no deduction needed
          log.info("Heat ID={}: Total usage within original allocation, no additional deduction needed", heatId);
        }
      } else {
        // This is a new heat not part of original allocation, deduct full current shift usage
        log.info("Heat ID={}: New heat not in original allocation, deducting full current shift usage={}", heatId, currentShiftUsage);
        
        // Validate heat has sufficient quantity
        if (heat.getAvailableHeatQuantity() < currentShiftUsage) {
          log.error("Insufficient heat quantity for new heat. Heat ID={}, required={}, available={}",
                   heatId, currentShiftUsage, heat.getAvailableHeatQuantity());
          throw new IllegalArgumentException(
              String.format("Insufficient heat quantity for new heat. Heat %d requires %.2f kg but only %.2f kg available",
                          heatId, currentShiftUsage, heat.getAvailableHeatQuantity())
          );
        }
        
        // Deduct full current shift usage from heat inventory
        double newAvailableQuantity = heat.getAvailableHeatQuantity() - currentShiftUsage;
        heat.setAvailableHeatQuantity(newAvailableQuantity);
        rawMaterialHeatService.updateRawMaterialHeat(heat);
        
        log.info("Deducted full current shift usage from new heat ID={}: usage={}, new available={}",
                heatId, currentShiftUsage, newAvailableQuantity);
      }
      
      // Create forge shift heat entity
      ForgeShiftHeat forgeShiftHeat = forgeShiftHeatAssembler.createAssemble(heatRep);
      forgeShiftHeats.add(forgeShiftHeat);
    }
    
    // Create forge shift
    ForgeShift forgeShift = ForgeShift.builder()
        .forge(forge)
        .startDateTime(startDateTime)
        .endDateTime(endDateTime)
        .forgeShiftHeats(forgeShiftHeats)
        .actualForgedPiecesCount(actualForgedPiecesCount)
        .rejectedForgePiecesCount(rejectedPiecesCount)
        .otherForgeRejectionsKg(otherRejectionsKg)
        .rejection(hasRejections)
        .createdAt(LocalDateTime.now())
        .build();
    
    // Set forge shift reference in forge shift heats
    forgeShiftHeats.forEach(heat -> heat.setForgeShift(forgeShift));
    
    // Add forge shift to forge
    forge.addForgeShift(forgeShift);
    
    // Update processedItem fields based on total actual forged pieces across all forge shifts
    updateProcessedItemFromForgeShifts(forge, forgeShift);
    
    return forgeShift;
  }

  /**
   * Rounds a double value to gram level precision (3 decimal places)
   * This eliminates floating-point precision errors from JSON parsing and calculations
   * @param value The value to round
   * @return The rounded value to 3 decimal places
   */
  private double roundToGramLevel(double value) {
    return Math.round(value * 1000.0) / 1000.0;
  }

  /**
   * Updates the processedItem fields based on the sum of all forge shifts' actual forged pieces count
   * Also updates the workflow with operation outcome data for workflow flow control
   */
  private void updateProcessedItemFromForgeShifts(Forge forge, ForgeShift currentForgeShift) {
    // Calculate total actual forged pieces across all forge shifts
    int totalActualForgedPieces = forge.getForgeShifts().stream()
        .filter(shift -> !shift.isDeleted())
        .mapToInt(ForgeShift::getActualForgedPiecesCount)
        .sum();
    
    log.info("Updating processedItem for forge ID={}: total actual forged pieces across all shifts = {}", 
             forge.getId(), totalActualForgedPieces);
    
    // Update processedItem fields
    ProcessedItem processedItem = forge.getProcessedItem();
    processedItem.setActualForgePiecesCount(totalActualForgedPieces);
    
    // Get current available pieces from workflow data using utility method
    Long itemWorkflowId = forge.getProcessedItem().getItemWorkflowId();
    int currentAvailablePieces = itemWorkflowService.getPiecesAvailableForNextFromOperation(
        itemWorkflowId, WorkflowStep.OperationType.FORGING);
    
    int incrementedAvailablePieces = currentAvailablePieces + currentForgeShift.getActualForgedPiecesCount();

    log.info("Forge ID={}: Incrementing available pieces from {} by {} to {}",
             forge.getId(), currentAvailablePieces, currentForgeShift.getActualForgedPiecesCount(), incrementedAvailablePieces);
    updateWorkflowForForgeShift(forge, currentForgeShift, totalActualForgedPieces, incrementedAvailablePieces);
    
  }

  /**
   * Consolidated workflow update method that handles both incremental pieces and complete outcome data
   * This replaces the separate updateWorkflowAfterForgeShift and updateWorkflowWithCompleteOutcomeData calls
   */
  private void updateWorkflowForForgeShift(Forge forge, ForgeShift currentForgeShift, 
                                          int totalActualForgedPieces, int incrementedAvailablePieces) {
    try {
      // Validate workflow and get ItemWorkflow
      ItemWorkflow workflow = validateAndGetWorkflow(forge);
      
      // Get existing forging outcome data
      ItemWorkflowStep forgingItemWorkflowStep = itemWorkflowService.getWorkflowStepByOperation(workflow.getId(), WorkflowStep.OperationType.FORGING);
      ItemWorkflowStepRepresentation forgingItemWorkflowStepRepresentation = itemWorkflowStepAssembler.dissemble(forgingItemWorkflowStep);
      OperationOutcomeData.ForgingOutcome forgingOutcome = forgingItemWorkflowStepRepresentation.getOperationOutcomeData().getForgingData();
      
      // Update forging outcome with forge shift data
      forgingOutcome.setInitialPiecesCount(totalActualForgedPieces);
      forgingOutcome.setPiecesAvailableForNext(incrementedAvailablePieces);
      forgingOutcome.setUpdatedAt(LocalDateTime.now());

      // Update workflow with complete outcome data
      updateWorkflowStepWithOutcomeData(workflow.getId(), forgingOutcome);
      
      log.info("Successfully updated workflow {} with forge shift data: {} additional pieces, {} total pieces, {} available pieces", 
               workflow.getId(), currentForgeShift.getActualForgedPiecesCount(), totalActualForgedPieces, incrementedAvailablePieces);
               
    } catch (Exception e) {
      // Re-throw the exception to fail the forge shift creation since workflow integration is now mandatory
      log.error("Failed to update workflow for forge shift on forge ID={}: {}. Failing forge shift creation.", 
               forge.getId(), e.getMessage());
      throw new RuntimeException("Failed to update workflow for forge shift: " + e.getMessage(), e);
    }
  }

  /**
   * Updates workflow when forge is started
   */
  private void updateWorkflowForForgeStart(Forge forge, LocalDateTime startedAt) {
    try {
      // Validate workflow and get ItemWorkflow
      ItemWorkflow workflow = validateAndGetWorkflow(forge);
      
      ProcessedItem processedItem = forge.getProcessedItem();

      // Create initial ForgingOutcome object for forge start
      OperationOutcomeData.ForgingOutcome forgingOutcome = OperationOutcomeData.ForgingOutcome.builder()
          .id(processedItem.getId())
          .initialPiecesCount(0)
          .piecesAvailableForNext(0)
          .startedAt(startedAt)
          .createdAt(processedItem.getCreatedAt())
          .updatedAt(LocalDateTime.now())
          .deletedAt(processedItem.getDeletedAt())
          .deleted(processedItem.isDeleted())
          .build();

      // Update workflow with initial outcome data
      updateWorkflowStepWithOutcomeData(workflow.getId(), forgingOutcome);

      log.info("Successfully updated workflow {} with forge start data, startedAt: {}", workflow.getId(), startedAt);

    } catch (Exception e) {
      // Re-throw the exception to fail the forge start since workflow integration is now mandatory
      log.error("Failed to update workflow for forge start on forge ID={}: {}. Failing forge start.",
                forge.getId(), e.getMessage());
      throw new RuntimeException("Failed to update workflow for forge start: " + e.getMessage(), e);
    }
  }

  /**
   * Validates workflow requirements and returns the ItemWorkflow
   * Extracted common validation logic used by both forge start and forge shift updates
   */
  private ItemWorkflow validateAndGetWorkflow(Forge forge) {
    // Ensure ProcessedItem is loaded to avoid lazy loading issues
    ProcessedItem processedItem = forge.getProcessedItem();
    
    // Handle potential lazy loading issues by explicitly fetching ProcessedItem if needed
    Long itemWorkflowId = processedItem.getItemWorkflowId();

    if (itemWorkflowId == null) {
      log.error("itemWorkflowId is mandatory for forge operations as it's required for workflow integration");
      throw new IllegalArgumentException("itemWorkflowId is mandatory for forge operations. " +
          "This is required to update the workflow step so that next operations can consume the produced pieces.");
    }

    // Get the specific workflow by ID and validate it
    ItemWorkflow workflow = itemWorkflowService.getItemWorkflowById(itemWorkflowId);
    Long itemId = workflow.getItem().getId();

    // Validate that the workflow belongs to the same item as the forge
    // Access the item through processedItem to ensure it's loaded
    Long processedItemItemId = processedItem.getItem().getId();

    if (!itemId.equals(processedItemItemId)) {
      log.error("ItemWorkflow {} does not belong to the same item as forge {}", itemWorkflowId, forge.getId());
      throw new IllegalArgumentException("ItemWorkflow does not belong to the same item as the forge");
    }
    
    return workflow;
  }

  /**
   * Updates the workflow step with the provided forging outcome data
   * Extracted common update logic used by both forge start and forge shift updates
   */
  private void updateWorkflowStepWithOutcomeData(Long itemWorkflowId, OperationOutcomeData.ForgingOutcome forgingOutcome) {
    itemWorkflowService.updateWorkflowStepForOperation(
        itemWorkflowId,
        WorkflowStep.OperationType.FORGING,
        OperationOutcomeData.forForgingOperation(
            forgingOutcome,
            LocalDateTime.now()
        )
    );
  }

  public Forge getForgeByProcessedItemId(long processedItemId) {
    Optional<Forge> forgeOptional = forgeRepository.findByProcessedItemIdAndDeletedFalse(processedItemId);
    if (forgeOptional.isEmpty()) {
      log.error("Forge does not exist for processedItemId={}", processedItemId);
      throw new ForgeNotFoundException("Forge does not exist for processedItemId=" + processedItemId);
    }
    return forgeOptional.get();
  }

  /**
   * Retrieves forges by multiple processed item IDs and validates they belong to the tenant
   * @param processedItemIds List of processed item IDs
   * @param tenantId The tenant ID for validation
   * @return List of ForgeRepresentation (distinct forges)
   */
  public List<ForgeRepresentation> getForgesByProcessedItemIds(List<Long> processedItemIds, Long tenantId) {
    if (processedItemIds == null || processedItemIds.isEmpty()) {
      log.info("No processed item IDs provided, returning empty list");
      return Collections.emptyList();
    }

    log.info("Getting forges for {} processed item IDs for tenant {}", processedItemIds.size(), tenantId);
    
    List<Forge> forges = forgeRepository.findByProcessedItemIdInAndDeletedFalse(processedItemIds);
    
    // Use a Set to track processed forge IDs to avoid duplicates
    Set<Long> processedForgeIds = new HashSet<>();
    List<ForgeRepresentation> validForges = new ArrayList<>();
    List<Long> invalidProcessedItemIds = new ArrayList<>();
    
    for (Long processedItemId : processedItemIds) {
      Optional<Forge> forgeOpt = forges.stream()
          .filter(forge -> forge.getProcessedItem().getId().equals(processedItemId))
          .findFirst();
          
      if (forgeOpt.isPresent()) {
        Forge forge = forgeOpt.get();
        if (Long.valueOf(forge.getTenant().getId()).equals(tenantId)) {
          // Only add if we haven't already processed this forge
          if (!processedForgeIds.contains(forge.getId())) {
            validForges.add(forgeAssembler.dissemble(forge));
            processedForgeIds.add(forge.getId());
          }
        } else {
          log.warn("Forge for processedItemId={} does not belong to tenant={}", processedItemId, tenantId);
          invalidProcessedItemIds.add(processedItemId);
        }
      } else {
        log.warn("No forge found for processedItemId={}", processedItemId);
        invalidProcessedItemIds.add(processedItemId);
      }
    }
    
    if (!invalidProcessedItemIds.isEmpty()) {
      log.warn("The following processed item IDs did not have valid forges for tenant {}: {}", 
               tenantId, invalidProcessedItemIds);
    }
    
    log.info("Found {} distinct valid forges out of {} requested processed item IDs", validForges.size(), processedItemIds.size());
    return validForges;
  }
}

