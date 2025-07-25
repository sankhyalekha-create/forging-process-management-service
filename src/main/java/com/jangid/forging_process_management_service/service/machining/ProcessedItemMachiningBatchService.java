package com.jangid.forging_process_management_service.service.machining;

import com.jangid.forging_process_management_service.entities.machining.ProcessedItemMachiningBatch;
import com.jangid.forging_process_management_service.exception.forging.ForgeNotFoundException;
import com.jangid.forging_process_management_service.repositories.machining.ProcessedItemMachiningBatchRepository;
import com.jangid.forging_process_management_service.service.product.ItemService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class ProcessedItemMachiningBatchService {

  @Autowired
  private ProcessedItemMachiningBatchRepository processedItemMachiningBatchRepository;

  @Autowired
  @Lazy
  private ItemService itemService;

  public List<ProcessedItemMachiningBatch> getProcessedItemMachiningBatchesEligibleForReworkMachining(long tenantId, long itemId) {
    itemService.isItemExistsForTenant(itemId, tenantId);
    return processedItemMachiningBatchRepository.findMachiningBatchesWithAvailableReworkPieces(itemId);
  }

  // getProcessedItemMachiningBatchById
  public ProcessedItemMachiningBatch getProcessedItemMachiningBatchById(long id) {
    Optional<ProcessedItemMachiningBatch> processedItemMachiningBatchOptional = processedItemMachiningBatchRepository.findByIdAndDeletedFalse(id);
    if (processedItemMachiningBatchOptional.isEmpty()) {
      log.error("ProcessedItemMachiningBatch does not exists for processedItemMachiningBatchId={}", id);
      throw new ForgeNotFoundException("ProcessedItemMachiningBatch does not exists for processedItemMachiningBatchId=" + id);
    }
    return processedItemMachiningBatchOptional.get();
  }

  @Transactional
  public ProcessedItemMachiningBatch save(ProcessedItemMachiningBatch processedItemMachiningBatch) {
    return processedItemMachiningBatchRepository.save(processedItemMachiningBatch);
  }

  /**
   * Retrieves the list of ProcessedItemMachiningBatch eligible for rework machining for a given Item.
   *
   * @param itemId the item for which eligible ProcessedItemMachiningBatch are to be retrieved
   * @return List of ProcessedItemMachiningBatch eligible for rework machining
   */
  public List<ProcessedItemMachiningBatch> getProcessedItemMachiningBatchesForItemAvailableForInspection(long itemId) {
    return processedItemMachiningBatchRepository.findMachiningBatchesByItemIdAvailableForInspection(itemId);
  }

  /**
   * Retrieves a ProcessedItemMachiningBatch by its machining batch ID.
   *
   * @param machiningBatchId the ID of the machining batch
   * @return ProcessedItemMachiningBatch associated with the given machining batch ID
   */
  public ProcessedItemMachiningBatch getProcessedItemMachiningBatchByMachiningBatchId(long machiningBatchId) {
    Optional<ProcessedItemMachiningBatch> processedItemMachiningBatchOptional = 
        processedItemMachiningBatchRepository.findByMachiningBatchIdAndDeletedFalse(machiningBatchId);
    
    if (processedItemMachiningBatchOptional.isEmpty()) {
      log.error("ProcessedItemMachiningBatch does not exist for machiningBatchId={}", machiningBatchId);
      throw new ForgeNotFoundException("ProcessedItemMachiningBatch does not exist for machiningBatchId=" + machiningBatchId);
    }
    
    return processedItemMachiningBatchOptional.get();
  }

  /**
   * Get the ProcessedItemMachiningBatch ID by its processed item ID
   * @param processedItemId The processed item ID to search for
   * @return The ProcessedItemMachiningBatch ID that has the given processedItemId, or null if not found
   */
  @Transactional(readOnly = true)
  public Long getProcessedItemMachiningBatchIdByProcessedItemId(Long processedItemId) {
    if (processedItemId == null) {
      log.warn("processed item ID is null, cannot retrieve ProcessedItemMachiningBatch ID");
      return null;
    }

    Optional<ProcessedItemMachiningBatch> processedItemOptional = 
        processedItemMachiningBatchRepository.findByIdAndDeletedFalse(processedItemId);
    
    if (processedItemOptional.isEmpty()) {
      log.warn("No ProcessedItemMachiningBatch found for processedItemId={}", processedItemId);
      return null;
    }
    
    ProcessedItemMachiningBatch processedItem = processedItemOptional.get();
    Long processedItemMachiningBatchId = processedItem.getId();
    
    log.info("Found ProcessedItemMachiningBatch ID={} for processedItemId={}",
             processedItemMachiningBatchId, processedItemId);
    
    return processedItemMachiningBatchId;
  }

//  public List<ProcessedItemMachiningBatch> getProcessedItemMachiningBatchesForItem(long itemId) {
//    List<Object[]> results = processedItemMachiningBatchRepository.findMachiningBatchesByItemId(itemId);
//
//    List<ProcessedItemMachiningBatch> processedItemMachiningBatches = new ArrayList<>();
//    for (Object[] result : results) {
//      ProcessedItemMachiningBatch batch = new ProcessedItemMachiningBatch();
//      batch.setId(((Number) result[0]).longValue());
//      batch.setReworkPiecesCount((Integer) result[1]);
//      batch.setDeleted((Boolean) result[2]);
//      batch.setItemStatus(ItemStatus.values()[Integer.valueOf((String)result[3])]); // Adjust if `ItemStatus` is an enum
//      batch.setMachiningBatchPiecesCount((Integer) result[4]);
//      batch.setAvailableMachiningBatchPiecesCount((Integer) result[5]);
//      batch.setActualMachiningBatchPiecesCount((Integer) result[6]);
//      batch.setRejectMachiningBatchPiecesCount((Integer) result[7]);
//      batch.setInitialInspectionBatchPiecesCount((Integer) result[8]);
//      batch.setAvailableInspectionBatchPiecesCount((Integer) result[9]);
//
//      ProcessedItem processedItem = new ProcessedItem();
//      processedItem.setId(((Number) result[13]).longValue());
//      processedItem.setExpectedForgePiecesCount((Integer) result[14]);
//      processedItem.setActualForgePiecesCount((Integer) result[15]);
//      processedItem.setAvailableForgePiecesCountForHeat((Integer) result[16]);
//      processedItem.setDeleted((Boolean) result[17]);
//
//      batch.setProcessedItem(processedItem);
//
//      processedItemMachiningBatches.add(batch);
//    }
//
//    return processedItemMachiningBatches;
//  }

}
