package com.jangid.forging_process_management_service.entitiesRepresentation.quality;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EquipmentGroupListRepresentation {

  private List<EquipmentGroupRepresentation> equipmentGroups;
}
