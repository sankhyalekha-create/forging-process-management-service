package com.jangid.forging_process_management_service.entities.gst;

import com.jangid.forging_process_management_service.entities.Tenant;
import com.jangid.forging_process_management_service.entities.vendor.VendorDispatchBatch;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Index;

import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "challan_vendor_dispatch_batch", indexes = {
    @Index(name = "idx_challan_vendor_dispatch_batch_challan", columnList = "delivery_challan_id"),
    @Index(name = "idx_challan_vendor_dispatch_batch_vendor_dispatch", columnList = "vendor_dispatch_batch_id"),
    @Index(name = "idx_challan_vendor_dispatch_batch_deleted", columnList = "deleted")
})
@EntityListeners(AuditingEntityListener.class)
public class ChallanVendorDispatchBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "challan_vendor_dispatch_batch_sequence_generator")
    @SequenceGenerator(name = "challan_vendor_dispatch_batch_sequence_generator",
                    sequenceName = "challan_vendor_dispatch_batch_sequence", allocationSize = 1)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_challan_id", nullable = false)
    private DeliveryChallan deliveryChallan;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_dispatch_batch_id", nullable = false)
    private VendorDispatchBatch vendorDispatchBatch;

    // Standard Audit Fields
    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    private boolean deleted;
}
