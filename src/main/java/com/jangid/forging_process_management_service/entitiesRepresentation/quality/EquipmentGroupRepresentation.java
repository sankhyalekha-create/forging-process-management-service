package com.jangid.forging_process_management_service.entitiesRepresentation.quality;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EquipmentGroupRepresentation {

  private Long id;
  private String groupName;
  private String groupDescription;
  private List<GaugeRepresentation> gauges;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
