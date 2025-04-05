package com.jangid.forging_process_management_service.assemblers.machining;

import com.jangid.forging_process_management_service.assemblers.inventory.RawMaterialHeatAssembler;
import com.jangid.forging_process_management_service.entities.machining.MachiningHeat;
import com.jangid.forging_process_management_service.entitiesRepresentation.machining.MachiningHeatRepresentation;
import com.jangid.forging_process_management_service.service.inventory.RawMaterialHeatService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class MachiningHeatAssembler {

  @Autowired
  private RawMaterialHeatService heatService;
  @Autowired
  private RawMaterialHeatAssembler rawMaterialHeatAssembler;

  public MachiningHeatRepresentation dissemble(MachiningHeat machiningHeat){
    return MachiningHeatRepresentation.builder()
        .machiningBatchId(String.valueOf(machiningHeat.getMachiningBatch().getId()))
        .id(machiningHeat.getId())
        .heat(rawMaterialHeatAssembler.dissemble(machiningHeat.getHeat()))
        .piecesUsed(machiningHeat.getPiecesUsed())
        .build();
  }

  public MachiningHeat createAssemble(MachiningHeatRepresentation machiningHeatRepresentation){
    return MachiningHeat.builder()
        .heat(heatService.getRawMaterialHeatById(machiningHeatRepresentation.getHeat().getId()))
        .piecesUsed(machiningHeatRepresentation.getPiecesUsed())
        .createdAt(LocalDateTime.now())
        .build();
  }

  public MachiningHeat assemble(MachiningHeatRepresentation machiningHeatRepresentation){
    return MachiningHeat.builder()
        .heat(heatService.getRawMaterialHeatById(machiningHeatRepresentation.getHeat().getId()))
        .piecesUsed(machiningHeatRepresentation.getPiecesUsed())
        .build();
  }

}
