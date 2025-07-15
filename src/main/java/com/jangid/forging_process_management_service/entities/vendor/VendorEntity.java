package com.jangid.forging_process_management_service.entities.vendor;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "vendor_entity", indexes = {
    @Index(name = "idx_vendor_entity_name", columnList = "vendor_entity_name")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uq_vendor_entity_name_tenant_deleted", columnNames = {"vendor_entity_name", "tenant_id", "deleted"})
})
public class VendorEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String vendorEntityName;

    @Column
    private String address;

    @Column(name = "gstin_uin")
    private String gstinUin;

    @Column(name = "phone_number", length = 15)
    private String phoneNumber;

    @Column(name = "pan_number", length = 10)
    private String panNumber;

    @Column(name = "is_billing_entity")
    private boolean isBillingEntity;

    @Column(name = "is_shipping_entity")
    private boolean isShippingEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

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
} 