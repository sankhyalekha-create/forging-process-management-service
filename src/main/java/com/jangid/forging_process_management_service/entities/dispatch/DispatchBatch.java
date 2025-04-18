package com.jangid.forging_process_management_service.entities.dispatch;

import com.jangid.forging_process_management_service.entities.Tenant;
import com.jangid.forging_process_management_service.entities.buyer.Buyer;
import com.jangid.forging_process_management_service.entities.buyer.BuyerEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
@Table(name = "dispatch_batch", indexes = {
    @Index(name = "idx_dispatch_batch_number", columnList = "dispatch_batch_number")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uq_dispatch_batch_number_tenant_deleted", columnNames = {"dispatch_batch_number", "tenant_id", "deleted"})
})
@EntityListeners(AuditingEntityListener.class)
public class DispatchBatch {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO, generator = "dispatch_batch_sequence_generator")
  @SequenceGenerator(name = "dispatch_batch_sequence_generator", sequenceName = "dispatch_batch_sequence", allocationSize = 1)
  private Long id;

  @Column(name = "dispatch_batch_number", nullable = false)
  private String dispatchBatchNumber;

  @OneToMany(mappedBy = "dispatchBatch", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  private List<DispatchProcessedItemInspection> dispatchProcessedItemInspections = new ArrayList<>();

  @OneToOne(mappedBy = "dispatchBatch", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  private ProcessedItemDispatchBatch processedItemDispatchBatch;

  @Column(name = "dispatch_batch_status", nullable = false)
  private DispatchBatchStatus dispatchBatchStatus;

  @Column(name = "dispatch_created_at")
  private LocalDateTime dispatchCreatedAt;

  @Column(name = "dispatch_ready_at")
  private LocalDateTime dispatchReadyAt;

  @Column(name = "dispatched_at")
  private LocalDateTime dispatchedAt;

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
  @JoinColumn(name = "buyer_id", nullable = false)
  private Buyer buyer;

  @NotNull
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "billing_entity_id", nullable = false)
  private BuyerEntity billingEntity;

  @NotNull
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "shipping_entity_id", nullable = false)
  private BuyerEntity shippingEntity;

  @Column(name = "packaging_type")
  private PackagingType packagingType;

  @Column(name = "packaging_quantity")
  private Integer packagingQuantity;

  @Column(name = "per_packaging_quantity")
  private Integer perPackagingQuantity;

  public enum DispatchBatchStatus {
    DISPATCH_IN_PROGRESS,
    READY_TO_DISPATCH,
    DISPATCHED
  }

  public enum PackagingType {
    Box,
    Bag,
    Jaal,
    Jaali,
    Loose
  }
}

