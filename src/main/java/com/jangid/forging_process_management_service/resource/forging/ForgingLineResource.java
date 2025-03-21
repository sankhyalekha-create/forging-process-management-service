package com.jangid.forging_process_management_service.resource.forging;

import com.jangid.forging_process_management_service.assemblers.forging.ForgingLineAssembler;
import com.jangid.forging_process_management_service.entities.forging.ForgingLine;
import com.jangid.forging_process_management_service.entitiesRepresentation.error.ErrorResponse;
import com.jangid.forging_process_management_service.entitiesRepresentation.forging.ForgingLineRepresentation;
import com.jangid.forging_process_management_service.exception.TenantNotFoundException;
import com.jangid.forging_process_management_service.exception.forging.ForgingLineNotFoundException;
import com.jangid.forging_process_management_service.service.forging.ForgingLineService;
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
public class ForgingLineResource {

  @Autowired
  private ForgingLineService forgingLineService;

  @GetMapping("tenant/{tenantId}/forgingLines")
  public ResponseEntity<Page<ForgingLineRepresentation>> getAllForgingLinesOfTenant(@ApiParam(value = "Identifier of the tenant", required = true) @PathVariable String tenantId,
                                                                                        @RequestParam(value = "page", defaultValue = "0") String page,
                                                                                        @RequestParam(value = "size", defaultValue = "5") String size) {
    Long tId = GenericResourceUtils.convertResourceIdToLong(tenantId)
        .orElseThrow(() -> new TenantNotFoundException(tenantId));

    int pageNumber = GenericResourceUtils.convertResourceIdToInt(page)
        .orElseThrow(() -> new RuntimeException("Invalid page="+page));

    int sizeNumber = GenericResourceUtils.convertResourceIdToInt(size)
        .orElseThrow(() -> new RuntimeException("Invalid size="+size));

    Page<ForgingLineRepresentation> forgingLines = forgingLineService.getAllForgingLinesByTenant(tId, pageNumber, sizeNumber);
    return ResponseEntity.ok(forgingLines);
  }

  @PostMapping("tenant/{tenantId}/forgingLine")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<ForgingLineRepresentation> createForgingLine(@PathVariable String tenantId, @RequestBody ForgingLineRepresentation forgingLineRepresentation) {
    try {
      if (tenantId == null || tenantId.isEmpty() || forgingLineRepresentation.getForgingLineName() == null ) {
        log.error("invalid input!");
        throw new RuntimeException("invalid input!");
      }
      Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Not valid id!"));
      ForgingLineRepresentation createdForgingLine = forgingLineService.createForgingLine(tenantIdLongValue, forgingLineRepresentation);
      return new ResponseEntity<>(createdForgingLine, HttpStatus.CREATED);
    } catch (Exception exception) {
      return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @PostMapping("tenant/{tenantId}/forgingLine/{forgingLineId}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<ForgingLineRepresentation> updateForgingLine(
      @PathVariable("tenantId") String tenantId, @PathVariable("forgingLineId") String forgingLineId,
      @RequestBody ForgingLineRepresentation forgingLineRepresentation) {
    if (tenantId == null || tenantId.isEmpty() || forgingLineId == null) {
      log.error("invalid input for update!");
      throw new RuntimeException("invalid input for update!");
    }
    Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
        .orElseThrow(() -> new RuntimeException("Not valid tenant id!"));

    Long forgingLineIdLongValue = GenericResourceUtils.convertResourceIdToLong(forgingLineId)
        .orElseThrow(() -> new RuntimeException("Not valid forgingLineId!"));

    ForgingLine updatedForgingLine = forgingLineService.updateForgingLine(tenantIdLongValue, forgingLineIdLongValue, forgingLineRepresentation);

    return ResponseEntity.ok(ForgingLineAssembler.dissemble(updatedForgingLine));
  }

  @DeleteMapping("tenant/{tenantId}/forgingLine/{forgingLineId}")
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<?> deleteForgingLine(
      @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable String tenantId,
      @ApiParam(value = "Identifier of the forgingLine", required = true) @PathVariable String forgingLineId) {

    try {
      Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Not valid tenantId!"));
      Long forgingLineIdLongValue = GenericResourceUtils.convertResourceIdToLong(forgingLineId)
          .orElseThrow(() -> new RuntimeException("Not valid forgingLineId!"));

      forgingLineService.deleteForgingLine(forgingLineIdLongValue, tenantIdLongValue);
      return ResponseEntity.ok().build();

    } catch (Exception exception) {
      if (exception instanceof ForgingLineNotFoundException) {
        return ResponseEntity.notFound().build();
      }
      if (exception.getMessage().contains("not in FORGE_NOT_APPLIED status")) {
        log.error("Cannot delete forgingLine as it is not in FORGE_NOT_APPLIED status!");
        return new ResponseEntity<>(new ErrorResponse("Cannot delete forgingLine as it is not in FORGE_NOT_APPLIED status!"), HttpStatus.CONFLICT);
      }
      log.error("Error while deleting forging line: {}", exception.getMessage());
      return new ResponseEntity<>(new ErrorResponse("Error while deleting forging line"),
                                 HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }
}

