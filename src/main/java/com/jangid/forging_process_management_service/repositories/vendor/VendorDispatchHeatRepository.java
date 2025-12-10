package com.jangid.forging_process_management_service.repositories.vendor;

import com.jangid.forging_process_management_service.entities.vendor.VendorDispatchHeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VendorDispatchHeatRepository extends JpaRepository<VendorDispatchHeat, Long> {

    /**
     * Check if a heat is used in any non-deleted vendor dispatch heat record
     * 
     * @param heatId The heat ID to check
     * @return true if heat is in use, false otherwise
     */
    boolean existsByHeatIdAndDeletedFalse(Long heatId);

    /**
     * Count how many times a heat is used in non-deleted vendor dispatch heat records
     * 
     * @param heatId The heat ID to check
     * @return count of usage
     */
    @Query("SELECT COUNT(vdh) FROM VendorDispatchHeat vdh WHERE vdh.heat.id = :heatId AND vdh.deleted = false")
    Long countByHeatIdAndDeletedFalse(@Param("heatId") Long heatId);

    @Query("SELECT vdh FROM VendorDispatchHeat vdh " +
           "JOIN vdh.processedItemVendorDispatchBatch pivdb " +
           "JOIN pivdb.vendorDispatchBatch vdb " +
           "WHERE vdb.vendor.id = :vendorId " +
           "AND vdb.tenant.id = :tenantId " +
           "AND vdh.deleted = false " +
           "AND vdb.deleted = false")
    List<VendorDispatchHeat> findAllByVendorIdAndTenantId(@Param("vendorId") Long vendorId, @Param("tenantId") Long tenantId);

    @Query("SELECT SUM(vdh.quantityUsed) FROM VendorDispatchHeat vdh " +
           "JOIN vdh.processedItemVendorDispatchBatch pivdb " +
           "JOIN pivdb.vendorDispatchBatch vdb " +
           "WHERE vdb.vendor.id = :vendorId " +
           "AND vdb.tenant.id = :tenantId " +
           "AND vdh.deleted = false " +
           "AND vdb.deleted = false " +
           "AND vdh.quantityUsed IS NOT NULL")
    Double sumQuantityConsumedByVendor(@Param("vendorId") Long vendorId, @Param("tenantId") Long tenantId);

    @Query("SELECT SUM(vdh.piecesUsed) FROM VendorDispatchHeat vdh " +
           "JOIN vdh.processedItemVendorDispatchBatch pivdb " +
           "JOIN pivdb.vendorDispatchBatch vdb " +
           "WHERE vdb.vendor.id = :vendorId " +
           "AND vdb.tenant.id = :tenantId " +
           "AND vdh.deleted = false " +
           "AND vdb.deleted = false " +
           "AND vdh.piecesUsed IS NOT NULL")
    Long sumPiecesConsumedByVendor(@Param("vendorId") Long vendorId, @Param("tenantId") Long tenantId);
}