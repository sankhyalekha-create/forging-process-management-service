package com.jangid.forging_process_management_service.service.product;

import com.jangid.forging_process_management_service.assemblers.product.ItemAssembler;
import com.jangid.forging_process_management_service.assemblers.product.ItemProductAssembler;
import com.jangid.forging_process_management_service.entities.Tenant;
import com.jangid.forging_process_management_service.entities.document.DocumentLink;
import com.jangid.forging_process_management_service.entities.product.Item;
import com.jangid.forging_process_management_service.entities.product.ItemProduct;
import com.jangid.forging_process_management_service.entitiesRepresentation.product.ItemListRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.product.ItemProductRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.product.ItemRepresentation;
import com.jangid.forging_process_management_service.exception.product.ItemNotFoundException;
import com.jangid.forging_process_management_service.exception.document.DocumentDeletionException;
import com.jangid.forging_process_management_service.repositories.product.ItemRepository;
import com.jangid.forging_process_management_service.repositories.workflow.ItemWorkflowRepository;
import com.jangid.forging_process_management_service.service.TenantService;
import com.jangid.forging_process_management_service.service.document.DocumentService;
import com.jangid.forging_process_management_service.entities.workflow.WorkflowStep;
import com.jangid.forging_process_management_service.entities.workflow.ItemWorkflow;
import com.jangid.forging_process_management_service.entities.forging.ItemWeightType;
import com.jangid.forging_process_management_service.utils.PrecisionUtils;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;

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
  private ItemWorkflowRepository itemWorkflowRepository;

  @Autowired
  private ItemAssembler itemAssembler;

  @Autowired
  private ItemProductAssembler itemProductAssembler;

  @Autowired
  private DocumentService documentService;


  @Cacheable(value = "items", key = "'tenant_' + #tenantId + '_page_' + #page + '_size_' + #size")
  public Page<ItemRepresentation> getAllItemsOfTenant(long tenantId, int page, int size) {
    log.info("Fetching items from database for tenantId={}, page={}, size={}", tenantId, page, size);
    Pageable pageable = PageRequest.of(page, size);
    Page<Item> itemPage = itemRepository.findByTenantIdAndDeletedFalseWithWorkflowOrderByUpdatedAtDesc(tenantId, pageable);
    return itemPage.map(itemAssembler::dissemble);
  }

  @Cacheable(value = "items", key = "'tenant_' + #tenantId + '_all'")
  public ItemListRepresentation getAllItemsOfTenantWithoutPagination(long tenantId) {
    List<Item> items = itemRepository.findByTenantIdAndDeletedFalseWithWorkflowOrderByCreatedAtDesc(tenantId);
    return ItemListRepresentation.builder()
            .items(items.stream().map(itemAssembler::dissemble).toList())
            .build();
  }

  @Cacheable(value = "items", key = "'tenant_' + #tenantId + '_item_' + #itemId")
  public ItemRepresentation getItemOfTenant(long tenantId, long itemId) {
    Item item = getItemByIdAndTenantId(itemId, tenantId);
    if(item.getTenant().getId() != tenantId) {
      throw new ItemNotFoundException("Item not found with itemId=" + itemId);
    }
    return itemAssembler.dissemble(item);
  }

  @CacheEvict(value = "items", allEntries = true)
  @Transactional
  public ItemRepresentation createItem(long tenantId, ItemRepresentation itemRepresentation) {

    // First check if an active (not deleted) item with the same name or code exists
    boolean existsByNameNotDeleted = itemRepository.existsByItemNameAndTenantIdAndDeletedFalse(
        itemRepresentation.getItemName(), tenantId);
    if (existsByNameNotDeleted) {
      log.error("Active item with name: {} already exists for tenant: {}!", 
                itemRepresentation.getItemName(), tenantId);
      throw new IllegalStateException("Item with name=" + itemRepresentation.getItemName() 
                                     + " already exists");
    }
    
    boolean existsByCodeNotDeleted = itemRepository.existsByItemCodeAndTenantIdAndDeletedFalse(
        itemRepresentation.getItemCode(), tenantId);
    if (existsByCodeNotDeleted) {
      log.error("Active item with code: {} already exists for tenant: {}!", 
                itemRepresentation.getItemCode(), tenantId);
      throw new IllegalStateException("Item with code=" + itemRepresentation.getItemCode() 
                                     + " already exists");
    }
    
    // Check if we're trying to revive a deleted item
    Item item = null;
    Optional<Item> deletedItemByName = itemRepository.findByItemNameAndTenantIdAndDeletedTrue(
        itemRepresentation.getItemName(), tenantId);
    
    if (deletedItemByName.isPresent()) {
      // We found a deleted item with the same name, reactivate it
      log.info("Reactivating previously deleted item with name: {}", itemRepresentation.getItemName());
      item = deletedItemByName.get();
      item.setDeleted(false);
      item.setDeletedAt(null);
      
      // Update ALL item fields from the representation to make it behave like a new item
      updateAllItemFields(item, itemRepresentation);
      
      // Reactivate all item products
      if (item.getItemProducts() != null) {
        item.getItemProducts().forEach(itemProduct -> {
          itemProduct.setDeleted(false);
          itemProduct.setDeletedAt(null);
        });
      }
    } else {
      // Check for deleted item with same code
      Optional<Item> deletedItemByCode = itemRepository.findByItemCodeAndTenantIdAndDeletedTrue(
          itemRepresentation.getItemCode(), tenantId);
          
      if (deletedItemByCode.isPresent()) {
        // We found a deleted item with the same code, reactivate it
        log.info("Reactivating previously deleted item with code: {}", itemRepresentation.getItemCode());
        item = deletedItemByCode.get();
        item.setDeleted(false);
        item.setDeletedAt(null);
        
        // Update ALL item fields from the representation to make it behave like a new item
        updateAllItemFields(item, itemRepresentation);
        
        // Reactivate all item products
        if (item.getItemProducts() != null) {
          item.getItemProducts().forEach(itemProduct -> {
            itemProduct.setDeleted(false);
            itemProduct.setDeletedAt(null);
          });
        }
      } else {
        // Create new item
        Tenant tenant = tenantService.getTenantById(tenantId);
        item = itemAssembler.createAssemble(itemRepresentation);
        item.setCreatedAt(LocalDateTime.now());
        item.setTenant(tenant);
      }
    }
    
    // Validate measurements based on product types
    if (!item.validateMeasurements()) {
      throw new IllegalArgumentException("Invalid measurement values: For KGS products, item weight must be provided. For PIECES products, item count is automatically set to 1.");
    }
    
    Item savedItem = saveItem(item);
    
    ItemRepresentation createdItemRepresentation = itemAssembler.dissemble(savedItem);
    return createdItemRepresentation;
  }


  /**
   * Helper method to update all item fields from ItemRepresentation
   * This method updates all fields to make reactivated items behave like new items
   */
  private void updateAllItemFields(Item item, ItemRepresentation representation) {
    // Update basic item fields
    if (representation.getItemName() != null) {
      item.setItemName(representation.getItemName());
    }
    
    if (representation.getItemCode() != null) {
      item.setItemCode(representation.getItemCode());
    }
    
    if (representation.getItemWeight() != null) {
      item.setItemWeight(PrecisionUtils.roundQuantity(Double.parseDouble(representation.getItemWeight())));
    }

    if (representation.getItemSlugWeight() != null) {
      item.setItemSlugWeight(PrecisionUtils.roundQuantity(Double.parseDouble(representation.getItemSlugWeight())));
    }
    
    if (representation.getItemForgedWeight() != null) {
      item.setItemForgedWeight(PrecisionUtils.roundQuantity(Double.parseDouble(representation.getItemForgedWeight())));
    }
    
    if (representation.getItemFinishedWeight() != null) {
      item.setItemFinishedWeight(PrecisionUtils.roundQuantity(Double.parseDouble(representation.getItemFinishedWeight())));
    }
    
    if (representation.getItemCount() != null) {
      item.setItemCount(Integer.parseInt(representation.getItemCount()));
    }
    
    // Update item products - completely replace existing products
    if (representation.getItemProducts() != null) {
      updateItemProducts(item, representation.getItemProducts());
    }
  }

  @Transactional
  public Item saveItem(Item item){
    return itemRepository.save(item);
  }

  @CacheEvict(value = "items", allEntries = true)
  @Transactional
  public ItemRepresentation updateItem(long tenantId, long itemId, ItemRepresentation itemRepresentation) {
    Tenant tenant = tenantService.getTenantById(tenantId);
    Item existingItem = getItemByIdAndTenantId(itemId, tenantId);

    // Check if item has any associated workflows
    boolean hasAssociatedWorkflows = hasAssociatedItemWorkflows(itemId);
    
    if (hasAssociatedWorkflows) {
      // When workflows exist, only allow product additions/updates
      validateWorkflowRestrictedUpdate(existingItem, itemRepresentation);
      log.info("Item {} has associated workflows. Only allowing product updates.", itemId);
      
      // Only update item products when workflows exist
      updateItemProducts(existingItem, itemRepresentation.getItemProducts());
    } else {
      // When no workflows exist, allow all field updates
      log.info("Item {} has no associated workflows. Allowing full field updates.", itemId);
      
      existingItem.setTenant(tenant);

      if (itemRepresentation.getItemName() != null && !existingItem.getItemName().equals(itemRepresentation.getItemName())) {
        existingItem.setItemName(itemRepresentation.getItemName());
      }

      if (itemRepresentation.getItemCode() != null && !existingItem.getItemCode().equals(itemRepresentation.getItemCode())) {
        existingItem.setItemCode(itemRepresentation.getItemCode());
      }

      if (itemRepresentation.getItemWeight() != null && !String.valueOf(existingItem.getItemWeight())
          .equals(itemRepresentation.getItemWeight())) {
        existingItem.setItemWeight(PrecisionUtils.roundQuantity(Double.parseDouble(itemRepresentation.getItemWeight())));
      }

      if (itemRepresentation.getItemSlugWeight() != null && !String.valueOf(existingItem.getItemSlugWeight())
          .equals(itemRepresentation.getItemSlugWeight())) {
        existingItem.setItemSlugWeight(PrecisionUtils.roundQuantity(Double.parseDouble(itemRepresentation.getItemSlugWeight())));
      }

      if (itemRepresentation.getItemForgedWeight() != null && !String.valueOf(existingItem.getItemForgedWeight())
          .equals(itemRepresentation.getItemForgedWeight())) {
        existingItem.setItemForgedWeight(PrecisionUtils.roundQuantity(Double.parseDouble(itemRepresentation.getItemForgedWeight())));
      }

      if (itemRepresentation.getItemFinishedWeight() != null && !String.valueOf(existingItem.getItemFinishedWeight())
          .equals(itemRepresentation.getItemFinishedWeight())) {
        existingItem.setItemFinishedWeight(PrecisionUtils.roundQuantity(Double.parseDouble(itemRepresentation.getItemFinishedWeight())));
      }
      
      if (itemRepresentation.getItemCount() != null) {
        existingItem.setItemCount(Integer.parseInt(itemRepresentation.getItemCount()));
      }

      updateItemProducts(existingItem, itemRepresentation.getItemProducts());
    }
    
    // Validate measurements based on product types
    if (!existingItem.validateMeasurements()) {
      throw new IllegalArgumentException("Invalid measurement values: For KGS products, item weight must be provided. For PIECES products, item count is automatically set to 1.");
    }
    
    Item savedItem = saveItem(existingItem);

    return itemAssembler.dissemble(savedItem);
  }

  private void updateItemProducts(Item existingItem, List<ItemProductRepresentation> newItemProductRepresentations){
    List<ItemProduct> existingItemProducts = existingItem.getItemProducts();

    Map<Long, ItemProduct> existingItemProductMap = existingItemProducts.stream()
        .collect(Collectors.toMap(ip -> ip.getProduct().getId(), ip -> ip));

    List<ItemProduct> productsToAdd = new ArrayList<>();
    Set<Long> newProductIds = new HashSet<>();

    // Check if item has workflows to determine update behavior
    boolean hasWorkflows = hasAssociatedItemWorkflows(existingItem.getId());

    // First pass: collect all new product IDs and identify products to add
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
    }

    // Second pass: remove products only if no workflows exist
    // When workflows exist, we should only add new products, not remove existing ones
    if (!hasWorkflows) {
      existingItemProducts.removeIf(existingProduct -> !newProductIds.contains(existingProduct.getProduct().getId()));
    }

    // Third pass: add new products
    existingItemProducts.addAll(productsToAdd);
  }
  public Item getItemByIdAndTenantId(long itemId, long tenantId){
    Optional<Item> optionalItem = itemRepository.findByIdAndTenantIdAndDeletedFalse(itemId, tenantId);
    if (optionalItem.isEmpty()) {
      log.error("Item with id=" + itemId + " having " + tenantId + " not found!");
      throw new RuntimeException("Item with id=" + itemId + " having " + tenantId + " not found!");
    }
    return optionalItem.get();
  }

  public Item getItemById(long itemId){
    Optional<Item> optionalItem = itemRepository.findByIdAndDeletedFalse(itemId);
    if (optionalItem.isEmpty()) {
      log.error("Item with id=" + itemId + " not found!");
      throw new RuntimeException("Item with id=" + itemId + " not found!");
    }
    return optionalItem.get();
  }

  @Transactional
  public void deleteItem(Long tenantId, Long itemId) throws DocumentDeletionException {
    // 1. Validate tenant exists
    tenantService.isTenantExists(tenantId);

    // 2. Validate item exists
    Item item = getItemByIdAndTenantId(itemId, tenantId);

    // 3. Check if item is part of any ItemWorkflow that is not COMPLETED
    List<ItemWorkflow> allItemWorkflows = itemWorkflowRepository.findByItemIdAndDeletedFalse(itemId);
    List<ItemWorkflow> activeItemWorkflows = allItemWorkflows.stream()
        .filter(workflow -> workflow.getWorkflowStatus() != ItemWorkflow.WorkflowStatus.COMPLETED)
        .collect(Collectors.toList());
        
    if (!activeItemWorkflows.isEmpty()) {
        log.error("Cannot delete item as it is part of active workflow(s). ItemId={}, TenantId={}, ActiveWorkflowCount={}, TotalWorkflowCount={}", 
                  itemId, tenantId, activeItemWorkflows.size(), allItemWorkflows.size());
        throw new IllegalStateException("Cannot delete item as it is part of one or more active workflows (not completed)");
    }

    // 4. Delete all documents attached to this item using bulk delete for efficiency
    try {
        // Use bulk delete method from DocumentService for better performance
        documentService.deleteDocumentsForEntity(tenantId, DocumentLink.EntityType.ITEM, itemId);
        log.info("Successfully bulk deleted all documents attached to item {} for tenant {}", itemId, tenantId);
    } catch (DataAccessException e) {
        log.error("Database error while deleting documents attached to item {}: {}", itemId, e.getMessage(), e);
        throw new DocumentDeletionException("Database error occurred while deleting attached documents for item " + itemId, e);
    } catch (RuntimeException e) {
        // Handle document service specific runtime exceptions (storage, file system errors, etc.)
        log.error("Document service error while deleting documents attached to item {}: {}", itemId, e.getMessage(), e);
        throw new DocumentDeletionException("Document service error occurred while deleting attached documents for item " + itemId + ": " + e.getMessage(), e);
    } catch (Exception e) {
        // Handle any other unexpected exceptions
        log.error("Unexpected error while deleting documents attached to item {}: {}", itemId, e.getMessage(), e);
        throw new DocumentDeletionException("Unexpected error occurred while deleting attached documents for item " + itemId, e);
    }

    // 5. Soft delete item and its products
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
    log.info("Successfully deleted item with id={} and all associated documents for tenant={}", itemId, tenantId);
  }

  public boolean isItemExistsForTenant(long itemId, long tenantId){
    boolean isItemExistsForTenant = itemRepository.existsByIdAndTenantIdAndDeletedFalse(itemId, tenantId);

    if(!isItemExistsForTenant){
      log.error("Item having id={} does not exists for tenant={}", itemId, tenantId);
      throw new ItemNotFoundException("Item having id=" + itemId + " does not exists for tenant=" + tenantId);
    }
    return true;
  }

  /**
   * Search for items by item name or item code substring with pagination
   * @param tenantId The tenant ID
   * @param searchType The type of search (ITEM_NAME or ITEM_CODE)
   * @param searchTerm The search term (substring matching)
   * @param page The page number (0-based)
   * @param size The page size
   * @return Page of ItemRepresentation containing the search results
   */
  public Page<ItemRepresentation> searchItems(Long tenantId, String searchType, String searchTerm, int page, int size) {
    if (searchTerm == null || searchTerm.trim().isEmpty() || searchType == null || searchType.trim().isEmpty()) {
      Pageable pageable = PageRequest.of(page, size);
      return Page.empty(pageable);
    }

    Pageable pageable = PageRequest.of(page, size);
    Page<Item> itemsPage;
    
    switch (searchType.toUpperCase()) {
      case "ITEM_NAME":
        itemsPage = itemRepository.findItemsByItemNameContainingIgnoreCase(tenantId, searchTerm.trim(), pageable);
        break;
      case "ITEM_CODE":
        itemsPage = itemRepository.findItemsByItemCodeContainingIgnoreCase(tenantId, searchTerm.trim(), pageable);
        break;
      default:
        log.error("Invalid search type: {}", searchType);
        throw new IllegalArgumentException("Invalid search type: " + searchType + ". Valid types are: ITEM_NAME, ITEM_CODE");
    }
    
    return itemsPage.map(itemAssembler::dissemble);
  }

  /**
   * Get Items that have ItemWorkflows with IN_PROGRESS status
   * Ordered by most recently updated workflow at the top
   * @param tenantId The tenant ID
   * @param page The page number (0-based)
   * @param size The page size
   * @return Page of ItemRepresentation containing items with in-progress workflows
   */
  public Page<ItemRepresentation> getItemsWithInProgressWorkflows(Long tenantId, int page, int size) {
    log.info("Fetching items with IN_PROGRESS workflows for tenantId={}, page={}, size={}", tenantId, page, size);
    
    Pageable pageable = PageRequest.of(page, size);
    Page<Item> itemsPage = itemRepository.findByTenantIdWithInProgressWorkflows(tenantId, pageable);
    
    return itemsPage.map(itemAssembler::dissemble);
  }

  /**
   * Legacy search method without pagination (keep for backward compatibility)
   * @param tenantId The tenant ID
   * @param searchType The type of search (ITEM_NAME or ITEM_CODE)
   * @param searchTerm The search term (substring matching)
   * @return ItemListRepresentation containing the search results
   */
  public ItemListRepresentation searchItemsLegacy(Long tenantId, String searchType, String searchTerm) {
    if (searchTerm == null || searchTerm.trim().isEmpty() || searchType == null || searchType.trim().isEmpty()) {
      return ItemListRepresentation.builder()
          .items(List.of())
          .build();
    }

    List<Item> items = new ArrayList<>();
    
    switch (searchType.toUpperCase()) {
      case "ITEM_NAME":
        items = itemRepository.findItemsByItemNameContainingIgnoreCaseList(tenantId, searchTerm.trim());
        break;
      case "ITEM_CODE":
        items = itemRepository.findItemsByItemCodeContainingIgnoreCaseList(tenantId, searchTerm.trim());
        break;
      default:
        log.error("Invalid search type: {}", searchType);
        throw new IllegalArgumentException("Invalid search type: " + searchType + ". Valid types are: ITEM_NAME, ITEM_CODE");
    }
    
    List<ItemRepresentation> itemRepresentations = items.stream()
        .map(itemAssembler::dissemble)
        .collect(Collectors.toList());
    
    return ItemListRepresentation.builder()
        .items(itemRepresentations)
        .build();
  }

  @CacheEvict(value = "items", allEntries = true)
  public void clearItemCache() {
    // This method is just for clearing the cache
  }

  @Cacheable(value = "items", key = "'tenant_' + #tenantId + '_operation_' + #operationType + '_page_' + #page + '_size_' + #size")
  public Page<ItemRepresentation> getItemsByOperationType(long tenantId, String operationType, int page, int size) {
    log.info("Fetching items by operation type {} for tenantId={}, page={}, size={}", operationType, tenantId, page, size);
    
    WorkflowStep.OperationType operationTypeEnum;
    try {
      // Validate and convert operation type to enum
      operationTypeEnum = WorkflowStep.OperationType.valueOf(operationType);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Invalid operation type: " + operationType + 
                                       ". Valid types are: " + String.join(", ", getValidOperationTypes()));
    }
    
    Pageable pageable = PageRequest.of(page, size);
    Page<Item> itemPage = itemRepository.findByTenantIdAndOperationTypeWithWorkflow(tenantId, operationTypeEnum, pageable);
    return itemPage.map(itemAssembler::dissemble);
  }

  @Cacheable(value = "items", key = "'tenant_' + #tenantId + '_operation_' + #operationType + '_all'")
  public ItemListRepresentation getItemsByOperationType(long tenantId, String operationType) {
    log.info("Fetching all items by operation type {} for tenantId={}", operationType, tenantId);
    
    WorkflowStep.OperationType operationTypeEnum;
    try {
      // Validate and convert operation type to enum
      operationTypeEnum = WorkflowStep.OperationType.valueOf(operationType);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Invalid operation type: " + operationType + 
                                       ". Valid types are: " + String.join(", ", getValidOperationTypes()));
    }
    
    List<Item> items = itemRepository.findByTenantIdAndOperationTypeWithWorkflow(tenantId, operationTypeEnum);
    return ItemListRepresentation.builder()
            .items(items.stream().map(itemAssembler::dissemble).toList())
            .build();
  }

  private List<String> getValidOperationTypes() {
    return Arrays.stream(WorkflowStep.OperationType.values())
                 .map(Enum::name)
                 .toList();
  }

  /**
   * Get the weight of an item based on the specified weight type
   * @param itemId The ID of the item
   * @param itemWeightType The type of weight to retrieve (ITEM_WEIGHT, ITEM_SLUG_WEIGHT, ITEM_FORGED_WEIGHT, ITEM_FINISHED_WEIGHT)
   * @return The weight value for the specified weight type
   * @throws RuntimeException if item is not found or weight type is invalid
   */
  public Double getItemWeightByType(Long itemId, ItemWeightType itemWeightType) {
    if (itemId == null) {
      throw new IllegalArgumentException("Item ID cannot be null");
    }
    
    if (itemWeightType == null) {
      throw new IllegalArgumentException("Item weight type cannot be null");
    }
    
    Item item = getItemById(itemId);
    
    Double weight = switch (itemWeightType) {
      case ITEM_WEIGHT -> item.getItemWeight();
      case ITEM_SLUG_WEIGHT -> item.getItemSlugWeight();
      case ITEM_FORGED_WEIGHT -> item.getItemForgedWeight();
      case ITEM_FINISHED_WEIGHT -> item.getItemFinishedWeight();
    };
    
    if (weight == null || weight <= 0) {
      log.warn("Item weight is null or zero for itemId={}, weightType={}", itemId, itemWeightType);
      throw new IllegalStateException("Item weight is not available for item ID " + itemId + " and weight type " + itemWeightType);
    }
    
    return weight;
  }

  /**
   * Get the weight of an item based on the specified weight type (string version for convenience)
   * @param itemId The ID of the item
   * @param itemWeightTypeStr The type of weight to retrieve as string
   * @return The weight value for the specified weight type
   * @throws RuntimeException if item is not found or weight type is invalid
   */
  public Double getItemWeightByType(Long itemId, String itemWeightTypeStr) {
    if (itemWeightTypeStr == null || itemWeightTypeStr.trim().isEmpty()) {
      throw new IllegalArgumentException("Item weight type string cannot be null or empty");
    }
    
    ItemWeightType itemWeightType;
    try {
      itemWeightType = ItemWeightType.valueOf(itemWeightTypeStr.toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Invalid item weight type: " + itemWeightTypeStr + 
                                       ". Valid types are: ITEM_WEIGHT, ITEM_SLUG_WEIGHT, ITEM_FORGED_WEIGHT, ITEM_FINISHED_WEIGHT");
    }
    
    return getItemWeightByType(itemId, itemWeightType);
  }

  /**
   * Check if an item has any associated ItemWorkflows
   * @param itemId The ID of the item to check
   * @return true if the item has associated workflows, false otherwise
   */
  private boolean hasAssociatedItemWorkflows(long itemId) {
    List<ItemWorkflow> workflows = itemWorkflowRepository.findByItemIdAndDeletedFalse(itemId);
    boolean hasWorkflows = !workflows.isEmpty();
    
    if (hasWorkflows) {
      log.debug("Item {} has {} associated workflow(s)", itemId, workflows.size());
    } else {
      log.debug("Item {} has no associated workflows", itemId);
    }
    
    return hasWorkflows;
  }

  /**
   * Validates that when workflows exist, only product modifications are allowed
   * @param existingItem The existing item entity
   * @param itemRepresentation The update request representation
   * @throws IllegalArgumentException if non-product fields are being modified
   */
  private void validateWorkflowRestrictedUpdate(Item existingItem, ItemRepresentation itemRepresentation) {
    List<String> attemptedChanges = new ArrayList<>();
    
    // Check if item name is being changed
    if (itemRepresentation.getItemName() != null && 
        !existingItem.getItemName().equals(itemRepresentation.getItemName())) {
      attemptedChanges.add("item name");
    }
    
    // Check if item code is being changed
    if (itemRepresentation.getItemCode() != null && 
        !existingItem.getItemCode().equals(itemRepresentation.getItemCode())) {
      attemptedChanges.add("item code");
    }
    
    // Check if item weight is being changed
    if (itemRepresentation.getItemWeight() != null && 
        !String.valueOf(existingItem.getItemWeight()).equals(itemRepresentation.getItemWeight())) {
      attemptedChanges.add("item weight");
    }
    
    // Check if item slug weight is being changed
    if (itemRepresentation.getItemSlugWeight() != null && 
        !String.valueOf(existingItem.getItemSlugWeight()).equals(itemRepresentation.getItemSlugWeight())) {
      attemptedChanges.add("item slug weight");
    }
    
    // Check if item forged weight is being changed
    if (itemRepresentation.getItemForgedWeight() != null && 
        !String.valueOf(existingItem.getItemForgedWeight()).equals(itemRepresentation.getItemForgedWeight())) {
      attemptedChanges.add("item forged weight");
    }
    
    // Check if item finished weight is being changed
    if (itemRepresentation.getItemFinishedWeight() != null && 
        !String.valueOf(existingItem.getItemFinishedWeight()).equals(itemRepresentation.getItemFinishedWeight())) {
      attemptedChanges.add("item finished weight");
    }
    
    // Check if item count is being changed
    if (itemRepresentation.getItemCount() != null && 
        !String.valueOf(existingItem.getItemCount()).equals(itemRepresentation.getItemCount())) {
      attemptedChanges.add("item count");
    }
    
    // If any non-product fields are being changed, throw an exception
    if (!attemptedChanges.isEmpty()) {
      String message = String.format(
        "Cannot update item fields [%s] because the item has associated workflows. " +
        "Only product additions/modifications are allowed when workflows exist. " +
        "To modify these fields, please complete or cancel all associated workflows first.",
        String.join(", ", attemptedChanges)
      );
      
      log.error("Workflow-restricted update validation failed for item {}: {}", existingItem.getId(), message);
      throw new IllegalArgumentException(message);
    }
  }
}
