package com.jangid.forging_process_management_service.entities.machining;

import com.jangid.forging_process_management_service.entities.Tenant;
import com.jangid.forging_process_management_service.entities.heating.ProcessedItemHeatTreatmentBatch;

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
@Table(name = "machining_batch"
    // Note: Uniqueness for active records handled by partial index in database migration V1_52
)
@EntityListeners(AuditingEntityListener.class)
public class MachiningBatch {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO, generator = "machining_batch_key_sequence_generator")
  @SequenceGenerator(name = "machining_batch_key_sequence_generator", sequenceName = "machining_batch_sequence", allocationSize = 1)
  private Long id;

  @Column(name = "machining_batch_number", nullable = false)
  private String machiningBatchNumber;
  
  @Column(name = "original_machining_batch_number")
  private String originalMachiningBatchNumber;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "processed_item_heat_treatment_batch_id")
  private ProcessedItemHeatTreatmentBatch processedItemHeatTreatmentBatch;

  @OneToOne(mappedBy = "machiningBatch", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  private ProcessedItemMachiningBatch processedItemMachiningBatch;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "input_processed_item_machining_batch_id")
  private ProcessedItemMachiningBatch inputProcessedItemMachiningBatch;

  @Enumerated(EnumType.STRING)
  @Column(name = "machining_batch_status", nullable = false)
  private MachiningBatchStatus machiningBatchStatus;

  @Enumerated(EnumType.STRING)
  @Column(name = "machining_batch_type", nullable = false)
  private MachiningBatchType machiningBatchType;

  @OneToMany(mappedBy = "machiningBatch", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  private List<DailyMachiningBatch> dailyMachiningBatch = new ArrayList<>();

  @Column(name = "create_at")
  private LocalDateTime createAt;

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

  public enum MachiningBatchStatus {
    IDLE,
    IN_PROGRESS,
    COMPLETED;
  }

  public enum MachiningBatchType {
    FRESH,
    REWORK;
  }

}
