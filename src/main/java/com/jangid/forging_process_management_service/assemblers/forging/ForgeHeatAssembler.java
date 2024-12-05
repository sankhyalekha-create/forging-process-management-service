package com.jangid.forging_process_management_service.assemblers.forging;

import com.jangid.forging_process_management_service.assemblers.inventory.RawMaterialHeatAssembler;
import com.jangid.forging_process_management_service.entities.forging.ForgeHeat;
import com.jangid.forging_process_management_service.entitiesRepresentation.forging.ForgeHeatRepresentation;
import com.jangid.forging_process_management_service.service.inventory.RawMaterialHeatService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class ForgeHeatAssembler {

  @Autowired
  private RawMaterialHeatService heatService;

  public ForgeHeatRepresentation dissemble(ForgeHeat forgeHeat){
    return ForgeHeatRepresentation.builder()
        .forgeId(String.valueOf(forgeHeat.getForge().getId()))
        .id(forgeHeat.getId())
        .heat(RawMaterialHeatAssembler.dissemble(forgeHeat.getHeat()))
        .heatQuantityUsed(String.valueOf(forgeHeat.getHeatQuantityUsed()))
        .build();
  }

  public ForgeHeat createAssemble(ForgeHeatRepresentation forgeHeatRepresentation){
    return ForgeHeat.builder()
        .heat(heatService.getRawMaterialHeatById(forgeHeatRepresentation.getHeat().getId()))
        .heatQuantityUsed(Double.parseDouble(forgeHeatRepresentation.getHeatQuantityUsed()))
        .createdAt(LocalDateTime.now())
        .build();
  }

}
