package com.jangid.forging_process_management_service.resource.machining;

import com.jangid.forging_process_management_service.assemblers.machining.MachiningBatchAssembler;
import com.jangid.forging_process_management_service.entities.machining.MachineSet;
import com.jangid.forging_process_management_service.entities.machining.MachiningBatch;
import com.jangid.forging_process_management_service.entitiesRepresentation.error.ErrorResponse;
import com.jangid.forging_process_management_service.entitiesRepresentation.machining.DailyMachiningBatchRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.machining.MachiningBatchRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.machining.MachiningBatchListRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.machining.MachiningBatchStatisticsRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.machining.MonthlyMachiningStatisticsRepresentation;
import com.jangid.forging_process_management_service.exception.TenantNotFoundException;
import com.jangid.forging_process_management_service.exception.forging.ForgeNotFoundException;
import com.jangid.forging_process_management_service.exception.machining.MachiningBatchNotFoundException;
import com.jangid.forging_process_management_service.service.machining.DailyMachiningBatchService;
import com.jangid.forging_process_management_service.service.machining.MachiningBatchService;
import com.jangid.forging_process_management_service.utils.GenericResourceUtils;
import com.jangid.forging_process_management_service.dto.MachiningBatchAssociationsDTO;

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

import java.util.Collection;
import java.util.List;
import java.util.Arrays;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Component
public class MachiningBatchResource {

  @Autowired
  private final MachiningBatchService machiningBatchService;

  @Autowired
  private final DailyMachiningBatchService dailyMachiningBatchService;

  @Autowired
  private final MachiningBatchAssembler machiningBatchAssembler;

  @PostMapping("tenant/{tenantId}/create-machining-batch")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<?> createMachiningBatch(
      @PathVariable String tenantId,
      @RequestBody MachiningBatchRepresentation machiningBatchRepresentation) {
    try {
      if (tenantId == null || tenantId.isEmpty() ||
          isInvalidMachiningBatchDetailsForCreating(machiningBatchRepresentation)) {
        log.error("Invalid createMachiningBatch input!");
        throw new RuntimeException("Invalid createMachiningBatch input!");
      }

      Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Not valid tenantId!"));

      MachiningBatchRepresentation createdMachiningBatch = machiningBatchService.createMachiningBatch(
          tenantIdLongValue, machiningBatchRepresentation);

      return new ResponseEntity<>(createdMachiningBatch, HttpStatus.CREATED);
    } catch (Exception exception) {
      if (exception instanceof MachiningBatchNotFoundException) {
        return ResponseEntity.notFound().build();
      }

      if (exception instanceof IllegalStateException) {
        log.error("Machining Batch exists with the given machining batch number: {}", machiningBatchRepresentation.getMachiningBatchNumber());
        return new ResponseEntity<>(new ErrorResponse("Machining Batch exists with the given machining batch number"), HttpStatus.CONFLICT);
      }
      return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @PostMapping("tenant/{tenantId}/machine-set/{machineSetId}/apply-machining-batch")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<?> applyMachiningBatch(
      @PathVariable String tenantId,
      @PathVariable String machineSetId,
      @RequestBody MachiningBatchRepresentation machiningBatchRepresentation,
      @RequestParam(required = false, defaultValue = "false") boolean rework) {
    try {
      if (machineSetId == null || machineSetId.isEmpty() ||
          tenantId == null || tenantId.isEmpty() ||
          isInvalidMachiningBatchDetailsForApplying(machiningBatchRepresentation, rework)) {
        log.error("Invalid applyMachiningBatch input!");
        throw new RuntimeException("Invalid applyMachiningBatch input!");
      }

      Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Not valid tenantId!"));
      Long machineSetIdLongValue = GenericResourceUtils.convertResourceIdToLong(machineSetId)
          .orElseThrow(() -> new RuntimeException("Not valid machineSetId!"));

      log.info("Rework flag: {} for machine-set: {}", rework, machineSetId);

      // Pass the rework flag to the service layer for further handling
      MachiningBatchRepresentation createdMachiningBatch = machiningBatchService.applyMachiningBatch(
          tenantIdLongValue, machineSetIdLongValue, machiningBatchRepresentation, rework);

      return new ResponseEntity<>(createdMachiningBatch, HttpStatus.CREATED);
    } catch (Exception exception) {
      if (exception instanceof MachiningBatchNotFoundException) {
        return ResponseEntity.notFound().build();
      }

      if (exception instanceof IllegalStateException) {
        log.error("Machining Batch exists with the given machining batch number: {}", machiningBatchRepresentation.getMachiningBatchNumber());
        return new ResponseEntity<>(new ErrorResponse("Machining Batch exists with the given machining batch number"), HttpStatus.CONFLICT);
      }
      return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @PostMapping("tenant/{tenantId}/machine-set/{machineSetId}/machining-batch/{machiningBatchId}/start-matchining-batch")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<MachiningBatchRepresentation> startMachiningBatch(@PathVariable String tenantId, @PathVariable String machineSetId, @PathVariable String machiningBatchId,
                                                                          @RequestBody MachiningBatchRepresentation machiningBatchRepresentation,
                                                                          @RequestParam(required = false, defaultValue = "false") boolean rework) {
    try {
      if (machineSetId == null || machineSetId.isEmpty() || tenantId == null || tenantId.isEmpty() || machiningBatchId == null || machiningBatchId.isEmpty()
          || machiningBatchRepresentation.getStartAt() == null
          || machiningBatchRepresentation.getStartAt().isEmpty()) {
        log.error("invalid startMachiningBatch input!");
        throw new RuntimeException("invalid startMachiningBatch input!");
      }
      Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Not valid tenantId!"));
      Long machineSetIdLongValue = GenericResourceUtils.convertResourceIdToLong(machineSetId)
          .orElseThrow(() -> new RuntimeException("Not valid machineSetId!"));
      Long machiningBatchIdLongValue = GenericResourceUtils.convertResourceIdToLong(machiningBatchId)
          .orElseThrow(() -> new RuntimeException("Not valid machiningBatchId!"));

      MachiningBatchRepresentation startedMachiningBatch = machiningBatchService.startMachiningBatch(tenantIdLongValue, machineSetIdLongValue, machiningBatchIdLongValue,
                                                                                                     machiningBatchRepresentation.getStartAt(), rework);
      return new ResponseEntity<>(startedMachiningBatch, HttpStatus.ACCEPTED);
    } catch (Exception exception) {
      if (exception instanceof MachiningBatchNotFoundException) {
        return ResponseEntity.notFound().build();
      }
      return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @PostMapping("tenant/{tenantId}/machining-batch/{machiningBatchId}/end-matchining-batch")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<MachiningBatchRepresentation> endMachiningBatch(@PathVariable String tenantId, @PathVariable String machiningBatchId,
                                                                        @RequestBody MachiningBatchRepresentation machiningBatchRepresentation,
                                                                        @RequestParam(required = false, defaultValue = "false") boolean rework) {
    try {
      if (tenantId == null || tenantId.isEmpty() || machiningBatchId == null || machiningBatchId.isEmpty() || isInvalidMachiningBatchDetailsForEnding(
          machiningBatchRepresentation)) {
        log.error("invalid endMachiningBatch input!");
        throw new RuntimeException("invalid endMachiningBatch input!");
      }
      Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Not valid tenantId!"));
      Long machiningBatchIdLongValue = GenericResourceUtils.convertResourceIdToLong(machiningBatchId)
          .orElseThrow(() -> new RuntimeException("Not valid machiningBatchId!"));

      MachiningBatchRepresentation endedMachiningBatch = machiningBatchService.endMachiningBatch(tenantIdLongValue, machiningBatchIdLongValue, machiningBatchRepresentation,
                                                                                                 rework);
      return new ResponseEntity<>(endedMachiningBatch, HttpStatus.ACCEPTED);
    } catch (Exception exception) {
      if (exception instanceof MachiningBatchNotFoundException) {
        return ResponseEntity.notFound().build();
      }
      return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @PostMapping("tenant/{tenantId}/machining-batch/{machiningBatchId}/daily-matchining-batch-update")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<?> dailyMachiningBatchUpdate(@PathVariable String tenantId, @PathVariable String machiningBatchId,
                                                     @RequestBody DailyMachiningBatchRepresentation dailyMachiningBatchRepresentation) {
    try {
      if (tenantId == null || tenantId.isEmpty() || machiningBatchId == null || machiningBatchId.isEmpty()
          || isInvalidDailyMachiningBatchRepresentation(dailyMachiningBatchRepresentation)) {
        log.error("invalid dailyMachiningBatchUpdate input!");
        throw new RuntimeException("invalid dailyMachiningBatchUpdate input!");
      }
      Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Not valid tenantId!"));
      Long machiningBatchIdLongValue = GenericResourceUtils.convertResourceIdToLong(machiningBatchId)
          .orElseThrow(() -> new RuntimeException("Not valid machiningBatchId!"));

      MachiningBatchRepresentation dailyMachiningBatchUpdate = machiningBatchService.dailyMachiningBatchUpdate(tenantIdLongValue, machiningBatchIdLongValue,
                                                                                                               dailyMachiningBatchRepresentation);
      return new ResponseEntity<>(dailyMachiningBatchUpdate, HttpStatus.ACCEPTED);
    } catch (Exception exception) {
      if (exception instanceof MachiningBatchNotFoundException) {
        return ResponseEntity.notFound().build();
      }

      if (exception instanceof IllegalStateException) {
        if (exception.getMessage().contains("Machining Shift with batch number")) {
          log.error("Machining Shift with the given daily machining batch number already exists: {}",
                    dailyMachiningBatchRepresentation.getDailyMachiningBatchNumber());
          return new ResponseEntity<>(new ErrorResponse("Machining Shift with the given batch number already exists"),
                                     HttpStatus.CONFLICT);
        }
      }

      return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }


  @GetMapping(value = "tenant/{tenantId}/machine-set/{machineSetId}/machining-batch", produces = MediaType.APPLICATION_JSON)
  public ResponseEntity<MachiningBatchRepresentation> getMachiningBatchOfMachineSet(
      @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable("tenantId") String tenantId,
      @ApiParam(value = "Identifier of the machineSet", required = true) @PathVariable("machineSetId") String machineSetId) {

    try {
      Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Not valid tenantId!"));

      Long machineSetIdLongValue = GenericResourceUtils.convertResourceIdToLong(machineSetId)
          .orElseThrow(() -> new RuntimeException("Not valid machineSetId!"));

      MachineSet machineSet = machiningBatchService.getMachineSetUsingTenantIdAndMachineSetId(tenantIdLongValue, machineSetIdLongValue);
      MachiningBatch machiningBatch = machiningBatchService.getAppliedMachiningBatchByMachineSet(machineSet.getId());
      if (machiningBatch.getId() == null) {
        return ResponseEntity.ok(MachiningBatchRepresentation.builder().build());
      }
      MachiningBatchRepresentation representation = machiningBatchAssembler.dissemble(machiningBatch);
      return ResponseEntity.ok(representation);
    } catch (Exception e) {
      if (e instanceof ForgeNotFoundException) {
        return ResponseEntity.ok().build();
      }
      throw e;
    }
  }

  @GetMapping("tenant/{tenantId}/machining-batches")
  public ResponseEntity<Page<MachiningBatchRepresentation>> getAllMachiningBatchByTenantId(
      @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable String tenantId,
      @RequestParam(value = "page", required = false) String page,
      @RequestParam(value = "size", required = false) String size) {
    Long tId = GenericResourceUtils.convertResourceIdToLong(tenantId)
        .orElseThrow(() -> new TenantNotFoundException(tenantId));

    int pageNumber = GenericResourceUtils.convertResourceIdToInt(page)
        .orElseThrow(() -> new RuntimeException("Invalid page=" + page));

    int sizeNumber = GenericResourceUtils.convertResourceIdToInt(size)
        .orElseThrow(() -> new RuntimeException("Invalid size=" + size));

    Page<MachiningBatchRepresentation> batches = machiningBatchService.getAllMachiningBatchByTenantId(tId, pageNumber, sizeNumber);
    return ResponseEntity.ok(batches);
  }

  @DeleteMapping("tenant/{tenantId}/machining-batch/{machiningBatchId}")
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<?> deleteMachiningBatch(
      @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable String tenantId,
      @ApiParam(value = "Identifier of the machining batch", required = true) @PathVariable String machiningBatchId) {

    try {
      Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Not valid tenantId!"));
      Long machiningBatchIdLongValue = GenericResourceUtils.convertResourceIdToLong(machiningBatchId)
          .orElseThrow(() -> new RuntimeException("Not valid machiningBatchId!"));

      machiningBatchService.deleteMachiningBatch(tenantIdLongValue, machiningBatchIdLongValue);
      return ResponseEntity.ok().build();

    } catch (Exception exception) {
      if (exception instanceof MachiningBatchNotFoundException) {
        return ResponseEntity.notFound().build();
      }
      if (exception instanceof IllegalStateException) {
        if (exception.getMessage().contains("not in COMPLETED status")) {
          log.error("This machining batch cannot be deleted as it is not in the COMPLETED status.");
          return new ResponseEntity<>(new ErrorResponse("This machining batch cannot be deleted as it is not in the COMPLETED status."), HttpStatus.CONFLICT);
        }
        if (exception.getMessage().contains("There exists inspection batch entry for the machiningBatch")) {
          log.error("This machining batch cannot be deleted because an inspection batch entry exists for it.");
          return new ResponseEntity<>(new ErrorResponse("This machining batch cannot be deleted as an inspection batch entry exists for it."), HttpStatus.CONFLICT);
        }
      }
      log.error("Error while deleting machining batch: {}", exception.getMessage());
      return new ResponseEntity<>(new ErrorResponse("Error while deleting machining batch"),
                                  HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @GetMapping("tenant/{tenantId}/machining-batch-statistics")
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<MachiningBatchStatisticsRepresentation> getMachiningBatchStatistics(
      @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable String tenantId) {
    try {
      Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Not valid tenantId!"));

      MachiningBatchStatisticsRepresentation statistics = machiningBatchService.getMachiningBatchStatistics(tenantIdLongValue);
      return ResponseEntity.ok(statistics);
    } catch (Exception exception) {
      log.error("Error while fetching machining batch statistics: {}", exception.getMessage());
      return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }


  /**
   * Get all associated inspection batches and dispatch batches for a specific machining batch in a single API call
   *
   * @param tenantId The tenant ID
   * @param machiningBatchId The machining batch ID
   * @return Combined DTO containing machining batch details, inspection batches, and dispatch batches
   */
  @GetMapping(value = "tenant/{tenantId}/machining-batch/{machiningBatchId}/associations", produces = MediaType.APPLICATION_JSON)
  public ResponseEntity<MachiningBatchAssociationsDTO> getMachiningBatchAssociations(
      @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable("tenantId") String tenantId,
      @ApiParam(value = "Identifier of the machining batch", required = true) @PathVariable("machiningBatchId") String machiningBatchId) {
    
    try {
      Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Not valid tenantId!"));
          
      Long machiningBatchIdLongValue = GenericResourceUtils.convertResourceIdToLong(machiningBatchId)
          .orElseThrow(() -> new RuntimeException("Not valid machiningBatchId!"));
      
      // Get associations using the service method
      MachiningBatchAssociationsDTO associationsDTO = 
          machiningBatchService.getMachiningBatchAssociations(machiningBatchIdLongValue, tenantIdLongValue);
      
      return ResponseEntity.ok(associationsDTO);
    } catch (Exception e) {
      log.error("Error getting associations for machining batch: {}", e.getMessage());
      return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Get monthly statistics of machining batches for a specific date range
   *
   * @param tenantId The tenant ID
   * @param fromMonth The starting month (1-12)
   * @param fromYear The starting year
   * @param toMonth The ending month (1-12)
   * @param toYear The ending year
   * @return Monthly statistics for machining batches in the given date range
   */
  @GetMapping("tenant/{tenantId}/machiningBatch/monthlyStatistics")
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<MonthlyMachiningStatisticsRepresentation> getMachiningBatchMonthlyStatistics(
      @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable String tenantId,
      @RequestParam(value = "fromMonth", required = true) int fromMonth,
      @RequestParam(value = "fromYear", required = true) int fromYear,
      @RequestParam(value = "toMonth", required = true) int toMonth,
      @RequestParam(value = "toYear", required = true) int toYear) {
    
    try {
      // Validate input parameters
      if (fromMonth < 1 || fromMonth > 12 || toMonth < 1 || toMonth > 12) {
        log.error("Invalid month values: fromMonth={}, toMonth={}", fromMonth, toMonth);
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
      }
      
      if (fromYear > toYear || (fromYear == toYear && fromMonth > toMonth)) {
        log.error("Invalid date range: fromMonth={}, fromYear={}, toMonth={}, toYear={}", 
                 fromMonth, fromYear, toMonth, toYear);
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
      }
      
      Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Not valid tenantId!"));
      
      MonthlyMachiningStatisticsRepresentation statistics = 
          machiningBatchService.getMachiningBatchMonthlyStatistics(tenantIdLongValue, fromMonth, fromYear, toMonth, toYear);
      
      return ResponseEntity.ok(statistics);
    } catch (Exception exception) {
      log.error("Error while fetching monthly machining batch statistics: {}", exception.getMessage());
      return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @GetMapping(value = "tenant/{tenantId}/searchMachiningBatches", produces = MediaType.APPLICATION_JSON)
  public ResponseEntity<Page<MachiningBatchRepresentation>> searchMachiningBatches(
      @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable("tenantId") String tenantId,
      @ApiParam(value = "Type of search", required = true, allowableValues = "ITEM_NAME,FORGE_TRACEABILITY_NUMBER,MACHINING_BATCH_NUMBER") @RequestParam("searchType") String searchType,
      @ApiParam(value = "Search term", required = true) @RequestParam("searchTerm") String searchTerm,
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

      Page<MachiningBatchRepresentation> searchResults = machiningBatchService.searchMachiningBatches(tenantIdLongValue, searchType.trim(), searchTerm.trim(), pageNumber, pageSize);
      return ResponseEntity.ok(searchResults);

    } catch (IllegalArgumentException e) {
      log.error("Invalid search parameters: {}", e.getMessage());
      return ResponseEntity.badRequest().build();
    } catch (Exception e) {
      log.error("Error during machining batch search: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  @GetMapping(value = "tenant/{tenantId}/processedItemMachiningBatches/machiningBatches", produces = MediaType.APPLICATION_JSON)
  public ResponseEntity<?> getMachiningBatchesByProcessedItemMachiningBatchIds(
      @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable("tenantId") String tenantId,
      @ApiParam(value = "Comma-separated list of processed item machining batch IDs", required = true) @RequestParam("processedItemMachiningBatchIds") String processedItemMachiningBatchIds) {

    try {
      if (tenantId == null || tenantId.isEmpty() || processedItemMachiningBatchIds == null || processedItemMachiningBatchIds.isEmpty()) {
        log.error("Invalid input for getMachiningBatchesByProcessedItemMachiningBatchIds - tenantId or processedItemMachiningBatchIds is null/empty");
        throw new IllegalArgumentException("Tenant ID and Processed Item Machining Batch IDs are required and cannot be empty");
      }

      Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Not valid tenantId!"));

      // Parse comma-separated processed item machining batch IDs
      List<Long> processedItemMachiningBatchIdList = Arrays.stream(processedItemMachiningBatchIds.split(","))
          .map(String::trim)
          .filter(id -> !id.isEmpty())
          .map(id -> GenericResourceUtils.convertResourceIdToLong(id)
              .orElseThrow(() -> new RuntimeException("Not valid processedItemMachiningBatchId: " + id)))
          .collect(Collectors.toList());

      if (processedItemMachiningBatchIdList.isEmpty()) {
        log.error("No valid processed item machining batch IDs provided");
        throw new IllegalArgumentException("At least one valid processed item machining batch ID is required");
      }

      List<MachiningBatchRepresentation> machiningBatchRepresentations = machiningBatchService.getMachiningBatchesByProcessedItemMachiningBatchIds(processedItemMachiningBatchIdList, tenantIdLongValue);
      
      MachiningBatchListRepresentation machiningBatchListRepresentation = MachiningBatchListRepresentation.builder()
          .machiningBatches(machiningBatchRepresentations)
          .build();
      
      return ResponseEntity.ok(machiningBatchListRepresentation);

    } catch (Exception exception) {
      if (exception instanceof IllegalArgumentException) {
        log.error("Invalid data for getMachiningBatchesByProcessedItemMachiningBatchIds: {}", exception.getMessage());
        return new ResponseEntity<>(new ErrorResponse(exception.getMessage()), HttpStatus.BAD_REQUEST);
      }
      log.error("Error processing getMachiningBatchesByProcessedItemMachiningBatchIds: {}", exception.getMessage());
      return new ResponseEntity<>(new ErrorResponse("Error retrieving machining batches by processed item machining batch IDs"), HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  private boolean isInvalidMachiningBatchDetailsForApplying(MachiningBatchRepresentation representation, boolean rework) {
    if (representation == null ||
        isNullOrEmpty(representation.getMachiningBatchNumber()) ||
        isNullOrEmpty(representation.getCreateAt()) ||
        representation.getProcessedItemMachiningBatch() == null ||
        isInvalidMachiningBatchPiecesCount(representation.getProcessedItemMachiningBatch().getMachiningBatchPiecesCount())) {
      return true;
    }

    if (rework) {
      return isReworkInvalid(representation);
    } else {
      return isNonReworkInvalid(representation);
    }
  }

  private boolean isReworkInvalid(MachiningBatchRepresentation representation) {
    return representation.getInputProcessedItemMachiningBatch() == null ||
           representation.getInputProcessedItemMachiningBatch().getId() == null ||
           isInvalidMachiningBatchPiecesCount(representation.getInputProcessedItemMachiningBatch().getReworkPiecesCountAvailableForRework());
  }

  private boolean isNonReworkInvalid(MachiningBatchRepresentation representation) {
    if (!isNullOrEmpty(representation.getMachiningHeats())) {
      return representation.getMachiningHeats().stream()
                 .filter(heat -> heat.getHeat().getIsInPieces())
                 .anyMatch(machiningHeatRepresentation ->
                               isInvalidMachiningBatchPiecesCount(machiningHeatRepresentation.getHeat().getPiecesCount())) ||
             representation.getMachiningHeats().stream()
                 .filter(heat -> heat.getHeat().getIsInPieces())
                 .anyMatch(h -> isInvalidMachiningBatchPiecesCount(h.getPiecesUsed()));
    }
    return representation.getProcessedItemHeatTreatmentBatch() == null ||
           representation.getProcessedItemHeatTreatmentBatch().getId() == null ||
           representation.getProcessedItemHeatTreatmentBatch().getAvailableMachiningBatchPiecesCount() == null;
  }

  private boolean isNullOrEmpty(String value) {
    return value == null || value.isEmpty();
  }

  private boolean isNullOrEmpty(Collection collection) {
    return collection == null || collection.isEmpty();
  }

  private boolean isInvalidMachiningBatchPiecesCount(Integer count) {
    return count == null || count == 0;
  }

  private boolean isInvalidDailyMachiningBatchRepresentation(DailyMachiningBatchRepresentation dailyMachiningBatchRepresentation) {
    if (dailyMachiningBatchRepresentation == null ||
        isNullOrEmpty(dailyMachiningBatchRepresentation.getDailyMachiningBatchNumber()) ||
        dailyMachiningBatchRepresentation.getStartDateTime() == null || dailyMachiningBatchRepresentation.getStartDateTime().isEmpty() ||
        dailyMachiningBatchRepresentation.getEndDateTime() == null || dailyMachiningBatchRepresentation.getEndDateTime().isEmpty() ||
        dailyMachiningBatchRepresentation.getMachineOperator() == null || dailyMachiningBatchRepresentation.getMachineOperator().getOperator().getId() == null ||
        dailyMachiningBatchRepresentation.getMachineSet() == null || dailyMachiningBatchRepresentation.getMachineSet().getId() == null) {
      return true;
    }
    return false;
  }

  private boolean isInvalidMachiningBatchDetailsForEnding(MachiningBatchRepresentation machiningBatchRepresentation) {
    if (machiningBatchRepresentation == null ||
        machiningBatchRepresentation.getEndAt() == null || machiningBatchRepresentation.getEndAt().isEmpty()) {
      return true;
    }
    return false;
  }

  private boolean isInvalidMachiningBatchDetailsForCreating(MachiningBatchRepresentation representation) {
    if (representation == null ||
        isNullOrEmpty(representation.getMachiningBatchNumber()) ||
        isNullOrEmpty(representation.getCreateAt()) ||
        representation.getProcessedItemMachiningBatch() == null ||
        isInvalidMachiningBatchPiecesCount(representation.getProcessedItemMachiningBatch().getMachiningBatchPiecesCount())) {
      return true;
    }

    return representation.getProcessedItemHeatTreatmentBatch() == null ||
           representation.getProcessedItemHeatTreatmentBatch().getId() == null ||
           representation.getProcessedItemHeatTreatmentBatch().getAvailableMachiningBatchPiecesCount() == null;
  }
}
