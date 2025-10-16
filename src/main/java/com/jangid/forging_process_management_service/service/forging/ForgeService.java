package com.jangid.forging_process_management_service.service.forging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jangid.forging_process_management_service.assemblers.forging.ForgeAssembler;

import com.jangid.forging_process_management_service.entities.Tenant;
import com.jangid.forging_process_management_service.entities.forging.Forge;
import com.jangid.forging_process_management_service.entities.forging.ForgeHeat;
import com.jangid.forging_process_management_service.entities.forging.ForgingLine;
import com.jangid.forging_process_management_service.entities.forging.ItemWeightType;
import com.jangid.forging_process_management_service.entities.ProcessedItem;
import com.jangid.forging_process_management_service.entities.inventory.Heat;
import com.jangid.forging_process_management_service.entities.order.Order;
import com.jangid.forging_process_management_service.entities.product.Item;
import com.jangid.forging_process_management_service.entitiesRepresentation.dispatch.DispatchBatchRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.forging.ForgeRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.heating.HeatTreatmentBatchRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.machining.MachiningBatchRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.quality.InspectionBatchRepresentation;

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
  private DocumentService documentService;

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

  public Page<ForgeRepresentation> getAllForges(long tenantId, int page, int size) {
    Pageable pageable = PageRequest.of(page, size);
    Page<Forge> forgePage = forgeRepository.findByTenantIdAndDeletedIsFalseOrderByUpdatedAtDesc(tenantId, pageable);
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

  @Transactional(rollbackFor = Exception.class) // Ensures all database operations succeed or roll back
  public ForgeRepresentation applyForge(long tenantId, long forgingLineId, ForgeRepresentation representation) {
    log.info("Starting forge creation transaction for tenant: {}, forgingLine: {}, forge: {}", 
             tenantId, forgingLineId, representation.getProcessedItem().getWorkflowIdentifier());
    
    Forge createdForge = null;
    try {
      // 1. Validate inputs and preconditions
      validateApplyForgeInputs(tenantId, forgingLineId, representation);

      // 2. Process heat quantity updates - CRITICAL: Heat inventory modifications
      LocalDateTime applyAtLocalDateTime = processHeatQuantityUpdates(representation, tenantId);

      // 3. Create and persist forge with processed item - generates required ID for workflow integration
      createdForge = createAndPersistForge(representation, tenantId, forgingLineId, applyAtLocalDateTime);
      log.info("Successfully persisted forge with ID: {}", createdForge.getId());

      // 4. Update forging line status
      updateForgingLineStatus(forgingLineId, tenantId);

      // 5. Integrate with workflow system - if this fails, entire transaction will rollback
      integrateWithWorkflowSystem(createdForge, representation);

      log.info("Successfully completed forge creation transaction for ID: {}", createdForge.getId());
      // 6. Return the created forge representation
      return forgeAssembler.dissemble(createdForge);
      
    } catch (Exception e) {
      log.error("Forge creation transaction failed for tenant: {}, forgingLine: {}. " +
                "All changes will be rolled back. Error: {}", 
                tenantId, forgingLineId, e.getMessage());
      
      if (createdForge != null) {
        log.error("Forge with ID {} was persisted but workflow integration failed. " +
                  "Transaction rollback will restore database consistency including heat inventory.", createdForge.getId());
      }
      
      // Re-throw to ensure transaction rollback
      throw e;
    }
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
  }

  /**
   * Processes heat quantity updates and validations
   *
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
        .itemWorkflowId(representation.getProcessedItem().getItemWorkflowId() != null ? representation.getProcessedItem().getItemWorkflowId()
                                                                                      : itemWorkflowService.getItemWorkflowByWorkflowIdentifier(item.getId(), representation.getProcessedItem()
                                                                                          .getWorkflowIdentifier()).getId())
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
      ItemWorkflow workflow = itemWorkflowService.startItemWorkflowStepOperation(
          item,
          WorkflowStep.OperationType.FORGING,
          workflowIdentifier,
          itemWorkflowId,
          null
      );

      // Update the ProcessedItem with the workflow ID (store as Long)
      createdForge.getProcessedItem().setItemWorkflowId(workflow.getId());
      forgeRepository.save(createdForge);

      log.info("Successfully integrated forge creation with workflow system. " +
               "Forge ID: {}, Workflow ID: {}, WorkflowIdentifier: {}",
               createdForge.getId(), workflow.getId(), workflow.getWorkflowIdentifier());

      // Update relatedEntityIds for FORGING operation step with ProcessedItem.id
      ItemWorkflowStep forgingStep = itemWorkflowService.findForgingItemWorkflowStep(workflow);

      if (forgingStep != null) {
        // Update relatedEntityIds for the specific FORGING operation step with processedItem.id
        itemWorkflowService.updateRelatedEntityIdsForSpecificStep(forgingStep, createdForge.getProcessedItem().getId());
      } else {
        log.warn("Could not find target FORGING ItemWorkflowStep for processedItem={}", createdForge.getProcessedItem().getId());
      }
    } catch (Exception e) {
      log.error("Failed to update workflow for forging on item {}: {}",
                createdForge.getProcessedItem().getItem().getId(), e.getMessage());
      throw e;
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

  @Transactional(rollbackFor = Exception.class)
  public ForgeRepresentation startForge(long tenantId, long forgingLineId, long forgeId, String startAt) {
    log.info("Starting forge start transaction for tenant: {}, forgingLine: {}, forge: {}", 
             tenantId, forgingLineId, forgeId);
    
    Forge startedForge = null;
    try {
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

      // Save forge status changes - generates required updates for workflow integration
      startedForge = forgeRepository.save(existingForge);
      log.info("Successfully persisted started forge with ID: {}", startedForge.getId());

      // Update workflow - if this fails, entire transaction will rollback
      updateWorkflowForForgeStart(startedForge, startTimeLocalDateTime);

      // Update Order status to IN_PROGRESS (after workflow integration is complete and relatedEntityIds are persisted)
      Long itemWorkflowId = existingForge.getProcessedItem().getItemWorkflowId();
      if (itemWorkflowId != null) {
        try {
          itemWorkflowService.updateOrderStatusOnWorkflowStatusChange(
              itemWorkflowId,
              Order.OrderStatus.IN_PROGRESS);
          log.info("Successfully updated Order status for ItemWorkflow {} after forge integration", itemWorkflowId);
        } catch (Exception e) {
          log.error("Failed to update Order status for ItemWorkflow {}: {}", itemWorkflowId, e.getMessage());
          throw e;
        }
      }

      forgingLine.setForgingLineStatus(ForgingLine.ForgingLineStatus.FORGE_IN_PROGRESS);
      forgingLineService.saveForgingLine(forgingLine);

      log.info("Successfully completed forge start transaction for ID: {}", startedForge.getId());
      return forgeAssembler.dissemble(startedForge);
      
    } catch (Exception e) {
      log.error("Forge start transaction failed for tenant: {}, forge: {}. " +
                "All changes will be rolled back. Error: {}", 
                tenantId, forgeId, e.getMessage());
      
      if (startedForge != null) {
        log.error("Forge with ID {} was started but workflow integration failed. " +
                  "Transaction rollback will restore database consistency.", startedForge.getId());
      }
      
      // Re-throw to ensure transaction rollback
      throw e;
    }
  }

  @Transactional(rollbackFor = Exception.class)
  public ForgeRepresentation endForge(long tenantId, long forgingLineId, long forgeId, String endAt, Long itemWorkflowId) {
    log.info("Starting forge completion transaction for tenant: {}, forgingLine: {}, forge: {}, itemWorkflow: {}", 
             tenantId, forgingLineId, forgeId, itemWorkflowId);
    
    Forge endedForge = null;
    try {
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

      // 6. Process heat quantity adjustments based on forge shifts usage - CRITICAL: Heat inventory modifications
      processHeatQuantityAdjustmentsOnForgeCompletion(existingForge);

      // 7. Update processedItem fields based on forge shifts totals
      existingForge.setForgingStatus(Forge.ForgeStatus.COMPLETED);
      existingForge.setEndAt(endDateTime);

      // 8. Save the completed forge - generates required updates for workflow integration
      endedForge = forgeRepository.save(existingForge);
      log.info("Successfully persisted completed forge with ID: {}", endedForge.getId());

      // 9. Update forging line status
      forgingLine.setForgingLineStatus(ForgingLine.ForgingLineStatus.FORGE_NOT_APPLIED);
      forgingLineService.saveForgingLine(forgingLine);

      // 10. Update ItemWorkflowStep entities - if this fails, entire transaction will rollback
      updateItemWorkflowStepsForForgeCompletion(itemWorkflowId, endedForge, endDateTime);

      log.info("Successfully completed forge completion transaction for ID: {} at {}", forgeId, endDateTime);
      return forgeAssembler.dissemble(endedForge);
      
    } catch (Exception e) {
      log.error("Forge completion transaction failed for tenant: {}, forge: {}. " +
                "All changes will be rolled back. Error: {}", 
                tenantId, forgeId, e.getMessage());
      
      if (endedForge != null) {
        log.error("Forge with ID {} was completed but workflow integration failed. " +
                  "Transaction rollback will restore database consistency including heat adjustments.", endedForge.getId());
      }
      
      // Re-throw to ensure transaction rollback
      throw e;
    }
  }

  /**
   * Updates the ItemWorkflowStep entities associated with the provided itemWorkflowId
   * Sets the FORGING step status to COMPLETED and sets completedAt timestamp
   * Uses tree-based workflow update methods for proper transactional handling
   *
   * @param itemWorkflowId The workflow ID to update
   * @param completedForge The completed forge entity
   * @param completedAt    The completion timestamp
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

      // Find the FORGING ItemWorkflowStep that contains the forge's ProcessedItem
      ItemWorkflowStep forgingStep = itemWorkflowService.findForgingItemWorkflowStep(itemWorkflow);

      if (forgingStep == null) {
        log.warn("No FORGING step found containing ProcessedItem {} in workflow {}",
                 completedForge.getProcessedItem().getId(), itemWorkflowId);
        return;
      }

      log.info("Found FORGING ItemWorkflowStep {} containing ProcessedItem {} for completion update",
               forgingStep.getId(), completedForge.getProcessedItem().getId());

      // Update the operation outcome data using tree-based workflow methods
      try {
        String existingOutcomeDataJson = forgingStep.getOperationOutcomeData();
        if (existingOutcomeDataJson != null && !existingOutcomeDataJson.trim().isEmpty()) {
          OperationOutcomeData operationOutcomeData = objectMapper.readValue(existingOutcomeDataJson, OperationOutcomeData.class);
          operationOutcomeData.setOperationLastUpdatedAt(completedAt);
          OperationOutcomeData.ForgingOutcome forgingOutcome = operationOutcomeData.getForgingData();

          if (forgingOutcome != null) {
            forgingOutcome.setCompletedAt(completedAt);
            forgingOutcome.setUpdatedAt(completedAt);

            // Use the tree-based workflow update method
            itemWorkflowService.updateWorkflowStepForSpecificStep(forgingStep, operationOutcomeData);

            log.info("Successfully updated FORGING step completion time for forge {} in workflow {}",
                     completedForge.getId(), itemWorkflowId);
          } else {
            log.warn("No forging data found in operation outcome data for FORGING step in workflow {}", itemWorkflowId);
          }
        } else {
          log.warn("No operation outcome data found for FORGING step in workflow for itemWorkflowId: {}", itemWorkflowId);
        }
      } catch (Exception e) {
        log.error("Failed to update operation outcome data for FORGING step: {}", e.getMessage());
        throw new RuntimeException("Failed to update operation outcome data: " + e.getMessage(), e);
      }

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

  @Transactional(rollbackFor = Exception.class)
  public void deleteForge(Long tenantId, Long forgeId) throws DocumentDeletionException {
    log.info("Starting forge deletion transaction for tenant: {}, forge: {}", tenantId, forgeId);
    
    try {
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
        boolean canDeleteForge = itemWorkflowService.areAllNextOperationBatchesDeleted(itemWorkflowId, processedItem.getId(), WorkflowStep.OperationType.FORGING);

        if (!canDeleteForge) {
          log.error("Cannot delete forge id={} as the next operation has active (non-deleted) batches", forgeId);
          throw new IllegalStateException("This forging cannot be deleted as the next operation has active batch entries.");
        }

        log.info("Forge id={} is eligible for deletion - all next operation batches are deleted", forgeId);
      } else {
        // No workflow found - allowing deletion as this may be a legacy forge without workflow
        log.warn("No workflow found for forge id={}, allowing deletion (legacy forge without workflow)", forgeId);
        throw new RuntimeException("No workflow found for forge id: " + forgeId);
      }

      // 5. Update workflow step to mark forging operation as deleted and adjust piece counts - CRITICAL: Workflow operations
      Integer actualForgePiecesCount = processedItem.getActualForgePiecesCount();
      if (actualForgePiecesCount != null && actualForgePiecesCount > 0) {
        itemWorkflowService.updateCurrentOperationStepForReturnedPieces(
            itemWorkflowId,
            WorkflowStep.OperationType.FORGING,
            actualForgePiecesCount,
            processedItem.getId()
        );
        log.info("Successfully marked forge operation as deleted and updated workflow step for forge id={}, subtracted {} pieces",
                 forgeId, actualForgePiecesCount);
      } else {
        log.info("No actual forged pieces to subtract for deleted forge id={}", forgeId);
      }

      // 6. Delete all documents attached to this forge using bulk delete for efficiency
      try {
          // Use bulk delete method from DocumentService for better performance
          documentService.deleteDocumentsForEntity(tenantId, DocumentLink.EntityType.FORGE, forgeId);
          log.info("Successfully bulk deleted all documents attached to forge {} for tenant {}", forgeId, tenantId);
      } catch (DataAccessException e) {
          log.error("Database error while deleting documents attached to forge {}: {}", forgeId, e.getMessage(), e);
          throw new DocumentDeletionException("Database error occurred while deleting attached documents for forge " + forgeId, e);
      } catch (RuntimeException e) {
          // Handle document service specific runtime exceptions (storage, file system errors, etc.)
          log.error("Document service error while deleting documents attached to forge {}: {}", forgeId, e.getMessage(), e);
          throw new DocumentDeletionException("Document service error occurred while deleting attached documents for forge " + forgeId + ": " + e.getMessage(), e);
      } catch (Exception e) {
          // Handle any other unexpected exceptions
          log.error("Unexpected error while deleting documents attached to forge {}: {}", forgeId, e.getMessage(), e);
          throw new DocumentDeletionException("Unexpected error occurred while deleting attached documents for forge " + forgeId, e);
      }

      // 7. Revert all heat quantity operations and soft delete related entities - CRITICAL: Heat inventory operations
      LocalDateTime currentTime = LocalDateTime.now();
      revertHeatQuantitiesOnForgeDeletion(forge, currentTime);

      // 8. Soft delete ProcessedItem
      processedItem.setDeleted(true);
      processedItem.setDeletedAt(currentTime);

      // 9. Rename forge traceability number and soft delete Forge
      renameForgeTraceabilityNumberForDeletion(forge, currentTime);
      forge.setDeleted(true);
      forge.setDeletedAt(currentTime);

      // Save the updated forge which will cascade to processed item and forge heats
      forgeRepository.save(forge);
      log.info("Successfully persisted forge deletion with ID: {}", forgeId);

      log.info("Successfully completed forge deletion transaction for ID: {}", forgeId);
      
    } catch (Exception e) {
      log.error("Forge deletion transaction failed for tenant: {}, forge: {}. " +
                "All changes will be rolled back. Error: {}", 
                tenantId, forgeId, e.getMessage());
      
      log.error("Forge deletion failed - workflow updates, heat inventory reversals, and entity deletions will be rolled back.");
      
      // Re-throw to ensure transaction rollback
      throw e;
    }
  }

  /**
   * Reverts all heat quantity operations performed during the forge lifecycle
   * This includes both ForgeHeat (applyForge) and ForgeShiftHeat (createForgeShift) operations
   *
   * @param forge       The forge to revert heat quantities for
   * @param currentTime The current timestamp for soft deletion
   */
  private void revertHeatQuantitiesOnForgeDeletion(Forge forge, LocalDateTime currentTime) {
    log.info("Reverting heat quantities for forge deletion, forge ID={}", forge.getId());

    // Create a map of original forge heat allocations (heatId -> quantity allocated during applyForge)
    Map<Long, Double> originalHeatAllocations = forge.getForgeHeats().stream()
        .collect(Collectors.toMap(
            fh -> fh.getHeat().getId(),
            ForgeHeat::getHeatQuantityUsed
        ));

    log.info("Original heat allocations from applyForge: {}", originalHeatAllocations);

    // Calculate total actual usage of each heat across all forge shifts
    Map<Long, Double> totalActualUsageByHeat = new HashMap<>();
    Set<Long> allUsedHeatIds = new HashSet<>(originalHeatAllocations.keySet());

    // Process all forge shifts and their heat usage
    if (forge.getForgeShifts() != null && !forge.getForgeShifts().isEmpty()) {
      for (ForgeShift forgeShift : forge.getForgeShifts()) {
        if (!forgeShift.isDeleted()) {
          log.info("Processing forge shift ID={} for heat quantity reversal", forgeShift.getId());

          for (ForgeShiftHeat shiftHeat : forgeShift.getForgeShiftHeats()) {
            if (!shiftHeat.isDeleted()) {
              Long heatId = shiftHeat.getHeat().getId();
              double usage = shiftHeat.getHeatQuantityUsed();
              totalActualUsageByHeat.merge(heatId, usage, Double::sum);
              allUsedHeatIds.add(heatId);

              // Soft delete forge shift heat
              shiftHeat.setDeleted(true);
              shiftHeat.setDeletedAt(currentTime);

              log.debug("Soft deleted ForgeShiftHeat ID={}, usage={}", shiftHeat.getId(), usage);
            }
          }

          // Soft delete forge shift
          forgeShift.setDeleted(true);
          forgeShift.setDeletedAt(currentTime);

          log.info("Soft deleted ForgeShift ID={}", forgeShift.getId());
        }
      }
    }

    log.info("Total actual usage by heat across all forge shifts: {}", totalActualUsageByHeat);

    // Process heat quantity reversions for each heat that was used
    for (Long heatId : allUsedHeatIds) {
      Heat heat = rawMaterialHeatService.getRawMaterialHeatById(heatId);
      double originalAllocation = originalHeatAllocations.getOrDefault(heatId, 0.0);
      double totalActualUsage = totalActualUsageByHeat.getOrDefault(heatId, 0.0);

      double quantityToReturn = 0.0;

      if (originalAllocation > 0) {
        // This heat was part of original allocation during applyForge
        // Regardless of whether usage was within or exceeded allocation,
        // we always need to return the totalActualUsage to restore the original state
        quantityToReturn = totalActualUsage;

        if (totalActualUsage <= originalAllocation) {
          log.info("Heat ID={}: Usage within allocation - returning actual usage={}", heatId, totalActualUsage);
        } else {
          log.info("Heat ID={}: Usage exceeded allocation - returning actual usage={}", heatId, totalActualUsage);
        }
      } else {
        // This heat was NOT part of original allocation (new heat used in forge shifts)
        // createForgeShift deducted the full usage from inventory
        // We need to return the full actual usage amount
        quantityToReturn = totalActualUsage;
        log.info("Heat ID={}: New heat not in original allocation - returning full actual usage={}", heatId, totalActualUsage);
      }

      if (quantityToReturn > 0) {
        double newAvailableQuantity = heat.getAvailableHeatQuantity() + quantityToReturn;
        heat.setAvailableHeatQuantity(newAvailableQuantity);
        rawMaterialHeatService.updateRawMaterialHeat(heat);

        log.info("Returned heat quantity for deletion - Heat ID={}, returned={}, new available={}",
                 heatId, quantityToReturn, newAvailableQuantity);
      }
    }

    // Soft delete all ForgeHeat entities
    forge.getForgeHeats().forEach(forgeHeat -> {
      forgeHeat.setDeleted(true);
      forgeHeat.setDeletedAt(currentTime);
      log.debug("Soft deleted ForgeHeat ID={}", forgeHeat.getId());
    });

    log.info("Completed heat quantity reversal for forge deletion, forge ID={}", forge.getId());
  }

  /**
   * Renames the forge traceability number when deleting to avoid unique constraint issues
   * and allow the original number to be reused for sequential numbering.
   *
   * @param forge       The forge to rename
   * @param currentTime The current timestamp for the deletion
   */
  private void renameForgeTraceabilityNumberForDeletion(Forge forge, LocalDateTime currentTime) {
    if (forge.getForgeTraceabilityNumber() != null && !forge.getForgeTraceabilityNumber().isEmpty()) {
      // Create a unique suffix using timestamp to avoid conflicts
      String timestamp = currentTime.toString().replaceAll("[^0-9]", ""); // Remove non-numeric characters
      String deletedTraceabilityNumber = forge.getForgeTraceabilityNumber() + "-DELETED-" + timestamp;

      log.info("Renaming forge traceability number for deletion: {} -> {}",
               forge.getForgeTraceabilityNumber(), deletedTraceabilityNumber);

      forge.setForgeTraceabilityNumber(deletedTraceabilityNumber);
    }
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
   *
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
   *
   * @param item       The item from which to extract the weight
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
   *
   * @param tenantId   The tenant ID
   * @param searchType The type of search (ITEM_NAME, FORGE_TRACEABILITY_NUMBER, or FORGING_LINE_NAME)
   * @param searchTerm The search term (substring matching for all search types)
   * @param page       The page number (0-based)
   * @param size       The page size
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
   *
   * @param tenantId                 The tenant ID
   * @param forgingLineId            The forging line ID
   * @param forgeId                  The forge ID
   * @param forgeShiftRepresentation The forge shift data
   * @return The created forge shift representation
   */
  @Transactional(rollbackFor = Exception.class)
  public ForgeShiftRepresentation createForgeShift(long tenantId, long forgingLineId, long forgeId,
                                                   ForgeShiftRepresentation forgeShiftRepresentation) {
    log.info("Starting forge shift creation transaction for tenant: {}, forgingLine: {}, forge: {}", 
             tenantId, forgingLineId, forgeId);
    
    ForgeShift savedForgeShift = null;
    try {
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
          otherRejectionsKg = Double.parseDouble(forgeShiftRepresentation.getOtherForgeRejectionsKg());
        }
      }

      // 9. Get item weight for calculations
      ItemWeightType weightType = forge.getItemWeightType();
      Double itemWeight = determineItemWeight(forge.getProcessedItem().getItem(), weightType);

      // 10. Validate forge shift heat data and totals
      validateForgeShiftHeatsAndTotals(forgeShiftRepresentation, actualForgedPiecesCount,
                                       rejectedPiecesCount, otherRejectionsKg, itemWeight, hasRejections);

      // 11. Process heat allocations and create forge shift - CRITICAL: Heat inventory modifications
      ForgeShift forgeShift = processForgeShiftWithProperHeatAllocation(forge, forgeShiftRepresentation,
                                                                        startDateTime, endDateTime,
                                                                        actualForgedPiecesCount, rejectedPiecesCount,
                                                                        otherRejectionsKg, hasRejections, itemWeight);

      // 12. Save forge shift - if this fails, heat inventory changes will be rolled back
      savedForgeShift = forgeShiftRepository.save(forgeShift);
      log.info("Successfully persisted forge shift with ID: {}", savedForgeShift.getId());

      log.info("Successfully completed forge shift creation transaction for ID: {}", savedForgeShift.getId());
      // 13. Return representation (workflow update is handled in updateProcessedItemFromForgeShifts)
      return forgeShiftAssembler.dissemble(savedForgeShift);
      
    } catch (Exception e) {
      log.error("Forge shift creation transaction failed for tenant: {}, forge: {}. " +
                "All changes will be rolled back. Error: {}", 
                tenantId, forgeId, e.getMessage());
      
      if (savedForgeShift != null) {
        log.error("Forge shift with ID {} was persisted but operation failed. " +
                  "Transaction rollback will restore database consistency including heat inventory.", savedForgeShift.getId());
      }
      
      // Re-throw to ensure transaction rollback
      throw e;
    }
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
      double remainder = heatPiecesQuantity % itemWeight;
      double normalizedRemainder = Math.min(remainder, itemWeight - remainder); // Get distance to nearest multiple
      
      if (heatPiecesQuantity < 0 || normalizedRemainder > 0.999) { // Allow less than 1g tolerance for precision errors
        log.error("Invalid heat quantity calculation for heat. Total quantity: {}, rejections: {}, other: {}, heatPiecesQuantity: {}, itemWeight: {}, remainder: {}, normalizedRemainder: {}",
                 heatQuantityUsed, rejectedPiecesQuantity, otherRejectionsQuantity, heatPiecesQuantity, itemWeight, remainder, normalizedRemainder);
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
        itemWorkflowId, WorkflowStep.OperationType.FORGING, forge.getProcessedItem().getId());

    int incrementedAvailablePieces = currentAvailablePieces + currentForgeShift.getActualForgedPiecesCount();

    log.info("Forge ID={}: Incrementing available pieces from {} by {} to {}",
             forge.getId(), currentAvailablePieces, currentForgeShift.getActualForgedPiecesCount(), incrementedAvailablePieces);
    updateWorkflowForForgeShift(forge, currentForgeShift, totalActualForgedPieces, incrementedAvailablePieces);

  }

  /**
   * Consolidated workflow update method that handles both incremental pieces and complete outcome data
   * This replaces the separate updateWorkflowAfterForgeShift and updateWorkflowWithCompleteOutcomeData calls
   * Uses tree-based workflow methods to find the specific FORGING step for this forge
   * Updates both the operationOutcomeData JSON field AND the direct ItemWorkflowStep fields
   */
  private void updateWorkflowForForgeShift(Forge forge, ForgeShift currentForgeShift,
                                           int totalActualForgedPieces, int incrementedAvailablePieces) {
    try {
      // Validate workflow and get ItemWorkflow
      ItemWorkflow workflow = validateAndGetWorkflow(forge);

      log.info("Updating workflow {} for forge shift: forge ID={}, total pieces={}, available pieces={}",
               workflow.getId(), forge.getId(), totalActualForgedPieces, incrementedAvailablePieces);

      // Get existing forging outcome data from the workflow step
      ItemWorkflowStep forgingItemWorkflowStep = itemWorkflowService.findItemWorkflowStepByRelatedEntityId(
          workflow.getId(),
          forge.getProcessedItem().getId(),
          WorkflowStep.OperationType.FORGING
      );

      if (forgingItemWorkflowStep == null) {
        log.error("No FORGING step found containing ProcessedItem {} in workflow {}",
                  forge.getProcessedItem().getId(), workflow.getId());
        throw new RuntimeException("FORGING step not found for ProcessedItem " + forge.getProcessedItem().getId());
      }

      // Get existing forging outcome data or create new if none exists
      OperationOutcomeData.ForgingOutcome forgingOutcome;
      
      if (forgingItemWorkflowStep.getOperationOutcomeData() != null && 
          !forgingItemWorkflowStep.getOperationOutcomeData().trim().isEmpty()) {
        try {
          OperationOutcomeData existingOutcomeData = objectMapper.readValue(
              forgingItemWorkflowStep.getOperationOutcomeData(), OperationOutcomeData.class);
          forgingOutcome = existingOutcomeData.getForgingData();
        } catch (Exception e) {
          log.warn("Failed to parse existing forging outcome data, creating new: {}", e.getMessage());
          forgingOutcome = null;
        }
      } else {
        forgingOutcome = null;
      }

      if (forgingOutcome == null) {
        // Create new forging outcome data
        forgingOutcome = OperationOutcomeData.ForgingOutcome.builder()
            .initialPiecesCount(totalActualForgedPieces)
            .piecesAvailableForNext(incrementedAvailablePieces)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
        log.info("Created new forging outcome data for forge {}", forge.getId());
      } else {
        // Update existing forging outcome data
        forgingOutcome.setInitialPiecesCount(totalActualForgedPieces);
        forgingOutcome.setPiecesAvailableForNext(incrementedAvailablePieces);
        forgingOutcome.setUpdatedAt(LocalDateTime.now());
        log.info("Updated existing forging outcome data for forge {}", forge.getId());
      }

      // Use updateWorkflowStepForOperation like other services to update both JSON field AND direct entity fields
      itemWorkflowService.updateWorkflowStepForForgingOperation(
          workflow.getId(),
          forge.getProcessedItem().getId(),
          OperationOutcomeData.forForgingOperation(forgingOutcome, LocalDateTime.now())
      );

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
   * Uses tree-based workflow methods to find the specific FORGING step for this forge
   */
  private void updateWorkflowForForgeStart(Forge forge, LocalDateTime startedAt) {
    try {
      // Validate workflow and get ItemWorkflow
      ItemWorkflow workflow = validateAndGetWorkflow(forge);

      if (workflow.getStartedAt() == null) {
        itemWorkflowService.updateWorkflowStartedAtFromFirstOperation(workflow.getId());
      }

      ProcessedItem processedItem = forge.getProcessedItem();

      // Find the specific FORGING ItemWorkflowStep that contains this forge's ProcessedItem
      ItemWorkflowStep forgingItemWorkflowStep = itemWorkflowService.findForgingItemWorkflowStep(workflow);

      if (forgingItemWorkflowStep == null) {
        log.error("No FORGING step found containing ProcessedItem {} in workflow {}",
                  processedItem.getId(), workflow.getId());
        throw new RuntimeException("FORGING step not found for ProcessedItem " + processedItem.getId());
      }

      log.info("Found FORGING ItemWorkflowStep {} containing ProcessedItem {} for forge start update",
               forgingItemWorkflowStep.getId(), processedItem.getId());

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

      // Update workflow with initial outcome data using tree-based method
      itemWorkflowService.updateWorkflowStepForSpecificStep(
          forgingItemWorkflowStep,
          OperationOutcomeData.forForgingOperation(forgingOutcome, LocalDateTime.now())
      );

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
   *
   * @param processedItemIds List of processed item IDs
   * @param tenantId         The tenant ID for validation
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

