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
public class ChallanSettingsRequest {

  // Challan Number Configuration
  @Size(max = 10, message = "Prefix cannot exceed 10 characters")
  @Pattern(regexp = "^[A-Z0-9]*$", message = "Prefix must contain only uppercase letters and numbers")
  private String challanPrefix;

  @Min(value = 1, message = "Start from must be at least 1")
  private Integer startFrom;

  @Size(max = 20, message = "Series format cannot exceed 20 characters")
  private String seriesFormat;

  // Tax Configuration
  @Size(max = 10, message = "HSN/SAC code cannot exceed 10 characters")
  @Pattern(regexp = "^[0-9]{6,8}$", message = "HSN/SAC code must be 6-8 digits")
  private String hsnSacCode;

  @DecimalMin(value = "0.00", message = "CGST rate cannot be negative")
  @DecimalMax(value = "50.00", message = "CGST rate cannot exceed 50%")
  private BigDecimal cgstRate;

  @DecimalMin(value = "0.00", message = "SGST rate cannot be negative")
  @DecimalMax(value = "50.00", message = "SGST rate cannot exceed 50%")
  private BigDecimal sgstRate;

  @DecimalMin(value = "0.00", message = "IGST rate cannot be negative")
  @DecimalMax(value = "50.00", message = "IGST rate cannot exceed 50%")
  private BigDecimal igstRate;

  private Boolean activateTCS;

  // Bank Details
  private Boolean bankDetailsSameAsJobwork;

  @Size(max = 100, message = "Bank name cannot exceed 100 characters")
  private String bankName;

  @Size(max = 20, message = "Account number cannot exceed 20 characters")
  private String accountNumber;

  @Pattern(regexp = "^[A-Z]{4}0[A-Z0-9]{6}$", message = "Invalid IFSC code format")
  private String ifscCode;

  // Terms and Conditions
  private String termsAndConditions;
}
