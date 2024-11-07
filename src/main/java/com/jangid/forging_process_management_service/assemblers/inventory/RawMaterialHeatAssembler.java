package com.jangid.forging_process_management_service.assemblers.inventory;

import com.jangid.forging_process_management_service.entities.inventory.RawMaterialHeat;
import com.jangid.forging_process_management_service.entitiesRepresentation.inventory.RawMaterialHeatRepresentation;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RawMaterialHeatAssembler {

  public static RawMaterialHeatRepresentation dissemble(RawMaterialHeat rawMaterialHeat) {
    RawMaterialHeatRepresentation heatRepresentation = RawMaterialHeatRepresentation.builder()
        .id(rawMaterialHeat.getId())
        .heatNumber(rawMaterialHeat.getHeatNumber())
        .heatQuantity(String.valueOf(rawMaterialHeat.getHeatQuantity()))
        .availableHeatQuantity(String.valueOf(rawMaterialHeat.getAvailableHeatQuantity()))
        .rawMaterialTestCertificateNumber(rawMaterialHeat.getRawMaterialTestCertificateNumber())
        .barDiameter(rawMaterialHeat.getBarDiameter() != null ? rawMaterialHeat.getBarDiameter().toString() : null)
        .rawMaterialReceivingInspectionReportNumber(rawMaterialHeat.getRawMaterialReceivingInspectionReportNumber())
        .rawMaterialInspectionSource(rawMaterialHeat.getRawMaterialInspectionSource())
        .createdAt(rawMaterialHeat.getCreatedAt() != null ? rawMaterialHeat.getCreatedAt().toString() : null)
        .updatedAt(rawMaterialHeat.getUpdatedAt() != null ? rawMaterialHeat.getUpdatedAt().toString() : null)
        .build();
    return heatRepresentation;
  }
}
