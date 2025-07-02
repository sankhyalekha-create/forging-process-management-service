package com.jangid.forging_process_management_service.service.quality;

import com.jangid.forging_process_management_service.entities.quality.ProcessedItemInspectionBatch;
import com.jangid.forging_process_management_service.exception.product.ItemNotFoundException;
import com.jangid.forging_process_management_service.exception.quality.ProcessedItemInspectionBatchNotFound;
import com.jangid.forging_process_management_service.repositories.product.ItemRepository;
import com.jangid.forging_process_management_service.repositories.quality.ProcessedItemInspectionBatchRepository;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class ProcessedItemInspectionBatchService {

  @Autowired
  private ProcessedItemInspectionBatchRepository processedItemInspectionBatchRepository;

  public ProcessedItemInspectionBatch getProcessedItemInspectionBatchById(long id){
    Optional<ProcessedItemInspectionBatch> processedItemInspectionBatchOptional = processedItemInspectionBatchRepository.findByIdAndDeletedFalse(id);
    if(processedItemInspectionBatchOptional.isEmpty()){
      log.error("ProcessedItemInspectionBatch is not found for id={}", id);
      throw new ProcessedItemInspectionBatchNotFound("ProcessedItemInspectionBatch is not found for id=" + id);
    }
    return processedItemInspectionBatchOptional.get();
  }

  public List<ProcessedItemInspectionBatch> getProcessedItemInspectionBatchesForItem(long itemId) {
    return processedItemInspectionBatchRepository.findInspectionBatchesByItemId(itemId);
  }

  /**
   * Get the ProcessedItemInspectionBatch ID by its previous operation processed item ID
   * @param previousOperationProcessedItemId The previous operation processed item ID to search for
   * @return The ProcessedItemInspectionBatch ID that has the given previousOperationProcessedItemId, or null if not found
   */
  @Transactional(readOnly = true)
  public Long getProcessedItemInspectionBatchIdByPreviousOperationProcessedItemId(Long previousOperationProcessedItemId) {
    if (previousOperationProcessedItemId == null) {
      log.warn("Previous operation processed item ID is null, cannot retrieve ProcessedItemInspectionBatch ID");
      return null;
    }

    Optional<ProcessedItemInspectionBatch> processedItemOptional = 
        processedItemInspectionBatchRepository.findByPreviousOperationProcessedItemIdAndDeletedFalse(previousOperationProcessedItemId);
    
    if (processedItemOptional.isEmpty()) {
      log.warn("No ProcessedItemInspectionBatch found for previousOperationProcessedItemId={}", previousOperationProcessedItemId);
      return null;
    }
    
    ProcessedItemInspectionBatch processedItem = processedItemOptional.get();
    Long processedItemInspectionBatchId = processedItem.getId();
    
    log.info("Found ProcessedItemInspectionBatch ID={} for previousOperationProcessedItemId={}", 
             processedItemInspectionBatchId, previousOperationProcessedItemId);
    
    return processedItemInspectionBatchId;
  }
}
