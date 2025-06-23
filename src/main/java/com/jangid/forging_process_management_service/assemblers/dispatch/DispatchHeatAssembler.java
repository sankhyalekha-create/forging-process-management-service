package com.jangid.forging_process_management_service.assemblers.dispatch;

import com.jangid.forging_process_management_service.assemblers.inventory.RawMaterialHeatAssembler;
import com.jangid.forging_process_management_service.entities.dispatch.DispatchHeat;
import com.jangid.forging_process_management_service.entitiesRepresentation.dispatch.DispatchHeatRepresentation;
import com.jangid.forging_process_management_service.service.inventory.RawMaterialHeatService;
import com.jangid.forging_process_management_service.service.dispatch.ProcessedItemDispatchBatchService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class DispatchHeatAssembler {

  @Autowired
  private RawMaterialHeatService rawMaterialHeatService;
  @Autowired
  private RawMaterialHeatAssembler rawMaterialHeatAssembler;

  @Autowired
  @Lazy
  private ProcessedItemDispatchBatchService processedItemDispatchBatchService;

  @Autowired
  @Lazy
  private ProcessedItemDispatchBatchAssembler processedItemDispatchBatchAssembler;

  public DispatchHeatRepresentation dissemble(DispatchHeat dispatchHeat){
    return DispatchHeatRepresentation.builder()
        .processedItemDispatchBatch(null) // Avoid circular dependency
        .id(dispatchHeat.getId())
        .heat(rawMaterialHeatAssembler.dissemble(dispatchHeat.getHeat()))
        .piecesUsed(dispatchHeat.getPiecesUsed())
        .createdAt(dispatchHeat.getCreatedAt() != null ? dispatchHeat.getCreatedAt().toString() : null)
        .updatedAt(dispatchHeat.getUpdatedAt() != null ? dispatchHeat.getUpdatedAt().toString() : null)
        .deletedAt(dispatchHeat.getDeletedAt() != null ? dispatchHeat.getDeletedAt().toString() : null)
        .deleted(dispatchHeat.isDeleted())
        .build();
  }

  public DispatchHeat createAssemble(DispatchHeatRepresentation dispatchHeatRepresentation){
    return DispatchHeat.builder()
        .heat(rawMaterialHeatService.getRawMaterialHeatById(dispatchHeatRepresentation.getHeat().getId()))
        .piecesUsed(dispatchHeatRepresentation.getPiecesUsed())
        .createdAt(LocalDateTime.now())
        .deleted(false)
        .build();
  }

  public DispatchHeat assemble(DispatchHeatRepresentation dispatchHeatRepresentation){
    return DispatchHeat.builder()
        .heat(rawMaterialHeatService.getRawMaterialHeatById(dispatchHeatRepresentation.getHeat().getId()))
        .piecesUsed(dispatchHeatRepresentation.getPiecesUsed())
        .createdAt(LocalDateTime.now())
        .deleted(false)
        .build();
  }
} 