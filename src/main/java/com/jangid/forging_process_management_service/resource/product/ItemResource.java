package com.jangid.forging_process_management_service.resource.product;

import com.jangid.forging_process_management_service.entitiesRepresentation.error.ErrorResponse;
import com.jangid.forging_process_management_service.entitiesRepresentation.product.ItemListRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.product.ItemRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.product.ItemProductRepresentation;
import com.jangid.forging_process_management_service.exception.TenantNotFoundException;
import com.jangid.forging_process_management_service.exception.product.ItemNotFoundException;
import com.jangid.forging_process_management_service.service.product.ItemService;
import com.jangid.forging_process_management_service.utils.GenericResourceUtils;

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
  public ResponseEntity<ItemRepresentation> addItem(@PathVariable String tenantId, @RequestBody ItemRepresentation itemRepresentation) {
    try {
      if (tenantId == null || tenantId.isEmpty() || isInValidItemRepresentation(itemRepresentation)) {
        log.error("invalid item input!");
        throw new RuntimeException("invalid item input!");
      }

      Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Not valid id!"));

      ItemRepresentation createdItem = itemService.createItem(tenantIdLongValue, itemRepresentation);
      return new ResponseEntity<>(createdItem, HttpStatus.CREATED);
    } catch (Exception exception) {
      return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @PostMapping("tenant/{tenantId}/item/{itemId}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<ItemRepresentation> updateItem(
      @PathVariable("tenantId") String tenantId, @PathVariable("itemId") String itemId,
      @RequestBody ItemRepresentation itemRepresentation) {
    if (tenantId == null || tenantId.isEmpty() || itemId == null || isInValidItemRepresentation(itemRepresentation)) {
      log.error("invalid input for item update!");
      throw new RuntimeException("invalid input for item update!");
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
    
    boolean hasValidMeasurement = !hasPiecesProduct || !hasKgsProduct || itemRepresentation.getItemWeight() != null;
                                 
    return itemRepresentation.getItemName() == null || !hasValidProducts || !hasValidMeasurement;
  }
}
