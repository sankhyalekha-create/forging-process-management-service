package com.jangid.forging_process_management_service.entities.machining;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "machining_batch_piece_detail")
@EntityListeners(AuditingEntityListener.class)
public class MachiningBatchPieceDetail {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO, generator = "piece_detail_key_sequence_generator")
  @SequenceGenerator(name = "piece_detail_key_sequence_generator", sequenceName = "piece_detail_sequence", allocationSize = 1)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "machining_batch_id", nullable = false)
  private MachiningBatch machiningBatch;

  @Enumerated(EnumType.STRING)
  @Column(name = "piece_status", nullable = false)
  private PieceStatus pieceStatus;

  @Column(name = "quantity", nullable = false)
  private Integer quantity;

  public enum PieceStatus {
    REJECTED,
    REWORK
  }

  @CreatedDate
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Version
  private LocalDateTime updatedAt;

  private LocalDateTime deletedAt;

  private boolean deleted;

}
