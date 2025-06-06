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
                 + "where htb.furnace_id = :furnaceId and htb.deleted=false and htb.heat_treatment_batch_status != 'COMPLETED'"
                 + "order by htb.created_at desc LIMIT 1", nativeQuery = true)
  Optional<HeatTreatmentBatch> findAppliedHeatTreatmentBatchOnFurnace(@Param("furnaceId") long furnaceId);

  Optional<HeatTreatmentBatch> findByIdAndDeletedFalse(long id);
  Page<HeatTreatmentBatch> findByTenantIdAndDeletedFalseOrderByUpdatedAtDesc(Long tenantId, Pageable pageable);

  @Query("SELECT b FROM HeatTreatmentBatch b " +
         "JOIN FETCH b.processedItemHeatTreatmentBatches p " +
         "JOIN FETCH p.processedItem " +
         "WHERE b.id = :batchId")
  Optional<HeatTreatmentBatch> findByIdWithProcessedItems(@Param("batchId") Long batchId);

  boolean existsByHeatTreatmentBatchNumberAndTenantIdAndDeletedFalse(String heatTreatmentBatchNumber, Long tenantId);
  
  /**
   * Check if a heat treatment batch with the given batch number was previously used and deleted
   * Uses the original batch number to find records that have been deleted and renamed
   */
  @Query("SELECT CASE WHEN COUNT(htb) > 0 THEN TRUE ELSE FALSE END FROM HeatTreatmentBatch htb " +
         "WHERE htb.originalHeatTreatmentBatchNumber = :batchNumber " +
         "AND htb.tenant.id = :tenantId " +
         "AND htb.deleted = true")
  boolean existsByHeatTreatmentBatchNumberAndTenantIdAndOriginalHeatTreatmentBatchNumber(
          @Param("batchNumber") String batchNumber, 
          @Param("tenantId") Long tenantId);
  
  /**
   * Find heat treatment batches associated with a specific forge traceability number
   * @param forgeTraceabilityNumber The forge traceability number to search for
   * @return List of heat treatment batches associated with the forge
   */
  @Query("SELECT h FROM HeatTreatmentBatch h " +
         "JOIN h.processedItemHeatTreatmentBatches pih " +
         "JOIN pih.processedItem p " +
         "JOIN p.forge f " +
         "WHERE f.forgeTraceabilityNumber = :forgeTraceabilityNumber " +
         "AND h.deleted = false")
  List<HeatTreatmentBatch> findByForgeTraceabilityNumber(@Param("forgeTraceabilityNumber") String forgeTraceabilityNumber);

  /**
   * Find machining batches associated with a specific heat treatment batch
   * @param heatTreatmentBatchId The heat treatment batch ID
   * @return List of machining batches associated with the heat treatment batch
   */
  @Query("SELECT m FROM MachiningBatch m " +
         "JOIN m.processedItemHeatTreatmentBatch pih " +
         "JOIN pih.heatTreatmentBatch h " +
         "WHERE h.id = :heatTreatmentBatchId " +
         "AND m.deleted = false")
  List<com.jangid.forging_process_management_service.entities.machining.MachiningBatch> findMachiningBatchesByHeatTreatmentBatchId(@Param("heatTreatmentBatchId") Long heatTreatmentBatchId);

  // Search methods for HeatTreatmentBatch with pagination support
  
  /**
   * Search HeatTreatmentBatch by item name (substring matching)
   * @param tenantId The tenant ID
   * @param itemName The item name to search for (substring)
   * @param pageable Pagination information
   * @return Page of HeatTreatmentBatch entities
   */
  @Query("""
        SELECT DISTINCT htb
        FROM HeatTreatmentBatch htb
        JOIN htb.processedItemHeatTreatmentBatches pihtb
        JOIN pihtb.processedItem pi
        JOIN pi.item i
        WHERE htb.tenant.id = :tenantId
          AND LOWER(i.itemName) LIKE LOWER(CONCAT('%', :itemName, '%'))
          AND htb.deleted = false
        ORDER BY htb.createdAt DESC
    """)
  Page<HeatTreatmentBatch> findHeatTreatmentBatchesByItemNameContainingIgnoreCase(@Param("tenantId") Long tenantId, @Param("itemName") String itemName, Pageable pageable);

  /**
   * Search HeatTreatmentBatch by forge traceability number (substring matching)
   * @param tenantId The tenant ID
   * @param forgeTraceabilityNumber The forge traceability number to search for
   * @param pageable Pagination information
   * @return Page of HeatTreatmentBatch entities
   */
  @Query("""
        SELECT DISTINCT htb
        FROM HeatTreatmentBatch htb
        JOIN htb.processedItemHeatTreatmentBatches pihtb
        JOIN pihtb.processedItem pi
        JOIN pi.forge f
        WHERE htb.tenant.id = :tenantId
          AND LOWER(f.forgeTraceabilityNumber) LIKE LOWER(CONCAT('%', :forgeTraceabilityNumber, '%'))
          AND htb.deleted = false
        ORDER BY htb.createdAt DESC
    """)
  Page<HeatTreatmentBatch> findHeatTreatmentBatchesByForgeTraceabilityNumberContainingIgnoreCase(@Param("tenantId") Long tenantId, @Param("forgeTraceabilityNumber") String forgeTraceabilityNumber, Pageable pageable);

  /**
   * Search HeatTreatmentBatch by heat treatment batch number (substring matching)
   * @param tenantId The tenant ID
   * @param heatTreatmentBatchNumber The heat treatment batch number to search for
   * @param pageable Pagination information
   * @return Page of HeatTreatmentBatch entities
   */
  @Query("""
        SELECT htb
        FROM HeatTreatmentBatch htb
        WHERE htb.tenant.id = :tenantId
          AND LOWER(htb.heatTreatmentBatchNumber) LIKE LOWER(CONCAT('%', :heatTreatmentBatchNumber, '%'))
          AND htb.deleted = false
        ORDER BY htb.createdAt DESC
    """)
  Page<HeatTreatmentBatch> findHeatTreatmentBatchesByHeatTreatmentBatchNumberContainingIgnoreCase(@Param("tenantId") Long tenantId, @Param("heatTreatmentBatchNumber") String heatTreatmentBatchNumber, Pageable pageable);

  /**
   * Search HeatTreatmentBatch by furnace name (substring matching)
   * @param tenantId The tenant ID
   * @param furnaceName The furnace name to search for
   * @param pageable Pagination information
   * @return Page of HeatTreatmentBatch entities
   */
  @Query("""
        SELECT htb
        FROM HeatTreatmentBatch htb
        JOIN htb.furnace f
        WHERE htb.tenant.id = :tenantId
          AND LOWER(f.furnaceName) LIKE LOWER(CONCAT('%', :furnaceName, '%'))
          AND htb.deleted = false
        ORDER BY htb.createdAt DESC
    """)
  Page<HeatTreatmentBatch> findHeatTreatmentBatchesByFurnaceNameContainingIgnoreCase(@Param("tenantId") Long tenantId, @Param("furnaceName") String furnaceName, Pageable pageable);
}
