package com.jangid.forging_process_management_service.resource.quality;

import com.jangid.forging_process_management_service.configuration.security.TenantContextHolder;
import com.jangid.forging_process_management_service.entitiesRepresentation.quality.GaugeListRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.quality.GaugeRepresentation;
import com.jangid.forging_process_management_service.service.quality.GaugeService;
import com.jangid.forging_process_management_service.utils.GenericResourceUtils;
import com.jangid.forging_process_management_service.utils.GenericExceptionHandler;

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

  @PostMapping("gauge")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<?> createGauge(@RequestBody GaugeRepresentation gaugeRepresentation) {
    try {
      if (isInvalidGaugeRepresentation(gaugeRepresentation)) {
        log.error("invalid createGauge input!");
        throw new RuntimeException("invalid createGauge input!");
      }
      Long tenantIdLongValue = TenantContextHolder.getAuthenticatedTenantId();
      GaugeRepresentation createdGauge = gaugeService.createGauge(tenantIdLongValue, gaugeRepresentation);
      return new ResponseEntity<>(createdGauge, HttpStatus.CREATED);
    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "createGauge");
    }
  }

  @GetMapping("gauges")
  public ResponseEntity<?> getAllGaugesOfTenant(
                                                  @RequestParam(value = "page", required = false) String page,
                                                  @RequestParam(value = "size", required = false) String size) {
    try {
      Long tId = TenantContextHolder.getAuthenticatedTenantId();

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


  @PostMapping("gauge/{gaugeId}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<?> updateGauge(
      @PathVariable("gaugeId") String gaugeId,
      @RequestBody GaugeRepresentation gaugeRepresentation) {
    try {
      if ( gaugeId == null || gaugeId.isEmpty() || isInvalidGaugeRepresentation(gaugeRepresentation)) {
        log.error("invalid input for updateGauge!");
        throw new RuntimeException("invalid input for updateGauge!");
      }
      Long tenantIdLongValue = TenantContextHolder.getAuthenticatedTenantId();

      Long gaugeIdLongValue = GenericResourceUtils.convertResourceIdToLong(gaugeId)
          .orElseThrow(() -> new RuntimeException("Not valid gaugeId!"));

      GaugeRepresentation updatedGauge = gaugeService.updateGauge(gaugeIdLongValue, tenantIdLongValue, gaugeRepresentation);
      return ResponseEntity.ok(updatedGauge);
    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "updateGauge");
    }
  }

  @DeleteMapping("gauge/{gaugeId}")
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<?> deleteGauge(
      @PathVariable("gaugeId") String gaugeId) {
    try {
      if ( gaugeId == null || gaugeId.isEmpty()) {
        log.error("invalid input for deleteGauge!");
        throw new RuntimeException("invalid input for deleteGauge!");
      }

      Long tenantIdLongValue = TenantContextHolder.getAuthenticatedTenantId();

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
