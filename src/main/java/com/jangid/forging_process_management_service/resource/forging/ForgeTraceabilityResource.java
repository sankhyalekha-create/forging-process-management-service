package com.jangid.forging_process_management_service.resource.forging;

import com.jangid.forging_process_management_service.entitiesRepresentation.forging.ForgeTraceabilityRepresentation;
import com.jangid.forging_process_management_service.service.forging.ForgeTraceabilityService;
import com.jangid.forging_process_management_service.utils.ResourceUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
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

  @PostMapping("forgingLine/{forgingLineId}/forgeTraceability")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<ForgeTraceabilityRepresentation> createForgeTraceability(@PathVariable String forgingLineId, @RequestBody ForgeTraceabilityRepresentation forgeTraceabilityRepresentation) {
    try {
      if (forgingLineId == null || forgingLineId.isEmpty() ||
          forgeTraceabilityRepresentation.getForgingLineName() == null || forgeTraceabilityRepresentation.getForgePieceWeight() == null ||
          forgeTraceabilityRepresentation.getHeatId() == null || forgeTraceabilityRepresentation.getHeatIdQuantityUsed() == null ||
          forgeTraceabilityRepresentation.getStartAt() == null) {
        log.error("invalid input!");
        throw new RuntimeException("invalid input!");
      }
      Long forgingLineIdLongValue = ResourceUtils.convertIdToLong(forgingLineId)
          .orElseThrow(() -> new RuntimeException("Not valid id!"));
      ForgeTraceabilityRepresentation createdForgeTraceability = forgeTraceabilityService.createForgeTraceability(forgingLineIdLongValue, forgeTraceabilityRepresentation);
      return new ResponseEntity<>(createdForgeTraceability, HttpStatus.CREATED);
    } catch (Exception exception) {
      return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

}
