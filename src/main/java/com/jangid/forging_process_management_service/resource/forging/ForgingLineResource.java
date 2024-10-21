package com.jangid.forging_process_management_service.resource.forging;

import com.jangid.forging_process_management_service.assemblers.forging.ForgingLineAssembler;
import com.jangid.forging_process_management_service.entities.forging.ForgingLine;
import com.jangid.forging_process_management_service.entitiesRepresentation.forging.ForgingLineListRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.forging.ForgingLineRepresentation;
import com.jangid.forging_process_management_service.exception.TenantNotFoundException;
import com.jangid.forging_process_management_service.service.forging.ForgingLineService;
import com.jangid.forging_process_management_service.utils.ResourceUtils;

import io.swagger.annotations.ApiParam;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
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
public class ForgingLineResource {

  @Autowired
  private ForgingLineService forgingLineService;

  @GetMapping("tenant/{tenantId}/forgingLines")
  public ResponseEntity<ForgingLineListRepresentation> getAllForgingLinesOfTenant(@ApiParam(value = "Identifier of the tenant", required = true) @PathVariable String tenantId) {
    Long tId = ResourceUtils.convertIdToLong(tenantId)
        .orElseThrow(() -> new TenantNotFoundException(tenantId));

    List<ForgingLine> forgingLines = forgingLineService.getAllForgingLinesByTenant(tId);
    List<ForgingLineRepresentation> forgingLineRepresentations = forgingLines.stream().map(ForgingLineAssembler::dissemble).collect(Collectors.toList());
    ForgingLineListRepresentation forgingLineListRepresentation = ForgingLineListRepresentation.builder()
        .forgingLines(forgingLineRepresentations).build();
    return ResponseEntity.ok(forgingLineListRepresentation);
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
      Long tenantIdLongValue = ResourceUtils.convertIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Not valid id!"));
      ForgingLineRepresentation createdForgingLine = forgingLineService.createForgingLine(tenantIdLongValue, forgingLineRepresentation);
      return new ResponseEntity<>(createdForgingLine, HttpStatus.CREATED);
    } catch (Exception exception) {
      return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }
}

