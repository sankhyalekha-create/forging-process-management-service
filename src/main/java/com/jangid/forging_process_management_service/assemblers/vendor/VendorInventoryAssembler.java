package com.jangid.forging_process_management_service.assemblers.vendor;

import com.jangid.forging_process_management_service.assemblers.inventory.RawMaterialHeatAssembler;
import com.jangid.forging_process_management_service.assemblers.inventory.RawMaterialProductAssembler;
import com.jangid.forging_process_management_service.entities.vendor.VendorInventory;
import com.jangid.forging_process_management_service.entitiesRepresentation.vendor.VendorInventoryRepresentation;
import com.jangid.forging_process_management_service.utils.ConvertorUtils;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Assembler for converting between VendorInventory entity and VendorInventoryRepresentation
 */
@Slf4j
@Component
public class VendorInventoryAssembler {

    @Autowired
    private VendorAssembler vendorAssembler;

    @Autowired
    private RawMaterialHeatAssembler rawMaterialHeatAssembler;

    @Autowired
    private RawMaterialProductAssembler rawMaterialProductAssembler;

    /**
     * Convert VendorInventory entity to VendorInventoryRepresentation
     */
    public VendorInventoryRepresentation dissemble(VendorInventory vendorInventory) {
        if (vendorInventory == null) {
            return null;
        }

        return VendorInventoryRepresentation.builder()
                .id(vendorInventory.getId())
                .vendor(vendorAssembler.dissemble(vendorInventory.getVendor()))
                .originalHeat(rawMaterialHeatAssembler.dissemble(vendorInventory.getOriginalHeat()))
                .rawMaterialProduct(rawMaterialProductAssembler.dissemble(vendorInventory.getRawMaterialProduct()))
                .heatNumber(vendorInventory.getHeatNumber())
                .totalDispatchedQuantity(vendorInventory.getTotalDispatchedQuantity())
                .availableQuantity(vendorInventory.getAvailableQuantity())
                .isInPieces(vendorInventory.getIsInPieces())
                .totalDispatchedPieces(vendorInventory.getTotalDispatchedPieces())
                .availablePiecesCount(vendorInventory.getAvailablePiecesCount())
                .testCertificateNumber(vendorInventory.getTestCertificateNumber())
                .createdAt(ConvertorUtils.convertLocalDateTimeToString(vendorInventory.getCreatedAt()))
                .updatedAt(ConvertorUtils.convertLocalDateTimeToString(vendorInventory.getUpdatedAt()))
                .build();
    }

    /**
     * Convert VendorInventoryRepresentation to VendorInventory entity
     * Note: This method is for completeness but typically not used in read-only scenarios
     */
    public VendorInventory assemble(VendorInventoryRepresentation representation) {
        if (representation == null) {
            return null;
        }

        return VendorInventory.builder()
                .id(representation.getId())
                .heatNumber(representation.getHeatNumber())
                .totalDispatchedQuantity(representation.getTotalDispatchedQuantity())
                .availableQuantity(representation.getAvailableQuantity())
                .isInPieces(representation.getIsInPieces())
                .totalDispatchedPieces(representation.getTotalDispatchedPieces())
                .availablePiecesCount(representation.getAvailablePiecesCount())
                .testCertificateNumber(representation.getTestCertificateNumber())
                .createdAt(ConvertorUtils.convertStringToLocalDateTime(representation.getCreatedAt()))
                .updatedAt(ConvertorUtils.convertStringToLocalDateTime(representation.getUpdatedAt()))
                .build();
    }
} 