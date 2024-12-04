package com.jangid.forging_process_management_service.assemblers.product;

import com.jangid.forging_process_management_service.entities.product.ItemProduct;
import com.jangid.forging_process_management_service.entitiesRepresentation.product.ItemProductRepresentation;
import com.jangid.forging_process_management_service.service.product.ProductService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ItemProductAssembler {

  @Autowired
  private ProductService productService;


  public ItemProductRepresentation dissemble(ItemProduct itemProduct) {
    return ItemProductRepresentation.builder()
        .id(itemProduct.getId())
        .itemId(String.valueOf(itemProduct.getItem().getId()))
        .product(ProductAssembler.dissemble(itemProduct.getProduct()))
        .createdAt(itemProduct.getCreatedAt() != null ? itemProduct.getCreatedAt().toString() : null).build();
  }

  public ItemProduct assemble(ItemProductRepresentation itemProductRepresentation) {
    return ItemProduct.builder()
        .product(productService.getProductById(itemProductRepresentation.getProduct().getId()))
        .build();
  }
}
