package com.jangid.forging_process_management_service.resource.order;

import com.jangid.forging_process_management_service.configuration.security.TenantContextHolder;
import com.jangid.forging_process_management_service.entitiesRepresentation.order.TenantOrderSettingsRepresentation;
import com.jangid.forging_process_management_service.service.order.TenantOrderSettingsService;
import com.jangid.forging_process_management_service.utils.GenericExceptionHandler;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/api")
@Api(tags = "Tenant Order Settings", description = "Operations for managing tenant-level order settings")
public class TenantOrderSettingsResource {

  @Autowired
  private TenantOrderSettingsService settingsService;

  @GetMapping("/order-settings")
  @ApiOperation(value = "Get tenant order settings", 
               notes = "Returns order management settings for the tenant. Creates default settings if none exist.")
  public ResponseEntity<?> getTenantSettings() {
    try {
      Long tenantIdLong = TenantContextHolder.getAuthenticatedTenantId();

      TenantOrderSettingsRepresentation settings = settingsService.getTenantSettings(tenantIdLong);
      return ResponseEntity.ok(settings);

    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "getTenantSettings");
    }
  }

  @PostMapping("/order-settings")
  @ApiOperation(value = "Create tenant order settings", 
               notes = "Creates new order management settings for the tenant")
  public ResponseEntity<?> createTenantSettings(
      @Valid @RequestBody TenantOrderSettingsRepresentation settings) {
    try {
      Long tenantIdLong = TenantContextHolder.getAuthenticatedTenantId();

      TenantOrderSettingsRepresentation savedSettings = settingsService.saveSettings(tenantIdLong, settings);
      return ResponseEntity.status(HttpStatus.CREATED).body(savedSettings);

    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "createTenantSettings");
    }
  }

  @PutMapping("/order-settings")
  @ApiOperation(value = "Update tenant order settings", 
               notes = "Updates order management settings for the tenant")
  public ResponseEntity<?> updateTenantSettings(
      @Valid @RequestBody TenantOrderSettingsRepresentation settings) {
    try {
      Long tenantIdLong = TenantContextHolder.getAuthenticatedTenantId();

      TenantOrderSettingsRepresentation updatedSettings = settingsService.saveSettings(tenantIdLong, settings);
      return ResponseEntity.ok(updatedSettings);

    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "updateTenantSettings");
    }
  }

  @DeleteMapping("/order-settings")
  @ApiOperation(value = "Reset tenant order settings", 
               notes = "Resets order management settings to default values for the tenant")
  public ResponseEntity<?> resetTenantSettings() {
    try {
      Long tenantIdLong = TenantContextHolder.getAuthenticatedTenantId();

      TenantOrderSettingsRepresentation defaultSettings = settingsService.resetSettings(tenantIdLong);
      return ResponseEntity.ok(defaultSettings);

    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "resetTenantSettings");
    }
  }
}

