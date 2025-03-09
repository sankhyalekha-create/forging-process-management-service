package com.jangid.forging_process_management_service.entities.machining;

import com.jangid.forging_process_management_service.entities.heating.ProcessedItemHeatTreatmentBatch;
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
@Table(name = "machining_batch")
@EntityListeners(AuditingEntityListener.class)
public class MachiningBatch {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO, generator = "machining_batch_key_sequence_generator")
  @SequenceGenerator(name = "machining_batch_key_sequence_generator", sequenceName = "machining_batch_sequence", allocationSize = 1)
  private Long id;

  @Column(name = "machining_batch_number", nullable = false, unique = true)
  private String machiningBatchNumber;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "processed_item_heat_treatment_batch_id")
  private ProcessedItemHeatTreatmentBatch processedItemHeatTreatmentBatch;

  @OneToOne(mappedBy = "machiningBatch", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  private ProcessedItemMachiningBatch processedItemMachiningBatch;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "input_processed_item_machining_batch_id")
  private ProcessedItemMachiningBatch inputProcessedItemMachiningBatch;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "machine_set", nullable = false)
  private MachineSet machineSet;


  @Column(name = "machining_batch_status", nullable = false)
  private MachiningBatchStatus machiningBatchStatus;

  @Column(name = "machining_batch_type", nullable = false)
  private MachiningBatchType machiningBatchType;

  @OneToMany(mappedBy = "machiningBatch", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  private List<DailyMachiningBatch> dailyMachiningBatch = new ArrayList<>();

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

  public enum MachiningBatchStatus {
    IDLE,
    IN_PROGRESS,
    COMPLETED
  }

  public enum MachiningBatchType {
    FRESH,
    REWORK;
  }


  public void setProcessedItemMachiningBatch(ProcessedItemMachiningBatch machiningBatch, ProcessedItemHeatTreatmentBatch heatTreatmentBatch) {
    if (heatTreatmentBatch.getAvailableMachiningBatchPiecesCount() < machiningBatch.getMachiningBatchPiecesCount()) {
      throw new IllegalArgumentException("Machining batch pieces count exceeds available machining batch pieces count.");
    }

    // Deduct the pieces from the heat treatment batch
    heatTreatmentBatch.setAvailableMachiningBatchPiecesCount(
        heatTreatmentBatch.getAvailableMachiningBatchPiecesCount() - machiningBatch.getMachiningBatchPiecesCount()
    );

    // Link the machining batch to this MachiningBatch entity
    machiningBatch.setMachiningBatch(this);
    this.processedItemMachiningBatch = machiningBatch;
  }
}
