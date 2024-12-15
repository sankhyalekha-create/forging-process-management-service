package com.jangid.forging_process_management_service.service;

import com.jangid.forging_process_management_service.entities.ProcessedItem;
import com.jangid.forging_process_management_service.entities.product.Item;
import com.jangid.forging_process_management_service.entities.product.ItemStatus;
import com.jangid.forging_process_management_service.repositories.ProcessedItemRepository;
import com.jangid.forging_process_management_service.repositories.product.ItemRepository;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class ProcessedItemService {

  @Autowired
  private ProcessedItemRepository processedItemRepository;

  @Autowired
  private ItemRepository itemRepository;

  public List<ProcessedItem> getProcessedItemList(long tenantId){
    List<Item> items = itemRepository.findByTenantIdAndDeletedFalseOrderByCreatedAtDesc(tenantId);

    return items.stream()
        .map(item -> processedItemRepository.findByItemIdAndDeletedFalse(item.getId())
            .orElseThrow(() -> {
              String errorMessage = String.format(
                  "ProcessedItem does not exist for item=%d having name=%s for tenant=%d",
                  item.getId(), item.getItemName(), tenantId
              );
              log.error(errorMessage);
              return new RuntimeException(errorMessage);
            }))
        .toList();
  }

  public List<ProcessedItem> getForgedProcessedItemList(long tenantId){
    List<Item> items = itemRepository.findByTenantIdAndDeletedFalseOrderByCreatedAtDesc(tenantId);

    return items.stream()
        .map(item -> processedItemRepository.findByItemIdAndDeletedFalse(item.getId()).filter(processedItem -> processedItem.getItemStatus() == ItemStatus.FORGING_COMPLETED)
            .orElseThrow(() -> {
              String errorMessage = String.format(
                  "ProcessedItem having FORGING_COMPLETED status does not exist for item=%d having name=%s for tenant=%d",
                  item.getId(), item.getItemName(), tenantId
              );
              log.error(errorMessage);
              return new RuntimeException(errorMessage);
            }))
        .toList();
  }
}
