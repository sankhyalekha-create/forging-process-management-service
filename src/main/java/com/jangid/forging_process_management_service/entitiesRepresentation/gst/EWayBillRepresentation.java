package com.jangid.forging_process_management_service.entitiesRepresentation.gst;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EWayBillRepresentation {

    @JsonProperty("id")
    private Long id;

    @Size(max = 20, message = "E-Way Bill number cannot exceed 20 characters")
    @JsonProperty("ewayBillNumber")
    private String ewayBillNumber;

    @JsonProperty("ewayBillDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime ewayBillDate;

    @JsonProperty("dispatchBatchId")
    private Long dispatchBatchId;

    @JsonProperty("invoiceId")
    private Long invoiceId;

    @JsonProperty("deliveryChallanId")
    private Long deliveryChallanId;

    // Document Details
    @NotBlank(message = "Document number is required")
    @Size(max = 50, message = "Document number cannot exceed 50 characters")
    @JsonProperty("documentNumber")
    private String documentNumber;

    @JsonProperty("documentDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime documentDate;

    @NotBlank(message = "Document type is required")
    @JsonProperty("documentType")
    private String documentType; // INVOICE or CHALLAN

    // Supplier Details
    @NotBlank(message = "Supplier GSTIN is required")
    @Size(max = 15, message = "GSTIN cannot exceed 15 characters")
    @Pattern(regexp = "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$", 
             message = "Invalid GSTIN format")
    @JsonProperty("supplierGstin")
    private String supplierGstin;

    @NotBlank(message = "Supplier name is required")
    @Size(max = 200, message = "Supplier name cannot exceed 200 characters")
    @JsonProperty("supplierName")
    private String supplierName;

    @NotBlank(message = "Supplier state code is required")
    @Size(max = 2, message = "State code must be 2 characters")
    @JsonProperty("supplierStateCode")
    private String supplierStateCode;

    @NotBlank(message = "Supplier pincode is required")
    @Size(max = 6, message = "Pincode must be 6 characters")
    @Pattern(regexp = "^[0-9]{6}$", message = "Invalid pincode format")
    @JsonProperty("supplierPincode")
    private String supplierPincode;

    // Recipient Details
    @Size(max = 15, message = "GSTIN cannot exceed 15 characters")
    @Pattern(regexp = "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$", 
             message = "Invalid GSTIN format")
    @JsonProperty("recipientGstin")
    private String recipientGstin;

    @NotBlank(message = "Recipient name is required")
    @Size(max = 200, message = "Recipient name cannot exceed 200 characters")
    @JsonProperty("recipientName")
    private String recipientName;

    @NotBlank(message = "Recipient state code is required")
    @Size(max = 2, message = "State code must be 2 characters")
    @JsonProperty("recipientStateCode")
    private String recipientStateCode;

    @NotBlank(message = "Recipient pincode is required")
    @Size(max = 6, message = "Pincode must be 6 characters")
    @Pattern(regexp = "^[0-9]{6}$", message = "Invalid pincode format")
    @JsonProperty("recipientPincode")
    private String recipientPincode;

    // Goods Details
    @Size(max = 10, message = "HSN code cannot exceed 10 characters")
    @JsonProperty("hsnCode")
    private String hsnCode;

    @JsonProperty("goodsDescription")
    private String goodsDescription;

    @DecimalMin(value = "0.0", message = "Total quantity must be non-negative")
    @JsonProperty("totalQuantity")
    @Builder.Default
    private BigDecimal totalQuantity = BigDecimal.ZERO;

    @NotNull(message = "Total value is required")
    @DecimalMin(value = "0.0", message = "Total value must be non-negative")
    @JsonProperty("totalValue")
    private BigDecimal totalValue;

    // Transportation Details
    @JsonProperty("transportationMode")
    @Builder.Default
    private String transportationMode = "ROAD";

    @JsonProperty("transportationDistance")
    private Integer transportationDistance;

    @Size(max = 20, message = "Vehicle number cannot exceed 20 characters")
    @JsonProperty("vehicleNumber")
    private String vehicleNumber;

    @Size(max = 200, message = "Transporter name cannot exceed 200 characters")
    @JsonProperty("transporterName")
    private String transporterName;

    // Validity
    @JsonProperty("validFrom")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime validFrom;

    @JsonProperty("validUntil")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime validUntil;

    @JsonProperty("status")
    private String status;

    // Tenant Information
    @JsonProperty("tenantId")
    private Long tenantId;

    // Audit Fields
    @JsonProperty("createdAt")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonProperty("updatedAt")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    // Computed Properties
    @JsonProperty("isValid")
    public boolean isValid() {
        return "ACTIVE".equals(status) && 
               validUntil != null && 
               LocalDateTime.now().isBefore(validUntil);
    }

    @JsonProperty("canBeCancelled")
    public boolean canBeCancelled() {
        return "ACTIVE".equals(status);
    }

    @JsonProperty("isExpired")
    public boolean isExpired() {
        return validUntil != null && LocalDateTime.now().isAfter(validUntil);
    }

    @JsonProperty("isExpiringSoon")
    public boolean isExpiringSoon() {
        if (validUntil == null || !"ACTIVE".equals(status)) {
            return false;
        }
        return LocalDateTime.now().plus(24, ChronoUnit.HOURS).isAfter(validUntil);
    }

    @JsonProperty("hoursUntilExpiry")
    public long getHoursUntilExpiry() {
        if (validUntil == null) {
            return 0;
        }
        long hours = ChronoUnit.HOURS.between(LocalDateTime.now(), validUntil);
        return Math.max(0, hours);
    }

    @JsonProperty("statusDisplayName")
    public String getStatusDisplayName() {
        if (status == null) return "";
        return switch (status) {
            case "ACTIVE" -> "Active";
            case "CANCELLED" -> "Cancelled";
            case "EXPIRED" -> "Expired";
            default -> status;
        };
    }

    @JsonProperty("transportationModeDisplayName")
    public String getTransportationModeDisplayName() {
        if (transportationMode == null) return "";
        return switch (transportationMode) {
            case "ROAD" -> "Road";
            case "RAIL" -> "Rail";
            case "AIR" -> "Air";
            case "SHIP" -> "Ship";
            case "OTHER" -> "Other";
            default -> transportationMode;
        };
    }

    @JsonProperty("isInterState")
    public boolean isInterState() {
        return supplierStateCode != null && recipientStateCode != null 
               && !supplierStateCode.equals(recipientStateCode);
    }

    @JsonProperty("validityDays")
    public int getValidityDays() {
        if (validFrom == null || validUntil == null) {
            return 0;
        }
        return (int) ChronoUnit.DAYS.between(validFrom, validUntil) + 1;
    }
}
