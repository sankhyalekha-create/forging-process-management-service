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
import java.util.stream.Collectors;

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
    /**
     * Many-to-many relationship with DispatchBatch through junction table.
     * An invoice can be associated with multiple dispatch batches.
     */
    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<InvoiceDispatchBatch> invoiceDispatchBatches = new ArrayList<>();

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

    // Supplier Details - Persisted at invoice generation (for data integrity and audit trail)
    @Size(max = 15)
    @Column(name = "supplier_gstin", length = 15)
    private String supplierGstin;

    @Size(max = 200)
    @Column(name = "supplier_name", length = 200)
    private String supplierName;

    @Size(max = 500)
    @Column(name = "supplier_address", length = 500)
    private String supplierAddress;

    @Size(max = 2)
    @Column(name = "supplier_state_code", length = 2)
    private String supplierStateCode;

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

    /**
     * Transport identifier - stores vehicle/transport number based on transportation mode:
     * - ROAD: Vehicle registration number (e.g., MH12AB1234)
     * - RAIL: Railway Receipt Number or Train/Wagon Number
     * - AIR: Airway Bill Number or Flight Number
     * - SHIP: Bill of Lading Number or Ship Name
     * Required for E-Way Bill generation (GST compliance)
     */
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

    // Invoice Approval Information
    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    // Invoice Cancellation Information
    @Column(name = "cancelled_by", length = 100)
    private String cancelledBy;

    @Size(max = 500)
    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @Column(name = "cancellation_date")
    private LocalDateTime cancellationDate;

    // Terms and Conditions (persisted at invoice generation time)
    @Size(max = 2000)
    @Column(name = "terms_and_conditions", length = 2000)
    private String termsAndConditions;

    // Bank Details (persisted at invoice generation time for data integrity and audit trail)
    @Size(max = 100)
    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Size(max = 20)
    @Column(name = "account_number", length = 20)
    private String accountNumber;

    @Size(max = 11)
    @Column(name = "ifsc_code", length = 11)
    private String ifscCode;

    // Amount in Words (for legal/invoice display purposes)
    @Size(max = 500)
    @Column(name = "amount_in_words", length = 500)
    private String amountInWords;

    // Payment tracking - Advanced payment management
    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Payment> payments = new ArrayList<>();

    @Column(name = "total_paid_amount", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalPaidAmount = BigDecimal.ZERO;

    @Column(name = "total_tds_amount_deducted", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalTdsAmountDeducted = BigDecimal.ZERO;

    public boolean canBeCancelled() {
        // Only GENERATED invoices with no payments can be cancelled
        // SENT invoices cannot be cancelled because goods are already dispatched
        // (SENT status means invoice sent AND dispatch batches are DISPATCHED)
        // For SENT invoices, use Credit Note process instead
        return status == InvoiceStatus.GENERATED
               && (totalPaidAmount == null || totalPaidAmount.compareTo(BigDecimal.ZERO) == 0);
    }

    public boolean canBeDeleted() {
        // Only DRAFT invoices can be deleted
        return status == InvoiceStatus.DRAFT;
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

    // Note: Supplier getters/setters are generated by Lombok
    // Supplier fields (supplierGstin, supplierName, supplierAddress, supplierStateCode) 
    // are populated during invoice generation from GSTConfiguration

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
            return "CUSTOMER";
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

    // Helper methods for managing invoice dispatch batches
    public void addInvoiceDispatchBatch(InvoiceDispatchBatch invoiceDispatchBatch) {
        if (invoiceDispatchBatches == null) {
            invoiceDispatchBatches = new ArrayList<>();
        }
        invoiceDispatchBatches.add(invoiceDispatchBatch);
        invoiceDispatchBatch.setInvoice(this);
        invoiceDispatchBatch.setTenant(this.tenant);
    }

    public void removeInvoiceDispatchBatch(InvoiceDispatchBatch invoiceDispatchBatch) {
        if (invoiceDispatchBatches != null) {
            invoiceDispatchBatches.remove(invoiceDispatchBatch);
            invoiceDispatchBatch.setInvoice(null);
        }
    }

    public boolean hasDispatchBatches() {
        return invoiceDispatchBatches != null && !invoiceDispatchBatches.isEmpty();
    }

    public int getDispatchBatchCount() {
        return invoiceDispatchBatches != null ? invoiceDispatchBatches.size() : 0;
    }

    public List<DispatchBatch> getDispatchBatches() {
        if (invoiceDispatchBatches == null || invoiceDispatchBatches.isEmpty()) {
            return new ArrayList<>();
        }
        return invoiceDispatchBatches.stream()
            .map(InvoiceDispatchBatch::getDispatchBatch)
            .collect(Collectors.toList());
    }

    public List<Long> getDispatchBatchIds() {
        if (invoiceDispatchBatches == null || invoiceDispatchBatches.isEmpty()) {
            return new ArrayList<>();
        }
        return invoiceDispatchBatches.stream()
            .map(idb -> idb.getDispatchBatch().getId())
            .collect(Collectors.toList());
    }

    // Helper methods for managing payments
    public void addPayment(Payment payment) {
        if (payments == null) {
            payments = new ArrayList<>();
        }
        payments.add(payment);
        payment.setInvoice(this);
        payment.setTenant(this.tenant);
        
        // Update total paid amount
        updateTotalPaidAmount();
    }

    public void removePayment(Payment payment) {
        if (payments != null) {
            payments.remove(payment);
            payment.setInvoice(null);
            
            // Update total paid amount
            updateTotalPaidAmount();
        }
    }

    public boolean hasPayments() {
        return payments != null && !payments.isEmpty();
    }

    public int getPaymentCount() {
        return payments != null ? payments.size() : 0;
    }

    public void updateTotalPaidAmount() {
        if (payments == null || payments.isEmpty()) {
            totalPaidAmount = BigDecimal.ZERO;
            totalTdsAmountDeducted = BigDecimal.ZERO;
        } else {
            // Sum only RECEIVED payments (exclude REVERSED, CANCELLED)
            totalPaidAmount = payments.stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.RECEIVED)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            // Sum TDS amounts from RECEIVED payments
            totalTdsAmountDeducted = payments.stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.RECEIVED)
                .map(payment -> payment.getTdsAmount() != null ? payment.getTdsAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        
        // Update invoice status based on payment
        updateInvoiceStatusByPayment();
    }

    public void updateInvoiceStatusByPayment() {
        if (totalPaidAmount == null || totalPaidAmount.compareTo(BigDecimal.ZERO) == 0) {
            // No payment received - keep current status (GENERATED or SENT)
            return;
        }
        
        if (totalPaidAmount.compareTo(totalInvoiceValue) >= 0) {
            // Fully paid
            this.status = InvoiceStatus.PAID;
        } else if (totalPaidAmount.compareTo(BigDecimal.ZERO) > 0) {
            // Partially paid
            this.status = InvoiceStatus.PARTIALLY_PAID;
        }
    }

    public BigDecimal getRemainingAmount() {
        if (totalPaidAmount == null) {
            return totalInvoiceValue;
        }
        return totalInvoiceValue.subtract(totalPaidAmount);
    }

    public boolean isFullyPaid() {
        return status == InvoiceStatus.PAID;
    }

    public boolean isPartiallyPaid() {
        return status == InvoiceStatus.PARTIALLY_PAID;
    }
}