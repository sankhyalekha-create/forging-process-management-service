package com.jangid.forging_process_management_service.repository.gst;

import com.jangid.forging_process_management_service.entities.gst.TenantEwayBillCredentials;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for TenantEwayBillCredentials
 */
@Repository
public interface TenantEwayBillCredentialsRepository extends JpaRepository<TenantEwayBillCredentials, Long> {

    /**
     * Find credentials by tenant ID
     */
    Optional<TenantEwayBillCredentials> findByTenantId(Long tenantId);

}
