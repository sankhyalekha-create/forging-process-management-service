package com.jangid.forging_process_management_service.resource.machining;

import com.jangid.forging_process_management_service.entitiesRepresentation.error.ErrorResponse;
import com.jangid.forging_process_management_service.entitiesRepresentation.machining.MachineSetRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.machining.MachineSetListRepresentation;
import com.jangid.forging_process_management_service.exception.ResourceNotFoundException;
import com.jangid.forging_process_management_service.exception.TenantNotFoundException;
import com.jangid.forging_process_management_service.exception.machining.MachineSetNotFoundException;
import com.jangid.forging_process_management_service.service.machining.MachineSetService;
import com.jangid.forging_process_management_service.utils.GenericResourceUtils;
import com.jangid.forging_process_management_service.utils.ConvertorUtils;

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

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Component
public class MachineSetResource {

  @Autowired
  private MachineSetService machineSetService;

  @PostMapping("tenant/{tenantId}/machineSet")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<?> createMachineSet(@PathVariable String tenantId, @RequestBody MachineSetRepresentation machineSetRepresentation) {
    try {
      if (tenantId == null || tenantId.isEmpty() || isInvalidMachineSetRepresentation(machineSetRepresentation)) {
        log.error("invalid createMachine input!");
        throw new RuntimeException("invalid createMachine input!");
      }
      Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Not valid tenantId!"));
      MachineSetRepresentation createdMachineSet = machineSetService.createMachineSet(tenantIdLongValue, machineSetRepresentation);
      return new ResponseEntity<>(createdMachineSet, HttpStatus.CREATED);
    } catch (Exception exception) {
      if (exception instanceof IllegalStateException) {
        // Generate a more descriptive error message
        String errorMessage = exception.getMessage();
        log.error("Machine set creation failed: {}", errorMessage);
        
        if (errorMessage.contains("with name=")) {
          return new ResponseEntity<>(
              new ErrorResponse("A machine set with the name '" + machineSetRepresentation.getMachineSetName() + "' already exists for this tenant"),
              HttpStatus.CONFLICT);
        } else {
          return new ResponseEntity<>(
              new ErrorResponse(errorMessage),
              HttpStatus.CONFLICT);
        }
      } else if (exception instanceof ResourceNotFoundException) {
        log.error("Resource not found: {}", exception.getMessage());
        return new ResponseEntity<>(
            new ErrorResponse(exception.getMessage()),
            HttpStatus.NOT_FOUND);
      } else if (exception instanceof IllegalArgumentException) {
        log.error("Invalid machine set data: {}", exception.getMessage());
        return new ResponseEntity<>(
            new ErrorResponse(exception.getMessage()),
            HttpStatus.BAD_REQUEST);
      }
      
      log.error("Error creating machine set: {}", exception.getMessage());
      return new ResponseEntity<>(
          new ErrorResponse("Error creating machine set: " + exception.getMessage()),
          HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @GetMapping("tenant/{tenantId}/machineSets")
  public ResponseEntity<Page<MachineSetRepresentation>> getAllMachineSetsOfTenant(@ApiParam(value = "Identifier of the tenant", required = true) @PathVariable String tenantId,
                                                                            @RequestParam(value = "page", defaultValue = "0") String page,
                                                                            @RequestParam(value = "size", defaultValue = "5") String size) {
    Long tId = GenericResourceUtils.convertResourceIdToLong(tenantId)
        .orElseThrow(() -> new TenantNotFoundException(tenantId));

    int pageNumber = GenericResourceUtils.convertResourceIdToInt(page)
        .orElseThrow(() -> new RuntimeException("Invalid page="+page));

    int sizeNumber = GenericResourceUtils.convertResourceIdToInt(size)
        .orElseThrow(() -> new RuntimeException("Invalid size="+size));

    Page<MachineSetRepresentation> machines = machineSetService.getAllMachineSetPagesOfTenant(tId, pageNumber, sizeNumber);
    return ResponseEntity.ok(machines);
  }

  @GetMapping("tenant/{tenantId}/available-machine-sets-for-provided-time-period")
  public ResponseEntity<MachineSetListRepresentation> getAvailableMachineSetsForProvidedTimePeriod(
      @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable String tenantId,
      @RequestParam(value = "startDateTime", required = true) String startDateTime,
      @RequestParam(value = "endDateTime", required = true) String endDateTime) {
    
    Long tId = GenericResourceUtils.convertResourceIdToLong(tenantId)
        .orElseThrow(() -> new TenantNotFoundException(tenantId));

    LocalDateTime startTime = ConvertorUtils.convertStringToLocalDateTime(startDateTime);
    LocalDateTime endTime = ConvertorUtils.convertStringToLocalDateTime(endDateTime);

    MachineSetListRepresentation machineSetListRepresentation = machineSetService.getAvailableMachineSetsForTimeRange(tId, startTime, endTime);
    return ResponseEntity.ok(machineSetListRepresentation);
  }

  @PostMapping("tenant/{tenantId}/machineSet/{machineSetId}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<MachineSetRepresentation> updateMachineSet(
      @PathVariable("tenantId") String tenantId, @PathVariable("machineSetId") String machineSetId,
      @RequestBody MachineSetRepresentation machineSetRepresentation) {
    if (tenantId == null || tenantId.isEmpty() || machineSetId == null || machineSetId.isEmpty() || isInvalidMachineSetRepresentation(machineSetRepresentation)) {
      log.error("invalid input for updateMachineSet!");
      throw new RuntimeException("invalid input for updateMachineSet!");
    }
    Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
        .orElseThrow(() -> new RuntimeException("Not valid tenant id!"));

    Long machineSetIdLongValue = GenericResourceUtils.convertResourceIdToLong(machineSetId)
        .orElseThrow(() -> new RuntimeException("Not valid machineSetId!"));

    MachineSetRepresentation updatedMachineSet = machineSetService.updateMachineSet(machineSetIdLongValue, tenantIdLongValue, machineSetRepresentation);
    return ResponseEntity.ok(updatedMachineSet);
  }

  @DeleteMapping("tenant/{tenantId}/machineSet/{machineSetId}")
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<?> deleteMachineSet(
      @PathVariable("tenantId") String tenantId,
      @PathVariable("machineSetId") String machineSetId) {
    try {
      if (tenantId == null || tenantId.isEmpty() || machineSetId == null || machineSetId.isEmpty()) {
        log.error("invalid input for deleteMachineSet!");
        throw new RuntimeException("invalid input for deleteMachineSet!");
      }

      Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Not valid tenant id!"));

      Long machineSetIdLongValue = GenericResourceUtils.convertResourceIdToLong(machineSetId)
          .orElseThrow(() -> new RuntimeException("Not valid machineSetId!"));

      machineSetService.deleteMachineSet(machineSetIdLongValue, tenantIdLongValue);
      return ResponseEntity.ok().build();
    } catch (Exception exception) {
      if (exception instanceof MachineSetNotFoundException) {
        return ResponseEntity.notFound().build();
      }
      if (exception instanceof IllegalStateException) {
        if (exception.getMessage().contains("not in MACHINING_NOT_APPLIED status")) {
          log.error("This MachineSet cannot be deleted because it is not in the MACHINING_NOT_APPLIED status.");
          return new ResponseEntity<>(new ErrorResponse("This MachineSet cannot be deleted as it is not in the MACHINING_NOT_APPLIED status."), HttpStatus.CONFLICT);
        }
      }
      log.error("Error while deleting machine set: {}", exception.getMessage());
      return new ResponseEntity<>(new ErrorResponse("Error while deleting machine set"),
                                  HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @GetMapping(value = "tenant/{tenantId}/searchMachineSets", produces = MediaType.APPLICATION_JSON)
  public ResponseEntity<Page<MachineSetRepresentation>> searchMachineSets(
      @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable("tenantId") String tenantId,
      @ApiParam(value = "Type of search", required = true, allowableValues = "MACHINE_SET_NAME,MACHINE_NAME") @RequestParam("searchType") String searchType,
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

      Page<MachineSetRepresentation> searchResults = machineSetService.searchMachineSets(tenantIdLongValue, searchType.trim(), searchTerm.trim(), pageNumber, pageSize);
      return ResponseEntity.ok(searchResults);

    } catch (IllegalArgumentException e) {
      log.error("Invalid search parameters: {}", e.getMessage());
      return ResponseEntity.badRequest().build();
    } catch (Exception e) {
      log.error("Error during machine set search: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  private boolean isInvalidMachineSetRepresentation(MachineSetRepresentation machineSetRepresentation) {
    if (machineSetRepresentation == null ||
        machineSetRepresentation.getMachineSetName() == null || machineSetRepresentation.getMachineSetName().isEmpty() ||
        machineSetRepresentation.getMachines() == null || machineSetRepresentation.getMachines().isEmpty()) {
      return true;
    }
    return false;
  }
}
