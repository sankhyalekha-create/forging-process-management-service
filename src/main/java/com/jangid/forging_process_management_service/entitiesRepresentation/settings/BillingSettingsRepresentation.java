package com.jangid.forging_process_management_service.entitiesRepresentation.settings;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillingSettingsRepresentation {

  private Long tenantId;
  private String tenantName;
  
  // Invoice Settings
  private InvoiceSettings invoiceSettings;
  
  // Challan Settings
  private ChallanSettings challanSettings;
  
  private LocalDateTime lastUpdated;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class InvoiceSettings {
    
    // Job Work Invoice Settings
    private String jobWorkInvoicePrefix;
    private String jobWorkSeriesFormat;
    private Integer jobWorkStartFrom;
    private Integer jobWorkCurrentSequence;
    
    // Job Work Tax Settings
    private String jobWorkHsnSacCode;
    private BigDecimal jobWorkCgstRate;
    private BigDecimal jobWorkSgstRate;
    private BigDecimal jobWorkIgstRate;
    
    // Material Invoice Settings
    private String materialInvoicePrefix;
    private String materialSeriesFormat;
    private Integer materialStartFrom;
    private Integer materialCurrentSequence;
    
    // Material Tax Settings
    private String materialHsnSacCode;
    private BigDecimal materialCgstRate;
    private BigDecimal materialSgstRate;
    private BigDecimal materialIgstRate;
    
    // Manual Invoice Settings
    private Boolean manualInvoiceEnabled;
    private String manualInvoiceTitle;
    private Integer manualStartFrom;
    
    // Integration Settings (reduced)
    private Boolean showVehicleNumber;
    
    // E-Way Bill & E-Invoice Settings
    private Boolean activateEWayBill;
    private Boolean activateEInvoice;
    private Boolean activateTCS;
    
    // Transport & Bank Details
    private Boolean transporterDetails;
    private Boolean bankDetailsSameAsJobwork;
    private String bankName;
    private String accountNumber;
    private String ifscCode;
    
    // Terms and Conditions - Separate for each work type
    private String jobWorkTermsAndConditions;
    private String materialTermsAndConditions;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ChallanSettings {
    
    // Challan Number Configuration
    private Integer startFrom;
    private Integer currentSequence;
    private String seriesFormat;
    
    // Tax Configuration
    private String hsnSacCode;
    private BigDecimal cgstRate;
    private BigDecimal sgstRate;
    private BigDecimal igstRate;
    private Boolean activateTCS;
    
    // Bank Details
    private Boolean bankDetailsSameAsJobwork;
    private String bankName;
    private String accountNumber;
    private String ifscCode;
    
    // Terms and Conditions
    private String termsAndConditions;
  }
}
