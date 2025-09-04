package com.jangid.forging_process_management_service.service.quality;

import com.jangid.forging_process_management_service.assemblers.quality.InspectionBatchAssembler;
import com.jangid.forging_process_management_service.dto.workflow.OperationOutcomeData;
import com.jangid.forging_process_management_service.dto.workflow.WorkflowOperationContext;
import com.jangid.forging_process_management_service.entities.Tenant;
import com.jangid.forging_process_management_service.entities.inventory.Heat;
import com.jangid.forging_process_management_service.entities.machining.DailyMachiningBatch;
import com.jangid.forging_process_management_service.entities.machining.MachiningBatch;
import com.jangid.forging_process_management_service.entities.machining.ProcessedItemMachiningBatch;
import com.jangid.forging_process_management_service.entities.product.ItemStatus;
import com.jangid.forging_process_management_service.entities.product.Item;
import com.jangid.forging_process_management_service.entities.quality.DailyMachiningBatchInspectionDistribution;
import com.jangid.forging_process_management_service.entities.quality.InspectionBatch;
import com.jangid.forging_process_management_service.entities.quality.ProcessedItemInspectionBatch;
import com.jangid.forging_process_management_service.entities.workflow.ItemWorkflow;
import com.jangid.forging_process_management_service.entities.workflow.ItemWorkflowStep;
import com.jangid.forging_process_management_service.entities.workflow.WorkflowStep;
import com.jangid.forging_process_management_service.entitiesRepresentation.quality.DailyMachiningBatchInspectionDistributionRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.quality.InspectionBatchListRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.quality.InspectionBatchRepresentation;
import com.jangid.forging_process_management_service.exception.quality.InspectionBatchNotFoundException;
import com.jangid.forging_process_management_service.repositories.dispatch.DispatchProcessedItemInspectionRepository;
import com.jangid.forging_process_management_service.repositories.quality.InspectionBatchRepository;
import com.jangid.forging_process_management_service.service.TenantService;
import com.jangid.forging_process_management_service.service.inventory.RawMaterialHeatService;
import com.jangid.forging_process_management_service.service.machining.DailyMachiningBatchService;
import com.jangid.forging_process_management_service.service.machining.ProcessedItemMachiningBatchService;
import com.jangid.forging_process_management_service.service.workflow.ItemWorkflowService;
import com.jangid.forging_process_management_service.utils.ConvertorUtils;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class InspectionBatchService {

  @Autowired
  private InspectionBatchRepository inspectionBatchRepository;

  @Autowired
  private DispatchProcessedItemInspectionRepository dispatchProcessedItemInspectionRepository;

  @Autowired
  private TenantService tenantService;

  @Autowired
  private ProcessedItemMachiningBatchService processedItemMachiningBatchService;

  @Autowired
  private DailyMachiningBatchService dailyMachiningBatchService;

  @Autowired
  private InspectionBatchAssembler inspectionBatchAssembler;

  @Autowired
  private ItemWorkflowService itemWorkflowService;

  @Autowired
  private RawMaterialHeatService rawMaterialHeatService;
  @Autowired
  private ProcessedItemInspectionBatchService processedItemInspectionBatchService;

  @Transactional(rollbackFor = Exception.class)
  public InspectionBatchRepresentation createInspectionBatch(long tenantId, InspectionBatchRepresentation inspectionBatchRepresentation) {
    log.info("Starting inspection batch creation transaction for tenant: {}, batch: {}", 
             tenantId, inspectionBatchRepresentation.getInspectionBatchNumber());
    
    InspectionBatch createdInspectionBatch = null;
    try {
      // Validate tenant and batch number uniqueness
      validateTenantAndBatchNumber(tenantId, inspectionBatchRepresentation);

      // Assemble and validate basic inspection batch data
      InspectionBatch inspectionBatch = assembleAndValidateInspectionBatch(inspectionBatchRepresentation);
      ProcessedItemInspectionBatch inspectionBatchDetails = inspectionBatch.getProcessedItemInspectionBatch();

      // Determine previous operation type and get machining batch if applicable
      ProcessedItemMachiningBatch machiningBatch = determineParentOperationAndGetMachiningBatch(inspectionBatchRepresentation);

      // Apply conditional validations based on previous operation type
      applyConditionalValidations(machiningBatch, inspectionBatchDetails, inspectionBatchRepresentation);

      // Complete inspection batch setup
      completeInspectionBatchSetup(inspectionBatchDetails);

      // Link entities and persist
      linkInspectionBatchEntities(inspectionBatch, tenantId, inspectionBatchDetails, machiningBatch);
      persistGaugeInspectionReports(inspectionBatchDetails);

      // Save entities - this generates the required ID for workflow integration
      createdInspectionBatch = saveInspectionBatchEntities(machiningBatch, inspectionBatch);
      log.info("Successfully persisted inspection batch with ID: {}", createdInspectionBatch.getId());

      // Handle workflow integration - if this fails, entire transaction will rollback
      handleWorkflowIntegration(inspectionBatchRepresentation, createdInspectionBatch.getProcessedItemInspectionBatch());
      
      log.info("Successfully completed inspection batch creation transaction for ID: {}", createdInspectionBatch.getId());
      return inspectionBatchAssembler.dissemble(createdInspectionBatch);
      
    } catch (Exception e) {
      log.error("Inspection batch creation transaction failed for tenant: {}, batch: {}. " +
                "All changes will be rolled back. Error: {}", 
                tenantId, inspectionBatchRepresentation.getInspectionBatchNumber(), e.getMessage());
      
      if (createdInspectionBatch != null) {
        log.error("Inspection batch with ID {} was persisted but workflow integration failed. " +
                  "Transaction rollback will restore database consistency.", createdInspectionBatch.getId());
      }
      
      // Re-throw to ensure transaction rollback
      throw e;
    }
  }

  /**
   * Validates tenant existence and batch number uniqueness
   */
  private void validateTenantAndBatchNumber(long tenantId, InspectionBatchRepresentation inspectionBatchRepresentation) {
    // Validate tenant existence
    tenantService.validateTenantExists(tenantId);

    boolean exists = isInspectionBatchNumberForTenantExists(inspectionBatchRepresentation.getInspectionBatchNumber(), tenantId);
    if (exists) {
      log.error("The provided inspectionBatch number={} already exists",
                inspectionBatchRepresentation.getInspectionBatchNumber());
      throw new IllegalStateException("The provided inspectionBatch number=" + 
                                     inspectionBatchRepresentation.getInspectionBatchNumber() + 
                                     " already exists");
    }
    
    // Check if this batch number was previously used and deleted
    if (isInspectionBatchNumberPreviouslyUsed(inspectionBatchRepresentation.getInspectionBatchNumber(), tenantId)) {
      log.warn("Inspection Batch with batch number: {} was previously used and deleted for tenant: {}", 
               inspectionBatchRepresentation.getInspectionBatchNumber(), tenantId);
    }
  }

  /**
   * Assembles and validates basic inspection batch data
   */
  private InspectionBatch assembleAndValidateInspectionBatch(InspectionBatchRepresentation inspectionBatchRepresentation) {
    InspectionBatch inspectionBatch = inspectionBatchAssembler.createAssemble(inspectionBatchRepresentation);
    validateBatchTimeRange(inspectionBatchRepresentation);
    
    ProcessedItemInspectionBatch inspectionBatchDetails = inspectionBatch.getProcessedItemInspectionBatch();
    inspectionBatchDetails.updatePieceCounts();
    
    return inspectionBatch;
  }

  /**
   * Determines the parent operation type and retrieves machining batch if applicable
   */
  private ProcessedItemMachiningBatch determineParentOperationAndGetMachiningBatch(InspectionBatchRepresentation inspectionBatchRepresentation) {
    // Get workflow information to determine parent operation
    String workflowIdentifier = inspectionBatchRepresentation.getProcessedItemInspectionBatch().getWorkflowIdentifier();
    Long itemWorkflowId = inspectionBatchRepresentation.getProcessedItemInspectionBatch().getItemWorkflowId();
    
    if (itemWorkflowId != null) {
      return getMachiningBatchFromParentOperation(itemWorkflowId, inspectionBatchRepresentation);
    } else if (workflowIdentifier != null) {
      log.info("This is a first operation (workflowIdentifier: {}) - no previous operation validations needed", workflowIdentifier);
    }
    
    return null;
  }

  /**
   * Gets machining batch from previous operation if it's a MACHINING operation
   */
  private ProcessedItemMachiningBatch getMachiningBatchFromParentOperation(Long itemWorkflowId, InspectionBatchRepresentation inspectionBatchRepresentation) {
    try {
      ItemWorkflowStep parentOperationStep = itemWorkflowService.findImmediateParentStepByEntityId(itemWorkflowId, WorkflowStep.OperationType.QUALITY, inspectionBatchRepresentation.getProcessedItemInspectionBatch().getPreviousOperationProcessedItemId());
      if (parentOperationStep != null &&
          WorkflowStep.OperationType.MACHINING.equals(parentOperationStep.getOperationType())) {
        return extractMachiningBatchFromWorkflowStep(parentOperationStep);
      }
    } catch (Exception e) {
      log.warn("Failed to determine previous operation type for workflow {}: {}", itemWorkflowId, e.getMessage());
    }
    
    return null;
  }

  /**
   * Extracts machining batch from workflow step using relatedEntityIds
   */
  private ProcessedItemMachiningBatch extractMachiningBatchFromWorkflowStep(ItemWorkflowStep previousOperationStep) {
    // Get machining batch details using relatedEntityIds
    if (previousOperationStep.getRelatedEntityIds() != null && 
        !previousOperationStep.getRelatedEntityIds().isEmpty()) {
      // For MACHINING operation, relatedEntityIds always contain single ID
      Long processedItemMachiningBatchId = previousOperationStep.getRelatedEntityIds().get(0);
      return processedItemMachiningBatchService.getProcessedItemMachiningBatchById(processedItemMachiningBatchId);
    }
    
    return null;
  }

  /**
   * Applies conditional validations based on whether previous operation is MACHINING
   */
  private void applyConditionalValidations(ProcessedItemMachiningBatch machiningBatch, 
                                         ProcessedItemInspectionBatch inspectionBatchDetails,
                                         InspectionBatchRepresentation inspectionBatchRepresentation) {
    if (machiningBatch != null) {
      applyMachiningSpecificValidations(machiningBatch, inspectionBatchDetails, inspectionBatchRepresentation);
    } else {
      log.info("Previous operation is not MACHINING or no machining batch found - skipping MACHINING-specific validations");
    }
  }

  /**
   * Applies MACHINING-specific validations when previous operation is MACHINING
   */
  private void applyMachiningSpecificValidations(ProcessedItemMachiningBatch machiningBatch, 
                                               ProcessedItemInspectionBatch inspectionBatchDetails,
                                               InspectionBatchRepresentation inspectionBatchRepresentation) {
    log.info("Previous operation is MACHINING - applying MACHINING-specific validations");
    
    validateMachiningBatchStartTime(machiningBatch, inspectionBatchRepresentation);

    // Validate and distribute inspection results among daily machining batches
    if (hasDailyMachiningBatchDistribution(inspectionBatchRepresentation)) {
      validateAndDistributeInspectionResults(machiningBatch, inspectionBatchDetails, 
                                            inspectionBatchRepresentation.getProcessedItemInspectionBatch().getDailyMachiningBatchInspectionDistribution());
    }
  }

  /**
   * Checks if daily machining batch distribution is provided
   */
  private boolean hasDailyMachiningBatchDistribution(InspectionBatchRepresentation inspectionBatchRepresentation) {
    return inspectionBatchRepresentation.getProcessedItemInspectionBatch().getDailyMachiningBatchInspectionDistribution() != null &&
           !inspectionBatchRepresentation.getProcessedItemInspectionBatch().getDailyMachiningBatchInspectionDistribution().isEmpty();
  }

  /**
   * Completes the inspection batch setup with common validations
   */
  private void completeInspectionBatchSetup(ProcessedItemInspectionBatch inspectionBatchDetails) {
    inspectionBatchDetails.setItemStatus(ItemStatus.QUALITY_COMPLETED);
    inspectionBatchDetails.setCreatedAt(LocalDateTime.now());
    validateGaugeInspectionReportCounts(inspectionBatchDetails);
  }

  /**
   * Saves inspection batch entities conditionally and returns the created inspection batch
   */
  private InspectionBatch saveInspectionBatchEntities(ProcessedItemMachiningBatch machiningBatch, InspectionBatch inspectionBatch) {
    // Persist updated machining batch only if it exists (i.e., previous operation was MACHINING)
    if (machiningBatch != null) {
      processedItemMachiningBatchService.save(machiningBatch);
    }

    // Save the inspection batch and return it
    return inspectionBatchRepository.save(inspectionBatch);
  }

  private void validateBatchTimeRange(InspectionBatchRepresentation inspectionBatchRepresentation) {
    LocalDateTime startAt = ConvertorUtils.convertStringToLocalDateTime(inspectionBatchRepresentation.getStartAt());
    LocalDateTime endAt = ConvertorUtils.convertStringToLocalDateTime(inspectionBatchRepresentation.getEndAt());

    if (startAt.isAfter(endAt) || startAt.isEqual(endAt)) {
      log.error("The inspectionBatch end time is before or equal to start time!");
      throw new RuntimeException("The inspectionBatch end time is before or equal to start time!");
    }
  }


  private void validateMachiningBatchStartTime(ProcessedItemMachiningBatch machiningBatch, InspectionBatchRepresentation batchRepresentation) {
    LocalDateTime batchStartAt = ConvertorUtils.convertStringToLocalDateTime(batchRepresentation.getStartAt());
    LocalDateTime machiningBatchStartAt = machiningBatch.getMachiningBatch().getStartAt();

    if (!batchStartAt.isAfter(machiningBatchStartAt)) {
      log.error("The inspectionBatch start time is before or equal to start time of the selected machining batch={} for inspection!",
                machiningBatch.getMachiningBatch().getMachiningBatchNumber());
      throw new RuntimeException("The inspectionBatch start time is before or equal to start time of the selected machining batch="
                                 + machiningBatch.getMachiningBatch().getMachiningBatchNumber());
    }
  }

  private void validateGaugeInspectionReportCounts(ProcessedItemInspectionBatch inspectionBatch) {
    int totalInspectionPiecesCount = inspectionBatch.getInspectionBatchPiecesCount();
    int totalFinishedPiecesCount = inspectionBatch.getFinishedInspectionBatchPiecesCount();
    int totalRejectedPiecesCount = inspectionBatch.getRejectInspectionBatchPiecesCount();
    int totalReworkedPiecesCount = inspectionBatch.getReworkPiecesCount();

    if (totalInspectionPiecesCount != totalFinishedPiecesCount + totalRejectedPiecesCount + totalReworkedPiecesCount) {
      log.error("The provided pieces count for each gauge inspection report is not equal to totalInspectionPiecesCount={}!", totalInspectionPiecesCount);
      throw new RuntimeException("The provided pieces count for each gauge inspection report is not equal to totalInspectionPiecesCount=" + totalInspectionPiecesCount);
    }
  }

  private void linkInspectionBatchEntities(InspectionBatch inspectionBatch, long tenantId,
                                           ProcessedItemInspectionBatch processedItemInspectionBatch,
                                           ProcessedItemMachiningBatch machiningBatch) {
    Tenant tenant = tenantService.getTenantById(tenantId);

    processedItemInspectionBatch.setInspectionBatch(inspectionBatch);
    processedItemInspectionBatch.setAvailableInspectionBatchPiecesCount(processedItemInspectionBatch.getInspectionBatchPiecesCount());
    processedItemInspectionBatch.setCreatedAt(LocalDateTime.now());

    inspectionBatch.setTenant(tenant);
    inspectionBatch.setProcessedItemInspectionBatch(processedItemInspectionBatch);
    inspectionBatch.setCreatedAt(LocalDateTime.now());
    inspectionBatch.setInspectionBatchStatus(InspectionBatch.InspectionBatchStatus.COMPLETED);

    // Only link to machining batch and perform machining-related operations if machiningBatch is not null
    if (machiningBatch != null) {
      inspectionBatch.setInputProcessedItemMachiningBatch(machiningBatch);
      
      if (processedItemInspectionBatch.getReworkPiecesCount() > 0) {
        machiningBatch.setReworkPiecesCount(machiningBatch.getReworkPiecesCount() + processedItemInspectionBatch.getReworkPiecesCount());
        machiningBatch.setReworkPiecesCountAvailableForRework(machiningBatch.getReworkPiecesCountAvailableForRework() + processedItemInspectionBatch.getReworkPiecesCount());
      }

      if (processedItemInspectionBatch.getRejectInspectionBatchPiecesCount() > 0) {
        machiningBatch.setRejectMachiningBatchPiecesCount(machiningBatch.getRejectMachiningBatchPiecesCount() + processedItemInspectionBatch.getRejectInspectionBatchPiecesCount());
      }

      if (processedItemInspectionBatch.getReworkPiecesCount() > 0 || processedItemInspectionBatch.getRejectInspectionBatchPiecesCount() > 0) {
        int rejectedAndReworkPiecesIncreasedDueToInspection = processedItemInspectionBatch.getReworkPiecesCount() + processedItemInspectionBatch.getRejectInspectionBatchPiecesCount();
        machiningBatch.setActualMachiningBatchPiecesCount(machiningBatch.getActualMachiningBatchPiecesCount() - rejectedAndReworkPiecesIncreasedDueToInspection);
      }
      
      log.info("Linked inspection batch to machining batch: {}", machiningBatch.getMachiningBatch().getMachiningBatchNumber());
    } else {
      // No machining batch to link to - this is for non-MACHINING previous operations
      inspectionBatch.setInputProcessedItemMachiningBatch(null);
      log.info("Inspection batch created without machining batch link (previous operation is not MACHINING)");
    }
  }

  private void persistGaugeInspectionReports(ProcessedItemInspectionBatch inspectionBatch) {
    inspectionBatch.getGaugeInspectionReports().forEach(report -> report.setProcessedItemInspectionBatch(inspectionBatch));
  }

  /**
   * Validates and distributes inspection results among daily machining batches
   */
  private void validateAndDistributeInspectionResults(ProcessedItemMachiningBatch machiningBatch, 
                                                     ProcessedItemInspectionBatch inspectionBatchDetails,
                                                     List<DailyMachiningBatchInspectionDistributionRepresentation> distributionList) {
    MachiningBatch mb = machiningBatch.getMachiningBatch();
    List<DailyMachiningBatch> dailyMachiningBatches = mb.getDailyMachiningBatch();
    
    if (dailyMachiningBatches == null || dailyMachiningBatches.isEmpty()) {
      log.error("No daily machining batches found for machining batch: {}", mb.getMachiningBatchNumber());
      throw new RuntimeException("No daily machining batches found for machining batch: " + mb.getMachiningBatchNumber());
    }

    // Validate that all daily machining batch IDs exist
    Map<Long, DailyMachiningBatch> dailyBatchMap = dailyMachiningBatches.stream()
        .collect(Collectors.toMap(DailyMachiningBatch::getId, dmb -> dmb));
    
    for (DailyMachiningBatchInspectionDistributionRepresentation distribution : distributionList) {
      if (!dailyBatchMap.containsKey(distribution.getDailyMachiningBatchId())) {
        log.error("Daily machining batch with ID {} not found in machining batch {}", 
                 distribution.getDailyMachiningBatchId(), mb.getMachiningBatchNumber());
        throw new RuntimeException("Daily machining batch with ID " + distribution.getDailyMachiningBatchId() + 
                                 " not found in machining batch " + mb.getMachiningBatchNumber());
      }
    }

    // Validate that the total distributed pieces match the inspection results
    validateDistributionTotals(inspectionBatchDetails, distributionList);

    // Save distribution records for reversal during deletion
    saveDistributionRecords(inspectionBatchDetails, distributionList, dailyBatchMap);

    // Update the daily machining batches with distributed results
    updateDailyMachiningBatchesWithInspectionResults(distributionList, dailyBatchMap);
  }

  /**
   * Validates that the total distributed pieces match the inspection batch totals
   */
  private void validateDistributionTotals(ProcessedItemInspectionBatch inspectionBatchDetails,
                                         List<DailyMachiningBatchInspectionDistributionRepresentation> distributionList) {
    int totalDistributedRejected = distributionList.stream()
        .mapToInt(d -> d.getRejectedPiecesCount() != null ? d.getRejectedPiecesCount() : 0)
        .sum();
    
    int totalDistributedRework = distributionList.stream()
        .mapToInt(d -> d.getReworkPiecesCount() != null ? d.getReworkPiecesCount() : 0)
        .sum();

    if (totalDistributedRejected != inspectionBatchDetails.getRejectInspectionBatchPiecesCount()) {
      log.error("Total distributed rejected pieces ({}) does not match inspection batch rejected pieces ({})", 
               totalDistributedRejected, inspectionBatchDetails.getRejectInspectionBatchPiecesCount());
      throw new RuntimeException("Total distributed rejected pieces (" + totalDistributedRejected + 
                               ") does not match inspection batch rejected pieces (" + 
                               inspectionBatchDetails.getRejectInspectionBatchPiecesCount() + ")");
    }

    if (totalDistributedRework != inspectionBatchDetails.getReworkPiecesCount()) {
      log.error("Total distributed rework pieces ({}) does not match inspection batch rework pieces ({})", 
               totalDistributedRework, inspectionBatchDetails.getReworkPiecesCount());
      throw new RuntimeException("Total distributed rework pieces (" + totalDistributedRework + 
                               ") does not match inspection batch rework pieces (" + 
                               inspectionBatchDetails.getReworkPiecesCount() + ")");
    }
  }

  /**
   * Updates daily machining batches with the distributed inspection results
   */
  private void updateDailyMachiningBatchesWithInspectionResults(List<DailyMachiningBatchInspectionDistributionRepresentation> distributionList,
                                                               Map<Long, DailyMachiningBatch> dailyBatchMap) {
    for (DailyMachiningBatchInspectionDistributionRepresentation distribution : distributionList) {
      DailyMachiningBatch dailyBatch = dailyBatchMap.get(distribution.getDailyMachiningBatchId());
      
      // Store the original completed pieces count before modification
      int originalCompleted = dailyBatch.getCompletedPiecesCount();
      int distributedRejected = distribution.getRejectedPiecesCount() != null ? distribution.getRejectedPiecesCount() : 0;
      int distributedRework = distribution.getReworkPiecesCount() != null ? distribution.getReworkPiecesCount() : 0;
      
      // Update the counts to reflect actual post-inspection results
      int actualCompleted = originalCompleted - distributedRejected - distributedRework;
      dailyBatch.setCompletedPiecesCount(actualCompleted);
      
      // Add to existing rejected and rework counts
      dailyBatch.setRejectedPiecesCount(dailyBatch.getRejectedPiecesCount() + distributedRejected);
      dailyBatch.setReworkPiecesCount(dailyBatch.getReworkPiecesCount() + distributedRework);
      
      // Save the updated daily machining batch
      dailyMachiningBatchService.save(dailyBatch);
      
      log.info("Updated daily machining batch {} - Actual completed: {}, Additional rejected: {}, Additional rework: {}",
               dailyBatch.getId(), actualCompleted, distributedRejected, distributedRework);
    }
  }

  /**
   * Saves distribution records for reversal during deletion
   */
  private void saveDistributionRecords(ProcessedItemInspectionBatch inspectionBatchDetails,
                                      List<DailyMachiningBatchInspectionDistributionRepresentation> distributionList,
                                      Map<Long, DailyMachiningBatch> dailyBatchMap) {
    for (DailyMachiningBatchInspectionDistributionRepresentation distribution : distributionList) {
      DailyMachiningBatch dailyBatch = dailyBatchMap.get(distribution.getDailyMachiningBatchId());
      
      // Calculate original completed pieces before inspection distribution
      int distributedRejected = distribution.getRejectedPiecesCount() != null ? distribution.getRejectedPiecesCount() : 0;
      int distributedRework = distribution.getReworkPiecesCount() != null ? distribution.getReworkPiecesCount() : 0;
      int originalCompleted = dailyBatch.getCompletedPiecesCount() - distributedRejected - distributedRework;
      
      DailyMachiningBatchInspectionDistribution distributionRecord = DailyMachiningBatchInspectionDistribution.builder()
          .processedItemInspectionBatch(inspectionBatchDetails)
          .dailyMachiningBatch(dailyBatch)
          .rejectedPiecesCount(distributedRejected)
          .reworkPiecesCount(distributedRework)
          .originalCompletedPiecesCount(originalCompleted)
          .deleted(false)
          .build();
      
      // Initialize the list if it's null
      if (inspectionBatchDetails.getDailyMachiningBatchInspectionDistributions() == null) {
        inspectionBatchDetails.setDailyMachiningBatchInspectionDistributions(new ArrayList<>());
      }
      
      inspectionBatchDetails.getDailyMachiningBatchInspectionDistributions().add(distributionRecord);
    }
  }

  /**
   * Reverts daily machining batch distribution during inspection batch deletion
   */
  private void revertDailyMachiningBatchDistribution(ProcessedItemInspectionBatch inspectionBatchDetails) {
    List<DailyMachiningBatchInspectionDistribution> distributions = 
        inspectionBatchDetails.getDailyMachiningBatchInspectionDistributions();
    
    if (distributions != null && !distributions.isEmpty()) {
      for (DailyMachiningBatchInspectionDistribution distribution : distributions) {
        DailyMachiningBatch dailyBatch = distribution.getDailyMachiningBatch();
        
        // Restore original completed pieces count
        dailyBatch.setCompletedPiecesCount(dailyBatch.getCompletedPiecesCount() +  distribution.getRejectedPiecesCount() + distribution.getReworkPiecesCount());
        
        // Remove the distributed rejected and rework pieces
        dailyBatch.setRejectedPiecesCount(
            dailyBatch.getRejectedPiecesCount() - distribution.getRejectedPiecesCount());
        dailyBatch.setReworkPiecesCount(
            dailyBatch.getReworkPiecesCount() - distribution.getReworkPiecesCount());
        
        // Save the reverted daily machining batch
        dailyMachiningBatchService.save(dailyBatch);
        
        log.info("Reverted daily machining batch {} - Restored completed: {}, Removed rejected: {}, Removed rework: {}",
                 dailyBatch.getId(), distribution.getOriginalCompletedPiecesCount(), 
                 distribution.getRejectedPiecesCount(), distribution.getReworkPiecesCount());
      }
    }
  }

  public boolean isInspectionBatchNumberForTenantExists(String inspectionBatchNumber, long tenantId) {
    return inspectionBatchRepository.existsByInspectionBatchNumberAndTenantIdAndDeletedFalse(inspectionBatchNumber, tenantId);
  }

  /**
   * Check if an inspection batch number was previously used and deleted
   * 
   * @param inspectionBatchNumber The inspection batch number to check
   * @param tenantId The tenant ID
   * @return True if the batch number was previously used and deleted
   */
  public boolean isInspectionBatchNumberPreviouslyUsed(String inspectionBatchNumber, Long tenantId) {
    return inspectionBatchRepository.existsByInspectionBatchNumberAndTenantIdAndOriginalInspectionBatchNumber(
        inspectionBatchNumber, tenantId);
  }

  public InspectionBatchListRepresentation getAllInspectionBatchesOfTenantWithoutPagination(long tenantId) {
    List<InspectionBatch> inspectionBatches = inspectionBatchRepository.findByTenantIdAndDeletedIsFalseOrderByUpdatedAtDesc(tenantId);
    return InspectionBatchListRepresentation.builder()
        .inspectionBatches(inspectionBatches.stream().map(inspectionBatch -> inspectionBatchAssembler.dissemble(inspectionBatch)).toList()).build();
  }

  public Page<InspectionBatchRepresentation> getAllInspectionBatchesOfTenant(long tenantId, int page, int size) {
    Pageable pageable = PageRequest.of(page, size);
    Page<InspectionBatch> inspectionBatchPage = inspectionBatchRepository.findByTenantIdAndDeletedIsFalseOrderByUpdatedAtDesc(tenantId, pageable);
    return inspectionBatchPage.map(inspectionBatch -> inspectionBatchAssembler.dissemble(inspectionBatch));
  }

  @Transactional(rollbackFor = Exception.class)
  public void deleteInspectionBatch(long tenantId, long inspectionBatchId) {
    log.info("Starting inspection batch deletion transaction for tenant: {}, batch: {}", 
             tenantId, inspectionBatchId);
    
    try {
      // Phase 1: Validate all deletion preconditions
      InspectionBatch inspectionBatch = validateInspectionBatchDeletionPreconditions(tenantId, inspectionBatchId);
      
      // Phase 2: Process inventory reversal for processed item - CRITICAL: Workflow and heat inventory operations
      ProcessedItemInspectionBatch processedItemInspectionBatch = inspectionBatch.getProcessedItemInspectionBatch();
      processInventoryReversalForProcessedItem(processedItemInspectionBatch, inspectionBatchId);
      
      // Phase 3: Soft delete processed item and associated records
      softDeleteProcessedItemAndAssociatedRecords(inspectionBatch, processedItemInspectionBatch);
      
      // Phase 4: Finalize inspection batch deletion
      finalizeInspectionBatchDeletion(inspectionBatch, inspectionBatchId, tenantId);
      log.info("Successfully persisted inspection batch deletion with ID: {}", inspectionBatchId);
      
      log.info("Successfully completed inspection batch deletion transaction for ID: {}", inspectionBatchId);
      
    } catch (Exception e) {
      log.error("Inspection batch deletion transaction failed for tenant: {}, batch: {}. " +
                "All changes will be rolled back. Error: {}", 
                tenantId, inspectionBatchId, e.getMessage());
      
      log.error("Inspection batch deletion failed - workflow updates, heat inventory reversals, and entity deletions will be rolled back.");
      
      // Re-throw to ensure transaction rollback
      throw e;
    }
  }

  /**
   * Phase 1: Validate all deletion preconditions
   */
  private InspectionBatch validateInspectionBatchDeletionPreconditions(long tenantId, long inspectionBatchId) {
    // Validate tenant existence
    tenantService.validateTenantExists(tenantId);

    // Find the inspection batch
    InspectionBatch inspectionBatch = inspectionBatchRepository.findByIdAndTenantIdAndDeletedFalse(
            inspectionBatchId, tenantId)
            .orElseThrow(() -> new InspectionBatchNotFoundException("Inspection batch not found with id=" + inspectionBatchId));

    // Validate inspection batch status is COMPLETED
    validateInspectionBatchStatusForDelete(inspectionBatch);

    // Validate no dispatch batches exist for this inspection batch
    validateIfAnyDispatchBatchExistsForInspectionBatch(inspectionBatch);

    return inspectionBatch;
  }

  /**
   * Phase 2: Process inventory reversal for processed item
   */
  private void processInventoryReversalForProcessedItem(ProcessedItemInspectionBatch processedItemInspectionBatch, long inspectionBatchId) {
    Item item = processedItemInspectionBatch.getItem();
    Long itemWorkflowId = processedItemInspectionBatch.getItemWorkflowId();

    if (itemWorkflowId != null) {
      try {
        validateWorkflowDeletionEligibility(itemWorkflowId, processedItemInspectionBatch.getId(), inspectionBatchId);
        handleInventoryReversalBasedOnWorkflowPosition(processedItemInspectionBatch, itemWorkflowId, inspectionBatchId, item);
      } catch (Exception e) {
        log.warn("Failed to handle workflow pieces reversion for item {}: {}. This may indicate workflow data inconsistency.",
                 item.getId(), e.getMessage());
        throw e;
      }
    } else {
      log.warn("No workflow ID found for item {} during inspection batch deletion. " +
               "This may be a legacy record before workflow integration.", item.getId());
      throw new IllegalStateException("No workflow ID found for item " + item.getId());
    }
  }

  /**
   * Validate workflow deletion eligibility
   */
  private void validateWorkflowDeletionEligibility(Long itemWorkflowId, Long entityId, Long inspectionBatchId) {
    // Use workflow-based validation: check if all entries in next operation are marked deleted
    boolean canDeleteInspection = itemWorkflowService.areAllNextOperationBatchesDeleted(itemWorkflowId, entityId, WorkflowStep.OperationType.QUALITY);

    if (!canDeleteInspection) {
      log.error("Cannot delete inspection id={} as the next operation has active (non-deleted) batches", inspectionBatchId);
      throw new IllegalStateException("This inspection cannot be deleted as the next operation has active batch entries.");
    }

    log.info("Inspection id={} is eligible for deletion - all next operation batches are deleted", inspectionBatchId);
  }

  /**
   * Handle inventory reversal based on workflow position (first operation vs subsequent operations)
   */
  private void handleInventoryReversalBasedOnWorkflowPosition(ProcessedItemInspectionBatch processedItemInspectionBatch, 
                                                              Long itemWorkflowId, long inspectionBatchId, Item item) {
    // Get the workflow to check if inspection was the first operation
    ItemWorkflow workflow = itemWorkflowService.getItemWorkflowById(itemWorkflowId);
    boolean wasFirstOperation = WorkflowStep.OperationType.QUALITY.equals(
        workflow.getWorkflowTemplate().getFirstStep().getOperationType());

    if (wasFirstOperation) {
      handleHeatInventoryReversalForFirstOperation(processedItemInspectionBatch, inspectionBatchId, item, itemWorkflowId);
    } else {
      handlePiecesReturnToPreviousOperation(processedItemInspectionBatch, itemWorkflowId);
    }
  }

  /**
   * Handle heat inventory reversal when inspection was the first operation
   */
  private void handleHeatInventoryReversalForFirstOperation(ProcessedItemInspectionBatch processedItemInspectionBatch, 
                                                            long inspectionBatchId, Item item, Long itemWorkflowId) {
    // This was the first operation - heat quantities will be returned to heat inventory
    log.info("Inspection batch {} was first operation for item {}, heat inventory will be reverted",
             inspectionBatchId, item.getId());

    // Update workflow step to mark inspection batch as deleted and adjust piece counts
    Integer finishedInspectionBatchPiecesCount = processedItemInspectionBatch.getFinishedInspectionBatchPiecesCount();
    if (finishedInspectionBatchPiecesCount != null && finishedInspectionBatchPiecesCount > 0) {
      try {
        itemWorkflowService.updateCurrentOperationStepForReturnedPieces(
            itemWorkflowId, 
            WorkflowStep.OperationType.QUALITY, 
            finishedInspectionBatchPiecesCount,
            processedItemInspectionBatch.getId()
        );
        log.info("Successfully marked inspection operation as deleted and updated workflow step for processed item {}, subtracted {} pieces", 
                 processedItemInspectionBatch.getId(), finishedInspectionBatchPiecesCount);
      } catch (Exception e) {
        log.error("Failed to update workflow step for deleted inspection processed item {}: {}", 
                 processedItemInspectionBatch.getId(), e.getMessage());
        throw new RuntimeException("Failed to update workflow step for inspection deletion: " + e.getMessage(), e);
      }
    } else {
      log.info("No finished inspection pieces to subtract for deleted processed item {}", processedItemInspectionBatch.getId());
    }

    // Return heat quantities to original heats (similar to HeatTreatmentBatchService.deleteHeatTreatmentBatch method)
    LocalDateTime currentTime = LocalDateTime.now();
    if (processedItemInspectionBatch.getInspectionHeats() != null &&
        !processedItemInspectionBatch.getInspectionHeats().isEmpty()) {

      processedItemInspectionBatch.getInspectionHeats().forEach(inspectionHeat -> {
        Heat heat = inspectionHeat.getHeat();
        int piecesToReturn = inspectionHeat.getPiecesUsed();

        // Return pieces to heat inventory based on heat's unit of measurement
        if (heat.getIsInPieces()) {
          // Heat is managed in pieces - return to availablePiecesCount
          int newAvailablePieces = heat.getAvailablePiecesCount() + piecesToReturn;
          heat.setAvailablePiecesCount(newAvailablePieces);
          log.info("Returned {} pieces to heat {} (pieces-based), new available pieces: {}",
                   piecesToReturn, heat.getId(), newAvailablePieces);
        } else {
          throw new IllegalStateException("Inspection batch has no pieces!");
        }

        // Persist the updated heat
        rawMaterialHeatService.updateRawMaterialHeat(heat);

        // Soft delete inspection heat record
        inspectionHeat.setDeleted(true);
        inspectionHeat.setDeletedAt(currentTime);

        log.info("Successfully returned {} pieces from heat {} for deleted inspection batch processed item {}",
                 piecesToReturn, heat.getId(), processedItemInspectionBatch.getId());
      });
    }
  }

  /**
   * Handle pieces return to previous operation when inspection was not the first operation
   */
  private void handlePiecesReturnToPreviousOperation(ProcessedItemInspectionBatch processedItemInspectionBatch, 
                                                     Long itemWorkflowId) {
    // This was not the first operation - return pieces to previous operation
    Long previousOperationBatchId = itemWorkflowService.getPreviousOperationBatchId(
        itemWorkflowId, 
        WorkflowStep.OperationType.QUALITY,
        processedItemInspectionBatch.getPreviousOperationProcessedItemId()
    );

    if (previousOperationBatchId != null) {
      itemWorkflowService.returnPiecesToSpecificPreviousOperation(
          itemWorkflowId,
          WorkflowStep.OperationType.QUALITY,
          previousOperationBatchId,
          processedItemInspectionBatch.getInspectionBatchPiecesCount(),
          processedItemInspectionBatch.getId()
      );

      log.info("Successfully returned {} pieces from inspection back to previous operation {} in workflow {}",
               processedItemInspectionBatch.getInspectionBatchPiecesCount(),
               previousOperationBatchId,
               itemWorkflowId);
    } else {
      log.warn("Could not determine previous operation batch ID for inspection batch processed item {}. " +
               "Pieces may not be properly returned to previous operation.", processedItemInspectionBatch.getId());
    }
  }

  /**
   * Phase 3: Soft delete processed item and associated records
   */
  private void softDeleteProcessedItemAndAssociatedRecords(InspectionBatch inspectionBatch, 
                                                           ProcessedItemInspectionBatch processedItemInspectionBatch) {
    LocalDateTime now = LocalDateTime.now();
    
    // Revert daily machining batch distribution if it exists (legacy logic for MACHINING-specific cases)
    revertDailyMachiningBatchDistribution(processedItemInspectionBatch);
    
    // Soft delete gauge inspection reports
    softDeleteGaugeInspectionReports(processedItemInspectionBatch, now);

    // Soft delete inspection heats if they exist
    softDeleteInspectionHeats(processedItemInspectionBatch, now);

    // Soft delete processedItemInspectionBatch
    processedItemInspectionBatch.setDeleted(true);
    processedItemInspectionBatch.setDeletedAt(now);
  }

  /**
   * Soft delete gauge inspection reports associated with the processed item
   */
  private void softDeleteGaugeInspectionReports(ProcessedItemInspectionBatch processedItemInspectionBatch, LocalDateTime now) {
    if (processedItemInspectionBatch.getGaugeInspectionReports() != null) {
      processedItemInspectionBatch.getGaugeInspectionReports().forEach(report -> {
        report.setDeleted(true);
        report.setDeletedAt(now);
      });
    }
  }

  /**
   * Soft delete inspection heats associated with the processed item (for first operation cases)
   */
  private void softDeleteInspectionHeats(ProcessedItemInspectionBatch processedItemInspectionBatch, LocalDateTime now) {
    if (processedItemInspectionBatch.getInspectionHeats() != null && 
        !processedItemInspectionBatch.getInspectionHeats().isEmpty()) {
      processedItemInspectionBatch.getInspectionHeats().forEach(inspectionHeat -> {
        inspectionHeat.setDeleted(true);
        inspectionHeat.setDeletedAt(now);
      });
    }
  }

  /**
   * Phase 4: Finalize inspection batch deletion
   */
  private void finalizeInspectionBatchDeletion(InspectionBatch inspectionBatch, long inspectionBatchId, long tenantId) {
    LocalDateTime now = LocalDateTime.now();
    
    // Store the original batch number and modify the batch number for deletion
    inspectionBatch.setOriginalInspectionBatchNumber(inspectionBatch.getInspectionBatchNumber());
    inspectionBatch.setInspectionBatchNumber(
        inspectionBatch.getInspectionBatchNumber() + "_deleted_" + inspectionBatch.getId() + "_" + now.toEpochSecond(java.time.ZoneOffset.UTC)
    );

    // Soft delete the inspection batch
    inspectionBatch.setDeleted(true);
    inspectionBatch.setDeletedAt(now);
    inspectionBatchRepository.save(inspectionBatch);

    log.info("Successfully deleted inspection batch with number={}, original number={} for tenant={}",
             inspectionBatch.getInspectionBatchNumber(), inspectionBatch.getOriginalInspectionBatchNumber(), tenantId);
  }

  private void validateInspectionBatchStatusForDelete(InspectionBatch inspectionBatch){
    if (inspectionBatch.getInspectionBatchStatus() != InspectionBatch.InspectionBatchStatus.COMPLETED) {
      log.error("The inspection batch with number={} is not in COMPLETED status!", inspectionBatch.getInspectionBatchNumber());
      throw new IllegalStateException("This inspection batch cannot be deleted as it is not in the COMPLETED status.");
    }
  }

  private void validateIfAnyDispatchBatchExistsForInspectionBatch(InspectionBatch inspectionBatch) {
    boolean isExists = dispatchProcessedItemInspectionRepository.existsByProcessedItemInspectionBatchIdAndDeletedFalse(inspectionBatch.getProcessedItemInspectionBatch().getId());
    if(isExists){
      log.error("There exists Dispatch entry for the inspectionBatchNumber={}!", inspectionBatch.getInspectionBatchNumber());
      throw new IllegalStateException("This inspection batch cannot be deleted as a dispatch entry exists for it.");
    }
  }
  
  /**
   * Gets all inspection batches associated with a specific machining batch
   * 
   * @param tenantId Tenant ID
   * @param machiningBatchId Machining batch ID
   * @return List representation of inspection batches
   */
  public InspectionBatchListRepresentation getInspectionBatchesByMachiningBatch(Long tenantId, Long machiningBatchId) {
    // Validate tenant existence
    tenantService.validateTenantExists(tenantId);
    
    // Get the processed item machining batch
    ProcessedItemMachiningBatch processedItemMachiningBatch = 
        processedItemMachiningBatchService.getProcessedItemMachiningBatchByMachiningBatchId(machiningBatchId);
    
    if (processedItemMachiningBatch == null) {
      log.error("Processed item machining batch not found for machining batch ID: {}", machiningBatchId);
      throw new RuntimeException("Processed item machining batch not found for machining batch ID: " + machiningBatchId);
    }
    
    // Find inspection batches associated with this machining batch
    List<InspectionBatch> inspectionBatches = 
        inspectionBatchRepository.findByInputProcessedItemMachiningBatchIdAndTenantIdAndDeletedFalse(
            processedItemMachiningBatch.getId(), tenantId);
    
    if (inspectionBatches.isEmpty()) {
      log.warn("No inspection batches found for machining batch ID: {}", machiningBatchId);
      throw new InspectionBatchNotFoundException("No inspection batches found for machining batch ID: " + machiningBatchId);
    }
    
    return InspectionBatchListRepresentation.builder()
        .inspectionBatches(inspectionBatches.stream()
            .map(inspectionBatch -> inspectionBatchAssembler.dissemble(inspectionBatch))
            .toList())
        .build();
  }

  /**
   * Find all inspection batches associated with a specific machining batch
   * @param machiningBatchId The ID of the machining batch
   * @return List of inspection batch representations associated with the machining batch
   */
  public List<InspectionBatchRepresentation> getInspectionBatchesByMachiningBatchId(Long machiningBatchId) {
    log.info("Finding inspection batches for machining batch ID: {}", machiningBatchId);
    
    List<InspectionBatch> inspectionBatches = inspectionBatchRepository.findByMachiningBatchId(machiningBatchId);
    
    return inspectionBatches.stream()
        .map(inspectionBatchAssembler::dissemble)
        .collect(Collectors.toList());
  }

  /**
   * Search for inspection batches by various criteria with pagination
   * @param tenantId The tenant ID
   * @param searchType The type of search (ITEM_NAME, FORGE_TRACEABILITY_NUMBER, or INSPECTION_BATCH_NUMBER)
   * @param searchTerm The search term (substring matching for all search types)
   * @param page The page number (0-based)
   * @param size The page size
   * @return Page of InspectionBatchRepresentation containing the search results
   */
  @Transactional(readOnly = true)
  public Page<InspectionBatchRepresentation> searchInspectionBatches(Long tenantId, String searchType, String searchTerm, int page, int size) {
    if (searchTerm == null || searchTerm.trim().isEmpty() || searchType == null || searchType.trim().isEmpty()) {
      Pageable pageable = PageRequest.of(page, size);
      return Page.empty(pageable);
    }

    tenantService.validateTenantExists(tenantId);
    Pageable pageable = PageRequest.of(page, size);
    Page<InspectionBatch> inspectionBatchPage;
    
    switch (searchType.toUpperCase()) {
      case "ITEM_NAME":
        inspectionBatchPage = inspectionBatchRepository.findInspectionBatchesByItemNameContainingIgnoreCase(tenantId, searchTerm.trim(), pageable);
        break;
      case "FORGE_TRACEABILITY_NUMBER":
        inspectionBatchPage = inspectionBatchRepository.findInspectionBatchesByForgeTraceabilityNumberContainingIgnoreCase(tenantId, searchTerm.trim(), pageable);
        break;
      case "INSPECTION_BATCH_NUMBER":
        inspectionBatchPage = inspectionBatchRepository.findInspectionBatchesByInspectionBatchNumberContainingIgnoreCase(tenantId, searchTerm.trim(), pageable);
        break;
      default:
        log.error("Invalid search type: {}", searchType);
        throw new IllegalArgumentException("Invalid search type: " + searchType + ". Valid types are: ITEM_NAME, FORGE_TRACEABILITY_NUMBER, INSPECTION_BATCH_NUMBER");
    }
    
    return inspectionBatchPage.map(inspectionBatchAssembler::dissemble);
  }

  /**
   * Retrieves inspection batches by multiple processed item inspection batch IDs and validates they belong to the tenant
   * @param processedItemInspectionBatchIds List of processed item inspection batch IDs
   * @param tenantId The tenant ID for validation
   * @return List of InspectionBatchRepresentation (distinct inspection batches)
   */
  public List<InspectionBatchRepresentation> getInspectionBatchesByProcessedItemInspectionBatchIds(List<Long> processedItemInspectionBatchIds, Long tenantId) {
    if (processedItemInspectionBatchIds == null || processedItemInspectionBatchIds.isEmpty()) {
      log.info("No processed item inspection batch IDs provided, returning empty list");
      return Collections.emptyList();
    }

    log.info("Getting inspection batches for {} processed item inspection batch IDs for tenant {}", processedItemInspectionBatchIds.size(), tenantId);
    
    List<InspectionBatch> inspectionBatches = inspectionBatchRepository.findByProcessedItemInspectionBatchIdInAndDeletedFalse(processedItemInspectionBatchIds);
    
    // Use a Set to track processed inspection batch IDs to avoid duplicates
    Set<Long> processedInspectionBatchIds = new HashSet<>();
    List<InspectionBatchRepresentation> validInspectionBatches = new ArrayList<>();
    List<Long> invalidProcessedItemInspectionBatchIds = new ArrayList<>();
    
    for (Long processedItemInspectionBatchId : processedItemInspectionBatchIds) {
      Optional<InspectionBatch> inspectionBatchOpt = inspectionBatches.stream()
          .filter(ib -> ib.getProcessedItemInspectionBatch() != null && 
                       ib.getProcessedItemInspectionBatch().getId().equals(processedItemInspectionBatchId))
          .findFirst();
          
      if (inspectionBatchOpt.isPresent()) {
        InspectionBatch inspectionBatch = inspectionBatchOpt.get();
        if (Long.valueOf(inspectionBatch.getTenant().getId()).equals(tenantId)) {
          // Only add if we haven't already processed this inspection batch
          if (!processedInspectionBatchIds.contains(inspectionBatch.getId())) {
            validInspectionBatches.add(inspectionBatchAssembler.dissemble(inspectionBatch));
            processedInspectionBatchIds.add(inspectionBatch.getId());
          }
        } else {
          log.warn("InspectionBatch for processedItemInspectionBatchId={} does not belong to tenant={}", processedItemInspectionBatchId, tenantId);
          invalidProcessedItemInspectionBatchIds.add(processedItemInspectionBatchId);
        }
      } else {
        log.warn("No inspection batch found for processedItemInspectionBatchId={}", processedItemInspectionBatchId);
        invalidProcessedItemInspectionBatchIds.add(processedItemInspectionBatchId);
      }
    }
    
    if (!invalidProcessedItemInspectionBatchIds.isEmpty()) {
      log.warn("The following processed item inspection batch IDs did not have valid inspection batches for tenant {}: {}", 
               tenantId, invalidProcessedItemInspectionBatchIds);
    }
    
    log.info("Found {} distinct valid inspection batches out of {} requested processed item inspection batch IDs", validInspectionBatches.size(), processedItemInspectionBatchIds.size());
    return validInspectionBatches;
  }

  public InspectionBatchRepresentation getInspectionBatchByProcessedItemInspectionBatchId(Long processedItemInspectionBatchId, Long tenantId) {
    if (processedItemInspectionBatchId == null ) {
      log.info("No processed item inspection batch ID provided, returning null");
      return null;
    }

    Optional<InspectionBatch> inspectionBatchOptional = inspectionBatchRepository.findByProcessedItemInspectionBatchIdAndDeletedFalse(processedItemInspectionBatchId);

    if (inspectionBatchOptional.isPresent()) {
      InspectionBatch inspectionBatch = inspectionBatchOptional.get();
      return inspectionBatchAssembler.dissemble(inspectionBatch);
    } else {
      log.error("No inspection batch found for processedItemInspectionBatchId={}", processedItemInspectionBatchId);
      throw new RuntimeException("No inspection batch found for processedItemInspectionBatchId=" + processedItemInspectionBatchId);
    }

  }

  /**
   * Handle workflow integration including pieces consumption and workflow step updates
   */
  private void handleWorkflowIntegration(InspectionBatchRepresentation inspectionBatchRepresentation, ProcessedItemInspectionBatch processedItemInspectionBatch) {
    try {
      // Get or validate workflow
      ItemWorkflow workflow = getOrValidateWorkflow(processedItemInspectionBatch);

      // Determine operation position and get workflow step
      WorkflowOperationContext operationContext = createOperationContext(processedItemInspectionBatch, workflow);

      // Start workflow step operation
      itemWorkflowService.startItemWorkflowStepOperation(operationContext.getTargetWorkflowStep());

      // Handle inventory/pieces consumption based on operation position
      handleInventoryConsumption(inspectionBatchRepresentation, processedItemInspectionBatch, workflow, operationContext.isFirstOperation());

      // Update workflow step with batch outcome data
      updateWorkflowStepWithBatchOutcome(processedItemInspectionBatch, operationContext.isFirstOperation(), operationContext.getTargetWorkflowStep());

      // Update related entity IDs
      updateRelatedEntityIds(operationContext.getTargetWorkflowStep(), processedItemInspectionBatch.getId());

    } catch (Exception e) {
      log.error("Failed to integrate inspection batch with workflow for item {}: {}", 
                processedItemInspectionBatch.getItem().getId(), e.getMessage());
      // Re-throw to fail the operation since workflow integration is critical
      throw new RuntimeException("Failed to integrate with workflow system: " + e.getMessage(), e);
    }
  }

  private void updateRelatedEntityIds(ItemWorkflowStep targetInspectionStep, Long processedItemId) {
    if (targetInspectionStep != null) {
      itemWorkflowService.updateRelatedEntityIdsForSpecificStep(targetInspectionStep, processedItemId);
    } else {
      log.warn("Could not find target Inspection/Quality ItemWorkflowStep for machining batch {}", processedItemId);
    }
  }

  private void handleInventoryConsumption(InspectionBatchRepresentation representation,
                                          ProcessedItemInspectionBatch processedItemInspectionBatch,
                                          ItemWorkflow workflow,
                                          boolean isFirstOperation) {
    if (isFirstOperation) {
      log.info("Inspection is the first operation in workflow - consuming inventory from heat");
      handleHeatConsumptionForFirstOperation(representation, processedItemInspectionBatch, workflow);
    } else {
      handlePiecesConsumptionFromPreviousOperation(processedItemInspectionBatch, workflow);
    }
  }

  private ItemWorkflow getOrValidateWorkflow(ProcessedItemInspectionBatch processedItemInspectionBatch) {
    Long itemWorkflowId = processedItemInspectionBatch.getItemWorkflowId();
    if (itemWorkflowId == null) {
      throw new IllegalStateException("ItemWorkflowId is required for inspection batch integration");
    }

    ItemWorkflow workflow = itemWorkflowService.getItemWorkflowById(itemWorkflowId);

    // Ensure workflow ID is set (defensive programming)
    if (processedItemInspectionBatch.getItemWorkflowId() == null) {
      processedItemInspectionBatch.setItemWorkflowId(workflow.getId());
      processedItemInspectionBatchService.save(processedItemInspectionBatch);
    }

    return workflow;
  }

  private WorkflowOperationContext createOperationContext(ProcessedItemInspectionBatch processedItemInspectionBatch, ItemWorkflow workflow) {
    Long previousOperationProcessedItemId = processedItemInspectionBatch.getPreviousOperationProcessedItemId();

    // Determine if this is the first operation
    boolean isFirstOperation = isFirstWorkflowOperation(previousOperationProcessedItemId, workflow);

    // Find the appropriate workflow step
    ItemWorkflowStep targetQualityStep = findTargetInspectionStep(workflow, previousOperationProcessedItemId, isFirstOperation);

    return new WorkflowOperationContext(isFirstOperation, targetQualityStep, previousOperationProcessedItemId);
  }

  private boolean isFirstWorkflowOperation(Long previousOperationProcessedItemId, ItemWorkflow workflow) {
    return previousOperationProcessedItemId == null ||
           WorkflowStep.OperationType.QUALITY.equals(workflow.getWorkflowTemplate().getFirstStep().getOperationType());
  }

  private ItemWorkflowStep findTargetInspectionStep(ItemWorkflow workflow, Long previousOperationProcessedItemId, boolean isFirstOperation) {
    if (isFirstOperation) {
      return workflow.getFirstRootStep();
    } else {
      return itemWorkflowService.findItemWorkflowStepByParentEntityId(
          workflow.getId(),
          previousOperationProcessedItemId,
          WorkflowStep.OperationType.QUALITY);
    }
  }

  /**
   * Handles pieces consumption from previous operation (if applicable)
   * Uses optimized single-call method to improve performance
   */
  private void handlePiecesConsumptionFromPreviousOperation(ProcessedItemInspectionBatch processedItemInspectionBatch, 
                                                           ItemWorkflow workflow) {
    // Use the optimized method that combines find + validate + consume in a single efficient call
    try {
      ItemWorkflowStep parentOperationStep = itemWorkflowService.validateAndConsumePiecesFromParentOperation(
          workflow.getId(),
          WorkflowStep.OperationType.QUALITY,
          processedItemInspectionBatch.getPreviousOperationProcessedItemId(),
          processedItemInspectionBatch.getInspectionBatchPiecesCount()
      );

      log.info("Efficiently consumed {} pieces from {} operation {} for inspection in workflow {}",
               processedItemInspectionBatch.getInspectionBatchPiecesCount(),
               parentOperationStep.getOperationType(),
               processedItemInspectionBatch.getPreviousOperationProcessedItemId(),
               workflow.getId());

    } catch (IllegalArgumentException e) {
      // Re-throw with context for inspection batch
      log.error("Failed to consume pieces for inspection batch: {}", e.getMessage());
      throw e;
    }
  }

  /**
   * Handles heat consumption from inventory when quality/inspection is the first operation
   */
  private void handleHeatConsumptionForFirstOperation(InspectionBatchRepresentation inspectionBatchRepresentation,
                                                     ProcessedItemInspectionBatch processedItemInspectionBatch,
                                                     ItemWorkflow workflow) {
    // Get the inspection heat data if provided
    if (inspectionBatchRepresentation.getProcessedItemInspectionBatch() == null || 
        inspectionBatchRepresentation.getProcessedItemInspectionBatch().getInspectionHeats() == null || 
        inspectionBatchRepresentation.getProcessedItemInspectionBatch().getInspectionHeats().isEmpty()) {
      log.warn("No heat consumption data provided for first operation inspection batch processed item {}. This may result in inventory inconsistency.",
               processedItemInspectionBatch.getId());
      return;
    }

    // Validate that heat consumption matches the required pieces
    int totalPiecesFromHeats = inspectionBatchRepresentation.getProcessedItemInspectionBatch().getInspectionHeats().stream()
        .mapToInt(heatRep -> heatRep.getPiecesUsed())
        .sum();

    if (totalPiecesFromHeats != processedItemInspectionBatch.getInspectionBatchPiecesCount()) {
      throw new IllegalArgumentException("Total pieces from heats (" + totalPiecesFromHeats + 
                                        ") does not match inspection batch pieces count (" + 
                                        processedItemInspectionBatch.getInspectionBatchPiecesCount() + 
                                        ") for processed item " + processedItemInspectionBatch.getId());
    }

    // Get the inspection batch start time for validation
    LocalDateTime startAtLocalDateTime = ConvertorUtils.convertStringToLocalDateTime(inspectionBatchRepresentation.getStartAt());

    // Validate heat availability and consume pieces from inventory
    inspectionBatchRepresentation.getProcessedItemInspectionBatch().getInspectionHeats().forEach(heatRepresentation -> {
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
        
        // Validate timing - heat should be received before the inspection start time
        LocalDateTime rawMaterialReceivingDate = heat.getRawMaterialProduct().getRawMaterial().getRawMaterialReceivingDate();
        if (rawMaterialReceivingDate != null && rawMaterialReceivingDate.compareTo(startAtLocalDateTime) > 0) {
          log.error("The provided start at time={} is before raw material receiving date={} for heat={} !", 
                    startAtLocalDateTime, rawMaterialReceivingDate, heat.getHeatNumber());
          throw new RuntimeException("The provided start at time=" + startAtLocalDateTime + 
                                   " is before raw material receiving date=" + rawMaterialReceivingDate + 
                                   " for heat=" + heat.getHeatNumber() + " !");
        }
        
        // Update heat available pieces count
        log.info("Updating AvailablePiecesCount for heat={} from {} to {} for inspection batch processed item {}", 
                 heat.getId(), heat.getAvailablePiecesCount(), newHeatPieces, processedItemInspectionBatch.getId());
        heat.setAvailablePiecesCount(newHeatPieces);
        
        // Persist the updated heat
        rawMaterialHeatService.updateRawMaterialHeat(heat);
        
        log.info("Successfully consumed {} pieces from heat {} for inspection batch processed item {} in workflow {}", 
                 heatRepresentation.getPiecesUsed(),
                 heatRepresentation.getHeat().getId(),
                 processedItemInspectionBatch.getId(),
                 workflow.getId());
        
      } catch (Exception e) {
        log.error("Failed to consume pieces from heat {} for inspection processed item {}: {}", 
                  heatRepresentation.getHeat().getId(), processedItemInspectionBatch.getId(), e.getMessage());
        throw new RuntimeException("Failed to consume inventory from heat: " + e.getMessage(), e);
      }
    });

    log.info("Successfully consumed inventory from {} heats for inspection batch processed item {} in workflow {}", 
             inspectionBatchRepresentation.getProcessedItemInspectionBatch().getInspectionHeats().size(), 
             processedItemInspectionBatch.getId(), workflow.getId());
  }


  private void updateWorkflowStepWithBatchOutcome(ProcessedItemInspectionBatch processedItemInspectionBatch, boolean isFirstOperation, ItemWorkflowStep operationStep) {
    OperationOutcomeData.BatchOutcome inspectionBatchOutcome = OperationOutcomeData.BatchOutcome.builder()
        .id(processedItemInspectionBatch.getId())
        .initialPiecesCount(processedItemInspectionBatch.getFinishedInspectionBatchPiecesCount())
        .piecesAvailableForNext(processedItemInspectionBatch.getFinishedInspectionBatchPiecesCount())
        .startedAt(processedItemInspectionBatch.getInspectionBatch().getStartAt())
        .createdAt(processedItemInspectionBatch.getCreatedAt())
        .updatedAt(LocalDateTime.now())
        .deletedAt(processedItemInspectionBatch.getDeletedAt())
        .deleted(processedItemInspectionBatch.isDeleted())
        .build();

    List<OperationOutcomeData.BatchOutcome> accumulatedBatchData = new ArrayList<>();

    if (!isFirstOperation) {
      accumulatedBatchData = itemWorkflowService.getAccumulatedBatchOutcomeData(operationStep);
    }
    accumulatedBatchData.add(inspectionBatchOutcome);
    itemWorkflowService.updateWorkflowStepForOperation(operationStep, OperationOutcomeData.forQualityOperation(accumulatedBatchData, LocalDateTime.now()));
  }

}
