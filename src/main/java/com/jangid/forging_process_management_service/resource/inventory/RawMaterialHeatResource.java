package com.jangid.forging_process_management_service.resource.inventory;

import com.jangid.forging_process_management_service.assemblers.inventory.RawMaterialHeatAssembler;
import com.jangid.forging_process_management_service.entities.inventory.Heat;
import com.jangid.forging_process_management_service.entitiesRepresentation.inventory.HeatRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.inventory.HeatListRepresentation;
import com.jangid.forging_process_management_service.service.inventory.RawMaterialHeatService;
import com.jangid.forging_process_management_service.utils.GenericResourceUtils;

import io.swagger.annotations.ApiParam;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Component
public class RawMaterialHeatResource {

  @Autowired
  private final RawMaterialHeatService rawMaterialHeatService;

  @Autowired
  private RawMaterialHeatAssembler rawMaterialHeatAssembler;

  @GetMapping("/tenant/{tenantId}/product/{productId}/heats")
  public ResponseEntity<HeatListRepresentation> getRawMaterialProductHeats(
      @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable("tenantId") String tenantId,
      @ApiParam(value = "Identifier of the product", required = true) @PathVariable("productId") String productId
  ) {
    Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
        .orElseThrow(() -> new RuntimeException("Not valid tenantId!"));

    Long productIdLongValue = GenericResourceUtils.convertResourceIdToLong(productId)
        .orElseThrow(() -> new RuntimeException("Not valid productId!"));

    List<Heat> heats = rawMaterialHeatService.getProductHeats(tenantIdLongValue, productIdLongValue);
    HeatListRepresentation rawMaterialListRepresentation = getRawMaterialHeatListRepresentation(heats);
    return ResponseEntity.ok(rawMaterialListRepresentation);
  }

  @GetMapping("/tenant/{tenantId}/heat-inventory")
  public ResponseEntity<Page<HeatRepresentation>> getHeatInventory(
      @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable("tenantId") String tenantId,
      @ApiParam(value = "Page number (0-based)", required = false) @RequestParam(defaultValue = "0") int page,
      @ApiParam(value = "Page size", required = false) @RequestParam(defaultValue = "10") int size
  ) {
    Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
        .orElseThrow(() -> new RuntimeException("Not valid tenantId!"));

    Page<Heat> heatPage = rawMaterialHeatService.getTenantHeats(tenantIdLongValue, page, size);
    Page<HeatRepresentation> heatRepresentationPage = heatPage.map(rawMaterialHeatAssembler::dissemble);
    return ResponseEntity.ok(heatRepresentationPage);
  }

//  @GetMapping("/tenant/{tenantId}/rawMaterialHeats/available")
//  public ResponseEntity<RawMaterialHeatListRepresentation> getAvailableRawMaterialHeats(
//      @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable("tenantId") String tenantId) {
//    Long tenantIdLongValue = ResourceUtils.convertIdToLong(tenantId)
//        .orElseThrow(() -> new RuntimeException("Not valid tenantId!"));
//
//    List<Heat> heats = rawMaterialHeatService.getTenantsAvailableHeats(tenantIdLongValue);
//    RawMaterialHeatListRepresentation rawMaterialListRepresentation = getRawMaterialHeatListRepresentation(heats);
//    return ResponseEntity.ok(rawMaterialListRepresentation);
//  }

  private HeatListRepresentation getRawMaterialHeatListRepresentation(List<Heat> heats){
    if (heats == null) {
      log.error("heats list is null!");
      return HeatListRepresentation.builder().build();
    }
    List<HeatRepresentation> heatRepresentations = new ArrayList<>();
    heats.forEach(heat -> heatRepresentations.add(rawMaterialHeatAssembler.dissemble(heat)));
    return HeatListRepresentation.builder()
        .heats(heatRepresentations).build();
  }
}
