package com.jangid.forging_process_management_service.service.quality;

import com.jangid.forging_process_management_service.assemblers.quality.InspectionBatchAssembler;
import com.jangid.forging_process_management_service.entities.Tenant;
import com.jangid.forging_process_management_service.entities.machining.DailyMachiningBatch;
import com.jangid.forging_process_management_service.entities.machining.MachiningBatch;
import com.jangid.forging_process_management_service.entities.machining.ProcessedItemMachiningBatch;
import com.jangid.forging_process_management_service.entities.product.ItemStatus;
import com.jangid.forging_process_management_service.entities.quality.DailyMachiningBatchInspectionDistribution;
import com.jangid.forging_process_management_service.entities.quality.InspectionBatch;
import com.jangid.forging_process_management_service.entities.quality.ProcessedItemInspectionBatch;
import com.jangid.forging_process_management_service.entitiesRepresentation.quality.DailyMachiningBatchInspectionDistributionRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.quality.InspectionBatchListRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.quality.InspectionBatchRepresentation;
import com.jangid.forging_process_management_service.exception.quality.InspectionBatchNotFoundException;
import com.jangid.forging_process_management_service.repositories.dispatch.DispatchProcessedItemInspectionRepository;
import com.jangid.forging_process_management_service.repositories.quality.DailyMachiningBatchInspectionDistributionRepository;
import com.jangid.forging_process_management_service.repositories.quality.InspectionBatchRepository;
import com.jangid.forging_process_management_service.service.TenantService;
import com.jangid.forging_process_management_service.service.machining.DailyMachiningBatchService;
import com.jangid.forging_process_management_service.service.machining.ProcessedItemMachiningBatchService;
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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class InspectionBatchService {

  @Autowired
  private InspectionBatchRepository inspectionBatchRepository;

  @Autowired
  private DispatchProcessedItemInspectionRepository dispatchProcessedItemInspectionRepository;

  @Autowired
  private DailyMachiningBatchInspectionDistributionRepository dailyMachiningBatchInspectionDistributionRepository;

  @Autowired
  private TenantService tenantService;

  @Autowired
  private ProcessedItemMachiningBatchService processedItemMachiningBatchService;

  @Autowired
  private DailyMachiningBatchService dailyMachiningBatchService;

  @Autowired
  private InspectionBatchAssembler inspectionBatchAssembler;

  @Transactional
  public InspectionBatchRepresentation createInspectionBatch(long tenantId, InspectionBatchRepresentation inspectionBatchRepresentation) {
    // Validate tenant existence
    tenantService.validateTenantExists(tenantId);

    boolean exists = isInspectionBatchNumberForTenantExists(inspectionBatchRepresentation.getInspectionBatchNumber(), tenantId);
    if (exists) {
      log.error("The provided inspectionBatch number={} already exists for the tenant={}!", inspectionBatchRepresentation.getInspectionBatchNumber(), tenantId);
      throw new IllegalStateException("The provided inspectionBatch number=" + inspectionBatchRepresentation.getInspectionBatchNumber() + " already exists for the tenant=" + tenantId);
    }
    
    // Check if this batch number was previously used and deleted
    if (isInspectionBatchNumberPreviouslyUsed(inspectionBatchRepresentation.getInspectionBatchNumber(), tenantId)) {
      log.warn("Inspection Batch with batch number: {} was previously used and deleted for tenant: {}", 
               inspectionBatchRepresentation.getInspectionBatchNumber(), tenantId);
    }

    // Assemble and validate InspectionBatch
    InspectionBatch inspectionBatch = inspectionBatchAssembler.createAssemble(inspectionBatchRepresentation);
    validateBatchTimeRange(inspectionBatchRepresentation);

    // Validate and update associated machining and inspection batch entities
    ProcessedItemMachiningBatch machiningBatch = inspectionBatch.getInputProcessedItemMachiningBatch();
    ProcessedItemInspectionBatch inspectionBatchDetails = inspectionBatch.getProcessedItemInspectionBatch();
    inspectionBatchDetails.updatePieceCounts();

    validateInspectionBatchPiecesCount(machiningBatch, inspectionBatchDetails);
    validateMachiningBatchStartTime(machiningBatch, inspectionBatchRepresentation);

    inspectionBatchDetails.setItemStatus(ItemStatus.QUALITY_COMPLETED);
    inspectionBatchDetails.setCreatedAt(LocalDateTime.now());

    validateGaugeInspectionReportCounts(inspectionBatchDetails);

    // Validate and distribute inspection results among daily machining batches
    if (inspectionBatchRepresentation.getProcessedItemInspectionBatch().getDailyMachiningBatchInspectionDistribution() != null &&
        !inspectionBatchRepresentation.getProcessedItemInspectionBatch().getDailyMachiningBatchInspectionDistribution().isEmpty()) {
      validateAndDistributeInspectionResults(machiningBatch, inspectionBatchDetails, 
                                            inspectionBatchRepresentation.getProcessedItemInspectionBatch().getDailyMachiningBatchInspectionDistribution());
    }

    // Link entities and prepare for persistence
    linkInspectionBatchEntities(inspectionBatch, tenantId, inspectionBatchDetails, machiningBatch);
    persistGaugeInspectionReports(inspectionBatchDetails);

    // Persist updated machining batch
    processedItemMachiningBatchService.save(machiningBatch);

    // Save the inspection batch
    InspectionBatch createdInspectionBatch = inspectionBatchRepository.save(inspectionBatch);
    return inspectionBatchAssembler.dissemble(createdInspectionBatch);
  }

  private void validateBatchTimeRange(InspectionBatchRepresentation inspectionBatchRepresentation) {
    LocalDateTime startAt = ConvertorUtils.convertStringToLocalDateTime(inspectionBatchRepresentation.getStartAt());
    LocalDateTime endAt = ConvertorUtils.convertStringToLocalDateTime(inspectionBatchRepresentation.getEndAt());

    if (startAt.isAfter(endAt) || startAt.isEqual(endAt)) {
      log.error("The inspectionBatch end time is before or equal to start time!");
      throw new RuntimeException("The inspectionBatch end time is before or equal to start time!");
    }
  }

  private void validateInspectionBatchPiecesCount(ProcessedItemMachiningBatch machiningBatch, ProcessedItemInspectionBatch inspectionBatch) {
    if (machiningBatch.getAvailableInspectionBatchPiecesCount() < inspectionBatch.getInspectionBatchPiecesCount()) {
      log.error("The inspectionBatch selected pieces count exceeds the available inspection batch pieces count for machining batch={}!",
                machiningBatch.getMachiningBatch().getMachiningBatchNumber());
      throw new RuntimeException("The inspectionBatch selected pieces count exceeds the available inspection batch pieces count for machining batch="
                                 + machiningBatch.getMachiningBatch().getMachiningBatchNumber());
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
    processedItemInspectionBatch.setProcessedItem(machiningBatch.getProcessedItem());

    inspectionBatch.setTenant(tenant);
    inspectionBatch.setProcessedItemInspectionBatch(processedItemInspectionBatch);
    inspectionBatch.setInputProcessedItemMachiningBatch(machiningBatch);
    inspectionBatch.setCreatedAt(LocalDateTime.now());
    inspectionBatch.setInspectionBatchStatus(InspectionBatch.InspectionBatchStatus.COMPLETED);

    int updatedAvailablePieces = machiningBatch.getAvailableInspectionBatchPiecesCount() - processedItemInspectionBatch.getInspectionBatchPiecesCount();
    machiningBatch.setAvailableInspectionBatchPiecesCount(updatedAvailablePieces);

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

  @Transactional
  public void deleteInspectionBatch(long tenantId, long inspectionBatchId) {
    // Validate tenant existence
    tenantService.validateTenantExists(tenantId);

    // Find the inspection batch
    InspectionBatch inspectionBatch = inspectionBatchRepository.findByIdAndTenantIdAndDeletedFalse(
            inspectionBatchId, tenantId)
            .orElseThrow(() -> new InspectionBatchNotFoundException("Inspection batch not found with id=" + inspectionBatchId));

    validateInspectionBatchStatusForDelete(inspectionBatch);

    ProcessedItemMachiningBatch machiningBatch = inspectionBatch.getInputProcessedItemMachiningBatch();
    ProcessedItemInspectionBatch inspectionBatchDetails = inspectionBatch.getProcessedItemInspectionBatch();
    validateIfAnyDispatchBatchExistsForInspectionBatch(inspectionBatch);

    // Revert daily machining batch distribution if it exists
    revertDailyMachiningBatchDistribution(inspectionBatchDetails);

    // Revert machining batch counts
    int updatedAvailablePieces = machiningBatch.getAvailableInspectionBatchPiecesCount() +
            inspectionBatchDetails.getInspectionBatchPiecesCount();
    machiningBatch.setAvailableInspectionBatchPiecesCount(updatedAvailablePieces);

    if (inspectionBatchDetails.getReworkPiecesCount() > 0) {
        machiningBatch.setReworkPiecesCount(
                machiningBatch.getReworkPiecesCount() - inspectionBatchDetails.getReworkPiecesCount());
      machiningBatch.setReworkPiecesCountAvailableForRework(machiningBatch.getReworkPiecesCountAvailableForRework() - inspectionBatchDetails.getReworkPiecesCount());
    }

    if (inspectionBatchDetails.getRejectInspectionBatchPiecesCount() > 0) {
        machiningBatch.setRejectMachiningBatchPiecesCount(
                machiningBatch.getRejectMachiningBatchPiecesCount() -
                inspectionBatchDetails.getRejectInspectionBatchPiecesCount());
    }

    // Save the updated machining batch
    processedItemMachiningBatchService.save(machiningBatch);

    // Mark all gauge inspection reports as deleted
    LocalDateTime now = LocalDateTime.now();
    if (inspectionBatchDetails.getGaugeInspectionReports() != null) {
        inspectionBatchDetails.getGaugeInspectionReports().forEach(report -> {
            report.setDeleted(true);
            report.setDeletedAt(now);
        });
    }

    inspectionBatchDetails.setDeleted(true);
    inspectionBatchDetails.setDeletedAt(now);

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
      log.error("There exists Dispatch entry for the inspectionBatchNumber={} for the tenant={}!", inspectionBatch.getInspectionBatchNumber(), inspectionBatch.getTenant().getId());
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
}
