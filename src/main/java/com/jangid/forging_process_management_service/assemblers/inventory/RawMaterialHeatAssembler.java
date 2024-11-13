package com.jangid.forging_process_management_service.assemblers.inventory;

import com.jangid.forging_process_management_service.entities.inventory.Heat;
import com.jangid.forging_process_management_service.entitiesRepresentation.inventory.HeatRepresentation;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class RawMaterialHeatAssembler {

  public static HeatRepresentation dissemble(Heat heat) {
    return HeatRepresentation.builder()
        .id(heat.getId())
        .heatNumber(heat.getHeatNumber())
        .heatQuantity(String.valueOf(heat.getHeatQuantity()))
        .availableHeatQuantity(String.valueOf(heat.getAvailableHeatQuantity()))
        .testCertificateNumber(heat.getTestCertificateNumber())
        .location(heat.getLocation())
        .createdAt(heat.getCreatedAt() != null ? heat.getCreatedAt().toString() : null)
        .build();
  }

  public static Heat assemble(HeatRepresentation heatRepresentation) {
    return Heat.builder()
        .id(heatRepresentation.getId())
        .heatNumber(heatRepresentation.getHeatNumber())
        .heatQuantity(heatRepresentation.getHeatQuantity() != null ? Double.valueOf(heatRepresentation.getHeatQuantity()) : null)
        .testCertificateNumber(heatRepresentation.getTestCertificateNumber())
        .availableHeatQuantity(heatRepresentation.getAvailableHeatQuantity() != null ? Double.valueOf(heatRepresentation.getAvailableHeatQuantity())
                                                                                     : heatRepresentation.getHeatQuantity() != null ? Double.valueOf(heatRepresentation.getHeatQuantity()) : null)
        .location(heatRepresentation.getLocation())
        .build();
  }

  public static List<HeatRepresentation> getHeatRepresentations(List<Heat> heats) {
    List<HeatRepresentation> heatRepresentations = new ArrayList<>();
    heats.forEach(heat -> {
      heatRepresentations.add(dissemble(heat));
    });
    return heatRepresentations;
  }

  public static List<Heat> getCreateHeats(List<HeatRepresentation> heatRepresentations) {
    List<Heat> heats = new ArrayList<>();
    heatRepresentations.forEach(heatRepresentation -> {
      Heat heat = assemble(heatRepresentation);
      heat.setCreatedAt(LocalDateTime.now());
      heats.add(heat);
    });
    return heats;
  }

  public static List<Heat> getHeats(List<HeatRepresentation> heatRepresentations) {
    List<Heat> heats = new ArrayList<>();
    heatRepresentations.forEach(heatRepresentation -> {
      Heat heat = assemble(heatRepresentation);
      heats.add(heat);
    });
    return heats;
  }
}
