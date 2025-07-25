package com.jangid.forging_process_management_service.entities.forging;

import com.jangid.forging_process_management_service.entities.ProcessedItem;
import com.jangid.forging_process_management_service.entities.Tenant;

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
import jakarta.validation.constraints.NotNull;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "forge", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"forge_traceability_number", "tenant_id", "deleted"})
})
@EntityListeners(AuditingEntityListener.class)
public class Forge {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO, generator = "forge_key_sequence_generator")
  @SequenceGenerator(name = "forge_key_sequence_generator", sequenceName = "forge_sequence", allocationSize = 1)
  private Long id;

  @Column(name = "forge_traceability_number")
  private String forgeTraceabilityNumber;

  @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  @JoinColumn(name = "processed_item_id", nullable = false)
  private ProcessedItem processedItem;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "forge_id")
  private List<ForgeHeat> forgeHeats = new ArrayList<>();

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "forge_id")
  @Builder.Default
  private List<ForgeShift> forgeShifts = new ArrayList<>();

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "forging_line_id", nullable = false)
  private ForgingLine forgingLine;

  @Enumerated(EnumType.STRING)
  @Column(name = "forging_status", nullable = false)
  private ForgeStatus forgingStatus;

  @Enumerated(EnumType.STRING)
  @Column(name = "item_weight_type")
  private ItemWeightType itemWeightType;

  @Column(name = "apply_at")
  private LocalDateTime applyAt;

  @Column(name = "start_at")
  private LocalDateTime startAt;

  @Column(name = "end_at")
  private LocalDateTime endAt;

  @CreatedDate
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Version
  private LocalDateTime updatedAt;

  private LocalDateTime deletedAt;

  private boolean deleted;

  @NotNull
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "tenant_id", nullable = false)
  private Tenant tenant;


  public enum ForgeStatus {
    IDLE,
    IN_PROGRESS,
    COMPLETED
  }

  public void setForge(ForgeHeat forgeHeat) {
    forgeHeat.setForge(this);
  }

//  public void updateForgeHeats(List<ForgeHeat> forgeHeats) {
//    forgeHeats.forEach(this::addForgeHeat);
//  }
//
public void setProcessedItem(ProcessedItem processedItem) {
  processedItem.setForge(this);
  this.processedItem = processedItem;
}

  // Helper method to add forge shift
  public void addForgeShift(ForgeShift forgeShift) {
    forgeShift.setForge(this);
    this.forgeShifts.add(forgeShift);
  }

  // Helper method to get the latest forge shift
  public ForgeShift getLatestForgeShift() {
    return forgeShifts.stream()
        .filter(shift -> !shift.isDeleted())
        .max((s1, s2) -> s1.getEndDateTime().compareTo(s2.getEndDateTime()))
        .orElse(null);
  }

//  public void calculateActualForgeCount() {
//    this.actualForgeCount = (int) Math.floor(this.processedItem.getItem().getItemWeight() / this.forgeCount);
//  }
}
