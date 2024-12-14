package com.jangid.forging_process_management_service.resource.forging;

import com.jangid.forging_process_management_service.assemblers.forging.ForgeAssembler;
import com.jangid.forging_process_management_service.entities.forging.Forge;
import com.jangid.forging_process_management_service.entities.forging.ForgingLine;
import com.jangid.forging_process_management_service.entitiesRepresentation.forging.ForgeRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.forging.ForgeListRepresentation;
import com.jangid.forging_process_management_service.exception.forging.ForgeNotFoundException;
import com.jangid.forging_process_management_service.service.forging.ForgeService;
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

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Component
public class ForgeResource {

  @Autowired
  private final ForgeService forgeService;

  @Autowired
  private final ForgeAssembler forgeAssembler;

  @PostMapping("tenant/{tenantId}/forgingLine/{forgingLineId}/forge")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<ForgeRepresentation> applyForge(@PathVariable String tenantId, @PathVariable String forgingLineId, @RequestBody ForgeRepresentation forgeRepresentation) {
    try {
      if (forgingLineId == null || forgingLineId.isEmpty() || tenantId == null || tenantId.isEmpty() || isInvalidForgingDetails(forgeRepresentation)) {
        log.error("invalid applyForge input!");
        throw new RuntimeException("invalid applyForge input!");
      }
      Long tenantIdLongValue = ResourceUtils.convertIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Not valid tenantId!"));
      Long forgingLineIdLongValue = ResourceUtils.convertIdToLong(forgingLineId)
          .orElseThrow(() -> new RuntimeException("Not valid forgingLineId!"));
      ForgeRepresentation createdForgeTraceability = forgeService.applyForge(tenantIdLongValue, forgingLineIdLongValue, forgeRepresentation);
      return new ResponseEntity<>(createdForgeTraceability, HttpStatus.CREATED);
    } catch (Exception exception) {
      if (exception instanceof ForgeNotFoundException) {
        return ResponseEntity.notFound().build();
      }
      return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @PostMapping("tenant/{tenantId}/forgingLine/{forgingLineId}/forge/{forgeId}/startForge")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<ForgeRepresentation> startForge(@PathVariable String tenantId, @PathVariable String forgingLineId, @PathVariable String forgeId,
                                                        @RequestBody ForgeRepresentation forgeRepresentation) {
    try {
      if (forgingLineId == null || forgingLineId.isEmpty() || tenantId == null || tenantId.isEmpty() || forgeId == null || forgeId.isEmpty() || forgeRepresentation.getStartAt() == null
          || forgeRepresentation.getStartAt().isEmpty()) {
        log.error("invalid startForge input!");
        throw new RuntimeException("invalid startForge input!");
      }
      Long tenantIdLongValue = ResourceUtils.convertIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Not valid tenantId!"));
      Long forgingLineIdLongValue = ResourceUtils.convertIdToLong(forgingLineId)
          .orElseThrow(() -> new RuntimeException("Not valid forgingLineId!"));
      Long forgeIdLongValue = ResourceUtils.convertIdToLong(forgeId)
          .orElseThrow(() -> new RuntimeException("Not valid forgeId!"));

      ForgeRepresentation updatedForge = forgeService.startForge(tenantIdLongValue, forgingLineIdLongValue, forgeIdLongValue, forgeRepresentation.getStartAt());
      return new ResponseEntity<>(updatedForge, HttpStatus.ACCEPTED);
    } catch (Exception exception) {
      if (exception instanceof ForgeNotFoundException) {
        return ResponseEntity.notFound().build();
      }
      return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @PostMapping("tenant/{tenantId}/forgingLine/{forgingLineId}/forge/{forgeId}/endForge")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<ForgeRepresentation> endForge(@PathVariable String tenantId, @PathVariable String forgingLineId, @PathVariable String forgeId,
                                                      @RequestBody ForgeRepresentation forgeRepresentation) {
    try {
      if (forgingLineId == null || forgingLineId.isEmpty() || tenantId == null || tenantId.isEmpty() || forgeId == null || forgeId.isEmpty() || forgeRepresentation.getEndAt() == null
          || forgeRepresentation.getEndAt().isEmpty() || forgeRepresentation.getActualForgeCount() == null || forgeRepresentation.getActualForgeCount().isEmpty()) {
        log.error("invalid endForge input!");
        throw new RuntimeException("invalid endForge input!");
      }
      Long tenantIdLongValue = ResourceUtils.convertIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Not valid tenantId!"));
      Long forgingLineIdLongValue = ResourceUtils.convertIdToLong(forgingLineId)
          .orElseThrow(() -> new RuntimeException("Not valid forgingLineId!"));
      Long forgeIdLongValue = ResourceUtils.convertIdToLong(forgeId)
          .orElseThrow(() -> new RuntimeException("Not valid forgeId!"));

      ForgeRepresentation updatedForge = forgeService.endForge(tenantIdLongValue, forgingLineIdLongValue, forgeIdLongValue, forgeRepresentation);
      return new ResponseEntity<>(updatedForge, HttpStatus.ACCEPTED);
    } catch (Exception exception) {
      if (exception instanceof ForgeNotFoundException) {
        return ResponseEntity.notFound().build();
      }
      return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }


  //  @PostMapping("tenant/{tenantId}/forgingLine/{forgingLineId}/forgeTraceability/{forgeTraceabilityId}")
//  @Consumes(MediaType.APPLICATION_JSON)
//  @Produces(MediaType.APPLICATION_JSON)
//  public ResponseEntity<ForgeTraceabilityRepresentation> updateForgeTraceability(@PathVariable String tenantId, @PathVariable String forgingLineId, @PathVariable String forgeTraceabilityId, @RequestBody ForgeTraceabilityRepresentation forgeTraceabilityRepresentation) {
//    try {
//      if (forgingLineId == null || forgingLineId.isEmpty() ||
//          forgeTraceabilityId == null || forgeTraceabilityId.isEmpty() ||
//          forgeTraceabilityRepresentation.getForgePieceWeight() == null || forgeTraceabilityRepresentation.getHeatIdQuantityUsed() == null) {
//        log.error("invalid input!");
//        throw new RuntimeException("invalid input!");
//      }
//      Long tenantIdLongValue = ResourceUtils.convertIdToLong(tenantId)
//          .orElseThrow(() -> new RuntimeException("Not valid tenantId!"));
//      Long forgingLineIdLongValue = ResourceUtils.convertIdToLong(forgingLineId)
//          .orElseThrow(() -> new RuntimeException("Not valid forgingLineId!"));
//      Long forgeTraceabilityIdLongValue = ResourceUtils.convertIdToLong(forgeTraceabilityId)
//          .orElseThrow(() -> new RuntimeException("Not valid forgeTraceabilityId!"));
//
//      ForgeTraceabilityRepresentation updatedForgeTraceability = forgeService.updateForgeTraceability(tenantIdLongValue, forgingLineIdLongValue, forgeTraceabilityIdLongValue, forgeTraceabilityRepresentation);
//      return ResponseEntity.ok(updatedForgeTraceability);
//    } catch (Exception exception) {
//      if (exception instanceof ForgeTraceabilityNotFoundException){
//        return ResponseEntity.notFound().build();
//      }
//      return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
//    }
//  }
//
  @GetMapping(value = "tenant/{tenantId}/forgingLine/{forgingLineId}/forge", produces = MediaType.APPLICATION_JSON)
  public ResponseEntity<ForgeRepresentation> getForgeOfForgingLine(
      @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable("tenantId") String tenantId,
      @ApiParam(value = "Identifier of the forgingLine", required = true) @PathVariable("forgingLineId") String forgingLineId) {

    try {
      Long tenantIdLongValue = ResourceUtils.convertIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Not valid tenantId!"));

      Long forgingLineIdLongValue = ResourceUtils.convertIdToLong(forgingLineId)
          .orElseThrow(() -> new RuntimeException("Not valid forgingLineId!"));

      ForgingLine forgingLine = forgeService.getForgingLineUsingTenantIdAndForgingLineId(tenantIdLongValue, forgingLineIdLongValue);
      Forge forge = forgeService.getAppliedForgeByForgingLine(forgingLine.getId());
      ForgeRepresentation representation = forgeAssembler.dissemble(forge);
      return ResponseEntity.ok(representation);
    } catch (Exception e) {
      if (e instanceof ForgeNotFoundException) {
        return ResponseEntity.ok().build();
      }
      throw e;
    }
  }

  @GetMapping(value = "tenant/{tenantId}/forges", produces = MediaType.APPLICATION_JSON)
  public ResponseEntity<ForgeListRepresentation> getTenantForges(
      @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable("tenantId") String tenantId) {

    try {
      Long tenantIdLongValue = ResourceUtils.convertIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Not valid tenantId!"));

      List<Forge> forges = forgeService.getAllForges(tenantIdLongValue);
      ForgeListRepresentation forgeListRepresentation = ForgeListRepresentation.builder()
          .forges(forges.stream().map(forgeAssembler::dissemble).collect(Collectors.toList())).build();
      return ResponseEntity.ok(forgeListRepresentation);
    } catch (Exception e) {
      if (e instanceof ForgeNotFoundException) {
        return ResponseEntity.ok().build();
      }
      throw e;
    }
  }

  @DeleteMapping("tenant/{tenantId}/forgingLine/{forgingLineId}/forge/{forgeId}")
  public ResponseEntity<Void> deleteForgeTraceability(@PathVariable("tenantId") String tenantId,
                                                      @PathVariable("forgingLineId") String forgingLineId,
                                                      @PathVariable("forgeId") String forgeId) {
    if (tenantId == null || tenantId.isEmpty() || forgingLineId == null || forgingLineId.isEmpty() || forgeId == null || forgeId.isEmpty()) {
      log.error("invalid input for delete forge!");
      throw new RuntimeException("invalid input for delete forge!");
    }
    Long tenantIdLongValue = ResourceUtils.convertIdToLong(tenantId)
        .orElseThrow(() -> new RuntimeException("Not valid tenant id!"));

    Long forgingLineIdLongValue = ResourceUtils.convertIdToLong(forgingLineId)
        .orElseThrow(() -> new RuntimeException("Not valid forgingLineId!"));

    Long forgeIdLongValue = ResourceUtils.convertIdToLong(forgeId)
        .orElseThrow(() -> new RuntimeException("Not valid forgeId!"));

    ForgingLine forgingLine = forgeService.getForgingLineUsingTenantIdAndForgingLineId(tenantIdLongValue, forgingLineIdLongValue);
    Forge forge = forgeService.getForgeByIdAndForgingLineId(forgeIdLongValue, forgingLine.getId());

    forgeService.deleteForge(forge);
    return ResponseEntity.noContent().build();
  }
//
//  @PostMapping("tenant/{tenantId}/forge-traceability/filter")
//  @Consumes(MediaType.APPLICATION_JSON)
//  @Produces(MediaType.APPLICATION_JSON)
//  public ResponseEntity<ForgeTraceabilityListRepresentation> fetchFilteredForgeTraceability(
//      @PathVariable("tenantId") String tenantId,
//      @RequestBody Map<String, Object> filters) {
//    try {
//      Long tenantIdLongValue = ResourceUtils.convertIdToLong(tenantId)
//          .orElseThrow(() -> new RuntimeException("Not valid tenantId!"));
//
//      // Extract filters from the request body
//      List<Long> forgingLines = (List<Long>) filters.get("forgingLines");
//      String heatNumber = (String) filters.get("heatNumber");
//      String forgingLineName = (String) filters.get("forgingLineName");
//      String forgingStatus = (String) filters.get("forgingStatus");
//      String startDate = (String) filters.get("startDate");
//      String endDate = (String) filters.get("endDate");
//
//      // Call service method with filters
//      List<ForgeTraceability> filteredTraceability = new ArrayList<>();
////          forgeTraceabilityService.fetchFilteredForgeTraceability(tenantIdLongValue, forgingLines, heatNumber, forgingLineName, forgingStatus, startDate, endDate);
//
//      // Convert to representation
//      List<ForgeTraceabilityRepresentation> representations = filteredTraceability.stream()
//          .map(forgeAssembler::dissemble)
//          .collect(Collectors.toList());
//
//      return ResponseEntity.ok(ForgeTraceabilityListRepresentation.builder().forgeTraceabilities(representations).build());
//    } catch (Exception exception) {
//      log.error("Error fetching filtered forge traceability: ", exception);
//      return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
//    }
//  }

  private boolean isInvalidForgingDetails(ForgeRepresentation forgeRepresentation) {
    if (forgeRepresentation.getProcessedItem() == null ||
        forgeRepresentation.getForgingLine() == null ||
        forgeRepresentation.getForgeHeats() == null || forgeRepresentation.getForgeHeats().isEmpty() ||
        forgeRepresentation.getForgeHeats().stream().anyMatch(forgeHeat -> forgeHeat.getHeatQuantityUsed() == null || forgeHeat.getHeatQuantityUsed().isEmpty())) {
      log.error("invalid forging input!");
      return true;
    }
    return false;
  }
}
