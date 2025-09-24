package com.jangid.forging_process_management_service.assemblers.quality;

import com.jangid.forging_process_management_service.entities.quality.EquipmentGroup;
import com.jangid.forging_process_management_service.entities.quality.EquipmentGroupGauge;
import com.jangid.forging_process_management_service.entitiesRepresentation.quality.EquipmentGroupRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.quality.EquipmentGroupListRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.quality.GaugeRepresentation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class EquipmentGroupAssembler {

  @Autowired
  private GaugeAssembler gaugeAssembler;

  public EquipmentGroupRepresentation toRepresentation(EquipmentGroup equipmentGroup) {
    if (equipmentGroup == null) {
      return null;
    }

    List<GaugeRepresentation> gaugeRepresentations = equipmentGroup.getEquipmentGroupGauges()
      .stream()
      .filter(egg -> !egg.isDeleted())
      .map(EquipmentGroupGauge::getGauge)
      .filter(gauge -> !gauge.isDeleted())
      .map(gaugeAssembler::dissemble)
      .collect(Collectors.toList());

    return EquipmentGroupRepresentation.builder()
      .id(equipmentGroup.getId())
      .groupName(equipmentGroup.getGroupName())
      .groupDescription(equipmentGroup.getGroupDescription())
      .gauges(gaugeRepresentations)
      .createdAt(equipmentGroup.getCreatedAt())
      .updatedAt(equipmentGroup.getUpdatedAt())
      .build();
  }

  public EquipmentGroupListRepresentation toListRepresentation(List<EquipmentGroup> equipmentGroups) {
    if (equipmentGroups == null) {
      return EquipmentGroupListRepresentation.builder()
        .equipmentGroups(List.of())
        .build();
    }

    List<EquipmentGroupRepresentation> representations = equipmentGroups.stream()
      .map(this::toRepresentation)
      .collect(Collectors.toList());

    return EquipmentGroupListRepresentation.builder()
      .equipmentGroups(representations)
      .build();
  }
}
