package com.jangid.forging_process_management_service.assemblers;

import com.jangid.forging_process_management_service.entities.RawMaterial;
import com.jangid.forging_process_management_service.entities.RawMaterialHeat;
import com.jangid.forging_process_management_service.entitiesRepresentation.RawMaterialHeatRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.RawMaterialRepresentation;
import com.jangid.forging_process_management_service.utils.ConstantUtils;

import java.time.LocalDateTime;
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

  public static RawMaterial assemble(RawMaterialRepresentation rawMaterialRepresentation){
    List<RawMaterialHeat> heats = new ArrayList<>();
    rawMaterialRepresentation.getHeats().forEach(h -> heats.add(RawMaterialHeat.builder()
                                                                    .id(h.getId())
                                                                    .heatNumber(h.getHeatNumber())
                                                                    .heatQuantity(Float.valueOf(h.getHeatQuantity())).build()));
    RawMaterial rawMaterial =  RawMaterial.builder()
        .id(rawMaterialRepresentation.getId())
        .rawMaterialInvoiceNumber(rawMaterialRepresentation.getRawMaterialInvoiceNumber())
        .rawMaterialTotalQuantity(rawMaterialRepresentation.getRawMaterialTotalQuantity())
        .rawMaterialReceivingDate(rawMaterialRepresentation.getRawMaterialReceivingDate()!= null ? LocalDateTime.parse(rawMaterialRepresentation.getRawMaterialReceivingDate(), ConstantUtils.DATE_TIME_FORMATTER) : null)
        .rawMaterialInputCode(rawMaterialRepresentation.getRawMaterialInputCode())
        .rawMaterialHsnCode(rawMaterialRepresentation.getRawMaterialHsnCode())
        .rawMaterialGoodsDescription(rawMaterialRepresentation.getRawMaterialGoodsDescription())
        .heats(heats)
        .createdAt(rawMaterialRepresentation.getCreatedAt() != null ? LocalDateTime.parse(rawMaterialRepresentation.getCreatedAt(), ConstantUtils.DATE_TIME_FORMATTER) : null)
        .updatedAt(rawMaterialRepresentation.getUpdatedAt() != null ? LocalDateTime.parse(rawMaterialRepresentation.getUpdatedAt(), ConstantUtils.DATE_TIME_FORMATTER) : null)
        .build();
    return rawMaterial;
  }

}
