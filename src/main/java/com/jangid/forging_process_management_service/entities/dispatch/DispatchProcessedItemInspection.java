package com.jangid.forging_process_management_service.entities.dispatch;

import com.jangid.forging_process_management_service.entities.quality.ProcessedItemInspectionBatch;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "dispatch_processed_item_inspection")
public class DispatchProcessedItemInspection {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO, generator = "dispatch_processed_item_inspection_sequence_generator")
  @SequenceGenerator(name = "dispatch_processed_item_inspection_sequence_generator", sequenceName = "dispatch_processed_item_inspection_sequence", allocationSize = 1)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "dispatch_batch_id", nullable = false)
  private DispatchBatch dispatchBatch;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "processed_item_inspection_batch_id", nullable = false)
  private ProcessedItemInspectionBatch processedItemInspectionBatch;

  @Column(name = "dispatched_pieces_count", nullable = false)
  private Integer dispatchedPiecesCount;

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

