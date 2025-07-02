package com.jangid.forging_process_management_service.service.dispatch;


import com.jangid.forging_process_management_service.entities.dispatch.ProcessedItemDispatchBatch;
import com.jangid.forging_process_management_service.exception.dispatch.ProcessedItemDispatchBatchNotFound;
import com.jangid.forging_process_management_service.repositories.dispatch.ProcessedItemDispatchBatchRepository;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;


@Slf4j
@Service
public class ProcessedItemDispatchBatchService {

  @Autowired
  private ProcessedItemDispatchBatchRepository processedItemDispatchBatchRepository;

  public ProcessedItemDispatchBatch getProcessedItemDispatchBatchById(long id){
    Optional<ProcessedItemDispatchBatch> processedItemDispatchBatchOptional = processedItemDispatchBatchRepository.findByIdAndDeletedFalse(id);
    if(processedItemDispatchBatchOptional.isEmpty()){
      log.error("ProcessedItemDispatchBatch is not found for id={}", id);
      throw new ProcessedItemDispatchBatchNotFound("ProcessedItemDispatchBatch is not found for id=" + id);

    }
    return processedItemDispatchBatchOptional.get();
  }

  /**
   * Get the ProcessedItemDispatchBatch ID by its previous operation processed item ID
   * @param previousOperationProcessedItemId The previous operation processed item ID to search for
   * @return The ProcessedItemDispatchBatch ID that has the given previousOperationProcessedItemId, or null if not found
   */
  @Transactional(readOnly = true)
  public Long getProcessedItemDispatchBatchIdByPreviousOperationProcessedItemId(Long previousOperationProcessedItemId) {
    if (previousOperationProcessedItemId == null) {
      log.warn("Previous operation processed item ID is null, cannot retrieve ProcessedItemDispatchBatch ID");
      return null;
    }

    Optional<ProcessedItemDispatchBatch> processedItemOptional = 
        processedItemDispatchBatchRepository.findByPreviousOperationProcessedItemIdAndDeletedFalse(previousOperationProcessedItemId);
    
    if (processedItemOptional.isEmpty()) {
      log.warn("No ProcessedItemDispatchBatch found for previousOperationProcessedItemId={}", previousOperationProcessedItemId);
      return null;
    }
    
    ProcessedItemDispatchBatch processedItem = processedItemOptional.get();
    Long processedItemDispatchBatchId = processedItem.getId();
    
    log.info("Found ProcessedItemDispatchBatch ID={} for previousOperationProcessedItemId={}", 
             processedItemDispatchBatchId, previousOperationProcessedItemId);
    
    return processedItemDispatchBatchId;
  }

}
