package com.jangid.forging_process_management_service.service.heating;

import com.jangid.forging_process_management_service.entities.heating.ProcessedItemHeatTreatmentBatch;
import com.jangid.forging_process_management_service.entities.product.Item;
import com.jangid.forging_process_management_service.exception.forging.ForgeNotFoundException;
import com.jangid.forging_process_management_service.repositories.heating.ProcessedItemHeatTreatmentBatchRepository;
import com.jangid.forging_process_management_service.repositories.product.ItemRepository;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class ProcessedItemHeatTreatmentBatchService {

  @Autowired
  private ProcessedItemHeatTreatmentBatchRepository processedItemHeatTreatmentBatchRepository;

  @Transactional
  public void save(ProcessedItemHeatTreatmentBatch processedItemHeatTreatmentBatch) {
    processedItemHeatTreatmentBatchRepository.save(processedItemHeatTreatmentBatch);
  }

  public List<ProcessedItemHeatTreatmentBatch> getProcessedItemHeatTreatmentBatchesEligibleForMachining(long itemId){
    return processedItemHeatTreatmentBatchRepository.findBatchesWithAvailableMachiningPieces(itemId);
  }

  /**
   * Get the ProcessedItemHeatTreatmentBatch ID by its previous operation processed item ID
   * @param previousOperationProcessedItemId The previous operation processed item ID to search for
   * @return The ProcessedItemHeatTreatmentBatch ID that has the given previousOperationProcessedItemId, or null if not found
   */
  @Transactional(readOnly = true)
  public Long getProcessedItemHeatTreatmentBatchIdByPreviousOperationProcessedItemId(Long previousOperationProcessedItemId) {
    if (previousOperationProcessedItemId == null) {
      log.warn("Previous operation processed item ID is null, cannot retrieve ProcessedItemHeatTreatmentBatch ID");
      return null;
    }

    Optional<ProcessedItemHeatTreatmentBatch> processedItemOptional = 
        processedItemHeatTreatmentBatchRepository.findByPreviousOperationProcessedItemIdAndDeletedFalse(previousOperationProcessedItemId);
    
    if (processedItemOptional.isEmpty()) {
      log.warn("No ProcessedItemHeatTreatmentBatch found for previousOperationProcessedItemId={}", previousOperationProcessedItemId);
      return null;
    }
    
    ProcessedItemHeatTreatmentBatch processedItem = processedItemOptional.get();
    Long processedItemHeatTreatmentBatchId = processedItem.getId();
    
    log.info("Found ProcessedItemHeatTreatmentBatch ID={} for previousOperationProcessedItemId={}", 
             processedItemHeatTreatmentBatchId, previousOperationProcessedItemId);
    
    return processedItemHeatTreatmentBatchId;
  }


}
