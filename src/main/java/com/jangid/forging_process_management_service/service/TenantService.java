package com.jangid.forging_process_management_service.service;

import com.jangid.forging_process_management_service.entities.Tenant;
import com.jangid.forging_process_management_service.repositories.TenantRepository;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

  public Tenant getTenantById(long tenantId){
    Optional<Tenant> optionalTenant = tenantRepository.findById(tenantId);
    if (optionalTenant.isEmpty()){
      log.error("Tenant with id="+tenantId+" not found!");
      throw new RuntimeException("Tenant with id="+tenantId+" not found!");
    }
    return optionalTenant.get();
  }

  public boolean isTenantExists(long tenantId){
    Optional<Tenant> optionalTenant = tenantRepository.findById(tenantId);
    if (optionalTenant.isEmpty()){
      log.error("Tenant with id="+tenantId+" not found!");
      return false;
    }
    return true;
  }

  public List<Tenant> getAllTenants() {
    return (List<Tenant>) tenantRepository.findAll();
  }
}
