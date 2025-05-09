package com.jangid.forging_process_management_service.dto;

import com.jangid.forging_process_management_service.entitiesRepresentation.heating.HeatTreatmentBatchRepresentation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO that combines all associated machining batches for a heat treatment batch
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HeatTreatmentBatchAssociationsDTO {
    private Long heatTreatmentBatchId;
    private HeatTreatmentBatchRepresentation heatTreatmentBatch;
    private List<MachiningBatchAssociationsDTO> machiningBatches;
} 