package com.jangid.forging_process_management_service.assemblers.forging;

import com.jangid.forging_process_management_service.entities.forging.ForgingLine;
import com.jangid.forging_process_management_service.entitiesRepresentation.forging.ForgingLineRepresentation;

public class ForgingLineAssembler {

  public static ForgingLineRepresentation dissemble(ForgingLine forgingLine){
    ForgingLineRepresentation representation = ForgingLineRepresentation.builder()
        .id(forgingLine.getId())
        .forgingLineName(forgingLine.getForgingLineName())
        .forgingLineStatus(forgingLine.getForgingLineStatus().name())
        .forgingDetails(forgingLine.getForgingDetails())
        .createdAt(forgingLine.getCreatedAt() != null ? forgingLine.getCreatedAt().toString() : null)
        .updatedAt(forgingLine.getUpdatedAt() != null ? forgingLine.getUpdatedAt().toString() : null).build();

    return representation;
  }

  public static ForgingLine assemble(ForgingLineRepresentation forgingLineRepresentation){
    if (forgingLineRepresentation == null){
      return ForgingLine.builder().build();
    }

    return ForgingLine.builder()
        .forgingLineName(forgingLineRepresentation.getForgingLineName())
        .forgingDetails(forgingLineRepresentation.getForgingDetails())
        .forgingLineStatus(ForgingLine.ForgingLineStatus.FORGE_NOT_APPLIED)
        .build();
  }

}
