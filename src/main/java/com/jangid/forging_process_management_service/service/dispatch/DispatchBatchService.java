package com.jangid.forging_process_management_service.service.dispatch;

import com.jangid.forging_process_management_service.assemblers.dispatch.DispatchBatchAssembler;
import com.jangid.forging_process_management_service.dto.workflow.OperationOutcomeData;
import com.jangid.forging_process_management_service.entities.PackagingType;
import com.jangid.forging_process_management_service.entities.dispatch.DispatchBatch;
import com.jangid.forging_process_management_service.entities.dispatch.DispatchPackage;
import com.jangid.forging_process_management_service.entities.dispatch.DispatchProcessedItemInspection;
import com.jangid.forging_process_management_service.entities.dispatch.ProcessedItemDispatchBatch;
import com.jangid.forging_process_management_service.entities.inventory.Heat;
import com.jangid.forging_process_management_service.entities.product.ItemStatus;
import com.jangid.forging_process_management_service.entities.product.Item;
import com.jangid.forging_process_management_service.entities.quality.ProcessedItemInspectionBatch;
import com.jangid.forging_process_management_service.entities.workflow.ItemWorkflow;
import com.jangid.forging_process_management_service.entities.workflow.ItemWorkflowStep;
import com.jangid.forging_process_management_service.entities.workflow.WorkflowStep;
import com.jangid.forging_process_management_service.entitiesRepresentation.dispatch.DispatchBatchListRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.dispatch.DispatchBatchRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.dispatch.DispatchPackageRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.dispatch.DispatchStatisticsRepresentation;
import com.jangid.forging_process_management_service.exception.dispatch.DispatchBatchNotFoundException;
import com.jangid.forging_process_management_service.repositories.dispatch.DispatchBatchRepository;
import com.jangid.forging_process_management_service.repositories.workflow.ItemWorkflowRepository;
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
  private final TenantService tenantService;
  private final BuyerService buyerService;
  private final ItemWorkflowService itemWorkflowService;
  private final ItemWorkflowRepository itemWorkflowRepository;
  private final DispatchBatchAssembler dispatchBatchAssembler;
  private final ObjectMapper objectMapper;
  private final RawMaterialHeatService rawMaterialHeatService;

  @Autowired
  public DispatchBatchService(
      DispatchBatchRepository dispatchBatchRepository,
      TenantService tenantService,
      BuyerService buyerService,
      ItemWorkflowService itemWorkflowService,
      ItemWorkflowRepository itemWorkflowRepository,
      DispatchBatchAssembler dispatchBatchAssembler,
      ObjectMapper objectMapper,
      RawMaterialHeatService rawMaterialHeatService) {
    this.dispatchBatchRepository = dispatchBatchRepository;
    this.tenantService = tenantService;
    this.buyerService = buyerService;
    this.itemWorkflowService = itemWorkflowService;
    this.itemWorkflowRepository = itemWorkflowRepository;
    this.dispatchBatchAssembler = dispatchBatchAssembler;
    this.objectMapper = objectMapper;
    this.rawMaterialHeatService = rawMaterialHeatService;
  }

  @Transactional
  public DispatchBatchRepresentation createDispatchBatch(long tenantId, DispatchBatchRepresentation representation) {
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

    DispatchBatch createdDispatchBatch = dispatchBatchRepository.save(dispatchBatch);
    
    // Handle workflow integration and pieces consumption
    handleWorkflowIntegration(representation, createdDispatchBatch.getProcessedItemDispatchBatch());
    
    return dispatchBatchAssembler.dissemble(createdDispatchBatch);
  }

  /**
   * Handle workflow integration including pieces consumption and workflow step updates
   */
  private void handleWorkflowIntegration(DispatchBatchRepresentation representation, 
                                        ProcessedItemDispatchBatch processedItemDispatchBatch) {
    try {
      // Extract item directly from ProcessedItemDispatchBatch
      Item item = processedItemDispatchBatch.getItem();
      
      // Get workflow fields from the ProcessedItemDispatchBatch entity
      String workflowIdentifier = processedItemDispatchBatch.getWorkflowIdentifier();
      Long itemWorkflowId = processedItemDispatchBatch.getItemWorkflowId();
      
      // Use the generic workflow handling method
      ItemWorkflow workflow = itemWorkflowService.handleWorkflowForOperation(
          item,
          WorkflowStep.OperationType.DISPATCH,
          workflowIdentifier,
          itemWorkflowId
      );
      
      // Update the dispatch batch with workflow ID for future reference
      log.info("Successfully integrated dispatch batch with workflow. Workflow ID: {}, Workflow Identifier: {}", 
               workflow.getId(), workflow.getWorkflowIdentifier());

      if (processedItemDispatchBatch.getItemWorkflowId() == null) {
        processedItemDispatchBatch.setItemWorkflowId(workflow.getId());
        dispatchBatchRepository.save(processedItemDispatchBatch.getDispatchBatch());
      }
      
      // Check if dispatch is the first operation in the workflow template
      boolean isFirstOperation = WorkflowStep.OperationType.DISPATCH.equals(
          workflow.getWorkflowTemplate().getFirstStep().getOperationType());
      
      if (isFirstOperation) {
        // This is the first operation in workflow - consume inventory from Heat (if applicable)
        log.info("Dispatch is the first operation in workflow - consuming inventory from heat");
        handleHeatConsumptionForFirstOperation(representation, processedItemDispatchBatch, workflow);
      } else {
        // This is not the first operation - consume pieces from previous operation
        handlePiecesConsumptionFromPreviousOperation(processedItemDispatchBatch, workflow);
      }
      
      // Update workflow step with batch data
      updateWorkflowStepWithBatchOutcome(processedItemDispatchBatch, workflow.getId());
      
      // Update relatedEntityIds for DISPATCH operation step with ProcessedItemDispatchBatch.id
      itemWorkflowService.updateRelatedEntityIds(workflow.getId(), WorkflowStep.OperationType.DISPATCH, processedItemDispatchBatch.getId());
      
    } catch (Exception e) {
      log.error("Failed to integrate dispatch batch with workflow for item {}: {}", 
                processedItemDispatchBatch.getItem().getId(), e.getMessage());
      // Re-throw to fail the operation since workflow integration is critical
      throw new RuntimeException("Failed to integrate with workflow system: " + e.getMessage(), e);
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
        
        // Validate timing - heat should be created before the dispatch creation time
        if (heat.getCreatedAt().compareTo(dispatchCreatedAt) > 0) {
          log.error("The provided dispatch created at time={} is before to heat={} created at time={} !", 
                    dispatchCreatedAt, heat.getHeatNumber(), heat.getCreatedAt());
          throw new RuntimeException("The provided dispatch created at time=" + dispatchCreatedAt + 
                                   " is before to heat=" + heat.getHeatNumber() + 
                                   " created at time=" + heat.getCreatedAt() + " !");
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
   */
  private void handlePiecesConsumptionFromPreviousOperation(ProcessedItemDispatchBatch processedItemDispatchBatch, 
                                                           ItemWorkflow workflow) {
    // For non-first operations, consume pieces from the previous operation
    Long previousOperationProcessedItemId = processedItemDispatchBatch.getPreviousOperationProcessedItemId();
    
    if (previousOperationProcessedItemId != null) {
      int piecesToConsume = processedItemDispatchBatch.getTotalDispatchPiecesCount();
      
      // Update available pieces in the previous operation
      itemWorkflowService.updateAvailablePiecesInSpecificPreviousOperation(
          workflow.getId(), 
          WorkflowStep.OperationType.DISPATCH, 
          previousOperationProcessedItemId, 
          piecesToConsume
      );
      
      log.info("Consumed {} pieces from previous operation entity {} for dispatch batch {}", 
               piecesToConsume, previousOperationProcessedItemId, processedItemDispatchBatch.getId());
    }
  }

  /**
   * Updates workflow step with batch outcome data
   */
  private void updateWorkflowStepWithBatchOutcome(ProcessedItemDispatchBatch processedItemDispatchBatch, 
                                                 Long workflowId) {
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
    List<OperationOutcomeData.BatchOutcome> accumulatedBatchData = getAccumulatedBatchData(workflowId);
    
    // Add the current batch outcome to the accumulated list
    accumulatedBatchData.add(dispatchBatchOutcome);

    // Update workflow step with accumulated batch data
    itemWorkflowService.updateWorkflowStepForOperation(
        workflowId,
        WorkflowStep.OperationType.DISPATCH,
        OperationOutcomeData.forDispatchOperation(accumulatedBatchData, LocalDateTime.now()));
  }

  /**
   * Get accumulated batch data from existing workflow step
   */
  private List<OperationOutcomeData.BatchOutcome> getAccumulatedBatchData(Long workflowId) {
    List<OperationOutcomeData.BatchOutcome> accumulatedBatchData = new ArrayList<>();
    
    try {
      ItemWorkflowStep existingDispatchStep = itemWorkflowService.getWorkflowStepByOperation(
          workflowId, WorkflowStep.OperationType.DISPATCH);
      
      if (existingDispatchStep != null && 
          existingDispatchStep.getOperationOutcomeData() != null && 
          !existingDispatchStep.getOperationOutcomeData().trim().isEmpty()) {
        
        // Parse existing outcome data and get existing batch data
        OperationOutcomeData existingOutcomeData = objectMapper.readValue(
            existingDispatchStep.getOperationOutcomeData(), OperationOutcomeData.class);
        
        if (existingOutcomeData.getBatchData() != null) {
          accumulatedBatchData.addAll(existingOutcomeData.getBatchData());
        }
      }
    } catch (Exception e) {
      log.warn("Failed to parse existing workflow outcome data for dispatch step in workflow {}: {}", 
               workflowId, e.getMessage());
    }
    
    return accumulatedBatchData;
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
      
      // Find the DISPATCH operation step using the operationType column for efficient filtering
      ItemWorkflowStep dispatchStep = itemWorkflow.getItemWorkflowSteps().stream()
          .filter(step -> step.getOperationType() == WorkflowStep.OperationType.DISPATCH)
          .findFirst()
          .orElse(null);
      
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
                        dispatchStep.setPiecesAvailableForNext(dispatchStep.getPiecesAvailableForNext() - totalDispatchPiecesCount);
                        log.info("Deducted {} dispatched pieces from piecesAvailableForNext for batch {}. " +
                                "Previous: {}, New: {}", 
                                totalDispatchPiecesCount, batch.getId(), currentAvailableForNext, newAvailableForNext);
                      } else {
                        log.warn("piecesAvailableForNext is null for batch {} in dispatch completion", batch.getId());
                      }

                    }
                  });
            }
            
            dispatchStep.setOperationOutcomeData(objectMapper.writeValueAsString(operationOutcomeData));

          } else {
            log.warn("No operation outcome data found for DISPATCH step in workflow for itemWorkflowId: {}", itemWorkflowId);
          }
        } catch (Exception e) {
          log.error("Failed to update operation outcome data for DISPATCH step: {}", e.getMessage());
          throw new RuntimeException("Failed to update operation outcome data: " + e.getMessage(), e);
        }
      } else {
        log.warn("No DISPATCH step found in workflow for itemWorkflowId: {}", itemWorkflowId);
      }
      
      // Save the updated workflow
      itemWorkflowRepository.save(itemWorkflow);
      
    } catch (Exception e) {
      log.error("Failed to update ItemWorkflowStep entities for dispatch completion on dispatch batch ID: {} - {}", dispatchBatch.getId(), e.getMessage());
      // Re-throw the exception to fail the dispatch completion since workflow integration is now mandatory
      throw new RuntimeException("Failed to update workflow step for dispatch completion: " + e.getMessage(), e);
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

  @Transactional
  public DispatchBatchRepresentation deleteDispatchBatch(long tenantId, long dispatchBatchId) {
    // 1. Validate tenant exists
    tenantService.validateTenantExists(tenantId);

    // 2. Validate dispatch batch exists
    DispatchBatch dispatchBatch = getDispatchBatchById(dispatchBatchId);

    // 3. Validate dispatch batch status is DISPATCHED
    if (dispatchBatch.getDispatchBatchStatus() != DispatchBatch.DispatchBatchStatus.DISPATCHED) {
      log.error("Cannot delete dispatch batch={} as it is not in DISPATCHED status!", dispatchBatchId);
      throw new IllegalStateException("This dispatch batch cannot be deleted as it is not in the DISPATCHED status.");
    }

    // 4. Handle inventory reversal based on whether this was the first operation or not
    LocalDateTime now = LocalDateTime.now();
    ProcessedItemDispatchBatch processedItemDispatchBatch = dispatchBatch.getProcessedItemDispatchBatch();
    Item item = processedItemDispatchBatch.getItem();

    // Get the item workflow ID from the ProcessedItemDispatchBatch entity
    Long itemWorkflowId = processedItemDispatchBatch.getItemWorkflowId();

    if (itemWorkflowId != null) {
      try {
        // Use workflow-based validation: check if all entries in next operation are marked deleted
        // Note: Dispatch is typically the last operation, so this check may not be necessary
        // but we'll include it for consistency with the architecture
        boolean canDeleteDispatch = itemWorkflowService.areAllNextOperationBatchesDeleted(itemWorkflowId, WorkflowStep.OperationType.DISPATCH);

        if (!canDeleteDispatch) {
          log.error("Cannot delete dispatch id={} as the next operation has active (non-deleted) batches", dispatchBatchId);
          throw new IllegalStateException("This dispatch cannot be deleted as the next operation has active batch entries.");
        }

        log.info("Dispatch id={} is eligible for deletion - all next operation batches are deleted", dispatchBatchId);

        // Get the workflow to check if dispatch was the first operation
        ItemWorkflow workflow = itemWorkflowService.getItemWorkflowById(itemWorkflowId);
        boolean wasFirstOperation = WorkflowStep.OperationType.DISPATCH.equals(
            workflow.getWorkflowTemplate().getFirstStep().getOperationType());

        if (wasFirstOperation) {
          // This was the first operation - heat quantities will be returned to heat inventory
          log.info("Dispatch batch {} was first operation for item {}, heat inventory will be reverted",
                   dispatchBatchId, item.getId());

          // Update workflow step to mark dispatch batch as deleted and adjust piece counts
          Integer totalDispatchPiecesCount = processedItemDispatchBatch.getTotalDispatchPiecesCount();
          if (totalDispatchPiecesCount != null && totalDispatchPiecesCount > 0) {
            try {
              itemWorkflowService.markOperationAsDeletedAndUpdatePieceCounts(
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
          // This was not the first operation - return pieces to previous operation
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
                 processedItemDispatchBatch.getTotalDispatchPiecesCount(),
                 processedItemDispatchBatch.getId()
             );

             log.info("Successfully returned {} pieces from dispatch back to previous operation {} in workflow {}",
                      processedItemDispatchBatch.getTotalDispatchPiecesCount(),
                      previousOperationBatchId,
                      itemWorkflowId);
          } else {
            log.error("Could not determine previous operation batch ID for dispatch batch processed item {}. " +
                     "Pieces may not be properly returned to previous operation.", processedItemDispatchBatch.getId());
            throw new IllegalStateException("Could not determine previous operation batch ID for dispatch batch processed item {}. " +
                                            "Pieces may not be properly returned to previous operation: " + processedItemDispatchBatch.getId());
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

    // 5. Revert the inspection batch changes
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

    // 6. Soft delete dispatch heat consumption records
    if (processedItemDispatchBatch.getDispatchHeats() != null &&
        !processedItemDispatchBatch.getDispatchHeats().isEmpty()) {
      processedItemDispatchBatch.getDispatchHeats().forEach(dispatchHeat -> {
        dispatchHeat.setDeleted(true);
        dispatchHeat.setDeletedAt(now);
      });
    }

    // 7. Mark dispatch packages as deleted
    if (dispatchBatch.getDispatchPackages() != null) {
      for (DispatchPackage dispatchPackage : dispatchBatch.getDispatchPackages()) {
        dispatchPackage.setDeleted(true);
        dispatchPackage.setDeletedAt(now);
      }
    }

    // 8. Store the original batch number and modify the batch number for deletion
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

    // 9. Soft delete the DispatchBatch
    dispatchBatch.setDeleted(true);
    dispatchBatch.setDeletedAt(now);
    processedItemDispatchBatch.setDeleted(true);
    processedItemDispatchBatch.setDeletedAt(now);
    DispatchBatch deletedDispatchBatch = dispatchBatchRepository.save(dispatchBatch);

    log.info("Successfully deleted dispatch batch={}, original batch number={}",
             dispatchBatchId, dispatchBatch.getOriginalDispatchBatchNumber());
    return dispatchBatchAssembler.dissemble(deletedDispatchBatch);
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
}
