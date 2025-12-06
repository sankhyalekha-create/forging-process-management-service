package com.jangid.forging_process_management_service.assemblers.forging;

import com.jangid.forging_process_management_service.entities.forging.ForgeShift;
import com.jangid.forging_process_management_service.entities.forging.ForgeShiftHeat;
import com.jangid.forging_process_management_service.entitiesRepresentation.forging.ForgeShiftRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.forging.ForgeShiftHeatRepresentation;
import com.jangid.forging_process_management_service.utils.PrecisionUtils;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ForgeShiftAssembler {

  @Autowired
  private ForgeShiftHeatAssembler forgeShiftHeatAssembler;

  public ForgeShiftRepresentation dissemble(ForgeShift forgeShift) {
    if (forgeShift == null) {
      return null;
    }

    return ForgeShiftRepresentation.builder()
        .id(forgeShift.getId())
        .forgeId(forgeShift.getForge().getId())
        .startDateTime(forgeShift.getStartDateTime() != null ? forgeShift.getStartDateTime().toString() : null)
        .endDateTime(forgeShift.getEndDateTime() != null ? forgeShift.getEndDateTime().toString() : null)
        .forgeShiftHeats(getForgeShiftHeatRepresentations(forgeShift.getForgeShiftHeats()))
        .actualForgedPiecesCount(forgeShift.getActualForgedPiecesCount() != null ? 
                                 forgeShift.getActualForgedPiecesCount().toString() : null)
        .rejectedForgePiecesCount(forgeShift.getRejectedForgePiecesCount() != null ? 
                                  forgeShift.getRejectedForgePiecesCount().toString() : null)
        .otherForgeRejectionsKg(forgeShift.getOtherForgeRejectionsKg() != null ? 
                                forgeShift.getOtherForgeRejectionsKg().toString() : null)
        .rejection(forgeShift.getRejection())
        .createdAt(forgeShift.getCreatedAt() != null ? forgeShift.getCreatedAt().toString() : null)
        .updatedAt(forgeShift.getUpdatedAt() != null ? forgeShift.getUpdatedAt().toString() : null)
        .deletedAt(forgeShift.getDeletedAt() != null ? forgeShift.getDeletedAt().toString() : null)
        .build();
  }

  public ForgeShift createAssemble(ForgeShiftRepresentation forgeShiftRepresentation) {
    if (forgeShiftRepresentation == null) {
      return null;
    }

    List<ForgeShiftHeat> forgeShiftHeats = forgeShiftRepresentation.getForgeShiftHeats() != null ? 
        forgeShiftRepresentation.getForgeShiftHeats().stream()
            .map(forgeShiftHeatAssembler::createAssemble)
            .collect(Collectors.toList()) : null;

    return ForgeShift.builder()
        .startDateTime(forgeShiftRepresentation.getStartDateTime() != null ? 
                       LocalDateTime.parse(forgeShiftRepresentation.getStartDateTime()) : null)
        .endDateTime(forgeShiftRepresentation.getEndDateTime() != null ? 
                     LocalDateTime.parse(forgeShiftRepresentation.getEndDateTime()) : null)
        .forgeShiftHeats(forgeShiftHeats)
        .actualForgedPiecesCount(forgeShiftRepresentation.getActualForgedPiecesCount() != null ? 
                                 Integer.parseInt(forgeShiftRepresentation.getActualForgedPiecesCount()) : null)
        .rejectedForgePiecesCount(forgeShiftRepresentation.getRejectedForgePiecesCount() != null ? 
                                  Integer.parseInt(forgeShiftRepresentation.getRejectedForgePiecesCount()) : 0)
        .otherForgeRejectionsKg(forgeShiftRepresentation.getOtherForgeRejectionsKg() != null ? 
                                PrecisionUtils.roundQuantity(Double.parseDouble(forgeShiftRepresentation.getOtherForgeRejectionsKg())) : 0.0)
        .rejection(forgeShiftRepresentation.getRejection() != null ? forgeShiftRepresentation.getRejection() : false)
        .createdAt(LocalDateTime.now())
        .build();
  }

  public ForgeShift assemble(ForgeShiftRepresentation forgeShiftRepresentation) {
    if (forgeShiftRepresentation == null) {
      return null;
    }

    List<ForgeShiftHeat> forgeShiftHeats = forgeShiftRepresentation.getForgeShiftHeats() != null ? 
        forgeShiftRepresentation.getForgeShiftHeats().stream()
            .map(forgeShiftHeatAssembler::assemble)
            .collect(Collectors.toList()) : null;

    return ForgeShift.builder()
        .id(forgeShiftRepresentation.getId())
        .startDateTime(forgeShiftRepresentation.getStartDateTime() != null ? 
                       LocalDateTime.parse(forgeShiftRepresentation.getStartDateTime()) : null)
        .endDateTime(forgeShiftRepresentation.getEndDateTime() != null ? 
                     LocalDateTime.parse(forgeShiftRepresentation.getEndDateTime()) : null)
        .forgeShiftHeats(forgeShiftHeats)
        .actualForgedPiecesCount(forgeShiftRepresentation.getActualForgedPiecesCount() != null ? 
                                 Integer.parseInt(forgeShiftRepresentation.getActualForgedPiecesCount()) : null)
        .rejectedForgePiecesCount(forgeShiftRepresentation.getRejectedForgePiecesCount() != null ? 
                                  Integer.parseInt(forgeShiftRepresentation.getRejectedForgePiecesCount()) : 0)
        .otherForgeRejectionsKg(forgeShiftRepresentation.getOtherForgeRejectionsKg() != null ? 
                                PrecisionUtils.roundQuantity(Double.parseDouble(forgeShiftRepresentation.getOtherForgeRejectionsKg())) : 0.0)
        .rejection(forgeShiftRepresentation.getRejection() != null ? forgeShiftRepresentation.getRejection() : false)
        .build();
  }

  private List<ForgeShiftHeatRepresentation> getForgeShiftHeatRepresentations(List<ForgeShiftHeat> forgeShiftHeats) {
    if (forgeShiftHeats == null) {
      return null;
    }
    return forgeShiftHeats.stream()
        .map(forgeShiftHeatAssembler::dissemble)
        .collect(Collectors.toList());
  }
} 