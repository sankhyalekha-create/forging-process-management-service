package com.jangid.forging_process_management_service.entities.quality;

import com.jangid.forging_process_management_service.entities.machining.DailyMachiningBatch;

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
@Table(name = "daily_machining_batch_inspection_distribution")
@EntityListeners(AuditingEntityListener.class)
public class DailyMachiningBatchInspectionDistribution {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO, generator = "daily_machining_batch_inspection_distribution_sequence_generator")
  @SequenceGenerator(name = "daily_machining_batch_inspection_distribution_sequence_generator", 
                     sequenceName = "daily_machining_batch_inspection_distribution_sequence", allocationSize = 1)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "processed_item_inspection_batch_id", nullable = false)
  private ProcessedItemInspectionBatch processedItemInspectionBatch;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "daily_machining_batch_id", nullable = false)
  private DailyMachiningBatch dailyMachiningBatch;

  @Column(name = "rejected_pieces_count", nullable = false)
  private Integer rejectedPiecesCount;

  @Column(name = "rework_pieces_count", nullable = false)
  private Integer reworkPiecesCount;

  @Column(name = "original_completed_pieces_count", nullable = false)
  private Integer originalCompletedPiecesCount;

  @CreatedDate
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Version
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  @Column(name = "deleted")
  private boolean deleted;
} 