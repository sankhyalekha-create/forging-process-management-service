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

  /**
   * Check if an inspection batch with the given batch number was previously used and deleted
   * Uses the original batch number to find records that have been deleted and renamed
   */
  @Query("SELECT CASE WHEN COUNT(ib) > 0 THEN TRUE ELSE FALSE END FROM InspectionBatch ib " +
         "WHERE ib.originalInspectionBatchNumber = :batchNumber " +
         "AND ib.tenant.id = :tenantId " +
         "AND ib.deleted = true")
  boolean existsByInspectionBatchNumberAndTenantIdAndOriginalInspectionBatchNumber(
          @Param("batchNumber") String batchNumber, 
          @Param("tenantId") Long tenantId);

  List<InspectionBatch> findByTenantIdAndDeletedIsFalseOrderByUpdatedAtDesc(long tenantId);

  Page<InspectionBatch> findByTenantIdAndDeletedIsFalseOrderByUpdatedAtDesc(long tenantId, Pageable pageable);

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
         "WHERE pii.workflowIdentifier IS NOT NULL " +
         "AND i.deleted = false " +
         "AND pii.workflowIdentifier LIKE CONCAT('%', :forgeTraceabilityNumber, '%')")
  List<InspectionBatch> findByForgeTraceabilityNumber(@Param("forgeTraceabilityNumber") String forgeTraceabilityNumber);

  /**
   * Find all inspection batches associated with a specific machining batch
   * @param machiningBatchId The ID of the machining batch
   * @return List of inspection batches associated with the machining batch
   */
  @Query("SELECT i FROM InspectionBatch i " +
         "JOIN i.inputProcessedItemMachiningBatch pim " +
         "WHERE pim.machiningBatch.id = :machiningBatchId " +
         "AND i.deleted = false")
  List<InspectionBatch> findByMachiningBatchId(@Param("machiningBatchId") Long machiningBatchId);

  // Search methods for InspectionBatch with pagination support
  
  /**
   * Search InspectionBatch by item name (substring matching)
   * @param tenantId The tenant ID
   * @param itemName The item name to search for (substring)
   * @param pageable Pagination information
   * @return Page of InspectionBatch entities
   */
  @Query("""
        SELECT i
        FROM InspectionBatch i
        JOIN i.processedItemInspectionBatch pii
        JOIN pii.item item
        WHERE i.tenant.id = :tenantId
          AND LOWER(item.itemName) LIKE LOWER(CONCAT('%', :itemName, '%'))
          AND i.deleted = false
        ORDER BY i.createdAt DESC
    """)
  Page<InspectionBatch> findInspectionBatchesByItemNameContainingIgnoreCase(@Param("tenantId") Long tenantId, @Param("itemName") String itemName, Pageable pageable);

  /**
   * Search InspectionBatch by forge traceability number (substring matching)
   * @param tenantId The tenant ID
   * @param forgeTraceabilityNumber The forge traceability number to search for (substring)
   * @param pageable Pagination information
   * @return Page of InspectionBatch entities
   */
  @Query("""
        SELECT i
        FROM InspectionBatch i
        JOIN i.processedItemInspectionBatch pii
        WHERE i.tenant.id = :tenantId
          AND pii.workflowIdentifier IS NOT NULL
          AND LOWER(pii.workflowIdentifier) LIKE LOWER(CONCAT('%', :forgeTraceabilityNumber, '%'))
          AND i.deleted = false
        ORDER BY i.createdAt DESC
    """)
  Page<InspectionBatch> findInspectionBatchesByForgeTraceabilityNumberContainingIgnoreCase(@Param("tenantId") Long tenantId, @Param("forgeTraceabilityNumber") String forgeTraceabilityNumber, Pageable pageable);

  /**
   * Search InspectionBatch by inspection batch number (substring matching)
   * @param tenantId The tenant ID
   * @param inspectionBatchNumber The inspection batch number to search for (substring)
   * @param pageable Pagination information
   * @return Page of InspectionBatch entities
   */
  @Query("""
        SELECT i
        FROM InspectionBatch i
        WHERE i.tenant.id = :tenantId
          AND LOWER(i.inspectionBatchNumber) LIKE LOWER(CONCAT('%', :inspectionBatchNumber, '%'))
          AND i.deleted = false
        ORDER BY i.createdAt DESC
    """)
  Page<InspectionBatch> findInspectionBatchesByInspectionBatchNumberContainingIgnoreCase(@Param("tenantId") Long tenantId, @Param("inspectionBatchNumber") String inspectionBatchNumber, Pageable pageable);

  /**
   * Find inspection batches by multiple processed item inspection batch IDs
   * @param processedItemInspectionBatchIds List of processed item inspection batch IDs
   * @return List of inspection batches associated with the processed item inspection batch IDs
   */
  @Query("SELECT ib FROM InspectionBatch ib " +
         "JOIN ib.processedItemInspectionBatch piib " +
         "WHERE piib.id IN :processedItemInspectionBatchIds " +
         "AND ib.deleted = false")
  List<InspectionBatch> findByProcessedItemInspectionBatchIdInAndDeletedFalse(@Param("processedItemInspectionBatchIds") List<Long> processedItemInspectionBatchIds);
}
