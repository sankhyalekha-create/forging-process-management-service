package com.jangid.forging_process_management_service.entities.gst;

import com.jangid.forging_process_management_service.entities.Tenant;
import com.jangid.forging_process_management_service.entities.dispatch.DispatchBatch;

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
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "eway_bill", indexes = {
    @Index(name = "idx_eway_bill_dispatch_batch", columnList = "dispatch_batch_id"),
    @Index(name = "idx_eway_bill_invoice", columnList = "invoice_id"),
    @Index(name = "idx_eway_bill_challan", columnList = "delivery_challan_id"),
    @Index(name = "idx_eway_bill_tenant_status", columnList = "tenant_id, status"),
    @Index(name = "idx_eway_bill_validity", columnList = "valid_until"),
    @Index(name = "idx_eway_bill_deleted", columnList = "deleted")
})
@EntityListeners(AuditingEntityListener.class)
public class EWayBill {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "eway_bill_sequence_generator")
    @SequenceGenerator(name = "eway_bill_sequence_generator", 
                      sequenceName = "eway_bill_sequence", allocationSize = 1)
    private Long id;

    @Size(max = 20)
    @Column(name = "eway_bill_number", length = 20, unique = true)
    private String ewayBillNumber;

    @NotNull
    @Column(name = "eway_bill_date", nullable = false)
    private LocalDateTime ewayBillDate;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dispatch_batch_id")
    private DispatchBatch dispatchBatch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_challan_id")
    private DeliveryChallan deliveryChallan;

    // Document Details (Simplified)
    @NotNull
    @Size(max = 50)
    @Column(name = "document_number", nullable = false, length = 50)
    private String documentNumber;

    @NotNull
    @Column(name = "document_date", nullable = false)
    private LocalDateTime documentDate;

    @NotNull
    @Size(max = 20)
    @Column(name = "document_type", nullable = false, length = 20)
    private String documentType; // INVOICE or CHALLAN

    // Supplier Details (Simplified)
    @NotNull
    @Size(max = 15)
    @Column(name = "supplier_gstin", nullable = false, length = 15)
    private String supplierGstin;

    @NotNull
    @Size(max = 200)
    @Column(name = "supplier_name", nullable = false, length = 200)
    private String supplierName;

    @NotNull
    @Size(max = 2)
    @Column(name = "supplier_state_code", nullable = false, length = 2)
    private String supplierStateCode;

    @NotNull
    @Size(max = 6)
    @Column(name = "supplier_pincode", nullable = false, length = 6)
    private String supplierPincode;

    // Recipient Details (Simplified)
    @Size(max = 15)
    @Column(name = "recipient_gstin", length = 15)
    private String recipientGstin;

    @NotNull
    @Size(max = 200)
    @Column(name = "recipient_name", nullable = false, length = 200)
    private String recipientName;

    @NotNull
    @Size(max = 2)
    @Column(name = "recipient_state_code", nullable = false, length = 2)
    private String recipientStateCode;

    @NotNull
    @Size(max = 6)
    @Column(name = "recipient_pincode", nullable = false, length = 6)
    private String recipientPincode;

    // Goods Details (Simplified)
    @Size(max = 10)
    @Column(name = "hsn_code", length = 10)
    private String hsnCode;

    @Column(name = "goods_description", columnDefinition = "TEXT")
    private String goodsDescription;

    @Column(name = "total_quantity", precision = 15, scale = 3)
    @Builder.Default
    private BigDecimal totalQuantity = BigDecimal.ZERO;

    @NotNull
    @Column(name = "total_value", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalValue;

    // Transportation Details (Simplified)
    @Enumerated(EnumType.STRING)
    @Column(name = "transportation_mode", length = 20)
    @Builder.Default
    private TransportationMode transportationMode = TransportationMode.ROAD;

    @Column(name = "transportation_distance")
    private Integer transportationDistance;

    @Size(max = 20)
    @Column(name = "vehicle_number", length = 20)
    private String vehicleNumber;

    @Size(max = 200)
    @Column(name = "transporter_name", length = 200)
    private String transporterName;

    // Validity and Status
    @NotNull
    @Column(name = "valid_from", nullable = false)
    private LocalDateTime validFrom;

    @NotNull
    @Column(name = "valid_until", nullable = false)
    private LocalDateTime validUntil;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    @Builder.Default
    private EWayBillStatus status = EWayBillStatus.ACTIVE;

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
    public boolean isValid() {
        return status == EWayBillStatus.ACTIVE && 
               LocalDateTime.now().isBefore(validUntil);
    }

    public boolean canBeCancelled() {
        return status == EWayBillStatus.ACTIVE;
    }

    public void cancel() {
        this.status = EWayBillStatus.CANCELLED;
    }

    public void markAsExpired() {
        this.status = EWayBillStatus.EXPIRED;
    }
}
