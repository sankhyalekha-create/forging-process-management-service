package com.jangid.forging_process_management_service.resource.product;

import com.jangid.forging_process_management_service.configuration.security.TenantContextHolder;
import com.jangid.forging_process_management_service.entitiesRepresentation.product.ItemListRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.product.ItemRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.product.ItemProductRepresentation;
import com.jangid.forging_process_management_service.service.product.ItemService;
import com.jangid.forging_process_management_service.utils.GenericResourceUtils;
import com.jangid.forging_process_management_service.utils.GenericExceptionHandler;

import io.swagger.annotations.ApiParam;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Component
public class ItemResource {

  @Autowired
  private ItemService itemService;

  @PostMapping("item")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<?> addItem(@RequestBody ItemRepresentation itemRepresentation) {
    try {
      // Check for weight hierarchy validation first
      if (hasInvalidWeightHierarchy(itemRepresentation)) {
        log.error("Invalid weight hierarchy: weights must follow itemWeight >= itemSlugWeight >= itemForgedWeight >= itemFinishedWeight");
        throw new IllegalArgumentException("Invalid weight hierarchy: weights must follow itemWeight >= itemSlugWeight >= itemForgedWeight >= itemFinishedWeight");
      }
      
      // Then check other validations
      if (isInValidItemRepresentation(itemRepresentation)) {
        log.error("Invalid item input!");
        throw new RuntimeException("Invalid item input!");
      }

      // Get tenant ID from authenticated context
      Long tenantId = TenantContextHolder.getAuthenticatedTenantId();

      ItemRepresentation createdItem = itemService.createItem(tenantId, itemRepresentation);
      return new ResponseEntity<>(createdItem, HttpStatus.CREATED);
    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "addItem");
    }
  }

  @PostMapping("item/{itemId}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<?> updateItem(
      @PathVariable("itemId") String itemId,
      @RequestBody ItemRepresentation itemRepresentation) {
    try {
      if (itemId == null) {
        log.error("Invalid item ID");
        throw new RuntimeException("Invalid item ID");
      }
      
      // Check for weight hierarchy validation first
      if (hasInvalidWeightHierarchy(itemRepresentation)) {
        log.error("Invalid weight hierarchy: weights must follow itemWeight >= itemSlugWeight >= itemForgedWeight >= itemFinishedWeight");
        throw new IllegalArgumentException("Invalid weight hierarchy: weights must follow itemWeight >= itemSlugWeight >= itemForgedWeight >= itemFinishedWeight");
      }
      
      // Then check other validations
      if (isInValidItemRepresentation(itemRepresentation)) {
        log.error("Invalid input for item update!");
        throw new RuntimeException("Invalid input for item update!");
      }
      
      // Get tenant ID from authenticated context
      Long tenantId = TenantContextHolder.getAuthenticatedTenantId();

      Long itemIdLongValue = GenericResourceUtils.convertResourceIdToLong(itemId)
          .orElseThrow(() -> new RuntimeException("Not valid itemId!"));

      ItemRepresentation updatedItem = itemService.updateItem(tenantId, itemIdLongValue, itemRepresentation);
      return ResponseEntity.ok(updatedItem);
    } catch (IllegalArgumentException exception) {
      // Handle workflow restriction and validation errors with specific HTTP status
      log.error("Validation error during item update for itemId={}: {}", itemId, exception.getMessage());
      Long tenantId = TenantContextHolder.getAuthenticatedTenantId();
      return ResponseEntity.badRequest().body(Map.of(
        "error", "VALIDATION_ERROR",
        "message", exception.getMessage(),
        "itemId", itemId,
        "tenantId", tenantId
      ));
    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "updateItem");
    }
  }

  @GetMapping("items")
  public ResponseEntity<?> getAllItemsOfTenant(
      @RequestParam(value = "page") String page,
      @RequestParam(value = "size") String size) {
    try {
      // Get tenant ID from authenticated context
      Long tenantId = TenantContextHolder.getAuthenticatedTenantId();

      Integer pageNumber = (page == null || page.isBlank()) ? -1
        : GenericResourceUtils.convertResourceIdToInt(page)
        .orElseThrow(() -> new RuntimeException("Invalid page=" + page));

      Integer sizeNumber = (size == null || size.isBlank()) ? -1
        : GenericResourceUtils.convertResourceIdToInt(size)
        .orElseThrow(() -> new RuntimeException("Invalid size=" + size));

      if (pageNumber == -1 || sizeNumber == -1) {
        ItemListRepresentation itemListRepresentation = itemService.getAllItemsOfTenantWithoutPagination(tenantId);
        return ResponseEntity.ok(itemListRepresentation);
      }

      Page<ItemRepresentation> items = itemService.getAllItemsOfTenant(tenantId, pageNumber, sizeNumber);
      return ResponseEntity.ok(items);
    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "getAllItemsOfTenant");
    }
  }

  @GetMapping("items/by-operation/{operationType}")
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<?> getItemsByOperationType(
      @ApiParam(value = "Operation type", required = true, allowableValues = "FORGING,HEAT_TREATMENT,MACHINING,QUALITY,DISPATCH") @PathVariable String operationType,
      @RequestParam(value = "page", required = false) String page,
      @RequestParam(value = "size", required = false) String size) {
    
    try {
      // Get tenant ID from authenticated context
      Long tenantId = TenantContextHolder.getAuthenticatedTenantId();

      // Validate operation type
      if (operationType == null || operationType.trim().isEmpty()) {
        throw new IllegalArgumentException("Operation type is required");
      }

      Integer pageNumber = (page == null || page.isBlank()) ? -1
        : GenericResourceUtils.convertResourceIdToInt(page)
        .orElseThrow(() -> new RuntimeException("Invalid page=" + page));

      Integer sizeNumber = (size == null || size.isBlank()) ? -1
        : GenericResourceUtils.convertResourceIdToInt(size)
        .orElseThrow(() -> new RuntimeException("Invalid size=" + size));

      if (pageNumber == -1 || sizeNumber == -1) {
        ItemListRepresentation itemListRepresentation = itemService.getItemsByOperationType(tenantId, operationType.toUpperCase());
        return ResponseEntity.ok(itemListRepresentation);
      }

      Page<ItemRepresentation> items = itemService.getItemsByOperationType(tenantId, operationType.toUpperCase(), pageNumber, sizeNumber);
      return ResponseEntity.ok(items);
      
    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "getItemsByOperationType");
    }
  }

  @DeleteMapping("item/{itemId}")
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<?> deleteItem(
      @ApiParam(value = "Identifier of the item", required = true) @PathVariable String itemId) {

      try {
          // Get tenant ID from authenticated context
          Long tenantId = TenantContextHolder.getAuthenticatedTenantId();
          
          Long itemIdLongValue = GenericResourceUtils.convertResourceIdToLong(itemId)
              .orElseThrow(() -> new RuntimeException("Not valid itemId!"));

          itemService.deleteItem(tenantId, itemIdLongValue);
          return ResponseEntity.noContent().build();

      } catch (Exception exception) {
          return GenericExceptionHandler.handleException(exception, "deleteItem");
      }
  }

  @GetMapping(value = "searchItems", produces = MediaType.APPLICATION_JSON)
  public ResponseEntity<?> searchItems(
      @ApiParam(value = "Type of search", required = true, allowableValues = "ITEM_NAME,ITEM_CODE") @RequestParam("searchType") String searchType,
      @ApiParam(value = "Search term", required = true) @RequestParam("searchTerm") String searchTerm,
      @ApiParam(value = "Page number (0-based)", required = false) @RequestParam(value = "page", defaultValue = "0") String pageParam,
      @ApiParam(value = "Page size", required = false) @RequestParam(value = "size", defaultValue = "10") String sizeParam) {

    try {
      // Get tenant ID from authenticated context
      Long tenantId = TenantContextHolder.getAuthenticatedTenantId();
      
      if (searchType == null || searchType.trim().isEmpty()) {
        throw new IllegalArgumentException("Search type is required");
      }
      
      if (searchTerm == null || searchTerm.trim().isEmpty()) {
        throw new IllegalArgumentException("Search term is required");
      }

      int pageNumber = GenericResourceUtils.convertResourceIdToInt(pageParam)
        .orElseThrow(() -> new RuntimeException("Invalid page=" + pageParam));

      int pageSize = GenericResourceUtils.convertResourceIdToInt(sizeParam)
        .orElseThrow(() -> new RuntimeException("Invalid size=" + sizeParam));

      if (pageNumber < 0) {
        pageNumber = 0;
      }

      if (pageSize <= 0) {
        pageSize = 10;
      }

      Page<ItemRepresentation> searchResults = itemService.searchItems(tenantId, searchType.trim(), searchTerm.trim(), pageNumber, pageSize);
      return ResponseEntity.ok(searchResults);

    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "searchItems");
    }
  }

  /**
   * Validates that weights follow the hierarchy: itemWeight >= itemSlugWeight >= itemForgedWeight >= itemFinishedWeight
   * @param itemRepresentation The item representation to validate
   * @return true if the weight hierarchy is invalid, false otherwise
   */
  private boolean hasInvalidWeightHierarchy(ItemRepresentation itemRepresentation) {
    // Skip validation if any of the weights are null or if we're dealing with PIECES
    if (itemRepresentation.getItemWeight() == null || 
        itemRepresentation.getItemSlugWeight() == null || 
        itemRepresentation.getItemForgedWeight() == null || 
        itemRepresentation.getItemFinishedWeight() == null) {
      return false;
    }
    
    try {
      double itemWeight = Double.parseDouble(itemRepresentation.getItemWeight());
      double itemSlugWeight = Double.parseDouble(itemRepresentation.getItemSlugWeight());
      double itemForgedWeight = Double.parseDouble(itemRepresentation.getItemForgedWeight());
      double itemFinishedWeight = Double.parseDouble(itemRepresentation.getItemFinishedWeight());
      
      return itemWeight < itemSlugWeight || 
             itemSlugWeight < itemForgedWeight || 
             itemForgedWeight < itemFinishedWeight;
    } catch (NumberFormatException e) {
      // If weights can't be parsed as numbers, consider it an invalid hierarchy
      return true;
    }
  }

  private boolean isInValidItemRepresentation(ItemRepresentation itemRepresentation) {
    boolean hasValidProducts = itemRepresentation.getItemProducts() != null && 
                              !itemRepresentation.getItemProducts().isEmpty();
    
    // For simplicity in API, we'll consider the representation valid if:
    // 1. Either itemWeight is provided (required for KGS)
    // 2. Or we're dealing with PIECES (where itemCount is automatically set to 1)
    boolean hasKgsProduct = false;
    boolean hasPiecesProduct = false;
    
    if (hasValidProducts) {
      for (ItemProductRepresentation itemProduct : itemRepresentation.getItemProducts()) {
        if (itemProduct.getProduct() != null && itemProduct.getProduct().getUnitOfMeasurement() != null) {
          if ("KGS".equals(itemProduct.getProduct().getUnitOfMeasurement())) {
            hasKgsProduct = true;
          } else if ("PIECES".equals(itemProduct.getProduct().getUnitOfMeasurement())) {
            hasPiecesProduct = true;
          }
        }
      }
    }
    
    boolean hasValidMeasurement = !hasPiecesProduct || !hasKgsProduct;
    boolean hasInValidQuantity = false;

    if (hasKgsProduct) {
      hasInValidQuantity = itemRepresentation.getItemWeight() == null || 
                           itemRepresentation.getItemSlugWeight() == null || 
                           itemRepresentation.getItemForgedWeight() == null || 
                           itemRepresentation.getItemFinishedWeight() == null;
    }
                                 
    return itemRepresentation.getItemName() == null || itemRepresentation.getItemCode() == null || !hasValidProducts || !hasValidMeasurement || hasInValidQuantity;
  }


}
