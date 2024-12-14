package com.jangid.forging_process_management_service.assemblers.heating;

import com.jangid.forging_process_management_service.entities.forging.Furnace;
import com.jangid.forging_process_management_service.entitiesRepresentation.forging.FurnaceRepresentation;

public class FurnaceAssembler {

  public static FurnaceRepresentation dissemble(Furnace furnace){
    FurnaceRepresentation representation = FurnaceRepresentation.builder()
        .id(furnace.getId())
        .furnaceName(furnace.getFurnaceName())
        .furnaceLocation(furnace.getFurnaceLocation())
        .furnaceCapacity(String.valueOf(furnace.getFurnaceCapacity()))
        .furnaceDetails(furnace.getFurnaceDetails())
        .furnaceStatus(furnace.getFurnaceStatus().name())
        .createdAt(furnace.getCreatedAt() != null ? furnace.getCreatedAt().toString() : null)
        .updatedAt(furnace.getUpdatedAt() != null ? furnace.getUpdatedAt().toString() : null)
        .build();

    return representation;
  }

  public static Furnace assemble(FurnaceRepresentation representation){
    if (representation == null){
      return Furnace.builder().build();
    }
    return Furnace.builder()
        .furnaceName(representation.getFurnaceName())
        .furnaceCapacity(Double.valueOf(representation.getFurnaceCapacity()))
        .furnaceLocation(representation.getFurnaceLocation())
        .furnaceDetails(representation.getFurnaceDetails())
        .furnaceStatus(Furnace.FurnaceStatus.HEAT_TREATMENT_BATCH_NOT_APPLIED)
        .build();
  }

}
