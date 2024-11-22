package com.jangid.forging_process_management_service.assemblers.product;

import com.jangid.forging_process_management_service.entities.product.ItemProduct;
import com.jangid.forging_process_management_service.entitiesRepresentation.product.ItemProductRepresentation;

public class ItemProductAssembler {

  public static ItemProductRepresentation dissemble(ItemProduct itemProduct) {
    return ItemProductRepresentation.builder()
        .id(itemProduct.getId())
        .item(ItemAssembler.dissemble(itemProduct.getItem()))
        .product(ProductAssembler.dissemble(itemProduct.getProduct()))
        .createdAt(itemProduct.getCreatedAt() != null ? itemProduct.getCreatedAt().toString() : null).build();
  }

  public static ItemProduct assemble(ItemProductRepresentation itemProductRepresentation) {
    return ItemProduct.builder()
        .item(ItemAssembler.assemble(itemProductRepresentation.getItem()))
        .product(ProductAssembler.assemble(itemProductRepresentation.getProduct()))
        .build();
  }
}
