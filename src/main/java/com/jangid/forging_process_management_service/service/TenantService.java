package com.jangid.forging_process_management_service.service;

import com.jangid.forging_process_management_service.entities.Tenant;
import com.jangid.forging_process_management_service.exception.ResourceNotFoundException;
import com.jangid.forging_process_management_service.repositories.TenantRepository;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class TenantService {
  private final TenantRepository tenantRepository;

  public TenantService(TenantRepository tenantRepository) {
    this.tenantRepository = tenantRepository;
  }

  @Transactional
  public Tenant createTenant(Tenant tenant) {
    tenant.setCreatedAt(LocalDateTime.now());
    return tenantRepository.save(tenant);
  }

  @Cacheable(value = "tenants", key = "#tenantId")
  public Tenant getTenantById(long tenantId){
    Optional<Tenant> optionalTenant = tenantRepository.findByIdAndDeletedFalse(tenantId);
    if (optionalTenant.isEmpty()){
      log.error("Tenant with id="+tenantId+" not found!");
      throw new RuntimeException("Tenant with id="+tenantId+" not found!");
    }
    return optionalTenant.get();
  }

  public boolean isTenantExists(long tenantId){
    Optional<Tenant> optionalTenant = tenantRepository.findByIdAndDeletedFalse(tenantId);
    if (optionalTenant.isEmpty()){
      log.error("Tenant with id="+tenantId+" not found!");
      return false;
    }
    return true;
  }

  public void validateTenantExists(long tenantId) {
    boolean isTenantExists = isTenantExists(tenantId);
    if (!isTenantExists) {
      log.error("Tenant with id=" + tenantId + " not found!");
      throw new ResourceNotFoundException("Tenant with id=" + tenantId + " not found!");
    }
  }

  public List<Tenant> getAllTenants() {
    return (List<Tenant>) tenantRepository.findAll();
  }
}
