package com.jangid.forging_process_management_service.entitiesRepresentation.machining;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MachiningBatchDetailRepresentation {
    private Long id;
    private String machiningBatchNumber;
    private String machineSetName;
    private Integer totalPieces;
    private Integer completedPieces;
    private Integer rejectedPieces;
    private Integer reworkPieces;
    private Integer availablePieces;
    private String startAt;
    private Double processingTimeInHours;
    private String machiningBatchType;
    private String machiningBatchStatus;
    private Integer totalOperatorsAssigned;
} 