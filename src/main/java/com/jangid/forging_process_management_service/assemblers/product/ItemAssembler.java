package com.jangid.forging_process_management_service.assemblers.product;

import com.jangid.forging_process_management_service.entities.product.Item;
import com.jangid.forging_process_management_service.entities.product.ItemProduct;
import com.jangid.forging_process_management_service.entitiesRepresentation.product.ItemProductRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.product.ItemRepresentation;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
public class ItemAssembler {

  @Autowired
  private ItemProductAssembler itemProductAssembler;

  public ItemRepresentation dissemble(Item item) {
    return ItemRepresentation.builder()
        .id(item.getId())
        .itemName(item.getItemName())
        .itemWeight(String.valueOf(item.getItemWeight()))
        .tenantId(item.getTenant().getId())
        .itemProducts(getItemProductRepresentations(item.getItemProducts()))
        .createdAt(item.getCreatedAt() != null ? item.getCreatedAt().toString() : null).build();
  }

  public Item assemble(ItemRepresentation itemRepresentation) {
    if(itemRepresentation==null){
      return null;
    }
    Item item =  Item.builder()
        .itemName(itemRepresentation.getItemName())
        .itemCode(itemRepresentation.getItemCode())
        .itemWeight(Double.parseDouble(itemRepresentation.getItemWeight()))
        .build();

    List<ItemProduct> itemProducts = getItemProducts(itemRepresentation.getItemProducts());
    itemProducts.forEach(itemProduct -> itemProduct.setCreatedAt(LocalDateTime.now()));
    item.updateItemProducts(itemProducts);

    if(item.getItemProducts()!=null){
      item.getItemProducts().clear();
    }
    item.setItemProducts(itemProducts);
    return item;
  }

  public Item createAssemble(ItemRepresentation itemRepresentation) {
    Item item = assemble(itemRepresentation);
    item.setCreatedAt(LocalDateTime.now());
    return item;
  }

  private List<ItemProduct> getItemProducts(List<ItemProductRepresentation> itemProductRepresentations){
    List<ItemProduct> itemProducts =  itemProductRepresentations.stream().map(itemProductRepresentation -> itemProductAssembler.assemble(itemProductRepresentation)).toList();
    return itemProducts;
  }

  private List<ItemProductRepresentation> getItemProductRepresentations(List<ItemProduct> itemProducts){
    List<ItemProductRepresentation> itemProductRepresentations = itemProducts.stream().map(itemProduct -> itemProductAssembler.dissemble(itemProduct)).toList();
    return itemProductRepresentations;
  }

}
