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
@Table(name = "delivery_challan", indexes = {
    @Index(name = "idx_delivery_challan_dispatch_batch", columnList = "dispatch_batch_id"),
    @Index(name = "idx_delivery_challan_tenant_status", columnList = "tenant_id, status"),
    @Index(name = "idx_delivery_challan_challan_date", columnList = "challan_date"),
    @Index(name = "idx_delivery_challan_deleted", columnList = "deleted")
})
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
    @Column(name = "challan_date", nullable = false)
    private LocalDateTime challanDate;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "challan_type", nullable = false, length = 50)
    private ChallanType challanType;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dispatch_batch_id")
    private DispatchBatch dispatchBatch;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "converted_to_invoice_id")
    private Invoice convertedToInvoice;

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

    // Basic Financial Summary
    @Column(name = "total_quantity", precision = 15, scale = 3)
    @Builder.Default
    private BigDecimal totalQuantity = BigDecimal.ZERO;

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

    // Helper methods to get consignor details from tenant's GST configuration
    public String getConsignorName() {
        return tenant != null ? tenant.getTenantName() : null;
    }

    public String getConsignorGstin() {
        return tenant != null ? tenant.getGstin() : null;
    }

    public String getConsignorAddress() {
        return tenant != null ? tenant.getAddress() : null;
    }

    public String getConsignorStateCodeFromTenant() {
        return tenant != null ? tenant.getStateCode() : null;
    }

    public String getConsignorPincode() {
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