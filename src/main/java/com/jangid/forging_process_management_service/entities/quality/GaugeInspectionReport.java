package com.jangid.forging_process_management_service.entities.quality;

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
@Table(name = "gauge_inspection_report")
public class GaugeInspectionReport {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO, generator = "gauge_inspection_report_sequence_generator")
  @SequenceGenerator(name = "gauge_inspection_report_sequence_generator", sequenceName = "gauge_inspection_report_sequence", allocationSize = 1)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "processed_item_inspection_batch_id", nullable = false)
  private ProcessedItemInspectionBatch processedItemInspectionBatch;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "gauge_id", nullable = false)
  private Gauge gauge;

  @Column(name = "finished_pieces_count", nullable = false)
  private Integer finishedPiecesCount;

  @Column(name = "rejected_pieces_count", nullable = false)
  private Integer rejectedPiecesCount;

  @Column(name = "rework_pieces_count", nullable = false)
  private Integer reworkPiecesCount;

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

