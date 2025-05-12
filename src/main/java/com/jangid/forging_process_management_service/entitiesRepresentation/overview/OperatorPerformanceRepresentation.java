package com.jangid.forging_process_management_service.entitiesRepresentation.overview;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperatorPerformanceRepresentation {
  // Operator details
  private Long operatorId;
  private String fullName;

  // Time period
  private LocalDateTime startPeriod;
  private LocalDateTime endPeriod;

  // Performance metrics
  private Integer totalBatchesCompleted;
  private Integer totalPiecesCompleted;
  private Integer totalPiecesRejected;
  private Integer totalPiecesReworked;

  // Calculated metrics
  private Double completionRate; // (completed pieces / total pieces) * 100
  private Double rejectionRate; // (rejected pieces / total pieces) * 100
  private Double reworkRate; // (rework pieces / total pieces) * 100
  private Double averagePiecesPerBatch;

  // Time metrics
  private Long totalWorkingHours;
  private Double averageProductionRatePerHour;
  
  // Wage information
  private BigDecimal hourlyWages;
  private BigDecimal totalWages;

  // Current status
  private String currentBatchStatus;
  private LocalDateTime lastActive;
}
