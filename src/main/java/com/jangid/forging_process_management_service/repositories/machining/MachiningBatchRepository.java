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

  @Query(value = "select DISTINCT mb.* FROM machining_batch mb "
                 + "JOIN daily_machining_batch dmb ON mb.id = dmb.machining_batch_id "
                 + "WHERE dmb.machine_set_id = :machineSetId AND mb.deleted=false AND mb.machining_batch_status != 'COMPLETED' "
                 + "ORDER BY mb.created_at DESC LIMIT 1", nativeQuery = true)
  Optional<MachiningBatch> findAppliedMachiningBatchOnMachineSet(@Param("machineSetId") long machineSetId);

  // findByIdAndDeletedFalse
  Optional<MachiningBatch> findByIdAndDeletedFalse(long id);
  Optional<MachiningBatch> findByIdAndTenantIdAndDeletedFalse(long id, long tenantId);

  Page<MachiningBatch> findByTenantIdAndDeletedFalseOrderByUpdatedAtDesc(long tenantId, Pageable pageable);

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
         "WHERE pim.workflowIdentifier IS NOT NULL " +
         "AND pim.workflowIdentifier LIKE CONCAT('%', :forgeTraceabilityNumber, '%') " +
         "AND m.deleted = false")
  List<MachiningBatch> findByForgeTraceabilityNumber(@Param("forgeTraceabilityNumber") String forgeTraceabilityNumber);

  /**
   * Find machining batches associated with a specific ItemWorkflow ID
   * @param itemWorkflowId The ItemWorkflow ID to search for
   * @return List of machining batches associated with the ItemWorkflow
   */
  @Query("SELECT m FROM MachiningBatch m " +
         "JOIN m.processedItemMachiningBatch pim " +
         "WHERE pim.itemWorkflowId = :itemWorkflowId " +
         "AND m.deleted = false")
  List<MachiningBatch> findByProcessedItemMachiningBatchItemWorkflowIdAndDeletedFalse(@Param("itemWorkflowId") Long itemWorkflowId);

  /**
   * Find completed machining batches within a specific date range
   *
   * @param machineSetIds List of machine set IDs to search
   * @param startDateTime Start date time (inclusive)
   * @param endDateTime End date time (inclusive)
   * @return List of completed machining batches in the date range
   */
  @Query("SELECT DISTINCT mb FROM MachiningBatch mb " +
         "JOIN mb.dailyMachiningBatch dmb " +
         "WHERE dmb.machineSet.id IN :machineSetIds " +
         "AND mb.machiningBatchStatus = com.jangid.forging_process_management_service.entities.machining.MachiningBatch.MachiningBatchStatus.COMPLETED " +
         "AND mb.endAt BETWEEN :startDateTime AND :endDateTime " +
         "AND mb.deleted = false " +
         "ORDER BY mb.endAt ASC")
  List<MachiningBatch> findCompletedBatchesInDateRange(
          @Param("machineSetIds") List<Long> machineSetIds,
          @Param("startDateTime") LocalDateTime startDateTime,
          @Param("endDateTime") LocalDateTime endDateTime);

  // Search methods for MachiningBatch with pagination support
  
  /**
   * Search MachiningBatch by item name (substring matching)
   * @param tenantId The tenant ID
   * @param itemName The item name to search for (substring)
   * @param pageable Pagination information
   * @return Page of MachiningBatch entities
   */
  @Query("""
        SELECT DISTINCT mb
        FROM MachiningBatch mb
        JOIN mb.processedItemMachiningBatch pimb
        JOIN pimb.item i
        WHERE mb.tenant.id = :tenantId
          AND LOWER(i.itemName) LIKE LOWER(CONCAT('%', :itemName, '%'))
          AND mb.deleted = false
        ORDER BY mb.createdAt DESC
    """)
  Page<MachiningBatch> findMachiningBatchesByItemNameContainingIgnoreCase(@Param("tenantId") Long tenantId, @Param("itemName") String itemName, Pageable pageable);

  /**
   * Search MachiningBatch by forge traceability number (substring matching)
   * @param tenantId The tenant ID
   * @param forgeTraceabilityNumber The forge traceability number to search for
   * @param pageable Pagination information
   * @return Page of MachiningBatch entities
   */
  @Query("""
        SELECT DISTINCT mb
        FROM MachiningBatch mb
        JOIN mb.processedItemMachiningBatch pimb
        WHERE mb.tenant.id = :tenantId
          AND pimb.workflowIdentifier IS NOT NULL
          AND LOWER(pimb.workflowIdentifier) LIKE LOWER(CONCAT('%', :forgeTraceabilityNumber, '%'))
          AND mb.deleted = false
        ORDER BY mb.createdAt DESC
    """)
  Page<MachiningBatch> findMachiningBatchesByForgeTraceabilityNumberContainingIgnoreCase(@Param("tenantId") Long tenantId, @Param("forgeTraceabilityNumber") String forgeTraceabilityNumber, Pageable pageable);

  /**
   * Search MachiningBatch by machining batch number (substring matching)
   * @param tenantId The tenant ID
   * @param machiningBatchNumber The machining batch number to search for
   * @param pageable Pagination information
   * @return Page of MachiningBatch entities
   */
  @Query("""
        SELECT mb
        FROM MachiningBatch mb
        WHERE mb.tenant.id = :tenantId
          AND LOWER(mb.machiningBatchNumber) LIKE LOWER(CONCAT('%', :machiningBatchNumber, '%'))
          AND mb.deleted = false
        ORDER BY mb.createdAt DESC
    """)
  Page<MachiningBatch> findMachiningBatchesByMachiningBatchNumberContainingIgnoreCase(@Param("tenantId") Long tenantId, @Param("machiningBatchNumber") String machiningBatchNumber, Pageable pageable);

  /**
   * Find machining batches by multiple processed item machining batch IDs
   * @param processedItemMachiningBatchIds List of processed item machining batch IDs
   * @return List of machining batches associated with the processed item machining batch IDs
   */
  @Query("SELECT mb FROM MachiningBatch mb " +
         "JOIN mb.processedItemMachiningBatch pimb " +
         "WHERE pimb.id IN :processedItemMachiningBatchIds " +
         "AND mb.deleted = false")
  List<MachiningBatch> findByProcessedItemMachiningBatchIdInAndDeletedFalse(@Param("processedItemMachiningBatchIds") List<Long> processedItemMachiningBatchIds);
}
