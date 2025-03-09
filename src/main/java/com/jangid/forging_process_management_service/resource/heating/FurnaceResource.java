package com.jangid.forging_process_management_service.resource.heating;

import com.jangid.forging_process_management_service.entitiesRepresentation.forging.FurnaceRepresentation;
import com.jangid.forging_process_management_service.exception.TenantNotFoundException;
import com.jangid.forging_process_management_service.service.heating.FurnaceService;
import com.jangid.forging_process_management_service.utils.GenericResourceUtils;

import io.swagger.annotations.ApiParam;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Component
public class FurnaceResource {

  @Autowired
  private FurnaceService furnaceService;

  @GetMapping("tenant/{tenantId}/furnaces")
  public ResponseEntity<Page<FurnaceRepresentation>> getAllFurnacesOfTenant(@ApiParam(value = "Identifier of the tenant", required = true) @PathVariable String tenantId,
                                                                            @RequestParam(value = "page", defaultValue = "0") String page,
                                                                            @RequestParam(value = "size", defaultValue = "5") String size) {
    Long tId = GenericResourceUtils.convertResourceIdToLong(tenantId)
        .orElseThrow(() -> new TenantNotFoundException(tenantId));

    int pageNumber = GenericResourceUtils.convertResourceIdToInt(page)
        .orElseThrow(() -> new RuntimeException("Invalid page="+page));

    int sizeNumber = GenericResourceUtils.convertResourceIdToInt(size)
        .orElseThrow(() -> new RuntimeException("Invalid size="+size));


    Page<FurnaceRepresentation> furnacesPage = furnaceService.getAllFurnacesOfTenant(tId, pageNumber, sizeNumber);
    return ResponseEntity.ok(furnacesPage);
  }

  @PostMapping("tenant/{tenantId}/furnace")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<FurnaceRepresentation> createFurnace(@PathVariable String tenantId, @RequestBody FurnaceRepresentation furnaceRepresentation) {
    try {
      if (tenantId == null || tenantId.isEmpty() || furnaceRepresentation.getFurnaceName() == null ||
          furnaceRepresentation.getFurnaceCapacity() == null || furnaceRepresentation.getFurnaceCapacity().isEmpty()) {
        log.error("invalid input!");
        throw new RuntimeException("invalid input!");
      }
      Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Not valid id!"));
      FurnaceRepresentation createdFurnace = furnaceService.createFurnace(tenantIdLongValue, furnaceRepresentation);
      return new ResponseEntity<>(createdFurnace, HttpStatus.CREATED);
    } catch (Exception exception) {
      return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @PostMapping("tenant/{tenantId}/furnace/{furnaceId}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<FurnaceRepresentation> updateFurnace(
      @PathVariable("tenantId") String tenantId, @PathVariable("furnaceId") String furnaceId,
      @RequestBody FurnaceRepresentation furnaceRepresentation) {
    if (tenantId == null || tenantId.isEmpty() || furnaceId == null || furnaceId.isEmpty() || isInvalidFurnaceRepresentation(furnaceRepresentation)) {
      log.error("invalid input for updateFurnace!");
      throw new RuntimeException("invalid input for updateFurnace!");
    }
    Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
        .orElseThrow(() -> new RuntimeException("Not valid tenant id!"));

    Long furnaceIdLongValue = GenericResourceUtils.convertResourceIdToLong(furnaceId)
        .orElseThrow(() -> new RuntimeException("Not valid furnaceId!"));

    FurnaceRepresentation updatedFurnace = furnaceService.updateFurnace(furnaceIdLongValue, tenantIdLongValue, furnaceRepresentation);
    return ResponseEntity.ok(updatedFurnace);
  }

  private boolean isInvalidFurnaceRepresentation(FurnaceRepresentation furnaceRepresentation){
    return furnaceRepresentation.getFurnaceName() == null ||
           furnaceRepresentation.getFurnaceCapacity() == null || furnaceRepresentation.getFurnaceCapacity().isEmpty();
  }
}
