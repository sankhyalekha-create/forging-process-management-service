package com.jangid.forging_process_management_service.repositories.machining;

import com.jangid.forging_process_management_service.entities.machining.MachiningBatch;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MachiningBatchRepository extends CrudRepository<MachiningBatch, Long> {
// findAppliedMachiningBatchOnMachineSet

  @Query(value = "select * FROM machining_batch mb "
                 + "where mb.machine_set = :machineSetId and mb.deleted=false and mb.machining_batch_status != 'COMPLETED'"
                 + "order by mb.created_at desc LIMIT 1", nativeQuery = true)
  Optional<MachiningBatch> findAppliedMachiningBatchOnMachineSet(@Param("machineSetId") long machineSetId);

  // findByIdAndDeletedFalse
  Optional<MachiningBatch> findByIdAndDeletedFalse(long id);
  Optional<MachiningBatch> findByIdAndTenantIdAndDeletedFalse(long id, long tenantId);

  Page<MachiningBatch> findByMachineSetIdInAndDeletedFalseOrderByCreatedAtDesc(List<Long> machineSetIds, Pageable pageable);

  boolean existsByMachiningBatchNumberAndTenantIdAndDeletedFalse(String machiningBatchNumber, Long tenantId);
  
  /**
   * Check if a machining batch with the given batch number was previously used and deleted
   * Uses the original batch number to find records that have been deleted and renamed
   */
  @Query("SELECT CASE WHEN COUNT(mb) > 0 THEN TRUE ELSE FALSE END FROM MachiningBatch mb " +
         "WHERE mb.originalMachiningBatchNumber = :batchNumber " +
         "AND mb.tenant.id = :tenantId " +
         "AND mb.deleted = true")
  boolean existsByMachiningBatchNumberAndTenantIdAndOriginalMachiningBatchNumber(
          @Param("batchNumber") String batchNumber, 
          @Param("tenantId") Long tenantId);

  @Query(value = "SELECT * FROM machining_batch WHERE tenant_id = :tenantId AND machining_batch_status = 'IN_PROGRESS' AND deleted = false", nativeQuery = true)
  List<MachiningBatch> findByTenantIdAndMachiningBatchStatusInProgressAndDeletedFalse(@Param("tenantId") long tenantId);

  /**
   * Find machining batches associated with a specific forge traceability number
   * @param forgeTraceabilityNumber The forge traceability number to search for
   * @return List of machining batches associated with the forge
   */
  @Query("SELECT m FROM MachiningBatch m " +
         "JOIN m.processedItemMachiningBatch pim " +
         "JOIN pim.processedItem p " +
         "JOIN p.forge f " +
         "WHERE f.forgeTraceabilityNumber = :forgeTraceabilityNumber " +
         "AND m.deleted = false")
  List<MachiningBatch> findByForgeTraceabilityNumber(@Param("forgeTraceabilityNumber") String forgeTraceabilityNumber);

  /**
   * Find completed machining batches within a specific date range
   *
   * @param machineSetIds List of machine set IDs to search
   * @param startDateTime Start date time (inclusive)
   * @param endDateTime End date time (inclusive)
   * @return List of completed machining batches in the date range
   */
  @Query("SELECT mb FROM MachiningBatch mb " +
         "WHERE mb.machineSet.id IN :machineSetIds " +
         "AND mb.machiningBatchStatus = com.jangid.forging_process_management_service.entities.machining.MachiningBatch.MachiningBatchStatus.COMPLETED " +
         "AND mb.endAt BETWEEN :startDateTime AND :endDateTime " +
         "AND mb.deleted = false " +
         "ORDER BY mb.endAt ASC")
  List<MachiningBatch> findCompletedBatchesInDateRange(
          @Param("machineSetIds") List<Long> machineSetIds,
          @Param("startDateTime") LocalDateTime startDateTime,
          @Param("endDateTime") LocalDateTime endDateTime);

}
