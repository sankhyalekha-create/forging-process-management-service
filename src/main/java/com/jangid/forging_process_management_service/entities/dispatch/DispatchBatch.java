package com.jangid.forging_process_management_service.entities.dispatch;

import com.jangid.forging_process_management_service.entities.PackagingType;
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
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
    @Index(name = "idx_dispatch_batch_number", columnList = "dispatch_batch_number"),
    @Index(name = "idx_invoice_number", columnList = "invoice_number")
}
    // Note: Uniqueness for active records handled by partial indexes in database migration V1_52
)
@EntityListeners(AuditingEntityListener.class)
public class DispatchBatch {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO, generator = "dispatch_batch_sequence_generator")
  @SequenceGenerator(name = "dispatch_batch_sequence_generator", sequenceName = "dispatch_batch_sequence", allocationSize = 1)
  private Long id;

  @Column(name = "dispatch_batch_number", nullable = false)
  private String dispatchBatchNumber;
   
  @Column(name = "original_dispatch_batch_number")
  private String originalDispatchBatchNumber;

  @OneToMany(mappedBy = "dispatchBatch", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  private List<DispatchProcessedItemInspection> dispatchProcessedItemInspections = new ArrayList<>();

  @OneToMany(mappedBy = "dispatchBatch", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  private List<DispatchProcessedItemConsumption> dispatchProcessedItemConsumptions = new ArrayList<>();

  @OneToOne(mappedBy = "dispatchBatch", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  private ProcessedItemDispatchBatch processedItemDispatchBatch;

  @OneToMany(mappedBy = "dispatchBatch", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  private List<DispatchPackage> dispatchPackages = new ArrayList<>();

  @Enumerated(EnumType.STRING)
  @Column(name = "dispatch_batch_status", nullable = false)
  private DispatchBatchStatus dispatchBatchStatus;

  @Column(name = "dispatch_created_at")
  private LocalDateTime dispatchCreatedAt;

  @Column(name = "dispatch_ready_at")
  private LocalDateTime dispatchReadyAt;

  @Column(name = "dispatched_at")
  private LocalDateTime dispatchedAt;
  
  @Column(name = "invoice_number")
  private String invoiceNumber;
  
  @Column(name = "invoice_date_time")
  private LocalDateTime invoiceDateTime;
  
  @Column(name = "challan_number")
  private String challanNumber;
  
  @Column(name = "challan_date_time")
  private LocalDateTime challanDateTime;
  
  @Column(name = "order_po_number")
  private String orderPoNumber;
  
  @Column(name = "order_date")
  private LocalDateTime orderDate;

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

  @Enumerated(EnumType.STRING)
  @Column(name = "packaging_type")
  private PackagingType packagingType;

  @Column(name = "packaging_quantity")
  private Integer packagingQuantity;

  @Column(name = "per_packaging_quantity")
  private Integer perPackagingQuantity;

  @Column(name = "use_uniform_packaging")
  private Boolean useUniformPackaging;

  public enum DispatchBatchStatus {
    DISPATCH_IN_PROGRESS,
    READY_TO_DISPATCH,
    INVOICE_DRAFT_CREATED, // Draft invoice created - prevents duplicate invoices for same batch
    DISPATCH_INVOICE_APPROVED, // Invoice approved - batch ready for physical dispatch
    CHALLAN_CREATED,
    DISPATCHED
  }
}

