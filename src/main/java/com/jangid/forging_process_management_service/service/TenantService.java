package com.jangid.forging_process_management_service.service;

import com.jangid.forging_process_management_service.entities.Tenant;
import com.jangid.forging_process_management_service.exception.ResourceNotFoundException;
import com.jangid.forging_process_management_service.repositories.TenantRepository;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

  public Tenant getTenantByTenantName(String tenantName){
    Optional<Tenant> optionalTenant = tenantRepository.findByTenantNameAndDeletedFalse(tenantName);
    if (optionalTenant.isEmpty()){
      log.error("Tenant with name="+tenantName+" not found!");
      throw new RuntimeException("Tenant with name="+tenantName+" not found!");
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

  @Transactional
  @CacheEvict(value = "tenants", key = "#tenantId")
  public Tenant updateTenantConfigurations(Long tenantId, Map<String, Object> configurations) {
    Tenant tenant = getTenantById(tenantId);
    
    // Initialize configurations map if null
    if (tenant.getTenantConfigurations() == null) {
      tenant.setTenantConfigurations(new HashMap<>());
    }
    
    // Update all configurations
    tenant.getTenantConfigurations().putAll(configurations);
    
    log.info("Updated {} tenant configurations for tenantId={}", 
             configurations.size(), tenantId);
    
    return tenantRepository.save(tenant);
  }

  @Cacheable(value = "tenantConfigurations", key = "#tenantId + '_' + #configurationKey")
  public Object getTenantConfiguration(Long tenantId, String configurationKey) {
    Tenant tenant = getTenantById(tenantId);
    return tenant.getTenantConfigurations().get(configurationKey);
  }

  @Cacheable(value = "tenantConfigurations", key = "#tenantId + '_all'")
  public Map<String, Object> getAllTenantConfigurations(Long tenantId) {
    Tenant tenant = getTenantById(tenantId);
    return tenant.getTenantConfigurations();
  }
}
