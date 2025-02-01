package com.jangid.forging_process_management_service.entities.quality;

import com.jangid.forging_process_management_service.entities.ProcessedItem;
import com.jangid.forging_process_management_service.entities.dispatch.DispatchBatch;
import com.jangid.forging_process_management_service.entities.product.ItemStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "processed_item_inspection_batch")
public class ProcessedItemInspectionBatch {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO, generator = "processed_item_inspection_batch_sequence_generator")
  @SequenceGenerator(name = "processed_item_inspection_batch_sequence_generator", sequenceName = "processed_item_inspection_batch_sequence", allocationSize = 1)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "inspection_batch_id", nullable = false)
  private InspectionBatch inspectionBatch;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "processed_item_id", nullable = false)
  private ProcessedItem processedItem;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "dispatch_batch_id")
  private DispatchBatch dispatchBatch;

  @OneToMany(mappedBy = "processedItemInspectionBatch", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  private List<GaugeInspectionReport> gaugeInspectionReports = new ArrayList<>();

  @Column(name = "inspection_batch_pieces_count", nullable = false)
  private Integer inspectionBatchPiecesCount;

  @Column(name = "available_inspection_batch_pieces_count")
  private Integer availableInspectionBatchPiecesCount;

  @Column(name = "finished_inspection_batch_pieces_count")
  private Integer finishedInspectionBatchPiecesCount;

  @Column(name = "reject_inspection_batch_pieces_count")
  private Integer rejectInspectionBatchPiecesCount;

  @Column(name = "rework_pieces_count")
  private Integer reworkPiecesCount;

  @Column(name = "available_dispatch_pieces_count")
  private Integer availableDispatchPiecesCount;

  @Column(name = "dispatched_pieces_count")
  private Integer dispatchedPiecesCount;

  @Column(name = "item_status", nullable = false)
  private ItemStatus itemStatus;

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

  public void updatePieceCounts() {
    this.finishedInspectionBatchPiecesCount = gaugeInspectionReports.stream()
        .mapToInt(GaugeInspectionReport::getFinishedPiecesCount)
        .min()
        .orElse(0); // Default to 0 if no reports exist

    this.rejectInspectionBatchPiecesCount = gaugeInspectionReports.stream()
        .mapToInt(GaugeInspectionReport::getRejectedPiecesCount)
        .sum();

    this.reworkPiecesCount = gaugeInspectionReports.stream()
        .mapToInt(GaugeInspectionReport::getReworkPiecesCount)
        .sum();

    this.availableDispatchPiecesCount = this.finishedInspectionBatchPiecesCount;
  }

}
