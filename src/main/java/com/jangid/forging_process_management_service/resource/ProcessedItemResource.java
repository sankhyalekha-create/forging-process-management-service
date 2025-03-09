package com.jangid.forging_process_management_service.resource;

import com.jangid.forging_process_management_service.assemblers.ProcessedItemAssembler;
import com.jangid.forging_process_management_service.entities.ProcessedItem;
import com.jangid.forging_process_management_service.entitiesRepresentation.ProcessedItemListRepresentation;
import com.jangid.forging_process_management_service.exception.forging.ForgeNotFoundException;
import com.jangid.forging_process_management_service.service.ProcessedItemService;
import com.jangid.forging_process_management_service.service.product.ItemService;
import com.jangid.forging_process_management_service.utils.GenericResourceUtils;

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

  @GetMapping(value = "tenant/{tenantId}/processedItems", produces = MediaType.APPLICATION_JSON)
  public ResponseEntity<ProcessedItemListRepresentation> getProcessedItemOfTenant(
      @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable("tenantId") String tenantId) {

    try {
      Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Not valid tenantId!"));
      List<ProcessedItem> processedItems = processedItemService.getProcessedItemList(tenantIdLongValue);
      ProcessedItemListRepresentation processedItemListRepresentation = ProcessedItemListRepresentation.builder()
          .processedItems(processedItems.stream().map(processedItemAssembler::dissemble).toList()).build();
      return ResponseEntity.ok(processedItemListRepresentation);
    } catch (Exception e) {
      if (e instanceof ForgeNotFoundException) {
        return ResponseEntity.ok().build();
      }
      throw e;
    }
  }

  @GetMapping(value = "tenant/{tenantId}/item/{itemId}/forgedProcessedItems", produces = MediaType.APPLICATION_JSON)
  public ResponseEntity<ProcessedItemListRepresentation> getForgedProcessedItemOfTenant(
      @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable("tenantId") String tenantId,
      @ApiParam(value = "Identifier of the item", required = true) @PathVariable("itemId") String itemId) {

    try {
      Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Not valid tenantId!"));
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
    } catch (Exception e) {
      if (e instanceof ForgeNotFoundException) {
        return ResponseEntity.ok().build();
      }
      throw e;
    }
  }
}
