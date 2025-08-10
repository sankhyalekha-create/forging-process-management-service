package com.jangid.forging_process_management_service.resource.inventory;

import com.jangid.forging_process_management_service.assemblers.inventory.RawMaterialHeatAssembler;
import com.jangid.forging_process_management_service.entities.inventory.Heat;
import com.jangid.forging_process_management_service.entitiesRepresentation.inventory.HeatRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.inventory.HeatListRepresentation;
import com.jangid.forging_process_management_service.service.inventory.RawMaterialHeatService;
import com.jangid.forging_process_management_service.utils.GenericResourceUtils;
import com.jangid.forging_process_management_service.utils.GenericExceptionHandler;

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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

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
  public ResponseEntity<?> getRawMaterialProductHeats(
      @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable("tenantId") String tenantId,
      @ApiParam(value = "Identifier of the product", required = true) @PathVariable("productId") String productId,
      @ApiParam(value = "Filter by active status (true for active, false for inactive). Default is true.") @RequestParam(value = "active", required = false, defaultValue = "true") Boolean active
  ) {
    try {
      Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Not valid tenantId!"));

      Long productIdLongValue = GenericResourceUtils.convertResourceIdToLong(productId)
          .orElseThrow(() -> new RuntimeException("Not valid productId!"));

      List<Heat> heats = rawMaterialHeatService.getProductHeatsByActiveStatus(tenantIdLongValue, productIdLongValue, active);
      HeatListRepresentation rawMaterialListRepresentation = getRawMaterialHeatListRepresentation(heats);
      return ResponseEntity.ok(rawMaterialListRepresentation);
    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "getRawMaterialProductHeats");
    }
  }

  @GetMapping("/tenant/{tenantId}/heat-inventory")
  public ResponseEntity<?> getHeatInventory(
      @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable("tenantId") String tenantId,
      @RequestParam(value = "page") String page,
      @RequestParam(value = "size") String size
  ) {
    try {
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
    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "getHeatInventory");
    }
  }

  @GetMapping("/tenant/{tenantId}/heat/{heatId}")
  public ResponseEntity<?> getHeatById(
      @ApiParam(value = "Identifier of the heat", required = true) @PathVariable("heatId") String heatId
  ) {
    try {
      Long heatIdLongValue = GenericResourceUtils.convertResourceIdToLong(heatId)
          .orElseThrow(() -> new RuntimeException("Not valid heatId!"));

      Heat heat = rawMaterialHeatService.getHeatById(heatIdLongValue);
      HeatRepresentation heatRepresentation = rawMaterialHeatAssembler.dissemble(heat);
      return ResponseEntity.ok(heatRepresentation);
    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "getHeatById");
    }
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

  @GetMapping("/tenant/{tenantId}/heat-inventory/inactive")
  public ResponseEntity<?> getInactiveHeatInventory(
      @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable("tenantId") String tenantId,
      @RequestParam(value = "page", required = false) String page,
      @RequestParam(value = "size", required = false) String size
  ) {
    try {
      Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Not valid tenantId!"));

      Integer pageNumber = (page == null || page.isBlank()) ? -1
                                                            : GenericResourceUtils.convertResourceIdToInt(page)
                               .orElseThrow(() -> new RuntimeException("Invalid page=" + page));

      Integer sizeNumber = (size == null || size.isBlank()) ? -1
                                                            : GenericResourceUtils.convertResourceIdToInt(size)
                               .orElseThrow(() -> new RuntimeException("Invalid size=" + size));

      if (pageNumber == -1 || sizeNumber == -1) {
        List<Heat> inactiveHeats = rawMaterialHeatService.getAllInactiveTenantHeats(tenantIdLongValue);
        HeatListRepresentation heatListRepresentation = getRawMaterialHeatListRepresentation(inactiveHeats);
        return ResponseEntity.ok(heatListRepresentation);
      }

      Page<Heat> inactiveHeatPage = rawMaterialHeatService.getInactiveTenantHeats(tenantIdLongValue, pageNumber, sizeNumber);
      Page<HeatRepresentation> heatRepresentationPage = inactiveHeatPage.map(rawMaterialHeatAssembler::dissemble);
      return ResponseEntity.ok(heatRepresentationPage);
    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "getInactiveHeatInventory");
    }
  }

  @PostMapping("/tenant/{tenantId}/heat/activate")
  public ResponseEntity<?> activateHeats(
      @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable("tenantId") String tenantId,
      @ApiParam(value = "Identifiers of the heats to activate", required = true) @RequestBody List<String> heatIds
  ) {
    try {
      List<Long> heatIdLongValues = new ArrayList<>();
      for (String heatId : heatIds) {
        heatIdLongValues.add(GenericResourceUtils.convertResourceIdToLong(heatId)
            .orElseThrow(() -> new RuntimeException("Not valid heatId: " + heatId)));
      }
      rawMaterialHeatService.markHeatsAsActive(heatIdLongValues);
      return ResponseEntity.ok("Heats have been successfully activated.");
    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "activateHeats");
    }
  }

  @PostMapping("/tenant/{tenantId}/heat/deactivate")
  public ResponseEntity<?> deactivateHeats(
      @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable("tenantId") String tenantId,
      @ApiParam(value = "Identifiers of the heats to deactivate", required = true) @RequestBody List<String> heatIds
  ) {
    try {
      List<Long> heatIdLongValues = new ArrayList<>();
      for (String heatId : heatIds) {
        heatIdLongValues.add(GenericResourceUtils.convertResourceIdToLong(heatId)
            .orElseThrow(() -> new RuntimeException("Not valid heatId: " + heatId)));
      }
      rawMaterialHeatService.markHeatsAsInactive(heatIdLongValues);
      return ResponseEntity.ok("Heats have been successfully deactivated.");
    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "deactivateHeats");
    }
  }

}
