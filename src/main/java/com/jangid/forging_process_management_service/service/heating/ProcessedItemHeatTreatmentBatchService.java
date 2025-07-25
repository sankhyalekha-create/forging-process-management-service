package com.jangid.forging_process_management_service.service.heating;

import com.jangid.forging_process_management_service.entities.heating.ProcessedItemHeatTreatmentBatch;
import com.jangid.forging_process_management_service.repositories.heating.ProcessedItemHeatTreatmentBatchRepository;

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
   * Get the ProcessedItemHeatTreatmentBatch ID by itsprocessed item ID
   * @param processedItemId The processed item ID to search for
   * @return The ProcessedItemHeatTreatmentBatch ID that has the given processedItemId, or null if not found
   */
  @Transactional(readOnly = true)
  public Long getProcessedItemHeatTreatmentBatchIdByProcessedItemId(Long processedItemId) {
    if (processedItemId == null) {
      log.warn("processed item ID is null, cannot retrieve ProcessedItemHeatTreatmentBatch ID");
      return null;
    }

    Optional<ProcessedItemHeatTreatmentBatch> processedItemOptional = 
        processedItemHeatTreatmentBatchRepository.findByIdAndDeletedFalse(processedItemId);
    
    if (processedItemOptional.isEmpty()) {
      log.warn("No ProcessedItemHeatTreatmentBatch found for processedItemId={}", processedItemId);
      return null;
    }
    
    ProcessedItemHeatTreatmentBatch processedItem = processedItemOptional.get();
    Long processedItemHeatTreatmentBatchId = processedItem.getId();
    
    log.info("Found ProcessedItemHeatTreatmentBatch ID={} for processedItemId={}",
             processedItemHeatTreatmentBatchId, processedItemId);
    
    return processedItemHeatTreatmentBatchId;
  }


}
