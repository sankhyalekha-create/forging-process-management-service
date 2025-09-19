package com.jangid.forging_process_management_service.resource;

import com.jangid.forging_process_management_service.entities.Tenant;
import com.jangid.forging_process_management_service.entitiesRepresentation.tenant.TenantConfigurationRequest;
import com.jangid.forging_process_management_service.entitiesRepresentation.tenant.TenantConfigurationResponse;
import com.jangid.forging_process_management_service.service.TenantService;
import com.jangid.forging_process_management_service.utils.GenericResourceUtils;
import com.jangid.forging_process_management_service.utils.GenericExceptionHandler;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Api(tags = "Tenant Configuration Management", description = "Operations for managing tenant-specific configurations")
public class TenantConfigurationResource {

  @Autowired
  private final TenantService tenantService;

  @GetMapping("/tenant/{tenantId}/configurations")
  @ApiOperation(value = "Get all configurations for a tenant", 
                notes = "Returns all tenant-specific configurations including default values")
  public ResponseEntity<?> getTenantConfigurations(
      @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable("tenantId") String tenantId) {
    try {
      Long tId = GenericResourceUtils.convertResourceIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Invalid tenant ID: " + tenantId));

      Map<String, Object> configurations = tenantService.getAllTenantConfigurations(tId);
      Tenant tenant = tenantService.getTenantById(tId);
      
      TenantConfigurationResponse response = TenantConfigurationResponse.builder()
          .tenantId(tId)
          .tenantName(tenant.getTenantName())
          .configurations(configurations)
          .lastUpdated(tenant.getUpdatedAt())
          .build();
      
      return ResponseEntity.ok(response);
    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "getTenantConfigurations");
    }
  }

  @GetMapping("/tenant/{tenantId}/configuration/{configurationKey}")
  @ApiOperation(value = "Get specific configuration for a tenant", 
                notes = "Returns a specific configuration value or default if not set")
  public ResponseEntity<?> getTenantConfiguration(
      @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable("tenantId") String tenantId,
      @ApiParam(value = "Configuration key", required = true) @PathVariable("configurationKey") String configurationKey) {
    try {
      Long tId = GenericResourceUtils.convertResourceIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Invalid tenant ID: " + tenantId));

      Object configurationValue = tenantService.getTenantConfiguration(tId, configurationKey);
      
      return ResponseEntity.ok(Map.of(
          "tenantId", tId,
          "configurationKey", configurationKey,
          "configurationValue", configurationValue
      ));
    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "getTenantConfiguration");
    }
  }


  @PostMapping("/tenant/{tenantId}/configurations")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Update multiple tenant configurations", 
                notes = "Bulk update multiple configuration key-value pairs for the tenant")
  public ResponseEntity<?> updateTenantConfigurations(
      @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable("tenantId") String tenantId,
      @Validated @RequestBody TenantConfigurationRequest request) {
    try {
      Long tId = GenericResourceUtils.convertResourceIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Invalid tenant ID: " + tenantId));

      if (request.getConfigurations() == null || request.getConfigurations().isEmpty()) {
        throw new RuntimeException("Configurations map is required for bulk update");
      }

      Tenant updatedTenant = tenantService.updateTenantConfigurations(tId, request.getConfigurations());
      
      TenantConfigurationResponse response = TenantConfigurationResponse.builder()
          .tenantId(tId)
          .tenantName(updatedTenant.getTenantName())
          .configurations(request.getConfigurations())
          .lastUpdated(updatedTenant.getUpdatedAt())
          .build();
      
      return ResponseEntity.ok(response);
    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "updateTenantConfigurations");
    }
  }
}
