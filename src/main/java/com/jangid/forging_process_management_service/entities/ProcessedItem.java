package com.jangid.forging_process_management_service.entities;

import com.jangid.forging_process_management_service.entities.forging.Forge;
import com.jangid.forging_process_management_service.entities.heating.HeatTreatmentBatch;
import com.jangid.forging_process_management_service.entities.machining.MachiningBatch;
import com.jangid.forging_process_management_service.entities.product.Item;
import com.jangid.forging_process_management_service.entities.product.ItemStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.EntityListeners;
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
@Table(name = "processedItem")
@EntityListeners(AuditingEntityListener.class)
public class ProcessedItem {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO, generator = "processed_item_key_sequence_generator")
  @SequenceGenerator(name = "processed_item_key_sequence_generator", sequenceName = "processed_item_sequence", allocationSize = 1)
  private Long id;

  @OneToOne(mappedBy = "processedItem", fetch = FetchType.LAZY)
  private Forge forge;

  @Column(name = "expected_forge_pieces_count", nullable = false)
  private Integer expectedForgePiecesCount;

  @Column(name = "actual_forge_pieces_count")
  private Integer actualForgePiecesCount;

  @Column(name = "available_forge_pieces_count_for_heat")
  private Integer availableForgePiecesCountForHeat;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "heat_treatment_batch_id")
  private HeatTreatmentBatch heatTreatmentBatch;

  @Column(name = "heat_treat_batch_pieces_count")
  private Integer heatTreatBatchPiecesCount;

  @Column(name = "actual_heat_treat_batch_pieces_count")
  private Integer actualHeatTreatBatchPiecesCount;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "machining_batch_id")
  private MachiningBatch machiningBatch;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "item_id", nullable = false)
  private Item item;

  @Enumerated(EnumType.STRING)
  @Column(name = "item_status", nullable = false)
  private ItemStatus itemStatus;

  @CreatedDate
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Version
  private LocalDateTime updatedAt;

  private LocalDateTime deletedAt;

  private boolean deleted;
}
