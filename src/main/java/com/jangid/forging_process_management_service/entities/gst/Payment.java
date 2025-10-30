package com.jangid.forging_process_management_service.entities.gst;

import com.jangid.forging_process_management_service.entities.Tenant;

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
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payment entity representing a payment transaction against an invoice.
 * Supports partial payments, TDS tracking, and payment history.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "payment", indexes = {
  @Index(name = "idx_payment_invoice", columnList = "invoice_id"),
  @Index(name = "idx_payment_date_time", columnList = "payment_date_time"),
  @Index(name = "idx_payment_status", columnList = "status"),
  @Index(name = "idx_payment_tenant", columnList = "tenant_id"),
  @Index(name = "idx_payment_deleted", columnList = "deleted")
})
@EntityListeners(AuditingEntityListener.class)
public class Payment {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO, generator = "payment_sequence_generator")
  @SequenceGenerator(name = "payment_sequence_generator",
    sequenceName = "payment_sequence", allocationSize = 1)
  private Long id;

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "invoice_id", nullable = false)
  private Invoice invoice;

  @NotNull
  @Column(name = "amount", nullable = false, precision = 15, scale = 2)
  private BigDecimal amount;

  @NotNull
  @Column(name = "payment_date_time", nullable = false)
  private LocalDateTime paymentDateTime;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "payment_method", nullable = false, length = 20)
  private PaymentMethod paymentMethod;

  @Size(max = 100)
  @Column(name = "payment_reference", length = 100)
  private String paymentReference;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  @Builder.Default
  private PaymentStatus status = PaymentStatus.RECEIVED;

  @Size(max = 100)
  @Column(name = "recorded_by", length = 100)
  private String recordedBy;

  @Size(max = 500)
  @Column(name = "payment_proof_path", length = 500)
  private String paymentProofPath;

  // TDS (Tax Deducted at Source) tracking - common in Indian B2B
  @Column(name = "tds_amount", precision = 15, scale = 2)
  @Builder.Default
  private BigDecimal tdsAmount = BigDecimal.ZERO;

  @Size(max = 100)
  @Column(name = "tds_reference", length = 100)
  private String tdsReference;

  @Size(max = 1000)
  @Column(name = "notes", length = 1000)
  private String notes;

  // Audit and tracking fields
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

  // Business methods
  public boolean isReceived() {
    return status == PaymentStatus.RECEIVED;
  }

  public boolean isReversed() {
    return status == PaymentStatus.REVERSED;
  }

  public boolean isPendingClearance() {
    return status == PaymentStatus.PENDING_CLEARANCE;
  }

  public BigDecimal getEffectiveAmount() {
    // Effective amount = Payment amount - TDS (if any)
    if (tdsAmount != null && tdsAmount.compareTo(BigDecimal.ZERO) > 0) {
      return amount.subtract(tdsAmount);
    }
    return amount;
  }
}

