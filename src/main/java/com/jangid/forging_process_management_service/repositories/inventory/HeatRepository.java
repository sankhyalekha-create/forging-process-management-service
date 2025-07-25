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
  @Query("SELECT h FROM heat h WHERE h.heatNumber = :heatNumber AND h.active = true AND h.deleted = false")
  List<Heat> findByHeatNumberAndDeletedIsFalse(@Param("heatNumber") String heatNumber);
  Optional<Heat> findByIdAndActiveTrueAndDeletedFalse(long heatId);

  // For heat status management - can find any heat regardless of active status
  Optional<Heat> findByIdAndDeletedFalse(long heatId);


  @Query("""
        SELECT h
        FROM heat h
        JOIN h.rawMaterialProduct rmp
        JOIN rmp.rawMaterial rm
        JOIN rmp.product p
        WHERE p.id = :productId
          AND rm.tenant.id = :tenantId
          AND (h.availableHeatQuantity > 0)
          AND h.active = true
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
          AND h.active = true
          AND h.deleted = false
          AND rmp.deleted = false
          AND rm.deleted = false
    """)
  List<Heat> findHeatsHavingPiecesByProductIdAndTenantId(@Param("productId") Long productId, @Param("tenantId") Long tenantId);

  @Modifying
  @Query("UPDATE heat h SET h.availableHeatQuantity = h.availableHeatQuantity + :quantity WHERE h.id = :heatId AND h.deleted = false")
  void incrementAvailableHeatQuantity(@Param("heatId") Long heatId, @Param("quantity") Double quantity);

  /**
   * Find inactive heats that have available quantities for a specific product
   */
  @Query("""
        SELECT h
        FROM heat h
        JOIN h.rawMaterialProduct rmp
        JOIN rmp.rawMaterial rm
        JOIN rmp.product p
        WHERE p.id = :productId
          AND rm.tenant.id = :tenantId
          AND (h.availableHeatQuantity > 0)
          AND h.active = false
          AND h.deleted = false
          AND rmp.deleted = false
          AND rm.deleted = false
    """)
  List<Heat> findInactiveHeatsHavingQuantitiesByProductIdAndTenantId(@Param("productId") Long productId, @Param("tenantId") Long tenantId);

  /**
   * Find inactive heats that have available pieces for a specific product
   */
  @Query("""
        SELECT h
        FROM heat h
        JOIN h.rawMaterialProduct rmp
        JOIN rmp.rawMaterial rm
        JOIN rmp.product p
        WHERE p.id = :productId
          AND rm.tenant.id = :tenantId
          AND (h.availablePiecesCount > 0)
          AND h.active = false
          AND h.deleted = false
          AND rmp.deleted = false
          AND rm.deleted = false
    """)
  List<Heat> findInactiveHeatsHavingPiecesByProductIdAndTenantId(@Param("productId") Long productId, @Param("tenantId") Long tenantId);

  @Query("""
        SELECT h
        FROM heat h
        JOIN h.rawMaterialProduct rmp
        JOIN rmp.rawMaterial rm
        WHERE rm.tenant.id = :tenantId
          AND h.active = true
          AND h.deleted = false
          AND rmp.deleted = false
          AND rm.deleted = false
        ORDER BY h.createdAt DESC
    """)
  Page<Heat> findHeatsByTenantId(@Param("tenantId") Long tenantId, Pageable pageable);

  @Query("""
        SELECT h
        FROM heat h
        JOIN h.rawMaterialProduct rmp
        JOIN rmp.rawMaterial rm
        WHERE rm.tenant.id = :tenantId
          AND h.active = true
          AND h.deleted = false
          AND rmp.deleted = false
          AND rm.deleted = false
        ORDER BY h.createdAt DESC
    """)
  List<Heat> findAllHeatsByTenantId(@Param("tenantId") Long tenantId);

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
          "AND h.active = true " +
         "AND h.deleted = false " +
         "AND rmp.deleted = false " +
         "AND rm.deleted = false " +
         "ORDER BY rm.rawMaterialReceivingDate ASC")
  List<Heat> findHeatsByDateRange(
          @Param("tenantId") Long tenantId,
          @Param("startDateTime") LocalDateTime startDateTime,
          @Param("endDateTime") LocalDateTime endDateTime);



  // Search method for the search API with pagination support
  @Query("""
        SELECT h
        FROM heat h
        JOIN h.rawMaterialProduct rmp
        JOIN rmp.rawMaterial rm
        WHERE rm.tenant.id = :tenantId
          AND LOWER(h.heatNumber) LIKE LOWER(CONCAT('%', :heatNumber, '%'))
          AND h.active = true
          AND h.deleted = false
          AND rmp.deleted = false
          AND rm.deleted = false
        ORDER BY h.heatNumber ASC
    """)
  Page<Heat> findHeatsByHeatNumberContainingIgnoreCase(@Param("tenantId") Long tenantId, @Param("heatNumber") String heatNumber, Pageable pageable);

  // New methods for active/inactive heat management

  /**
   * Find heats by tenant with active status filter and pagination
   */
  @Query("""
        SELECT h
        FROM heat h
        JOIN h.rawMaterialProduct rmp
        JOIN rmp.rawMaterial rm
        WHERE rm.tenant.id = :tenantId
          AND h.active = :active
          AND h.deleted = false
          AND rmp.deleted = false
          AND rm.deleted = false
        ORDER BY h.createdAt DESC
    """)
  Page<Heat> findHeatsByTenantIdAndActiveStatus(@Param("tenantId") Long tenantId, @Param("active") Boolean active, Pageable pageable);

  /**
   * Find all heats by tenant with active status filter
   */
  @Query("""
        SELECT h
        FROM heat h
        JOIN h.rawMaterialProduct rmp
        JOIN rmp.rawMaterial rm
        WHERE rm.tenant.id = :tenantId
          AND h.active = :active
          AND h.deleted = false
          AND rmp.deleted = false
          AND rm.deleted = false
        ORDER BY h.createdAt DESC
    """)
  List<Heat> findAllHeatsByTenantIdAndActiveStatus(@Param("tenantId") Long tenantId, @Param("active") Boolean active);

  /**
   * Update heat active status
   */
  @Modifying
  @Query("UPDATE heat h SET h.active = :active WHERE h.id = :heatId AND h.deleted = false")
  void updateHeatActiveStatus(@Param("heatId") Long heatId, @Param("active") Boolean active);

}

