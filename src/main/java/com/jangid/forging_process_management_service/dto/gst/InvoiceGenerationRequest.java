package com.jangid.forging_process_management_service.dto.gst;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InvoiceGenerationRequest {

  @NotEmpty(message = "At least one dispatch batch ID is required")
  private List<Long> dispatchBatchIds;

  @NotNull(message = "Invoice type is required")
  private String invoiceType; // "JOB_WORK" or "MATERIAL"

  @NotNull(message = "Unit rate is required")
  @DecimalMin(value = "0.01", message = "Unit rate must be greater than 0")
  private BigDecimal unitRate;

  @NotNull(message = "CGST rate is required")
  @DecimalMin(value = "0.0", message = "CGST rate cannot be negative")
  private BigDecimal cgstRate;

  @NotNull(message = "SGST rate is required")
  @DecimalMin(value = "0.0", message = "SGST rate cannot be negative")
  private BigDecimal sgstRate;

  @NotNull(message = "IGST rate is required")
  @DecimalMin(value = "0.0", message = "IGST rate cannot be negative")
  private BigDecimal igstRate;

  @NotNull(message = "HSN/SAC code is required")
  @Size(min = 6, max = 8, message = "HSN/SAC code must be 6-8 digits")
  private String hsnSacCode;

  private String paymentTerms;

  private String termsAndConditions;
}
