package com.jangid.forging_process_management_service.assemblers.settings;

import com.jangid.forging_process_management_service.entities.Tenant;
import com.jangid.forging_process_management_service.entities.settings.TenantInvoiceSettings;
import com.jangid.forging_process_management_service.entities.settings.TenantChallanSettings;
import com.jangid.forging_process_management_service.entities.settings.TenantVendorChallanSettings;
import com.jangid.forging_process_management_service.entitiesRepresentation.settings.BillingSettingsRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.settings.InvoiceSettingsRequest;
import com.jangid.forging_process_management_service.entitiesRepresentation.settings.ChallanSettingsRequest;
import com.jangid.forging_process_management_service.entitiesRepresentation.settings.VendorChallanSettingsRequest;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Assembler for converting between billing settings entities and their representations.
 *
 * This assembler handles the conversion between:
 * - TenantInvoiceSettings <-> InvoiceSettingsRequest
 * - TenantChallanSettings <-> ChallanSettingsRequest
 * - Combined entities <-> BillingSettingsRepresentation
 *
 * Follows the established assembler pattern in the codebase for consistent
 * entity-representation conversions.
 */
@Slf4j
@Component
public class BillingSettingsAssembler {

  // ======================================================================
  // Invoice Settings Conversions
  // ======================================================================

  /**
   * Convert InvoiceSettingsRequest to TenantInvoiceSettings entity
   * Used for creating new invoice settings
   */
  public TenantInvoiceSettings createInvoiceSettingsAssemble(InvoiceSettingsRequest request, Tenant tenant) {
    TenantInvoiceSettings settings = assembleInvoiceSettings(request);
    settings.setTenant(tenant);
    settings.setCreatedAt(LocalDateTime.now());
    settings.setUpdatedAt(LocalDateTime.now());
    return settings;
  }

  /**
   * Convert InvoiceSettingsRequest to TenantInvoiceSettings entity
   * Used for updates (without setting tenant and timestamps)
   */
  public TenantInvoiceSettings assembleInvoiceSettings(InvoiceSettingsRequest request) {
    if (request == null) {
      return null;
    }

    return TenantInvoiceSettings.builder()
        // Job Work Invoice Settings
        .jobWorkInvoicePrefix(request.getJobWorkInvoicePrefix())
        .jobWorkSeriesFormat(request.getJobWorkSeriesFormat())
        .jobWorkStartFrom(request.getJobWorkStartFrom())
        .jobWorkCurrentSequence(request.getJobWorkCurrentSequence())

        // Job Work Tax Settings
        .jobWorkHsnSacCode(request.getJobWorkHsnSacCode())
        .jobWorkCgstRate(request.getJobWorkCgstRate())
        .jobWorkSgstRate(request.getJobWorkSgstRate())
        .jobWorkIgstRate(request.getJobWorkIgstRate())

        // Material Invoice Settings
        .materialInvoicePrefix(request.getMaterialInvoicePrefix())
        .materialSeriesFormat(request.getMaterialSeriesFormat())
        .materialStartFrom(request.getMaterialStartFrom())
        .materialCurrentSequence(request.getMaterialCurrentSequence())

        // Material Tax Settings
        .materialHsnSacCode(request.getMaterialHsnSacCode())
        .materialCgstRate(request.getMaterialCgstRate())
        .materialSgstRate(request.getMaterialSgstRate())
        .materialIgstRate(request.getMaterialIgstRate())

        // Manual Invoice Settings
        .manualInvoiceEnabled(request.getManualInvoiceEnabled())
        .manualInvoiceTitle(request.getManualInvoiceTitle())
        .manualStartFrom(request.getManualStartFrom())

        // Integration Settings
        .showVehicleNumber(request.getShowVehicleNumber())

        // E-Way Bill & E-Invoice Settings
        .activateEWayBill(request.getActivateEWayBill())
        .activateEInvoice(request.getActivateEInvoice())
        .activateTCS(request.getActivateTCS())

        // Transport & Bank Details
        .transporterDetails(request.getTransporterDetails())
        .bankDetailsSameAsJobwork(request.getBankDetailsSameAsJobwork())
        .bankName(request.getBankName())
        .accountNumber(request.getAccountNumber())
        .ifscCode(request.getIfscCode())

        // Terms and Conditions - Separate for each work type
        .jobWorkTermsAndConditions(request.getJobWorkTermsAndConditions())
        .materialTermsAndConditions(request.getMaterialTermsAndConditions())
        .build();
  }

  /**
   * Convert TenantInvoiceSettings entity to InvoiceSettings representation
   */
  public BillingSettingsRepresentation.InvoiceSettings disassembleInvoiceSettings(TenantInvoiceSettings entity) {
    if (entity == null) {
      return null;
    }

    return BillingSettingsRepresentation.InvoiceSettings.builder()
        // Job Work Invoice Settings
        .jobWorkInvoicePrefix(entity.getJobWorkInvoicePrefix())
        .jobWorkSeriesFormat(entity.getJobWorkSeriesFormat())
        .jobWorkStartFrom(entity.getJobWorkStartFrom())
        .jobWorkCurrentSequence(entity.getJobWorkCurrentSequence())

        // Job Work Tax Settings
        .jobWorkHsnSacCode(entity.getJobWorkHsnSacCode())
        .jobWorkCgstRate(entity.getJobWorkCgstRate())
        .jobWorkSgstRate(entity.getJobWorkSgstRate())
        .jobWorkIgstRate(entity.getJobWorkIgstRate())

        // Material Invoice Settings
        .materialInvoicePrefix(entity.getMaterialInvoicePrefix())
        .materialSeriesFormat(entity.getMaterialSeriesFormat())
        .materialStartFrom(entity.getMaterialStartFrom())
        .materialCurrentSequence(entity.getMaterialCurrentSequence())

        // Material Tax Settings
        .materialHsnSacCode(entity.getMaterialHsnSacCode())
        .materialCgstRate(entity.getMaterialCgstRate())
        .materialSgstRate(entity.getMaterialSgstRate())
        .materialIgstRate(entity.getMaterialIgstRate())

        // Manual Invoice Settings
        .manualInvoiceEnabled(entity.getManualInvoiceEnabled())
        .manualInvoiceTitle(entity.getManualInvoiceTitle())
        .manualStartFrom(entity.getManualStartFrom())

        // Integration Settings
        .showVehicleNumber(entity.getShowVehicleNumber())

        // E-Way Bill & E-Invoice Settings
        .activateEWayBill(entity.getActivateEWayBill())
        .activateEInvoice(entity.getActivateEInvoice())
        .activateTCS(entity.getActivateTCS())

        // Transport & Bank Details
        .transporterDetails(entity.getTransporterDetails())
        .bankDetailsSameAsJobwork(entity.getBankDetailsSameAsJobwork())
        .bankName(entity.getBankName())
        .accountNumber(entity.getAccountNumber())
        .ifscCode(entity.getIfscCode())

        // Terms and Conditions - Separate for each work type
        .jobWorkTermsAndConditions(entity.getJobWorkTermsAndConditions())
        .materialTermsAndConditions(entity.getMaterialTermsAndConditions())
        .build();
  }

  // ======================================================================
  // Challan Settings Conversions
  // ======================================================================

  /**
   * Convert ChallanSettingsRequest to TenantChallanSettings entity
   * Used for creating new challan settings
   */
  public TenantChallanSettings createChallanSettingsAssemble(ChallanSettingsRequest request, Tenant tenant) {
    TenantChallanSettings settings = assembleChallanSettings(request);
    settings.setTenant(tenant);
    settings.setCreatedAt(LocalDateTime.now());
    settings.setUpdatedAt(LocalDateTime.now());
    return settings;
  }

  /**
   * Convert ChallanSettingsRequest to TenantChallanSettings entity
   * Used for updates (without setting tenant and timestamps)
   */
  public TenantChallanSettings assembleChallanSettings(ChallanSettingsRequest request) {
    if (request == null) {
      return null;
    }

    return TenantChallanSettings.builder()
        // Challan Number Configuration
        .challanPrefix(request.getChallanPrefix())
        .startFrom(request.getStartFrom())
        .seriesFormat(request.getSeriesFormat())
        .currentSequence(request.getCurrentSequence())
        .build();
  }

  /**
   * Convert TenantChallanSettings entity to ChallanSettings representation
   */
  public BillingSettingsRepresentation.ChallanSettings disassembleChallanSettings(TenantChallanSettings entity) {
    if (entity == null) {
      return null;
    }

    return BillingSettingsRepresentation.ChallanSettings.builder()
        // Challan Number Configuration
        .challanPrefix(entity.getChallanPrefix())
        .startFrom(entity.getStartFrom())
        .currentSequence(entity.getCurrentSequence())
        .seriesFormat(entity.getSeriesFormat())
        .build();
  }

  // ======================================================================
  // Vendor Challan Settings Conversions
  // ======================================================================

  /**
   * Convert VendorChallanSettingsRequest to TenantVendorChallanSettings entity
   * Used for creating new vendor challan settings
   */
  public TenantVendorChallanSettings createVendorChallanSettingsAssemble(VendorChallanSettingsRequest request, com.jangid.forging_process_management_service.entities.Tenant tenant) {
    TenantVendorChallanSettings settings = assembleVendorChallanSettings(request);
    settings.setTenant(tenant);
    settings.setCreatedAt(LocalDateTime.now());
    settings.setUpdatedAt(LocalDateTime.now());
    return settings;
  }

  /**
   * Convert VendorChallanSettingsRequest to TenantVendorChallanSettings entity
   * Used for updates (without setting tenant and timestamps)
   */
  public TenantVendorChallanSettings assembleVendorChallanSettings(VendorChallanSettingsRequest request) {
    if (request == null) {
      return null;
    }

    return TenantVendorChallanSettings.builder()
        // Vendor Challan Number Configuration
        .challanPrefix(request.getChallanPrefix())
        .startFrom(request.getStartFrom())
        .seriesFormat(request.getSeriesFormat())
        .currentSequence(request.getCurrentSequence())
        .build();
  }

  /**
   * Convert TenantVendorChallanSettings entity to VendorChallanSettings representation
   */
  public BillingSettingsRepresentation.VendorChallanSettings disassembleVendorChallanSettings(com.jangid.forging_process_management_service.entities.settings.TenantVendorChallanSettings entity) {
    if (entity == null) {
      return null;
    }

    return BillingSettingsRepresentation.VendorChallanSettings.builder()
        // Vendor Challan Number Configuration
        .challanPrefix(entity.getChallanPrefix())
        .startFrom(entity.getStartFrom())
        .currentSequence(entity.getCurrentSequence())
        .seriesFormat(entity.getSeriesFormat())
        .build();
  }

  // ======================================================================
  // Combined Billing Settings Conversions
  // ======================================================================

  /**
   * Convert combined entities to complete BillingSettingsRepresentation
   */
  public BillingSettingsRepresentation disassembleBillingSettings(
      Tenant tenant,
      TenantInvoiceSettings invoiceSettings,
      TenantChallanSettings challanSettings,
      TenantVendorChallanSettings vendorChallanSettings) {

    if (tenant == null) {
      log.warn("Tenant is null when disassembling billing settings");
      return null;
    }

    return BillingSettingsRepresentation.builder()
        .tenantId(tenant.getId())
        .tenantName(tenant.getTenantName())
        .invoiceSettings(disassembleInvoiceSettings(invoiceSettings))
        .challanSettings(disassembleChallanSettings(challanSettings))
        .vendorChallanSettings(disassembleVendorChallanSettings(vendorChallanSettings))
        .lastUpdated(getLatestUpdateTime(invoiceSettings, challanSettings, vendorChallanSettings))
        .build();
  }

  /**
   * Convert combined entities to complete BillingSettingsRepresentation (overloaded method for backward compatibility)
   */
  public BillingSettingsRepresentation disassembleBillingSettings(
      Tenant tenant,
      TenantInvoiceSettings invoiceSettings,
      TenantChallanSettings challanSettings) {

    return disassembleBillingSettings(tenant, invoiceSettings, challanSettings, null);
  }

  // ======================================================================
  // Helper Methods
  // ======================================================================

  /**
   * Get the latest update time from all settings entities
   */
  private LocalDateTime getLatestUpdateTime(TenantInvoiceSettings invoiceSettings, TenantChallanSettings challanSettings,
      TenantVendorChallanSettings vendorChallanSettings) {
    LocalDateTime invoiceTime = invoiceSettings != null ? invoiceSettings.getUpdatedAt() : null;
    LocalDateTime challanTime = challanSettings != null ? challanSettings.getUpdatedAt() : null;
    LocalDateTime vendorChallanTime = vendorChallanSettings != null ? vendorChallanSettings.getUpdatedAt() : null;

    LocalDateTime latest = invoiceTime;
    if (challanTime != null && (latest == null || challanTime.isAfter(latest))) {
      latest = challanTime;
    }
    if (vendorChallanTime != null && (latest == null || vendorChallanTime.isAfter(latest))) {
      latest = vendorChallanTime;
    }
    
    return latest != null ? latest : LocalDateTime.now();
  }
}
