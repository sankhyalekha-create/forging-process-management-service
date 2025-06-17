package com.jangid.forging_process_management_service.assemblers.machining;

import com.jangid.forging_process_management_service.assemblers.inventory.RawMaterialHeatAssembler;
import com.jangid.forging_process_management_service.entities.machining.MachiningHeat;
import com.jangid.forging_process_management_service.entities.machining.ProcessedItemMachiningBatch;
import com.jangid.forging_process_management_service.entitiesRepresentation.machining.MachiningHeatRepresentation;
import com.jangid.forging_process_management_service.service.inventory.RawMaterialHeatService;
import com.jangid.forging_process_management_service.service.machining.ProcessedItemMachiningBatchService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class MachiningHeatAssembler {

  @Autowired
  private RawMaterialHeatService rawMaterialHeatService;
  @Autowired
  private RawMaterialHeatAssembler rawMaterialHeatAssembler;

  @Autowired
  @Lazy
  private ProcessedItemMachiningBatchService processedItemMachiningBatchService;

  @Autowired
  @Lazy
  private ProcessedItemMachiningBatchAssembler processedItemMachiningBatchAssembler;

  public MachiningHeatRepresentation dissemble(MachiningHeat machiningHeat){
    return MachiningHeatRepresentation.builder()
        .processedItemMachiningBatch(processedItemMachiningBatchAssembler.dissemble(machiningHeat.getProcessedItemMachiningBatch()))
        .id(machiningHeat.getId())
        .heat(rawMaterialHeatAssembler.dissemble(machiningHeat.getHeat()))
        .piecesUsed(machiningHeat.getPiecesUsed())
        .createdAt(machiningHeat.getCreatedAt() != null ? machiningHeat.getCreatedAt().toString() : null)
        .updatedAt(machiningHeat.getUpdatedAt() != null ? machiningHeat.getUpdatedAt().toString() : null)
        .deletedAt(machiningHeat.getDeletedAt() != null ? machiningHeat.getDeletedAt().toString() : null)
        .deleted(machiningHeat.isDeleted())
        .build();
  }

  public MachiningHeat createAssemble(MachiningHeatRepresentation machiningHeatRepresentation){
    return MachiningHeat.builder()
        .heat(rawMaterialHeatService.getRawMaterialHeatById(machiningHeatRepresentation.getHeat().getId()))
        .piecesUsed(machiningHeatRepresentation.getPiecesUsed())
        .createdAt(LocalDateTime.now())
        .deleted(false)
        .build();
  }

  public MachiningHeat assemble(MachiningHeatRepresentation machiningHeatRepresentation, ProcessedItemMachiningBatch processedItemMachiningBatch){
    return MachiningHeat.builder()
        .processedItemMachiningBatch(processedItemMachiningBatch)
        .heat(rawMaterialHeatService.getRawMaterialHeatById(machiningHeatRepresentation.getHeat().getId()))
        .piecesUsed(machiningHeatRepresentation.getPiecesUsed())
        .createdAt(LocalDateTime.now())
        .deleted(false)
        .build();
  }
}
