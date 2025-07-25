package com.jangid.forging_process_management_service.service.vendor;


import com.jangid.forging_process_management_service.entities.vendor.ProcessedItemVendorDispatchBatch;
import com.jangid.forging_process_management_service.repositories.vendor.ProcessedItemVendorDispatchBatchRepository;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;


@Slf4j
@Service
public class ProcessedItemVendorDispatchBatchService {

  @Autowired
  private ProcessedItemVendorDispatchBatchRepository processedItemVendorDispatchBatchRepository;

  /**
   * Get the ProcessedItemVendorDispatchBatch ID by its processed item ID
   * @param processedItemId The processed item ID to search for
   * @return The ProcessedItemVendorDispatchBatch ID that has the given processedItemId, or null if not found
   */
  @Transactional(readOnly = true)
  public Long getProcessedItemVendorDispatchBatchIdByProcessedItemId(Long processedItemId) {
    if (processedItemId == null) {
      log.warn("processed item ID is null, cannot retrieve ProcessedItemVendorDispatchBatch ID");
      return null;
    }

    Optional<ProcessedItemVendorDispatchBatch> processedItemOptional =
        processedItemVendorDispatchBatchRepository.findByIdAndDeletedFalse(processedItemId);
    
    if (processedItemOptional.isEmpty()) {
      log.warn("No ProcessedItemVendorDispatchBatch found for processedItemId={}", processedItemId);
      return null;
    }
    
    ProcessedItemVendorDispatchBatch processedItem = processedItemOptional.get();
    Long processedItemVendorDispatchBatchId = processedItem.getId();
    
    log.info("Found ProcessedItemVendorDispatchBatch ID={} for processedItemId={}",
             processedItemVendorDispatchBatchId, processedItemId);
    
    return processedItemVendorDispatchBatchId;
  }

}
