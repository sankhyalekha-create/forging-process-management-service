package com.jangid.forging_process_management_service.assemblers.vendor;

import com.jangid.forging_process_management_service.assemblers.inventory.RawMaterialHeatAssembler;
import com.jangid.forging_process_management_service.entities.ConsumptionType;
import com.jangid.forging_process_management_service.entities.vendor.VendorDispatchHeat;
import com.jangid.forging_process_management_service.entities.vendor.ProcessedItemVendorDispatchBatch;
import com.jangid.forging_process_management_service.entitiesRepresentation.vendor.VendorDispatchHeatRepresentation;
import com.jangid.forging_process_management_service.service.inventory.RawMaterialHeatService;
import com.jangid.forging_process_management_service.utils.PrecisionUtils;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class VendorDispatchHeatAssembler {

    @Autowired
    private RawMaterialHeatService rawMaterialHeatService;
    
    @Autowired
    private RawMaterialHeatAssembler rawMaterialHeatAssembler;

    @Autowired
    @Lazy
    private ProcessedItemVendorDispatchBatchAssembler processedItemVendorDispatchBatchAssembler;

    public VendorDispatchHeatRepresentation dissemble(VendorDispatchHeat vendorDispatchHeat) {
        return dissemble(vendorDispatchHeat, false);
    }

    public VendorDispatchHeatRepresentation dissemble(VendorDispatchHeat vendorDispatchHeat, boolean includeProcessedItemVendorDispatchBatch) {
        return VendorDispatchHeatRepresentation.builder()
                .id(vendorDispatchHeat.getId())
                .processedItemVendorDispatchBatch(includeProcessedItemVendorDispatchBatch && vendorDispatchHeat.getProcessedItemVendorDispatchBatch() != null ? 
                    processedItemVendorDispatchBatchAssembler.dissemble(vendorDispatchHeat.getProcessedItemVendorDispatchBatch(), false) : null)
                .heat(rawMaterialHeatAssembler.dissemble(vendorDispatchHeat.getHeat()))
                .consumptionType(vendorDispatchHeat.getConsumptionType() != null ? vendorDispatchHeat.getConsumptionType().name() : null)
                .quantityUsed(vendorDispatchHeat.getQuantityUsed())
                .piecesUsed(vendorDispatchHeat.getPiecesUsed())
                .createdAt(vendorDispatchHeat.getCreatedAt() != null ? vendorDispatchHeat.getCreatedAt().toString() : null)
                .updatedAt(vendorDispatchHeat.getUpdatedAt() != null ? vendorDispatchHeat.getUpdatedAt().toString() : null)
                .deletedAt(vendorDispatchHeat.getDeletedAt() != null ? vendorDispatchHeat.getDeletedAt().toString() : null)
                .deleted(vendorDispatchHeat.isDeleted())
                .build();
    }

    public VendorDispatchHeat createAssemble(VendorDispatchHeatRepresentation vendorDispatchHeatRepresentation) {
        return VendorDispatchHeat.builder()
                .heat(rawMaterialHeatService.getRawMaterialHeatById(vendorDispatchHeatRepresentation.getHeat().getId()))
                .consumptionType(vendorDispatchHeatRepresentation.getConsumptionType() != null ? 
                    ConsumptionType.valueOf(vendorDispatchHeatRepresentation.getConsumptionType()) : null)
                .quantityUsed(PrecisionUtils.roundQuantity(vendorDispatchHeatRepresentation.getQuantityUsed()))
                .piecesUsed(vendorDispatchHeatRepresentation.getPiecesUsed())
                .createdAt(LocalDateTime.now())
                .deleted(false)
                .build();
    }

    public VendorDispatchHeat assemble(VendorDispatchHeatRepresentation vendorDispatchHeatRepresentation, ProcessedItemVendorDispatchBatch processedItemVendorDispatchBatch) {
        return VendorDispatchHeat.builder()
                .processedItemVendorDispatchBatch(processedItemVendorDispatchBatch)
                .heat(rawMaterialHeatService.getRawMaterialHeatById(vendorDispatchHeatRepresentation.getHeat().getId()))
                .consumptionType(vendorDispatchHeatRepresentation.getConsumptionType() != null ? 
                    ConsumptionType.valueOf(vendorDispatchHeatRepresentation.getConsumptionType()) : null)
                .quantityUsed(PrecisionUtils.roundQuantity(vendorDispatchHeatRepresentation.getQuantityUsed()))
                .piecesUsed(vendorDispatchHeatRepresentation.getPiecesUsed())
                .createdAt(LocalDateTime.now())
                .deleted(false)
                .build();
    }
} 