package com.jangid.forging_process_management_service.resource.quality;

import com.jangid.forging_process_management_service.assemblers.quality.ProcessedItemInspectionBatchAssembler;
import com.jangid.forging_process_management_service.entities.quality.ProcessedItemInspectionBatch;
import com.jangid.forging_process_management_service.entitiesRepresentation.quality.ProcessedItemInspectionBatchListRepresentation;
import com.jangid.forging_process_management_service.service.product.ItemService;
import com.jangid.forging_process_management_service.service.quality.ProcessedItemInspectionBatchService;
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
public class ProcessedItemInspectionBatchResource {

  @Autowired
  private ItemService itemService;

  @Autowired
  private ProcessedItemInspectionBatchService processedItemInspectionBatchService;

  @Autowired
  private ProcessedItemInspectionBatchAssembler processedItemInspectionBatchAssembler;


  @GetMapping(value = "tenant/{tenantId}/item/{itemId}/processed-item-inspection-batches-available-for-dispatch", produces = MediaType.APPLICATION_JSON)
  public ResponseEntity<?> getProcessedItemInspectionBatchesAvailableForDispatchForItemOfTenant(
      @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable("tenantId") String tenantId,
      @ApiParam(value = "Identifier of the item", required = true) @PathVariable("itemId") String itemId
  ) {

    try {
      Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Not valid tenantId!"));
      Long itemIdLongValue = GenericResourceUtils.convertResourceIdToLong(itemId)
          .orElseThrow(() -> new RuntimeException("Not valid itemId!"));
      boolean itemExistsForTenant = itemService.isItemExistsForTenant(itemIdLongValue, tenantIdLongValue);
      if(!itemExistsForTenant){
        return ResponseEntity.ok().build();
      }
      List<ProcessedItemInspectionBatch> processedItems = processedItemInspectionBatchService.getProcessedItemInspectionBatchesForItem(itemIdLongValue);

      ProcessedItemInspectionBatchListRepresentation processedItemInspectionBatchListRepresentation = ProcessedItemInspectionBatchListRepresentation.builder()
          .processedItemInspectionBatches(processedItems.stream().map(processedItemInspectionBatchAssembler::dissemble).toList()).build();
      return ResponseEntity.ok(processedItemInspectionBatchListRepresentation);
    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "getProcessedItemInspectionBatchesAvailableForDispatchForItemOfTenant");
    }
  }

}
