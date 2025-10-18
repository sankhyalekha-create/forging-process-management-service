package com.jangid.forging_process_management_service.repositories.settings;

import com.jangid.forging_process_management_service.entities.settings.TenantInvoiceSettings;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TenantInvoiceSettingsRepository extends JpaRepository<TenantInvoiceSettings, Long> {

  /**
   * Find active invoice settings for a tenant
   * Uses composite index for optimal performance
   */
  Optional<TenantInvoiceSettings> findByTenantIdAndIsActiveTrueAndDeletedFalse(Long tenantId);

  /**
   * Check if invoice settings exist for a tenant
   * Lightweight existence check without loading full entity
   */
  boolean existsByTenantIdAndDeletedFalse(Long tenantId);

  /**
   * Find invoice settings by ID with tenant validation
   * Ensures tenant isolation in multi-tenant environment
   */
  Optional<TenantInvoiceSettings> findByIdAndTenantIdAndDeletedFalse(Long id, Long tenantId);

}
