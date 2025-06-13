package com.jangid.forging_process_management_service.entities.quality;

import com.jangid.forging_process_management_service.entities.Tenant;
import com.jangid.forging_process_management_service.entities.machining.ProcessedItemMachiningBatch;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "inspection_batch", indexes = {
    @Index(name = "idx_inspection_batch_number", columnList = "inspection_batch_number")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uq_inspection_batch_number_tenant_deleted", columnNames = {"inspection_batch_number", "tenant_id", "deleted"})
})
@EntityListeners(AuditingEntityListener.class)
  public class InspectionBatch {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO, generator = "inspection_batch_sequence_generator")
  @SequenceGenerator(name = "inspection_batch_sequence_generator", sequenceName = "inspection_batch_sequence", allocationSize = 1)
  private Long id;

  @Column(name = "inspection_batch_number", nullable = false)
  private String inspectionBatchNumber;
  
  @Column(name = "original_inspection_batch_number")
  private String originalInspectionBatchNumber;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "input_processed_item_machining_batch_id")
  private ProcessedItemMachiningBatch inputProcessedItemMachiningBatch;

  @OneToOne(mappedBy = "inspectionBatch", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  private ProcessedItemInspectionBatch processedItemInspectionBatch;

  @Enumerated(EnumType.STRING)
  @Column(name = "inspection_batch_status", nullable = false)
  private InspectionBatchStatus inspectionBatchStatus;

  @Column(name = "start_at")
  private LocalDateTime startAt;

  @Column(name = "end_at")
  private LocalDateTime endAt;

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

  public enum InspectionBatchStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED
  }
}

