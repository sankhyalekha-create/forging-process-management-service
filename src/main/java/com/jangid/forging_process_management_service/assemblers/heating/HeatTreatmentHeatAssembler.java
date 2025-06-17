package com.jangid.forging_process_management_service.assemblers.heating;

import com.jangid.forging_process_management_service.assemblers.inventory.RawMaterialHeatAssembler;
import com.jangid.forging_process_management_service.entities.heating.HeatTreatmentHeat;
import com.jangid.forging_process_management_service.entitiesRepresentation.heating.HeatTreatmentHeatRepresentation;
import com.jangid.forging_process_management_service.service.inventory.RawMaterialHeatService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class HeatTreatmentHeatAssembler {

  @Autowired
  private RawMaterialHeatService heatService;
  @Autowired
  private RawMaterialHeatAssembler rawMaterialHeatAssembler;

  public HeatTreatmentHeatRepresentation dissemble(HeatTreatmentHeat heatTreatmentHeat){
    return HeatTreatmentHeatRepresentation.builder()
        .heatTreatmentBatchId(String.valueOf(heatTreatmentHeat.getHeatTreatmentBatch().getId()))
        .id(heatTreatmentHeat.getId())
        .heat(rawMaterialHeatAssembler.dissemble(heatTreatmentHeat.getHeat()))
        .piecesUsed(heatTreatmentHeat.getPiecesUsed())
        .build();
  }

  public HeatTreatmentHeat createAssemble(HeatTreatmentHeatRepresentation heatTreatmentHeatRepresentation){
    return HeatTreatmentHeat.builder()
        .heat(heatService.getRawMaterialHeatById(heatTreatmentHeatRepresentation.getHeat().getId()))
        .piecesUsed(heatTreatmentHeatRepresentation.getPiecesUsed())
        .createdAt(LocalDateTime.now())
        .build();
  }

  public HeatTreatmentHeat assemble(HeatTreatmentHeatRepresentation heatTreatmentHeatRepresentation){
    return HeatTreatmentHeat.builder()
        .heat(heatService.getRawMaterialHeatById(heatTreatmentHeatRepresentation.getHeat().getId()))
        .piecesUsed(heatTreatmentHeatRepresentation.getPiecesUsed())
        .build();
  }

} 