package com.jangid.forging_process_management_service.entities.gst;

import com.jangid.forging_process_management_service.entities.Tenant;
import com.jangid.forging_process_management_service.entities.dispatch.DispatchBatch;
import com.jangid.forging_process_management_service.entities.buyer.BuyerEntity;
import com.jangid.forging_process_management_service.entities.vendor.VendorEntity;
import com.jangid.forging_process_management_service.entities.order.WorkType;

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
import jakarta.persistence.UniqueConstraint;
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
@Table(name = "delivery_challan", 
    indexes = {
        @Index(name = "idx_delivery_challan_dispatch_batch", columnList = "dispatch_batch_id"),
        @Index(name = "idx_delivery_challan_tenant_status", columnList = "tenant_id, status"),
        @Index(name = "idx_delivery_challan_challan_date", columnList = "challan_date"),
        @Index(name = "idx_delivery_challan_deleted", columnList = "deleted")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_challan_number_tenant", columnNames = {"challan_number", "tenant_id"})
    }
)
@EntityListeners(AuditingEntityListener.class)
public class DeliveryChallan {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "delivery_challan_sequence_generator")
    @SequenceGenerator(name = "delivery_challan_sequence_generator", 
                      sequenceName = "delivery_challan_sequence", allocationSize = 1)
    private Long id;

    @NotNull
    @Size(max = 50)
    @Column(name = "challan_number", nullable = false, length = 50)
    private String challanNumber;

    @NotNull
    @Column(name = "challan_date_time", nullable = false)
    private LocalDateTime challanDateTime;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "challan_type", nullable = false, length = 50)
    private ChallanType challanType;

    // Details for "OTHER" challan type
    @Size(max = 500)
    @Column(name = "other_challan_type_details", length = 500)
    private String otherChallanTypeDetails;

    // Work Type - determines which HSN/SAC and tax rates to use (persisted for challan generation logic)
    @Enumerated(EnumType.STRING)
    @Column(name = "work_type", length = 20)
    private WorkType workType;

    // Relationships
    /**
     * Many-to-many relationship with DispatchBatch through junction table.
     * A challan can be associated with multiple dispatch batches.
     * Use Case: Multiple batches sent together in one shipment (e.g., to same vendor for job work)
     */
    @OneToMany(mappedBy = "deliveryChallan", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ChallanDispatchBatch> challanDispatchBatches = new ArrayList<>();

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "converted_to_invoice_id")
    private Invoice convertedToInvoice;

    // Order Reference (for traceability and reporting)
    @Column(name = "order_id")
    private Long orderId;

    // Consignee Details - Use existing Buyer/Vendor entities
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consignee_buyer_entity_id")
    private BuyerEntity consigneeBuyerEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consignee_vendor_entity_id")
    private VendorEntity consigneeVendorEntity;

    // Transportation Details
    @NotNull
    @Column(name = "transportation_reason", nullable = false, columnDefinition = "TEXT")
    private String transportationReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "transportation_mode", length = 20)
    @Builder.Default
    private TransportationMode transportationMode = TransportationMode.ROAD;

    @Column(name = "expected_delivery_date")
    private LocalDate expectedDeliveryDate;

    @Column(name = "actual_delivery_date")
    private LocalDate actualDeliveryDate;

    // Line Items (Itemized details of the challan)
    @OneToMany(mappedBy = "deliveryChallan", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ChallanLineItem> lineItems = new ArrayList<>();

    // Transportation Details Extended
    @Size(max = 200)
    @Column(name = "transporter_name", length = 200)
    private String transporterName;

    @Size(max = 15)
    @Column(name = "transporter_id", length = 15)
    private String transporterId;

    @Size(max = 20)
    @Column(name = "vehicle_number", length = 20)
    private String vehicleNumber;

    @Column(name = "transportation_distance")
    private Integer transportationDistance;

    // Basic Financial Summary
    @Column(name = "total_quantity", precision = 15, scale = 3)
    @Builder.Default
    private BigDecimal totalQuantity = BigDecimal.ZERO;

    // Declared value of goods (ALWAYS required for GST compliance - tracking purposes)
    @NotNull
    @Column(name = "total_taxable_value", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalTaxableValue = BigDecimal.ZERO;

    @Column(name = "total_cgst_amount", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalCgstAmount = BigDecimal.ZERO;

    @Column(name = "total_sgst_amount", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalSgstAmount = BigDecimal.ZERO;

    @Column(name = "total_igst_amount", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalIgstAmount = BigDecimal.ZERO;

    // Total value = taxable value + taxes (if applicable)
    // For non-taxable challans, this equals totalTaxableValue
    @Column(name = "total_value", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalValue = BigDecimal.ZERO;

    // Status and Workflow
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    @Builder.Default
    private ChallanStatus status = ChallanStatus.DRAFT;

    // Document References
    @Size(max = 500)
    @Column(name = "document_path", length = 500)
    private String documentPath;

    // Consignor/Supplier Details (Persisted at challan generation for data integrity)
    @Size(max = 15)
    @Column(name = "consignor_gstin", length = 15)
    private String consignorGstin;

    @Size(max = 200)
    @Column(name = "consignor_name", length = 200)
    private String consignorName;

    @Size(max = 500)
    @Column(name = "consignor_address", length = 500)
    private String consignorAddress;

    @Size(max = 2)
    @Column(name = "consignor_state_code", length = 2)
    private String consignorStateCode;

    // Terms and Conditions (persisted at challan generation time)
    @Size(max = 2000)
    @Column(name = "terms_and_conditions", length = 2000)
    private String termsAndConditions;

    // Bank Details (persisted for reference - optional for challans)
    @Size(max = 100)
    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Size(max = 20)
    @Column(name = "account_number", length = 20)
    private String accountNumber;

    @Size(max = 11)
    @Column(name = "ifsc_code", length = 11)
    private String ifscCode;

    // Amount in Words (for display purposes)
    @Size(max = 500)
    @Column(name = "amount_in_words", length = 500)
    private String amountInWords;

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
    public boolean canBeConvertedToInvoice() {
        return status == ChallanStatus.DELIVERED && convertedToInvoice == null;
    }

    public boolean canBeCancelled() {
        // Only GENERATED challans can be cancelled
        return status == ChallanStatus.GENERATED;
    }

    public boolean canBeDeleted() {
        // Only GENERATED challans can be deleted
        return status == ChallanStatus.GENERATED;
    }

    public boolean canBeUpdated() {
        // Only GENERATED challans can be updated
        return status == ChallanStatus.GENERATED;
    }

    public boolean isInterState() {
        String consignorStateCode = getConsignorStateCodeFromTenant();
        String consigneeStateCode = getConsigneeStateCode();
        
        return consignorStateCode != null && consigneeStateCode != null 
               && !consignorStateCode.equals(consigneeStateCode);
    }

    public void markAsConverted(Invoice invoice) {
        this.convertedToInvoice = invoice;
        this.status = ChallanStatus.CONVERTED_TO_INVOICE;
    }

    // Calculate totals from line items
    public void calculateTotals() {
        if (lineItems != null && !lineItems.isEmpty()) {
            totalQuantity = lineItems.stream()
                .map(ChallanLineItem::getQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            totalTaxableValue = lineItems.stream()
                .map(ChallanLineItem::getTaxableValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            // Calculate tax amounts only if tax is applicable for this challan type
            totalCgstAmount = lineItems.stream()
                .map(ChallanLineItem::getCgstAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            totalSgstAmount = lineItems.stream()
                .map(ChallanLineItem::getSgstAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            totalIgstAmount = lineItems.stream()
                .map(ChallanLineItem::getIgstAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        
        // Calculate total value
        // For taxable challans: taxable value + taxes
        // For non-taxable challans: just the declared taxable value
        totalValue = totalTaxableValue
            .add(totalCgstAmount)
            .add(totalSgstAmount)
            .add(totalIgstAmount);
    }

    // Helper methods for line items
    public void addLineItem(ChallanLineItem lineItem) {
        if (lineItems == null) {
            lineItems = new ArrayList<>();
        }
        lineItems.add(lineItem);
        lineItem.setDeliveryChallan(this);
        lineItem.setTenant(this.tenant);
    }

    public void removeLineItem(ChallanLineItem lineItem) {
        if (lineItems != null) {
            lineItems.remove(lineItem);
            lineItem.setDeliveryChallan(null);
        }
    }

    public boolean hasLineItems() {
        return lineItems != null && !lineItems.isEmpty();
    }

    public int getLineItemCount() {
        return lineItems != null ? lineItems.size() : 0;
    }

    // Helper methods for managing challan dispatch batches
    public void addChallanDispatchBatch(ChallanDispatchBatch challanDispatchBatch) {
        if (challanDispatchBatches == null) {
            challanDispatchBatches = new ArrayList<>();
        }
        challanDispatchBatches.add(challanDispatchBatch);
        challanDispatchBatch.setDeliveryChallan(this);
        challanDispatchBatch.setTenant(this.tenant);
    }

    public void removeChallanDispatchBatch(ChallanDispatchBatch challanDispatchBatch) {
        if (challanDispatchBatches != null) {
            challanDispatchBatches.remove(challanDispatchBatch);
            challanDispatchBatch.setDeliveryChallan(null);
        }
    }

    public boolean hasDispatchBatches() {
        return challanDispatchBatches != null && !challanDispatchBatches.isEmpty();
    }

    public int getDispatchBatchCount() {
        return challanDispatchBatches != null ? challanDispatchBatches.size() : 0;
    }

    public List<DispatchBatch> getDispatchBatches() {
        if (challanDispatchBatches == null || challanDispatchBatches.isEmpty()) {
            return new ArrayList<>();
        }
        return challanDispatchBatches.stream()
            .map(ChallanDispatchBatch::getDispatchBatch)
            .collect(java.util.stream.Collectors.toList());
    }

    public List<Long> getDispatchBatchIds() {
        if (challanDispatchBatches == null || challanDispatchBatches.isEmpty()) {
            return new ArrayList<>();
        }
        return challanDispatchBatches.stream()
            .map(cdb -> cdb.getDispatchBatch().getId())
            .collect(java.util.stream.Collectors.toList());
    }

    // Helper methods to get consignor details
    // Note: These fields are now persisted. Use tenant as fallback for backward compatibility.
    public String getConsignorName() {
        return consignorName != null ? consignorName : (tenant != null ? tenant.getTenantName() : null);
    }

    public String getConsignorGstin() {
        return consignorGstin != null ? consignorGstin : (tenant != null ? tenant.getGstin() : null);
    }

    public String getConsignorAddress() {
        return consignorAddress != null ? consignorAddress : (tenant != null ? tenant.getAddress() : null);
    }

    public String getConsignorStateCodeFromTenant() {
        return consignorStateCode != null ? consignorStateCode : (tenant != null ? tenant.getStateCode() : null);
    }

    public String getConsignorPincode() {
        // Fallback to tenant if needed (though not persisted separately for challan)
        return tenant != null ? tenant.getPincode() : null;
    }

    // Helper methods to get consignee details
    public String getConsigneeName() {
        if (consigneeBuyerEntity != null) {
            return consigneeBuyerEntity.getBuyerEntityName();
        } else if (consigneeVendorEntity != null) {
            return consigneeVendorEntity.getVendorEntityName();
        }
        return null;
    }

    public String getConsigneeGstin() {
        if (consigneeBuyerEntity != null) {
            return consigneeBuyerEntity.getGstinUin();
        } else if (consigneeVendorEntity != null) {
            return consigneeVendorEntity.getGstinUin();
        }
        return null;
    }

    public String getConsigneeAddress() {
        if (consigneeBuyerEntity != null) {
            return consigneeBuyerEntity.getAddress();
        } else if (consigneeVendorEntity != null) {
            return consigneeVendorEntity.getAddress();
        }
        return null;
    }

    public String getConsigneeStateCode() {
        if (consigneeBuyerEntity != null) {
            return consigneeBuyerEntity.getStateCode();
        } else if (consigneeVendorEntity != null) {
            return consigneeVendorEntity.getStateCode();
        }
        return null;
    }

    public String getConsigneePincode() {
        if (consigneeBuyerEntity != null) {
            return consigneeBuyerEntity.getPincode();
        } else if (consigneeVendorEntity != null) {
            return consigneeVendorEntity.getPincode();
        }
        return null;
    }

    // Check if we have a valid consignee
    public boolean hasValidConsignee() {
        return consigneeBuyerEntity != null || consigneeVendorEntity != null;
    }

    // Get consignee type for display
    public String getConsigneeType() {
        if (consigneeBuyerEntity != null) {
            return "BUYER";
        } else if (consigneeVendorEntity != null) {
            return "VENDOR";
        }
        return "UNKNOWN";
    }
}