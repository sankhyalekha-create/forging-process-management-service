package com.jangid.forging_process_management_service.entities;

import com.jangid.forging_process_management_service.entities.forging.Forge;
import com.jangid.forging_process_management_service.entities.heating.ProcessedItemHeatTreatmentBatch;
import com.jangid.forging_process_management_service.entities.machining.ProcessedItemMachiningBatch;
import com.jangid.forging_process_management_service.entities.product.Item;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
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
import java.util.ArrayList;
import java.util.List;

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

  @Column(name = "rejected_forge_pieces_count")
  @Builder.Default
  private Integer rejectedForgePiecesCount = 0;

  @Column(name = "other_forge_rejections_kg")
  @Builder.Default
  private Double otherForgeRejectionsKg = 0.0;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "item_id", nullable = false)
  private Item item;

  @OneToMany(mappedBy = "processedItem", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<ProcessedItemHeatTreatmentBatch> processedItemHeatTreatmentBatches = new ArrayList<>();

  @OneToMany(mappedBy = "processedItem", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<ProcessedItemMachiningBatch> processedItemMachiningBatches = new ArrayList<>();

  @CreatedDate
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Version
  private LocalDateTime updatedAt;

  private LocalDateTime deletedAt;

  private boolean deleted;
}
