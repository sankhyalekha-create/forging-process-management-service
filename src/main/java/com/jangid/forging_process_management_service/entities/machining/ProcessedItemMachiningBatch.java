package com.jangid.forging_process_management_service.entities.machining;

import com.jangid.forging_process_management_service.entities.ProcessedItem;
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
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "processed_item_machining_batch")
public class ProcessedItemMachiningBatch {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO, generator = "processed_item_machining_batch_sequence_generator")
  @SequenceGenerator(name = "processed_item_machining_batch_sequence_generator", sequenceName = "processed_item_machining_batch_sequence", allocationSize = 1)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "processed_item_id")
  private ProcessedItem processedItem;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "machining_batch_id", nullable = false)
  private MachiningBatch machiningBatch;

  @OneToMany(mappedBy = "inputProcessedItemMachiningBatch", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private List<MachiningBatch> machiningBatchesForRework = new ArrayList<>();

  @Enumerated(EnumType.STRING)
  @Column(name = "item_status", nullable = false)
  private ItemStatus itemStatus;

  @Column(name = "machining_batch_pieces_count", nullable = false)
  private Integer machiningBatchPiecesCount;

  @Column(name = "available_machining_batch_pieces_count")
  private Integer availableMachiningBatchPiecesCount;

  @Column(name = "actual_machining_batch_pieces_count")
  private Integer actualMachiningBatchPiecesCount;

  @Column(name = "reject_machining_batch_pieces_count")
  private Integer rejectMachiningBatchPiecesCount;

  @Column(name = "rework_pieces_count")
  private Integer reworkPiecesCount;

  @Column(name = "rework_pieces_count_available_for_rework")
  private Integer reworkPiecesCountAvailableForRework;

  @Column(name = "initial_inspection_batch_pieces_count")
  private Integer initialInspectionBatchPiecesCount;

  @Column(name = "available_inspection_batch_pieces_count")
  private Integer availableInspectionBatchPiecesCount;

  @CreatedDate
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Version
  private LocalDateTime updatedAt;

  private LocalDateTime deletedAt;

  private boolean deleted;

}
