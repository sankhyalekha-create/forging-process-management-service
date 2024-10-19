package com.jangid.forging_process_management_service.entities.forging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Index;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "forge_tracebility", indexes = {
    @Index(name = "idx_forge_tracebility_heat_id", columnList = "heat_id"),
    @Index(name = "idx_forge_tracebility_forging_line_id", columnList = "forging_line_id")
})
@EntityListeners(AuditingEntityListener.class)
public class ForgeTracebility {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "heat_id", nullable = false)
  private Long heatId;

  @Column(name = "heat_id_quantity_used", nullable = false)
  private Float heatIdQuantityUsed;

  @Column(name = "start_at")
  private LocalDateTime startAt;

  @Column(name = "end_at")
  private LocalDateTime endAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "forging_line_id", nullable = false)
  private ForgingLine forgingLine;

  @Column(name = "forge_piece_weight", nullable = false)
  private Float forgePieceWeight;

  @Column(name = "actual_forge_count")
  private Integer actualForgeCount;

  @Column(name = "forging_status", nullable = false)
  private String forgingStatus;

  @Column(name = "created_at")
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;
}

