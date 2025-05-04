package com.jangid.forging_process_management_service.repositories.inventory;

import com.jangid.forging_process_management_service.entities.inventory.Heat;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface HeatRepository extends CrudRepository<Heat, Long> {
  List<Heat> findByHeatNumberAndDeletedIsFalse(String heatNumber);
  Optional<Heat> findByIdAndDeletedFalse(long heatId);
  Optional<Heat> findByHeatNumberAndRawMaterialProductIdAndDeletedFalse(String heatNumber, long tenantId);

  @Query("""
        SELECT h
        FROM heat h
        JOIN h.rawMaterialProduct rmp
        JOIN rmp.rawMaterial rm
        WHERE rm.tenant.id = :tenantId
          AND h.heatNumber = :heatNumber
          AND h.deleted = false
          AND rmp.deleted = false
          AND rm.deleted = false
    """)
  Optional<Heat> findHeatByHeatNumberAndTenantId(@Param("heatNumber") String heatNumber, @Param("tenantId") Long tenantId);

  @Query("""
        SELECT h
        FROM heat h
        JOIN h.rawMaterialProduct rmp
        JOIN rmp.rawMaterial rm
        JOIN rmp.product p
        WHERE p.id = :productId
          AND rm.tenant.id = :tenantId
          AND (h.availableHeatQuantity > 0)
          AND h.deleted = false
          AND rmp.deleted = false
          AND rm.deleted = false
    """)
  List<Heat> findHeatsHavingQuantitiesByProductIdAndTenantId(@Param("productId") Long productId, @Param("tenantId") Long tenantId);

  @Query("""
        SELECT h
        FROM heat h
        JOIN h.rawMaterialProduct rmp
        JOIN rmp.rawMaterial rm
        JOIN rmp.product p
        WHERE p.id = :productId
          AND rm.tenant.id = :tenantId
          AND (h.availablePiecesCount > 0)
          AND h.deleted = false
          AND rmp.deleted = false
          AND rm.deleted = false
    """)
  List<Heat> findHeatsHavingPiecesByProductIdAndTenantId(@Param("productId") Long productId, @Param("tenantId") Long tenantId);

  @Modifying
  @Query("UPDATE heat h SET h.availableHeatQuantity = h.availableHeatQuantity + :quantity WHERE h.id = :heatId AND h.deleted = false")
  void incrementAvailableHeatQuantity(@Param("heatId") Long heatId, @Param("quantity") Double quantity);

  @Query("""
        SELECT h
        FROM heat h
        JOIN h.rawMaterialProduct rmp
        JOIN rmp.rawMaterial rm
        WHERE rm.tenant.id = :tenantId
          AND h.deleted = false
          AND rmp.deleted = false
          AND rm.deleted = false
        ORDER BY h.createdAt DESC
    """)
  Page<Heat> findHeatsByTenantId(@Param("tenantId") Long tenantId, Pageable pageable);

  /**
   * Find heats created within a date range for a specific tenant
   *
   * @param tenantId Tenant ID
   * @param startDateTime Start date/time (inclusive)
   * @param endDateTime End date/time (inclusive)
   * @return List of heats
   */
  @Query("SELECT h FROM heat h " +
         "JOIN h.rawMaterialProduct rmp " +
         "JOIN rmp.rawMaterial rm " +
         "WHERE rm.tenant.id = :tenantId " +
         "AND rm.rawMaterialReceivingDate BETWEEN :startDateTime AND :endDateTime " +
         "AND h.deleted = false " +
         "AND rmp.deleted = false " +
         "AND rm.deleted = false " +
         "ORDER BY rm.rawMaterialReceivingDate ASC")
  List<Heat> findHeatsByDateRange(
          @Param("tenantId") Long tenantId,
          @Param("startDateTime") LocalDateTime startDateTime,
          @Param("endDateTime") LocalDateTime endDateTime);
          
  /**
   * Determine if a dispatch item originated from heat measured in pieces
   *
   * @param dispatchItemId Dispatch item ID
   * @return true if from a pieces heat, false if from KGS heat
   */
  @Query(value = "SELECT CASE WHEN COUNT(*) > 0 THEN TRUE ELSE FALSE END " +
                 "FROM dispatch_item di " +
                 "JOIN processed_item pi ON di.processed_item_id = pi.id " +
                 "JOIN processed_item_input pii ON pi.id = pii.processed_item_id " +
                 "JOIN heat h ON pii.heat_id = h.id " +
                 "WHERE di.id = :dispatchItemId AND h.is_in_pieces = true", 
                 nativeQuery = true)
  boolean isDispatchItemFromPiecesHeat(@Param("dispatchItemId") Long dispatchItemId);
  
  /**
   * Determine if a processed item originated from heat measured in pieces
   *
   * @param processedItemId Processed item ID
   * @return true if from a pieces heat, false if from KGS heat
   */
  @Query(value = "SELECT CASE WHEN COUNT(*) > 0 THEN TRUE ELSE FALSE END " +
                 "FROM processed_item pi " +
                 "JOIN processed_item_input pii ON pi.id = pii.processed_item_id " +
                 "JOIN heat h ON pii.heat_id = h.id " +
                 "WHERE pi.id = :processedItemId AND h.is_in_pieces = true", 
                 nativeQuery = true)
  boolean isProcessedItemFromPiecesHeat(@Param("processedItemId") Long processedItemId);

  /**
   * Get the finished weight of a processed item in KGS
   *
   * @param processedItemId Processed item ID
   * @return Finished weight in KGS
   */
  @Query(value = "SELECT COALESCE(i.item_finished_weight, 0) " +
                 "FROM processed_item pi " +
                 "JOIN item i ON pi.item_id = i.id " +
                 "WHERE pi.id = :processedItemId", 
                 nativeQuery = true)
  double getProcessedItemFinishedWeight(@Param("processedItemId") Long processedItemId);
}

