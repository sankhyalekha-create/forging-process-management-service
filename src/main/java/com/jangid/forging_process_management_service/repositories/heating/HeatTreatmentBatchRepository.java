package com.jangid.forging_process_management_service.repositories.heating;

import com.jangid.forging_process_management_service.entities.heating.HeatTreatmentBatch;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HeatTreatmentBatchRepository extends CrudRepository<HeatTreatmentBatch, Long> {

  @Query(value = "select * FROM heat_treatment_batch htb "
                 + "where htb.furnace_id = :furnaceId and htb.deleted=false and htb.heat_treatment_batch_status != '2'"
                 + "order by htb.created_at desc LIMIT 1", nativeQuery = true)
  Optional<HeatTreatmentBatch> findAppliedHeatTreatmentBatchOnFurnace(@Param("furnaceId") long furnaceId);

  Optional<HeatTreatmentBatch> findByIdAndDeletedFalse(long id);
  Page<HeatTreatmentBatch> findByFurnaceIdInAndDeletedFalseOrderByCreatedAtDesc(List<Long> furnaceIds, Pageable pageable);

  @Query("SELECT b FROM HeatTreatmentBatch b " +
         "JOIN FETCH b.processedItemHeatTreatmentBatches p " +
         "JOIN FETCH p.processedItem " +
         "WHERE b.id = :batchId")
  Optional<HeatTreatmentBatch> findByIdWithProcessedItems(@Param("batchId") Long batchId);
}
