package com.jangid.forging_process_management_service.entities.vendor;

import com.jangid.forging_process_management_service.entities.inventory.Heat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "vendor_inventory_transaction_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VendorInventoryTransactionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "vendor_inventory_transaction_id", nullable = false)
    private VendorInventoryTransaction vendorInventoryTransaction;

    @ManyToOne
    @JoinColumn(name = "heat_id", nullable = false)
    private Heat heat;

    @Column(name = "quantity_transferred")
    private Double quantityTransferred;

    @Column(name = "pieces_transferred")
    private Integer piecesTransferred;

    @Column(name = "heat_number")
    private String heatNumber;

    @Column(name = "test_certificate_number")
    private String testCertificateNumber;

    @Column(name = "location")
    private String location;

    @Column(name = "is_in_pieces")
    private Boolean isInPieces;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        
        // Copy heat details for historical reference
        if (heat != null) {
            this.heatNumber = heat.getHeatNumber();
            this.testCertificateNumber = heat.getTestCertificateNumber();
            this.location = heat.getLocation();
            this.isInPieces = heat.getIsInPieces();
        }
    }
} 