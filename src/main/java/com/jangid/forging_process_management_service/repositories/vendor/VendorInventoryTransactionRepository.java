package com.jangid.forging_process_management_service.repositories.vendor;

import com.jangid.forging_process_management_service.entities.vendor.VendorInventoryTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface VendorInventoryTransactionRepository extends JpaRepository<VendorInventoryTransaction, Long> {

    @Query("SELECT vit FROM VendorInventoryTransaction vit WHERE vit.tenant.id = :tenantId ORDER BY vit.createdAt DESC")
    Page<VendorInventoryTransaction> findByTenantId(@Param("tenantId") Long tenantId, Pageable pageable);

    @Query("SELECT vit FROM VendorInventoryTransaction vit WHERE vit.tenant.id = :tenantId AND vit.vendor.id = :vendorId ORDER BY vit.createdAt DESC")
    Page<VendorInventoryTransaction> findByTenantIdAndVendorId(@Param("tenantId") Long tenantId, @Param("vendorId") Long vendorId, Pageable pageable);

    @Query("SELECT MAX(vit.transactionDateTime) FROM VendorInventoryTransaction vit " +
           "JOIN vit.transactionItems item " +
           "WHERE item.heat.id = :heatId")
    java.time.LocalDateTime findLatestTransactionDateTimeByHeatId(@Param("heatId") Long heatId);
} 