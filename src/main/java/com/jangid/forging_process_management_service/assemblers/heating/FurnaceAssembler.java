package com.jangid.forging_process_management_service.assemblers.heating;

import com.jangid.forging_process_management_service.entities.forging.Furnace;
import com.jangid.forging_process_management_service.entitiesRepresentation.forging.FurnaceRepresentation;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FurnaceAssembler {

  public FurnaceRepresentation dissemble(Furnace furnace) {
    FurnaceRepresentation representation = FurnaceRepresentation.builder()
        .id(furnace.getId())
        .furnaceName(furnace.getFurnaceName())
        .furnaceLocation(furnace.getFurnaceLocation())
        .furnaceCapacity(furnace.getFurnaceCapacity() != null ? String.valueOf(furnace.getFurnaceCapacity()) : null)
        .furnaceDetails(furnace.getFurnaceDetails())
        .furnaceStatus(furnace.getFurnaceStatus().name())
        .createdAt(furnace.getCreatedAt() != null ? furnace.getCreatedAt().toString() : null)
        .updatedAt(furnace.getUpdatedAt() != null ? furnace.getUpdatedAt().toString() : null)
        .build();

    return representation;
  }

  public Furnace assemble(FurnaceRepresentation representation) {
    if (representation == null) {
      return Furnace.builder().build();
    }
    return Furnace.builder()
        .furnaceName(representation.getFurnaceName())
        .furnaceCapacity(representation.getFurnaceCapacity() != null ? Double.parseDouble(representation.getFurnaceCapacity()) : null)
        .furnaceLocation(representation.getFurnaceLocation())
        .furnaceDetails(representation.getFurnaceDetails())
        .furnaceStatus(Furnace.FurnaceStatus.HEAT_TREATMENT_BATCH_NOT_APPLIED)
        .build();
  }

}
