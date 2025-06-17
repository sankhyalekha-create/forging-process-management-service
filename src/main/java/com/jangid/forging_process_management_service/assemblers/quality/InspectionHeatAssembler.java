package com.jangid.forging_process_management_service.assemblers.quality;

import com.jangid.forging_process_management_service.assemblers.inventory.RawMaterialHeatAssembler;
import com.jangid.forging_process_management_service.entities.quality.InspectionHeat;
import com.jangid.forging_process_management_service.entities.quality.ProcessedItemInspectionBatch;
import com.jangid.forging_process_management_service.entitiesRepresentation.quality.InspectionHeatRepresentation;
import com.jangid.forging_process_management_service.service.inventory.RawMaterialHeatService;
import com.jangid.forging_process_management_service.service.quality.ProcessedItemInspectionBatchService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class InspectionHeatAssembler {

  @Autowired
  private RawMaterialHeatService rawMaterialHeatService;
  @Autowired
  private RawMaterialHeatAssembler rawMaterialHeatAssembler;

  @Autowired
  @Lazy
  private ProcessedItemInspectionBatchService processedItemInspectionBatchService;

  @Autowired
  @Lazy
  private ProcessedItemInspectionBatchAssembler processedItemInspectionBatchAssembler;

  public InspectionHeatRepresentation dissemble(InspectionHeat inspectionHeat){
    return InspectionHeatRepresentation.builder()
        .processedItemInspectionBatch(processedItemInspectionBatchAssembler.dissemble(inspectionHeat.getProcessedItemInspectionBatch()))
        .id(inspectionHeat.getId())
        .heat(rawMaterialHeatAssembler.dissemble(inspectionHeat.getHeat()))
        .piecesUsed(inspectionHeat.getPiecesUsed())
        .createdAt(inspectionHeat.getCreatedAt() != null ? inspectionHeat.getCreatedAt().toString() : null)
        .updatedAt(inspectionHeat.getUpdatedAt() != null ? inspectionHeat.getUpdatedAt().toString() : null)
        .deletedAt(inspectionHeat.getDeletedAt() != null ? inspectionHeat.getDeletedAt().toString() : null)
        .deleted(inspectionHeat.isDeleted())
        .build();
  }

  public InspectionHeat createAssemble(InspectionHeatRepresentation inspectionHeatRepresentation){
    return InspectionHeat.builder()
        .heat(rawMaterialHeatService.getRawMaterialHeatById(inspectionHeatRepresentation.getHeat().getId()))
        .piecesUsed(inspectionHeatRepresentation.getPiecesUsed())
        .createdAt(LocalDateTime.now())
        .deleted(false)
        .build();
  }

  public InspectionHeat assemble(InspectionHeatRepresentation inspectionHeatRepresentation, ProcessedItemInspectionBatch processedItemInspectionBatch){
    return InspectionHeat.builder()
        .processedItemInspectionBatch(processedItemInspectionBatch)
        .heat(rawMaterialHeatService.getRawMaterialHeatById(inspectionHeatRepresentation.getHeat().getId()))
        .piecesUsed(inspectionHeatRepresentation.getPiecesUsed())
        .createdAt(LocalDateTime.now())
        .deleted(false)
        .build();
  }
} 