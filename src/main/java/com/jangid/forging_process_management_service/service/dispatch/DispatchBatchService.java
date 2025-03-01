package com.jangid.forging_process_management_service.service.dispatch;

import com.jangid.forging_process_management_service.assemblers.dispatch.DispatchBatchAssembler;
import com.jangid.forging_process_management_service.entities.ProcessedItem;
import com.jangid.forging_process_management_service.entities.dispatch.DispatchBatch;
import com.jangid.forging_process_management_service.entities.dispatch.ProcessedItemDispatchBatch;
import com.jangid.forging_process_management_service.entities.product.ItemStatus;
import com.jangid.forging_process_management_service.entities.quality.ProcessedItemInspectionBatch;
import com.jangid.forging_process_management_service.entitiesRepresentation.dispatch.DispatchBatchListRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.dispatch.DispatchBatchRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.quality.ProcessedItemInspectionBatchRepresentation;
import com.jangid.forging_process_management_service.exception.dispatch.DispatchBatchException;
import com.jangid.forging_process_management_service.exception.dispatch.DispatchBatchNotFoundException;
import com.jangid.forging_process_management_service.repositories.dispatch.DispatchBatchRepository;
import com.jangid.forging_process_management_service.service.ProcessedItemService;
import com.jangid.forging_process_management_service.service.TenantService;
import com.jangid.forging_process_management_service.service.quality.ProcessedItemInspectionBatchService;
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
import java.util.Optional;

@Slf4j
@Service
public class DispatchBatchService {

  private final DispatchBatchRepository dispatchBatchRepository;
  private final TenantService tenantService;
  private final ProcessedItemService processedItemService;
  private final ProcessedItemInspectionBatchService processedItemInspectionBatchService;
  private final DispatchBatchAssembler dispatchBatchAssembler;

  @Autowired
  public DispatchBatchService(
      DispatchBatchRepository dispatchBatchRepository,
      TenantService tenantService,
      ProcessedItemService processedItemService,
      ProcessedItemInspectionBatchService processedItemInspectionBatchService,
      DispatchBatchAssembler dispatchBatchAssembler) {
    this.dispatchBatchRepository = dispatchBatchRepository;
    this.tenantService = tenantService;
    this.processedItemService = processedItemService;
    this.processedItemInspectionBatchService = processedItemInspectionBatchService;
    this.dispatchBatchAssembler = dispatchBatchAssembler;
  }

  @Transactional
  public DispatchBatchRepresentation createDispatchBatch(long tenantId, DispatchBatchRepresentation representation) {
    tenantService.validateTenantExists(tenantId);

    DispatchBatch dispatchBatch = dispatchBatchAssembler.createAssemble(representation);
    validateDispatchBatchNumber(tenantId, dispatchBatch);
    validateCreateDispatchTime(dispatchBatch, dispatchBatch.getDispatchCreatedAt());

    long processedItemId = representation.getProcessedItemInspectionBatches()
        .stream()
        .findFirst()
        .map(batch -> batch.getProcessedItem().getId())
        .orElseThrow(() -> new DispatchBatchException("Processed Item ID is missing"));

    ProcessedItem processedItem = processedItemService.getProcessedItemById(processedItemId);

    ProcessedItemDispatchBatch processedItemDispatchBatch = ProcessedItemDispatchBatch.builder()
        .dispatchBatch(dispatchBatch)
        .itemStatus(ItemStatus.COMPLETE_DISPATCH_IN_PROGRESS)
        .processedItem(processedItem)
        .totalDispatchPiecesCount(representation.getProcessedItemDispatchBatch().getTotalDispatchPiecesCount())
        .createdAt(LocalDateTime.now())
        .build();

    dispatchBatch.setProcessedItemDispatchBatch(processedItemDispatchBatch);
    dispatchBatch.setTenant(tenantService.getTenantById(tenantId));
    dispatchBatch.setDispatchBatchStatus(DispatchBatch.DispatchBatchStatus.DISPATCH_IN_PROGRESS);

    // Update inspection batches and their status
    List<ProcessedItemInspectionBatch> updatedBatches = representation.getProcessedItemInspectionBatches().stream()
        .map(batchRep -> updateProcessedItemInspectionBatch(batchRep))
        .toList();

    updatedBatches.forEach(processedItemInspectionBatch -> processedItemInspectionBatch.setDispatchBatch(dispatchBatch));

    dispatchBatch.getProcessedItemInspectionBatches().clear();
    dispatchBatch.setProcessedItemInspectionBatches(updatedBatches);

    DispatchBatch createdDispatchBatch = dispatchBatchRepository.save(dispatchBatch);
    return dispatchBatchAssembler.dissemble(createdDispatchBatch);
  }

  private ProcessedItemInspectionBatch updateProcessedItemInspectionBatch(ProcessedItemInspectionBatchRepresentation batchRep) {
    ProcessedItemInspectionBatch batch = processedItemInspectionBatchService.getProcessedItemInspectionBatchById(batchRep.getId());
    int updatedAvailableDispatchPieces = batch.getAvailableDispatchPiecesCount() - batchRep.getSelectedDispatchPiecesCount();
    batch.setAvailableDispatchPiecesCount(updatedAvailableDispatchPieces);
    int dispatchedPiecesCount;
    if (batch.getDispatchedPiecesCount() == null) {
      dispatchedPiecesCount = 0;
    } else {
      dispatchedPiecesCount = batch.getDispatchedPiecesCount();
    }

    batch.setDispatchedPiecesCount(dispatchedPiecesCount + batchRep.getSelectedDispatchPiecesCount());

    batch.setItemStatus(updatedAvailableDispatchPieces == 0
                        ? ItemStatus.COMPLETE_DISPATCH_IN_PROGRESS
                        : ItemStatus.PARTIAL_DISPATCH_IN_PROGRESS);

    return batch;
  }

  private void validateDispatchBatchNumber(long tenantId, DispatchBatch dispatchBatch) {
    if (dispatchBatchRepository.existsByDispatchBatchNumberAndTenantIdAndDeletedFalse(dispatchBatch.getDispatchBatchNumber(), tenantId)) {
      log.error("Dispatch batch number={} already exists for tenant={}", dispatchBatch.getDispatchBatchNumber(), tenantId);
      throw new DispatchBatchException("Dispatch batch number " + dispatchBatch.getDispatchBatchNumber() + " already exists for tenant " + tenantId);
    }
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

  public DispatchBatchRepresentation markReadyToDispatchBatch(long tenantId, long dispatchBatchId, String readyToDispatchTime){
    tenantService.validateTenantExists(tenantId);
    DispatchBatch existingDispatchBatch = getDispatchBatchById(dispatchBatchId);

    if(existingDispatchBatch.getDispatchBatchStatus() != DispatchBatch.DispatchBatchStatus.DISPATCH_IN_PROGRESS){
    log.error("DispatchBatch having dispatch batch number={}, having id={} is not in DISPATCH_IN_PROGRESS status!", existingDispatchBatch.getDispatchBatchNumber(), existingDispatchBatch.getId());
    }
    LocalDateTime dispatchReadyTime = ConvertorUtils.convertStringToLocalDateTime(readyToDispatchTime);
    validateReadyToDispatchTime(existingDispatchBatch, dispatchReadyTime);

    existingDispatchBatch.setDispatchBatchStatus(DispatchBatch.DispatchBatchStatus.READY_TO_DISPATCH);
    existingDispatchBatch.setDispatchReadyAt(dispatchReadyTime);
    DispatchBatch updatedDispatchBatch = dispatchBatchRepository.save(existingDispatchBatch);
    return dispatchBatchAssembler.dissemble(updatedDispatchBatch);

  }

  private void validateReadyToDispatchTime(DispatchBatch existingDispatchBatch, LocalDateTime providedReadyToDispatchTime){
    if (existingDispatchBatch.getCreatedAt().compareTo(providedReadyToDispatchTime) > 0) {
      log.error("The provided ReadyToDispatchTime for DispatchBatch having dispatch batch number={}, having id={} is before dispatch batch created time!", existingDispatchBatch.getDispatchBatchNumber(), existingDispatchBatch.getId());
      throw new RuntimeException("The provided ReadyToDispatchTime for DispatchBatch having dispatch batch number=" + existingDispatchBatch.getDispatchBatchNumber() + " , having id=" + existingDispatchBatch.getId() + " is before dispatch batch created time!");
    }
  }

  private void validateCreateDispatchTime(DispatchBatch dispatchBatch, LocalDateTime providedTime){
    boolean isInvalidTime = dispatchBatch.getProcessedItemInspectionBatches().stream()
        .map(ib -> ib.getInspectionBatch().getEndAt())
        .anyMatch(endAt -> endAt.compareTo(providedTime) > 0);

    if (isInvalidTime) {
      log.error("The provided dispatchCreatedAt for DispatchBatch having dispatch batch number={}, having id={} is before one or more inspection batch end times!",
                dispatchBatch.getDispatchBatchNumber(), dispatchBatch.getId());
      throw new RuntimeException("The provided dispatchCreatedAt for DispatchBatch having dispatch batch number="
                                 + dispatchBatch.getDispatchBatchNumber() + " , having id=" + dispatchBatch.getId()
                                 + " is before one or more inspection batch end times!");
    }
  }

  private void validateDispatchedTime(DispatchBatch existingDispatchBatch, LocalDateTime providedDispatchedTime){
    if (existingDispatchBatch.getDispatchReadyAt().compareTo(providedDispatchedTime) > 0) {
      log.error("The provided dispatched time for DispatchBatch having dispatch batch number={}, having id={} is before the dispatch ready time!", existingDispatchBatch.getDispatchBatchNumber(), existingDispatchBatch.getId());
      throw new RuntimeException("The provided dispatched time for DispatchBatch having dispatch batch number=" + existingDispatchBatch.getDispatchBatchNumber() + " , having id=" + existingDispatchBatch.getId() + " is before the dispatch ready time!");
    }
  }

  // markDispatchedToDispatchBatch
  public DispatchBatchRepresentation markDispatchedToDispatchBatch(long tenantId, long dispatchBatchId, String dispatchedTime){
    tenantService.validateTenantExists(tenantId);
    DispatchBatch existingDispatchBatch = getDispatchBatchById(dispatchBatchId);

    if(existingDispatchBatch.getDispatchBatchStatus() != DispatchBatch.DispatchBatchStatus.READY_TO_DISPATCH){
      log.error("DispatchBatch having dispatch batch number={}, having id={} is not in READY_TO_DISPATCH status!", existingDispatchBatch.getDispatchBatchNumber(), existingDispatchBatch.getId());
    }
    LocalDateTime dispatchTime = ConvertorUtils.convertStringToLocalDateTime(dispatchedTime);
    validateDispatchedTime(existingDispatchBatch, dispatchTime);

    existingDispatchBatch.setDispatchBatchStatus(DispatchBatch.DispatchBatchStatus.DISPATCHED);
    existingDispatchBatch.setDispatchedAt(dispatchTime);
    DispatchBatch updatedDispatchBatch = dispatchBatchRepository.save(existingDispatchBatch);
    return dispatchBatchAssembler.dissemble(updatedDispatchBatch);
  }



  public DispatchBatch getDispatchBatchById(long id){
    Optional<DispatchBatch> dispatchBatchOptional = dispatchBatchRepository.findByIdAndDeletedFalse(id);
    if(dispatchBatchOptional.isEmpty()){
      log.error("Dispatch Batch not found with id={}", id);
      throw new DispatchBatchNotFoundException("Dispatch Batch not found with id="+id);
    }
    return dispatchBatchOptional.get();
  }
}
