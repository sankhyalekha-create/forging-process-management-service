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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryChallanRepresentation {

    @JsonProperty("id")
    private Long id;

    @NotBlank(message = "Challan number is required")
    @Size(max = 50, message = "Challan number cannot exceed 50 characters")
    @JsonProperty("challanNumber")
    private String challanNumber;

    @JsonProperty("challanDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime challanDate;

    @NotBlank(message = "Challan type is required")
    @JsonProperty("challanType")
    private String challanType; // Will be converted to ChallanType enum

    @JsonProperty("dispatchBatchId")
    private Long dispatchBatchId;

    @JsonProperty("convertedToInvoiceId")
    private Long convertedToInvoiceId;

    // Transportation Details
    @NotBlank(message = "Transportation reason is required")
    @JsonProperty("transportationReason")
    private String transportationReason;

    @JsonProperty("transportationMode")
    @Builder.Default
    private String transportationMode = "ROAD";

    @JsonProperty("expectedDeliveryDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate expectedDeliveryDate;

    @JsonProperty("actualDeliveryDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate actualDeliveryDate;

    // Consignee Details - IDs for relationships
    @JsonProperty("consigneeBuyerEntityId")
    private Long consigneeBuyerEntityId;

    @JsonProperty("consigneeVendorEntityId")
    private Long consigneeVendorEntityId;

    // Consignor Details - Computed from tenant and GST configuration
    @JsonProperty("consignorGstin")
    private String consignorGstin;

    @JsonProperty("consignorName")
    private String consignorName;

    @JsonProperty("consignorAddress")
    private String consignorAddress;

    @JsonProperty("consignorStateCode")
    private String consignorStateCode;

    // Consignee Details - Computed from buyer/vendor entities
    @JsonProperty("consigneeGstin")
    private String consigneeGstin;

    @JsonProperty("consigneeName")
    private String consigneeName;

    @JsonProperty("consigneeAddress")
    private String consigneeAddress;

    @JsonProperty("consigneeStateCode")
    private String consigneeStateCode;

    @JsonProperty("consigneeType")
    private String consigneeType; // "BUYER" or "VENDOR"

    // Financial Summary
    @DecimalMin(value = "0.0", message = "Total quantity must be non-negative")
    @JsonProperty("totalQuantity")
    @Builder.Default
    private BigDecimal totalQuantity = BigDecimal.ZERO;

    @DecimalMin(value = "0.0", message = "Total value must be non-negative")
    @JsonProperty("totalValue")
    @Builder.Default
    private BigDecimal totalValue = BigDecimal.ZERO;

    // Status
    @JsonProperty("status")
    private String status;

    // Document Reference
    @JsonProperty("documentPath")
    private String documentPath;

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
    @JsonProperty("isInterState")
    public boolean isInterState() {
        return consignorStateCode != null && consigneeStateCode != null 
               && !consignorStateCode.equals(consigneeStateCode);
    }

    @JsonProperty("canBeConvertedToInvoice")
    public boolean canBeConvertedToInvoice() {
        return "DELIVERED".equals(status) && convertedToInvoiceId == null;
    }

    @JsonProperty("statusDisplayName")
    public String getStatusDisplayName() {
        if (status == null) return "";
        return switch (status) {
            case "DRAFT" -> "Draft";
            case "GENERATED" -> "Generated";
            case "DISPATCHED" -> "Dispatched";
            case "DELIVERED" -> "Delivered";
            case "CONVERTED_TO_INVOICE" -> "Converted to Invoice";
            case "CANCELLED" -> "Cancelled";
            default -> status;
        };
    }

    @JsonProperty("challanTypeDisplayName")
    public String getChallanTypeDisplayName() {
        if (challanType == null) return "";
        return switch (challanType) {
            case "JOB_WORK" -> "Job Work";
            case "BRANCH_TRANSFER" -> "Branch Transfer";
            case "SAMPLE_DISPATCH" -> "Sample Dispatch";
            case "RETURN_GOODS" -> "Return Goods";
            case "OTHER" -> "Other";
            default -> challanType;
        };
    }
}
