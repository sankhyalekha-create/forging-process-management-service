package com.jangid.forging_process_management_service.entities.buyer;

import com.jangid.forging_process_management_service.entities.Tenant;
import com.jangid.forging_process_management_service.entities.dispatch.DispatchBatch;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "buyer", indexes = {
    @Index(name = "idx_buyer_name", columnList = "buyer_name")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uq_buyer_name_tenant_deleted", columnNames = {"buyer_name", "tenant_id", "deleted"})
})
public class Buyer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String buyerName;

    @Column
    private String address;

    @Column(name = "gstin_uin")
    private String gstinUin;

    @Column(name = "phone_number", length = 15)
    private String phoneNumber;

    @OneToMany(mappedBy = "buyer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BuyerEntity> entities = new ArrayList<>();

    @OneToMany(mappedBy = "buyer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DispatchBatch> dispatchBatches = new ArrayList<>();

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

    public void addEntity(BuyerEntity entity) {
        entities.add(entity);
        entity.setBuyer(this);
    }

    public void removeEntity(BuyerEntity entity) {
        entities.remove(entity);
        entity.setBuyer(null);
    }
} 