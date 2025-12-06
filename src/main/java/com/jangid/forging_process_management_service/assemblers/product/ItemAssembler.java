package com.jangid.forging_process_management_service.assemblers.product;

import com.jangid.forging_process_management_service.assemblers.workflow.ItemWorkflowAssembler;
import com.jangid.forging_process_management_service.entities.product.Item;
import com.jangid.forging_process_management_service.entities.product.ItemProduct;
import com.jangid.forging_process_management_service.entities.workflow.ItemWorkflow;
import com.jangid.forging_process_management_service.entitiesRepresentation.product.ItemProductRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.product.ItemRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.workflow.ItemWorkflowRepresentation;
import com.jangid.forging_process_management_service.service.product.ItemService;
import com.jangid.forging_process_management_service.utils.PrecisionUtils;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ItemAssembler {

  private final ItemProductAssembler itemProductAssembler;
  
  @Autowired
  @Lazy
  private ItemWorkflowAssembler itemWorkflowAssembler;

  private final ItemService itemService;

  public ItemAssembler(ItemProductAssembler itemProductAssembler,@Lazy ItemService itemService) {
    this.itemProductAssembler = itemProductAssembler;
    this.itemService = itemService;
  }

  public ItemRepresentation dissemble(Item item) {
    ItemRepresentation.ItemRepresentationBuilder builder = ItemRepresentation.builder()
        .id(item.getId())
        .itemName(item.getItemName())
        .itemCode(item.getItemCode())
        .itemWeight(item.getItemWeight() != null ? String.valueOf(item.getItemWeight()) : null)
        .itemForgedWeight(item.getItemForgedWeight() != null ? String.valueOf(item.getItemForgedWeight()) : null)
        .itemSlugWeight(item.getItemSlugWeight() != null ? String.valueOf(item.getItemSlugWeight()) : null)
        .itemFinishedWeight(item.getItemFinishedWeight() != null ? String.valueOf(item.getItemFinishedWeight()) : null)
        .itemCount(item.getItemCount() != null ? String.valueOf(item.getItemCount()) : null)
        .tenantId(item.getTenant().getId())
        .itemProducts(getItemProductRepresentations(item.getItemProducts()))
        .itemWorkflows(getItemWorkflowRepresentations(item.getItemWorkflows()))
        .createdAt(item.getCreatedAt() != null ? item.getCreatedAt().toString() : null);

    return builder.build();
  }

  /**
   * Lightweight dissemble method that excludes itemWorkflows and uses basic itemProducts.
   * This method should be used by ProcessedItem assemblers to avoid excessive payload sizes.
   */
  public ItemRepresentation dissembleBasic(Item item) {
    return ItemRepresentation.builder()
        .id(item.getId())
        .itemName(item.getItemName())
        .itemCode(item.getItemCode())
        .itemWeight(item.getItemWeight() != null ? String.valueOf(item.getItemWeight()) : null)
        .itemForgedWeight(item.getItemForgedWeight() != null ? String.valueOf(item.getItemForgedWeight()) : null)
        .itemSlugWeight(item.getItemSlugWeight() != null ? String.valueOf(item.getItemSlugWeight()) : null)
        .itemFinishedWeight(item.getItemFinishedWeight() != null ? String.valueOf(item.getItemFinishedWeight()) : null)
        .itemCount(item.getItemCount() != null ? String.valueOf(item.getItemCount()) : null)
        .tenantId(item.getTenant().getId())
        .itemProducts(getItemProductRepresentationsBasic(item.getItemProducts())) // Use basic product representations
        // Exclude itemWorkflows to reduce payload size
        .createdAt(item.getCreatedAt() != null ? item.getCreatedAt().toString() : null)
        .build();
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
        item.setItemWeight(PrecisionUtils.roundQuantity(Double.parseDouble(itemRepresentation.getItemWeight())));
      }

      if (itemRepresentation.getItemForgedWeight() != null) {
        item.setItemForgedWeight(PrecisionUtils.roundQuantity(Double.parseDouble(itemRepresentation.getItemForgedWeight())));
      }

      if (itemRepresentation.getItemSlugWeight() != null) {
        item.setItemSlugWeight(PrecisionUtils.roundQuantity(Double.parseDouble(itemRepresentation.getItemSlugWeight())));
      }

      if (itemRepresentation.getItemFinishedWeight() != null) {
        item.setItemFinishedWeight(PrecisionUtils.roundQuantity(Double.parseDouble(itemRepresentation.getItemFinishedWeight())));
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
      
      // Note: ItemWorkflows are typically managed separately through ItemWorkflowService
      // For new items, workflows are usually created via dedicated workflow creation endpoints
      // So we don't handle itemWorkflows assembly for new items here
      
      return item;
    }

    // For existing items, fetch from service which will include all relationships
    return itemService.getItemById(itemRepresentation.getId());
  }

  public Item createAssemble(ItemRepresentation itemRepresentation) {
    Item item = assemble(itemRepresentation);
    item.setCreatedAt(LocalDateTime.now());
    return item;
  }

  public List<ItemProduct> getItemProducts(List<ItemProductRepresentation> itemProductRepresentations){
    List<ItemProduct> itemProducts =  itemProductRepresentations.stream().map(itemProductRepresentation -> itemProductAssembler.assemble(itemProductRepresentation)).toList();
    return itemProducts;
  }

  private List<ItemProductRepresentation> getItemProductRepresentations(List<ItemProduct> itemProducts){
    List<ItemProductRepresentation> itemProductRepresentations = itemProducts.stream().map(itemProduct -> itemProductAssembler.dissemble(itemProduct)).toList();
    return itemProductRepresentations;
  }

  /**
   * Creates basic item product representations without supplier details to reduce payload size.
   */
  private List<ItemProductRepresentation> getItemProductRepresentationsBasic(List<ItemProduct> itemProducts){
    List<ItemProductRepresentation> itemProductRepresentations = itemProducts.stream().map(itemProduct -> itemProductAssembler.dissembleBasic(itemProduct)).toList();
    return itemProductRepresentations;
  }

  private List<ItemWorkflowRepresentation> getItemWorkflowRepresentations(List<ItemWorkflow> itemWorkflows){
    if (itemWorkflows == null || itemWorkflows.isEmpty()) {
      return null;
    }
    
    return itemWorkflows.stream()
        .filter(itemWorkflow -> !itemWorkflow.getDeleted()) // Filter out deleted workflows
        .filter(itemWorkflow -> !itemWorkflow.isCompleted())
        .sorted(Comparator.comparing(ItemWorkflow::getCreatedAt).reversed()) // Sort by creation date, newest first
        .map(itemWorkflow -> {
          try {
            return itemWorkflowAssembler.dissemble(itemWorkflow);
          } catch (Exception e) {
            log.warn("Error converting ItemWorkflow {} to representation: {}", itemWorkflow.getId(), e.getMessage());
            return null;
          }
        })
        .filter(representation -> representation != null) // Filter out null representations from errors
        .collect(Collectors.toList());
  }

}
