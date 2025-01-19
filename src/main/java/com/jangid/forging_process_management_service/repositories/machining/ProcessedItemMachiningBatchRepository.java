package com.jangid.forging_process_management_service.repositories.machining;

import com.jangid.forging_process_management_service.entities.machining.ProcessedItemMachiningBatch;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface ProcessedItemMachiningBatchRepository extends CrudRepository<ProcessedItemMachiningBatch, Long> {

  Optional<ProcessedItemMachiningBatch> findByIdAndDeletedFalse(Long id);

  @Query(value = "SELECT * FROM processed_item_machining_batch " +
                 "WHERE rework_pieces_count > 0 " +
                 "AND deleted = false " +
                 "AND CAST(item_status AS INTEGER) IN (11, 13)",
         nativeQuery = true)
  List<ProcessedItemMachiningBatch> findMachiningBatchesWithAvailableReworkPieces();

}
