package com.jangid.forging_process_management_service.service.machining;

import com.jangid.forging_process_management_service.entities.machining.ProcessedItemMachiningBatch;
import com.jangid.forging_process_management_service.entities.product.Item;
import com.jangid.forging_process_management_service.exception.forging.ForgeNotFoundException;
import com.jangid.forging_process_management_service.exception.product.ItemNotFoundException;
import com.jangid.forging_process_management_service.repositories.machining.ProcessedItemMachiningBatchRepository;
import com.jangid.forging_process_management_service.repositories.product.ItemRepository;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
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
  private ItemRepository itemRepository;

  public List<ProcessedItemMachiningBatch> getProcessedItemMachiningBatchesEligibleForReworkMachining(long tenantId) {
    List<Item> items = itemRepository.findByTenantIdAndDeletedFalseOrderByCreatedAtDesc(tenantId);

    return items.stream()
        .flatMap(item -> {
          return getProcessedItemMachiningBatchesEligibleForReworkMachining().stream()
              .filter(processedItemHeatTreatmentBatch ->
                          processedItemHeatTreatmentBatch.getProcessedItem().getItem().getId().equals(item.getId()));
        })
        .toList();
  }

  public List<ProcessedItemMachiningBatch> getProcessedItemMachiningBatchesEligibleForReworkMachining(){
    List<ProcessedItemMachiningBatch>  processedItemMachiningBatches = processedItemMachiningBatchRepository.findMachiningBatchesWithAvailableReworkPieces();
    return processedItemMachiningBatches;
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
  public void save(ProcessedItemMachiningBatch processedItemMachiningBatch) {
    processedItemMachiningBatchRepository.save(processedItemMachiningBatch);
  }

  /**
   * Retrieves the list of ProcessedItemMachiningBatch eligible for rework machining for a given Item.
   *
   * @param itemId the item for which eligible ProcessedItemMachiningBatch are to be retrieved
   * @return List of ProcessedItemMachiningBatch eligible for rework machining
   */
  public List<ProcessedItemMachiningBatch> getProcessedItemMachiningBatchesForItem(long itemId) {
    return processedItemMachiningBatchRepository.findMachiningBatchesByItemId(itemId);
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
