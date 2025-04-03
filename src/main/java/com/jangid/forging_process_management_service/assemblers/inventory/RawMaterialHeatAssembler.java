package com.jangid.forging_process_management_service.assemblers.inventory;

import com.jangid.forging_process_management_service.entities.inventory.Heat;
import com.jangid.forging_process_management_service.entitiesRepresentation.inventory.HeatRepresentation;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class RawMaterialHeatAssembler {

  @Autowired
  private RawMaterialProductAssembler rawMaterialProductAssembler;

  public HeatRepresentation dissemble(Heat heat) {
    return HeatRepresentation.builder()
        .id(heat.getId())
        .heatNumber(heat.getHeatNumber())
        .heatQuantity(String.valueOf(heat.getHeatQuantity()))
        .availableHeatQuantity(String.valueOf(heat.getAvailableHeatQuantity()))
        .isInPieces(heat.getIsInPieces())
        .piecesCount(heat.getPiecesCount())
        .availablePiecesCount(heat.getAvailablePiecesCount())
        .testCertificateNumber(heat.getTestCertificateNumber())
        .location(heat.getLocation())
        .rawMaterialProduct(rawMaterialProductAssembler.dissemble(heat.getRawMaterialProduct()))
        .createdAt(heat.getCreatedAt() != null ? heat.getCreatedAt().toString() : null)
        .build();
  }

  public Heat assemble(HeatRepresentation heatRepresentation) {
    return Heat.builder()
        .heatNumber(heatRepresentation.getHeatNumber())
        .heatQuantity(heatRepresentation.getHeatQuantity() != null ? Double.valueOf(heatRepresentation.getHeatQuantity()) : null)
        .availableHeatQuantity(heatRepresentation.getAvailableHeatQuantity() != null ? Double.valueOf(heatRepresentation.getAvailableHeatQuantity()) : null)
        .isInPieces(heatRepresentation.getIsInPieces())
        .piecesCount(heatRepresentation.getPiecesCount())
        .availablePiecesCount(heatRepresentation.getAvailablePiecesCount())
        .testCertificateNumber(heatRepresentation.getTestCertificateNumber())
        .location(heatRepresentation.getLocation())
        .build();
  }

  public List<HeatRepresentation> getHeatRepresentations(List<Heat> heats) {
    List<HeatRepresentation> heatRepresentations = new ArrayList<>();
    heats.forEach(heat -> {
      heatRepresentations.add(dissemble(heat));
    });
    return heatRepresentations;
  }

  public List<Heat> getCreateHeats(List<HeatRepresentation> heatRepresentations) {
    List<Heat> heats = new ArrayList<>();
    heatRepresentations.forEach(heatRepresentation -> {
      Heat heat = assemble(heatRepresentation);
      heat.setCreatedAt(LocalDateTime.now());
      heats.add(heat);
    });
    return heats;
  }

  public List<Heat> getHeats(List<HeatRepresentation> heatRepresentations) {
    List<Heat> heats = new ArrayList<>();
    heatRepresentations.forEach(heatRepresentation -> {
      Heat heat = assemble(heatRepresentation);
      heats.add(heat);
    });
    return heats;
  }
}
