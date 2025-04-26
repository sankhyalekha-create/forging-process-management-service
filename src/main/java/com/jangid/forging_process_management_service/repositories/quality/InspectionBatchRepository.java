package com.jangid.forging_process_management_service.repositories.quality;

import com.jangid.forging_process_management_service.entities.quality.InspectionBatch;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InspectionBatchRepository extends CrudRepository<InspectionBatch, Long> {

  boolean existsByInspectionBatchNumberAndTenantIdAndDeletedFalse(String inspectionBatchNumber, long tenantId);

  List<InspectionBatch> findByTenantIdAndDeletedIsFalseOrderByCreatedAtDesc(long tenantId);

  Page<InspectionBatch> findByTenantIdAndDeletedIsFalseOrderByCreatedAtDesc(long tenantId, Pageable pageable);

  Optional<InspectionBatch> findByIdAndTenantIdAndDeletedFalse(long id, long tenantId);

  boolean existsByInputProcessedItemMachiningBatchIdAndDeletedFalse(long inputProcessedItemMachiningBatchId);

  List<InspectionBatch> findByInputProcessedItemMachiningBatchIdAndTenantIdAndDeletedFalse(
      long inputProcessedItemMachiningBatchId, long tenantId);

  /**
   * Find inspection batches associated with a specific forge traceability number
   * @param forgeTraceabilityNumber The forge traceability number to search for
   * @return List of inspection batches associated with the forge
   */
  @Query("SELECT i FROM InspectionBatch i " +
         "JOIN i.processedItemInspectionBatch pii " +
         "JOIN pii.processedItem p " +
         "JOIN p.forge f " +
         "WHERE f.forgeTraceabilityNumber = :forgeTraceabilityNumber " +
         "AND i.deleted = false")
  List<InspectionBatch> findByForgeTraceabilityNumber(@Param("forgeTraceabilityNumber") String forgeTraceabilityNumber);

}
