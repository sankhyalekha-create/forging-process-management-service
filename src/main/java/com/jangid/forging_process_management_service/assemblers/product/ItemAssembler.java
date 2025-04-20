package com.jangid.forging_process_management_service.assemblers.product;

import com.jangid.forging_process_management_service.entities.product.Item;
import com.jangid.forging_process_management_service.entities.product.ItemProduct;
import com.jangid.forging_process_management_service.entitiesRepresentation.product.ItemProductRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.product.ItemRepresentation;
import com.jangid.forging_process_management_service.service.product.ItemService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
public class ItemAssembler {

  private final ItemProductAssembler itemProductAssembler;

  private final ItemService itemService;

  public ItemAssembler(ItemProductAssembler itemProductAssembler,@Lazy ItemService itemService) {
    this.itemProductAssembler = itemProductAssembler;
    this.itemService = itemService;
  }

  public ItemRepresentation dissemble(Item item) {
    return ItemRepresentation.builder()
        .id(item.getId())
        .itemName(item.getItemName())
        .itemCode(item.getItemCode())
        .itemWeight(item.getItemWeight() != null ? String.valueOf(item.getItemWeight()) : null)
        .itemCount(item.getItemCount() != null ? String.valueOf(item.getItemCount()) : null)
        .tenantId(item.getTenant().getId())
        .itemProducts(getItemProductRepresentations(item.getItemProducts()))
        .createdAt(item.getCreatedAt() != null ? item.getCreatedAt().toString() : null).build();
  }

  public Item assemble(ItemRepresentation itemRepresentation) {
    if(itemRepresentation==null){
      return null;
    }
    if (itemRepresentation.getId() == null) {
      Item item = Item.builder()
          .itemName(itemRepresentation.getItemName())
          .itemCode(itemRepresentation.getItemCode())
          .build();
          
      if (itemRepresentation.getItemWeight() != null) {
        item.setItemWeight(Double.parseDouble(itemRepresentation.getItemWeight()));
      }
      
      if (itemRepresentation.getItemCount() != null) {
        item.setItemCount(Integer.parseInt(itemRepresentation.getItemCount()));
      }

      List<ItemProduct> itemProducts = getItemProducts(itemRepresentation.getItemProducts());
      itemProducts.forEach(itemProduct -> itemProduct.setCreatedAt(LocalDateTime.now()));
      item.updateItemProducts(itemProducts);

      if(item.getItemProducts()!=null){
        item.getItemProducts().clear();
      }
      item.setItemProducts(itemProducts);
      return item;
    }

    return itemService.getItemById(itemRepresentation.getId());
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
