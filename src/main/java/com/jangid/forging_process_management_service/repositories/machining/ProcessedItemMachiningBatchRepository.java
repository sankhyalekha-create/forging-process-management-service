package com.jangid.forging_process_management_service.repositories.machining;

import com.jangid.forging_process_management_service.entities.machining.ProcessedItemMachiningBatch;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProcessedItemMachiningBatchRepository extends CrudRepository<ProcessedItemMachiningBatch, Long> {

  Optional<ProcessedItemMachiningBatch> findByIdAndDeletedFalse(Long id);

  Optional<ProcessedItemMachiningBatch> findByMachiningBatchIdAndDeletedFalse(Long machiningBatchId);

  @Query(value = "SELECT pimb.* FROM processed_item_machining_batch pimb " +
                 "WHERE pimb.rework_pieces_count_available_for_rework > 0 " +
                 "AND pimb.deleted = false " +
                 "AND pimb.item_id = :itemId",
         nativeQuery = true)
  List<ProcessedItemMachiningBatch> findMachiningBatchesWithAvailableReworkPieces(@Param("itemId") Long itemId);

//  @Query(value = "SELECT * FROM processed_item_machining_batch pim " +
//                 "JOIN processed_item pi ON pim.processed_item_id = pi.id " +
//                 "WHERE pim.rework_pieces_count > 0 " +
//                 "AND pim.deleted = false " +
//                 "AND CAST(pim.item_status AS INTEGER) IN (11, 13) " +
//                 "AND pi.item_id = :itemId",
//         nativeQuery = true)
//  List<ProcessedItemMachiningBatch> findMachiningBatchesByItemId(@Param("itemId") Long itemId);

  @Query("SELECT pim FROM ProcessedItemMachiningBatch pim " +
         "WHERE pim.availableInspectionBatchPiecesCount > 0 " +
         "AND pim.deleted = false " +
//         "AND CAST(pim.itemStatus AS INTEGER) IN (9, 10, 11, 12, 13) " +
         "AND pim.item.id = :itemId")
  List<ProcessedItemMachiningBatch> findMachiningBatchesByItemIdAvailableForInspection(@Param("itemId") Long itemId);

}
