package com.jangid.forging_process_management_service.assemblers.forging;

import com.jangid.forging_process_management_service.assemblers.product.ItemAssembler;
import com.jangid.forging_process_management_service.entities.ProcessedItem;
import com.jangid.forging_process_management_service.entities.forging.Forge;
import com.jangid.forging_process_management_service.entities.forging.ForgeHeat;
import com.jangid.forging_process_management_service.entities.forging.ForgeShift;
import com.jangid.forging_process_management_service.entities.forging.ItemWeightType;
import com.jangid.forging_process_management_service.entitiesRepresentation.ProcessedItemRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.forging.ForgeHeatRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.forging.ForgeRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.forging.ForgeShiftRepresentation;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ForgeAssembler {

  @Autowired
  private ForgeHeatAssembler forgeHeatAssembler;

  @Autowired
  private ItemAssembler itemAssembler;

  @Autowired
  private ForgeShiftAssembler forgeShiftAssembler;

  public ForgeRepresentation dissemble(Forge forge) {
    ProcessedItemRepresentation processedItemRepresentation = null;
    if (forge != null) {
      ProcessedItem processedItem = forge.getProcessedItem();
      processedItemRepresentation = ProcessedItemRepresentation.builder()
          .id(processedItem.getId())
//        .forge(forgeAssembler.dissemble(processedItem.getForge()))
          .item(itemAssembler.dissembleBasic(processedItem.getItem()))
          .expectedForgePiecesCount(processedItem.getExpectedForgePiecesCount())
          .actualForgePiecesCount(processedItem.getActualForgePiecesCount())
//          .availableForgePiecesCountForHeat(processedItem.getAvailableForgePiecesCountForHeat())
          .rejectedForgePiecesCount(processedItem.getRejectedForgePiecesCount())
          .otherForgeRejectionsKg(processedItem.getOtherForgeRejectionsKg())
          .workflowIdentifier(processedItem.getWorkflowIdentifier())
          .itemWorkflowId(processedItem.getItemWorkflowId())
          .build();
      
      // Determine if there were rejections
      boolean hasRejections = (processedItem.getRejectedForgePiecesCount() != null && processedItem.getRejectedForgePiecesCount() > 0) || 
                              (processedItem.getOtherForgeRejectionsKg() != null && processedItem.getOtherForgeRejectionsKg() > 0);

      return ForgeRepresentation.builder()
          .id(forge.getId())
          .forgeTraceabilityNumber(forge.getForgeTraceabilityNumber())
          .processedItem(processedItemRepresentation)
          .applyAt(forge.getApplyAt() != null ? forge.getApplyAt().toString() : null)
          .startAt(forge.getStartAt() != null ? forge.getStartAt().toString() : null)
          .endAt(forge.getEndAt() != null ? forge.getEndAt().toString() : null)
          .forgingLine(ForgingLineAssembler.dissemble(forge.getForgingLine()))
          .forgingStatus(forge.getForgingStatus().name())
          .itemWeightType(forge.getItemWeightType() != null ? forge.getItemWeightType().name() : null)
          .forgeHeats(getForgeHeatRepresentations(forge.getForgeHeats()))
          .forgeShifts(getForgeShiftRepresentations(forge.getForgeShifts()))
          .rejectedForgePiecesCount(processedItem.getRejectedForgePiecesCount() != null ? 
                                    processedItem.getRejectedForgePiecesCount().toString() : null)
          .otherForgeRejectionsKg(processedItem.getOtherForgeRejectionsKg() != null ? 
                                  processedItem.getOtherForgeRejectionsKg().toString() : null)
          .rejection(hasRejections)
          .build();
    }
    return null;
  }

  public Forge createAssemble(ForgeRepresentation forgeRepresentation) {
    List<ForgeHeat> forgeHeats = forgeRepresentation.getForgeHeats().stream().map(forgeHeatAssembler::createAssemble).collect(Collectors.toList());
    return Forge.builder()
        .forgingStatus(Forge.ForgeStatus.IDLE)
        .itemWeightType(ItemWeightType.fromString(forgeRepresentation.getItemWeightType()))
        .forgeHeats(forgeHeats)
        .createdAt(LocalDateTime.now())
        .build();
  }

  public Forge assemble(ForgeRepresentation forgeRepresentation) {
    if (forgeRepresentation == null) {
      return null;
    }
    List<ForgeHeat> forgeHeats = forgeRepresentation.getForgeHeats().stream().map(forgeHeatAssembler::assemble).collect(Collectors.toList());
    return Forge.builder()
        .forgingStatus(Forge.ForgeStatus.IDLE)
        .forgeTraceabilityNumber(forgeRepresentation.getForgeTraceabilityNumber())
        .itemWeightType(ItemWeightType.fromString(forgeRepresentation.getItemWeightType()))
        .forgeHeats(forgeHeats)
        .build();
  }

  private List<ForgeHeatRepresentation> getForgeHeatRepresentations(List<ForgeHeat> forgeHeats) {
    return forgeHeats.stream().map(forgeHeat -> forgeHeatAssembler.dissemble(forgeHeat)).collect(Collectors.toList());
  }

  private List<ForgeShiftRepresentation> getForgeShiftRepresentations(List<ForgeShift> forgeShifts) {
    if (forgeShifts == null || forgeShifts.isEmpty()) {
      return null;
    }
    return forgeShifts.stream()
        .filter(forgeShift -> !forgeShift.isDeleted())
        .map(forgeShiftAssembler::dissemble)
        .collect(Collectors.toList());
  }
}
