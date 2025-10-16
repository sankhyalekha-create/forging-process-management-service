package com.jangid.forging_process_management_service.service.order;

import com.jangid.forging_process_management_service.entities.Tenant;
import com.jangid.forging_process_management_service.entities.order.TenantOrderSettings;
import com.jangid.forging_process_management_service.entitiesRepresentation.order.TenantOrderSettingsRepresentation;
import com.jangid.forging_process_management_service.repositories.order.TenantOrderSettingsRepository;
import com.jangid.forging_process_management_service.service.TenantService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class TenantOrderSettingsService {

  @Autowired
  private TenantOrderSettingsRepository settingsRepository;

  @Autowired
  private TenantService tenantService;

  private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

  /**
   * Get tenant settings or create and persist default if not exists
   */
  @Transactional
  public TenantOrderSettingsRepresentation getTenantSettings(Long tenantId) {
    log.info("Getting order settings for tenant: {}", tenantId);
    
    TenantOrderSettings settings = settingsRepository.findByTenantId(tenantId)
      .orElseGet(() -> {
        log.info("No existing settings found for tenant: {}. Creating and persisting default settings.", tenantId);
        
        // Validate tenant exists
        Tenant tenant = tenantService.getTenantById(tenantId);
        
        // Create and save default settings
        TenantOrderSettings defaultSettings = TenantOrderSettings.builder()
          .tenant(tenant)
          .warningDays(3)
          .enableHighlighting(true)
          .overdueColor("#ffebee")
          .warningColor("#fff8e1")
          .autoRefreshInterval(30)
          .enableNotifications(true)
          .showCompletedOrders(true)
          .defaultPriority(3)
          .build();
        
        TenantOrderSettings savedSettings = settingsRepository.save(defaultSettings);
        log.info("Successfully created and saved default settings for tenant: {}", tenantId);
        return savedSettings;
      });

    return convertToRepresentation(settings);
  }

  /**
   * Create or update tenant settings
   */
  @Transactional
  public TenantOrderSettingsRepresentation saveSettings(Long tenantId, TenantOrderSettingsRepresentation settingsRep) {
    log.info("Saving order settings for tenant: {}", tenantId);

    // Validate tenant
    Tenant tenant = tenantService.getTenantById(tenantId);

    // Check if settings exist
    TenantOrderSettings settings = settingsRepository.findByTenantId(tenantId)
      .orElse(null);

    if (settings == null) {
      // Create new settings
      settings = TenantOrderSettings.builder()
        .tenant(tenant)
        .build();
      log.info("Creating new settings for tenant: {}", tenantId);
    } else {
      log.info("Updating existing settings for tenant: {}", tenantId);
    }

    // Update settings fields
    settings.setWarningDays(settingsRep.getWarningDays() != null ? settingsRep.getWarningDays() : 3);
    settings.setEnableHighlighting(settingsRep.getEnableHighlighting() != null ? settingsRep.getEnableHighlighting() : true);
    settings.setOverdueColor(settingsRep.getOverdueColor() != null ? settingsRep.getOverdueColor() : "#ffebee");
    settings.setWarningColor(settingsRep.getWarningColor() != null ? settingsRep.getWarningColor() : "#fff8e1");
    settings.setAutoRefreshInterval(settingsRep.getAutoRefreshInterval() != null ? settingsRep.getAutoRefreshInterval() : 30);
    settings.setEnableNotifications(settingsRep.getEnableNotifications() != null ? settingsRep.getEnableNotifications() : true);
    settings.setShowCompletedOrders(settingsRep.getShowCompletedOrders() != null ? settingsRep.getShowCompletedOrders() : true);
    settings.setDefaultPriority(settingsRep.getDefaultPriority() != null ? settingsRep.getDefaultPriority() : 3);

    settings = settingsRepository.save(settings);
    log.info("Successfully saved settings for tenant: {}", tenantId);

    return convertToRepresentation(settings);
  }

  /**
   * Reset settings to default values
   */
  @Transactional
  public TenantOrderSettingsRepresentation resetSettings(Long tenantId) {
    log.info("Resetting order settings to defaults for tenant: {}", tenantId);

    settingsRepository.findByTenantId(tenantId)
      .ifPresent(settingsRepository::delete);

    TenantOrderSettings defaultSettings = createDefaultSettings(tenantId);
    return convertToRepresentation(defaultSettings);
  }

  /**
   * Create default settings (not persisted)
   */
  private TenantOrderSettings createDefaultSettings(Long tenantId) {
    Tenant tenant = new Tenant();
    tenant.setId(tenantId);

    return TenantOrderSettings.builder()
      .tenant(tenant)
      .warningDays(3)
      .enableHighlighting(true)
      .overdueColor("#ffebee")
      .warningColor("#fff8e1")
      .autoRefreshInterval(30)
      .enableNotifications(true)
      .showCompletedOrders(true)
      .defaultPriority(3)
      .build();
  }

  /**
   * Convert entity to representation
   */
  private TenantOrderSettingsRepresentation convertToRepresentation(TenantOrderSettings settings) {
    return TenantOrderSettingsRepresentation.builder()
      .id(settings.getId())
      .tenantId(settings.getTenant() != null ? settings.getTenant().getId() : null)
      .warningDays(settings.getWarningDays())
      .enableHighlighting(settings.getEnableHighlighting())
      .overdueColor(settings.getOverdueColor())
      .warningColor(settings.getWarningColor())
      .autoRefreshInterval(settings.getAutoRefreshInterval())
      .enableNotifications(settings.getEnableNotifications())
      .showCompletedOrders(settings.getShowCompletedOrders())
      .defaultPriority(settings.getDefaultPriority())
      .createdAt(settings.getCreatedAt() != null ? settings.getCreatedAt().format(FORMATTER) : null)
      .updatedAt(settings.getUpdatedAt() != null ? settings.getUpdatedAt().format(FORMATTER) : null)
      .build();
  }
}

