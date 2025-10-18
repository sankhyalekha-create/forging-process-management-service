package com.jangid.forging_process_management_service.repositories.settings;

import com.jangid.forging_process_management_service.entities.settings.TenantChallanSettings;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TenantChallanSettingsRepository extends JpaRepository<TenantChallanSettings, Long> {

  /**
   * Find active challan settings for a tenant
   * Uses composite index for optimal performance
   */
  Optional<TenantChallanSettings> findByTenantIdAndIsActiveTrueAndDeletedFalse(Long tenantId);

  /**
   * Check if challan settings exist for a tenant
   * Lightweight existence check without loading full entity
   */
  boolean existsByTenantIdAndDeletedFalse(Long tenantId);

  /**
   * Find challan settings by ID with tenant validation
   * Ensures tenant isolation in multi-tenant environment
   */
  Optional<TenantChallanSettings> findByIdAndTenantIdAndDeletedFalse(Long id, Long tenantId);

}
