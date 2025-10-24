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

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "invoice", indexes = {
    @Index(name = "idx_invoice_dispatch_batch", columnList = "dispatch_batch_id"),
    @Index(name = "idx_invoice_order", columnList = "order_id"),
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
    private InvoiceType invoiceType = InvoiceType.TAX_INVOICE;

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

    // Order Reference (for traceability and reporting)
    @Column(name = "order_id")
    private Long orderId;

    @Size(max = 50)
    @Column(name = "customer_po_number", length = 50)
    private String customerPoNumber;

    @Column(name = "customer_po_date")
    private LocalDate customerPoDate;

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

    // Invoice Line Items (Itemized details of the invoice)
    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<InvoiceLineItem> lineItems = new ArrayList<>();

    // Transportation Details (for E-Way Bill compliance)
    @Enumerated(EnumType.STRING)
    @Column(name = "transportation_mode", length = 20)
    @Builder.Default
    private TransportationMode transportationMode = TransportationMode.ROAD;

    @Column(name = "transportation_distance")
    private Integer transportationDistance;

    @Size(max = 200)
    @Column(name = "transporter_name", length = 200)
    private String transporterName;

    @Size(max = 15)
    @Column(name = "transporter_id", length = 15)
    private String transporterId;

    @Size(max = 20)
    @Column(name = "vehicle_number", length = 20)
    private String vehicleNumber;

    @Column(name = "dispatch_date")
    private LocalDateTime dispatchDate;

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
        // Calculate totals from line items if they exist
        if (lineItems != null && !lineItems.isEmpty()) {
            totalTaxableValue = lineItems.stream()
                .map(InvoiceLineItem::getTaxableValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            totalCgstAmount = lineItems.stream()
                .map(InvoiceLineItem::getCgstAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            totalSgstAmount = lineItems.stream()
                .map(InvoiceLineItem::getSgstAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            totalIgstAmount = lineItems.stream()
                .map(InvoiceLineItem::getIgstAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        
        // Calculate total invoice value
        totalInvoiceValue = totalTaxableValue
            .add(totalCgstAmount)
            .add(totalSgstAmount)
            .add(totalIgstAmount);
    }

    // Helper methods for line items
    public void addLineItem(InvoiceLineItem lineItem) {
        if (lineItems == null) {
            lineItems = new ArrayList<>();
        }
        lineItems.add(lineItem);
        lineItem.setInvoice(this);
        lineItem.setTenant(this.tenant);
    }

    public void removeLineItem(InvoiceLineItem lineItem) {
        if (lineItems != null) {
            lineItems.remove(lineItem);
            lineItem.setInvoice(null);
        }
    }

    public boolean hasLineItems() {
        return lineItems != null && !lineItems.isEmpty();
    }

    public int getLineItemCount() {
        return lineItems != null ? lineItems.size() : 0;
    }

    // Check if e-way bill is required (based on invoice value threshold)
    public boolean requiresEwayBill() {
        // E-way bill required if invoice value > ₹50,000
        return totalInvoiceValue != null && 
               totalInvoiceValue.compareTo(BigDecimal.valueOf(50000)) > 0;
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