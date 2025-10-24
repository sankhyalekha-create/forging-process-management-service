package com.jangid.forging_process_management_service.service.settings;

import com.jangid.forging_process_management_service.entities.Tenant;
import com.jangid.forging_process_management_service.entities.settings.TenantInvoiceSettings;
import com.jangid.forging_process_management_service.entities.settings.TenantChallanSettings;
import com.jangid.forging_process_management_service.repositories.settings.TenantInvoiceSettingsRepository;
import com.jangid.forging_process_management_service.repositories.settings.TenantChallanSettingsRepository;
import com.jangid.forging_process_management_service.service.TenantService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantSettingsService {

  private final TenantInvoiceSettingsRepository invoiceSettingsRepository;
  private final TenantChallanSettingsRepository challanSettingsRepository;
  private final TenantService tenantService;

  /**
   * Get invoice settings for a tenant with caching
   * Creates default settings if none exist
   */
  @Cacheable(value = "tenantInvoiceSettings", key = "#tenantId")
  @Transactional
  public TenantInvoiceSettings getInvoiceSettings(Long tenantId) {
    log.debug("Fetching invoice settings for tenant: {}", tenantId);
    
    return invoiceSettingsRepository.findByTenantIdAndIsActiveTrueAndDeletedFalse(tenantId)
        .orElseGet(() -> createDefaultInvoiceSettings(tenantId));
  }

  /**
   * Get challan settings for a tenant with caching
   * Creates default settings if none exist
   */
  @Cacheable(value = "tenantChallanSettings", key = "#tenantId")
  @Transactional
  public TenantChallanSettings getChallanSettings(Long tenantId) {
    log.debug("Fetching challan settings for tenant: {}", tenantId);
    
    return challanSettingsRepository.findByTenantIdAndIsActiveTrueAndDeletedFalse(tenantId)
        .orElseGet(() -> createDefaultChallanSettings(tenantId));
  }

  /**
   * Update invoice settings and evict cache
   */
  @CacheEvict(value = "tenantInvoiceSettings", key = "#tenantId")
  @Transactional
  public TenantInvoiceSettings updateInvoiceSettings(Long tenantId, TenantInvoiceSettings settings) {
    log.info("Updating invoice settings for tenant: {}", tenantId);
    
    // Validate tenant exists
    tenantService.validateTenantExists(tenantId);
    
    TenantInvoiceSettings existingSettings = invoiceSettingsRepository
        .findByTenantIdAndIsActiveTrueAndDeletedFalse(tenantId)
        .orElseGet(() -> createDefaultInvoiceSettings(tenantId));
    
    // Update fields (keeping ID and tenant reference)
    updateInvoiceSettingsFields(existingSettings, settings);
    
    return invoiceSettingsRepository.save(existingSettings);
  }

  /**
   * Update challan settings and evict cache
   */
  @CacheEvict(value = "tenantChallanSettings", key = "#tenantId")
  @Transactional
  public TenantChallanSettings updateChallanSettings(Long tenantId, TenantChallanSettings settings) {
    log.info("Updating challan settings for tenant: {}", tenantId);
    
    // Validate tenant exists
    tenantService.validateTenantExists(tenantId);
    
    TenantChallanSettings existingSettings = challanSettingsRepository
        .findByTenantIdAndIsActiveTrueAndDeletedFalse(tenantId)
        .orElseGet(() -> createDefaultChallanSettings(tenantId));
    
    // Update fields (keeping ID and tenant reference)
    updateChallanSettingsFields(existingSettings, settings);
    
    return challanSettingsRepository.save(existingSettings);
  }

  /**
   * Create default invoice settings for a tenant
   */
  @Transactional
  public TenantInvoiceSettings createDefaultInvoiceSettings(Long tenantId) {
    log.info("Creating default invoice settings for tenant: {}", tenantId);
    
    Tenant tenant = tenantService.getTenantById(tenantId);
    String tenantInitials = generateTenantInitials(tenant.getTenantName());
    
    TenantInvoiceSettings defaultSettings = TenantInvoiceSettings.builder()
        .tenant(tenant)
        .materialInvoicePrefix(tenantInitials) // Set dynamic prefix based on tenant name
        .build(); // Uses @Builder.Default values for other fields
    
    return invoiceSettingsRepository.save(defaultSettings);
  }

  /**
   * Create default challan settings for a tenant
   */
  @Transactional
  public TenantChallanSettings createDefaultChallanSettings(Long tenantId) {
    log.info("Creating default challan settings for tenant: {}", tenantId);
    
    Tenant tenant = tenantService.getTenantById(tenantId);
    
    TenantChallanSettings defaultSettings = TenantChallanSettings.builder()
        .tenant(tenant)
        .build(); // Uses @Builder.Default values
    
    return challanSettingsRepository.save(defaultSettings);
  }

  /**
   * Evict all settings cache for a tenant
   */
  @CacheEvict(value = {"tenantInvoiceSettings", "tenantChallanSettings"}, key = "#tenantId")
  public void evictSettingsCache(Long tenantId) {
    log.debug("Evicting settings cache for tenant: {}", tenantId);
  }

  /**
   * Helper method to update invoice settings fields
   */
  private void updateInvoiceSettingsFields(TenantInvoiceSettings existing, TenantInvoiceSettings updated) {
    // Job Work Settings
    if (updated.getJobWorkInvoicePrefix() != null) {
      existing.setJobWorkInvoicePrefix(updated.getJobWorkInvoicePrefix());
    }
    if (updated.getJobWorkSeriesFormat() != null) {
      existing.setJobWorkSeriesFormat(updated.getJobWorkSeriesFormat());
    }
    if (updated.getJobWorkStartFrom() != null) {
      existing.setJobWorkStartFrom(updated.getJobWorkStartFrom());
    }
    if (updated.getJobWorkCurrentSequence() != null) {
      existing.setJobWorkCurrentSequence(updated.getJobWorkCurrentSequence());
    }
    
    // Job Work Tax Settings
    if (updated.getJobWorkHsnSacCode() != null) {
      existing.setJobWorkHsnSacCode(updated.getJobWorkHsnSacCode());
    }
    if (updated.getJobWorkCgstRate() != null) {
      existing.setJobWorkCgstRate(updated.getJobWorkCgstRate());
    }
    if (updated.getJobWorkSgstRate() != null) {
      existing.setJobWorkSgstRate(updated.getJobWorkSgstRate());
    }
    if (updated.getJobWorkIgstRate() != null) {
      existing.setJobWorkIgstRate(updated.getJobWorkIgstRate());
    }
    
    // Material Settings
    if (updated.getMaterialInvoicePrefix() != null) {
      existing.setMaterialInvoicePrefix(updated.getMaterialInvoicePrefix());
    }
    if (updated.getMaterialSeriesFormat() != null) {
      existing.setMaterialSeriesFormat(updated.getMaterialSeriesFormat());
    }
    if (updated.getMaterialStartFrom() != null) {
      existing.setMaterialStartFrom(updated.getMaterialStartFrom());
    }
    if (updated.getMaterialCurrentSequence() != null) {
      existing.setMaterialCurrentSequence(updated.getMaterialCurrentSequence());
    }
    
    // Material Tax Settings
    if (updated.getMaterialHsnSacCode() != null) {
      existing.setMaterialHsnSacCode(updated.getMaterialHsnSacCode());
    }
    if (updated.getMaterialCgstRate() != null) {
      existing.setMaterialCgstRate(updated.getMaterialCgstRate());
    }
    if (updated.getMaterialSgstRate() != null) {
      existing.setMaterialSgstRate(updated.getMaterialSgstRate());
    }
    if (updated.getMaterialIgstRate() != null) {
      existing.setMaterialIgstRate(updated.getMaterialIgstRate());
    }
    
    // Manual Invoice Settings
    if (updated.getManualInvoiceEnabled() != null) {
      existing.setManualInvoiceEnabled(updated.getManualInvoiceEnabled());
    }
    if (updated.getManualInvoiceTitle() != null) {
      existing.setManualInvoiceTitle(updated.getManualInvoiceTitle());
    }
    if (updated.getManualStartFrom() != null) {
      existing.setManualStartFrom(updated.getManualStartFrom());
    }
    
    // Integration Settings (reduced)
    if (updated.getShowVehicleNumber() != null) {
      existing.setShowVehicleNumber(updated.getShowVehicleNumber());
    }
    
    // E-Way Bill & E-Invoice Settings
    if (updated.getActivateEWayBill() != null) {
      existing.setActivateEWayBill(updated.getActivateEWayBill());
    }
    if (updated.getActivateEInvoice() != null) {
      existing.setActivateEInvoice(updated.getActivateEInvoice());
    }
    if (updated.getActivateTCS() != null) {
      existing.setActivateTCS(updated.getActivateTCS());
    }
    
    // Bank Details
    if (updated.getTransporterDetails() != null) {
      existing.setTransporterDetails(updated.getTransporterDetails());
    }
    if (updated.getBankDetailsSameAsJobwork() != null) {
      existing.setBankDetailsSameAsJobwork(updated.getBankDetailsSameAsJobwork());
    }
    if (updated.getBankName() != null) {
      existing.setBankName(updated.getBankName());
    }
    if (updated.getAccountNumber() != null) {
      existing.setAccountNumber(updated.getAccountNumber());
    }
    if (updated.getIfscCode() != null) {
      existing.setIfscCode(updated.getIfscCode());
    }
    
    // Terms and Conditions
    if (updated.getJobWorkTermsAndConditions() != null) {
      existing.setJobWorkTermsAndConditions(updated.getJobWorkTermsAndConditions());
    }

    if (updated.getMaterialTermsAndConditions() != null) {
      existing.setMaterialTermsAndConditions(updated.getMaterialTermsAndConditions());
    }
  }

  /**
   * Helper method to update challan settings fields
   */
  private void updateChallanSettingsFields(TenantChallanSettings existing, TenantChallanSettings updated) {
    // Challan Configuration
    if (updated.getStartFrom() != null) {
      existing.setStartFrom(updated.getStartFrom());
    }
    if (updated.getSeriesFormat() != null) {
      existing.setSeriesFormat(updated.getSeriesFormat());
    }
    
    // Tax Configuration
    if (updated.getHsnSacCode() != null) {
      existing.setHsnSacCode(updated.getHsnSacCode());
    }
    if (updated.getCgstRate() != null) {
      existing.setCgstRate(updated.getCgstRate());
    }
    if (updated.getSgstRate() != null) {
      existing.setSgstRate(updated.getSgstRate());
    }
    if (updated.getIgstRate() != null) {
      existing.setIgstRate(updated.getIgstRate());
    }
    if (updated.getActivateTCS() != null) {
      existing.setActivateTCS(updated.getActivateTCS());
    }
    
    // Bank Details
    if (updated.getBankDetailsSameAsJobwork() != null) {
      existing.setBankDetailsSameAsJobwork(updated.getBankDetailsSameAsJobwork());
    }
    if (updated.getBankName() != null) {
      existing.setBankName(updated.getBankName());
    }
    if (updated.getAccountNumber() != null) {
      existing.setAccountNumber(updated.getAccountNumber());
    }
    if (updated.getIfscCode() != null) {
      existing.setIfscCode(updated.getIfscCode());
    }
    
    // Terms and Conditions
    if (updated.getTermsAndConditions() != null) {
      existing.setTermsAndConditions(updated.getTermsAndConditions());
    }
  }

  /**
   * Generate tenant initials from tenant name
   * Examples: "Jangid Steel Turning" -> "JST", "ABC Company Ltd" -> "ACL"
   */
  private String generateTenantInitials(String tenantName) {
    if (tenantName == null || tenantName.trim().isEmpty()) {
      return "ORG"; // Default fallback
    }
    
    // Split by spaces and get first letter of each word
    String[] words = tenantName.trim().toUpperCase().split("\\s+");
    StringBuilder initials = new StringBuilder();
    
    for (String word : words) {
      if (!word.isEmpty() && Character.isLetter(word.charAt(0))) {
        initials.append(word.charAt(0));
      }
    }
    
    // If no valid initials found, use first 3 characters of tenant name
    if (initials.length() == 0) {
      String cleanName = tenantName.replaceAll("[^A-Za-z]", "").toUpperCase();
      return cleanName.length() >= 3 ? cleanName.substring(0, 3) : cleanName + "ORG".substring(cleanName.length());
    }
    
    // Limit to maximum 10 characters as per database constraint
    String result = initials.toString();
    return result.length() > 10 ? result.substring(0, 10) : result;
  }

  /**
   * Increment invoice sequence for a specific invoice type
   * This method is used when generating invoices to ensure unique sequential numbers
   */
  @CacheEvict(value = "tenantInvoiceSettings", key = "#tenantId")
  @Transactional
  public void incrementInvoiceSequence(Long tenantId, String invoiceType) {
    log.debug("Incrementing invoice sequence for tenant: {} and type: {}", tenantId, invoiceType);
    
    TenantInvoiceSettings settings = getInvoiceSettings(tenantId);
    
    switch (invoiceType) {
      case "MATERIAL_INVOICE":
        settings.setMaterialCurrentSequence(settings.getMaterialCurrentSequence() + 1);
        break;
      case "JOB_WORK_INVOICE":
        settings.setJobWorkCurrentSequence(settings.getJobWorkCurrentSequence() + 1);
        break;
      default:
        log.warn("Unsupported invoice type for sequence increment: {}", invoiceType);
        return;
    }
    
    invoiceSettingsRepository.save(settings);
    log.debug("Successfully incremented sequence for tenant: {} and type: {}", tenantId, invoiceType);
  }
}
