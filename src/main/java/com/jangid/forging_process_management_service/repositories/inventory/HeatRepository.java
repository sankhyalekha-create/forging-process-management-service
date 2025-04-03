package com.jangid.forging_process_management_service.repositories.inventory;

import com.jangid.forging_process_management_service.entities.inventory.Heat;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
          AND h.availableHeatQuantity > 0
          AND h.deleted = false
          AND rmp.deleted = false
          AND rm.deleted = false
    """)
  List<Heat> findHeatsByProductIdAndTenantId(@Param("productId") Long productId, @Param("tenantId") Long tenantId);

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
}

