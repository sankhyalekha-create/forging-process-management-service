package com.jangid.forging_process_management_service.resource.product;

import com.jangid.forging_process_management_service.entitiesRepresentation.product.ItemListRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.product.ItemRepresentation;
import com.jangid.forging_process_management_service.exception.TenantNotFoundException;
import com.jangid.forging_process_management_service.service.product.ItemService;
import com.jangid.forging_process_management_service.utils.ResourceUtils;

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

      Long tenantIdLongValue = ResourceUtils.convertIdToLong(tenantId)
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
    Long tenantIdLongValue = ResourceUtils.convertIdToLong(tenantId)
        .orElseThrow(() -> new RuntimeException("Not valid tenant id!"));

    Long itemIdLongValue = ResourceUtils.convertIdToLong(itemId)
        .orElseThrow(() -> new RuntimeException("Not valid itemId!"));

    ItemRepresentation updatedItem = itemService.updateItem(tenantIdLongValue, itemIdLongValue, itemRepresentation);
    return ResponseEntity.ok(updatedItem);
  }

  @GetMapping("tenant/{tenantId}/items")
  public ResponseEntity<?> getAllItemsOfTenant(
      @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable String tenantId,
      @RequestParam(value = "page") String page,
      @RequestParam(value = "size") String size) {
    Long tId = ResourceUtils.convertIdToLong(tenantId)
        .orElseThrow(() -> new TenantNotFoundException(tenantId));

    Integer pageNumber = (page == null || page.isBlank()) ? -1
                                                          : ResourceUtils.convertIdToInt(page)
                             .orElseThrow(() -> new RuntimeException("Invalid page=" + page));

    Integer sizeNumber = (size == null || size.isBlank()) ? -1
                                                          : ResourceUtils.convertIdToInt(size)
                             .orElseThrow(() -> new RuntimeException("Invalid size=" + size));

    if (pageNumber == -1 || sizeNumber == -1) {
      ItemListRepresentation itemListRepresentation = itemService.getAllItemsOfTenantWithoutPagination(tId);
      return ResponseEntity.ok(itemListRepresentation); // Returning list instead of paged response
    }

    Page<ItemRepresentation> items = itemService.getAllItemsOfTenant(tId, pageNumber, sizeNumber);
    return ResponseEntity.ok(items);
  }

  @DeleteMapping("tenant/{tenantId}/item/{itemId}")
  public ResponseEntity<Void> deleteItem(@PathVariable("tenantId") String tenantId, @PathVariable("itemId") String itemId) {
    if (tenantId == null || tenantId.isEmpty() || itemId == null) {
      log.error("invalid input for item delete!");
      throw new RuntimeException("invalid input for item delete!");
    }
    Long tenantIdLongValue = ResourceUtils.convertIdToLong(tenantId)
        .orElseThrow(() -> new RuntimeException("Not valid tenant id!"));

    Long itemIdLongValue = ResourceUtils.convertIdToLong(itemId)
        .orElseThrow(() -> new RuntimeException("Not valid itemId!"));

    itemService.deleteItemByIdAndTenantId(itemIdLongValue, tenantIdLongValue);
    return ResponseEntity.noContent().build();
  }

  private boolean isInValidItemRepresentation(ItemRepresentation itemRepresentation) {
    return itemRepresentation.getItemName() == null ||
           itemRepresentation.getItemProducts() == null || itemRepresentation.getItemProducts().isEmpty() ||
           itemRepresentation.getItemWeight() == null;
  }
}
