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
import java.util.List;

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

    @JsonProperty("challanDateTime")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime challanDateTime;

    @NotBlank(message = "Challan type is required")
    @JsonProperty("challanType")
    private String challanType; // Will be converted to ChallanType enum

    @JsonProperty("otherChallanTypeDetails")
    private String otherChallanTypeDetails; // Details when challan type is OTHER

    @JsonProperty("workType")
    private String workType; // JOB_WORK_ONLY or WITH_MATERIAL

    /**
     * List of dispatch batch IDs associated with this challan.
     * A challan can be generated from multiple dispatch batches.
     */
    @JsonProperty("dispatchBatchIds")
    private List<Long> dispatchBatchIds;

    /**
     * List of dispatch batch numbers associated with this challan.
     */
    @JsonProperty("dispatchBatchNumbers")
    private List<String> dispatchBatchNumbers;

    /**
     * Packaging details for each dispatch batch in this challan.
     * Includes dispatch batch number, packaging type, and individual package details.
     */
    @JsonProperty("packagingDetails")
    private List<DispatchBatchPackagingDetail> packagingDetails;

    // Order Reference (for traceability and reporting)
    @JsonProperty("orderId")
    private Long orderId;

    @JsonProperty("orderPoNumber")
    private String orderPoNumber;

    @JsonProperty("orderDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate orderDate;

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

    @JsonProperty("consignorPhoneNumber")
    private String consignorPhoneNumber;

    @JsonProperty("consignorEmail")
    private String consignorEmail;

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

    // Transportation Details Extended
    @JsonProperty("transporterName")
    private String transporterName;

    @JsonProperty("transporterId")
    private String transporterId;

    @JsonProperty("vehicleNumber")
    private String vehicleNumber;

    @JsonProperty("transportationDistance")
    private Integer transportationDistance;

    // Amount in Words
    @Size(max = 500, message = "Amount in words cannot exceed 500 characters")
    @JsonProperty("amountInWords")
    private String amountInWords;

    // Terms and Conditions
    @Size(max = 2000, message = "Terms and conditions cannot exceed 2000 characters")
    @JsonProperty("termsAndConditions")
    private String termsAndConditions;

    // Bank Details
    @Size(max = 100, message = "Bank name cannot exceed 100 characters")
    @JsonProperty("bankName")
    private String bankName;

    @Size(max = 20, message = "Account number cannot exceed 20 characters")
    @JsonProperty("accountNumber")
    private String accountNumber;

    @Size(max = 11, message = "IFSC code cannot exceed 11 characters")
    @JsonProperty("ifscCode")
    private String ifscCode;

    // Financial Summary
    @DecimalMin(value = "0.0", message = "Total quantity must be non-negative")
    @JsonProperty("totalQuantity")
    @Builder.Default
    private BigDecimal totalQuantity = BigDecimal.ZERO;

    @DecimalMin(value = "0.0", message = "Total taxable value must be non-negative")
    @JsonProperty("totalTaxableValue")
    @Builder.Default
    private BigDecimal totalTaxableValue = BigDecimal.ZERO;

    @JsonProperty("totalCgstAmount")
    @Builder.Default
    private BigDecimal totalCgstAmount = BigDecimal.ZERO;

    @JsonProperty("totalSgstAmount")
    @Builder.Default
    private BigDecimal totalSgstAmount = BigDecimal.ZERO;

    @JsonProperty("totalIgstAmount")
    @Builder.Default
    private BigDecimal totalIgstAmount = BigDecimal.ZERO;

    @DecimalMin(value = "0.0", message = "Total value must be non-negative")
    @JsonProperty("totalValue")
    @Builder.Default
    private BigDecimal totalValue = BigDecimal.ZERO;

    // Line Items
    @JsonProperty("lineItems")
    private List<ChallanLineItemRepresentation> lineItems;

    // Status
    @JsonProperty("status")
    private String status;

    // Document Reference
    @JsonProperty("documentPath")
    private String documentPath;

    // Tenant Information
    @JsonProperty("tenantId")
    private Long tenantId;

    // Status Tracking
    @JsonProperty("dispatchedAt")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dispatchedAt;

    @JsonProperty("dispatchedBy")
    private String dispatchedBy;

    @JsonProperty("deliveredAt")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime deliveredAt;

    @JsonProperty("deliveredBy")
    private String deliveredBy;

    @JsonProperty("cancelledAt")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime cancelledAt;

    @JsonProperty("cancelledBy")
    private String cancelledBy;

    @JsonProperty("cancellationReason")
    private String cancellationReason;

    // Audit Fields
    @JsonProperty("createdAt")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonProperty("createdBy")
    private String createdBy;

    @JsonProperty("updatedAt")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    @JsonProperty("updatedBy")
    private String updatedBy;

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

    @JsonProperty("canBeCancelled")
    public boolean canBeCancelled() {
        return "DRAFT".equals(status) || "GENERATED".equals(status);
    }

    @JsonProperty("isTaxApplicable")
    public boolean isTaxApplicable() {
        return "BRANCH_TRANSFER".equals(challanType);
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

    /**
     * Nested class to represent packaging details for a dispatch batch
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DispatchBatchPackagingDetail {
        @JsonProperty("dispatchBatchNumber")
        private String dispatchBatchNumber;

        @JsonProperty("packagingType")
        private String packagingType; // PALLET, BOX, BUNDLE, etc.

        @JsonProperty("packages")
        private List<PackageDetail> packages;
    }

    /**
     * Nested class to represent individual package details
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PackageDetail {
        @JsonProperty("packageNumber")
        private Integer packageNumber;

        @JsonProperty("quantityInPackage")
        private Integer quantityInPackage;
    }
}
