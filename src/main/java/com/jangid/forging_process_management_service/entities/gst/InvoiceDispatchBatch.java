package com.jangid.forging_process_management_service.entities.gst;

import com.jangid.forging_process_management_service.entities.Tenant;
import com.jangid.forging_process_management_service.entities.dispatch.DispatchBatch;

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
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * Junction entity representing the many-to-many relationship between Invoice and DispatchBatch.
 * Allows a single invoice to be associated with multiple dispatch batches.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "invoice_dispatch_batch", indexes = {
  @Index(name = "idx_invoice_dispatch_invoice", columnList = "invoice_id"),
  @Index(name = "idx_invoice_dispatch_batch", columnList = "dispatch_batch_id"),
  @Index(name = "idx_invoice_dispatch_primary", columnList = "is_primary"),
  @Index(name = "idx_invoice_dispatch_deleted", columnList = "deleted")
})
@EntityListeners(AuditingEntityListener.class)
public class InvoiceDispatchBatch {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO, generator = "invoice_dispatch_batch_sequence_generator")
  @SequenceGenerator(name = "invoice_dispatch_batch_sequence_generator",
    sequenceName = "invoice_dispatch_batch_sequence", allocationSize = 1)
  private Long id;

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "invoice_id", nullable = false)
  private Invoice invoice;

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "dispatch_batch_id", nullable = false)
  private DispatchBatch dispatchBatch;

  /**
   * Sequence order of dispatch batches in the invoice (for display purposes).
   */
  @Column(name = "sequence_order")
  private Integer sequenceOrder;

  @NotNull
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "tenant_id", nullable = false)
  private Tenant tenant;

  @CreatedDate
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Version
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  @Builder.Default
  private boolean deleted = false;
}

