package com.jangid.forging_process_management_service.entities.heating;

import com.jangid.forging_process_management_service.entities.ProcessedItem;
import com.jangid.forging_process_management_service.entities.product.ItemStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;
import jakarta.persistence.Version;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "processed_item_heat_treatment_batch")
public class ProcessedItemHeatTreatmentBatch {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO, generator = "processed_item_ht_batch_sequence_generator")
  @SequenceGenerator(name = "processed_item_ht_batch_sequence_generator", sequenceName = "processed_item_ht_batch_sequence", allocationSize = 1)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "processed_item_id", nullable = false)
  private ProcessedItem processedItem;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "heat_treatment_batch_id", nullable = false)
  private HeatTreatmentBatch heatTreatmentBatch;

  @Column(name = "item_status", nullable = false)
  private ItemStatus itemStatus;

  @Column(name = "heat_treat_batch_pieces_count", nullable = false)
  private Integer heatTreatBatchPiecesCount;

  @Column(name = "actual_heat_treat_batch_pieces_count")
  private Integer actualHeatTreatBatchPiecesCount;

  @Column(name = "initial_machining_batch_pieces_count")
  private Integer initialMachiningBatchPiecesCount;

  @Column(name = "available_machining_batch_pieces_count")
  private Integer availableMachiningBatchPiecesCount;

  @CreatedDate
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Version
  private LocalDateTime updatedAt;

  private LocalDateTime deletedAt;

  private boolean deleted;

  public boolean hasSufficientPiecesForMachining(int requiredPieces) {
    return this.availableMachiningBatchPiecesCount >= requiredPieces;
  }
}

