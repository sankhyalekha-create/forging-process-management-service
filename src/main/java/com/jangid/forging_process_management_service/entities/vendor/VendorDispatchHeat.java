package com.jangid.forging_process_management_service.entities.vendor;

import com.jangid.forging_process_management_service.entities.ConsumptionType;
import com.jangid.forging_process_management_service.entities.inventory.Heat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "vendor_dispatch_heat")
public class VendorDispatchHeat {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "vendor_dispatch_heat_key_sequence_generator")
    @SequenceGenerator(name = "vendor_dispatch_heat_key_sequence_generator", sequenceName = "vendor_dispatch_heat_sequence", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_item_vendor_dispatch_batch_id", nullable = false)
    private ProcessedItemVendorDispatchBatch processedItemVendorDispatchBatch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "heat_id", nullable = false)
    private Heat heat;

    @Enumerated(EnumType.STRING)
    @Column(name = "consumption_type", nullable = false)
    private ConsumptionType consumptionType;

    @Column(name = "quantity_used")
    @DecimalMin(value = "0.0", inclusive = false, message = "Quantity used must be greater than 0")
    private Double quantityUsed;

    @Column(name = "pieces_used")
    @Min(value = 0, message = "Pieces used must be greater than or equal to 0")
    private Integer piecesUsed;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Version
    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;

    @Builder.Default
    private boolean deleted = false;

    @PrePersist
    @PreUpdate
    protected void validateConsumption() {
        if (consumptionType == ConsumptionType.QUANTITY && (quantityUsed == null || quantityUsed <= 0)) {
            throw new IllegalStateException("Quantity must be provided and greater than 0 for QUANTITY consumption type");
        }
        if (consumptionType == ConsumptionType.PIECES && (piecesUsed == null || piecesUsed <= 0)) {
            throw new IllegalStateException("Pieces must be provided and greater than 0 for PIECES consumption type");
        }
        
        // Ensure only one type of consumption is set
        if (consumptionType == ConsumptionType.QUANTITY) {
            piecesUsed = null;
        } else {
            quantityUsed = null;
        }
    }
} 