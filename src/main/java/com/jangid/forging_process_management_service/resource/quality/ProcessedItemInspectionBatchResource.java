package com.jangid.forging_process_management_service.resource.quality;

import com.jangid.forging_process_management_service.assemblers.quality.ProcessedItemInspectionBatchAssembler;
import com.jangid.forging_process_management_service.entities.quality.ProcessedItemInspectionBatch;
import com.jangid.forging_process_management_service.entitiesRepresentation.quality.ProcessedItemInspectionBatchListRepresentation;
import com.jangid.forging_process_management_service.exception.product.ItemNotFoundException;
import com.jangid.forging_process_management_service.exception.quality.ProcessedItemInspectionBatchNotFound;
import com.jangid.forging_process_management_service.service.quality.ProcessedItemInspectionBatchService;
import com.jangid.forging_process_management_service.utils.ResourceUtils;

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
  private ProcessedItemInspectionBatchService processedItemInspectionBatchService;

  @Autowired
  private ProcessedItemInspectionBatchAssembler processedItemInspectionBatchAssembler;


  @GetMapping(value = "tenant/{tenantId}/item/{itemId}/processed-item-inspection-batches-available-for-dispatch", produces = MediaType.APPLICATION_JSON)
  public ResponseEntity<ProcessedItemInspectionBatchListRepresentation> getProcessedItemInspectionBatchesAvailableForDispatchForItemOfTenant(
      @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable("tenantId") String tenantId,
      @ApiParam(value = "Identifier of the item", required = true) @PathVariable("itemId") String itemId
  ) {

    try {
      Long tenantIdLongValue = ResourceUtils.convertIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Not valid tenantId!"));
      Long itemIdLongValue = ResourceUtils.convertIdToLong(itemId)
          .orElseThrow(() -> new RuntimeException("Not valid itemId!"));
      boolean isItemExistsforTenant = processedItemInspectionBatchService.isItemExistsForTenant(itemIdLongValue, tenantIdLongValue);
      if(!isItemExistsforTenant){
        return ResponseEntity.ok().build();
      }
      List<ProcessedItemInspectionBatch> processedItems = processedItemInspectionBatchService.getProcessedItemInspectionBatchesForItem(itemIdLongValue);

      ProcessedItemInspectionBatchListRepresentation processedItemInspectionBatchListRepresentation = ProcessedItemInspectionBatchListRepresentation.builder()
          .processedItemInspectionBatches(processedItems.stream().map(processedItemInspectionBatchAssembler::dissemble).toList()).build();
      return ResponseEntity.ok(processedItemInspectionBatchListRepresentation);
    } catch (Exception e) {
      if (e instanceof ProcessedItemInspectionBatchNotFound || e instanceof ItemNotFoundException) {
        return ResponseEntity.ok().build();
      }
      throw e;
    }
  }

}
