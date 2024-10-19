package com.jangid.forging_process_management_service.entities.forging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Index;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;
import jakarta.persistence.Version;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "heat_treatment_batch", indexes = {
    @Index(name = "idx_heat_treatment_batch_status", columnList = "heat_treatment_batch_status")
})
@EntityListeners(AuditingEntityListener.class)
public class HeatTreatmentBatch {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToMany
  @JoinTable(
      name = "heat_treatment_batch_forge_traceability", // Join table name
      joinColumns = @JoinColumn(name = "heat_treatment_batch_id"),
      inverseJoinColumns = @JoinColumn(name = "forge_traceability_id")
  )
  private List<ForgeTraceability> forgeTraceabilities;

  @Column(name = "start_at")
  private LocalDateTime startAt;

  @Column(name = "end_at")
  private LocalDateTime endAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "furnace_id", nullable = false)
  private Furnace furnace;

  @Column(name = "heat_treatment_batch_status", nullable = false)
  private HeatTreatmentBatchStatus heatTreatmentBatchStatus;

  @Column(name = "lab_testing_report")
  private String labTestingReport;

  @Column(name = "lab_testing_status")
  private String labTestingStatus;

  @CreatedDate
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Version
  private LocalDateTime updatedAt;

  private LocalDateTime deletedAt;

  public enum HeatTreatmentBatchStatus{
    IDLE,
    IN_PROGRESS,
    COMPLETED;
  }

}

