package com.jangid.forging_process_management_service.dto;

import com.jangid.forging_process_management_service.entitiesRepresentation.dispatch.DispatchBatchRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.forging.ForgeRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.heating.HeatTreatmentBatchRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.machining.MachiningBatchRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.quality.InspectionBatchRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.vendor.VendorDispatchBatchRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.workflow.ItemWorkflowRepresentation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemWorkflowTrackingResultDTO {
    private ItemWorkflowRepresentation itemWorkflow;
    
    // These lists are optional - only populated when requested or when no specific batch type is requested
    private List<ForgeRepresentation> forges;
    private List<HeatTreatmentBatchRepresentation> heatTreatmentBatches;
    private List<MachiningBatchRepresentation> machiningBatches;
    private List<InspectionBatchRepresentation> inspectionBatches;
    private List<VendorDispatchBatchRepresentation> vendorDispatchBatches;
    private List<DispatchBatchRepresentation> dispatchBatches;
    
    // Specific batch details - populated when requesting a specific batch type and number
    private ForgeRepresentation specificForge;
    private HeatTreatmentBatchRepresentation specificHeatTreatmentBatch;
    private MachiningBatchRepresentation specificMachiningBatch;
    private InspectionBatchRepresentation specificInspectionBatch;
    private VendorDispatchBatchRepresentation specificVendorDispatchBatch;
    private DispatchBatchRepresentation specificDispatchBatch;
} 