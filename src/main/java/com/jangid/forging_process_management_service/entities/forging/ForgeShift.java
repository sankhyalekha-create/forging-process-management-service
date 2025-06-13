package com.jangid.forging_process_management_service.entities.forging;

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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
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
@EntityListeners(AuditingEntityListener.class)
@Table(name = "forge_shift")
public class ForgeShift {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO, generator = "forge_shift_key_sequence_generator")
  @SequenceGenerator(name = "forge_shift_key_sequence_generator", sequenceName = "forge_shift_sequence", allocationSize = 1)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "forge_id", nullable = false)
  private Forge forge;

  @Column(name = "item_workflow_id")
  private Long itemWorkflowId;

  @Column(name = "start_date_time", nullable = false)
  private LocalDateTime startDateTime;

  @Column(name = "end_date_time", nullable = false)
  private LocalDateTime endDateTime;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "forge_shift_id")
  @Builder.Default
  private List<ForgeShiftHeat> forgeShiftHeats = new ArrayList<>();

  @Column(name = "actual_forged_pieces_count")
  private Integer actualForgedPiecesCount;

  @Column(name = "rejected_forge_pieces_count")
  @Builder.Default
  private Integer rejectedForgePiecesCount = 0;

  @Column(name = "other_forge_rejections_kg")
  @Builder.Default
  private Double otherForgeRejectionsKg = 0.0;

  @Column(name = "rejection")
  @Builder.Default
  private Boolean rejection = false;

  @CreatedDate
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Version
  private LocalDateTime updatedAt;

  private LocalDateTime deletedAt;

  private boolean deleted;

  // Helper method to add forge shift heat
  public void addForgeShiftHeat(ForgeShiftHeat forgeShiftHeat) {
    forgeShiftHeat.setForgeShift(this);
    this.forgeShiftHeats.add(forgeShiftHeat);
  }

  // Helper method to calculate total heat quantity used in this shift
  public Double getTotalHeatQuantityUsed() {
    return forgeShiftHeats.stream()
        .mapToDouble(ForgeShiftHeat::getHeatQuantityUsed)
        .sum();
  }

  // Helper method to check if shift has rejections
  public boolean hasRejections() {
    return rejection != null && rejection && 
           ((rejectedForgePiecesCount != null && rejectedForgePiecesCount > 0) || 
            (otherForgeRejectionsKg != null && otherForgeRejectionsKg > 0));
  }
} 