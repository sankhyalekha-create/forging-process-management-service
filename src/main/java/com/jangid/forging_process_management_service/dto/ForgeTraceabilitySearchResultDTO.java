package com.jangid.forging_process_management_service.dto;

import com.jangid.forging_process_management_service.entitiesRepresentation.dispatch.DispatchBatchRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.forging.ForgeRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.heating.HeatTreatmentBatchRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.machining.MachiningBatchRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.quality.InspectionBatchRepresentation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForgeTraceabilitySearchResultDTO {
    private ForgeRepresentation forge;
    private List<HeatTreatmentBatchRepresentation> heatTreatmentBatches;
    private List<MachiningBatchRepresentation> machiningBatches;
    private List<InspectionBatchRepresentation> inspectionBatches;
    private List<DispatchBatchRepresentation> dispatchBatches;
} 