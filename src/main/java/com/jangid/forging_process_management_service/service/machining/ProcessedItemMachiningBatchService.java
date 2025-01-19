package com.jangid.forging_process_management_service.service.machining;

import com.jangid.forging_process_management_service.entities.machining.ProcessedItemMachiningBatch;
import com.jangid.forging_process_management_service.entities.product.Item;
import com.jangid.forging_process_management_service.exception.forging.ForgeNotFoundException;
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

}
