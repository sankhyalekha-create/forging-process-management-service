package com.jangid.forging_process_management_service.assemblers.heating;

import com.jangid.forging_process_management_service.assemblers.inventory.RawMaterialHeatAssembler;
import com.jangid.forging_process_management_service.entities.heating.HeatTreatmentHeat;
import com.jangid.forging_process_management_service.entities.heating.ProcessedItemHeatTreatmentBatch;
import com.jangid.forging_process_management_service.entitiesRepresentation.heating.HeatTreatmentHeatRepresentation;
import com.jangid.forging_process_management_service.service.inventory.RawMaterialHeatService;
import com.jangid.forging_process_management_service.service.heating.ProcessedItemHeatTreatmentBatchService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class HeatTreatmentHeatAssembler {

  @Autowired
  private RawMaterialHeatAssembler rawMaterialHeatAssembler;

  @Autowired
  private RawMaterialHeatService rawMaterialHeatService;

  @Autowired
  @Lazy
  private ProcessedItemHeatTreatmentBatchService processedItemHeatTreatmentBatchService;

  @Autowired
  @Lazy
  private ProcessedItemHeatTreatmentBatchAssembler processedItemHeatTreatmentBatchAssembler;

  public HeatTreatmentHeat assemble(HeatTreatmentHeatRepresentation heatTreatmentHeatRepresentation, ProcessedItemHeatTreatmentBatch processedItemHeatTreatmentBatch) {
    HeatTreatmentHeat heatTreatmentHeat = HeatTreatmentHeat.builder()
        .processedItemHeatTreatmentBatch(processedItemHeatTreatmentBatch)
        .heat(rawMaterialHeatService.getRawMaterialHeatById(heatTreatmentHeatRepresentation.getHeat().getId()))
        .piecesUsed(heatTreatmentHeatRepresentation.getPiecesUsed())
        .createdAt(LocalDateTime.now())
        .deleted(false)
        .build();

    return heatTreatmentHeat;
  }

  public HeatTreatmentHeatRepresentation dissemble(HeatTreatmentHeat heatTreatmentHeat) {
    HeatTreatmentHeatRepresentation heatTreatmentHeatRepresentation = HeatTreatmentHeatRepresentation.builder()
        .id(heatTreatmentHeat.getId())
        .heat(rawMaterialHeatAssembler.dissemble(heatTreatmentHeat.getHeat()))
        .piecesUsed(heatTreatmentHeat.getPiecesUsed())
        .createdAt(heatTreatmentHeat.getCreatedAt() != null ? heatTreatmentHeat.getCreatedAt().toString() : null)
        .updatedAt(heatTreatmentHeat.getUpdatedAt() != null ? heatTreatmentHeat.getUpdatedAt().toString() : null)
        .deletedAt(heatTreatmentHeat.getDeletedAt() != null ? heatTreatmentHeat.getDeletedAt().toString() : null)
        .deleted(heatTreatmentHeat.isDeleted())
        .build();

    return heatTreatmentHeatRepresentation;
  }
} 