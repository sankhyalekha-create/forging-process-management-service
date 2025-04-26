package com.jangid.forging_process_management_service.dto;

import com.jangid.forging_process_management_service.entitiesRepresentation.dispatch.DispatchBatchRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.machining.MachiningBatchRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.quality.InspectionBatchRepresentation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO that combines all associated inspection batches and dispatch batches for a machining batch
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MachiningBatchAssociationsDTO {
    private Long machiningBatchId;
    private MachiningBatchRepresentation machiningBatch;
    private List<InspectionBatchRepresentation> inspectionBatches;
    private List<DispatchBatchRepresentation> dispatchBatches;
} 