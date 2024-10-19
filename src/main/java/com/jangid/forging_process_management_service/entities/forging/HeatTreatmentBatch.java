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
@Table(name = "heat_treatment_batch", indexes = {
    @Index(name = "idx_heat_treatment_batch_status", columnList = "heat_treatment_batch_status")
})
@EntityListeners(AuditingEntityListener.class)
public class HeatTreatmentBatch {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "forge_tracebility_id", nullable = false)
  private ForgeTracebility forgeTracebility;

  @Column(name = "start_at")
  private LocalDateTime startAt;

  @Column(name = "end_at")
  private LocalDateTime endAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "furnace_id", nullable = false)
  private Furnace furnace;

  @Column(name = "heat_treatment_batch_status", nullable = false)
  private String heatTreatmentBatchStatus;

  @Column(name = "lab_testing_report")
  private String labTestingReport;

  @Column(name = "lab_testing_status")
  private String labTestingStatus;

  @Column(name = "created_at")
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

}

