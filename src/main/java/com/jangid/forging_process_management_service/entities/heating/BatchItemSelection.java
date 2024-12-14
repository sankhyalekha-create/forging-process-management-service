package com.jangid.forging_process_management_service.entities.heating;

import com.jangid.forging_process_management_service.entities.forging.Forge;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
@Table(name = "batch_item_selection")
public class BatchItemSelection {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO, generator = "batch_item_selection_key_sequence_generator")
  @SequenceGenerator(name = "batch_item_selection_key_sequence_generator", sequenceName = "batch_item_selection_sequence", allocationSize = 1)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "forge_id", nullable = false)
  private Forge forge;

  @Column(name = "available_forged_pieces_count")
  private Integer availableForgedPiecesCount;

  @Column(name = "heat_treat_batch_pieces_count")
  private Integer heatTreatBatchPiecesCount;

  @Column(name = "actual_heat_treat_batch_pieces_count")
  private Integer actualHeatTreatBatchPiecesCount;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "heat_treatment_batch_id", nullable = false)
  private HeatTreatmentBatch heatTreatmentBatch;

  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Version
  private LocalDateTime updatedAt;

  private LocalDateTime deletedAt;

  private boolean deleted;
}
