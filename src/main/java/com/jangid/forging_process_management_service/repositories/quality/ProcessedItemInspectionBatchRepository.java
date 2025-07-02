package com.jangid.forging_process_management_service.repositories.quality;

import com.jangid.forging_process_management_service.entities.quality.ProcessedItemInspectionBatch;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProcessedItemInspectionBatchRepository extends CrudRepository<ProcessedItemInspectionBatch, Long> {
  Optional<ProcessedItemInspectionBatch> findByIdAndDeletedFalse(long id);

  @Query("SELECT pii FROM ProcessedItemInspectionBatch pii " +
         "WHERE pii.availableDispatchPiecesCount > 0 " +
         "AND pii.deleted = false " +
//         "AND CAST(pii.itemStatus AS INTEGER) IN (16, 17, 18) " +
         "AND pii.item.id = :itemId")
  List<ProcessedItemInspectionBatch> findInspectionBatchesByItemId(@Param("itemId") Long itemId);

  /**
   * Find ProcessedItemInspectionBatch by previous operation processed item ID
   * @param previousOperationProcessedItemId The previous operation processed item ID
   * @return Optional ProcessedItemInspectionBatch that has the given previousOperationProcessedItemId
   */
  Optional<ProcessedItemInspectionBatch> findByPreviousOperationProcessedItemIdAndDeletedFalse(Long previousOperationProcessedItemId);

}
