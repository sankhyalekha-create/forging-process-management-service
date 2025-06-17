package com.jangid.forging_process_management_service.service.dispatch;

import com.jangid.forging_process_management_service.assemblers.dispatch.DispatchBatchAssembler;
import com.jangid.forging_process_management_service.entities.dispatch.DispatchBatch;
import com.jangid.forging_process_management_service.entities.dispatch.DispatchPackage;
import com.jangid.forging_process_management_service.entities.dispatch.DispatchProcessedItemInspection;
import com.jangid.forging_process_management_service.entities.dispatch.ProcessedItemDispatchBatch;
import com.jangid.forging_process_management_service.entities.product.ItemStatus;
import com.jangid.forging_process_management_service.entities.quality.ProcessedItemInspectionBatch;
import com.jangid.forging_process_management_service.entitiesRepresentation.dispatch.DispatchBatchListRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.dispatch.DispatchBatchRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.dispatch.DispatchPackageRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.dispatch.DispatchStatisticsRepresentation;
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
import java.util.Collections;
import java.util.ArrayList;

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
    
    // Check if this batch number was previously used and deleted
    if (isDispatchBatchNumberPreviouslyUsed(representation.getDispatchBatchNumber(), tenantId)) {
      log.warn("Dispatch batch with batch number: {} was previously used and deleted for tenant: {}", 
               representation.getDispatchBatchNumber(), tenantId);
    }

    DispatchBatch dispatchBatch = dispatchBatchAssembler.createAssemble(representation);
    validateCreateDispatchTime(dispatchBatch, dispatchBatch.getDispatchCreatedAt());

    ProcessedItemDispatchBatch processedItemDispatchBatch = ProcessedItemDispatchBatch.builder()
        .dispatchBatch(dispatchBatch)
        .itemStatus(ItemStatus.COMPLETE_DISPATCH_IN_PROGRESS)
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
    
    // Handle uniform vs non-uniform packaging
    boolean useUniformPackaging = representation.getUseUniformPackaging() != null ? 
                                 representation.getUseUniformPackaging() : true;
    existingDispatchBatch.setUseUniformPackaging(useUniformPackaging);
    
    if (useUniformPackaging) {
        // Traditional uniform packaging validation
        validatePackagingQuantity(existingDispatchBatch, representation.getPackagingQuantity(), representation.getPerPackagingQuantity());
        existingDispatchBatch.setPackagingType(DispatchBatch.PackagingType.valueOf(representation.getPackagingType()));
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
        
        DispatchBatch.PackagingType packagingType = DispatchBatch.PackagingType.valueOf(representation.getPackagingType());
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
    return dispatchBatchAssembler.dissemble(updatedDispatchBatch);
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

    // Mark dispatch packages as deleted
    if (dispatchBatch.getDispatchPackages() != null) {
      for (DispatchPackage dispatchPackage : dispatchBatch.getDispatchPackages()) {
        dispatchPackage.setDeleted(true);
        dispatchPackage.setDeletedAt(LocalDateTime.now());
      }
    }

    LocalDateTime now = LocalDateTime.now();
    
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

    // Soft delete the dispatch batch
    dispatchBatch.setDeleted(true);
    dispatchBatch.setDeletedAt(now);
    dispatchBatch.getProcessedItemDispatchBatch().setDeleted(true);
    dispatchBatch.getProcessedItemDispatchBatch().setDeletedAt(now);
    DispatchBatch deletedDispatchBatch = dispatchBatchRepository.save(dispatchBatch);

    log.info("Successfully deleted dispatch batch with id={}, original batch number={}", 
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
   * @return List of DispatchBatchRepresentation
   */
  public List<DispatchBatchRepresentation> getDispatchBatchesByProcessedItemDispatchBatchIds(List<Long> processedItemDispatchBatchIds, Long tenantId) {
    if (processedItemDispatchBatchIds == null || processedItemDispatchBatchIds.isEmpty()) {
      log.info("No processed item dispatch batch IDs provided, returning empty list");
      return Collections.emptyList();
    }

    log.info("Getting dispatch batches for {} processed item dispatch batch IDs for tenant {}", processedItemDispatchBatchIds.size(), tenantId);
    
    List<DispatchBatch> dispatchBatches = dispatchBatchRepository.findByProcessedItemDispatchBatchIdInAndDeletedFalse(processedItemDispatchBatchIds);
    
    // Filter and validate that all dispatch batches belong to the tenant
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
          validDispatchBatches.add(dispatchBatchAssembler.dissemble(dispatchBatch));
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
    
    log.info("Found {} valid dispatch batches out of {} requested processed item dispatch batch IDs", validDispatchBatches.size(), processedItemDispatchBatchIds.size());
    return validDispatchBatches;
  }
}
