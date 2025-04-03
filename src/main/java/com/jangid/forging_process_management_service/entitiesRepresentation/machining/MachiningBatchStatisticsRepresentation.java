package com.jangid.forging_process_management_service.entitiesRepresentation.machining;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MachiningBatchStatisticsRepresentation {
    private Integer totalInProgressBatches;
    private Integer totalPiecesInProgress;
    private Integer totalMachineSetsInUse;
    private Integer totalOperatorsAssigned;
    private Double averageProcessingTimeInHours;
    private Integer totalReworkBatches;
    private Integer totalFreshBatches;
    private List<MachiningBatchDetailRepresentation> batchDetails;
} 