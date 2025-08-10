package com.jangid.forging_process_management_service.resource.machining;

import com.jangid.forging_process_management_service.assemblers.machining.ProcessedItemMachiningBatchAssembler;
import com.jangid.forging_process_management_service.entities.machining.ProcessedItemMachiningBatch;
import com.jangid.forging_process_management_service.entitiesRepresentation.machining.ProcessedItemMachiningBatchListRepresentation;
import com.jangid.forging_process_management_service.service.machining.ProcessedItemMachiningBatchService;
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
public class ProcessedItemMachiningBatchResource {

  @Autowired
  private ItemService itemService;
  @Autowired
  private ProcessedItemMachiningBatchService processedItemMachiningBatchService;

  @Autowired
  private ProcessedItemMachiningBatchAssembler processedItemMachiningBatchAssembler;

  @GetMapping(value = "tenant/{tenantId}/item/{itemId}/processed-item-machining-batches-available-for-rework", produces = MediaType.APPLICATION_JSON)
  public ResponseEntity<?> getProcessedItemMachiningBatchesAvailableForReworkOfTenant(
      @ApiParam(value = "Identifier of the item", required = true) @PathVariable("itemId") String itemId,
      @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable("tenantId") String tenantId) {

    try {
      Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
          .orElseThrow(() -> new IllegalArgumentException("Not valid tenantId!"));

      Long itemIdLongValue = GenericResourceUtils.convertResourceIdToLong(itemId)
          .orElseThrow(() -> new IllegalArgumentException("Not valid itemId!"));
      List<ProcessedItemMachiningBatch> processedItems = processedItemMachiningBatchService.getProcessedItemMachiningBatchesEligibleForReworkMachining(tenantIdLongValue, itemIdLongValue);
      ProcessedItemMachiningBatchListRepresentation processedItemMachiningBatchListRepresentation = ProcessedItemMachiningBatchListRepresentation.builder()
          .processedItemMachiningBatches(processedItems.stream().map(processedItemMachiningBatchAssembler::dissemble).toList()).build();
      return ResponseEntity.ok(processedItemMachiningBatchListRepresentation);
    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "getProcessedItemMachiningBatchesAvailableForReworkOfTenant");
    }
  }

  @GetMapping(value = "tenant/{tenantId}/item/{itemId}/processed-item-machining-batches-available-for-inspection", produces = MediaType.APPLICATION_JSON)
  public ResponseEntity<?> getProcessedItemMachiningBatchesAvailableForInspectionForItemOfTenant(
      @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable("tenantId") String tenantId,
      @ApiParam(value = "Identifier of the item", required = true) @PathVariable("itemId") String itemId
  ) {

    try {
      Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
          .orElseThrow(() -> new IllegalArgumentException("Not valid tenantId!"));
      Long itemIdLongValue = GenericResourceUtils.convertResourceIdToLong(itemId)
          .orElseThrow(() -> new IllegalArgumentException("Not valid itemId!"));
      boolean isItemExistsforTenant = itemService.isItemExistsForTenant(itemIdLongValue, tenantIdLongValue);
      if(!isItemExistsforTenant){
        return ResponseEntity.ok().build();
      }
      List<ProcessedItemMachiningBatch> processedItems = processedItemMachiningBatchService.getProcessedItemMachiningBatchesForItemAvailableForInspection(itemIdLongValue);

      ProcessedItemMachiningBatchListRepresentation processedItemMachiningBatchListRepresentation = ProcessedItemMachiningBatchListRepresentation.builder()
          .processedItemMachiningBatches(processedItems.stream().map(processedItemMachiningBatchAssembler::dissemble).toList()).build();
      return ResponseEntity.ok(processedItemMachiningBatchListRepresentation);
    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "getProcessedItemMachiningBatchesAvailableForInspectionForItemOfTenant");
    }
  }

}
