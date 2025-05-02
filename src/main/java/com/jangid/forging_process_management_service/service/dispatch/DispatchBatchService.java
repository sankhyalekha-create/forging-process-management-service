package com.jangid.forging_process_management_service.service.dispatch;

import com.jangid.forging_process_management_service.assemblers.dispatch.DispatchBatchAssembler;
import com.jangid.forging_process_management_service.entities.ProcessedItem;
import com.jangid.forging_process_management_service.entities.dispatch.DispatchBatch;
import com.jangid.forging_process_management_service.entities.dispatch.DispatchProcessedItemInspection;
import com.jangid.forging_process_management_service.entities.dispatch.ProcessedItemDispatchBatch;
import com.jangid.forging_process_management_service.entities.product.ItemStatus;
import com.jangid.forging_process_management_service.entities.quality.ProcessedItemInspectionBatch;
import com.jangid.forging_process_management_service.entitiesRepresentation.dispatch.DispatchBatchListRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.dispatch.DispatchBatchRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.dispatch.DispatchStatisticsRepresentation;
import com.jangid.forging_process_management_service.exception.dispatch.DispatchBatchException;
import com.jangid.forging_process_management_service.exception.dispatch.DispatchBatchNotFoundException;
import com.jangid.forging_process_management_service.repositories.dispatch.DispatchBatchRepository;
import com.jangid.forging_process_management_service.service.ProcessedItemService;
import com.jangid.forging_process_management_service.service.TenantService;
import com.jangid.forging_process_management_service.service.buyer.BuyerService;
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
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DispatchBatchService {

  private final DispatchBatchRepository dispatchBatchRepository;
  private final TenantService tenantService;
  private final BuyerService buyerService;
  private final ProcessedItemService processedItemService;
  private final ProcessedItemInspectionBatchService processedItemInspectionBatchService;
  private final DispatchBatchAssembler dispatchBatchAssembler;

  @Autowired
  public DispatchBatchService(
      DispatchBatchRepository dispatchBatchRepository,
      TenantService tenantService,
      BuyerService buyerService,
      ProcessedItemService processedItemService,
      ProcessedItemInspectionBatchService processedItemInspectionBatchService,
      DispatchBatchAssembler dispatchBatchAssembler) {
    this.dispatchBatchRepository = dispatchBatchRepository;
    this.tenantService = tenantService;
    this.buyerService = buyerService;
    this.processedItemService = processedItemService;
    this.processedItemInspectionBatchService = processedItemInspectionBatchService;
    this.dispatchBatchAssembler = dispatchBatchAssembler;
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

    DispatchBatch dispatchBatch = dispatchBatchAssembler.createAssemble(representation);
    validateCreateDispatchTime(dispatchBatch, dispatchBatch.getDispatchCreatedAt());

    long processedItemId = representation.getDispatchProcessedItemInspections()
        .stream()
        .findFirst()
        .map(batch -> batch.getProcessedItemInspectionBatch().getProcessedItem().getId())
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
    dispatchBatch.setBuyer(buyerService.getBuyerByIdAndTenantId(representation.getBuyerId(), tenantId));
    dispatchBatch.setBillingEntity(buyerService.getBuyerEntityById(representation.getBillingEntityId()));
    dispatchBatch.setShippingEntity(buyerService.getBuyerEntityById(representation.getShippingEntityId()));

    dispatchBatch.setDispatchBatchStatus(DispatchBatch.DispatchBatchStatus.DISPATCH_IN_PROGRESS);

    dispatchBatch.getDispatchProcessedItemInspections().forEach(this::updateProcessedItemInspectionBatch);

    dispatchBatch.getDispatchProcessedItemInspections().forEach(dispatchProcessedItemInspection -> dispatchProcessedItemInspection.setDispatchBatch(dispatchBatch));

    DispatchBatch createdDispatchBatch = dispatchBatchRepository.save(dispatchBatch);
    return dispatchBatchAssembler.dissemble(createdDispatchBatch);
  }

  private void updateProcessedItemInspectionBatch(DispatchProcessedItemInspection dispatchProcessedItemInspection) {
    ProcessedItemInspectionBatch batch = processedItemInspectionBatchService.getProcessedItemInspectionBatchById(dispatchProcessedItemInspection.getProcessedItemInspectionBatch().getId());
    int updatedAvailableDispatchPieces = batch.getAvailableDispatchPiecesCount() - dispatchProcessedItemInspection.getDispatchedPiecesCount();
    batch.setAvailableDispatchPiecesCount(updatedAvailableDispatchPieces);
    int dispatchedPiecesCount;
    if (batch.getDispatchedPiecesCount() == null) {
      dispatchedPiecesCount = 0;
    } else {
      dispatchedPiecesCount = batch.getDispatchedPiecesCount();
    }

    batch.setDispatchedPiecesCount(dispatchedPiecesCount + dispatchProcessedItemInspection.getDispatchedPiecesCount());

    batch.setItemStatus(updatedAvailableDispatchPieces == 0
                        ? ItemStatus.COMPLETE_DISPATCH_IN_PROGRESS
                        : ItemStatus.PARTIAL_DISPATCH_IN_PROGRESS);
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
    validatePackagingQuantity(existingDispatchBatch, representation.getPackagingQuantity(), representation.getPerPackagingQuantity());

    existingDispatchBatch.setDispatchBatchStatus(DispatchBatch.DispatchBatchStatus.READY_TO_DISPATCH);
    existingDispatchBatch.setDispatchReadyAt(readyAtTime);
    existingDispatchBatch.setPackagingType(DispatchBatch.PackagingType.valueOf(representation.getPackagingType()));
    existingDispatchBatch.setPackagingQuantity(representation.getPackagingQuantity());
    existingDispatchBatch.setPerPackagingQuantity(representation.getPerPackagingQuantity());

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
    int calculatedTotalPieces = packagingQuantity*perPackagingQuantity;

    if (calculatedTotalPieces != totalDispatchPieces) {
        log.error("Packaging quantity {} does not match total dispatch pieces count {} for dispatch batch id={}",
            calculatedTotalPieces, totalDispatchPieces, dispatchBatch.getId());
        throw new IllegalArgumentException(
            String.format("Packaging quantity (%d) must match total dispatch pieces count (%d)",
                calculatedTotalPieces, totalDispatchPieces)
        );
    }
  }

  private void validateCreateDispatchTime(DispatchBatch dispatchBatch, LocalDateTime providedTime){
    boolean isInvalidTime = dispatchBatch.getDispatchProcessedItemInspections().stream()
        .map(dispatchProcessedItemInspection -> dispatchProcessedItemInspection.getProcessedItemInspectionBatch().getInspectionBatch().getEndAt())
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

  @Transactional
  public DispatchBatchRepresentation deleteDispatchBatch(long tenantId, long dispatchBatchId) {
    tenantService.validateTenantExists(tenantId);
    DispatchBatch dispatchBatch = getDispatchBatchById(dispatchBatchId);

    // Can only delete if batch is in DISPATCHED state
    if (dispatchBatch.getDispatchBatchStatus() != DispatchBatch.DispatchBatchStatus.DISPATCHED) {
        log.error("Cannot delete dispatch batch with id={} as it is not in DISPATCHED status", dispatchBatchId);
        throw new IllegalStateException("Cannot delete dispatch batch as it is not in DISPATCHED status");
    }

    // Revert the inspection batch changes
    for (DispatchProcessedItemInspection dispatchProcessedItemInspection : dispatchBatch.getDispatchProcessedItemInspections()) {
        // Restore the original available dispatch pieces count
      ProcessedItemInspectionBatch processedItemInspectionBatch = dispatchProcessedItemInspection.getProcessedItemInspectionBatch();
        int dispatchedPiecesCount = dispatchProcessedItemInspection.getDispatchedPiecesCount() != null ?
            dispatchProcessedItemInspection.getDispatchedPiecesCount() : 0;
      processedItemInspectionBatch.setAvailableDispatchPiecesCount(
          processedItemInspectionBatch.getAvailableDispatchPiecesCount() + dispatchedPiecesCount
        );
      processedItemInspectionBatch.setDispatchedPiecesCount(processedItemInspectionBatch.getDispatchedPiecesCount()-dispatchedPiecesCount);
      dispatchProcessedItemInspection.setDeleted(true);
      dispatchProcessedItemInspection.setDeletedAt(LocalDateTime.now());
      processedItemInspectionBatch.setItemStatus(ItemStatus.DISPATCH_DELETED_QUALITY);
    }

    // Soft delete the dispatch batch
    dispatchBatch.setDeleted(true);
    dispatchBatch.setDeletedAt(LocalDateTime.now());
    dispatchBatch.getProcessedItemDispatchBatch().setDeleted(true);
    dispatchBatch.getProcessedItemDispatchBatch().setDeletedAt(LocalDateTime.now());
    DispatchBatch deletedDispatchBatch = dispatchBatchRepository.save(dispatchBatch);

    log.info("Successfully deleted dispatch batch with id={}", dispatchBatchId);
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
}
