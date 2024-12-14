package com.jangid.forging_process_management_service.entities.heating;

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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "heat_treatment_batch")
@EntityListeners(AuditingEntityListener.class)
public class HeatTreatmentBatch {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO, generator = "heat_treatment_batch_key_sequence_generator")
  @SequenceGenerator(name = "heat_treatment_batch_key_sequence_generator", sequenceName = "heat_treatment_batch_sequence", allocationSize = 1)
  private Long id;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "heat_treatment_batch_id")
  private List<BatchItemSelection> batchItems = new ArrayList<>();

  @Column(name = "total_weight", nullable = false)
  private Double totalWeight = 0.0;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "furnace_id", nullable = false)
  private Furnace furnace;

  @Column(name = "heat_treatment_batch_status", nullable = false)
  private HeatTreatmentBatchStatus heatTreatmentBatchStatus;

  @Column(name = "lab_testing_report")
  private String labTestingReport;

  @Column(name = "lab_testing_status")
  private String labTestingStatus;

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

//  public void addItem(BatchItemSelection itemSelection) {
//    // Validate the piece count is within the forge's actual forge count
//    if (itemSelection.getHeatTreatBatchPiecesCount() > itemSelection.getForge().getActualForgeCount()) {
//      throw new IllegalArgumentException("Piece count exceeds the forge's actual forge count.");
//    }
//    if(this.batchItems == null){
//      this.batchItems = new ArrayList<>();
//    }
//    this.batchItems.add(itemSelection);
//    this.calculateTotalWeight();
//  }
//
//  public void calculateTotalWeight() {
//    this.totalWeight = this.batchItems.stream()
//        .mapToDouble(item -> item.getHeatTreatBatchPiecesCount() * item.getForge().getItem().getItemWeight())
//        .sum();
//  }

  public enum HeatTreatmentBatchStatus {
    IDLE,
    IN_PROGRESS,
    COMPLETED;
  }
}

