package com.jangid.forging_process_management_service.resource.product;

import com.jangid.forging_process_management_service.entitiesRepresentation.error.ErrorResponse;
import com.jangid.forging_process_management_service.entitiesRepresentation.product.ItemListRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.product.ItemRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.product.ItemProductRepresentation;
import com.jangid.forging_process_management_service.exception.TenantNotFoundException;
import com.jangid.forging_process_management_service.exception.product.ItemNotFoundException;
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

  @PostMapping("tenant/{tenantId}/item")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<?> addItem(@PathVariable String tenantId, @RequestBody ItemRepresentation itemRepresentation) {
    try {
      if (tenantId == null || tenantId.isEmpty()) {
        log.error("Invalid tenant ID");
        throw new RuntimeException("Invalid tenant ID");
      }
      
      // Check for weight hierarchy validation first
      if (hasInvalidWeightHierarchy(itemRepresentation)) {
        log.error("Invalid weight hierarchy: weights must follow itemWeight >= itemSlugWeight >= itemForgedWeight >= itemFinishedWeight");
        return new ResponseEntity<>(
            new ErrorResponse("Invalid weight hierarchy: weights must follow itemWeight >= itemSlugWeight >= itemForgedWeight >= itemFinishedWeight"),
            HttpStatus.BAD_REQUEST);
      }
      
      // Then check other validations
      if (isInValidItemRepresentation(itemRepresentation)) {
        log.error("Invalid item input!");
        throw new RuntimeException("Invalid item input!");
      }

      Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Not valid id!"));

      ItemRepresentation createdItem = itemService.createItem(tenantIdLongValue, itemRepresentation);
      return new ResponseEntity<>(createdItem, HttpStatus.CREATED);
    } catch (Exception exception) {
      if (exception instanceof IllegalStateException) {
        // Generate a more descriptive error message
        String errorMessage = exception.getMessage();
        log.error("Item creation failed: {}", errorMessage);
        
        if (errorMessage.contains("with name=")) {
          return new ResponseEntity<>(
              new ErrorResponse("An item with the name '" + itemRepresentation.getItemName() + "' already exists for this tenant"),
              HttpStatus.CONFLICT);
        } else if (errorMessage.contains("with code=")) {
          return new ResponseEntity<>(
              new ErrorResponse("An item with the code '" + itemRepresentation.getItemCode() + "' already exists for this tenant"),
              HttpStatus.CONFLICT);
        } else {
          return new ResponseEntity<>(
              new ErrorResponse("An item with the same name or code already exists"),
              HttpStatus.CONFLICT);
        }
      } else if (exception instanceof IllegalArgumentException) {
        log.error("Invalid item data: {}", exception.getMessage());
        return new ResponseEntity<>(
            new ErrorResponse(exception.getMessage()),
            HttpStatus.BAD_REQUEST);
      }
      
      log.error("Error creating item: {}", exception.getMessage());
      return new ResponseEntity<>(
          new ErrorResponse("Error creating item: " + exception.getMessage()),
          HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @PostMapping("tenant/{tenantId}/item/{itemId}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<?> updateItem(
      @PathVariable("tenantId") String tenantId, @PathVariable("itemId") String itemId,
      @RequestBody ItemRepresentation itemRepresentation) {
    if (tenantId == null || tenantId.isEmpty() || itemId == null) {
      log.error("Invalid tenant ID or item ID");
      throw new RuntimeException("Invalid tenant ID or item ID");
    }
    
    // Check for weight hierarchy validation first
    if (hasInvalidWeightHierarchy(itemRepresentation)) {
      log.error("Invalid weight hierarchy: weights must follow itemWeight >= itemSlugWeight >= itemForgedWeight >= itemFinishedWeight");
      return new ResponseEntity<>(
          new ErrorResponse("Invalid weight hierarchy: weights must follow itemWeight >= itemSlugWeight >= itemForgedWeight >= itemFinishedWeight"),
          HttpStatus.BAD_REQUEST);
    }
    
    // Then check other validations
    if (isInValidItemRepresentation(itemRepresentation)) {
      log.error("Invalid input for item update!");
      throw new RuntimeException("Invalid input for item update!");
    }
    
    Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
        .orElseThrow(() -> new RuntimeException("Not valid tenant id!"));

    Long itemIdLongValue = GenericResourceUtils.convertResourceIdToLong(itemId)
        .orElseThrow(() -> new RuntimeException("Not valid itemId!"));

    ItemRepresentation updatedItem = itemService.updateItem(tenantIdLongValue, itemIdLongValue, itemRepresentation);
    return ResponseEntity.ok(updatedItem);
  }

  @GetMapping("tenant/{tenantId}/items")
  public ResponseEntity<?> getAllItemsOfTenant(
      @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable String tenantId,
      @RequestParam(value = "page") String page,
      @RequestParam(value = "size") String size) {
    Long tId = GenericResourceUtils.convertResourceIdToLong(tenantId)
        .orElseThrow(() -> new TenantNotFoundException(tenantId));

    Integer pageNumber = (page == null || page.isBlank()) ? -1
                                                          : GenericResourceUtils.convertResourceIdToInt(page)
                             .orElseThrow(() -> new RuntimeException("Invalid page=" + page));

    Integer sizeNumber = (size == null || size.isBlank()) ? -1
                                                          : GenericResourceUtils.convertResourceIdToInt(size)
                             .orElseThrow(() -> new RuntimeException("Invalid size=" + size));

    if (pageNumber == -1 || sizeNumber == -1) {
      ItemListRepresentation itemListRepresentation = itemService.getAllItemsOfTenantWithoutPagination(tId);
      return ResponseEntity.ok(itemListRepresentation); // Returning list instead of paged response
    }

    Page<ItemRepresentation> items = itemService.getAllItemsOfTenant(tId, pageNumber, sizeNumber);
    return ResponseEntity.ok(items);
  }

  @GetMapping("tenant/{tenantId}/items/by-operation/{operationType}")
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<?> getItemsByOperationType(
      @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable String tenantId,
      @ApiParam(value = "Operation type", required = true, allowableValues = "FORGING,HEAT_TREATMENT,MACHINING,QUALITY,DISPATCH") @PathVariable String operationType,
      @RequestParam(value = "page", required = false) String page,
      @RequestParam(value = "size", required = false) String size) {
    
    try {
      Long tId = GenericResourceUtils.convertResourceIdToLong(tenantId)
          .orElseThrow(() -> new TenantNotFoundException(tenantId));

      // Validate operation type
      if (operationType == null || operationType.trim().isEmpty()) {
        return new ResponseEntity<>(
            new ErrorResponse("Operation type is required"),
            HttpStatus.BAD_REQUEST);
      }

      Integer pageNumber = (page == null || page.isBlank()) ? -1
                                                            : GenericResourceUtils.convertResourceIdToInt(page)
                               .orElseThrow(() -> new RuntimeException("Invalid page=" + page));

      Integer sizeNumber = (size == null || size.isBlank()) ? -1
                                                            : GenericResourceUtils.convertResourceIdToInt(size)
                               .orElseThrow(() -> new RuntimeException("Invalid size=" + size));

      if (pageNumber == -1 || sizeNumber == -1) {
        ItemListRepresentation itemListRepresentation = itemService.getItemsByOperationType(tId, operationType.toUpperCase());
        return ResponseEntity.ok(itemListRepresentation);
      }

      Page<ItemRepresentation> items = itemService.getItemsByOperationType(tId, operationType.toUpperCase(), pageNumber, sizeNumber);
      return ResponseEntity.ok(items);
      
    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "getItemsByOperationType");
    }
  }

  @DeleteMapping("tenant/{tenantId}/item/{itemId}")
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<?> deleteItem(
      @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable String tenantId,
      @ApiParam(value = "Identifier of the item", required = true) @PathVariable String itemId) {

      try {
          Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
              .orElseThrow(() -> new RuntimeException("Not valid tenantId!"));
          Long itemIdLongValue = GenericResourceUtils.convertResourceIdToLong(itemId)
              .orElseThrow(() -> new RuntimeException("Not valid itemId!"));

          itemService.deleteItem(tenantIdLongValue, itemIdLongValue);
          return ResponseEntity.noContent().build();

      } catch (Exception exception) {
          if (exception instanceof ItemNotFoundException) {
              return ResponseEntity.notFound().build();
          }
          if (exception instanceof IllegalStateException) {
              log.error("Error while deleting item: {}", exception.getMessage());
              return new ResponseEntity<>(new ErrorResponse(exception.getMessage()),
                                        HttpStatus.CONFLICT);
          }
          log.error("Error while deleting item: {}", exception.getMessage());
          return new ResponseEntity<>(new ErrorResponse("Error while deleting item"),
                                     HttpStatus.INTERNAL_SERVER_ERROR);
      }
  }

  @GetMapping(value = "tenant/{tenantId}/searchItems", produces = MediaType.APPLICATION_JSON)
  public ResponseEntity<Page<ItemRepresentation>> searchItems(
      @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable("tenantId") String tenantId,
      @ApiParam(value = "Type of search", required = true, allowableValues = "ITEM_NAME,ITEM_CODE") @RequestParam("searchType") String searchType,
      @ApiParam(value = "Search term", required = true) @RequestParam("searchTerm") String searchTerm,
      @ApiParam(value = "Page number (0-based)", required = false) @RequestParam(value = "page", defaultValue = "0") String pageParam,
      @ApiParam(value = "Page size", required = false) @RequestParam(value = "size", defaultValue = "10") String sizeParam) {

    try {
      Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Not valid tenantId!"));
      
      if (searchType == null || searchType.trim().isEmpty()) {
        return ResponseEntity.badRequest().build();
      }
      
      if (searchTerm == null || searchTerm.trim().isEmpty()) {
        return ResponseEntity.badRequest().build();
      }

      int pageNumber = GenericResourceUtils.convertResourceIdToInt(pageParam)
          .orElseThrow(() -> new RuntimeException("Invalid page=" + pageParam));

      int pageSize = GenericResourceUtils.convertResourceIdToInt(sizeParam)
          .orElseThrow(() -> new RuntimeException("Invalid size=" + sizeParam));

      if (pageNumber < 0) {
        pageNumber = 0;
      }

      if (pageSize <= 0) {
        pageSize = 10; // Default page size
      }

      Page<ItemRepresentation> searchResults = itemService.searchItems(tenantIdLongValue, searchType.trim(), searchTerm.trim(), pageNumber, pageSize);
      return ResponseEntity.ok(searchResults);

    } catch (IllegalArgumentException e) {
      log.error("Invalid search parameters: {}", e.getMessage());
      return ResponseEntity.badRequest().build();
    } catch (Exception e) {
      log.error("Error during item search: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
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
