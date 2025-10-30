package com.jangid.forging_process_management_service.entitiesRepresentation.gst;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jangid.forging_process_management_service.entitiesRepresentation.tenant.TenantRepresentation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
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
public class InvoiceRepresentation {

    @JsonProperty("id")
    private Long id;

    @NotBlank(message = "Invoice number is required")
    @Size(max = 50, message = "Invoice number cannot exceed 50 characters")
    @JsonProperty("invoiceNumber")
    private String invoiceNumber;

    @JsonProperty("invoiceDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime invoiceDate;

    @JsonProperty("invoiceType")
    @Builder.Default
    private String invoiceType = "TAX_INVOICE";

    /**
     * List of dispatch batch IDs associated with this invoice.
     * An invoice can be generated from multiple dispatch batches.
     */
    @JsonProperty("dispatchBatchIds")
    private List<Long> dispatchBatchIds;

    /**
     * List of dispatch batch numbers associated with this invoice.
     */
    @JsonProperty("dispatchBatchNumbers")
    private List<String> dispatchBatchNumbers;

    /**
     * Packaging details for each dispatch batch in this invoice.
     * Includes dispatch batch number, packaging type, and individual package details.
     */
    @JsonProperty("packagingDetails")
    private List<DispatchBatchPackagingDetail> packagingDetails;

    @JsonProperty("deliveryChallanId")
    private Long deliveryChallanId;

    @JsonProperty("originalInvoiceId")
    private Long originalInvoiceId;

    // Order Reference (for traceability and reporting)
    @JsonProperty("orderId")
    private Long orderId;

    @JsonProperty("orderPoNumber")
    private String orderPoNumber;

    @JsonProperty("orderDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate orderDate;

    @JsonProperty("customerPoNumber")
    private String customerPoNumber;

    @JsonProperty("customerPoDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate customerPoDate;

    // Recipient Details - IDs for relationships
    @JsonProperty("recipientBuyerEntityId")
    private Long recipientBuyerEntityId;

    @JsonProperty("recipientVendorEntityId")
    private Long recipientVendorEntityId;

    // Supplier Details - Computed from tenant and GST configuration
    @JsonProperty("supplierGstin")
    private String supplierGstin;

    @JsonProperty("supplierName")
    private String supplierName;

    @JsonProperty("supplierAddress")
    private String supplierAddress;

    @JsonProperty("supplierStateCode")
    private String supplierStateCode;

    // Recipient Details - Computed from buyer/vendor entities
    @JsonProperty("recipientGstin")
    private String recipientGstin;

    @JsonProperty("recipientName")
    private String recipientName;

    @JsonProperty("recipientAddress")
    private String recipientAddress;

    @JsonProperty("recipientStateCode")
    private String recipientStateCode;

    @JsonProperty("recipientType")
    private String recipientType; // "BUYER" or "VENDOR"

    // Tax Jurisdiction
    @NotBlank(message = "Place of supply is required")
    @Size(max = 50, message = "Place of supply cannot exceed 50 characters")
    @JsonProperty("placeOfSupply")
    private String placeOfSupply;

    @JsonProperty("isInterState")
    @Builder.Default
    private Boolean isInterState = false;

    // Invoice Line Items
    @JsonProperty("lineItems")
    private List<InvoiceLineItemRepresentation> lineItems;

    // Transportation Details (for E-Way Bill compliance)
    @JsonProperty("transportationMode")
    private String transportationMode;

    @JsonProperty("transportationDistance")
    private Integer transportationDistance;

    @JsonProperty("transporterName")
    private String transporterName;

    @JsonProperty("transporterId")
    private String transporterId;

    @JsonProperty("vehicleNumber")
    private String vehicleNumber;

    @JsonProperty("dispatchDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dispatchDate;

    // Financial Details
    @NotNull(message = "Total taxable value is required")
    @DecimalMin(value = "0.0", message = "Total taxable value must be non-negative")
    @JsonProperty("totalTaxableValue")
    private BigDecimal totalTaxableValue;

    @DecimalMin(value = "0.0", message = "CGST amount must be non-negative")
    @JsonProperty("totalCgstAmount")
    @Builder.Default
    private BigDecimal totalCgstAmount = BigDecimal.ZERO;

    @DecimalMin(value = "0.0", message = "SGST amount must be non-negative")
    @JsonProperty("totalSgstAmount")
    @Builder.Default
    private BigDecimal totalSgstAmount = BigDecimal.ZERO;

    @DecimalMin(value = "0.0", message = "IGST amount must be non-negative")
    @JsonProperty("totalIgstAmount")
    @Builder.Default
    private BigDecimal totalIgstAmount = BigDecimal.ZERO;

    @NotNull(message = "Total invoice value is required")
    @DecimalMin(value = "0.0", message = "Total invoice value must be non-negative")
    @JsonProperty("totalInvoiceValue")
    private BigDecimal totalInvoiceValue;

    // Payment Details
    @Size(max = 500, message = "Payment terms cannot exceed 500 characters")
    @JsonProperty("paymentTerms")
    private String paymentTerms;

    @JsonProperty("dueDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate dueDate;

    // Status
    @JsonProperty("status")
    private String status;

    // Invoice Approval Information
    @JsonProperty("approvedBy")
    private String approvedBy;

    // Terms and Conditions (persisted at invoice generation time)
    @Size(max = 2000, message = "Terms and conditions cannot exceed 2000 characters")
    @JsonProperty("termsAndConditions")
    private String termsAndConditions;

    // Bank Details (persisted at invoice generation time)
    @Size(max = 100, message = "Bank name cannot exceed 100 characters")
    @JsonProperty("bankName")
    private String bankName;

    @Size(max = 20, message = "Account number cannot exceed 20 characters")
    @JsonProperty("accountNumber")
    private String accountNumber;

    @Size(max = 11, message = "IFSC code cannot exceed 11 characters")
    @JsonProperty("ifscCode")
    private String ifscCode;

    // Amount in Words
    @Size(max = 500, message = "Amount in words cannot exceed 500 characters")
    @JsonProperty("amountInWords")
    private String amountInWords;

    // Payment Tracking
    @DecimalMin(value = "0.0", message = "Total paid amount must be non-negative")
    @JsonProperty("totalPaidAmount")
    @Builder.Default
    private BigDecimal totalPaidAmount = BigDecimal.ZERO;

    @DecimalMin(value = "0.0", message = "Total TDS amount must be non-negative")
    @JsonProperty("totalTdsAmountDeducted")
    @Builder.Default
    private BigDecimal totalTdsAmountDeducted = BigDecimal.ZERO;

    // Document Reference
    @JsonProperty("documentPath")
    private String documentPath;

    // Tenant Information
    @JsonProperty("tenantId")
    private Long tenantId;

    /**
     * Complete tenant details including name, GSTIN, address, etc.
     * Provides all tenant information needed for invoice display without additional API calls.
     */
    @JsonProperty("tenant")
    private TenantRepresentation tenant;

    // Audit Fields
    @JsonProperty("createdAt")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonProperty("updatedAt")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    // Computed Properties
    @JsonProperty("canBeAmended")
    public boolean canBeAmended() {
        return "GENERATED".equals(status) || "SENT".equals(status);
    }

    @JsonProperty("isOverdue")
    public boolean isOverdue() {
        return dueDate != null && dueDate.isBefore(LocalDate.now()) 
               && !"PAID".equals(status) && !"CANCELLED".equals(status);
    }

    @JsonProperty("remainingAmount")
    public BigDecimal getRemainingAmount() {
        if (totalInvoiceValue == null) {
            return BigDecimal.ZERO;
        }
        if (totalPaidAmount == null) {
            return totalInvoiceValue;
        }
        return totalInvoiceValue.subtract(totalPaidAmount);
    }

    @JsonProperty("isFullyPaid")
    public boolean isFullyPaid() {
        return "PAID".equals(status);
    }

    @JsonProperty("isPartiallyPaid")
    public boolean isPartiallyPaid() {
        return "PARTIALLY_PAID".equals(status);
    }

    @JsonProperty("statusDisplayName")
    public String getStatusDisplayName() {
        if (status == null) return "";
        return switch (status) {
            case "DRAFT" -> "Draft";
            case "GENERATED" -> "Generated";
            case "SENT" -> "Sent";
            case "PARTIALLY_PAID" -> "Partially Paid";
            case "PAID" -> "Paid";
            case "CANCELLED" -> "Cancelled";
            default -> status;
        };
    }

    @JsonProperty("invoiceTypeDisplayName")
    public String getInvoiceTypeDisplayName() {
        if (invoiceType == null) return "";
        return switch (invoiceType) {
            case "TAX_INVOICE" -> "Tax Invoice";
            case "BILL_OF_SUPPLY" -> "Bill of Supply";
            case "EXPORT_INVOICE" -> "Export Invoice";
            case "REVISED_INVOICE" -> "Revised Invoice";
            case "CREDIT_NOTE" -> "Credit Note";
            case "DEBIT_NOTE" -> "Debit Note";
            default -> invoiceType;
        };
    }

    // GST Rate Calculations (Simplified)
    @JsonProperty("effectiveTaxRate")
    public BigDecimal getEffectiveTaxRate() {
        if (totalTaxableValue == null || totalTaxableValue.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal totalTax = totalCgstAmount.add(totalSgstAmount).add(totalIgstAmount);
        return totalTax.divide(totalTaxableValue, 2, java.math.RoundingMode.HALF_UP).multiply(new BigDecimal("100"));
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
        private String packagingType;
        
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
