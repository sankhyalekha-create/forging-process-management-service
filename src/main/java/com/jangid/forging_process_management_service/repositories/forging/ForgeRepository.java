package com.jangid.forging_process_management_service.repositories.forging;

import com.jangid.forging_process_management_service.entities.forging.Forge;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ForgeRepository extends CrudRepository<Forge, Long> {
  // Additional query methods if needed, for example, find by status or forging line
  Page<Forge> findByForgingLineIdInAndDeletedFalseOrderByUpdatedAtDesc(List<Long> forgingLineId, Pageable pageable);

  @Query(value = "select * FROM forge ft "
                 + "where ft.forging_line_id = :forgingLineId and ft.deleted=false and ft.forging_status != 'COMPLETED'"
                 + "order by ft.created_at desc LIMIT 1", nativeQuery = true)
  Optional<Forge> findAppliedForgeOnForgingLine(@Param("forgingLineId") long forgingLineId);

  @Query(value = "SELECT * FROM forge ft " +
                 "WHERE ft.forging_line_id = :forgingLineId " +
                 "AND ft.forge_traceability_number IS NOT NULL " +
                 "ORDER BY ft.created_at DESC " +
                 "LIMIT 1", nativeQuery = true)
  Optional<Forge> findLastDeletedAndNonDeletedForgeOnForgingLine(@Param("forgingLineId") long forgingLineId);

  @Query("SELECT f FROM Forge f WHERE f.forgingLine.id = :forgingLineId AND f.forgeTraceabilityNumber LIKE :forgePrefix% AND f.deleted = false ORDER BY f.createdAt DESC")
  List<Forge> findLastForgeForTheDay(@Param("forgingLineId") long forgingLineId, @Param("forgePrefix") String forgePrefix);

  Optional<Forge> findByIdAndDeletedFalse(long id);
  Optional<Forge> findByIdAndTenantIdAndDeletedFalse(long id, long tenantId);
  Optional<Forge> findByForgeTraceabilityNumberAndDeletedFalse(String forgeTraceabilityNumber);
  Optional<Forge> findByIdAndAndForgingLineIdAndDeletedFalse(long id, long forgingLineId);
  Optional<Forge> findByProcessedItemIdAndDeletedFalse(long processedItemId);

  // Method to find forges by multiple processed item IDs
  List<Forge> findByProcessedItemIdInAndDeletedFalse(List<Long> processedItemIds);

//  List<Forge> findByHeatIdAndDeletedFalse(long heatId);

  // Search methods for Forge with pagination support
  @Query("""
        SELECT f
        FROM Forge f
        WHERE f.tenant.id = :tenantId
          AND LOWER(f.processedItem.item.itemName) LIKE LOWER(CONCAT('%', :itemName, '%'))
          AND f.deleted = false
        ORDER BY f.createdAt DESC
    """)
  Page<Forge> findForgesByItemNameContainingIgnoreCase(@Param("tenantId") Long tenantId, @Param("itemName") String itemName, Pageable pageable);

  @Query("""
        SELECT f
        FROM Forge f
        WHERE f.tenant.id = :tenantId
          AND LOWER(f.forgeTraceabilityNumber) LIKE LOWER(CONCAT('%', :forgeTraceabilityNumber, '%'))
          AND f.deleted = false
        ORDER BY f.createdAt DESC
    """)
  Page<Forge> findForgesByForgeTraceabilityNumberContainingIgnoreCase(@Param("tenantId") Long tenantId, @Param("forgeTraceabilityNumber") String forgeTraceabilityNumber, Pageable pageable);

  @Query("""
        SELECT f
        FROM Forge f
        WHERE f.tenant.id = :tenantId
          AND LOWER(f.forgingLine.forgingLineName) LIKE LOWER(CONCAT('%', :forgingLineName, '%'))
          AND f.deleted = false
        ORDER BY f.createdAt DESC
    """)
  Page<Forge> findForgesByForgingLineNameContainingIgnoreCase(@Param("tenantId") Long tenantId, @Param("forgingLineName") String forgingLineName, Pageable pageable);
}
