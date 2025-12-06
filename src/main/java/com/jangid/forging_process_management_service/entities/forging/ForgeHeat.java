package com.jangid.forging_process_management_service.entities.forging;

import com.jangid.forging_process_management_service.entities.inventory.Heat;
import com.jangid.forging_process_management_service.utils.PrecisionUtils;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
@EntityListeners(AuditingEntityListener.class)
@Table(name = "forge_heat")
public class ForgeHeat {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO, generator = "forge_heat_key_sequence_generator")
  @SequenceGenerator(name = "forge_heat_key_sequence_generator", sequenceName = "forge_heat_sequence", allocationSize = 1)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "forge_id", nullable = false)
  private Forge forge;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "heat_id", nullable = false)
  private Heat heat;

  @Column(name = "heat_quantity_used", nullable = false)
  private Double heatQuantityUsed;

  @Column(name = "heat_quantity_used_in_rejected_pieces")
  @Builder.Default
  private Double heatQuantityUsedInRejectedPieces = 0.0;

  @Column(name = "heat_quantity_used_in_other_rejections")
  @Builder.Default
  private Double heatQuantityUsedInOtherRejections = 0.0;

  @Column(name = "rejected_pieces")
  @Builder.Default
  private Integer rejectedPieces = 0;

  @Column(name = "heat_quantity_returned")
  @Builder.Default
  private Double heatQuantityReturned = 0.0;

  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Version
  private LocalDateTime updatedAt;

  private LocalDateTime deletedAt;

  private boolean deleted;

  // Helper to update the available quantity in Heat
  public void consumeHeatQuantity() {
    double remainingQuantity = this.heat.getAvailableHeatQuantity() - this.heatQuantityUsed;
    if (remainingQuantity < 0) {
      throw new IllegalArgumentException("Consumed heat quantity exceeds available quantity.");
    }
    this.heat.setAvailableHeatQuantity(PrecisionUtils.roundQuantity(remainingQuantity));
  }

//  public void setForge(Forge forge) {
//    this.setForge(forge);
//  }
}
