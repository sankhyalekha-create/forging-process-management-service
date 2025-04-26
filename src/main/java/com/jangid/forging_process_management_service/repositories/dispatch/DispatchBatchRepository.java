package com.jangid.forging_process_management_service.repositories.dispatch;

import com.jangid.forging_process_management_service.entities.dispatch.DispatchBatch;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DispatchBatchRepository extends JpaRepository<DispatchBatch, Long> {

  boolean existsByDispatchBatchNumberAndTenantIdAndDeletedFalse(String dispatchBatchNumber, Long tenantId);

  Optional<DispatchBatch> findByIdAndDeletedFalse(Long id);
  List<DispatchBatch> findByTenantIdAndDeletedIsFalseOrderByUpdatedAtDesc(long tenantId);

  Page<DispatchBatch> findByTenantIdAndDeletedIsFalseOrderByUpdatedAtDesc(long tenantId, Pageable pageable);

  List<DispatchBatch> findByTenantIdAndDeletedIsFalseAndDispatchBatchStatusAndDispatchedAtBetween(
      long tenantId, DispatchBatch.DispatchBatchStatus status, LocalDateTime start, LocalDateTime end);

  /**
   * Find dispatch batches associated with a specific forge traceability number
   * @param forgeTraceabilityNumber The forge traceability number to search for
   * @return List of dispatch batches associated with the forge
   */
  @Query("SELECT d FROM DispatchBatch d " +
         "JOIN d.processedItemDispatchBatch pid " +
         "JOIN pid.processedItem p " +
         "JOIN p.forge f " +
         "WHERE f.forgeTraceabilityNumber = :forgeTraceabilityNumber " +
         "AND d.deleted = false")
  List<DispatchBatch> findByForgeTraceabilityNumber(@Param("forgeTraceabilityNumber") String forgeTraceabilityNumber);

  /**
   * Find all dispatch batches associated with a specific machining batch
   * @param machiningBatchId The ID of the machining batch
   * @return List of dispatch batches associated with the machining batch
   */
  @Query("SELECT DISTINCT d FROM DispatchBatch d " +
         "JOIN d.dispatchProcessedItemInspections dpi " +
         "JOIN dpi.processedItemInspectionBatch pii " +
         "JOIN pii.inspectionBatch i " +
         "JOIN i.inputProcessedItemMachiningBatch pim " +
         "WHERE pim.machiningBatch.id = :machiningBatchId " +
         "AND d.deleted = false")
  List<DispatchBatch> findByMachiningBatchId(@Param("machiningBatchId") Long machiningBatchId);
}
