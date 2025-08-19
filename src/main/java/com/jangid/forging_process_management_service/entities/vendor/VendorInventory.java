package com.jangid.forging_process_management_service.entities.vendor;

import com.jangid.forging_process_management_service.entities.inventory.Heat;
import com.jangid.forging_process_management_service.entities.inventory.RawMaterialProduct;

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
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * Entity to track inventory dispatched to vendors but not yet consumed by them.
 * This provides clear separation between tenant inventory (Heat) and vendor inventory.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "vendor_inventory")
@Table(name = "vendor_inventory"
       // Note: Uniqueness for active records handled by partial index in database migration V1_52
)
@EntityListeners(AuditingEntityListener.class)
public class VendorInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "vendor_inventory_key_sequence_generator")
    @SequenceGenerator(name = "vendor_inventory_key_sequence_generator", sequenceName = "vendor_inventory_sequence", allocationSize = 1)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_heat_id", nullable = false)
    private Heat originalHeat;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "raw_material_product_id", nullable = false)
    private RawMaterialProduct rawMaterialProduct;

    // Inventory tracking fields
    @Column(name = "heat_number", nullable = false)
    private String heatNumber; // Copy from original heat for reference

    @Column(name = "total_dispatched_quantity")
    private Double totalDispatchedQuantity; // Total quantity originally dispatched

    @Column(name = "available_quantity")
    private Double availableQuantity; // Current available quantity at vendor

    @Column(name = "is_in_pieces", nullable = false)
    private Boolean isInPieces;

    @Column(name = "total_dispatched_pieces")
    private Integer totalDispatchedPieces; // Total pieces originally dispatched

    @Column(name = "available_pieces_count")
    private Integer availablePiecesCount; // Current available pieces at vendor

    @Column(name = "test_certificate_number")
    private String testCertificateNumber; // Copy from original heat

    // Audit fields
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Version
    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;

    private boolean deleted;


    /**
     * Returns the available quantity based on the unit of measurement
     */
    public Double getAvailableQuantity() {
        if (isInPieces) {
            return (double) availablePiecesCount;
        } else {
            return availableQuantity;
        }
    }

    /**
     * Returns the total dispatched quantity based on the unit of measurement
     */
    public Double getTotalDispatchedQuantity() {
        if (isInPieces) {
            return (double) totalDispatchedPieces;
        } else {
            return totalDispatchedQuantity;
        }
    }
}