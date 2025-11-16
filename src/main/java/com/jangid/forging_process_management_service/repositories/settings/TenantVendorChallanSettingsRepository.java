package com.jangid.forging_process_management_service.repositories.settings;

import com.jangid.forging_process_management_service.entities.settings.TenantVendorChallanSettings;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TenantVendorChallanSettingsRepository extends CrudRepository<TenantVendorChallanSettings, Long> {
    Optional<TenantVendorChallanSettings> findByTenantIdAndIsActiveTrueAndDeletedFalse(Long tenantId);
    Optional<TenantVendorChallanSettings> findByTenantIdAndDeletedFalse(Long tenantId);
}