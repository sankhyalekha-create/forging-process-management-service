package com.jangid.forging_process_management_service.entitiesRepresentation.gst;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GSTConfigurationRepresentation {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("tenantId")
    private Long tenantId;

    // Company GST Details
    @NotBlank(message = "Company GSTIN is required")
    @Size(max = 15, message = "GSTIN cannot exceed 15 characters")
    @Pattern(regexp = "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$", 
             message = "Invalid GSTIN format")
    @JsonProperty("companyGstin")
    private String companyGstin;

    @NotBlank(message = "Company legal name is required")
    @Size(max = 200, message = "Company legal name cannot exceed 200 characters")
    @JsonProperty("companyLegalName")
    private String companyLegalName;

    @Size(max = 200, message = "Company trade name cannot exceed 200 characters")
    @JsonProperty("companyTradeName")
    private String companyTradeName;

    @NotBlank(message = "Company address is required")
    @JsonProperty("companyAddress")
    private String companyAddress;

    @NotBlank(message = "Company state code is required")
    @Size(max = 2, message = "State code must be 2 characters")
    @JsonProperty("companyStateCode")
    private String companyStateCode;

    @NotBlank(message = "Company pincode is required")
    @Size(max = 6, message = "Pincode must be 6 characters")
    @Pattern(regexp = "^[0-9]{6}$", message = "Invalid pincode format")
    @JsonProperty("companyPincode")
    private String companyPincode;

    // Number Generation Configuration
    @Size(max = 10, message = "Invoice number prefix cannot exceed 10 characters")
    @JsonProperty("invoiceNumberPrefix")
    private String invoiceNumberPrefix;

    @JsonProperty("currentInvoiceSequence")
    @Builder.Default
    private Integer currentInvoiceSequence = 1;

    @Size(max = 10, message = "Challan number prefix cannot exceed 10 characters")
    @JsonProperty("challanNumberPrefix")
    private String challanNumberPrefix;

    @JsonProperty("currentChallanSequence")
    @Builder.Default
    private Integer currentChallanSequence = 1;

    // E-Way Bill Configuration
    @DecimalMin(value = "0.0", message = "E-Way Bill threshold must be non-negative")
    @JsonProperty("ewayBillThreshold")
    @Builder.Default
    private BigDecimal ewayBillThreshold = new BigDecimal("50000.00");

    @JsonProperty("autoGenerateEwayBill")
    @Builder.Default
    private Boolean autoGenerateEwayBill = true;

    // Default Tax Rates
    @DecimalMin(value = "0.0", message = "CGST rate must be non-negative")
    @DecimalMax(value = "100.0", message = "CGST rate cannot exceed 100%")
    @JsonProperty("defaultCgstRate")
    @Builder.Default
    private BigDecimal defaultCgstRate = new BigDecimal("9.00");

    @DecimalMin(value = "0.0", message = "SGST rate must be non-negative")
    @DecimalMax(value = "100.0", message = "SGST rate cannot exceed 100%")
    @JsonProperty("defaultSgstRate")
    @Builder.Default
    private BigDecimal defaultSgstRate = new BigDecimal("9.00");

    @DecimalMin(value = "0.0", message = "IGST rate must be non-negative")
    @DecimalMax(value = "100.0", message = "IGST rate cannot exceed 100%")
    @JsonProperty("defaultIgstRate")
    @Builder.Default
    private BigDecimal defaultIgstRate = new BigDecimal("18.00");

    // Status
    @JsonProperty("isActive")
    @Builder.Default
    private Boolean isActive = true;

    // Audit Fields
    @JsonProperty("createdAt")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonProperty("updatedAt")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    // Computed Properties
    @JsonProperty("nextInvoiceNumber")
    public String getNextInvoiceNumber() {
        String prefix = invoiceNumberPrefix != null ? invoiceNumberPrefix : "INV";
        return String.format("%s-%04d", prefix, currentInvoiceSequence);
    }

    @JsonProperty("nextChallanNumber")
    public String getNextChallanNumber() {
        String prefix = challanNumberPrefix != null ? challanNumberPrefix : "CHN";
        return String.format("%s-%04d", prefix, currentChallanSequence);
    }

    @JsonProperty("totalDefaultTaxRate")
    public BigDecimal getTotalDefaultTaxRate() {
        return defaultCgstRate.add(defaultSgstRate);
    }

    @JsonProperty("isGstinValid")
    public boolean isGstinValid() {
        if (companyGstin == null) return false;
        return companyGstin.matches("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$");
    }

    @JsonProperty("stateCodeFromGstin")
    public String getStateCodeFromGstin() {
        if (companyGstin != null && companyGstin.length() >= 2) {
            return companyGstin.substring(0, 2);
        }
        return null;
    }

    @JsonProperty("requiresEwayBill")
    public boolean requiresEwayBill(BigDecimal amount) {
        return amount != null && amount.compareTo(ewayBillThreshold) >= 0;
    }

    // Display Names
    @JsonProperty("statusDisplayName")
    public String getStatusDisplayName() {
        return Boolean.TRUE.equals(isActive) ? "Active" : "Inactive";
    }

    @JsonProperty("configurationSummary")
    public String getConfigurationSummary() {
        return String.format("GSTIN: %s | Invoice: %s | Challan: %s | E-Way: ₹%s", 
            companyGstin, 
            getNextInvoiceNumber(), 
            getNextChallanNumber(),
            ewayBillThreshold);
    }
}
