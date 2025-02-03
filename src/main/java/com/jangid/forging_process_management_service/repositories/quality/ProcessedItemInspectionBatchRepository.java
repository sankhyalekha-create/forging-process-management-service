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
         "JOIN pii.processedItem pi " +
         "WHERE pii.availableDispatchPiecesCount > 0 " +
         "AND pii.deleted = false " +
         "AND CAST(pii.itemStatus AS INTEGER) IN (16, 17) " +
         "AND pi.item.id = :itemId")
  List<ProcessedItemInspectionBatch> findInspectionBatchesByItemId(@Param("itemId") Long itemId);

}
