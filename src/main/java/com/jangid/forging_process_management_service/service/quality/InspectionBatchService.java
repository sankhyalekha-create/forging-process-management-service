package com.jangid.forging_process_management_service.service.quality;

import com.jangid.forging_process_management_service.assemblers.quality.InspectionBatchAssembler;
import com.jangid.forging_process_management_service.entities.Tenant;
import com.jangid.forging_process_management_service.entities.machining.ProcessedItemMachiningBatch;
import com.jangid.forging_process_management_service.entities.product.ItemStatus;
import com.jangid.forging_process_management_service.entities.quality.InspectionBatch;
import com.jangid.forging_process_management_service.entities.quality.ProcessedItemInspectionBatch;
import com.jangid.forging_process_management_service.entitiesRepresentation.quality.InspectionBatchListRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.quality.InspectionBatchRepresentation;
import com.jangid.forging_process_management_service.exception.quality.InspectionBatchNotFoundException;
import com.jangid.forging_process_management_service.repositories.dispatch.DispatchProcessedItemInspectionRepository;
import com.jangid.forging_process_management_service.repositories.quality.InspectionBatchRepository;
import com.jangid.forging_process_management_service.service.TenantService;
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
import java.util.List;
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

  public boolean isInspectionBatchNumberForTenantExists(String inspectionBatchNumber, long tenantId) {
    return inspectionBatchRepository.existsByInspectionBatchNumberAndTenantIdAndDeletedFalse(inspectionBatchNumber, tenantId);
  }

  public InspectionBatchListRepresentation getAllInspectionBatchesOfTenantWithoutPagination(long tenantId) {
    List<InspectionBatch> inspectionBatches = inspectionBatchRepository.findByTenantIdAndDeletedIsFalseOrderByCreatedAtDesc(tenantId);
    return InspectionBatchListRepresentation.builder()
        .inspectionBatches(inspectionBatches.stream().map(inspectionBatch -> inspectionBatchAssembler.dissemble(inspectionBatch)).toList()).build();
  }

  public Page<InspectionBatchRepresentation> getAllInspectionBatchesOfTenant(long tenantId, int page, int size) {
    Pageable pageable = PageRequest.of(page, size);
    Page<InspectionBatch> inspectionBatchPage = inspectionBatchRepository.findByTenantIdAndDeletedIsFalseOrderByCreatedAtDesc(tenantId, pageable);
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
    if (inspectionBatchDetails.getGaugeInspectionReports() != null) {
        inspectionBatchDetails.getGaugeInspectionReports().forEach(report -> {
            report.setDeleted(true);
            report.setDeletedAt(LocalDateTime.now());
        });
    }

    inspectionBatchDetails.setDeleted(true);
    inspectionBatchDetails.setDeletedAt(LocalDateTime.now());

    // Soft delete the inspection batch
    inspectionBatch.setDeleted(true);
    inspectionBatch.setDeletedAt(LocalDateTime.now());
    inspectionBatchRepository.save(inspectionBatch);

    log.info("Successfully deleted inspection batch with number={} for tenant={}",
             inspectionBatch.getInspectionBatchNumber(), tenantId);
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
}
