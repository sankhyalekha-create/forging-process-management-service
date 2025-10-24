package com.jangid.forging_process_management_service.entitiesRepresentation.settings;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;

import java.math.BigDecimal;
 
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceSettingsRequest {

  // Job Work Invoice Settings
  @Size(max = 10, message = "Job work prefix cannot exceed 10 characters")
  @Pattern(regexp = "^[A-Z0-9]*$", message = "Prefix must contain only uppercase letters and numbers")
  private String jobWorkInvoicePrefix;

  @Size(max = 20, message = "Series format cannot exceed 20 characters")
  private String jobWorkSeriesFormat;

  @Min(value = 1, message = "Start from must be at least 1")
  private Integer jobWorkStartFrom;

  @Min(value = 1, message = "Current sequence must be at least 1")
  private Integer jobWorkCurrentSequence;

  // Job Work Tax Settings
  @Size(max = 10, message = "HSN/SAC code cannot exceed 10 characters")
  @Pattern(regexp = "^[0-9]{6,8}$", message = "HSN/SAC code must be 6-8 digits")
  private String jobWorkHsnSacCode;

  @DecimalMin(value = "0.00", message = "CGST rate cannot be negative")
  @DecimalMax(value = "50.00", message = "CGST rate cannot exceed 50%")
  private BigDecimal jobWorkCgstRate;

  @DecimalMin(value = "0.00", message = "SGST rate cannot be negative")
  @DecimalMax(value = "50.00", message = "SGST rate cannot exceed 50%")
  private BigDecimal jobWorkSgstRate;

  @DecimalMin(value = "0.00", message = "IGST rate cannot be negative")
  @DecimalMax(value = "50.00", message = "IGST rate cannot exceed 50%")
  private BigDecimal jobWorkIgstRate;

  // Material Invoice Settings
  @Size(max = 10, message = "Material prefix cannot exceed 10 characters")
  @Pattern(regexp = "^[A-Z0-9]*$", message = "Prefix must contain only uppercase letters and numbers")
  private String materialInvoicePrefix;

  @Size(max = 20, message = "Series format cannot exceed 20 characters")
  private String materialSeriesFormat;

  @Min(value = 1, message = "Start from must be at least 1")
  private Integer materialStartFrom;

  @Min(value = 1, message = "Current sequence must be at least 1")
  private Integer materialCurrentSequence;

  // Material Tax Settings
  @Size(max = 10, message = "HSN/SAC code cannot exceed 10 characters")
  @Pattern(regexp = "^[0-9]{6,8}$", message = "HSN/SAC code must be 6-8 digits")
  private String materialHsnSacCode;

  @DecimalMin(value = "0.00", message = "CGST rate cannot be negative")
  @DecimalMax(value = "50.00", message = "CGST rate cannot exceed 50%")
  private BigDecimal materialCgstRate;

  @DecimalMin(value = "0.00", message = "SGST rate cannot be negative")
  @DecimalMax(value = "50.00", message = "SGST rate cannot exceed 50%")
  private BigDecimal materialSgstRate;

  @DecimalMin(value = "0.00", message = "IGST rate cannot be negative")
  @DecimalMax(value = "50.00", message = "IGST rate cannot exceed 50%")
  private BigDecimal materialIgstRate;

  // Manual Invoice Settings
  private Boolean manualInvoiceEnabled;

  @Size(max = 100, message = "Manual invoice title cannot exceed 100 characters")
  private String manualInvoiceTitle;

  @Min(value = 1, message = "Start from must be at least 1")
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

  @Size(max = 100, message = "Bank name cannot exceed 100 characters")
  private String bankName;

  @Size(max = 20, message = "Account number cannot exceed 20 characters")
  private String accountNumber;

  @Pattern(regexp = "^[A-Z]{4}0[A-Z0-9]{6}$", message = "Invalid IFSC code format")
  private String ifscCode;

  // Terms and Conditions - Separate for each work type
  private String jobWorkTermsAndConditions;
  private String materialTermsAndConditions;
}
