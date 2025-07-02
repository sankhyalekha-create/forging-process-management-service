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
  
  /**
   * Check if a dispatch batch with the given batch number was previously used and deleted
   * Uses the original batch number to find records that have been deleted and renamed
   */
  @Query("SELECT CASE WHEN COUNT(db) > 0 THEN TRUE ELSE FALSE END FROM DispatchBatch db " +
         "WHERE db.originalDispatchBatchNumber = :batchNumber " +
         "AND db.tenant.id = :tenantId " +
         "AND db.deleted = true")
  boolean existsByDispatchBatchNumberAndTenantIdAndOriginalDispatchBatchNumber(
          @Param("batchNumber") String batchNumber, 
          @Param("tenantId") Long tenantId);
  
  boolean existsByInvoiceNumberAndTenantIdAndDeletedFalse(String invoiceNumber, Long tenantId);

  Optional<DispatchBatch> findByIdAndDeletedFalse(Long id);
  List<DispatchBatch> findByTenantIdAndDeletedIsFalseOrderByUpdatedAtDesc(long tenantId);

  @Query("SELECT db FROM DispatchBatch db " +
         "WHERE db.tenant.id = :tenantId " +
         "AND db.deleted = false " +
         "ORDER BY " +
         "CASE WHEN db.dispatchBatchStatus = com.jangid.forging_process_management_service.entities.dispatch.DispatchBatch.DispatchBatchStatus.READY_TO_DISPATCH THEN 0 ELSE 1 END, " +
         "CASE WHEN db.dispatchBatchStatus = com.jangid.forging_process_management_service.entities.dispatch.DispatchBatch.DispatchBatchStatus.READY_TO_DISPATCH THEN db.updatedAt END ASC, " +
         "CASE WHEN db.dispatchBatchStatus != com.jangid.forging_process_management_service.entities.dispatch.DispatchBatch.DispatchBatchStatus.READY_TO_DISPATCH THEN db.updatedAt END DESC")
  Page<DispatchBatch> findByTenantIdAndDeletedIsFalseOrderByUpdatedAtDesc(@Param("tenantId") long tenantId, Pageable pageable);

  List<DispatchBatch> findByTenantIdAndDeletedIsFalseAndDispatchBatchStatusAndDispatchedAtBetween(
      long tenantId, DispatchBatch.DispatchBatchStatus status, LocalDateTime start, LocalDateTime end);

  /**
   * Find dispatch batches associated with a specific forge traceability number
   * @param forgeTraceabilityNumber The forge traceability number to search for
   * @return List of dispatch batches associated with the forge
   */
  @Query("SELECT d FROM DispatchBatch d " +
         "JOIN d.processedItemDispatchBatch pid " +
         "WHERE pid.workflowIdentifier IS NOT NULL " +
         "AND pid.workflowIdentifier LIKE CONCAT('%', :forgeTraceabilityNumber, '%') " +
         "AND d.deleted = false")
  List<DispatchBatch> findByForgeTraceabilityNumber(@Param("forgeTraceabilityNumber") String forgeTraceabilityNumber);

  /**
   * Find dispatch batches associated with a specific ItemWorkflow ID
   * @param itemWorkflowId The ItemWorkflow ID to search for
   * @return List of dispatch batches associated with the ItemWorkflow
   */
  @Query("SELECT d FROM DispatchBatch d " +
         "JOIN d.processedItemDispatchBatch pid " +
         "WHERE pid.itemWorkflowId = :itemWorkflowId " +
         "AND d.deleted = false")
  List<DispatchBatch> findByProcessedItemDispatchBatchItemWorkflowIdAndDeletedFalse(@Param("itemWorkflowId") Long itemWorkflowId);

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

  /**
   * Find dispatched batches within a date range for a specific tenant
   *
   * @param tenantId Tenant ID
   * @param startDateTime Start date/time (inclusive)
   * @param endDateTime End date/time (inclusive)
   * @return List of dispatched batches
   */
  @Query("SELECT db FROM DispatchBatch db WHERE db.tenant.id = :tenantId " +
         "AND db.dispatchBatchStatus = com.jangid.forging_process_management_service.entities.dispatch.DispatchBatch.DispatchBatchStatus.DISPATCHED " +
         "AND db.dispatchedAt BETWEEN :startDateTime AND :endDateTime " +
         "AND db.deleted = false " +
         "ORDER BY db.dispatchedAt ASC")
  List<DispatchBatch> findDispatchedBatchesByDateRange(
          @Param("tenantId") Long tenantId,
          @Param("startDateTime") LocalDateTime startDateTime,
          @Param("endDateTime") LocalDateTime endDateTime);

  // Search methods for DispatchBatch with pagination support
  
  /**
   * Search DispatchBatch by item name (substring matching)
   * @param tenantId The tenant ID
   * @param itemName The item name to search for (substring)
   * @param pageable Pagination information
   * @return Page of DispatchBatch entities
   */
  @Query("""
        SELECT d
        FROM DispatchBatch d
        JOIN d.processedItemDispatchBatch pid
        JOIN pid.item item
        WHERE d.tenant.id = :tenantId
          AND LOWER(item.itemName) LIKE LOWER(CONCAT('%', :itemName, '%'))
          AND d.deleted = false
        ORDER BY d.updatedAt DESC
    """)
  Page<DispatchBatch> findDispatchBatchesByItemNameContainingIgnoreCase(@Param("tenantId") Long tenantId, @Param("itemName") String itemName, Pageable pageable);

  /**
   * Search DispatchBatch by forge traceability number (substring matching)
   * @param tenantId The tenant ID
   * @param forgeTraceabilityNumber The forge traceability number to search for (substring)
   * @param pageable Pagination information
   * @return Page of DispatchBatch entities
   */
  @Query("""
        SELECT d
        FROM DispatchBatch d
        JOIN d.processedItemDispatchBatch pid
        WHERE d.tenant.id = :tenantId
          AND pid.workflowIdentifier IS NOT NULL
          AND LOWER(pid.workflowIdentifier) LIKE LOWER(CONCAT('%', :forgeTraceabilityNumber, '%'))
          AND d.deleted = false
        ORDER BY d.updatedAt DESC
    """)
  Page<DispatchBatch> findDispatchBatchesByForgeTraceabilityNumberContainingIgnoreCase(@Param("tenantId") Long tenantId, @Param("forgeTraceabilityNumber") String forgeTraceabilityNumber, Pageable pageable);

  /**
   * Search DispatchBatch by dispatch batch number (substring matching)
   * @param tenantId The tenant ID
   * @param dispatchBatchNumber The dispatch batch number to search for (substring)
   * @param pageable Pagination information
   * @return Page of DispatchBatch entities
   */
  @Query("""
        SELECT d
        FROM DispatchBatch d
        WHERE d.tenant.id = :tenantId
          AND LOWER(d.dispatchBatchNumber) LIKE LOWER(CONCAT('%', :dispatchBatchNumber, '%'))
          AND d.deleted = false
        ORDER BY d.updatedAt DESC
    """)
  Page<DispatchBatch> findDispatchBatchesByDispatchBatchNumberContainingIgnoreCase(@Param("tenantId") Long tenantId, @Param("dispatchBatchNumber") String dispatchBatchNumber, Pageable pageable);

  /**
   * Search DispatchBatch by dispatch batch status (exact matching)
   * @param tenantId The tenant ID
   * @param dispatchBatchStatus The dispatch batch status to search for
   * @param pageable Pagination information
   * @return Page of DispatchBatch entities ordered by updatedAt DESC
   */
  @Query("""
        SELECT d
        FROM DispatchBatch d
        WHERE d.tenant.id = :tenantId
          AND d.dispatchBatchStatus = :dispatchBatchStatus
          AND d.deleted = false
        ORDER BY d.updatedAt DESC
    """)
  Page<DispatchBatch> findDispatchBatchesByDispatchBatchStatus(@Param("tenantId") Long tenantId, @Param("dispatchBatchStatus") DispatchBatch.DispatchBatchStatus dispatchBatchStatus, Pageable pageable);

  /**
   * Find dispatch batches by multiple processed item dispatch batch IDs
   * @param processedItemDispatchBatchIds List of processed item dispatch batch IDs
   * @return List of dispatch batches associated with the processed item dispatch batch IDs
   */
  @Query("SELECT db FROM DispatchBatch db " +
         "JOIN db.processedItemDispatchBatch pidb " +
         "WHERE pidb.id IN :processedItemDispatchBatchIds " +
         "AND db.deleted = false")
  List<DispatchBatch> findByProcessedItemDispatchBatchIdInAndDeletedFalse(@Param("processedItemDispatchBatchIds") List<Long> processedItemDispatchBatchIds);
}
