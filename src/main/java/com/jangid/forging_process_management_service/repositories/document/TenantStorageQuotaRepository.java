package com.jangid.forging_process_management_service.repositories.document;

import com.jangid.forging_process_management_service.entities.document.TenantStorageQuota;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TenantStorageQuotaRepository extends JpaRepository<TenantStorageQuota, Long> {
    
    // Find quota by tenant
    Optional<TenantStorageQuota> findByTenant_Id(Long tenantId);
    
    // Update used storage for a tenant
    @Modifying
    @Query("UPDATE TenantStorageQuota tsq SET tsq.usedStorageBytes = tsq.usedStorageBytes + :bytes " +
           "WHERE tsq.tenant.id = :tenantId")
    int addUsedStorage(@Param("tenantId") Long tenantId, @Param("bytes") Long bytes);
    
    @Modifying
    @Query("UPDATE TenantStorageQuota tsq SET tsq.usedStorageBytes = GREATEST(0, tsq.usedStorageBytes - :bytes) " +
           "WHERE tsq.tenant.id = :tenantId")
    int removeUsedStorage(@Param("tenantId") Long tenantId, @Param("bytes") Long bytes);
    
}
