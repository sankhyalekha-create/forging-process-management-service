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
  public ResponseEntity<?> getHeatInventory(
      @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable("tenantId") String tenantId,
      @RequestParam(value = "page") String page,
      @RequestParam(value = "size") String size
  ) {
    Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
        .orElseThrow(() -> new RuntimeException("Not valid tenantId!"));

    Integer pageNumber = (page == null || page.isBlank()) ? -1
                                                          : GenericResourceUtils.convertResourceIdToInt(page)
                             .orElseThrow(() -> new RuntimeException("Invalid page=" + page));

    Integer sizeNumber = (size == null || size.isBlank()) ? -1
                                                          : GenericResourceUtils.convertResourceIdToInt(size)
                             .orElseThrow(() -> new RuntimeException("Invalid size=" + size));

    if (pageNumber == -1 || sizeNumber == -1) {
      List<Heat> heats = rawMaterialHeatService.getAllTenantHeats(tenantIdLongValue);
      HeatListRepresentation heatListRepresentation = getRawMaterialHeatListRepresentation(heats);
      return ResponseEntity.ok(heatListRepresentation); // Returning list instead of paged response
    }

    Page<Heat> heatPage = rawMaterialHeatService.getTenantHeats(tenantIdLongValue, pageNumber, sizeNumber);
    Page<HeatRepresentation> heatRepresentationPage = heatPage.map(rawMaterialHeatAssembler::dissemble);
    return ResponseEntity.ok(heatRepresentationPage);
  }

  @GetMapping("/tenant/{tenantId}/heat/{heatId}")
  public ResponseEntity<HeatRepresentation> getHeatById(
      @ApiParam(value = "Identifier of the heat", required = true) @PathVariable("heatId") String heatId
  ) {
    Long heatIdLongValue = GenericResourceUtils.convertResourceIdToLong(heatId)
        .orElseThrow(() -> new RuntimeException("Not valid heatId!"));

    Heat heat = rawMaterialHeatService.getHeatById(heatIdLongValue);
    HeatRepresentation heatRepresentation = rawMaterialHeatAssembler.dissemble(heat);
    return ResponseEntity.ok(heatRepresentation);
  }

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
