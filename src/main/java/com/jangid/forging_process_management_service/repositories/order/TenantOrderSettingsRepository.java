package com.jangid.forging_process_management_service.repositories.order;

import com.jangid.forging_process_management_service.entities.order.TenantOrderSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TenantOrderSettingsRepository extends JpaRepository<TenantOrderSettings, Long> {

  /**
   * Find settings by tenant ID
   */
  @Query("SELECT s FROM TenantOrderSettings s WHERE s.tenant.id = :tenantId")
  Optional<TenantOrderSettings> findByTenantId(@Param("tenantId") Long tenantId);

  /**
   * Check if settings exist for a tenant
   */
  @Query("SELECT COUNT(s) > 0 FROM TenantOrderSettings s WHERE s.tenant.id = :tenantId")
  boolean existsByTenantId(@Param("tenantId") Long tenantId);

  /**
   * Delete settings by tenant ID
   */
  @Query("DELETE FROM TenantOrderSettings s WHERE s.tenant.id = :tenantId")
  void deleteByTenantId(@Param("tenantId") Long tenantId);
}

