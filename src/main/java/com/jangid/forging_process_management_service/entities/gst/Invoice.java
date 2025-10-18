package com.jangid.forging_process_management_service.entities.gst;

import com.jangid.forging_process_management_service.entities.Tenant;
import com.jangid.forging_process_management_service.entities.dispatch.DispatchBatch;
import com.jangid.forging_process_management_service.entities.buyer.BuyerEntity;
import com.jangid.forging_process_management_service.entities.vendor.VendorEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "invoice", indexes = {
    @Index(name = "idx_invoice_dispatch_batch", columnList = "dispatch_batch_id"),
    @Index(name = "idx_invoice_tenant_status", columnList = "tenant_id, status"),
    @Index(name = "idx_invoice_date", columnList = "invoice_date"),
    @Index(name = "idx_invoice_recipient_buyer", columnList = "recipient_buyer_entity_id"),
    @Index(name = "idx_invoice_recipient_vendor", columnList = "recipient_vendor_entity_id"),
    @Index(name = "idx_invoice_deleted", columnList = "deleted")
})
@EntityListeners(AuditingEntityListener.class)
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "invoice_sequence_generator")
    @SequenceGenerator(name = "invoice_sequence_generator", 
                      sequenceName = "invoice_sequence", allocationSize = 1)
    private Long id;

    @NotNull
    @Size(max = 50)
    @Column(name = "invoice_number", nullable = false, length = 50)
    private String invoiceNumber;

    @NotNull
    @Column(name = "invoice_date", nullable = false)
    private LocalDateTime invoiceDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "invoice_type", length = 20)
    @Builder.Default
    private InvoiceType invoiceType = InvoiceType.REGULAR;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dispatch_batch_id")
    private DispatchBatch dispatchBatch;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_challan_id")
    private DeliveryChallan deliveryChallan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_invoice_id")
    private Invoice originalInvoice;

    // Recipient Details - Use existing Buyer/Vendor entities
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_buyer_entity_id")
    private BuyerEntity recipientBuyerEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_vendor_entity_id")
    private VendorEntity recipientVendorEntity;

    // Tax Jurisdiction
    @NotNull
    @Size(max = 50)
    @Column(name = "place_of_supply", nullable = false, length = 50)
    private String placeOfSupply;

    @Column(name = "is_inter_state")
    @Builder.Default
    private Boolean isInterState = false;

    // Financial Details (Simplified - Basic GST calculation)
    @NotNull
    @Column(name = "total_taxable_value", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalTaxableValue;

    @Column(name = "total_cgst_amount", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalCgstAmount = BigDecimal.ZERO;

    @Column(name = "total_sgst_amount", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalSgstAmount = BigDecimal.ZERO;

    @Column(name = "total_igst_amount", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalIgstAmount = BigDecimal.ZERO;

    @NotNull
    @Column(name = "total_invoice_value", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalInvoiceValue;

    // Payment Details (Simplified)
    @Size(max = 500)
    @Column(name = "payment_terms", length = 500)
    private String paymentTerms;

    @Column(name = "due_date")
    private LocalDate dueDate;

    // Status and Workflow
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    @Builder.Default
    private InvoiceStatus status = InvoiceStatus.DRAFT;

    // Document References
    @Size(max = 500)
    @Column(name = "document_path", length = 500)
    private String documentPath;

    // Standard Audit Fields
    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Version
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder.Default
    private boolean deleted = false;

    // Business Methods
    public boolean canBeAmended() {
        return status == InvoiceStatus.GENERATED || status == InvoiceStatus.SENT;
    }

    public boolean isInterState() {
        return Boolean.TRUE.equals(isInterState);
    }

    public void calculateTotals() {
        // Simple calculation for basic implementation
        totalInvoiceValue = totalTaxableValue
            .add(totalCgstAmount)
            .add(totalSgstAmount)
            .add(totalIgstAmount);
    }

    public void markAsPaid() {
        this.status = InvoiceStatus.PAID;
    }

    // Helper methods to get supplier details from tenant's GST configuration
    public String getSupplierName() {
        return tenant != null ? tenant.getTenantName() : null;
    }

    public String getSupplierGstin() {
        // This would be fetched from GSTConfiguration in service layer
        return null; // To be populated from GSTConfiguration
    }

    public String getSupplierAddress() {
        return tenant != null ? tenant.getAddress() : null;
    }

    public String getSupplierStateCode() {
        // This would be fetched from GSTConfiguration in service layer
        return null; // To be populated from GSTConfiguration.companyStateCode
    }

    public String getSupplierPincode() {
        // This would be fetched from GSTConfiguration in service layer
        return null; // To be populated from GSTConfiguration.companyPincode
    }

    // Helper methods to get recipient details
    public String getRecipientName() {
        if (recipientBuyerEntity != null) {
            return recipientBuyerEntity.getBuyerEntityName();
        } else if (recipientVendorEntity != null) {
            return recipientVendorEntity.getVendorEntityName();
        }
        return null;
    }

    public String getRecipientGstin() {
        if (recipientBuyerEntity != null) {
            return recipientBuyerEntity.getGstinUin();
        } else if (recipientVendorEntity != null) {
            return recipientVendorEntity.getGstinUin();
        }
        return null;
    }

    public String getRecipientAddress() {
        if (recipientBuyerEntity != null) {
            return recipientBuyerEntity.getAddress();
        } else if (recipientVendorEntity != null) {
            return recipientVendorEntity.getAddress();
        }
        return null;
    }

    public String getRecipientStateCode() {
        if (recipientBuyerEntity != null) {
            return recipientBuyerEntity.getStateCode();
        } else if (recipientVendorEntity != null) {
            return recipientVendorEntity.getStateCode();
        }
        return null;
    }

    public String getRecipientPincode() {
        if (recipientBuyerEntity != null) {
            return recipientBuyerEntity.getPincode();
        } else if (recipientVendorEntity != null) {
            return recipientVendorEntity.getPincode();
        }
        return null;
    }

    // Check if we have a valid recipient
    public boolean hasValidRecipient() {
        return recipientBuyerEntity != null || recipientVendorEntity != null;
    }

    // Get recipient type for display
    public String getRecipientType() {
        if (recipientBuyerEntity != null) {
            return "BUYER";
        } else if (recipientVendorEntity != null) {
            return "VENDOR";
        }
        return "UNKNOWN";
    }

    // Update inter-state calculation based on actual addresses
    public void updateInterStateFlag() {
        String supplierStateCode = getSupplierStateCode();
        String recipientStateCode = getRecipientStateCode();
        
        this.isInterState = supplierStateCode != null && recipientStateCode != null 
                           && !supplierStateCode.equals(recipientStateCode);
    }
}