package com.jangid.forging_process_management_service.entities.vendor;

import com.jangid.forging_process_management_service.entities.PackagingType;
import com.jangid.forging_process_management_service.entities.Tenant;

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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "vendor_receive_batch")
@EntityListeners(AuditingEntityListener.class)
public class VendorReceiveBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "vendor_receive_batch_key_sequence_generator")
    @SequenceGenerator(name = "vendor_receive_batch_key_sequence_generator", sequenceName = "vendor_receive_batch_sequence", allocationSize = 1)
    private Long id;

    @Column(name = "vendor_receive_batch_number", nullable = false)
    private String vendorReceiveBatchNumber;

    @Column(name = "original_vendor_receive_batch_number")
    private String originalVendorReceiveBatchNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "vendor_receive_batch_status", nullable = false)
    private VendorReceiveBatchStatus vendorReceiveBatchStatus;

    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    @Column(name = "is_in_pieces", nullable = false)
    private Boolean isInPieces;

    @Column(name = "received_pieces_count")
    private Integer receivedPiecesCount;

    @Column(name = "rejected_pieces_count")
    private Integer rejectedPiecesCount;

    @Column(name = "tenant_rejects_count")
    private Integer tenantRejectsCount;

    @Column(name = "pieces_eligible_for_next_operation")
    private Integer piecesEligibleForNextOperation;

    @Column(name = "quality_check_required")
    private Boolean qualityCheckRequired;

    @Column(name = "quality_check_completed")
    private Boolean qualityCheckCompleted;

    @Column(name = "remarks")
    private String remarks;

    // Quality Check Completion Fields
    @Column(name = "quality_check_completed_at")
    private LocalDateTime qualityCheckCompletedAt;

    @Column(name = "final_vendor_rejects_count")
    private Integer finalVendorRejectsCount;

    @Column(name = "final_tenant_rejects_count") 
    private Integer finalTenantRejectsCount;

    @Column(name = "quality_check_remarks")
    private String qualityCheckRemarks;

    @Column(name = "is_locked", nullable = false)
    @Builder.Default
    private Boolean isLocked = false;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_dispatch_batch_id")
    private VendorDispatchBatch vendorDispatchBatch;

    @Enumerated(EnumType.STRING)
    @Column(name = "packaging_type")
    private PackagingType packagingType;

    @Column(name = "packaging_quantity")
    private Integer packagingQuantity;

    @Column(name = "per_packaging_quantity")
    private Integer perPackagingQuantity;

    @Column(name = "use_uniform_packaging")
    private Boolean useUniformPackaging;

    @Column(name = "remaining_pieces")
    private Integer remainingPieces;

    public enum VendorReceiveBatchStatus {
        RECEIVED,
        QUALITY_CHECK_PENDING,
        QUALITY_CHECK_DONE
    }

    /**
     * Complete quality check for this receive batch
     */
    public void completeQualityCheck(Integer finalVendorRejects, Integer finalTenantRejects, String qualityCheckRemarks) {
        if (this.isLocked) {
            throw new IllegalStateException("Cannot modify a locked vendor receive batch");
        }
        
        if (!Boolean.TRUE.equals(this.qualityCheckRequired)) {
            throw new IllegalStateException("Quality check was not required for this batch");
        }
        
        if (Boolean.TRUE.equals(this.qualityCheckCompleted)) {
            throw new IllegalStateException("Quality check has already been completed for this batch");
        }
        
        this.qualityCheckCompleted = true;
        this.qualityCheckRequired = false;
        this.qualityCheckCompletedAt = LocalDateTime.now();
        this.finalVendorRejectsCount = finalVendorRejects != null ? finalVendorRejects : 0;
        this.finalTenantRejectsCount = finalTenantRejects != null ? finalTenantRejects : 0;
        this.qualityCheckRemarks = qualityCheckRemarks;
        this.isLocked = true;
        this.vendorReceiveBatchStatus = VendorReceiveBatchStatus.QUALITY_CHECK_DONE;
    }

    /**
     * Check if this batch can be modified
     */
    public boolean canBeModified() {
        return !Boolean.TRUE.equals(this.isLocked);
    }

    /**
     * Get total final rejects count
     */
    public Integer getTotalFinalRejectsCount() {
        int vendorRejects = this.finalVendorRejectsCount != null ? this.finalVendorRejectsCount : 0;
        int tenantRejects = this.finalTenantRejectsCount != null ? this.finalTenantRejectsCount : 0;
        return vendorRejects + tenantRejects;
    }

    /**
     * Check if quality check is pending
     */
    public boolean isQualityCheckPending() {
        return Boolean.TRUE.equals(this.qualityCheckRequired) && !Boolean.TRUE.equals(this.qualityCheckCompleted);
    }

}