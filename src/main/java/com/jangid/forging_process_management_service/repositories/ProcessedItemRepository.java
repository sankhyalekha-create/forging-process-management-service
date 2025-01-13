package com.jangid.forging_process_management_service.repositories;

import com.jangid.forging_process_management_service.entities.ProcessedItem;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProcessedItemRepository extends CrudRepository<ProcessedItem, Long> {
  List<ProcessedItem> findByItemIdAndDeletedFalse(long itemId);
  Optional<ProcessedItem> findByIdAndDeletedFalse(long id);

  @Query("SELECT pi FROM ProcessedItem pi " +
         "WHERE pi.item.id = :itemId " +
         "AND pi.availableForgePiecesCountForHeat > 0 " +
         "AND pi.deleted = false")
  List<ProcessedItem> findAvailableForgePiecesByItemId(@Param("itemId") Long itemId);

}
