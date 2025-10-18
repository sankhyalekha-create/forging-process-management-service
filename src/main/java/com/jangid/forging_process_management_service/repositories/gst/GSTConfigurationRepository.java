package com.jangid.forging_process_management_service.repositories.gst;

import com.jangid.forging_process_management_service.entities.gst.GSTConfiguration;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GSTConfigurationRepository extends JpaRepository<GSTConfiguration, Long> {

    // Find by tenant ID
    Optional<GSTConfiguration> findByTenantIdAndDeletedFalse(Long tenantId);
    
    // Find by company GSTIN
    Optional<GSTConfiguration> findByCompanyGstinAndDeletedFalse(String companyGstin);
    
    // Check if configuration exists for tenant
    boolean existsByTenantIdAndDeletedFalse(Long tenantId);
    
    // Find active configuration for tenant
    @Query("SELECT gc FROM GSTConfiguration gc WHERE gc.tenant.id = :tenantId " +
           "AND gc.isActive = true AND gc.deleted = false")
    Optional<GSTConfiguration> findActiveConfigurationByTenantId(@Param("tenantId") Long tenantId);
}
