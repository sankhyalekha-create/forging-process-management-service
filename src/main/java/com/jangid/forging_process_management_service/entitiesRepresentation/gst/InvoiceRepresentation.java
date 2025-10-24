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

    @JsonProperty("dispatchBatchId")
    private Long dispatchBatchId;

    @JsonProperty("deliveryChallanId")
    private Long deliveryChallanId;

    @JsonProperty("originalInvoiceId")
    private Long originalInvoiceId;

    // Order Reference (for traceability and reporting)
    @JsonProperty("orderId")
    private Long orderId;

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
    @JsonProperty("canBeAmended")
    public boolean canBeAmended() {
        return "GENERATED".equals(status) || "SENT".equals(status);
    }

    @JsonProperty("isOverdue")
    public boolean isOverdue() {
        return dueDate != null && dueDate.isBefore(LocalDate.now()) 
               && !"PAID".equals(status) && !"CANCELLED".equals(status);
    }

    @JsonProperty("statusDisplayName")
    public String getStatusDisplayName() {
        if (status == null) return "";
        return switch (status) {
            case "DRAFT" -> "Draft";
            case "GENERATED" -> "Generated";
            case "SENT" -> "Sent";
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
}
