package com.jangid.forging_process_management_service.assemblers.inventory;

import com.jangid.forging_process_management_service.assemblers.product.ProductAssembler;
import com.jangid.forging_process_management_service.entities.inventory.Heat;
import com.jangid.forging_process_management_service.entities.inventory.RawMaterialProduct;
import com.jangid.forging_process_management_service.entitiesRepresentation.inventory.RawMaterialProductRepresentation;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class RawMaterialProductAssembler {

  public static RawMaterialProductRepresentation dissemble(RawMaterialProduct rawMaterialProduct) {
    return RawMaterialProductRepresentation.builder()
        .id(rawMaterialProduct.getId())
        .rawMaterialId(String.valueOf(rawMaterialProduct.getRawMaterial().getId()))
        .product(ProductAssembler.dissemble(rawMaterialProduct.getProduct()))
        .heats(RawMaterialHeatAssembler.getHeatRepresentations(rawMaterialProduct.getHeats()))
        .createdAt(rawMaterialProduct.getCreatedAt() != null ? rawMaterialProduct.getCreatedAt().toString() : null).build();
  }

  public static RawMaterialProduct createAssemble(RawMaterialProductRepresentation rawMaterialProductRepresentation) {
    List<Heat> heats = RawMaterialHeatAssembler.getCreateHeats(rawMaterialProductRepresentation.getHeats());
    RawMaterialProduct rawMaterialProduct =  RawMaterialProduct.builder()
        .id(rawMaterialProductRepresentation.getId())
        .build();
    heats.forEach(rawMaterialProduct::setRawMaterialProduct);
    if(rawMaterialProduct.getHeats()!=null){
      rawMaterialProduct.getHeats().clear();
    }
    rawMaterialProduct.setHeats(heats);
    return rawMaterialProduct;
  }

  public static RawMaterialProduct assemble(RawMaterialProductRepresentation rawMaterialProductRepresentation) {
    List<Heat> heats = RawMaterialHeatAssembler.getHeats(rawMaterialProductRepresentation.getHeats());
    RawMaterialProduct rawMaterialProduct =  RawMaterialProduct.builder()
        .id(rawMaterialProductRepresentation.getId())
        .build();
    heats.forEach(rawMaterialProduct::setRawMaterialProduct);
    if(rawMaterialProduct.getHeats()!=null){
      rawMaterialProduct.getHeats().clear();
    }
    rawMaterialProduct.setHeats(heats);
    return rawMaterialProduct;
  }

  public static List<RawMaterialProduct> getRawMaterialProducts(List<RawMaterialProductRepresentation> representationList){
    return representationList.stream()
        .map(RawMaterialProductAssembler::assemble)
        .collect(Collectors.toList());
  }

}
