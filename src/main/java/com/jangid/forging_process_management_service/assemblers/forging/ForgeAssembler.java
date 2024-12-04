package com.jangid.forging_process_management_service.assemblers.forging;

import com.jangid.forging_process_management_service.assemblers.product.ItemAssembler;
import com.jangid.forging_process_management_service.entities.forging.Forge;
import com.jangid.forging_process_management_service.entities.forging.ForgeHeat;
import com.jangid.forging_process_management_service.entitiesRepresentation.forging.ForgeHeatRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.forging.ForgeRepresentation;
import com.jangid.forging_process_management_service.service.inventory.RawMaterialHeatService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class ForgeAssembler {

  @Autowired
  private RawMaterialHeatService rawMaterialHeatService;

  @Autowired
  private ItemAssembler itemAssembler;

  @Autowired
  private ForgeHeatAssembler forgeHeatAssembler;

  public ForgeRepresentation dissemble(Forge forge) {
    return ForgeRepresentation.builder()
        .id(forge.getId())
        .forgeTraceabilityNumber(forge.getForgeTraceabilityNumber())
        .item(itemAssembler.dissemble(forge.getItem()))
        .startAt(forge.getStartAt() != null ? forge.getStartAt().toString() : null)
        .endAt(forge.getEndAt() != null ? forge.getEndAt().toString() : null)
        .forgingLine(ForgingLineAssembler.dissemble(forge.getForgingLine()))
        .forgeCount(String.valueOf(forge.getForgeCount()))
        .actualForgeCount(String.valueOf(forge.getActualForgeCount()))
        .forgingStatus(forge.getForgingStatus().name())
        .forgeHeats(getForgeHeatRepresentations(forge.getForgeHeats()))
        .build();
  }

  public Forge createAssemble(ForgeRepresentation forgeRepresentation) {
    List<ForgeHeat> forgeHeats = forgeRepresentation.getForgeHeats().stream().map(forgeHeatAssembler::createAssemble).toList();
    return Forge.builder()
        .forgingStatus(Forge.ForgeStatus.IDLE)
        .forgeTraceabilityNumber(forgeRepresentation.getForgeTraceabilityNumber())
        .createdAt(LocalDateTime.now())
        .forgeHeats(forgeHeats)
        .build();
  }

  private List<ForgeHeatRepresentation> getForgeHeatRepresentations(List<ForgeHeat> forgeHeats) {
    return forgeHeats.stream().map(forgeHeat -> forgeHeatAssembler.dissemble(forgeHeat)).toList();
  }
}
