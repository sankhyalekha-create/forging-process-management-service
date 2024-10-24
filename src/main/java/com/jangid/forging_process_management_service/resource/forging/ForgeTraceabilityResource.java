package com.jangid.forging_process_management_service.resource.forging;

import com.jangid.forging_process_management_service.assemblers.forging.ForgeTraceabilityAssembler;
import com.jangid.forging_process_management_service.entities.forging.ForgeTraceability;
import com.jangid.forging_process_management_service.entities.forging.ForgingLine;
import com.jangid.forging_process_management_service.entitiesRepresentation.forging.ForgeTraceabilityRepresentation;
import com.jangid.forging_process_management_service.exception.forging.ForgeTraceabilityNotFoundException;
import com.jangid.forging_process_management_service.service.forging.ForgeTraceabilityService;
import com.jangid.forging_process_management_service.utils.ResourceUtils;

import io.swagger.annotations.ApiParam;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Component
public class ForgeTraceabilityResource {

  @Autowired
  private final ForgeTraceabilityService forgeTraceabilityService;

  @Autowired
  private final ForgeTraceabilityAssembler forgeTraceabilityAssembler;

  @PostMapping("tenant/{tenantId}/forgingLine/{forgingLineId}/forgeTraceability")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<ForgeTraceabilityRepresentation> createForgeTraceability(@PathVariable String tenantId, @PathVariable String forgingLineId, @RequestBody ForgeTraceabilityRepresentation forgeTraceabilityRepresentation) {
    try {
      if (forgingLineId == null || forgingLineId.isEmpty() ||
          forgeTraceabilityRepresentation.getForgingLineName() == null || forgeTraceabilityRepresentation.getForgePieceWeight() == null ||
          forgeTraceabilityRepresentation.getHeatNumber() == null || forgeTraceabilityRepresentation.getInvoiceNumber() == null || forgeTraceabilityRepresentation.getHeatIdQuantityUsed() == null) {
        log.error("invalid input!");
        throw new RuntimeException("invalid input!");
      }
      Long tenantIdLongValue = ResourceUtils.convertIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Not valid id!"));
      Long forgingLineIdLongValue = ResourceUtils.convertIdToLong(forgingLineId)
          .orElseThrow(() -> new RuntimeException("Not valid id!"));
      ForgeTraceabilityRepresentation createdForgeTraceability = forgeTraceabilityService.createForgeTraceability(tenantIdLongValue, forgingLineIdLongValue, forgeTraceabilityRepresentation);
      return new ResponseEntity<>(createdForgeTraceability, HttpStatus.CREATED);
    } catch (Exception exception) {
      if (exception instanceof ForgeTraceabilityNotFoundException){
        return ResponseEntity.notFound().build();
      }
      return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @GetMapping(value = "tenant/{tenantId}/forgingLine/{forgingLineId}/forgeTraceability", produces = MediaType.APPLICATION_JSON)
  public ResponseEntity<ForgeTraceabilityRepresentation> getForgeTraceabilityOfForgingLine(
      @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable("tenantId") String tenantId,
      @ApiParam(value = "Identifier of the forgingLine", required = true) @PathVariable("forgingLineId") String forgingLineId) {

    try {
      Long tenantIdLongValue = ResourceUtils.convertIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Not valid tenantId!"));

      Long forgingLineIdLongValue = ResourceUtils.convertIdToLong(forgingLineId)
          .orElseThrow(() -> new RuntimeException("Not valid forgingLineId!"));

      ForgingLine forgingLine = forgeTraceabilityService.getForgingLineUsingTenantIdAndForgingLineId(tenantIdLongValue, forgingLineIdLongValue);
      ForgeTraceability forgeTraceability = forgeTraceabilityService.getForgeTraceabilityByForgingLine(forgingLine.getId());
      ForgeTraceabilityRepresentation representation = forgeTraceabilityAssembler.dissemble(forgeTraceability);
      return ResponseEntity.ok(representation);
    } catch (Exception e) {
      if (e instanceof ForgeTraceabilityNotFoundException) {
        return ResponseEntity.notFound().build();
      }
      throw e;
    }
  }

  @DeleteMapping("tenant/{tenantId}/forgingLine/{forgingLineId}/forgeTraceability")
  public ResponseEntity<Void> deleteForgeTraceability(@PathVariable("tenantId") String tenantId, @PathVariable("forgingLineId") String forgingLineId) {
    if (tenantId == null || tenantId.isEmpty() || forgingLineId == null || forgingLineId.isEmpty()) {
      log.error("invalid input for delete!");
      throw new RuntimeException("invalid input for delete!");
    }
    Long tenantIdLongValue = ResourceUtils.convertIdToLong(tenantId)
        .orElseThrow(() -> new RuntimeException("Not valid tenant id!"));

    Long forgingLineIdLongValue = ResourceUtils.convertIdToLong(forgingLineId)
        .orElseThrow(() -> new RuntimeException("Not valid id!"));
    ForgingLine forgingLine = forgeTraceabilityService.getForgingLineUsingTenantIdAndForgingLineId(tenantIdLongValue, forgingLineIdLongValue);
    ForgeTraceability forgeTraceability = forgeTraceabilityService.getForgeTraceabilityByForgingLine(forgingLine.getId());

    forgeTraceabilityService.deleteForgeTraceability(forgeTraceability);
    return ResponseEntity.noContent().build();
  }


}
