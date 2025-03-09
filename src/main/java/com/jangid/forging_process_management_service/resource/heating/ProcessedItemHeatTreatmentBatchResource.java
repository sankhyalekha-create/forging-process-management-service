package com.jangid.forging_process_management_service.resource.heating;

import com.jangid.forging_process_management_service.assemblers.heating.ProcessedItemHeatTreatmentBatchAssembler;
import com.jangid.forging_process_management_service.entities.heating.ProcessedItemHeatTreatmentBatch;
import com.jangid.forging_process_management_service.entitiesRepresentation.heating.ProcessedItemHeatTreatmentBatchListRepresentation;
import com.jangid.forging_process_management_service.exception.forging.ForgeNotFoundException;
import com.jangid.forging_process_management_service.service.heating.ProcessedItemHeatTreatmentBatchService;
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
public class ProcessedItemHeatTreatmentBatchResource {
  @Autowired
  private ProcessedItemHeatTreatmentBatchService processedItemHeatTreatmentBatchService;
  @Autowired
  private ItemService itemService;

  @Autowired
  private ProcessedItemHeatTreatmentBatchAssembler processedItemHeatTreatmentBatchAssembler;

  @GetMapping(value = "tenant/{tenantId}/item/{itemId}/processed-item-heat-treatment-batches", produces = MediaType.APPLICATION_JSON)
  public ResponseEntity<ProcessedItemHeatTreatmentBatchListRepresentation> getProcessedItemHeatTreatmentBatchesOfTenant(
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
      List<ProcessedItemHeatTreatmentBatch> processedItems = processedItemHeatTreatmentBatchService.getProcessedItemHeatTreatmentBatchesEligibleForMachining(itemIdLongValue);
      ProcessedItemHeatTreatmentBatchListRepresentation processedItemHeatTreatmentBatchListRepresentation = ProcessedItemHeatTreatmentBatchListRepresentation.builder()
          .processedItemHeatTreatmentBatches(processedItems.stream().map(processedItemHeatTreatmentBatchAssembler::dissemble).toList()).build();
      return ResponseEntity.ok(processedItemHeatTreatmentBatchListRepresentation);
    } catch (Exception e) {
      if (e instanceof ForgeNotFoundException) {
        return ResponseEntity.ok().build();
      }
      throw e;
    }
  }
}
