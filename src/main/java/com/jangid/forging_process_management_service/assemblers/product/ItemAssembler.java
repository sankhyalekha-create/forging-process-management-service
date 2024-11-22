package com.jangid.forging_process_management_service.assemblers.product;

import com.jangid.forging_process_management_service.entities.product.Item;
import com.jangid.forging_process_management_service.entities.product.ItemProduct;
import com.jangid.forging_process_management_service.entities.product.ItemStatus;
import com.jangid.forging_process_management_service.entitiesRepresentation.product.ItemProductRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.product.ItemRepresentation;

import java.util.List;

public class ItemAssembler {

  public static ItemRepresentation dissemble(Item item) {
    return ItemRepresentation.builder()
        .id(item.getId())
        .itemName(item.getItemName())
        .itemStatus(item.getItemStatus().name())
        .tenantId(item.getTenant().getId())
        .itemProducts(getItemProductRepresentations(item.getItemProducts()))
        .createdAt(item.getCreatedAt() != null ? item.getCreatedAt().toString() : null).build();
  }

  public static Item assemble(ItemRepresentation itemRepresentation) {
    return Item.builder()
        .itemName(itemRepresentation.getItemName())
        .itemStatus(ItemStatus.valueOf(itemRepresentation.getItemStatus()))
        .itemWeight(Double.parseDouble(itemRepresentation.getItemWeight()))
        .itemProducts(getItemProducts(itemRepresentation.getItemProducts()))
        .build();
  }

  private static List<ItemProduct> getItemProducts(List<ItemProductRepresentation> itemProductRepresentations){
    return itemProductRepresentations.stream().map(ItemProductAssembler::assemble).toList();
  }

  private static List<ItemProductRepresentation> getItemProductRepresentations(List<ItemProduct> itemProducts){
    return itemProducts.stream().map(ItemProductAssembler::dissemble).toList();
  }

}
