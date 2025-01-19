package com.jangid.forging_process_management_service.resource.machining;

import com.jangid.forging_process_management_service.assemblers.machining.ProcessedItemMachiningBatchAssembler;
import com.jangid.forging_process_management_service.entities.machining.ProcessedItemMachiningBatch;
import com.jangid.forging_process_management_service.entitiesRepresentation.machining.ProcessedItemMachiningBatchListRepresentation;
import com.jangid.forging_process_management_service.exception.forging.ForgeNotFoundException;
import com.jangid.forging_process_management_service.service.machining.ProcessedItemMachiningBatchService;
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
public class ProcessedItemMachiningBatchResource {
  @Autowired
  private ProcessedItemMachiningBatchService processedItemMachiningBatchService;

  @Autowired
  private ProcessedItemMachiningBatchAssembler processedItemMachiningBatchAssembler;

  @GetMapping(value = "tenant/{tenantId}/processed-item-machining-batches-available-for-rework", produces = MediaType.APPLICATION_JSON)
  public ResponseEntity<ProcessedItemMachiningBatchListRepresentation> getProcessedItemMachiningBatchesAvailableForReworkOfTenant(
      @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable("tenantId") String tenantId) {

    try {
      Long tenantIdLongValue = ResourceUtils.convertIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Not valid tenantId!"));
      List<ProcessedItemMachiningBatch> processedItems = processedItemMachiningBatchService.getProcessedItemMachiningBatchesEligibleForReworkMachining(tenantIdLongValue);
      ProcessedItemMachiningBatchListRepresentation processedItemMachiningBatchListRepresentation = ProcessedItemMachiningBatchListRepresentation.builder()
          .processedItemMachiningBatches(processedItems.stream().map(processedItemMachiningBatchAssembler::dissemble).toList()).build();
      return ResponseEntity.ok(processedItemMachiningBatchListRepresentation);
    } catch (Exception e) {
      if (e instanceof ForgeNotFoundException) {
        return ResponseEntity.ok().build();
      }
      throw e;
    }
  }
}
