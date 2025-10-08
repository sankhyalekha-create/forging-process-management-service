package com.jangid.forging_process_management_service.resource;

import com.jangid.forging_process_management_service.assemblers.ProcessedItemAssembler;
import com.jangid.forging_process_management_service.configuration.security.TenantContextHolder;
import com.jangid.forging_process_management_service.entities.ProcessedItem;
import com.jangid.forging_process_management_service.entitiesRepresentation.ProcessedItemListRepresentation;
import com.jangid.forging_process_management_service.service.ProcessedItemService;
import com.jangid.forging_process_management_service.service.product.ItemService;
import com.jangid.forging_process_management_service.utils.GenericResourceUtils;
import com.jangid.forging_process_management_service.utils.GenericExceptionHandler;

import io.swagger.annotations.ApiParam;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.ws.rs.core.MediaType;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Component
public class ProcessedItemResource {

  @Autowired
  private final ItemService itemService;

  @Autowired
  private final ProcessedItemService processedItemService;

  @Autowired
  private final ProcessedItemAssembler processedItemAssembler;

  @GetMapping(value = "processedItems", produces = MediaType.APPLICATION_JSON)
  public ResponseEntity<?> getProcessedItemOfTenant(
      ) {

    try {
      Long tenantIdLongValue = TenantContextHolder.getAuthenticatedTenantId();
      List<ProcessedItem> processedItems = processedItemService.getProcessedItemList(tenantIdLongValue);
      ProcessedItemListRepresentation processedItemListRepresentation = ProcessedItemListRepresentation.builder()
          .processedItems(processedItems.stream().map(processedItemAssembler::dissemble).toList()).build();
      return ResponseEntity.ok(processedItemListRepresentation);
    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "getProcessedItemOfTenant");
    }
  }

  @GetMapping(value = "item/{itemId}/forgedProcessedItems", produces = MediaType.APPLICATION_JSON)
  public ResponseEntity<?> getForgedProcessedItemOfTenant(
      
      @ApiParam(value = "Identifier of the item", required = true) @PathVariable("itemId") String itemId) {

    try {
      Long tenantIdLongValue = TenantContextHolder.getAuthenticatedTenantId();
      Long itemIdLongValue = GenericResourceUtils.convertResourceIdToLong(itemId)
          .orElseThrow(() -> new RuntimeException("Not valid itemId!"));
      boolean itemExistsForTenant = itemService.isItemExistsForTenant(itemIdLongValue, tenantIdLongValue);
      if(!itemExistsForTenant){
        return ResponseEntity.ok().build();
      }
      List<ProcessedItem> processedItems = processedItemService.getProcessedItemListEligibleForHeatTreatmentForItem(itemIdLongValue);
      ProcessedItemListRepresentation processedItemListRepresentation = ProcessedItemListRepresentation.builder()
          .processedItems(processedItems.stream().map(processedItemAssembler::dissemble).toList()).build();
      return ResponseEntity.ok(processedItemListRepresentation);
    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "getForgedProcessedItemOfTenant");
    }
  }
}
