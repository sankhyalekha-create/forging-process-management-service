package com.jangid.forging_process_management_service.service.dispatch;

import com.jangid.forging_process_management_service.assemblers.dispatch.DispatchBatchAssembler;
import com.jangid.forging_process_management_service.dto.workflow.OperationOutcomeData;
import com.jangid.forging_process_management_service.entities.PackagingType;
import com.jangid.forging_process_management_service.entities.dispatch.DispatchBatch;
import com.jangid.forging_process_management_service.entities.dispatch.DispatchPackage;
import com.jangid.forging_process_management_service.entities.dispatch.DispatchProcessedItemConsumption;
import com.jangid.forging_process_management_service.entities.dispatch.DispatchProcessedItemInspection;
import com.jangid.forging_process_management_service.entities.dispatch.ProcessedItemDispatchBatch;
import com.jangid.forging_process_management_service.entities.inventory.Heat;
import com.jangid.forging_process_management_service.entities.order.Order;
import com.jangid.forging_process_management_service.entities.product.ItemStatus;
import com.jangid.forging_process_management_service.entities.product.Item;
import com.jangid.forging_process_management_service.entities.quality.ProcessedItemInspectionBatch;
import com.jangid.forging_process_management_service.entities.workflow.ItemWorkflow;
import com.jangid.forging_process_management_service.entities.workflow.ItemWorkflowStep;
import com.jangid.forging_process_management_service.entities.workflow.WorkflowStep;
import com.jangid.forging_process_management_service.entitiesRepresentation.dispatch.DispatchBatchListRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.dispatch.DispatchBatchRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.dispatch.DispatchPackageRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.dispatch.DispatchProcessedItemConsumptionRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.dispatch.DispatchStatisticsRepresentation;
import com.jangid.forging_process_management_service.exception.dispatch.DispatchBatchNotFoundException;
import com.jangid.forging_process_management_service.repositories.dispatch.DispatchBatchRepository;
import com.jangid.forging_process_management_service.repositories.dispatch.DispatchProcessedItemConsumptionRepository;

import com.jangid.forging_process_management_service.service.TenantService;
import com.jangid.forging_process_management_service.service.buyer.BuyerService;
import com.jangid.forging_process_management_service.service.inventory.RawMaterialHeatService;
import com.jangid.forging_process_management_service.service.workflow.ItemWorkflowService;
import com.jangid.forging_process_management_service.utils.ConvertorUtils;

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
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.Collections;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Service
public class DispatchBatchService {

  private final DispatchBatchRepository dispatchBatchRepository;
  private final DispatchProcessedItemConsumptionRepository dispatchProcessedItemConsumptionRepository;
  private final TenantService tenantService;
  private final BuyerService buyerService;
  private final ItemWorkflowService itemWorkflowService;

  private final DispatchBatchAssembler dispatchBatchAssembler;
  private final ObjectMapper objectMapper;
  private final RawMaterialHeatService rawMaterialHeatService;
  private final DocumentService documentService;

  @Autowired
  public DispatchBatchService(
      DispatchBatchRepository dispatchBatchRepository,
      DispatchProcessedItemConsumptionRepository dispatchProcessedItemConsumptionRepository,
      TenantService tenantService,
      BuyerService buyerService,
      ItemWorkflowService itemWorkflowService,

      DispatchBatchAssembler dispatchBatchAssembler,
      ObjectMapper objectMapper,
      RawMaterialHeatService rawMaterialHeatService,
      DocumentService documentService) {
    this.dispatchBatchRepository = dispatchBatchRepository;
    this.dispatchProcessedItemConsumptionRepository = dispatchProcessedItemConsumptionRepository;
    this.tenantService = tenantService;
    this.buyerService = buyerService;
    this.itemWorkflowService = itemWorkflowService;

    this.dispatchBatchAssembler = dispatchBatchAssembler;
    this.objectMapper = objectMapper;
    this.rawMaterialHeatService = rawMaterialHeatService;
    this.documentService = documentService;
  }

  @Transactional(rollbackFor = Exception.class)
  public DispatchBatchRepresentation createDispatchBatch(long tenantId, DispatchBatchRepresentation representation) {
    log.info("Starting dispatch batch creation transaction for tenant: {}, batch: {}", 
             tenantId, representation.getDispatchBatchNumber());
    
    DispatchBatch createdDispatchBatch = null;
    try {
      tenantService.validateTenantExists(tenantId);
      buyerService.validateBuyerExists(representation.getBuyerId(), tenantId);
      buyerService.validateBuyerEntityExists(representation.getBillingEntityId(), tenantId);
      buyerService.validateBuyerEntityExists(representation.getShippingEntityId(), tenantId);

      boolean exists = dispatchBatchRepository.existsByDispatchBatchNumberAndTenantIdAndDeletedFalse(representation.getDispatchBatchNumber(), tenantId);
      if (exists) {
        log.error("Dispatch batch number={} already exists for tenant={}", representation.getDispatchBatchNumber(), tenantId);
        throw new IllegalStateException("Dispatch batch number " + representation.getDispatchBatchNumber() + " already exists for tenant " + tenantId);
      }
      
      // Check if this batch number was previously used and deleted
      if (isDispatchBatchNumberPreviouslyUsed(representation.getDispatchBatchNumber(), tenantId)) {
        log.warn("Dispatch batch with batch number: {} was previously used and deleted for tenant: {}", 
                 representation.getDispatchBatchNumber(), tenantId);
      }

      DispatchBatch dispatchBatch = dispatchBatchAssembler.createAssemble(representation);

      dispatchBatch.setTenant(tenantService.getTenantById(tenantId));
      dispatchBatch.setBuyer(buyerService.getBuyerByIdAndTenantId(representation.getBuyerId(), tenantId));
      dispatchBatch.setBillingEntity(buyerService.getBuyerEntityById(representation.getBillingEntityId()));
      dispatchBatch.setShippingEntity(buyerService.getBuyerEntityById(representation.getShippingEntityId()));

      dispatchBatch.setDispatchBatchStatus(DispatchBatch.DispatchBatchStatus.DISPATCH_IN_PROGRESS);

      // Save entity - this generates the required ID for workflow integration
      createdDispatchBatch = dispatchBatchRepository.save(dispatchBatch);
      log.info("Successfully persisted dispatch batch with ID: {}", createdDispatchBatch.getId());
      
      // Handle workflow integration - if this fails, entire transaction will rollback
      handleWorkflowIntegration(representation, createdDispatchBatch.getProcessedItemDispatchBatch());
      
      // Update Order status to IN_PROGRESS (after workflow integration is complete and relatedEntityIds are persisted)
      Long itemWorkflowId = createdDispatchBatch.getProcessedItemDispatchBatch().getItemWorkflowId();
      if (itemWorkflowId != null) {
        try {
          itemWorkflowService.updateOrderStatusOnWorkflowStatusChange(
              itemWorkflowId,
              Order.OrderStatus.IN_PROGRESS);
          log.info("Successfully updated Order status for ItemWorkflow {} after dispatch integration", itemWorkflowId);
        } catch (Exception e) {
          log.error("Failed to update Order status for ItemWorkflow {}: {}", itemWorkflowId, e.getMessage());
          throw e;
        }
      }
      
      log.info("Successfully completed dispatch batch creation transaction for ID: {}", createdDispatchBatch.getId());
      return dispatchBatchAssembler.dissemble(createdDispatchBatch);
      
    } catch (Exception e) {
      log.error("Dispatch batch creation transaction failed for tenant: {}, batch: {}. " +
                "All changes will be rolled back. Error: {}", 
                tenantId, representation.getDispatchBatchNumber(), e.getMessage());
      
      if (createdDispatchBatch != null) {
        log.error("Dispatch batch with ID {} was persisted but workflow integration failed. " +
                  "Transaction rollback will restore database consistency.", createdDispatchBatch.getId());
      }
      
      // Re-throw to ensure transaction rollback
      throw e;
    }
  }

  /**
   * Handle workflow integration including pieces consumption and workflow step updates
   */
  private void handleWorkflowIntegration(DispatchBatchRepresentation representation, 
                                        ProcessedItemDispatchBatch processedItemDispatchBatch) {
    try {
      // Get workflow from the ProcessedItemDispatchBatch entity
      Long itemWorkflowId = processedItemDispatchBatch.getItemWorkflowId();
      ItemWorkflow workflow = itemWorkflowService.getItemWorkflowById(itemWorkflowId);
      
      // Update the dispatch batch with workflow ID for future reference
      log.info("Successfully integrated dispatch batch with workflow. Workflow ID: {}, Workflow Identifier: {}", 
               workflow.getId(), workflow.getWorkflowIdentifier());

      if (processedItemDispatchBatch.getItemWorkflowId() == null) {
        processedItemDispatchBatch.setItemWorkflowId(workflow.getId());
        dispatchBatchRepository.save(processedItemDispatchBatch.getDispatchBatch());
      }
      
      // Check if dispatch is the first operation in the workflow template
      boolean isFirstOperation = workflow.getWorkflowTemplate().isFirstOperationType(WorkflowStep.OperationType.DISPATCH);
      
      if (isFirstOperation) {
        // This is the first operation in workflow - consume inventory from Heat (if applicable)
        log.info("Dispatch is the first operation in workflow - consuming inventory from heat");
        handleHeatConsumptionForFirstOperation(representation, processedItemDispatchBatch, workflow);
        
        // For first operation, handle single workflow step
        handleSingleDispatchWorkflowStep(processedItemDispatchBatch, workflow);
      } else {
        // Check if this is a multi-parent dispatch
        if (Boolean.TRUE.equals(processedItemDispatchBatch.getIsMultiParentDispatch()) && 
            representation.getDispatchProcessedItemConsumptions() != null && 
            !representation.getDispatchProcessedItemConsumptions().isEmpty()) {
          // Handle multiple parent operations consumption
          log.info("Handling multi-parent dispatch with {} parent entities", 
                   representation.getDispatchProcessedItemConsumptions().size());
          handleMultipleParentOperationsConsumption(representation, processedItemDispatchBatch, workflow);
          
          // Handle multiple dispatch workflow steps for multi-parent dispatch
          handleMultipleDispatchWorkflowSteps(representation, processedItemDispatchBatch, workflow);
        } else {
          // This is not the first operation - consume pieces from single previous operation (legacy mode)
          log.info("Handling single parent dispatch (legacy mode)");
          handlePiecesConsumptionFromPreviousOperation(processedItemDispatchBatch, workflow);
          
          // For single parent dispatch, handle single workflow step
          handleSingleDispatchWorkflowStep(processedItemDispatchBatch, workflow);
        }
      }
      
    } catch (Exception e) {
      log.error("Failed to integrate dispatch batch with workflow for item {}: {}", 
                processedItemDispatchBatch.getItem().getId(), e.getMessage());
      // Re-throw to fail the operation since workflow integration is critical
      throw new RuntimeException("Failed to integrate with workflow system: " + e.getMessage(), e);
    }
  }

  /**
   * Handle single dispatch workflow step for legacy single-parent dispatch and first operations
   */
  private void handleSingleDispatchWorkflowStep(ProcessedItemDispatchBatch processedItemDispatchBatch, 
                                               ItemWorkflow workflow) {
    try {
      log.info("Handling single dispatch workflow step for dispatch batch {}", 
               processedItemDispatchBatch.getId());
      
      // Update workflow step with batch data
      updateWorkflowStepWithBatchOutcome(processedItemDispatchBatch, workflow.getId());

      // Find the specific DISPATCH ItemWorkflowStep that corresponds to the user's selection
      ItemWorkflowStep targetDispatchStep = itemWorkflowService.findItemWorkflowStepByParentEntityId(
          workflow.getId(),
          processedItemDispatchBatch.getPreviousOperationProcessedItemId(),
          WorkflowStep.OperationType.DISPATCH);

      if (targetDispatchStep != null) {
        // Update relatedEntityIds for the specific DISPATCH operation step with processedItemDispatchBatch.id
        itemWorkflowService.updateRelatedEntityIdsForSpecificStep(targetDispatchStep, processedItemDispatchBatch.getId());
        log.info("Successfully updated single dispatch workflow step {} with batch outcome for dispatch batch {}", 
                 targetDispatchStep.getId(), processedItemDispatchBatch.getId());
      } else {
        log.warn("Could not find target DISPATCH ItemWorkflowStep for dispatch batch {}", 
                 processedItemDispatchBatch.getId());
      }
      
    } catch (Exception e) {
      log.error("Failed to handle single dispatch workflow step for dispatch batch {}: {}", 
                processedItemDispatchBatch.getId(), e.getMessage());
      throw new RuntimeException("Failed to update single dispatch workflow step: " + e.getMessage(), e);
    }
  }

  /**
   * Handle multiple dispatch workflow steps for multi-parent dispatch
   */
  private void handleMultipleDispatchWorkflowSteps(DispatchBatchRepresentation representation,
                                                  ProcessedItemDispatchBatch processedItemDispatchBatch, 
                                                  ItemWorkflow workflow) {
    try {
      log.info("Handling multiple dispatch workflow steps for dispatch batch {} with {} parent entities", 
               processedItemDispatchBatch.getId(), 
               representation.getDispatchProcessedItemConsumptions().size());
      
      // Process each consumption record to update corresponding workflow steps
      for (DispatchProcessedItemConsumptionRepresentation consumption : representation.getDispatchProcessedItemConsumptions()) {
        try {
          // Start workflow step operation for this specific parent entity
          log.debug("Starting dispatch workflow step for parent entity {} of type {}", 
                   consumption.getPreviousOperationEntityId(), consumption.getPreviousOperationType());
          
          ItemWorkflow updatedWorkflow = startDispatchWorkflowStepForParentEntity(
              processedItemDispatchBatch.getItem(),
              workflow,
              consumption.getPreviousOperationEntityId()
          );
          
          // Update workflow step with batch outcome for this specific parent entity
          updateDispatchWorkflowStepOutcomeForParentEntity(
              processedItemDispatchBatch, 
              updatedWorkflow.getId(), 
              consumption
          );
          
          // Update related entity IDs for this specific dispatch step
          updateRelatedEntityIdsForParentEntityDispatchStep(
              updatedWorkflow.getId(),
              consumption.getPreviousOperationEntityId(),
              processedItemDispatchBatch.getId()
          );
          
          log.info("Successfully processed dispatch workflow step for parent entity {} of type {}", 
                   consumption.getPreviousOperationEntityId(), consumption.getPreviousOperationType());
          
        } catch (Exception e) {
          log.error("Failed to process dispatch workflow step for parent entity {} of type {}: {}", 
                   consumption.getPreviousOperationEntityId(), consumption.getPreviousOperationType(), e.getMessage());
          throw new RuntimeException("Failed to process dispatch workflow step for parent entity " + 
                                   consumption.getPreviousOperationEntityId() + ": " + e.getMessage(), e);
        }
      }
      
      log.info("Successfully handled all {} dispatch workflow steps for multi-parent dispatch batch {}", 
               representation.getDispatchProcessedItemConsumptions().size(), processedItemDispatchBatch.getId());
      
    } catch (Exception e) {
      log.error("Failed to handle multiple dispatch workflow steps for dispatch batch {}: {}", 
                processedItemDispatchBatch.getId(), e.getMessage());
      throw new RuntimeException("Failed to update multiple dispatch workflow steps: " + e.getMessage(), e);
    }
  }

  /**
   * Start dispatch workflow step operation for a specific parent entity
   */
  private ItemWorkflow startDispatchWorkflowStepForParentEntity(Item item, 
                                                               ItemWorkflow workflow, 
                                                               Long parentEntityId) {
    try {
      log.debug("Starting dispatch workflow step for item {} and parent entity {}", 
               item.getId(), parentEntityId);
      
      return itemWorkflowService.startItemWorkflowStepOperationForDispatch(
          workflow.getId(),
          parentEntityId
      );
      
    } catch (Exception e) {
      log.error("Failed to start dispatch workflow step for parent entity {}: {}", parentEntityId, e.getMessage());
      throw new RuntimeException("Failed to start dispatch workflow step for parent entity " + 
                               parentEntityId + ": " + e.getMessage(), e);
    }
  }

  /**
   * Update workflow step outcome for a specific parent entity
   */
  private void updateDispatchWorkflowStepOutcomeForParentEntity(ProcessedItemDispatchBatch processedItemDispatchBatch,
                                                               Long workflowId,
                                                               DispatchProcessedItemConsumptionRepresentation consumption) {
    try {
      log.debug("Updating dispatch workflow step outcome for parent entity {} with consumed pieces {}", 
               consumption.getPreviousOperationEntityId(), consumption.getConsumedPiecesCount());

      // Create dispatchBatchOutcome object with data from ProcessedItemDispatchBatch for this specific parent entity
      OperationOutcomeData.BatchOutcome dispatchBatchOutcome = OperationOutcomeData.BatchOutcome.builder()
          .id(consumption.getPreviousOperationEntityId())
          .initialPiecesCount(consumption.getConsumedPiecesCount()) // Pieces consumed from this specific parent
          .piecesAvailableForNext(consumption.getConsumedPiecesCount()) // Available pieces from this parent consumption
          .startedAt(processedItemDispatchBatch.getDispatchBatch().getDispatchCreatedAt())
          .completedAt(processedItemDispatchBatch.getDispatchBatch().getDispatchedAt()) // Will be null initially, updated when dispatched
          .createdAt(processedItemDispatchBatch.getCreatedAt())
          .updatedAt(LocalDateTime.now())
          .deletedAt(processedItemDispatchBatch.getDeletedAt())
          .deleted(processedItemDispatchBatch.isDeleted())
          .build();

      // Get existing workflow step data and accumulate batch outcomes for this specific parent entity
      List<OperationOutcomeData.BatchOutcome> accumulatedBatchData = getAccumulatedBatchData(workflowId, consumption.getPreviousOperationEntityId());
      
      // Add the current batch outcome to the accumulated list
      accumulatedBatchData.add(dispatchBatchOutcome);

      ItemWorkflowStep dispatchItemWorkflowStep = itemWorkflowService.findItemWorkflowStepByParentEntityId(
          workflowId,
          processedItemDispatchBatch.getPreviousOperationProcessedItemId(),
          WorkflowStep.OperationType.DISPATCH);

      // Update workflow step with accumulated batch data for this specific parent entity
      itemWorkflowService.updateWorkflowStepForOperation(
          dispatchItemWorkflowStep,
          OperationOutcomeData.forDispatchOperation(accumulatedBatchData, LocalDateTime.now()));
      
      log.info("Successfully updated dispatch workflow step outcome for parent entity {} with {} consumed pieces", 
               consumption.getPreviousOperationEntityId(), consumption.getConsumedPiecesCount());
      
    } catch (Exception e) {
      log.error("Failed to update dispatch workflow step outcome for parent entity {}: {}", 
               consumption.getPreviousOperationEntityId(), e.getMessage());
      throw new RuntimeException("Failed to update dispatch workflow step outcome for parent entity " + 
                               consumption.getPreviousOperationEntityId() + ": " + e.getMessage(), e);
    }
  }

  /**
   * Update related entity IDs for a specific parent entity dispatch step
   */
  private void updateRelatedEntityIdsForParentEntityDispatchStep(Long workflowId,
                                                                Long parentEntityId,
                                                                Long dispatchBatchId) {
    try {
      log.debug("Updating related entity IDs for dispatch step with parent entity {} and dispatch batch {}", 
               parentEntityId, dispatchBatchId);
      
      // Find the specific DISPATCH ItemWorkflowStep for this parent entity
      ItemWorkflowStep targetDispatchStep = itemWorkflowService.findItemWorkflowStepByParentEntityId(
          workflowId,
          parentEntityId,
          WorkflowStep.OperationType.DISPATCH
      );

      if (targetDispatchStep != null) {
        // Update relatedEntityIds for this specific DISPATCH operation step
        itemWorkflowService.updateRelatedEntityIdsForSpecificStep(targetDispatchStep, dispatchBatchId);
        log.info("Successfully updated related entity IDs for dispatch step {} with parent entity {}", 
                 targetDispatchStep.getId(), parentEntityId);
      } else {
        log.warn("Could not find target DISPATCH ItemWorkflowStep for parent entity {} in workflow {}", 
                 parentEntityId, workflowId);
      }
      
    } catch (Exception e) {
      log.error("Failed to update related entity IDs for parent entity {} dispatch step: {}", 
               parentEntityId, e.getMessage());
      throw new RuntimeException("Failed to update related entity IDs for parent entity " + 
                               parentEntityId + " dispatch step: " + e.getMessage(), e);
    }
  }

  /**
   * Handles heat consumption for first operation (if applicable)
   */
  private void handleHeatConsumptionForFirstOperation(DispatchBatchRepresentation representation, 
                                                     ProcessedItemDispatchBatch processedItemDispatchBatch, 
                                                     ItemWorkflow workflow) {
    // Get the dispatch heat data if provided
    if (representation.getProcessedItemDispatchBatch() == null || 
        representation.getProcessedItemDispatchBatch().getDispatchHeats() == null || 
        representation.getProcessedItemDispatchBatch().getDispatchHeats().isEmpty()) {
      log.warn("No heat consumption data provided for first operation dispatch batch processed item {}. This may result in inventory inconsistency.",
               processedItemDispatchBatch.getId());
      return;
    }

    // Validate that heat consumption matches the required pieces
    int totalPiecesFromHeats = representation.getProcessedItemDispatchBatch().getDispatchHeats().stream()
        .mapToInt(heatRep -> heatRep.getPiecesUsed())
        .sum();

    if (totalPiecesFromHeats != processedItemDispatchBatch.getTotalDispatchPiecesCount()) {
      throw new IllegalArgumentException("Total pieces from heats (" + totalPiecesFromHeats + 
                                        ") does not match dispatch batch pieces count (" + 
                                        processedItemDispatchBatch.getTotalDispatchPiecesCount() + 
                                        ") for processed item " + processedItemDispatchBatch.getId());
    }

    // Get the dispatch batch creation time for validation
    LocalDateTime dispatchCreatedAt = processedItemDispatchBatch.getDispatchBatch().getDispatchCreatedAt();

    // Validate heat availability and consume pieces from inventory
    representation.getProcessedItemDispatchBatch().getDispatchHeats().forEach(heatRepresentation -> {
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
        
        // Validate timing - heat should be received before the dispatch creation time
        LocalDateTime rawMaterialReceivingDate = heat.getRawMaterialProduct().getRawMaterial().getRawMaterialReceivingDate();
        if (rawMaterialReceivingDate != null && rawMaterialReceivingDate.compareTo(dispatchCreatedAt) > 0) {
          log.error("The provided dispatch created at time={} is before raw material receiving date={} for heat={} !", 
                    dispatchCreatedAt, rawMaterialReceivingDate, heat.getHeatNumber());
          throw new RuntimeException("The provided dispatch created at time=" + dispatchCreatedAt + 
                                   " is before raw material receiving date=" + rawMaterialReceivingDate + 
                                   " for heat=" + heat.getHeatNumber() + " !");
        }
        
        // Update heat available pieces count
        log.info("Updating AvailablePiecesCount for heat={} from {} to {} for dispatch batch processed item {}", 
                 heat.getId(), heat.getAvailablePiecesCount(), newHeatPieces, processedItemDispatchBatch.getId());
        heat.setAvailablePiecesCount(newHeatPieces);
        
        // Persist the updated heat
        rawMaterialHeatService.updateRawMaterialHeat(heat);
        
        log.info("Successfully consumed {} pieces from heat {} for dispatch batch processed item {}", 
                 heatRepresentation.getPiecesUsed(), heat.getHeatNumber(), processedItemDispatchBatch.getId());
        
      } catch (Exception e) {
        log.error("Error processing heat consumption for dispatch batch: Heat ID={}, Error={}", 
                  heatRepresentation.getHeat().getId(), e.getMessage());
        throw new RuntimeException("Failed to process heat consumption for heat " + 
                                 heatRepresentation.getHeat().getId() + ": " + e.getMessage(), e);
      }
    });
  }

  /**
   * Handles pieces consumption from previous operation (if applicable)
   * Uses optimized single-call method to improve performance
   */
  private void handlePiecesConsumptionFromPreviousOperation(ProcessedItemDispatchBatch processedItemDispatchBatch, 
                                                           ItemWorkflow workflow) {
    // For non-first operations, consume pieces from the previous operation
    Long previousOperationProcessedItemId = processedItemDispatchBatch.getPreviousOperationProcessedItemId();
    
    if (previousOperationProcessedItemId != null) {
      // Use the optimized method that combines find + validate + consume in a single efficient call
      try {
        int piecesToConsume = processedItemDispatchBatch.getTotalDispatchPiecesCount() - processedItemDispatchBatch.getAdditionalPiecesCount();
        
        ItemWorkflowStep parentOperationStep = itemWorkflowService.validateAndConsumePiecesFromParentOperation(
            workflow.getId(),
            WorkflowStep.OperationType.DISPATCH,
            previousOperationProcessedItemId,
            piecesToConsume
        );

        log.info("Efficiently consumed {} pieces from {} operation {} for dispatch batch {}",
                 piecesToConsume, parentOperationStep.getOperationType(), 
                 previousOperationProcessedItemId, processedItemDispatchBatch.getId());

      } catch (IllegalArgumentException e) {
        // Re-throw with context for dispatch batch
        log.error("Failed to consume pieces for dispatch batch: {}", e.getMessage());
        throw e;
      }
    }
  }

  /**
   * Handles consumption from multiple parent operations for a single dispatch batch.
   * This method processes each parent operation consumption and validates/consumes pieces accordingly.
   */
  private void handleMultipleParentOperationsConsumption(DispatchBatchRepresentation representation,
                                                        ProcessedItemDispatchBatch processedItemDispatchBatch,
                                                        ItemWorkflow workflow) {
    
    List<DispatchProcessedItemConsumptionRepresentation> consumptions = 
        representation.getDispatchProcessedItemConsumptions();
    
    if (consumptions == null || consumptions.isEmpty()) {
      log.warn("No parent operation consumptions provided for multi-parent dispatch batch {}", 
               processedItemDispatchBatch.getId());
      return;
    }
    
    log.info("Processing {} parent operation consumptions for dispatch batch {}", 
             consumptions.size(), processedItemDispatchBatch.getId());
    
    // Create and save consumption records
    List<DispatchProcessedItemConsumption> consumptionEntities = new ArrayList<>();
    
    // Validate and consume pieces from each parent operation
    for (DispatchProcessedItemConsumptionRepresentation consumption : consumptions) {
      try {
        // Validate the consumption data
        if (consumption.getPreviousOperationEntityId() == null || 
            consumption.getConsumedPiecesCount() == null || 
            consumption.getConsumedPiecesCount() <= 0) {
          log.error("Invalid consumption data: entityId={}, pieces={}", 
                   consumption.getPreviousOperationEntityId(), consumption.getConsumedPiecesCount());
          throw new IllegalArgumentException("Invalid consumption data for entity " + 
                                           consumption.getPreviousOperationEntityId());
        }
        
        // Consume pieces from the parent operation
        itemWorkflowService.validateAndConsumePiecesFromParentOperation(
            workflow.getId(),
            WorkflowStep.OperationType.DISPATCH,
            consumption.getPreviousOperationEntityId(),
            consumption.getConsumedPiecesCount()
        );
        
        // Create consumption entity
        DispatchProcessedItemConsumption consumptionEntity = DispatchProcessedItemConsumption.builder()
            .dispatchBatch(processedItemDispatchBatch.getDispatchBatch())
            .previousOperationEntityId(consumption.getPreviousOperationEntityId())
            .previousOperationType(WorkflowStep.OperationType.valueOf(consumption.getPreviousOperationType()))
            .consumedPiecesCount(consumption.getConsumedPiecesCount())
            .availablePiecesCount(consumption.getAvailablePiecesCount())
            .batchIdentifier(consumption.getBatchIdentifier())
            .entityContext(consumption.getEntityContext())
            .createdAt(LocalDateTime.now())
            .deleted(false)
            .build();
        
        consumptionEntities.add(consumptionEntity);
        
        log.info("Successfully consumed {} pieces from {} operation {} for multi-parent dispatch batch {}",
                 consumption.getConsumedPiecesCount(), 
                 consumption.getPreviousOperationType(),
                 consumption.getPreviousOperationEntityId(), 
                 processedItemDispatchBatch.getId());
                 
      } catch (Exception e) {
        log.error("Failed to consume pieces from parent operation {}: {}", 
                 consumption.getPreviousOperationEntityId(), e.getMessage());
        throw new RuntimeException("Failed to consume pieces from parent operation " + 
                                 consumption.getPreviousOperationEntityId() + ": " + e.getMessage(), e);
      }
    }
    
    // Save all consumption entities
    dispatchProcessedItemConsumptionRepository.saveAll(consumptionEntities);
    
    // Update the dispatch batch with the consumption entities
    if (processedItemDispatchBatch.getDispatchBatch().getDispatchProcessedItemConsumptions() == null) {
      processedItemDispatchBatch.getDispatchBatch().setDispatchProcessedItemConsumptions(consumptionEntities);
    } else {
      processedItemDispatchBatch.getDispatchBatch().getDispatchProcessedItemConsumptions().addAll(consumptionEntities);
    }

    log.info("Successfully created {} consumption records for multi-parent dispatch batch {}", 
             consumptionEntities.size(), processedItemDispatchBatch.getId());
  }

  /**
   * Updates workflow step with batch outcome data
   */
  private void updateWorkflowStepWithBatchOutcome(ProcessedItemDispatchBatch processedItemDispatchBatch, 
                                                 Long workflowId) throws Exception {
    // Create dispatchBatchOutcome object with data from ProcessedItemDispatchBatch
    OperationOutcomeData.BatchOutcome dispatchBatchOutcome = OperationOutcomeData.BatchOutcome.builder()
        .id(processedItemDispatchBatch.getId())
        .initialPiecesCount(processedItemDispatchBatch.getTotalDispatchPiecesCount())
        .piecesAvailableForNext(processedItemDispatchBatch.getTotalDispatchPiecesCount()) // Available for next operations if any
        .startedAt(processedItemDispatchBatch.getDispatchBatch().getDispatchCreatedAt())
        .completedAt(processedItemDispatchBatch.getDispatchBatch().getDispatchedAt()) // Will be null initially, updated when dispatched
        .createdAt(processedItemDispatchBatch.getCreatedAt())
        .updatedAt(LocalDateTime.now())
        .deletedAt(processedItemDispatchBatch.getDeletedAt())
        .deleted(processedItemDispatchBatch.isDeleted())
        .build();

    // Get existing workflow step data and accumulate batch outcomes
    List<OperationOutcomeData.BatchOutcome> accumulatedBatchData = getAccumulatedBatchData(workflowId, processedItemDispatchBatch.getPreviousOperationProcessedItemId());
    
    // Add the current batch outcome to the accumulated list
    accumulatedBatchData.add(dispatchBatchOutcome);

    ItemWorkflowStep dispatchItemWorkflowStep = itemWorkflowService.findItemWorkflowStepByParentEntityId(
        workflowId,
        processedItemDispatchBatch.getPreviousOperationProcessedItemId(),
        WorkflowStep.OperationType.DISPATCH);

    // Update workflow step with accumulated batch data for this specific parent entity
    itemWorkflowService.updateWorkflowStepForOperation(
        dispatchItemWorkflowStep,
        OperationOutcomeData.forDispatchOperation(accumulatedBatchData, LocalDateTime.now()));
  }

  /*
   * Helper method to retrieve the DISPATCH ItemWorkflowStep for a given workflow & processed item
   */
  private ItemWorkflowStep getDispatchWorkflowStep(Long workflowId, Long parentEntityId) {
    return itemWorkflowService.findItemWorkflowStepByParentEntityId(
        workflowId,
        parentEntityId,
        WorkflowStep.OperationType.DISPATCH);
  }


  /**
   * Get accumulated batch data from existing workflow step
   */
  private List<OperationOutcomeData.BatchOutcome> getAccumulatedBatchData(Long workflowId, Long parentEntityId) throws Exception {
    // Get the existing dispatch workflow step using helper method
    ItemWorkflowStep existingDispatchStep = getDispatchWorkflowStep(workflowId, parentEntityId);
    
    if (existingDispatchStep != null) {
      // Extract existing batch data using helper method
      return itemWorkflowService.extractExistingBatchData(existingDispatchStep);
    }
    
    return new ArrayList<>();
  }

  public DispatchBatchListRepresentation getAllDispatchBatchesOfTenantWithoutPagination(long tenantId) {
    List<DispatchBatch> dispatchBatches = dispatchBatchRepository.findByTenantIdAndDeletedIsFalseOrderByUpdatedAtDesc(tenantId);
    return DispatchBatchListRepresentation.builder()
        .dispatchBatches(dispatchBatches.stream().map(dispatchBatchAssembler::dissemble).toList())
        .build();
  }

  public Page<DispatchBatchRepresentation> getAllDispatchBatchesOfTenant(long tenantId, int page, int size) {
    Pageable pageable = PageRequest.of(page, size);
    return dispatchBatchRepository.findByTenantIdAndDeletedIsFalseOrderByUpdatedAtDesc(tenantId, pageable)
        .map(dispatchBatchAssembler::dissemble);
  }

  public DispatchBatchRepresentation markReadyToDispatchBatch(long tenantId, long dispatchBatchId, DispatchBatchRepresentation representation) {
    tenantService.validateTenantExists(tenantId);
    DispatchBatch existingDispatchBatch = getDispatchBatchById(dispatchBatchId);

    if (existingDispatchBatch.getDispatchBatchStatus() != DispatchBatch.DispatchBatchStatus.DISPATCH_IN_PROGRESS) {
        log.error("DispatchBatch having dispatch batch number={}, having id={} is not in DISPATCH_IN_PROGRESS status!",
            existingDispatchBatch.getDispatchBatchNumber(), existingDispatchBatch.getId());
        throw new IllegalStateException("Dispatch batch must be in DISPATCH_IN_PROGRESS status");
    }
    LocalDateTime readyAtTime = ConvertorUtils.convertStringToLocalDateTime(representation.getDispatchReadyAt());
    validateReadyToDispatchTime(existingDispatchBatch, readyAtTime);
    
    // Handle uniform vs non-uniform packaging
    boolean useUniformPackaging = representation.getUseUniformPackaging() != null ? 
                                 representation.getUseUniformPackaging() : true;
    existingDispatchBatch.setUseUniformPackaging(useUniformPackaging);
    
    if (useUniformPackaging) {
        // Traditional uniform packaging validation
        validatePackagingQuantity(existingDispatchBatch, representation.getPackagingQuantity(), representation.getPerPackagingQuantity());
        existingDispatchBatch.setPackagingType(PackagingType.valueOf(representation.getPackagingType()));
        existingDispatchBatch.setPackagingQuantity(representation.getPackagingQuantity());
        existingDispatchBatch.setPerPackagingQuantity(representation.getPerPackagingQuantity());
        
        // Clear any existing packages
        existingDispatchBatch.getDispatchPackages().clear();
        
        // Create DispatchPackage entities for uniform packaging
        PackagingType packagingType = PackagingType.valueOf(representation.getPackagingType());
        int packagingQuantity = representation.getPackagingQuantity();
        int perPackagingQuantity = representation.getPerPackagingQuantity();
        
        for (int packageNumber = 1; packageNumber <= packagingQuantity; packageNumber++) {
            DispatchPackage dispatchPackage = DispatchPackage.builder()
                .dispatchBatch(existingDispatchBatch)
                .packagingType(packagingType)
                .quantityInPackage(perPackagingQuantity)
                .packageNumber(packageNumber)
                .createdAt(LocalDateTime.now())
                .build();
            existingDispatchBatch.getDispatchPackages().add(dispatchPackage);
        }
    } else {
        // Non-uniform packaging validation
        if (representation.getDispatchPackages() == null || representation.getDispatchPackages().isEmpty()) {
            log.error("Non-uniform packaging selected but no dispatch packages provided for dispatch batch id={}", 
                      existingDispatchBatch.getId());
            throw new IllegalArgumentException("Non-uniform packaging requires at least one package specification");
        }
        
        // Clear existing packages and add new ones from representation
        existingDispatchBatch.getDispatchPackages().clear();
        
        PackagingType packagingType = PackagingType.valueOf(representation.getPackagingType());
        existingDispatchBatch.setPackagingType(packagingType);
        
        int packageNumber = 1;
        for (DispatchPackageRepresentation packageRep : representation.getDispatchPackages()) {
            DispatchPackage dispatchPackage = DispatchPackage.builder()
                .dispatchBatch(existingDispatchBatch)
                .packagingType(packagingType)
                .quantityInPackage(packageRep.getQuantityInPackage())
                .packageNumber(packageNumber++)
                .createdAt(LocalDateTime.now())
                .build();
            existingDispatchBatch.getDispatchPackages().add(dispatchPackage);
        }
        
        // Set packaging quantity to the total number of packages
        existingDispatchBatch.setPackagingQuantity(existingDispatchBatch.getDispatchPackages().size());
        
        // Validate the total pieces
        validatePackagingQuantity(existingDispatchBatch, null, null);
    }

    existingDispatchBatch.setDispatchBatchStatus(DispatchBatch.DispatchBatchStatus.READY_TO_DISPATCH);
    existingDispatchBatch.setDispatchReadyAt(readyAtTime);

    DispatchBatch updatedDispatchBatch = dispatchBatchRepository.save(existingDispatchBatch);
    return dispatchBatchAssembler.dissemble(updatedDispatchBatch);
  }

  private void validateReadyToDispatchTime(DispatchBatch existingDispatchBatch, LocalDateTime providedReadyToDispatchTime){
    if (existingDispatchBatch.getDispatchCreatedAt().compareTo(providedReadyToDispatchTime) > 0) {
      log.error("The provided ReadyToDispatchTime for DispatchBatch having dispatch batch number={}, having id={} is before dispatch batch created time!", existingDispatchBatch.getDispatchBatchNumber(), existingDispatchBatch.getId());
      throw new RuntimeException("The provided ReadyToDispatchTime for DispatchBatch having dispatch batch number=" + existingDispatchBatch.getDispatchBatchNumber() + " , having id=" + existingDispatchBatch.getId() + " is before dispatch batch created time!");
    }
  }

  private void validatePackagingQuantity(DispatchBatch dispatchBatch, Integer packagingQuantity, Integer perPackagingQuantity) {
    int totalDispatchPieces = dispatchBatch.getProcessedItemDispatchBatch().getTotalDispatchPiecesCount();
    
    // Handle non-uniform packaging if dispatchPackages are provided
    if (dispatchBatch.getUseUniformPackaging() != null && !dispatchBatch.getUseUniformPackaging() && 
        dispatchBatch.getDispatchPackages() != null && !dispatchBatch.getDispatchPackages().isEmpty()) {
        
        int calculatedTotalPieces = dispatchBatch.getDispatchPackages().stream()
            .mapToInt(DispatchPackage::getQuantityInPackage)
            .sum();
            
        if (calculatedTotalPieces != totalDispatchPieces) {
            log.error("Sum of package quantities {} does not match total dispatch pieces count {} for dispatch batch id={}",
                calculatedTotalPieces, totalDispatchPieces, dispatchBatch.getId());
            throw new IllegalArgumentException(
                String.format("Sum of package quantities (%d) must match total dispatch pieces count (%d)",
                    calculatedTotalPieces, totalDispatchPieces)
            );
        }
    } 
    // Handle uniform packaging (backward compatibility)
    else {
        int calculatedTotalPieces = packagingQuantity * perPackagingQuantity;
        
        if (calculatedTotalPieces != totalDispatchPieces) {
            log.error("Packaging quantity {} does not match total dispatch pieces count {} for dispatch batch id={}",
                calculatedTotalPieces, totalDispatchPieces, dispatchBatch.getId());
            throw new IllegalArgumentException(
                String.format("Packaging quantity (%d) must match total dispatch pieces count (%d)",
                    calculatedTotalPieces, totalDispatchPieces)
            );
        }
    }
  }

  private void validateDispatchedTime(DispatchBatch existingDispatchBatch, LocalDateTime providedDispatchedTime){
    if (existingDispatchBatch.getDispatchReadyAt().compareTo(providedDispatchedTime) > 0) {
      log.error("The provided dispatched time for DispatchBatch having dispatch batch number={}, having id={} is before the dispatch ready time!", existingDispatchBatch.getDispatchBatchNumber(), existingDispatchBatch.getId());
      throw new RuntimeException("The provided dispatched time for DispatchBatch having dispatch batch number=" + existingDispatchBatch.getDispatchBatchNumber() + " , having id=" + existingDispatchBatch.getId() + " is before the dispatch ready time!");
    }
  }

  // markDispatchedToDispatchBatch
  public DispatchBatchRepresentation markDispatchedToDispatchBatch(long tenantId, long dispatchBatchId, DispatchBatchRepresentation representation){
    tenantService.validateTenantExists(tenantId);
    DispatchBatch existingDispatchBatch = getDispatchBatchById(dispatchBatchId);

    if(existingDispatchBatch.getDispatchBatchStatus() != DispatchBatch.DispatchBatchStatus.READY_TO_DISPATCH){
      log.error("DispatchBatch having dispatch batch number={}, having id={} is not in READY_TO_DISPATCH status!", existingDispatchBatch.getDispatchBatchNumber(), existingDispatchBatch.getId());
      throw new IllegalStateException("Dispatch batch must be in READY_TO_DISPATCH status");
    }
    
    // Validate dispatched time
    LocalDateTime dispatchTime = ConvertorUtils.convertStringToLocalDateTime(representation.getDispatchedAt());
    validateDispatchedTime(existingDispatchBatch, dispatchTime);
    
    // Validate and set invoice related fields
    validateAndSetInvoiceFields(existingDispatchBatch, representation, tenantId);
    
    existingDispatchBatch.setDispatchBatchStatus(DispatchBatch.DispatchBatchStatus.DISPATCHED);
    existingDispatchBatch.setDispatchedAt(dispatchTime);
    DispatchBatch updatedDispatchBatch = dispatchBatchRepository.save(existingDispatchBatch);
    
    // Update ItemWorkflowStep entities for the dispatch completion
    updateItemWorkflowStepsForDispatchCompletion(existingDispatchBatch, dispatchTime);
    
    return dispatchBatchAssembler.dissemble(updatedDispatchBatch);
  }

  /**
   * Updates the ItemWorkflowStep entities associated with the dispatch batch
   * Sets the DISPATCH step status to COMPLETED and updates completedAt timestamp for batch outcomes
   * Handles both single-parent and multi-parent dispatch batches
   * 
   * @param dispatchBatch The completed dispatch batch entity
   * @param completedAt The completion timestamp
   */
  private void updateItemWorkflowStepsForDispatchCompletion(DispatchBatch dispatchBatch, LocalDateTime completedAt) {
    try {
      ProcessedItemDispatchBatch processedItemDispatchBatch = dispatchBatch.getProcessedItemDispatchBatch();
      Long itemWorkflowId = processedItemDispatchBatch.getItemWorkflowId();
      
      if (itemWorkflowId == null) {
        log.warn("itemWorkflowId is null, skipping ItemWorkflowStep updates for dispatch completion");
        return;
      }
      
      log.info("Updating ItemWorkflowStep entities for itemWorkflowId: {} after dispatch completion", itemWorkflowId);
      
      // Get the ItemWorkflow
      ItemWorkflow itemWorkflow = itemWorkflowService.getItemWorkflowById(itemWorkflowId);
      
      // Validate that the workflow belongs to the same item as the dispatch batch
      Long dispatchItemId = processedItemDispatchBatch.getItem().getId();
      Long workflowItemId = itemWorkflow.getItem().getId();
      
      if (!dispatchItemId.equals(workflowItemId)) {
        log.error("ItemWorkflow {} does not belong to the same item as dispatch batch {}. Dispatch item ID: {}, Workflow item ID: {}", 
                 itemWorkflowId, dispatchBatch.getId(), dispatchItemId, workflowItemId);
        throw new IllegalArgumentException("ItemWorkflow does not belong to the same item as the dispatch batch");
      }
      
      // Check if this is a multi-parent dispatch batch
      boolean isMultiParentDispatch = Boolean.TRUE.equals(processedItemDispatchBatch.getIsMultiParentDispatch());
      
      if (isMultiParentDispatch && 
          dispatchBatch.getDispatchProcessedItemConsumptions() != null && 
          !dispatchBatch.getDispatchProcessedItemConsumptions().isEmpty()) {
        
        // Handle multi-parent dispatch completion
        log.info("Handling multi-parent dispatch completion for {} parent entities", 
                 dispatchBatch.getDispatchProcessedItemConsumptions().size());
        
        updateMultipleDispatchWorkflowStepsForCompletion(
            dispatchBatch, 
            processedItemDispatchBatch, 
            itemWorkflowId, 
            completedAt
        );
        
      } else {
        // Handle single-parent dispatch completion (legacy mode)
        log.info("Handling single-parent dispatch completion for dispatch batch {}", dispatchBatch.getId());
        
        updateSingleDispatchWorkflowStepForCompletion(
            dispatchBatch,
            processedItemDispatchBatch,
            itemWorkflowId,
            completedAt
        );
      }
      
    } catch (Exception e) {
      log.error("Failed to update ItemWorkflowStep entities for dispatch completion on dispatch batch ID: {} - {}", dispatchBatch.getId(), e.getMessage());
      // Re-throw the exception to fail the dispatch completion since workflow integration is now mandatory
      throw new RuntimeException("Failed to update workflow step for dispatch completion: " + e.getMessage(), e);
    }
  }

  /**
   * Update multiple dispatch workflow steps for multi-parent dispatch completion
   */
  private void updateMultipleDispatchWorkflowStepsForCompletion(DispatchBatch dispatchBatch,
                                                               ProcessedItemDispatchBatch processedItemDispatchBatch,
                                                               Long itemWorkflowId,
                                                               LocalDateTime completedAt) {
    try {
      log.info("Updating multiple dispatch workflow steps for completion of multi-parent dispatch batch {} with {} parent entities", 
               dispatchBatch.getId(), 
               dispatchBatch.getDispatchProcessedItemConsumptions().size());
      
      // Process each consumption record to update corresponding workflow steps
      for (DispatchProcessedItemConsumption consumption : dispatchBatch.getDispatchProcessedItemConsumptions()) {
        if (!consumption.isDeleted()) {
          try {
            log.debug("Updating dispatch workflow step completion for parent entity {} of type {} with {} consumed pieces", 
                     consumption.getPreviousOperationEntityId(), 
                     consumption.getPreviousOperationType(),
                     consumption.getConsumedPiecesCount());
            
            updateDispatchWorkflowStepCompletionForParentEntity(
                dispatchBatch,
                processedItemDispatchBatch,
                itemWorkflowId,
                consumption,
                completedAt
            );
            
            log.info("Successfully updated dispatch workflow step completion for parent entity {} of type {}", 
                     consumption.getPreviousOperationEntityId(), consumption.getPreviousOperationType());
            
          } catch (Exception e) {
            log.error("Failed to update dispatch workflow step completion for parent entity {} of type {}: {}", 
                     consumption.getPreviousOperationEntityId(), consumption.getPreviousOperationType(), e.getMessage());
            throw new RuntimeException("Failed to update dispatch workflow step completion for parent entity " + 
                                     consumption.getPreviousOperationEntityId() + ": " + e.getMessage(), e);
          }
        }
      }
      
      log.info("Successfully updated all {} dispatch workflow steps for multi-parent dispatch batch completion {}", 
               dispatchBatch.getDispatchProcessedItemConsumptions().size(), dispatchBatch.getId());
      
    } catch (Exception e) {
      log.error("Failed to update multiple dispatch workflow steps for completion of dispatch batch {}: {}", 
                dispatchBatch.getId(), e.getMessage());
      throw new RuntimeException("Failed to update multiple dispatch workflow steps for completion: " + e.getMessage(), e);
    }
  }

  /**
   * Update workflow step completion for a specific parent entity in multi-parent dispatch
   */
  private void updateDispatchWorkflowStepCompletionForParentEntity(DispatchBatch dispatchBatch,
                                                                  ProcessedItemDispatchBatch processedItemDispatchBatch,
                                                                  Long itemWorkflowId,
                                                                  DispatchProcessedItemConsumption consumption,
                                                                  LocalDateTime completedAt) {
    try {
      // Find the specific DISPATCH ItemWorkflowStep for this parent entity
      ItemWorkflowStep dispatchStep = itemWorkflowService.findItemWorkflowStepByParentEntityId(
          itemWorkflowId,
          consumption.getPreviousOperationEntityId(),
          WorkflowStep.OperationType.DISPATCH
      );

      if (dispatchStep != null) {
        // Deserialize, update, and serialize back the operation outcome data
        try {
          String existingOutcomeDataJson = dispatchStep.getOperationOutcomeData();
          if (existingOutcomeDataJson != null && !existingOutcomeDataJson.trim().isEmpty()) {
            OperationOutcomeData operationOutcomeData = objectMapper.readValue(existingOutcomeDataJson, OperationOutcomeData.class);
            operationOutcomeData.setOperationLastUpdatedAt(completedAt);
            
            // Get the consumed pieces count for this specific parent entity
            Integer consumedPiecesCount = consumption.getConsumedPiecesCount();
            
            // Update the completedAt and deduct consumed pieces from piecesAvailableForNext for the specific parent entity batch outcome
            if (operationOutcomeData.getBatchData() != null) {
              operationOutcomeData.getBatchData().stream()
                  .filter(batch -> !batch.getDeleted())
                  .filter(batch -> batch.getId().equals(consumption.getPreviousOperationEntityId()))
                  .forEach(batch -> {
                    batch.setCompletedAt(completedAt);
                    
                    // Since Dispatch is the last step, deduct the consumed pieces from piecesAvailableForNext
                    if (consumedPiecesCount != null && consumedPiecesCount > 0) {
                      Integer currentAvailableForNext = batch.getPiecesAvailableForNext();
                      if (currentAvailableForNext != null) {
                        int newAvailableForNext = currentAvailableForNext - consumedPiecesCount;
                        batch.setPiecesAvailableForNext(newAvailableForNext);
                        
                        // Update the workflow step's pieces available for next as well
                        Integer stepCurrentAvailable = dispatchStep.getPiecesAvailableForNext();
                        if (stepCurrentAvailable != null) {
                          dispatchStep.setPiecesAvailableForNext(stepCurrentAvailable - consumedPiecesCount);
                        }
                        
                        log.info("Deducted {} consumed pieces from piecesAvailableForNext for parent entity {} (batch {}). " +
                                "Previous: {}, New: {}", 
                                consumedPiecesCount, consumption.getPreviousOperationEntityId(), 
                                batch.getId(), currentAvailableForNext, newAvailableForNext);
                      } else {
                        log.warn("piecesAvailableForNext is null for parent entity batch {} in dispatch completion for parent entity {}", 
                                batch.getId(), consumption.getPreviousOperationEntityId());
                      }
                    }
                  });
            }
            
            // Use the tree-based service method to update the workflow step
            itemWorkflowService.updateWorkflowStepForSpecificStep(dispatchStep, operationOutcomeData);

          } else {
            log.warn("No operation outcome data found for DISPATCH step in workflow for itemWorkflowId: {} and parent entity: {}", 
                    itemWorkflowId, consumption.getPreviousOperationEntityId());
          }
        } catch (Exception e) {
          log.error("Failed to update operation outcome data for DISPATCH step for parent entity {}: {}", 
                   consumption.getPreviousOperationEntityId(), e.getMessage());
          throw new RuntimeException("Failed to update operation outcome data for parent entity " + 
                                   consumption.getPreviousOperationEntityId() + ": " + e.getMessage(), e);
        }
      } else {
        log.warn("No DISPATCH step found in workflow for itemWorkflowId: {} and parent entity: {}", 
                itemWorkflowId, consumption.getPreviousOperationEntityId());
      }
      
    } catch (Exception e) {
      log.error("Failed to update dispatch workflow step completion for parent entity {}: {}", 
               consumption.getPreviousOperationEntityId(), e.getMessage());
      throw new RuntimeException("Failed to update dispatch workflow step completion for parent entity " + 
                               consumption.getPreviousOperationEntityId() + ": " + e.getMessage(), e);
    }
  }

  /**
   * Update single dispatch workflow step for single-parent dispatch completion (legacy mode)
   */
  private void updateSingleDispatchWorkflowStepForCompletion(DispatchBatch dispatchBatch,
                                                            ProcessedItemDispatchBatch processedItemDispatchBatch,
                                                            Long itemWorkflowId,
                                                            LocalDateTime completedAt) {
    try {
      // Find the specific DISPATCH ItemWorkflowStep that corresponds to this dispatch batch using helper method
      ItemWorkflowStep dispatchStep = getDispatchWorkflowStep(itemWorkflowId, processedItemDispatchBatch.getPreviousOperationProcessedItemId());
      
      if (dispatchStep != null) {
        // Deserialize, update, and serialize back the operation outcome data
        try {
          String existingOutcomeDataJson = dispatchStep.getOperationOutcomeData();
          if (existingOutcomeDataJson != null && !existingOutcomeDataJson.trim().isEmpty()) {
            OperationOutcomeData operationOutcomeData = objectMapper.readValue(existingOutcomeDataJson, OperationOutcomeData.class);
            operationOutcomeData.setOperationLastUpdatedAt(completedAt);
            
            // Get the total dispatch pieces count to deduct from piecesAvailableForNext
            Integer totalDispatchPiecesCount = processedItemDispatchBatch.getTotalDispatchPiecesCount();
            
            // Update the completedAt and deduct dispatched pieces from piecesAvailableForNext for the specific batch outcome
            if (operationOutcomeData.getBatchData() != null) {
              operationOutcomeData.getBatchData().stream()
                  .filter(batch -> batch.getId().equals(processedItemDispatchBatch.getId()))
                  .forEach(batch -> {
                    batch.setCompletedAt(completedAt);
                    
                    // Since Dispatch is the last step, deduct the totalDispatchPiecesCount from piecesAvailableForNext
                    if (totalDispatchPiecesCount != null && totalDispatchPiecesCount > 0) {
                      Integer currentAvailableForNext = batch.getPiecesAvailableForNext();
                      if (currentAvailableForNext != null) {
                        int newAvailableForNext = currentAvailableForNext - totalDispatchPiecesCount;
                        batch.setPiecesAvailableForNext(newAvailableForNext);
                        
                        // Update the workflow step's pieces available for next as well
                        Integer stepCurrentAvailable = dispatchStep.getPiecesAvailableForNext();
                        if (stepCurrentAvailable != null) {
                          dispatchStep.setPiecesAvailableForNext(stepCurrentAvailable - totalDispatchPiecesCount);
                        }
                        
                        log.info("Deducted {} dispatched pieces from piecesAvailableForNext for single-parent batch {}. " +
                                "Previous: {}, New: {}", 
                                totalDispatchPiecesCount, batch.getId(), currentAvailableForNext, newAvailableForNext);
                      } else {
                        log.warn("piecesAvailableForNext is null for batch {} in single-parent dispatch completion", batch.getId());
                      }
                    }
                  });
            }
            
            // Use the tree-based service method to update the workflow step
            itemWorkflowService.updateWorkflowStepForSpecificStep(dispatchStep, operationOutcomeData);

          } else {
            log.warn("No operation outcome data found for DISPATCH step in workflow for itemWorkflowId: {}", itemWorkflowId);
          }
        } catch (Exception e) {
          log.error("Failed to update operation outcome data for single-parent DISPATCH step: {}", e.getMessage());
          throw new RuntimeException("Failed to update operation outcome data: " + e.getMessage(), e);
        }
      } else {
        log.warn("No DISPATCH step found in workflow for single-parent dispatch completion itemWorkflowId: {}", itemWorkflowId);
      }
      
    } catch (Exception e) {
      log.error("Failed to update single dispatch workflow step for completion: {}", e.getMessage());
      throw new RuntimeException("Failed to update single dispatch workflow step for completion: " + e.getMessage(), e);
    }
  }

  private void validateAndSetInvoiceFields(DispatchBatch dispatchBatch, DispatchBatchRepresentation representation, long tenantId) {
    // Validate invoice number is provided
    if (representation.getInvoiceNumber() == null || representation.getInvoiceNumber().isEmpty()) {
      log.error("Invoice number is required for dispatching batch with id={}", dispatchBatch.getId());
      throw new IllegalArgumentException("Invoice number is required for dispatch");
    }
    
    // Check invoice number uniqueness
    boolean invoiceExists = dispatchBatchRepository.existsByInvoiceNumberAndTenantIdAndDeletedFalse(
        representation.getInvoiceNumber(), tenantId);
    if (invoiceExists) {
      log.error("Invoice number {} already exists for tenant {}", representation.getInvoiceNumber(), tenantId);
      throw new IllegalStateException("Invoice number " + representation.getInvoiceNumber() + " already exists");
    }
    
    // Validate and set invoice date time
    if (representation.getInvoiceDateTime() == null || representation.getInvoiceDateTime().isEmpty()) {
      log.error("Invoice date time is required for dispatching batch with id={}", dispatchBatch.getId());
      throw new IllegalArgumentException("Invoice date time is required for dispatch");
    }
    
    LocalDateTime invoiceDateTime = ConvertorUtils.convertStringToLocalDateTime(representation.getInvoiceDateTime());
    
    // Validate invoice date time is after or equal to dispatch ready time
    if (dispatchBatch.getDispatchReadyAt().isAfter(invoiceDateTime)) {
      log.error("Invoice date time {} is before dispatch ready time {} for batch id={}", 
          invoiceDateTime, dispatchBatch.getDispatchReadyAt(), dispatchBatch.getId());
      throw new IllegalArgumentException(
          "Invoice date time must be greater than or equal to dispatch ready time");
    }
    
    // Set invoice details
    dispatchBatch.setInvoiceNumber(representation.getInvoiceNumber());
    dispatchBatch.setInvoiceDateTime(invoiceDateTime);
    
    // Set purchase order details if provided
    if (representation.getPurchaseOrderNumber() != null && !representation.getPurchaseOrderNumber().isEmpty()) {
      dispatchBatch.setPurchaseOrderNumber(representation.getPurchaseOrderNumber());
      
      if (representation.getPurchaseOrderDateTime() != null && !representation.getPurchaseOrderDateTime().isEmpty()) {
        dispatchBatch.setPurchaseOrderDateTime(
            ConvertorUtils.convertStringToLocalDateTime(representation.getPurchaseOrderDateTime()));
      }
    }
  }

  @Transactional(rollbackFor = Exception.class)
  public DispatchBatchRepresentation deleteDispatchBatch(long tenantId, long dispatchBatchId) throws DocumentDeletionException {
    log.info("Starting dispatch batch deletion transaction for tenant: {}, batch: {}", 
             tenantId, dispatchBatchId);
    
    try {
      // Phase 1: Validate all deletion preconditions
      tenantService.validateTenantExists(tenantId);
      DispatchBatch dispatchBatch = getDispatchBatchById(dispatchBatchId);

      if (dispatchBatch.getDispatchBatchStatus() != DispatchBatch.DispatchBatchStatus.DISPATCHED) {
        log.error("Cannot delete dispatch batch={} as it is not in DISPATCHED status!", dispatchBatchId);
        throw new IllegalStateException("This dispatch batch cannot be deleted as it is not in the DISPATCHED status.");
      }

      // Phase 2: Delete all documents attached to this dispatch batch using bulk delete for efficiency
      try {
          // Use bulk delete method from DocumentService for better performance
          documentService.deleteDocumentsForEntity(tenantId, DocumentLink.EntityType.DISPATCH_BATCH, dispatchBatchId);
          log.info("Successfully bulk deleted all documents attached to dispatch batch {} for tenant {}", dispatchBatchId, tenantId);
      } catch (DataAccessException e) {
          log.error("Database error while deleting documents attached to dispatch batch {}: {}", dispatchBatchId, e.getMessage(), e);
          throw new DocumentDeletionException("Database error occurred while deleting attached documents for dispatch batch " + dispatchBatchId, e);
      } catch (RuntimeException e) {
          // Handle document service specific runtime exceptions (storage, file system errors, etc.)
          log.error("Document service error while deleting documents attached to dispatch batch {}: {}", dispatchBatchId, e.getMessage(), e);
          throw new DocumentDeletionException("Document service error occurred while deleting attached documents for dispatch batch " + dispatchBatchId + ": " + e.getMessage(), e);
      } catch (Exception e) {
          // Handle any other unexpected exceptions
          log.error("Unexpected error while deleting documents attached to dispatch batch {}: {}", dispatchBatchId, e.getMessage(), e);
          throw new DocumentDeletionException("Unexpected error occurred while deleting attached documents for dispatch batch " + dispatchBatchId, e);
      }

      // Phase 3: Process inventory reversal for processed item - CRITICAL: Workflow and heat inventory operations
      ProcessedItemDispatchBatch processedItemDispatchBatch = dispatchBatch.getProcessedItemDispatchBatch();
      processInventoryReversalForProcessedItem(processedItemDispatchBatch, dispatchBatch, dispatchBatchId);

      // Phase 4: Soft delete associated records and finalize deletion
      softDeleteAssociatedRecordsAndFinalizeDeletion(dispatchBatch, processedItemDispatchBatch, dispatchBatchId);

      log.info("Successfully persisted dispatch batch deletion with ID: {}", dispatchBatchId);
      log.info("Successfully completed dispatch batch deletion transaction for ID: {}", dispatchBatchId);
      
      return dispatchBatchAssembler.dissemble(dispatchBatch);
      
    } catch (Exception e) {
      log.error("Dispatch batch deletion transaction failed for tenant: {}, batch: {}. " +
                "All changes will be rolled back. Error: {}", 
                tenantId, dispatchBatchId, e.getMessage());
      
      log.error("Dispatch batch deletion failed - workflow updates, heat inventory reversals, and entity deletions will be rolled back.");
      
      // Re-throw to ensure transaction rollback
      throw e;
    }
  }

  /**
   * Phase 2: Process inventory reversal for processed item
   */
  private void processInventoryReversalForProcessedItem(ProcessedItemDispatchBatch processedItemDispatchBatch, 
                                                       DispatchBatch dispatchBatch, 
                                                       long dispatchBatchId) {
    Item item = processedItemDispatchBatch.getItem();
    Long itemWorkflowId = processedItemDispatchBatch.getItemWorkflowId();

    if (itemWorkflowId != null) {
      try {
        // Use workflow-based validation: check if all entries in next operation are marked deleted
        // Note: Dispatch is typically the last operation, so this check may not be necessary
        // but we'll include it for consistency with the architecture
        boolean canDeleteDispatch = itemWorkflowService.areAllNextOperationBatchesDeleted(itemWorkflowId, processedItemDispatchBatch.getId(), WorkflowStep.OperationType.DISPATCH);

        if (!canDeleteDispatch) {
          log.error("Cannot delete dispatch id={} as the next operation has active (non-deleted) batches", dispatchBatchId);
          throw new IllegalStateException("This dispatch cannot be deleted as the next operation has active batch entries.");
        }

        log.info("Dispatch id={} is eligible for deletion - all next operation batches are deleted", dispatchBatchId);

        // Get the workflow to check if dispatch was the first operation
        ItemWorkflow workflow = itemWorkflowService.getItemWorkflowById(itemWorkflowId);
        boolean wasFirstOperation = workflow.getWorkflowTemplate().isFirstOperationType(WorkflowStep.OperationType.DISPATCH);

        if (wasFirstOperation) {
          // This was the first operation - heat quantities will be returned to heat inventory
          log.info("Dispatch batch {} was first operation for item {}, heat inventory will be reverted",
                   dispatchBatchId, item.getId());

          // Update workflow step to mark dispatch batch as deleted and adjust piece counts
          Integer totalDispatchPiecesCount = processedItemDispatchBatch.getTotalDispatchPiecesCount();
          if (totalDispatchPiecesCount != null && totalDispatchPiecesCount > 0) {
            try {
              itemWorkflowService.updateCurrentOperationStepForReturnedPieces(
                  itemWorkflowId, 
                  WorkflowStep.OperationType.DISPATCH, 
                  totalDispatchPiecesCount,
                  processedItemDispatchBatch.getId()
              );
              log.info("Successfully marked dispatch operation as deleted and updated workflow step for processed item {}, subtracted {} pieces", 
                       processedItemDispatchBatch.getId(), totalDispatchPiecesCount);
            } catch (Exception e) {
              log.error("Failed to update workflow step for deleted dispatch processed item {}: {}", 
                       processedItemDispatchBatch.getId(), e.getMessage());
              throw new RuntimeException("Failed to update workflow step for dispatch deletion: " + e.getMessage(), e);
            }
          } else {
            log.info("No dispatch pieces to subtract for deleted processed item {}", processedItemDispatchBatch.getId());
          }

          // Return heat quantities to original heats (similar to HeatTreatmentBatchService.deleteHeatTreatmentBatch method)
          LocalDateTime currentTime = LocalDateTime.now();
          if (processedItemDispatchBatch.getDispatchHeats() != null &&
              !processedItemDispatchBatch.getDispatchHeats().isEmpty()) {

            processedItemDispatchBatch.getDispatchHeats().forEach(dispatchHeat -> {
              Heat heat = dispatchHeat.getHeat();
              int piecesToReturn = dispatchHeat.getPiecesUsed();

              // Return pieces to heat inventory based on heat's unit of measurement
              if (heat.getIsInPieces()) {
                // Heat is managed in pieces - return to availablePiecesCount
                int newAvailablePieces = heat.getAvailablePiecesCount() + piecesToReturn;
                heat.setAvailablePiecesCount(newAvailablePieces);
                log.info("Returned {} pieces to heat {} (pieces-based), new available pieces: {}",
                         piecesToReturn, heat.getId(), newAvailablePieces);
              } else {
                throw new IllegalStateException("Dispatch batch has no pieces!");
              }

              // Persist the updated heat
              rawMaterialHeatService.updateRawMaterialHeat(heat);

              // Soft delete dispatch heat record
              dispatchHeat.setDeleted(true);
              dispatchHeat.setDeletedAt(currentTime);

              log.info("Successfully returned {} pieces from heat {} for deleted dispatch batch processed item {}",
                       piecesToReturn, heat.getId(), processedItemDispatchBatch.getId());
            });
          }
        } else {
          // Check if this is a multi-parent dispatch batch
          if (Boolean.TRUE.equals(processedItemDispatchBatch.getIsMultiParentDispatch()) &&
              dispatchBatch.getDispatchProcessedItemConsumptions() != null &&
              !dispatchBatch.getDispatchProcessedItemConsumptions().isEmpty()) {
            
            // Handle multi-parent consumption reversal
            log.info("Handling multi-parent dispatch deletion - returning pieces to {} parent operations", 
                     dispatchBatch.getDispatchProcessedItemConsumptions().size());
            
            for (DispatchProcessedItemConsumption consumption : dispatchBatch.getDispatchProcessedItemConsumptions()) {
              if (!consumption.isDeleted()) {
                try {
                  // Return pieces to each parent operation
                  itemWorkflowService.returnPiecesToSpecificPreviousOperation(
                      itemWorkflowId,
                      WorkflowStep.OperationType.DISPATCH,
                      consumption.getPreviousOperationEntityId(),
                      consumption.getConsumedPiecesCount(),
                      processedItemDispatchBatch.getId()
                  );
                  
                  log.info("Successfully returned {} pieces from dispatch back to {} operation {} in workflow {}",
                           consumption.getConsumedPiecesCount(),
                           consumption.getPreviousOperationType(),
                           consumption.getPreviousOperationEntityId(),
                           itemWorkflowId);
                  
                } catch (Exception e) {
                  log.error("Failed to return pieces to parent operation {} for multi-parent dispatch deletion: {}", 
                           consumption.getPreviousOperationEntityId(), e.getMessage());
                  throw new RuntimeException("Failed to return pieces to parent operation " + 
                                           consumption.getPreviousOperationEntityId() + ": " + e.getMessage(), e);
                }
              }
            }
            
            // Update dispatch workflow steps for multi-parent deletion - mark each dispatch step as deleted
            updateDispatchWorkflowStepsForMultiParentDeletion(
                itemWorkflowId, 
                dispatchBatch.getDispatchProcessedItemConsumptions(), 
                processedItemDispatchBatch.getId()
            );
            
          } else {
            // This was not the first operation - return pieces to single previous operation (legacy mode)
            Long previousOperationBatchId = itemWorkflowService.getPreviousOperationBatchId(
                itemWorkflowId, 
                WorkflowStep.OperationType.DISPATCH,
                processedItemDispatchBatch.getPreviousOperationProcessedItemId()
            );

            if (previousOperationBatchId != null) {
              itemWorkflowService.returnPiecesToSpecificPreviousOperation(
                  itemWorkflowId,
                  WorkflowStep.OperationType.DISPATCH,
                  previousOperationBatchId,
                  processedItemDispatchBatch.getTotalDispatchPiecesCount() - processedItemDispatchBatch.getAdditionalPiecesCount(),
                  processedItemDispatchBatch.getId()
              );

              log.info("Successfully returned {} pieces from dispatch back to previous operation {} in workflow {}",
                       processedItemDispatchBatch.getTotalDispatchPiecesCount() - processedItemDispatchBatch.getAdditionalPiecesCount(),
                       previousOperationBatchId,
                       itemWorkflowId);
            } else {
              log.error("Could not determine previous operation batch ID for dispatch batch processed item {}. " +
                       "Pieces may not be properly returned to previous operation.", processedItemDispatchBatch.getId());
              throw new IllegalStateException("Could not determine previous operation batch ID for dispatch batch processed item {}. " +
                                              "Pieces may not be properly returned to previous operation: " + processedItemDispatchBatch.getId());
            }
          }
        }
      } catch (Exception e) {
        log.warn("Failed to handle workflow pieces reversion for item {}: {}. This may indicate workflow data inconsistency.",
                 item.getId(), e.getMessage());
        throw e;
      }
    } else {
      log.warn("No workflow ID found for item {} during dispatch batch deletion. " +
               "This may be a legacy record before workflow integration.", item.getId());
      throw new IllegalStateException("No workflow ID found for item " + item.getId());
    }
  }

  /**
   * Update dispatch workflow steps for multi-parent deletion - mark each dispatch step as deleted
   * and update piece counts in the operation outcome data
   */
  private void updateDispatchWorkflowStepsForMultiParentDeletion(Long itemWorkflowId,
                                                                List<DispatchProcessedItemConsumption> consumptions,
                                                                Long processedItemDispatchBatchId) {
    if (consumptions == null || consumptions.isEmpty()) {
      log.warn("No consumption records provided for multi-parent dispatch workflow step deletion updates");
      return;
    }
    
    log.info("Updating {} dispatch workflow steps for multi-parent deletion of batch {}", 
             consumptions.size(), processedItemDispatchBatchId);
    
    for (DispatchProcessedItemConsumption consumption : consumptions) {
      if (!consumption.isDeleted()) {
        try {
          // Find the specific DISPATCH ItemWorkflowStep for this parent entity
          ItemWorkflowStep dispatchStep = itemWorkflowService.findItemWorkflowStepByParentEntityId(
              itemWorkflowId,
              consumption.getPreviousOperationEntityId(),
              WorkflowStep.OperationType.DISPATCH
          );

          if (dispatchStep != null) {
            // Update the operation outcome data to mark the batch as deleted
            markDispatchBatchAsDeletedInWorkflowStep(dispatchStep, consumption.getPreviousOperationEntityId(), consumption.getConsumedPiecesCount());
            
            log.info("Successfully updated dispatch workflow step for parent entity {} - marked batch {} as deleted and set piece counts to 0",
                     consumption.getPreviousOperationEntityId(), processedItemDispatchBatchId);
          } else {
            log.warn("Could not find DISPATCH workflow step for parent entity {} in workflow {} during multi-parent deletion",
                     consumption.getPreviousOperationEntityId(), itemWorkflowId);
          }
          
        } catch (Exception e) {
          log.error("Failed to update dispatch workflow step for parent entity {} during multi-parent dispatch deletion: {}", 
                   consumption.getPreviousOperationEntityId(), e.getMessage());
          throw new RuntimeException("Failed to update dispatch workflow step for parent entity " + 
                                   consumption.getPreviousOperationEntityId() + ": " + e.getMessage(), e);
        }
      }
    }
    
    log.info("Successfully updated all dispatch workflow steps for multi-parent deletion of batch {}", 
             processedItemDispatchBatchId);
  }

  /**
   * Mark a specific dispatch batch as deleted in the workflow step's operation outcome data
   * Sets the batch's deleted flag, deletedAt timestamp, and adjusts piece counts to 0
   */
  private void markDispatchBatchAsDeletedInWorkflowStep(ItemWorkflowStep dispatchStep, 
                                                       Long consumptionPreviousEntityId,
                                                       Integer consumedPiecesCount) {
    try {
      String existingOutcomeDataJson = dispatchStep.getOperationOutcomeData();
      if (existingOutcomeDataJson != null && !existingOutcomeDataJson.trim().isEmpty()) {
        OperationOutcomeData operationOutcomeData = objectMapper.readValue(existingOutcomeDataJson, OperationOutcomeData.class);
        LocalDateTime deletionTime = LocalDateTime.now();
        
        // Find and update the specific batch outcome
        if (operationOutcomeData.getBatchData() != null) {
          operationOutcomeData.getBatchData().stream()
              .filter(batch -> batch.getId().equals(consumptionPreviousEntityId))
              .forEach(batch -> {
                // Mark as deleted
                batch.setDeleted(true);
                batch.setDeletedAt(deletionTime);
                
                // Set piece counts to 0 as mentioned in the requirement
                batch.setInitialPiecesCount(0);
                batch.setPiecesAvailableForNext(0);
                
                log.info("Marked dispatch batch {} as deleted in workflow step {} - set initialPiecesCount and piecesAvailableForNext to 0",
                         consumptionPreviousEntityId, dispatchStep.getId());
              });
        }
        
        // Update the last modified timestamp
        operationOutcomeData.setOperationLastUpdatedAt(deletionTime);
        
        // Recalculate workflow step's aggregate piece counts based on all non-deleted batches
        if (operationOutcomeData.getBatchData() != null) {
          int totalInitialPieces = operationOutcomeData.getBatchData().stream()
              .filter(batch -> !batch.getDeleted())  // Only count non-deleted batches
              .mapToInt(batch -> batch.getInitialPiecesCount() != null ? batch.getInitialPiecesCount() : 0)
              .sum();
              
          int totalAvailableForNext = operationOutcomeData.getBatchData().stream()
              .filter(batch -> !batch.getDeleted())  // Only count non-deleted batches
              .mapToInt(batch -> batch.getPiecesAvailableForNext() != null ? batch.getPiecesAvailableForNext() : 0)
              .sum();
              
          // Update workflow step's aggregate piece counts
          dispatchStep.setInitialPiecesCount(totalInitialPieces);
          dispatchStep.setPiecesAvailableForNext(totalAvailableForNext);
          
          log.info("Recalculated workflow step {} piece counts after batch deletion - initialPiecesCount: {}, piecesAvailableForNext: {}",
                   dispatchStep.getId(), totalInitialPieces, totalAvailableForNext);
        }
        
        // Update the workflow step with the modified outcome data
        itemWorkflowService.updateWorkflowStepForSpecificStep(dispatchStep, operationOutcomeData);
        
        log.info("Successfully marked dispatch batch {} as deleted and updated piece counts in workflow step {}",
                 consumptionPreviousEntityId, dispatchStep.getId());
        
      } else {
        log.warn("No operation outcome data found for dispatch workflow step {} during deletion of batch {}",
                 dispatchStep.getId(), consumptionPreviousEntityId);
      }
      
    } catch (Exception e) {
      log.error("Failed to mark dispatch batch {} as deleted in workflow step {}: {}",
                consumptionPreviousEntityId, dispatchStep.getId(), e.getMessage());
      throw new RuntimeException("Failed to mark dispatch batch as deleted in workflow step: " + e.getMessage(), e);
    }
  }

  /**
   * Phase 3: Soft delete associated records and finalize deletion
   */
  private void softDeleteAssociatedRecordsAndFinalizeDeletion(DispatchBatch dispatchBatch, 
                                                             ProcessedItemDispatchBatch processedItemDispatchBatch, 
                                                             long dispatchBatchId) {
    LocalDateTime now = LocalDateTime.now();

    // Revert the inspection batch changes
    for (DispatchProcessedItemInspection dispatchProcessedItemInspection : dispatchBatch.getDispatchProcessedItemInspections()) {
      // Restore the original available dispatch pieces count
      ProcessedItemInspectionBatch processedItemInspectionBatch = dispatchProcessedItemInspection.getProcessedItemInspectionBatch();
      int dispatchedPiecesCount = dispatchProcessedItemInspection.getDispatchedPiecesCount() != null ?
          dispatchProcessedItemInspection.getDispatchedPiecesCount() : 0;
      processedItemInspectionBatch.setDispatchedPiecesCount(processedItemInspectionBatch.getDispatchedPiecesCount() - dispatchedPiecesCount);
      dispatchProcessedItemInspection.setDeleted(true);
      dispatchProcessedItemInspection.setDeletedAt(now);
      processedItemInspectionBatch.setItemStatus(ItemStatus.DISPATCH_DELETED_QUALITY);
    }

    // Soft delete dispatch heat consumption records
    if (processedItemDispatchBatch.getDispatchHeats() != null &&
        !processedItemDispatchBatch.getDispatchHeats().isEmpty()) {
      processedItemDispatchBatch.getDispatchHeats().forEach(dispatchHeat -> {
        dispatchHeat.setDeleted(true);
        dispatchHeat.setDeletedAt(now);
      });
    }

    // Soft delete dispatch processed item consumption records
    if (dispatchBatch.getDispatchProcessedItemConsumptions() != null &&
        !dispatchBatch.getDispatchProcessedItemConsumptions().isEmpty()) {
      log.info("Soft deleting {} consumption records for dispatch batch {}", 
               dispatchBatch.getDispatchProcessedItemConsumptions().size(), dispatchBatchId);
      
      dispatchBatch.getDispatchProcessedItemConsumptions().forEach(consumption -> {
        consumption.setDeleted(true);
        consumption.setDeletedAt(now);
        log.debug("Soft deleted consumption record for entity {} of type {}", 
                 consumption.getPreviousOperationEntityId(), consumption.getPreviousOperationType());
      });
    }

    // Mark dispatch packages as deleted
    if (dispatchBatch.getDispatchPackages() != null) {
      for (DispatchPackage dispatchPackage : dispatchBatch.getDispatchPackages()) {
        dispatchPackage.setDeleted(true);
        dispatchPackage.setDeletedAt(now);
      }
    }

    // Store the original batch number and modify the batch number for deletion
    dispatchBatch.setOriginalDispatchBatchNumber(dispatchBatch.getDispatchBatchNumber());
    dispatchBatch.setDispatchBatchNumber(
        dispatchBatch.getDispatchBatchNumber() + "_deleted_" + dispatchBatch.getId() + "_" + now.toEpochSecond(java.time.ZoneOffset.UTC)
    );

    // Also handle invoice number if it's unique
    if (dispatchBatch.getInvoiceNumber() != null && !dispatchBatch.getInvoiceNumber().isEmpty()) {
        dispatchBatch.setInvoiceNumber(
            dispatchBatch.getInvoiceNumber() + "_deleted_" + dispatchBatch.getId() + "_" + now.toEpochSecond(java.time.ZoneOffset.UTC)
        );
    }

    // Soft delete the DispatchBatch
    dispatchBatch.setDeleted(true);
    dispatchBatch.setDeletedAt(now);
    processedItemDispatchBatch.setDeleted(true);
    processedItemDispatchBatch.setDeletedAt(now);
    dispatchBatchRepository.save(dispatchBatch);

    log.info("Successfully deleted dispatch batch={}, original batch number={}",
             dispatchBatchId, dispatchBatch.getOriginalDispatchBatchNumber());
  }

  public DispatchBatch getDispatchBatchById(long id){
    Optional<DispatchBatch> dispatchBatchOptional = dispatchBatchRepository.findByIdAndDeletedFalse(id);
    if(dispatchBatchOptional.isEmpty()){
      log.error("Dispatch batch with id={} not found", id);
      throw new DispatchBatchNotFoundException("Dispatch batch with id=" + id + " not found");
    }
    return dispatchBatchOptional.get();
  }

  public List<DispatchStatisticsRepresentation> getDispatchStatisticsByMonthRange(
      long tenantId, int fromMonth, int fromYear, int toMonth, int toYear) {
    tenantService.validateTenantExists(tenantId);

    LocalDateTime startDate = LocalDateTime.of(fromYear, fromMonth, 1, 0, 0);
    LocalDateTime endDate = LocalDateTime.of(toYear, toMonth, 1, 23, 59, 59).plusMonths(1).minusNanos(1);

    if (startDate.isAfter(endDate)) {
      log.error("Start date {} is after end date {} for tenant {}", startDate, endDate, tenantId);
      throw new IllegalArgumentException("Start date cannot be after end date.");
    }

    List<DispatchBatch> dispatchedBatches = dispatchBatchRepository
        .findByTenantIdAndDeletedIsFalseAndDispatchBatchStatusAndDispatchedAtBetween(
            tenantId, DispatchBatch.DispatchBatchStatus.DISPATCHED, startDate, endDate);

    Map<YearMonth, Long> monthlyStats = dispatchedBatches.stream()
        .filter(batch -> batch.getProcessedItemDispatchBatch() != null &&
                         batch.getProcessedItemDispatchBatch().getTotalDispatchPiecesCount() != null)
        .collect(Collectors.groupingBy(
            batch -> YearMonth.from(batch.getDispatchedAt()),
            Collectors.summingLong(batch -> batch.getProcessedItemDispatchBatch().getTotalDispatchPiecesCount())
        ));

    return monthlyStats.entrySet().stream()
        .map(entry -> DispatchStatisticsRepresentation.builder()
            .year(entry.getKey().getYear())
            .month(entry.getKey().getMonthValue())
            .totalDispatchedPieces(entry.getValue())
            .build())
        .sorted((s1, s2) -> YearMonth.of(s1.getYear(), s1.getMonth())
                                      .compareTo(YearMonth.of(s2.getYear(), s2.getMonth())))
        .collect(Collectors.toList());
  }

  /**
   * Find all dispatch batches associated with a specific machining batch
   * @param machiningBatchId The ID of the machining batch
   * @return List of dispatch batch representations associated with the machining batch
   */
  public List<DispatchBatchRepresentation> getDispatchBatchesByMachiningBatchId(Long machiningBatchId) {
    log.info("Finding dispatch batches for machining batch ID: {}", machiningBatchId);
    
    List<DispatchBatch> dispatchBatches = dispatchBatchRepository.findByMachiningBatchId(machiningBatchId);
    
    return dispatchBatches.stream()
        .map(dispatchBatchAssembler::dissemble)
        .collect(Collectors.toList());
  }

  /**
   * Check if a dispatch batch number was previously used and deleted
   * 
   * @param dispatchBatchNumber The dispatch batch number to check
   * @param tenantId The tenant ID
   * @return True if the batch number was previously used and deleted
   */
  public boolean isDispatchBatchNumberPreviouslyUsed(String dispatchBatchNumber, Long tenantId) {
    return dispatchBatchRepository.existsByDispatchBatchNumberAndTenantIdAndOriginalDispatchBatchNumber(
        dispatchBatchNumber, tenantId);
  }

  /**
   * Search for dispatch batches by various criteria with pagination
   * @param tenantId The tenant ID
   * @param searchType The type of search (ITEM_NAME, FORGE_TRACEABILITY_NUMBER, DISPATCH_BATCH_NUMBER, or DISPATCH_BATCH_STATUS)
   * @param searchTerm The search term (substring matching for most search types, exact for status)
   * @param page The page number (0-based)
   * @param size The page size
   * @return Page of DispatchBatchRepresentation containing the search results
   */
  @Transactional(readOnly = true)
  public Page<DispatchBatchRepresentation> searchDispatchBatches(Long tenantId, String searchType, String searchTerm, int page, int size) {
    if (searchTerm == null || searchTerm.trim().isEmpty() || searchType == null || searchType.trim().isEmpty()) {
      Pageable pageable = PageRequest.of(page, size);
      return Page.empty(pageable);
    }

    tenantService.validateTenantExists(tenantId);
    Pageable pageable = PageRequest.of(page, size);
    Page<DispatchBatch> dispatchBatchPage;
    
    switch (searchType.toUpperCase()) {
      case "ITEM_NAME":
        dispatchBatchPage = dispatchBatchRepository.findDispatchBatchesByItemNameContainingIgnoreCase(tenantId, searchTerm.trim(), pageable);
        break;
      case "FORGE_TRACEABILITY_NUMBER":
        dispatchBatchPage = dispatchBatchRepository.findDispatchBatchesByForgeTraceabilityNumberContainingIgnoreCase(tenantId, searchTerm.trim(), pageable);
        break;
      case "DISPATCH_BATCH_NUMBER":
        dispatchBatchPage = dispatchBatchRepository.findDispatchBatchesByDispatchBatchNumberContainingIgnoreCase(tenantId, searchTerm.trim(), pageable);
        break;
      case "DISPATCH_BATCH_STATUS":
        try {
          DispatchBatch.DispatchBatchStatus status = DispatchBatch.DispatchBatchStatus.valueOf(searchTerm.trim().toUpperCase());
          dispatchBatchPage = dispatchBatchRepository.findDispatchBatchesByDispatchBatchStatus(tenantId, status, pageable);
        } catch (IllegalArgumentException e) {
          log.error("Invalid dispatch batch status: {}", searchTerm);
          throw new IllegalArgumentException("Invalid dispatch batch status: " + searchTerm + ". Valid statuses are: DISPATCH_IN_PROGRESS, READY_TO_DISPATCH, DISPATCHED");
        }
        break;
      default:
        log.error("Invalid search type: {}", searchType);
        throw new IllegalArgumentException("Invalid search type: " + searchType + ". Valid types are: ITEM_NAME, FORGE_TRACEABILITY_NUMBER, DISPATCH_BATCH_NUMBER, DISPATCH_BATCH_STATUS");
    }
    
    return dispatchBatchPage.map(dispatchBatchAssembler::dissemble);
  }
  
  /**
   * Retrieves dispatch batches by multiple processed item dispatch batch IDs and validates they belong to the tenant
   * @param processedItemDispatchBatchIds List of processed item dispatch batch IDs
   * @param tenantId The tenant ID for validation
   * @return List of DispatchBatchRepresentation (distinct dispatch batches)
   */
  public List<DispatchBatchRepresentation> getDispatchBatchesByProcessedItemDispatchBatchIds(List<Long> processedItemDispatchBatchIds, Long tenantId) {
    if (processedItemDispatchBatchIds == null || processedItemDispatchBatchIds.isEmpty()) {
      log.info("No processed item dispatch batch IDs provided, returning empty list");
      return Collections.emptyList();
    }

    log.info("Getting dispatch batches for {} processed item dispatch batch IDs for tenant {}", processedItemDispatchBatchIds.size(), tenantId);
    
    List<DispatchBatch> dispatchBatches = dispatchBatchRepository.findByProcessedItemDispatchBatchIdInAndDeletedFalse(processedItemDispatchBatchIds);
    
    // Use a Set to track processed dispatch batch IDs to avoid duplicates
    Set<Long> processedDispatchBatchIds = new HashSet<>();
    List<DispatchBatchRepresentation> validDispatchBatches = new ArrayList<>();
    List<Long> invalidProcessedItemDispatchBatchIds = new ArrayList<>();
    
    for (Long processedItemDispatchBatchId : processedItemDispatchBatchIds) {
      Optional<DispatchBatch> dispatchBatchOpt = dispatchBatches.stream()
          .filter(db -> db.getProcessedItemDispatchBatch() != null && 
                       db.getProcessedItemDispatchBatch().getId().equals(processedItemDispatchBatchId))
          .findFirst();
          
      if (dispatchBatchOpt.isPresent()) {
        DispatchBatch dispatchBatch = dispatchBatchOpt.get();
        if (Long.valueOf(dispatchBatch.getTenant().getId()).equals(tenantId)) {
          // Only add if we haven't already processed this dispatch batch
          if (!processedDispatchBatchIds.contains(dispatchBatch.getId())) {
            validDispatchBatches.add(dispatchBatchAssembler.dissemble(dispatchBatch));
            processedDispatchBatchIds.add(dispatchBatch.getId());
          }
        } else {
          log.warn("DispatchBatch for processedItemDispatchBatchId={} does not belong to tenant={}", processedItemDispatchBatchId, tenantId);
          invalidProcessedItemDispatchBatchIds.add(processedItemDispatchBatchId);
        }
      } else {
        log.warn("No dispatch batch found for processedItemDispatchBatchId={}", processedItemDispatchBatchId);
        invalidProcessedItemDispatchBatchIds.add(processedItemDispatchBatchId);
      }
    }
    
    if (!invalidProcessedItemDispatchBatchIds.isEmpty()) {
      log.warn("The following processed item dispatch batch IDs did not have valid dispatch batches for tenant {}: {}", 
               tenantId, invalidProcessedItemDispatchBatchIds);
    }
    
    log.info("Found {} distinct valid dispatch batches out of {} requested processed item dispatch batch IDs", validDispatchBatches.size(), processedItemDispatchBatchIds.size());
    return validDispatchBatches;
  }

  /**
   * Get dispatch batches that are ready for invoice generation (READY_TO_DISPATCH status)
   */
  @Transactional(readOnly = true)
  public Page<DispatchBatch> getReadyToDispatchBatches(Long tenantId, Pageable pageable) {
    log.info("Fetching READY_TO_DISPATCH batches for tenant: {}", tenantId);
    tenantService.validateTenantExists(tenantId);

    return dispatchBatchRepository.findDispatchBatchesByDispatchBatchStatus(
        tenantId, DispatchBatch.DispatchBatchStatus.READY_TO_DISPATCH, pageable);
  }

  /**
   * Get count of dispatch batches ready for invoice generation
   */
  @Transactional(readOnly = true)
  public long getReadyToDispatchBatchesCount(Long tenantId) {
    log.debug("Counting READY_TO_DISPATCH batches for tenant: {}", tenantId);
    tenantService.validateTenantExists(tenantId);

    return dispatchBatchRepository.findDispatchBatchesByDispatchBatchStatus(
        tenantId, DispatchBatch.DispatchBatchStatus.READY_TO_DISPATCH, Pageable.unpaged()).getTotalElements();
  }

  /**
   * Update dispatch batch status to DISPATCH_APPROVED after invoice generation
   */
  @Transactional
  public DispatchBatch updateStatusToDispatchApproved(Long dispatchBatchId) {
    log.info("Updating dispatch batch {} status to DISPATCH_APPROVED", dispatchBatchId);

    DispatchBatch dispatchBatch = getDispatchBatchById(dispatchBatchId);

    if (dispatchBatch.getDispatchBatchStatus() != DispatchBatch.DispatchBatchStatus.READY_TO_DISPATCH) {
      throw new IllegalStateException("Dispatch batch must be in READY_TO_DISPATCH status to approve. Current status: " +
                                      dispatchBatch.getDispatchBatchStatus());
    }

    dispatchBatch.setDispatchBatchStatus(DispatchBatch.DispatchBatchStatus.DISPATCH_APPROVED);
    DispatchBatch savedBatch = dispatchBatchRepository.save(dispatchBatch);

    log.info("Successfully updated dispatch batch {} to DISPATCH_APPROVED status", dispatchBatchId);
    return savedBatch;
  }

  /**
   * Update multiple dispatch batches to DISPATCH_APPROVED status (for multi-batch invoices)
   */
  @Transactional
  public void updateMultipleBatchesToDispatchApproved(List<Long> dispatchBatchIds) {
    log.info("Updating {} dispatch batches to DISPATCH_APPROVED status", dispatchBatchIds.size());

    List<DispatchBatch> updatedBatches = new ArrayList<>();

    for (Long batchId : dispatchBatchIds) {
      DispatchBatch updatedBatch = updateStatusToDispatchApproved(batchId);
      updatedBatches.add(updatedBatch);
    }

    log.info("Successfully updated {} dispatch batches to DISPATCH_APPROVED status", updatedBatches.size());
  }

  /**
   * Validate that dispatch batch can be marked as dispatched (must be DISPATCH_APPROVED)
   */
  public void validateDispatchBatchCanBeDispatched(Long dispatchBatchId) {
    DispatchBatch dispatchBatch = getDispatchBatchById(dispatchBatchId);

    if (dispatchBatch.getDispatchBatchStatus() != DispatchBatch.DispatchBatchStatus.DISPATCH_APPROVED) {
      throw new IllegalStateException("Dispatch batch must be in DISPATCH_APPROVED status to be dispatched. " +
                                      "Current status: " + dispatchBatch.getDispatchBatchStatus() +
                                      ". Please generate and approve invoice first.");
    }
  }

  /**
   * Revert dispatch batch status back to READY_TO_DISPATCH (for invoice deletion)
   */
  @Transactional
  public DispatchBatch revertStatusToReadyToDispatch(Long dispatchBatchId) {
    log.info("Reverting dispatch batch {} status back to READY_TO_DISPATCH", dispatchBatchId);

    DispatchBatch dispatchBatch = getDispatchBatchById(dispatchBatchId);

    if (dispatchBatch.getDispatchBatchStatus() == DispatchBatch.DispatchBatchStatus.DISPATCHED) {
      throw new IllegalStateException("Cannot revert status of dispatched batch");
    }

    dispatchBatch.setDispatchBatchStatus(DispatchBatch.DispatchBatchStatus.READY_TO_DISPATCH);
    DispatchBatch savedBatch = dispatchBatchRepository.save(dispatchBatch);

    log.info("Successfully reverted dispatch batch {} to READY_TO_DISPATCH status", dispatchBatchId);
    return savedBatch;
  }
}
