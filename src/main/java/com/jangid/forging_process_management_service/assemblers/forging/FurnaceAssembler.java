package com.jangid.forging_process_management_service.assemblers.forging;

import com.jangid.forging_process_management_service.entities.forging.Furnace;
import com.jangid.forging_process_management_service.entitiesRepresentation.forging.FurnaceRepresentation;

public class FurnaceAssembler {

  public static FurnaceRepresentation dissemble(Furnace furnace){
    FurnaceRepresentation representation = FurnaceRepresentation.builder()
        .id(furnace.getId())
        .furnaceName(furnace.getFurnaceName())
        .furnaceLocation(furnace.getFurnaceLocation())
        .furnaceCapacity(furnace.getFurnaceCapacity())
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
        .furnaceCapacity(representation.getFurnaceCapacity())
        .furnaceLocation(representation.getFurnaceLocation())
        .furnaceDetails(representation.getFurnaceDetails())
        .furnaceStatus(Furnace.FurnaceStatus.IDLE)
        .build();
  }

}
