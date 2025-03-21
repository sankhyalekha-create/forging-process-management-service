package com.jangid.forging_process_management_service.service.product;

import com.jangid.forging_process_management_service.assemblers.product.ItemAssembler;
import com.jangid.forging_process_management_service.assemblers.product.ItemProductAssembler;
import com.jangid.forging_process_management_service.entities.Tenant;
import com.jangid.forging_process_management_service.entities.product.Item;
import com.jangid.forging_process_management_service.entities.product.ItemProduct;
import com.jangid.forging_process_management_service.entitiesRepresentation.product.ItemListRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.product.ItemProductRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.product.ItemRepresentation;
import com.jangid.forging_process_management_service.exception.product.ItemNotFoundException;
import com.jangid.forging_process_management_service.repositories.ProcessedItemRepository;
import com.jangid.forging_process_management_service.repositories.product.ItemRepository;
import com.jangid.forging_process_management_service.service.TenantService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ItemService {
  @Autowired
  private TenantService tenantService;

  @Autowired
  private ProductService productService;

  @Autowired
  private ItemRepository itemRepository;

  @Autowired
  private ProcessedItemRepository processedItemRepository;

  @Autowired
  private ItemAssembler itemAssembler;

  @Autowired
  private ItemProductAssembler itemProductAssembler;

  public ItemRepresentation createItem(long tenantId, ItemRepresentation itemRepresentation){
    Tenant tenant = tenantService.getTenantById(tenantId);
    Item item = itemAssembler.createAssemble(itemRepresentation);
//    item.getItemProducts().forEach(itemProduct -> item.setItem(itemProduct));
    item.setCreatedAt(LocalDateTime.now());
    item.setTenant(tenant);
    Item savedItem = saveItem(item);
    ItemRepresentation createdItemRepresentation = itemAssembler.dissemble(savedItem);
    return createdItemRepresentation;
  }

  @Transactional
  public Item saveItem(Item item){
    return itemRepository.save(item);
  }

  public ItemRepresentation updateItem(Long tenantId, Long itemId, ItemRepresentation itemRepresentation) {

    Tenant tenant = tenantService.getTenantById(tenantId);
    Item existingItem = getItemByIdAndTenantId(itemId, tenantId);

    existingItem.setTenant(tenant);

    if (itemRepresentation.getItemName() != null && !existingItem.getItemName().equals(itemRepresentation.getItemName())) {
      existingItem.setItemName(itemRepresentation.getItemName());
    }

    if (itemRepresentation.getItemCode() != null && !existingItem.getItemCode().equals(itemRepresentation.getItemCode())) {
      existingItem.setItemName(itemRepresentation.getItemName());
    }

    if (itemRepresentation.getItemWeight() != null && !String.valueOf(existingItem.getItemWeight())
        .equals(itemRepresentation.getItemWeight())) {
      existingItem.setItemWeight(Double.parseDouble(itemRepresentation.getItemWeight()));
    }

    updateItemProducts(existingItem, itemRepresentation.getItemProducts());
    Item savedItem = saveItem(existingItem);

    return itemAssembler.dissemble(savedItem);
  }

  private void updateItemProducts(Item existingItem, List<ItemProductRepresentation> newItemProductRepresentations){
    List<ItemProduct> existingItemProducts = existingItem.getItemProducts();

    Map<Long, ItemProduct> existingItemProductMap = existingItemProducts.stream()
        .collect(Collectors.toMap(ip -> ip.getProduct().getId(), ip -> ip));

    List<ItemProduct> productsToAdd = new ArrayList<>();
    Set<Long> newProductIds = new HashSet<>();

    for (ItemProductRepresentation newItemProductRep : newItemProductRepresentations) {
      Long productId = newItemProductRep.getProduct().getId();
      newProductIds.add(productId); // Track all product IDs in the new representation

      ItemProduct existingItemProduct = existingItemProductMap.get(productId);

      if (existingItemProduct == null) {
        // Create and set up a new ItemProduct if it doesn't exist in the existing collection
        ItemProduct newItemProduct = itemProductAssembler.assemble(newItemProductRep);
        newItemProduct.setProduct(productService.getProductById(productId));
        newItemProduct.setItem(existingItem);
        productsToAdd.add(newItemProduct);
      }

      // Remove products that are no longer in the new representation
      existingItemProducts.removeIf(existingProduct -> !newProductIds.contains(existingProduct.getProduct().getId()));

      // Add new products
      existingItemProducts.addAll(productsToAdd);
    }

  }
  public Item getItemByIdAndTenantId(long itemId, long tenantId){
    Optional<Item> optionalItem = itemRepository.findByIdAndTenantIdAndDeletedFalse(itemId, tenantId);
    if (optionalItem.isEmpty()) {
      log.error("Item with id=" + itemId + " having " + tenantId + " not found!");
      throw new RuntimeException("Item with id=" + itemId + " having " + tenantId + " not found!");
    }
    return optionalItem.get();
  }

  public ItemListRepresentation getAllItemsOfTenantWithoutPagination(long tenantId){
    List<Item> items = itemRepository.findByTenantIdAndDeletedFalseOrderByCreatedAtDesc(tenantId);
    return ItemListRepresentation.builder().items(items.stream().map(item -> itemAssembler.dissemble(item)).toList()).build();
  }

  public Page<ItemRepresentation> getAllItemsOfTenant(long tenantId, int page, int size){
    Pageable pageable = PageRequest.of(page, size);
    Page<Item> itemPage = itemRepository.findByTenantIdAndDeletedFalseOrderByCreatedAtDesc(tenantId, pageable);
    return itemPage.map(itemAssembler::dissemble);
  }

  @Transactional
  public void deleteItem(Long tenantId, Long itemId) {
    // 1. Validate tenant exists
    tenantService.isTenantExists(tenantId);

    // 2. Validate item exists
    Item item = getItemByIdAndTenantId(itemId, tenantId);

    // 3. Check if item is used in any ProcessedItem of Forge
    boolean isItemUsedInForge = processedItemRepository.existsByItemIdAndDeletedFalse(itemId);
    if (isItemUsedInForge) {
        log.error("Cannot delete item as it is used in forging process. ItemId={}, TenantId={}", itemId, tenantId);
        throw new IllegalStateException("Cannot delete item as it is used in forging process");
    }

    // 4. Soft delete item and its products
    item.setDeleted(true);
    item.setDeletedAt(LocalDateTime.now());

    // Soft delete all associated ItemProducts
    if (item.getItemProducts() != null) {
        item.getItemProducts().forEach(itemProduct -> {
            itemProduct.setDeleted(true);
            itemProduct.setDeletedAt(LocalDateTime.now());
        });
    }

    saveItem(item);
    log.info("Successfully deleted item with id={} for tenant={}", itemId, tenantId);
  }

  public boolean isItemExistsForTenant(long itemId, long tenantId){
    boolean isItemExistsForTenant = itemRepository.existsByIdAndTenantIdAndDeletedFalse(itemId, tenantId);

    if(!isItemExistsForTenant){
      log.error("Item having id={} does not exists for tenant={}", itemId, tenantId);
      throw new ItemNotFoundException("Item having id=" + itemId + " does not exists for tenant=" + tenantId);
    }
    return true;
  }
}
