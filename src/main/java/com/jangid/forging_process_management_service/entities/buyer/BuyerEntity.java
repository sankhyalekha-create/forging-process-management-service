package com.jangid.forging_process_management_service.entities.buyer;

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
@Table(name = "buyer_entity", indexes = {
    @Index(name = "idx_buyer_entity_name", columnList = "buyer_entity_name")
    // Note: Uniqueness for active records handled by partial index in database migration V1_52
})
public class BuyerEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String buyerEntityName;

    @Column
    private String address;

    @Column(name = "gstin_uin")
    private String gstinUin;

    @Column(name = "phone_number", length = 15)
    private String phoneNumber;

    @Column(name = "email")
    private String email;

    @Column(name = "pan_number", length = 10)
    private String panNumber;

    @Column(name = "state_code", length = 2)
    private String stateCode;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "pincode", length = 6)
    private String pincode;

    @Column(name = "is_billing_entity")
    private boolean isBillingEntity;

    @Column(name = "is_shipping_entity")
    private boolean isShippingEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private Buyer buyer;

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