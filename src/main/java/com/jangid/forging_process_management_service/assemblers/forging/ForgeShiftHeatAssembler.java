package com.jangid.forging_process_management_service.assemblers.forging;

import com.jangid.forging_process_management_service.assemblers.inventory.RawMaterialHeatAssembler;
import com.jangid.forging_process_management_service.entities.forging.ForgeShiftHeat;
import com.jangid.forging_process_management_service.entitiesRepresentation.forging.ForgeShiftHeatRepresentation;
import com.jangid.forging_process_management_service.service.inventory.RawMaterialHeatService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class ForgeShiftHeatAssembler {

  @Autowired
  private RawMaterialHeatService heatService;
  
  @Autowired
  private RawMaterialHeatAssembler rawMaterialHeatAssembler;

  public ForgeShiftHeatRepresentation dissemble(ForgeShiftHeat forgeShiftHeat) {
    if (forgeShiftHeat == null) {
      return null;
    }

    return ForgeShiftHeatRepresentation.builder()
        .id(forgeShiftHeat.getId())
        .forgeShiftId(forgeShiftHeat.getForgeShift().getId())
        .heat(rawMaterialHeatAssembler.dissembleBasic(forgeShiftHeat.getHeat())) // Use basic representation to reduce payload
        .heatQuantityUsed(String.valueOf(forgeShiftHeat.getHeatQuantityUsed()))
        .heatPieces(String.valueOf(forgeShiftHeat.getHeatPieces()))
        .heatQuantityUsedInRejectedPieces(forgeShiftHeat.getHeatQuantityUsedInRejectedPieces() != null ? 
            String.valueOf(forgeShiftHeat.getHeatQuantityUsedInRejectedPieces()) : null)
        .heatQuantityUsedInOtherRejections(forgeShiftHeat.getHeatQuantityUsedInOtherRejections() != null ? 
            String.valueOf(forgeShiftHeat.getHeatQuantityUsedInOtherRejections()) : null)
        .rejectedPieces(forgeShiftHeat.getRejectedPieces() != null ? 
            String.valueOf(forgeShiftHeat.getRejectedPieces()) : null)
        .createdAt(forgeShiftHeat.getCreatedAt() != null ? forgeShiftHeat.getCreatedAt().toString() : null)
        .updatedAt(forgeShiftHeat.getUpdatedAt() != null ? forgeShiftHeat.getUpdatedAt().toString() : null)
        .deletedAt(forgeShiftHeat.getDeletedAt() != null ? forgeShiftHeat.getDeletedAt().toString() : null)
        .build();
  }

  public ForgeShiftHeat createAssemble(ForgeShiftHeatRepresentation forgeShiftHeatRepresentation) {
    if (forgeShiftHeatRepresentation == null) {
      return null;
    }

    // Get heat ID from either heatId field or heat.id field
    Long heatId = forgeShiftHeatRepresentation.getHeatId();
    if (heatId == null && forgeShiftHeatRepresentation.getHeat() != null) {
      heatId = forgeShiftHeatRepresentation.getHeat().getId();
    }
    
    if (heatId == null) {
      throw new IllegalArgumentException("Heat ID is required for forge shift heat creation");
    }

    return ForgeShiftHeat.builder()
        .heat(heatService.getRawMaterialHeatById(heatId))
        .heatQuantityUsed(Double.parseDouble(forgeShiftHeatRepresentation.getHeatQuantityUsed()))
        .heatPieces(forgeShiftHeatRepresentation.getHeatPieces() != null ? 
            Integer.parseInt(forgeShiftHeatRepresentation.getHeatPieces()) : 0)
        .heatQuantityUsedInRejectedPieces(forgeShiftHeatRepresentation.getHeatQuantityUsedInRejectedPieces() != null ? 
            Double.parseDouble(forgeShiftHeatRepresentation.getHeatQuantityUsedInRejectedPieces()) : 0.0)
        .heatQuantityUsedInOtherRejections(forgeShiftHeatRepresentation.getHeatQuantityUsedInOtherRejections() != null ? 
            Double.parseDouble(forgeShiftHeatRepresentation.getHeatQuantityUsedInOtherRejections()) : 0.0)
        .rejectedPieces(forgeShiftHeatRepresentation.getRejectedPieces() != null ? 
            Integer.parseInt(forgeShiftHeatRepresentation.getRejectedPieces()) : 0)
        .createdAt(LocalDateTime.now())
        .build();
  }

  public ForgeShiftHeat assemble(ForgeShiftHeatRepresentation forgeShiftHeatRepresentation) {
    if (forgeShiftHeatRepresentation == null) {
      return null;
    }

    return ForgeShiftHeat.builder()
        .id(forgeShiftHeatRepresentation.getId())
        .heat(heatService.getRawMaterialHeatById(forgeShiftHeatRepresentation.getHeat().getId()))
        .heatQuantityUsed(Double.parseDouble(forgeShiftHeatRepresentation.getHeatQuantityUsed()))
        .heatPieces(forgeShiftHeatRepresentation.getHeatPieces() != null ? 
            Integer.parseInt(forgeShiftHeatRepresentation.getHeatPieces()) : 0)
        .heatQuantityUsedInRejectedPieces(forgeShiftHeatRepresentation.getHeatQuantityUsedInRejectedPieces() != null ? 
            Double.parseDouble(forgeShiftHeatRepresentation.getHeatQuantityUsedInRejectedPieces()) : 0.0)
        .heatQuantityUsedInOtherRejections(forgeShiftHeatRepresentation.getHeatQuantityUsedInOtherRejections() != null ? 
            Double.parseDouble(forgeShiftHeatRepresentation.getHeatQuantityUsedInOtherRejections()) : 0.0)
        .rejectedPieces(forgeShiftHeatRepresentation.getRejectedPieces() != null ? 
            Integer.parseInt(forgeShiftHeatRepresentation.getRejectedPieces()) : 0)
        .build();
  }

  /**
   * Creates a ForgeShiftHeat entity from ForgeShiftHeatRepresentation when only heatId is provided
   * This is used for new forge shift heats
   */
  public ForgeShiftHeat createAssembleFromHeatId(ForgeShiftHeatRepresentation forgeShiftHeatRepresentation) {
    if (forgeShiftHeatRepresentation == null) {
      return null;
    }

    return ForgeShiftHeat.builder()
        .heat(heatService.getRawMaterialHeatById(forgeShiftHeatRepresentation.getHeatId()))
        .heatQuantityUsed(Double.parseDouble(forgeShiftHeatRepresentation.getHeatQuantityUsed()))
        .heatPieces(forgeShiftHeatRepresentation.getHeatPieces() != null ? 
            Integer.parseInt(forgeShiftHeatRepresentation.getHeatPieces()) : 0)
        .heatQuantityUsedInRejectedPieces(forgeShiftHeatRepresentation.getHeatQuantityUsedInRejectedPieces() != null ? 
            Double.parseDouble(forgeShiftHeatRepresentation.getHeatQuantityUsedInRejectedPieces()) : 0.0)
        .heatQuantityUsedInOtherRejections(forgeShiftHeatRepresentation.getHeatQuantityUsedInOtherRejections() != null ? 
            Double.parseDouble(forgeShiftHeatRepresentation.getHeatQuantityUsedInOtherRejections()) : 0.0)
        .rejectedPieces(forgeShiftHeatRepresentation.getRejectedPieces() != null ? 
            Integer.parseInt(forgeShiftHeatRepresentation.getRejectedPieces()) : 0)
        .createdAt(LocalDateTime.now())
        .build();
  }
} 