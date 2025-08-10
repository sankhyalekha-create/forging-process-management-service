package com.jangid.forging_process_management_service.resource.quality;

import com.jangid.forging_process_management_service.entitiesRepresentation.quality.GaugeListRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.quality.GaugeRepresentation;
import com.jangid.forging_process_management_service.exception.TenantNotFoundException;
import com.jangid.forging_process_management_service.service.quality.GaugeService;
import com.jangid.forging_process_management_service.utils.GenericResourceUtils;
import com.jangid.forging_process_management_service.utils.GenericExceptionHandler;

import io.swagger.annotations.ApiParam;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Component
public class GaugeResource {

  @Autowired
  private GaugeService gaugeService;

  @PostMapping("tenant/{tenantId}/gauge")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<?> createGauge(@PathVariable String tenantId, @RequestBody GaugeRepresentation gaugeRepresentation) {
    try {
      if (tenantId == null || tenantId.isEmpty() || isInvalidGaugeRepresentation(gaugeRepresentation)) {
        log.error("invalid createGauge input!");
        throw new RuntimeException("invalid createGauge input!");
      }
      Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Not valid tenantId!"));
      GaugeRepresentation createdGauge = gaugeService.createGauge(tenantIdLongValue, gaugeRepresentation);
      return new ResponseEntity<>(createdGauge, HttpStatus.CREATED);
    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "createGauge");
    }
  }

  @GetMapping("tenant/{tenantId}/gauges")
  public ResponseEntity<?> getAllGaugesOfTenant(@ApiParam(value = "Identifier of the tenant", required = true) @PathVariable String tenantId,
                                                  @RequestParam(value = "page", required = false) String page,
                                                  @RequestParam(value = "size", required = false) String size) {
    try {
      Long tId = GenericResourceUtils.convertResourceIdToLong(tenantId)
          .orElseThrow(() -> new TenantNotFoundException(tenantId));

      Integer pageNumber = (page == null || page.isBlank()) ? -1
                                                            : GenericResourceUtils.convertResourceIdToInt(page)
                               .orElseThrow(() -> new RuntimeException("Invalid page=" + page));

      Integer sizeNumber = (size == null || size.isBlank()) ? -1
                                                            : GenericResourceUtils.convertResourceIdToInt(size)
                               .orElseThrow(() -> new RuntimeException("Invalid size=" + size));

      if (pageNumber == -1 || sizeNumber == -1) {
        GaugeListRepresentation gaugeListRepresentation = gaugeService.getAllGaugesOfTenantWithoutPagination(tId);
        return ResponseEntity.ok(gaugeListRepresentation);
      }
      Page<GaugeRepresentation> gauges = gaugeService.getAllGaugesOfTenant(tId, pageNumber, sizeNumber);
      return ResponseEntity.ok(gauges);
    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "getAllGaugesOfTenant");
    }
  }


  @PostMapping("tenant/{tenantId}/gauge/{gaugeId}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<?> updateGauge(
      @PathVariable("tenantId") String tenantId, @PathVariable("gaugeId") String gaugeId,
      @RequestBody GaugeRepresentation gaugeRepresentation) {
    try {
      if (tenantId == null || tenantId.isEmpty() || gaugeId == null || gaugeId.isEmpty() || isInvalidGaugeRepresentation(gaugeRepresentation)) {
        log.error("invalid input for updateGauge!");
        throw new RuntimeException("invalid input for updateGauge!");
      }
      Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Not valid tenant id!"));

      Long gaugeIdLongValue = GenericResourceUtils.convertResourceIdToLong(gaugeId)
          .orElseThrow(() -> new RuntimeException("Not valid gaugeId!"));

      GaugeRepresentation updatedGauge = gaugeService.updateGauge(gaugeIdLongValue, tenantIdLongValue, gaugeRepresentation);
      return ResponseEntity.ok(updatedGauge);
    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "updateGauge");
    }
  }

  @DeleteMapping("tenant/{tenantId}/gauge/{gaugeId}")
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<?> deleteGauge(
      @PathVariable("tenantId") String tenantId,
      @PathVariable("gaugeId") String gaugeId) {
    try {
      if (tenantId == null || tenantId.isEmpty() || gaugeId == null || gaugeId.isEmpty()) {
        log.error("invalid input for deleteGauge!");
        throw new RuntimeException("invalid input for deleteGauge!");
      }

      Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Not valid tenant id!"));

      Long gaugeIdLongValue = GenericResourceUtils.convertResourceIdToLong(gaugeId)
          .orElseThrow(() -> new RuntimeException("Not valid gaugeId!"));

      gaugeService.deleteGauge(gaugeIdLongValue, tenantIdLongValue);
      return ResponseEntity.ok().build();
    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "deleteGauge");
    }
  }

  private boolean isInvalidGaugeRepresentation(GaugeRepresentation gaugeRepresentation) {
    if (gaugeRepresentation == null ||
        gaugeRepresentation.getGaugeName() == null || gaugeRepresentation.getGaugeName().isEmpty()) {
      return true;
    }
    return false;
  }
}
