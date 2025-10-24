package com.jangid.forging_process_management_service.resource.settings;

import com.jangid.forging_process_management_service.configuration.security.TenantContextHolder;
import com.jangid.forging_process_management_service.entities.Tenant;
import com.jangid.forging_process_management_service.entities.settings.TenantInvoiceSettings;
import com.jangid.forging_process_management_service.entities.settings.TenantChallanSettings;
import com.jangid.forging_process_management_service.entitiesRepresentation.settings.BillingSettingsRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.settings.InvoiceSettingsRequest;
import com.jangid.forging_process_management_service.entitiesRepresentation.settings.ChallanSettingsRequest;
import com.jangid.forging_process_management_service.utils.GenericExceptionHandler;
import com.jangid.forging_process_management_service.service.TenantService;
import com.jangid.forging_process_management_service.service.settings.TenantSettingsService;
import com.jangid.forging_process_management_service.assemblers.settings.BillingSettingsAssembler;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * REST Controller for managing tenant billing settings including invoice and challan configurations.
 * 
 * This controller provides endpoints for:
 * - Fetching complete billing settings for a tenant
 * - Updating invoice settings
 * - Updating challan settings
 * 
 * All operations are tenant-scoped and include proper validation and error handling.
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Api(tags = "Billing Settings Management", description = "Operations for managing tenant billing settings")
public class BillingSettingsResource {

  private final TenantSettingsService tenantSettingsService;
  private final TenantService tenantService;
  private final BillingSettingsAssembler billingSettingsAssembler;

  /**
   * Fetch complete billing settings for a tenant
   * 
   * @return BillingSettingsResponse containing both invoice and challan settings
   */
  @GetMapping("/billing-settings")
  @ApiOperation(value = "Get billing settings for tenant", 
                notes = "Returns complete billing settings including invoice and challan configurations")
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<?> getBillingSettings() {

    Long tenantIdLong = TenantContextHolder.getAuthenticatedTenantId();
    try {
      log.debug("Fetching billing settings for tenant: {}", tenantIdLong);
      
      // Validate tenant exists
      tenantService.validateTenantExists(tenantIdLong);
      Tenant tenant = tenantService.getTenantById(tenantIdLong);
      
      // Fetch settings
      TenantInvoiceSettings invoiceSettings = tenantSettingsService.getInvoiceSettings(tenantIdLong);
      TenantChallanSettings challanSettings = tenantSettingsService.getChallanSettings(tenantIdLong);
      
      // Build response using assembler
      BillingSettingsRepresentation response = billingSettingsAssembler.disassembleBillingSettings(
          tenant, invoiceSettings, challanSettings);
      
      log.info("Successfully fetched billing settings for tenant: {}", tenantIdLong);
      return ResponseEntity.ok(response);
      
    } catch (Exception exception) {
      log.error("Error fetching billing settings for tenant: {}", tenantIdLong, exception);
      return GenericExceptionHandler.handleException(exception, "getBillingSettings");
    }
  }

  /**
   * Update invoice settings for a tenant
   * 
   * @param request The invoice settings update request
   * @return Updated invoice settings
   */
  @PostMapping("/billing-settings/invoice")
  @ApiOperation(value = "Update invoice settings", 
                notes = "Updates invoice configuration including prefixes, tax rates, and other settings")
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<?> updateInvoiceSettings(
      @ApiParam(value = "Invoice settings update request", required = true) @Valid @RequestBody InvoiceSettingsRequest request) {

    Long tenantIdLong = TenantContextHolder.getAuthenticatedTenantId();
    try {
      log.debug("Updating invoice settings for tenant: {}", tenantIdLong);
      
      // Convert request to entity using assembler
      TenantInvoiceSettings settingsToUpdate = billingSettingsAssembler.assembleInvoiceSettings(request);
      
      // Update settings
      TenantInvoiceSettings updatedSettings = tenantSettingsService.updateInvoiceSettings(tenantIdLong, settingsToUpdate);
      
      // Return updated settings using assembler
      BillingSettingsRepresentation.InvoiceSettings response = billingSettingsAssembler.disassembleInvoiceSettings(updatedSettings);
      
      log.info("Successfully updated invoice settings for tenant: {}", tenantIdLong);
      return ResponseEntity.ok(response);
      
    } catch (Exception exception) {
      log.error("Error updating invoice settings for tenant: {}", tenantIdLong, exception);
      return GenericExceptionHandler.handleException(exception, "updateInvoiceSettings");
    }
  }

  /**
   * Update challan settings for a tenant
   * 
   * @param request The challan settings update request
   * @return Updated challan settings
   */
  @PostMapping("/billing-settings/challan")
  @ApiOperation(value = "Update challan settings", 
                notes = "Updates challan configuration including series format, tax rates, and other settings")
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<?> updateChallanSettings(
      @ApiParam(value = "Challan settings update request", required = true) @Valid @RequestBody ChallanSettingsRequest request) {

    Long tenantIdLong = TenantContextHolder.getAuthenticatedTenantId();
    try {
      log.debug("Updating challan settings for tenant: {}", tenantIdLong);
      
      // Convert request to entity using assembler
      TenantChallanSettings settingsToUpdate = billingSettingsAssembler.assembleChallanSettings(request);
      
      // Update settings
      TenantChallanSettings updatedSettings = tenantSettingsService.updateChallanSettings(tenantIdLong, settingsToUpdate);
      
      // Return updated settings using assembler
      BillingSettingsRepresentation.ChallanSettings response = billingSettingsAssembler.disassembleChallanSettings(updatedSettings);
      
      log.info("Successfully updated challan settings for tenant: {}", tenantIdLong);
      return ResponseEntity.ok(response);
      
    } catch (Exception exception) {
      log.error("Error updating challan settings for tenant: {}", tenantIdLong, exception);
      return GenericExceptionHandler.handleException(exception, "updateChallanSettings");
    }
  }

}
