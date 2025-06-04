package com.jangid.forging_process_management_service.resource.forging;

import com.jangid.forging_process_management_service.assemblers.forging.ForgeAssembler;
import com.jangid.forging_process_management_service.entities.forging.Forge;
import com.jangid.forging_process_management_service.entities.forging.ForgingLine;
import com.jangid.forging_process_management_service.entitiesRepresentation.error.ErrorResponse;
import com.jangid.forging_process_management_service.entitiesRepresentation.forging.ForgeRepresentation;
import com.jangid.forging_process_management_service.exception.forging.ForgeNotFoundException;
import com.jangid.forging_process_management_service.service.forging.ForgeService;
import com.jangid.forging_process_management_service.utils.GenericResourceUtils;
import com.jangid.forging_process_management_service.dto.ForgeTraceabilitySearchResultDTO;

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
      Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Not valid tenantId!"));
      Long forgingLineIdLongValue = GenericResourceUtils.convertResourceIdToLong(forgingLineId)
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
      Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Not valid tenantId!"));
      Long forgingLineIdLongValue = GenericResourceUtils.convertResourceIdToLong(forgingLineId)
          .orElseThrow(() -> new RuntimeException("Not valid forgingLineId!"));
      Long forgeIdLongValue = GenericResourceUtils.convertResourceIdToLong(forgeId)
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
      Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Not valid tenantId!"));
      Long forgingLineIdLongValue = GenericResourceUtils.convertResourceIdToLong(forgingLineId)
          .orElseThrow(() -> new RuntimeException("Not valid forgingLineId!"));
      Long forgeIdLongValue = GenericResourceUtils.convertResourceIdToLong(forgeId)
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
      Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Not valid tenantId!"));

      Long forgingLineIdLongValue = GenericResourceUtils.convertResourceIdToLong(forgingLineId)
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
  public ResponseEntity<Page<ForgeRepresentation>> getTenantForges(
      @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable("tenantId") String tenantId,
      @RequestParam(value = "page") String page,
      @RequestParam(value = "size") String size) {

    try {
      Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Not valid tenantId!"));

      int pageNumber = GenericResourceUtils.convertResourceIdToInt(page)
          .orElseThrow(() -> new RuntimeException("Invalid page="+page));

      int sizeNumber = GenericResourceUtils.convertResourceIdToInt(size)
          .orElseThrow(() -> new RuntimeException("Invalid size="+size));

      Page<ForgeRepresentation> forges = forgeService.getAllForges(tenantIdLongValue, pageNumber, sizeNumber);
      return ResponseEntity.ok(forges);
    } catch (Exception e) {
      if (e instanceof ForgeNotFoundException) {
        return ResponseEntity.ok().build();
      }
      throw e;
    }
  }

//  @DeleteMapping("tenant/{tenantId}/forgingLine/{forgingLineId}/forge/{forgeId}")
//  public ResponseEntity<Void> deleteForgeTraceability(@PathVariable("tenantId") String tenantId,
//                                                      @PathVariable("forgingLineId") String forgingLineId,
//                                                      @PathVariable("forgeId") String forgeId) {
//    if (tenantId == null || tenantId.isEmpty() || forgingLineId == null || forgingLineId.isEmpty() || forgeId == null || forgeId.isEmpty()) {
//      log.error("invalid input for delete forge!");
//      throw new RuntimeException("invalid input for delete forge!");
//    }
//    Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
//        .orElseThrow(() -> new RuntimeException("Not valid tenant id!"));
//
//    Long forgingLineIdLongValue = GenericResourceUtils.convertResourceIdToLong(forgingLineId)
//        .orElseThrow(() -> new RuntimeException("Not valid forgingLineId!"));
//
//    Long forgeIdLongValue = GenericResourceUtils.convertResourceIdToLong(forgeId)
//        .orElseThrow(() -> new RuntimeException("Not valid forgeId!"));
//
//    ForgingLine forgingLine = forgeService.getForgingLineUsingTenantIdAndForgingLineId(tenantIdLongValue, forgingLineIdLongValue);
//    Forge forge = forgeService.getForgeByIdAndForgingLineId(forgeIdLongValue, forgingLine.getId());
//
//    forgeService.deleteForge(tenantIdLongValue, forgeIdLongValue);
//    return ResponseEntity.noContent().build();
//  }

  @DeleteMapping("tenant/{tenantId}/forge/{forgeId}")
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<?> deleteForge(
      @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable String tenantId,
      @ApiParam(value = "Identifier of the forge", required = true) @PathVariable String forgeId) {

    try {
      Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Not valid tenantId!"));
      Long forgeIdLongValue = GenericResourceUtils.convertResourceIdToLong(forgeId)
          .orElseThrow(() -> new RuntimeException("Not valid forgeId!"));

      forgeService.deleteForge(tenantIdLongValue, forgeIdLongValue);
      return ResponseEntity.ok().build();

    } catch (Exception exception) {
      if (exception instanceof ForgeNotFoundException) {
        return ResponseEntity.notFound().build();
      }
      if (exception instanceof IllegalStateException) {
        log.error("Error while deleting forge: {}", exception.getMessage());
        return new ResponseEntity<>(new ErrorResponse(exception.getMessage()), HttpStatus.CONFLICT);
      }
      log.error("Error while deleting forge: {}", exception.getMessage());
      return new ResponseEntity<>(new ErrorResponse("Error while deleting forge"),
                                 HttpStatus.INTERNAL_SERVER_ERROR);
    }
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

  /**
   * Search for a forge and all its related entities by forge traceability number
   * @param forgeTraceabilityNumber The forge traceability number to search for
   * @return A DTO containing the forge and related entities information
   */
  @GetMapping(value = "tenant/{tenantId}/forge/search", produces = MediaType.APPLICATION_JSON)
  public ResponseEntity<ForgeTraceabilitySearchResultDTO> searchByForgeTraceabilityNumber(
      @RequestParam(value = "forgeTraceabilityNumber", required = true) String forgeTraceabilityNumber) {
    
    try {
      if (forgeTraceabilityNumber == null || forgeTraceabilityNumber.isEmpty()) {
        log.error("Invalid forgeTraceabilityNumber input!");
        throw new RuntimeException("Invalid forgeTraceabilityNumber input!");
      }
      
      ForgeTraceabilitySearchResultDTO searchResult = forgeService.searchByForgeTraceabilityNumber(forgeTraceabilityNumber);
      return ResponseEntity.ok(searchResult);
    } catch (Exception exception) {
      if (exception instanceof ForgeNotFoundException) {
        return ResponseEntity.notFound().build();
      }
      return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Search for forges by different criteria with pagination
   * @param tenantId The tenant ID
   * @param searchType The type of search (ITEM_NAME, FORGE_TRACEABILITY_NUMBER, or FORGING_LINE_NAME)
   * @param searchTerm The search term (substring matching for all search types)
   * @param pageParam The page number (0-based, defaults to 0)
   * @param sizeParam The page size (defaults to 10)
   * @return Page of ForgeRepresentation containing the search results
   */
  @GetMapping(value = "tenant/{tenantId}/searchForges", produces = MediaType.APPLICATION_JSON)
  public ResponseEntity<Page<ForgeRepresentation>> searchForges(
      @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable("tenantId") String tenantId,
      @ApiParam(value = "Type of search", required = true, allowableValues = "ITEM_NAME,FORGE_TRACEABILITY_NUMBER,FORGING_LINE_NAME") @RequestParam("searchType") String searchType,
      @ApiParam(value = "Search term (substring matching)", required = true) @RequestParam("searchTerm") String searchTerm,
      @ApiParam(value = "Page number (0-based)", required = false) @RequestParam(value = "page", defaultValue = "0") String pageParam,
      @ApiParam(value = "Page size", required = false) @RequestParam(value = "size", defaultValue = "10") String sizeParam) {

    try {
      Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Not valid tenantId!"));
      
      if (searchType == null || searchType.trim().isEmpty()) {
        return ResponseEntity.badRequest().build();
      }
      
      if (searchTerm == null || searchTerm.trim().isEmpty()) {
        return ResponseEntity.badRequest().build();
      }

      int pageNumber = GenericResourceUtils.convertResourceIdToInt(pageParam)
          .orElseThrow(() -> new RuntimeException("Invalid page=" + pageParam));

      int pageSize = GenericResourceUtils.convertResourceIdToInt(sizeParam)
          .orElseThrow(() -> new RuntimeException("Invalid size=" + sizeParam));

      if (pageNumber < 0) {
        pageNumber = 0;
      }

      if (pageSize <= 0) {
        pageSize = 10; // Default page size
      }

      Page<ForgeRepresentation> searchResults = forgeService.searchForges(tenantIdLongValue, searchType.trim(), searchTerm.trim(), pageNumber, pageSize);
      return ResponseEntity.ok(searchResults);

    } catch (IllegalArgumentException e) {
      log.error("Invalid search parameters: {}", e.getMessage());
      return ResponseEntity.badRequest().build();
    } catch (Exception e) {
      log.error("Error during forge search: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  private boolean isInvalidForgingDetails(ForgeRepresentation forgeRepresentation) {
    if (forgeRepresentation.getProcessedItem() == null ||
        forgeRepresentation.getForgingLine() == null ||
        forgeRepresentation.getForgeHeats() == null || forgeRepresentation.getForgeHeats().isEmpty() ||
        forgeRepresentation.getApplyAt() == null || forgeRepresentation.getApplyAt().isEmpty() ||
        forgeRepresentation.getForgeHeats().stream().anyMatch(forgeHeat -> forgeHeat.getHeatQuantityUsed() == null || forgeHeat.getHeatQuantityUsed().isEmpty()) ||
        forgeRepresentation.getForgeHeats().stream().anyMatch(forgeHeat -> forgeHeat.getHeat().getHeatNumber() == null || forgeHeat.getHeat().getHeatNumber().isEmpty())
    ) {
      log.error("invalid forging input!");
      return true;
    }
    
    // No need to validate itemWeightType if provided - all enum values are valid
    // The fromString method in the enum will handle conversion of invalid values
    
    return false;
  }
}
