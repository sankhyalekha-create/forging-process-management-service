package com.jangid.forging_process_management_service.assemblers;

import com.jangid.forging_process_management_service.entities.RawMaterial;
import com.jangid.forging_process_management_service.entitiesRepresentation.RawMaterialHeatRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.RawMaterialRepresentation;

import java.util.ArrayList;
import java.util.List;

public class RawMaterialAssembler {

  public static RawMaterialRepresentation dissemble(RawMaterial rawMaterial){

    List<RawMaterialHeatRepresentation> heatRepresentations = new ArrayList<>();
    rawMaterial.getHeats().forEach(h -> heatRepresentations.add(RawMaterialHeatRepresentation.builder()
                                                                             .id(h.getId())
                                                                             .heatNumber(h.getHeatNumber())
                                                                             .heatQuantity(String.valueOf(h.getHeatQuantity())).build()));
    RawMaterialRepresentation representation =  RawMaterialRepresentation.builder()
        .id(rawMaterial.getId())
        .rawMaterialInvoiceNumber(rawMaterial.getRawMaterialInvoiceNumber())
        .rawMaterialTotalQuantity(rawMaterial.getRawMaterialTotalQuantity())
        .rawMaterialReceivingDate(rawMaterial.getRawMaterialReceivingDate()!= null ? rawMaterial.getRawMaterialReceivingDate().toString() : null)
        .rawMaterialInputCode(rawMaterial.getRawMaterialInputCode())
        .rawMaterialHsnCode(rawMaterial.getRawMaterialHsnCode())
        .rawMaterialGoodsDescription(rawMaterial.getRawMaterialGoodsDescription())
        .heats(heatRepresentations)
        .createdAt(rawMaterial.getCreatedAt() != null ? rawMaterial.getCreatedAt().toString() : null)
        .updatedAt(rawMaterial.getUpdatedAt() != null ? rawMaterial.getUpdatedAt().toString() : null)
        .build();
    return representation;
  }

}
