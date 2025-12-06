package com.jangid.forging_process_management_service.assemblers.forging;

import com.jangid.forging_process_management_service.assemblers.inventory.RawMaterialHeatAssembler;
import com.jangid.forging_process_management_service.entities.forging.ForgeHeat;
import com.jangid.forging_process_management_service.entitiesRepresentation.forging.ForgeHeatRepresentation;
import com.jangid.forging_process_management_service.service.inventory.RawMaterialHeatService;
import com.jangid.forging_process_management_service.utils.PrecisionUtils;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class ForgeHeatAssembler {

  @Autowired
  private RawMaterialHeatService heatService;
  @Autowired
  private RawMaterialHeatAssembler rawMaterialHeatAssembler;

  public ForgeHeatRepresentation dissemble(ForgeHeat forgeHeat){
    return ForgeHeatRepresentation.builder()
        .forgeId(String.valueOf(forgeHeat.getForge().getId()))
        .id(forgeHeat.getId())
        .heat(rawMaterialHeatAssembler.dissembleBasic(forgeHeat.getHeat())) // Use basic representation to reduce payload
        .heatQuantityUsed(String.valueOf(forgeHeat.getHeatQuantityUsed()))
        .heatQuantityUsedInRejectedPieces(forgeHeat.getHeatQuantityUsedInRejectedPieces() != null ? 
            String.valueOf(forgeHeat.getHeatQuantityUsedInRejectedPieces()) : null)
        .heatQuantityUsedInOtherRejections(forgeHeat.getHeatQuantityUsedInOtherRejections() != null ? 
            String.valueOf(forgeHeat.getHeatQuantityUsedInOtherRejections()) : null)
        .rejectedPieces(forgeHeat.getRejectedPieces() != null ? 
            String.valueOf(forgeHeat.getRejectedPieces()) : null)
        .heatQuantityReturned(forgeHeat.getHeatQuantityReturned() != null ? 
            String.valueOf(forgeHeat.getHeatQuantityReturned()) : null)
        .build();
  }

  public ForgeHeat createAssemble(ForgeHeatRepresentation forgeHeatRepresentation){
    return ForgeHeat.builder()
        .heat(heatService.getRawMaterialHeatById(forgeHeatRepresentation.getHeat().getId()))
        .heatQuantityUsed(PrecisionUtils.roundQuantity(Double.parseDouble(forgeHeatRepresentation.getHeatQuantityUsed())))
        .heatQuantityUsedInRejectedPieces(forgeHeatRepresentation.getHeatQuantityUsedInRejectedPieces() != null ? 
            PrecisionUtils.roundQuantity(Double.parseDouble(forgeHeatRepresentation.getHeatQuantityUsedInRejectedPieces())) : null)
        .heatQuantityUsedInOtherRejections(forgeHeatRepresentation.getHeatQuantityUsedInOtherRejections() != null ? 
            PrecisionUtils.roundQuantity(Double.parseDouble(forgeHeatRepresentation.getHeatQuantityUsedInOtherRejections())) : null)
        .rejectedPieces(forgeHeatRepresentation.getRejectedPieces() != null ? 
            Integer.parseInt(forgeHeatRepresentation.getRejectedPieces()) : null)
        .createdAt(LocalDateTime.now())
        .build();
  }

  public ForgeHeat assemble(ForgeHeatRepresentation forgeHeatRepresentation){
    return ForgeHeat.builder()
        .heat(heatService.getRawMaterialHeatById(forgeHeatRepresentation.getHeat().getId()))
        .heatQuantityUsed(PrecisionUtils.roundQuantity(Double.parseDouble(forgeHeatRepresentation.getHeatQuantityUsed())))
        .heatQuantityUsedInRejectedPieces(forgeHeatRepresentation.getHeatQuantityUsedInRejectedPieces() != null ? 
            PrecisionUtils.roundQuantity(Double.parseDouble(forgeHeatRepresentation.getHeatQuantityUsedInRejectedPieces())) : null)
        .heatQuantityUsedInOtherRejections(forgeHeatRepresentation.getHeatQuantityUsedInOtherRejections() != null ? 
            PrecisionUtils.roundQuantity(Double.parseDouble(forgeHeatRepresentation.getHeatQuantityUsedInOtherRejections())) : null)
        .rejectedPieces(forgeHeatRepresentation.getRejectedPieces() != null ? 
            Integer.parseInt(forgeHeatRepresentation.getRejectedPieces()) : null)
        .build();
  }

  /**
   * Creates a ForgeHeat entity from ForgeHeatRepresentation when only heatId is provided
   * This is used for new forge heats added during endForge operation
   */
  public ForgeHeat createAssembleFromHeatId(ForgeHeatRepresentation forgeHeatRepresentation){
    return ForgeHeat.builder()
        .heat(heatService.getRawMaterialHeatById(forgeHeatRepresentation.getHeatId()))
        .heatQuantityUsed(PrecisionUtils.roundQuantity(Double.parseDouble(forgeHeatRepresentation.getHeatQuantityUsed())))
        .heatQuantityUsedInRejectedPieces(forgeHeatRepresentation.getHeatQuantityUsedInRejectedPieces() != null ? 
            PrecisionUtils.roundQuantity(Double.parseDouble(forgeHeatRepresentation.getHeatQuantityUsedInRejectedPieces())) : null)
        .heatQuantityUsedInOtherRejections(forgeHeatRepresentation.getHeatQuantityUsedInOtherRejections() != null ? 
            PrecisionUtils.roundQuantity(Double.parseDouble(forgeHeatRepresentation.getHeatQuantityUsedInOtherRejections())) : null)
        .rejectedPieces(forgeHeatRepresentation.getRejectedPieces() != null ? 
            Integer.parseInt(forgeHeatRepresentation.getRejectedPieces()) : null)
        .createdAt(LocalDateTime.now())
        .build();
  }

}
