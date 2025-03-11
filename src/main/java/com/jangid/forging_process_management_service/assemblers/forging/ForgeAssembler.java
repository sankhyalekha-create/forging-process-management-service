package com.jangid.forging_process_management_service.assemblers.forging;

import com.jangid.forging_process_management_service.assemblers.product.ItemAssembler;
import com.jangid.forging_process_management_service.entities.ProcessedItem;
import com.jangid.forging_process_management_service.entities.forging.Forge;
import com.jangid.forging_process_management_service.entities.forging.ForgeHeat;
import com.jangid.forging_process_management_service.entitiesRepresentation.ProcessedItemRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.forging.ForgeHeatRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.forging.ForgeRepresentation;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
public class ForgeAssembler {
  @Autowired
  private ForgeHeatAssembler forgeHeatAssembler;

  @Autowired
  private ItemAssembler itemAssembler;

  public ForgeRepresentation dissemble(Forge forge) {
    ProcessedItem processedItem =  forge.getProcessedItem();
    ProcessedItemRepresentation processedItemRepresentation = ProcessedItemRepresentation.builder()
        .id(processedItem.getId())
//        .forge(forgeAssembler.dissemble(processedItem.getForge()))
        .item(itemAssembler.dissemble(processedItem.getItem()))
        .expectedForgePiecesCount(processedItem.getExpectedForgePiecesCount())
        .actualForgePiecesCount(processedItem.getActualForgePiecesCount())
        .availableForgePiecesCountForHeat(processedItem.getAvailableForgePiecesCountForHeat())
        .build();
    return ForgeRepresentation.builder()
        .id(forge.getId())
        .forgeTraceabilityNumber(forge.getForgeTraceabilityNumber())
        .processedItem(processedItemRepresentation)
        .applyAt(forge.getApplyAt() != null ? forge.getApplyAt().toString() : null)
        .startAt(forge.getStartAt() != null ? forge.getStartAt().toString() : null)
        .endAt(forge.getEndAt() != null ? forge.getEndAt().toString() : null)
        .forgingLine(ForgingLineAssembler.dissemble(forge.getForgingLine()))
        .forgingStatus(forge.getForgingStatus().name())
        .forgeHeats(getForgeHeatRepresentations(forge.getForgeHeats()))
        .build();
  }

  public Forge createAssemble(ForgeRepresentation forgeRepresentation) {
    List<ForgeHeat> forgeHeats = forgeRepresentation.getForgeHeats().stream().map(forgeHeatAssembler::createAssemble).toList();
    return Forge.builder()
        .forgingStatus(Forge.ForgeStatus.IDLE)
        .forgeHeats(forgeHeats)
        .createdAt(LocalDateTime.now())
        .build();
  }

  public Forge assemble(ForgeRepresentation forgeRepresentation) {
    if(forgeRepresentation==null){
      return null;
    }
    List<ForgeHeat> forgeHeats = forgeRepresentation.getForgeHeats().stream().map(forgeHeatAssembler::assemble).toList();
    return Forge.builder()
        .forgingStatus(Forge.ForgeStatus.IDLE)
        .forgeTraceabilityNumber(forgeRepresentation.getForgeTraceabilityNumber())
        .forgeHeats(forgeHeats)
        .build();
  }

  private List<ForgeHeatRepresentation> getForgeHeatRepresentations(List<ForgeHeat> forgeHeats) {
    return forgeHeats.stream().map(forgeHeat -> forgeHeatAssembler.dissemble(forgeHeat)).toList();
  }
}
