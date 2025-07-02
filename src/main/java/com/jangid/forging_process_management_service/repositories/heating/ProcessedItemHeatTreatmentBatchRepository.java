package com.jangid.forging_process_management_service.repositories.heating;

import com.jangid.forging_process_management_service.entities.heating.ProcessedItemHeatTreatmentBatch;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProcessedItemHeatTreatmentBatchRepository extends CrudRepository<ProcessedItemHeatTreatmentBatch, Long> {

  Optional<ProcessedItemHeatTreatmentBatch> findByIdAndDeletedFalse(Long id);

  @Query(value = "SELECT pihb.* FROM processed_item_heat_treatment_batch pihb " +
                 "JOIN processed_item pi ON pihb.processed_item_id = pi.id " +
                 "WHERE pihb.available_machining_batch_pieces_count > 0 " +
                 "AND pihb.deleted = false " +
//                 "AND CAST(pihb.item_status AS INTEGER) IN (6, 7) " +
                 "AND pi.item_id = :itemId",
         nativeQuery = true)
  List<ProcessedItemHeatTreatmentBatch> findBatchesWithAvailableMachiningPieces(@Param("itemId") Long itemId);

  /**
   * Find ProcessedItemHeatTreatmentBatch by previous operation processed item ID
   * @param previousOperationProcessedItemId The previous operation processed item ID
   * @return Optional ProcessedItemHeatTreatmentBatch that has the given previousOperationProcessedItemId
   */
  Optional<ProcessedItemHeatTreatmentBatch> findByPreviousOperationProcessedItemIdAndDeletedFalse(Long previousOperationProcessedItemId);

}
