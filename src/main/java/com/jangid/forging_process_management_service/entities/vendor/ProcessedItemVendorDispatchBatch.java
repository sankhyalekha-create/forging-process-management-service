package com.jangid.forging_process_management_service.entities.vendor;

import com.jangid.forging_process_management_service.entities.product.Item;
import com.jangid.forging_process_management_service.entities.product.ItemStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;
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
@Table(name = "processed_item_vendor_dispatch_batch")
@EntityListeners(AuditingEntityListener.class)
public class ProcessedItemVendorDispatchBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "processed_item_vendor_dispatch_batch_sequence")
    @SequenceGenerator(name = "processed_item_vendor_dispatch_batch_sequence", sequenceName = "processed_item_vendor_dispatch_batch_sequence", allocationSize = 1)
    private Long id;

    @NotNull
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_dispatch_batch_id", nullable = false)
    private VendorDispatchBatch vendorDispatchBatch;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_status", nullable = false)
    @Builder.Default
    private ItemStatus itemStatus = ItemStatus.VENDOR_DISPATCH_COMPLETED;

    // Workflow Integration Fields
    @Column(name = "workflow_identifier")
    private String workflowIdentifier;

    @Column(name = "item_workflow_id")
    private Long itemWorkflowId;

    @Column(name = "previous_operation_processed_item_id")
    private Long previousOperationProcessedItemId;

    // Heat Tracking - Track which heats are consumed during vendor dispatch
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "processed_item_vendor_dispatch_batch_id")
    @Builder.Default
    private List<VendorDispatchHeat> vendorDispatchHeats = new ArrayList<>();

    // Simple Dispatch Tracking - Just what was sent to vendor
    @Column(name = "is_in_pieces", nullable = false)
    @Builder.Default
    private Boolean isInPieces = false;

    @Column(name = "dispatched_pieces_count")
    private Integer dispatchedPiecesCount;

    @Column(name = "dispatched_quantity")
    private Double dispatchedQuantity;

    @Column(name = "total_expected_pieces_count")
    @Builder.Default
    private Integer totalExpectedPiecesCount = 0;

    // Total Received Tracking - Track cumulative received quantities across all VendorReceiveBatch
    @Column(name = "total_received_pieces_count")
    @Builder.Default
    private Integer totalReceivedPiecesCount = 0;

    // Total Rejected Tracking - Track cumulative rejected quantities across all VendorReceiveBatch
    @Column(name = "total_rejected_pieces_count")
    @Builder.Default
    private Integer totalRejectedPiecesCount = 0;

    // Total Tenant Rejects Tracking - Track cumulative tenant rejects across all VendorReceiveBatch
    @Column(name = "total_tenant_rejects_count")
    @Builder.Default
    private Integer totalTenantRejectsCount = 0;

    // Total Pieces Eligible for Next Operation
    @Column(name = "total_pieces_eligible_for_next_operation")
    @Builder.Default
    private Integer totalPiecesEligibleForNextOperation = 0;

    // Completion Status
    @Column(name = "fully_received")
    @Builder.Default
    private Boolean fullyReceived = false;

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted", nullable = false)
    @Builder.Default
    private Boolean deleted = false;

    // Helper methods for workflow integration
    
    /**
     * Marks the processed item as dispatched to vendor
     */
    public void markAsDispatched() {
        this.itemStatus = ItemStatus.VENDOR_DISPATCH_COMPLETED;
    }

    /**
     * Set the vendor dispatch batch for this processed item
     */
    public void setVendorDispatchBatch(VendorDispatchBatch vendorDispatchBatch) {
        this.vendorDispatchBatch = vendorDispatchBatch;
        if (vendorDispatchBatch != null && vendorDispatchBatch.getProcessedItem() != this) {
            vendorDispatchBatch.setProcessedItem(this);
        }
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
} 