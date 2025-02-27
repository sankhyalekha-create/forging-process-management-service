package com.jangid.forging_process_management_service.service;

import com.jangid.forging_process_management_service.entities.ProcessedItem;
import com.jangid.forging_process_management_service.entities.product.Item;
import com.jangid.forging_process_management_service.repositories.ProcessedItemRepository;
import com.jangid.forging_process_management_service.repositories.product.ItemRepository;
import com.jangid.forging_process_management_service.service.heating.ProcessedItemHeatTreatmentBatchService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class ProcessedItemService {

  @Autowired
  private ProcessedItemRepository processedItemRepository;

  @Autowired
  private ProcessedItemHeatTreatmentBatchService processedItemHeatTreatmentBatchService;

  @Autowired
  private ItemRepository itemRepository;

  public List<ProcessedItem> getProcessedItemList(long tenantId) {
    List<Item> items = itemRepository.findByTenantIdAndDeletedFalseOrderByCreatedAtDesc(tenantId);

    return items.stream()
        .flatMap(item -> {
          List<ProcessedItem> processedItems = processedItemRepository.findByItemIdAndDeletedFalse(item.getId());
          if (processedItems.isEmpty()) {
            String errorMessage = String.format(
                "ProcessedItem does not exist for item=%d having name=%s for tenant=%d",
                item.getId(), item.getItemName(), tenantId
            );
            log.error(errorMessage);
            throw new RuntimeException(errorMessage);
          }
          return processedItems.stream();
        })
        .toList();
  }

  public List<ProcessedItem> getProcessedItemListEligibleForHeatTreatment(long itemId) {
    return processedItemRepository.findAvailableForgePiecesByItemId(itemId);
  }

  public ProcessedItem getProcessedItemById(long processedItemId){
    Optional<ProcessedItem> processedItemOptional = processedItemRepository.findByIdAndDeletedFalse(processedItemId);
    if(processedItemOptional.isEmpty()){
      log.error("ProcessedItem does not exist for processedItemId="+processedItemId);
      throw new RuntimeException("ProcessedItem does not exist for processedItemId="+processedItemId);
    }
    return processedItemOptional.get();
  }
}
