package com.jangid.forging_process_management_service.assemblers.inventory;

import com.jangid.forging_process_management_service.entities.inventory.Heat;
import com.jangid.forging_process_management_service.entitiesRepresentation.inventory.RawMaterialHeatRepresentation;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RawMaterialHeatAssembler {

  public static RawMaterialHeatRepresentation dissemble(Heat heat) {
    RawMaterialHeatRepresentation heatRepresentation = RawMaterialHeatRepresentation.builder()
        .id(heat.getId())
        .heatNumber(heat.getHeatNumber())
        .heatQuantity(String.valueOf(heat.getHeatQuantity()))
        .availableHeatQuantity(String.valueOf(heat.getAvailableHeatQuantity()))
        .rawMaterialTestCertificateNumber(heat.getRawMaterialTestCertificateNumber())
        .rawMaterialReceivingInspectionReportNumber(heat.getRawMaterialReceivingInspectionReportNumber())
        .rawMaterialInspectionSource(heat.getRawMaterialInspectionSource())
        .createdAt(heat.getCreatedAt() != null ? heat.getCreatedAt().toString() : null)
        .updatedAt(heat.getUpdatedAt() != null ? heat.getUpdatedAt().toString() : null)
        .build();
    return heatRepresentation;
  }
}
