package com.jangid.forging_process_management_service.repositories.heating;

import com.jangid.forging_process_management_service.entities.heating.ProcessedItemHeatTreatmentBatch;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface ProcessedItemHeatTreatmentBatchRepository extends CrudRepository<ProcessedItemHeatTreatmentBatch, Long> {

  Optional<ProcessedItemHeatTreatmentBatch> findByIdAndDeletedFalse(Long id);

  @Query(value = "SELECT * FROM processed_item_heat_treatment_batch WHERE available_machining_batch_pieces_count > 0 AND deleted=false AND CAST(item_status AS INTEGER) IN (6, 7)", nativeQuery = true)
  List<ProcessedItemHeatTreatmentBatch> findBatchesWithAvailableMachiningPieces();

}
