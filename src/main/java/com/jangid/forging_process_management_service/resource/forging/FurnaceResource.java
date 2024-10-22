package com.jangid.forging_process_management_service.resource.forging;

import com.jangid.forging_process_management_service.assemblers.forging.FurnaceAssembler;
import com.jangid.forging_process_management_service.entities.forging.Furnace;
import com.jangid.forging_process_management_service.entitiesRepresentation.forging.FurnaceListRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.forging.FurnaceRepresentation;
import com.jangid.forging_process_management_service.exception.TenantNotFoundException;
import com.jangid.forging_process_management_service.service.forging.FurnaceService;
import com.jangid.forging_process_management_service.utils.ResourceUtils;

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

import java.util.List;
import java.util.stream.Collectors;

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
                                                                            @RequestParam(value = "page", defaultValue = "1") String page,
                                                                            @RequestParam(value = "size", defaultValue = "5") String size) {
    Long tId = ResourceUtils.convertIdToLong(tenantId)
        .orElseThrow(() -> new TenantNotFoundException(tenantId));

    int pageNumber = ResourceUtils.convertIdToInt(page)
        .orElseThrow(() -> new RuntimeException("Invalid page="+page));

    int sizeNumber = ResourceUtils.convertIdToInt(size)
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
          furnaceRepresentation.getFurnaceCapacity() == 0f) {
        log.error("invalid input!");
        throw new RuntimeException("invalid input!");
      }
      Long tenantIdLongValue = ResourceUtils.convertIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Not valid id!"));
      FurnaceRepresentation createdFurnace = furnaceService.createFurnace(tenantIdLongValue, furnaceRepresentation);
      return new ResponseEntity<>(createdFurnace, HttpStatus.CREATED);
    } catch (Exception exception) {
      return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
    }

  }
}
