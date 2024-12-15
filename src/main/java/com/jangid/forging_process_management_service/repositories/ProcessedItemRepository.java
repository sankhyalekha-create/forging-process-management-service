package com.jangid.forging_process_management_service.repositories;

import com.jangid.forging_process_management_service.entities.ProcessedItem;

import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface ProcessedItemRepository extends CrudRepository<ProcessedItem, Long> {
  Optional<ProcessedItem> findByItemIdAndDeletedFalse(long itemId);
}
