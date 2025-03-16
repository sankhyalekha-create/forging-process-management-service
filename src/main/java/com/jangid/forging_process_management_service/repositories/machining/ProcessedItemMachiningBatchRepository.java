package com.jangid.forging_process_management_service.repositories.machining;

import com.jangid.forging_process_management_service.entities.machining.ProcessedItemMachiningBatch;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProcessedItemMachiningBatchRepository extends CrudRepository<ProcessedItemMachiningBatch, Long> {

  Optional<ProcessedItemMachiningBatch> findByIdAndDeletedFalse(Long id);

  @Query(value = "SELECT * FROM processed_item_machining_batch " +
                 "WHERE rework_pieces_count > 0 " +
                 "AND deleted = false ",
//                 "AND CAST(item_status AS INTEGER) IN (11, 13)",
         nativeQuery = true)
  List<ProcessedItemMachiningBatch> findMachiningBatchesWithAvailableReworkPieces();

//  @Query(value = "SELECT * FROM processed_item_machining_batch pim " +
//                 "JOIN processed_item pi ON pim.processed_item_id = pi.id " +
//                 "WHERE pim.rework_pieces_count > 0 " +
//                 "AND pim.deleted = false " +
//                 "AND CAST(pim.item_status AS INTEGER) IN (11, 13) " +
//                 "AND pi.item_id = :itemId",
//         nativeQuery = true)
//  List<ProcessedItemMachiningBatch> findMachiningBatchesByItemId(@Param("itemId") Long itemId);

  @Query("SELECT pim FROM ProcessedItemMachiningBatch pim " +
         "JOIN pim.processedItem pi " +
         "WHERE pim.availableInspectionBatchPiecesCount > 0 " +
         "AND pim.deleted = false " +
//         "AND CAST(pim.itemStatus AS INTEGER) IN (9, 10, 11, 12, 13) " +
         "AND pi.item.id = :itemId")
  List<ProcessedItemMachiningBatch> findMachiningBatchesByItemIdAvailableForInspection(@Param("itemId") Long itemId);


}
