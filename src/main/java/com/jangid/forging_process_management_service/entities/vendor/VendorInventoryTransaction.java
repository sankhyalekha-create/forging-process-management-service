package com.jangid.forging_process_management_service.entities.vendor;

import com.jangid.forging_process_management_service.entities.Tenant;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "vendor_inventory_transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VendorInventoryTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    private VendorInventoryTransactionType transactionType;

    @Column(name = "transaction_date_time", nullable = false)
    private LocalDateTime transactionDateTime;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "total_quantity_transferred")
    private Double totalQuantityTransferred;

    @Column(name = "total_pieces_transferred")
    private Integer totalPiecesTransferred;

    @OneToMany(mappedBy = "vendorInventoryTransaction", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<VendorInventoryTransactionItem> transactionItems;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (transactionDateTime == null) {
            transactionDateTime = LocalDateTime.now();
        }
    }

    public enum VendorInventoryTransactionType {
        TRANSFER_TO_VENDOR,
        RETURN_FROM_VENDOR
    }
} 