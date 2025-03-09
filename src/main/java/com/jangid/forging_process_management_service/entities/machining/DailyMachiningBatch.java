package com.jangid.forging_process_management_service.entities.machining;

import com.jangid.forging_process_management_service.entities.operator.MachineOperator;

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
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "daily_machining_batch")
@EntityListeners(AuditingEntityListener.class)
public class DailyMachiningBatch {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO, generator = "daily_machining_batch_key_sequence_generator")
  @SequenceGenerator(name = "daily_machining_batch_key_sequence_generator", sequenceName = "daily_machining_batch_sequence", allocationSize = 1)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "machining_batch_id", nullable = false)
  private MachiningBatch machiningBatch;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "machine_operator_id")
  private MachineOperator machineOperator;

  @Column(name = "daily_machining_batch_status", nullable = false)
  @Enumerated(EnumType.STRING)
  private DailyMachiningBatchStatus dailyMachiningBatchStatus;

  @Column(name = "start_date_time", nullable = false)
  private LocalDateTime startDateTime;

  @Column(name = "end_date_time", nullable = false)
  private LocalDateTime endDateTime;

  @Column(name = "completed_pieces_count", nullable = false)
  private Integer completedPiecesCount;

  @Column(name = "rejected_pieces_count", nullable = false)
  private Integer rejectedPiecesCount;

  @Column(name = "rework_pieces_count", nullable = false)
  private Integer reworkPiecesCount;

  @CreatedDate
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Version
  private LocalDateTime updatedAt;

  private LocalDateTime deletedAt;

  private boolean deleted;

  public enum DailyMachiningBatchStatus {
    IDLE,
    IN_PROGRESS,
    COMPLETED;
  }
}
