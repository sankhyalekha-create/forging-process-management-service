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

  @Autowired
  private ItemRepository itemRepository;

  public ProcessedItemHeatTreatmentBatch getProcessedItemHeatTreatmentBatchById(long id) {
    Optional<ProcessedItemHeatTreatmentBatch> processedItemHeatTreatmentBatchOptional = processedItemHeatTreatmentBatchRepository.findByIdAndDeletedFalse(id);
    if (processedItemHeatTreatmentBatchOptional.isEmpty()) {
      log.error("ProcessedItemHeatTreatmentBatch does not exists for processedItemHeatTreatmentBatchId={}", id);
      throw new ForgeNotFoundException("ProcessedItemHeatTreatmentBatch does not exists for processedItemHeatTreatmentBatchId=" + id);
    }
    return processedItemHeatTreatmentBatchOptional.get();
  }

  @Transactional
  public void save(ProcessedItemHeatTreatmentBatch processedItemHeatTreatmentBatch) {
    processedItemHeatTreatmentBatchRepository.save(processedItemHeatTreatmentBatch);
  }

  public List<ProcessedItemHeatTreatmentBatch> getProcessedItemHeatTreatmentBatchesEligibleForMachining(long tenantId) {
    List<Item> items = itemRepository.findByTenantIdAndDeletedFalseOrderByCreatedAtDesc(tenantId);

    return items.stream()
        .flatMap(item -> {
          return getProcessedItemHeatTreatmentBatchesEligibleForMachining().stream()
              .filter(processedItemHeatTreatmentBatch ->
                          processedItemHeatTreatmentBatch.getProcessedItem().getItem().getId().equals(item.getId()));
        })
        .toList();
  }

  public List<ProcessedItemHeatTreatmentBatch> getProcessedItemHeatTreatmentBatchesEligibleForMachining(){
    return processedItemHeatTreatmentBatchRepository.findBatchesWithAvailableMachiningPieces();
  }

}
