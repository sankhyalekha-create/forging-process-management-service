package com.jangid.forging_process_management_service.assemblers.forging;

import com.jangid.forging_process_management_service.entities.forging.ForgeTraceability;
import com.jangid.forging_process_management_service.entitiesRepresentation.forging.ForgeTraceabilityRepresentation;

import java.time.LocalDateTime;

public class ForgeTraceabilityAssembler {

  public static ForgeTraceabilityRepresentation dissemble(ForgeTraceability forgeTraceability){
    return ForgeTraceabilityRepresentation.builder()
        .id(forgeTraceability.getId())
        .heatId(String.valueOf(forgeTraceability.getHeatId()))
        .forgingLineName(forgeTraceability.getForgingLine().getForgingLineName())
        .forgePieceWeight(String.valueOf(forgeTraceability.getForgePieceWeight()))
        .startAt(forgeTraceability.getStartAt().toString())
        .build();
  }

  public static ForgeTraceability assemble(ForgeTraceabilityRepresentation representation){
    return ForgeTraceability.builder()
        .startAt(LocalDateTime.now())
        .forgingStatus(ForgeTraceability.ForgeTraceabilityStatus.IN_PROGRESS)
        .forgePieceWeight(Float.valueOf(representation.getForgePieceWeight()))
        .heatId(Long.valueOf(representation.getHeatId())).build();
  }

}
