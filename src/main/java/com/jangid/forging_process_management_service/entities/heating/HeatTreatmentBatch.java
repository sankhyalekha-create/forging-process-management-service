package com.jangid.forging_process_management_service.entities.heating;

import com.jangid.forging_process_management_service.entities.Tenant;
import com.jangid.forging_process_management_service.entities.forging.Furnace;

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
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "heat_treatment_batch",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_heat_treatment_batch_number_tenant_deleted",
            columnNames = {"heat_treatment_batch_number", "tenant_id", "deleted"}
        )
    })
@EntityListeners(AuditingEntityListener.class)
public class HeatTreatmentBatch {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO, generator = "heat_treatment_batch_key_sequence_generator")
  @SequenceGenerator(name = "heat_treatment_batch_key_sequence_generator", sequenceName = "heat_treatment_batch_sequence", allocationSize = 1)
  private Long id;

  @Column(name = "heat_treatment_batch_number")
  private String heatTreatmentBatchNumber;
  
  @Column(name = "original_heat_treatment_batch_number")
  private String originalHeatTreatmentBatchNumber;

  @OneToMany(mappedBy = "heatTreatmentBatch", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<ProcessedItemHeatTreatmentBatch> processedItemHeatTreatmentBatches = new ArrayList<>();

  @Column(name = "total_weight", nullable = false)
  private Double totalWeight = 0.0;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "furnace_id", nullable = false)
  private Furnace furnace;

  @Column(name = "heat_treatment_batch_status", nullable = false)
  @Enumerated(EnumType.STRING)
  private HeatTreatmentBatchStatus heatTreatmentBatchStatus;

  @Column(name = "lab_testing_report")
  private String labTestingReport;

  @Column(name = "lab_testing_status")
  private String labTestingStatus;

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

  // Method to add processed item heat treatment batch and validate pieces count
  public void addProcessedItemHeatTreatmentBatch(ProcessedItemHeatTreatmentBatch processedItemHeatTreatmentBatch, int availableForgePiecesForHeat) {
    if (availableForgePiecesForHeat < processedItemHeatTreatmentBatch.getHeatTreatBatchPiecesCount()) {
      throw new IllegalArgumentException("Piece count exceeds available forge pieces count.");
    }

    if (this.processedItemHeatTreatmentBatches == null) {
      this.processedItemHeatTreatmentBatches = new ArrayList<>();
    }

    this.processedItemHeatTreatmentBatches.add(processedItemHeatTreatmentBatch);
    this.calculateTotalWeight();
  }

  // Method to calculate total weight for the heat treatment batch
  public void calculateTotalWeight() {
    if (this.processedItemHeatTreatmentBatches!=null && !this.processedItemHeatTreatmentBatches.isEmpty()){
      this.totalWeight = this.processedItemHeatTreatmentBatches.stream()
          .mapToDouble(batch -> batch.getHeatTreatBatchPiecesCount() * batch.getItem().getItemForgedWeight())
          .sum();
    }
  }

  public enum HeatTreatmentBatchStatus {
    IDLE,
    IN_PROGRESS,
    COMPLETED;
  }
}
