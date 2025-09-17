package com.jangid.forging_process_management_service.repositories.vendor;

import com.jangid.forging_process_management_service.entities.vendor.VendorDispatchHeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VendorDispatchHeatRepository extends JpaRepository<VendorDispatchHeat, Long> {

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
