package com.jangid.forging_process_management_service.service.quality;

import com.jangid.forging_process_management_service.assemblers.quality.InspectionBatchAssembler;
import com.jangid.forging_process_management_service.entities.Tenant;
import com.jangid.forging_process_management_service.entities.machining.ProcessedItemMachiningBatch;
import com.jangid.forging_process_management_service.entities.product.ItemStatus;
import com.jangid.forging_process_management_service.entities.quality.InspectionBatch;
import com.jangid.forging_process_management_service.entities.quality.ProcessedItemInspectionBatch;
import com.jangid.forging_process_management_service.entitiesRepresentation.quality.InspectionBatchListRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.quality.InspectionBatchRepresentation;
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

@Slf4j
@Service
public class InspectionBatchService {

  @Autowired
  private InspectionBatchRepository inspectionBatchRepository;

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

    // Assemble and validate InspectionBatch
    InspectionBatch inspectionBatch = inspectionBatchAssembler.createAssemble(inspectionBatchRepresentation);
    validateInspectionBatchNumber(tenantId, inspectionBatch);
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

  private void validateInspectionBatchNumber(long tenantId, InspectionBatch inspectionBatch) {
    if (isInspectionBatchNumberForTenantExists(inspectionBatch.getInspectionBatchNumber(), tenantId)) {
      log.error("The provided inspectionBatch number={} already exists for the tenant={}!", inspectionBatch.getInspectionBatchNumber(), tenantId);
      throw new RuntimeException("The provided inspectionBatch number=" + inspectionBatch.getInspectionBatchNumber() + " already exists for the tenant=" + tenantId);
    }
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
    }

    if (processedItemInspectionBatch.getRejectInspectionBatchPiecesCount() > 0) {
      machiningBatch.setRejectMachiningBatchPiecesCount(machiningBatch.getRejectMachiningBatchPiecesCount() + processedItemInspectionBatch.getRejectInspectionBatchPiecesCount());
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
}
