package com.jangid.forging_process_management_service.entities.vendor;

import com.jangid.forging_process_management_service.entities.PackagingType;
import com.jangid.forging_process_management_service.entities.Tenant;
import com.jangid.forging_process_management_service.entities.forging.ItemWeightType;
import com.jangid.forging_process_management_service.entities.gst.DeliveryChallan;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "vendor_dispatch_batch")
@EntityListeners(AuditingEntityListener.class)
public class VendorDispatchBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "vendor_dispatch_batch_key_sequence_generator")
    @SequenceGenerator(name = "vendor_dispatch_batch_key_sequence_generator", sequenceName = "vendor_dispatch_batch_sequence", allocationSize = 1)
    private Long id;

    @Column(name = "vendor_dispatch_batch_number", nullable = false)
    private String vendorDispatchBatchNumber;

    @Column(name = "original_vendor_dispatch_batch_number")
    private String originalVendorDispatchBatchNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "vendor_dispatch_batch_status", nullable = false)
    private VendorDispatchBatchStatus vendorDispatchBatchStatus;

    @Column(name = "dispatched_at")
    private LocalDateTime dispatchedAt;

    @Column(name = "remarks")
    private String remarks;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Version
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    private boolean deleted;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "billing_entity_id", nullable = false)
    private VendorEntity billingEntity;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "shipping_entity_id", nullable = false)
    private VendorEntity shippingEntity;

    @OneToMany(mappedBy = "vendorDispatchBatch", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<VendorReceiveBatch> vendorReceiveBatches = new ArrayList<>();

    @ElementCollection(targetClass = VendorProcessType.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "vendor_dispatch_batch_processes", joinColumns = @JoinColumn(name = "vendor_dispatch_batch_id"))
    @Column(name = "process_type")
    @Enumerated(EnumType.STRING)
    private List<VendorProcessType> processes = new ArrayList<>();

    @OneToOne(mappedBy = "vendorDispatchBatch", cascade = CascadeType.ALL, orphanRemoval = true)
    private ProcessedItemVendorDispatchBatch processedItem;

    @Enumerated(EnumType.STRING)
    @Column(name = "packaging_type")
    private PackagingType packagingType;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_weight_type")
    private ItemWeightType itemWeightType;

    @Column(name = "packaging_quantity")
    private Integer packagingQuantity;

    @Column(name = "per_packaging_quantity")
    private Integer perPackagingQuantity;

    @Column(name = "use_uniform_packaging")
    private Boolean useUniformPackaging;

    @Column(name = "remaining_pieces")
    private Integer remainingPieces;

    // Challan related fields (similar to how DispatchBatch has invoice related fields)
    @Column(name = "challan_number")
    private String challanNumber;
    
    @Column(name = "challan_date_time")
    private LocalDateTime challanDateTime;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_challan_id")
    private DeliveryChallan deliveryChallan;

    @PrePersist
    protected void onCreate() {
        if (vendorDispatchBatchStatus == null) {
            vendorDispatchBatchStatus = VendorDispatchBatchStatus.READY_TO_DISPATCH;
        }
    }

    public enum VendorDispatchBatchStatus {
        READY_TO_DISPATCH,
        CHALLAN_CREATED,
        DISPATCHED
    }

    /**
     * Add a vendor receive batch to this dispatch batch
     */
    public void addVendorReceiveBatch(VendorReceiveBatch vendorReceiveBatch) {
        vendorReceiveBatches.add(vendorReceiveBatch);
        vendorReceiveBatch.setVendorDispatchBatch(this);
    }

    /**
     * Set the processed item for this dispatch batch
     */
    public void setProcessedItem(ProcessedItemVendorDispatchBatch processedItem) {
        this.processedItem = processedItem;
        if (processedItem != null) {
            processedItem.setVendorDispatchBatch(this);
        }
    }
} 