package com.jangid.forging_process_management_service.repository.gst;

import com.jangid.forging_process_management_service.entities.gst.TenantEInvoiceCredentials;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for managing tenant E-Invoice credentials
 */
@Repository
public interface TenantEInvoiceCredentialsRepository extends JpaRepository<TenantEInvoiceCredentials, Long> {

    /**
     * Find credentials by tenant ID
     */
    Optional<TenantEInvoiceCredentials> findByTenantId(Long tenantId);

}
