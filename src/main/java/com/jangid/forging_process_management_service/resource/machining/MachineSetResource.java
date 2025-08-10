package com.jangid.forging_process_management_service.resource.machining;

import com.jangid.forging_process_management_service.entitiesRepresentation.machining.MachineSetRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.machining.MachineSetListRepresentation;
import com.jangid.forging_process_management_service.exception.TenantNotFoundException;
import com.jangid.forging_process_management_service.service.machining.MachineSetService;
import com.jangid.forging_process_management_service.utils.GenericResourceUtils;
import com.jangid.forging_process_management_service.utils.GenericExceptionHandler;
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
        throw new IllegalArgumentException("Invalid createMachine input!");
      }
      Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
          .orElseThrow(() -> new IllegalArgumentException("Not valid tenantId!"));
      MachineSetRepresentation createdMachineSet = machineSetService.createMachineSet(tenantIdLongValue, machineSetRepresentation);
      return new ResponseEntity<>(createdMachineSet, HttpStatus.CREATED);
    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "createMachineSet");
    }
  }

  @GetMapping("tenant/{tenantId}/machineSets")
  public ResponseEntity<?> getAllMachineSetsOfTenant(@ApiParam(value = "Identifier of the tenant", required = true) @PathVariable String tenantId,
                                                                            @RequestParam(value = "page", defaultValue = "0") String page,
                                                                            @RequestParam(value = "size", defaultValue = "5") String size) {
    try {
      Long tId = GenericResourceUtils.convertResourceIdToLong(tenantId)
          .orElseThrow(() -> new TenantNotFoundException(tenantId));

      int pageNumber = GenericResourceUtils.convertResourceIdToInt(page)
          .orElseThrow(() -> new IllegalArgumentException("Invalid page="+page));

      int sizeNumber = GenericResourceUtils.convertResourceIdToInt(size)
          .orElseThrow(() -> new IllegalArgumentException("Invalid size="+size));

      Page<MachineSetRepresentation> machines = machineSetService.getAllMachineSetPagesOfTenant(tId, pageNumber, sizeNumber);
      return ResponseEntity.ok(machines);
    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "getAllMachineSetsOfTenant");
    }
  }

  @GetMapping("tenant/{tenantId}/available-machine-sets-for-provided-time-period")
  public ResponseEntity<?> getAvailableMachineSetsForProvidedTimePeriod(
      @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable String tenantId,
      @RequestParam(value = "startDateTime", required = true) String startDateTime,
      @RequestParam(value = "endDateTime", required = true) String endDateTime) {
    
    try {
      Long tId = GenericResourceUtils.convertResourceIdToLong(tenantId)
          .orElseThrow(() -> new TenantNotFoundException(tenantId));

      LocalDateTime startTime = ConvertorUtils.convertStringToLocalDateTime(startDateTime);
      LocalDateTime endTime = ConvertorUtils.convertStringToLocalDateTime(endDateTime);

      MachineSetListRepresentation machineSetListRepresentation = machineSetService.getAvailableMachineSetsForTimeRange(tId, startTime, endTime);
      return ResponseEntity.ok(machineSetListRepresentation);
    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "getAvailableMachineSetsForProvidedTimePeriod");
    }
  }

  @PostMapping("tenant/{tenantId}/machineSet/{machineSetId}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<?> updateMachineSet(
      @PathVariable("tenantId") String tenantId, @PathVariable("machineSetId") String machineSetId,
      @RequestBody MachineSetRepresentation machineSetRepresentation) {
    try {
      if (tenantId == null || tenantId.isEmpty() || machineSetId == null || machineSetId.isEmpty() || isInvalidMachineSetRepresentation(machineSetRepresentation)) {
        log.error("invalid input for updateMachineSet!");
        throw new IllegalArgumentException("Invalid input for updateMachineSet!");
      }
      Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
          .orElseThrow(() -> new IllegalArgumentException("Not valid tenant id!"));

      Long machineSetIdLongValue = GenericResourceUtils.convertResourceIdToLong(machineSetId)
          .orElseThrow(() -> new IllegalArgumentException("Not valid machineSetId!"));

      MachineSetRepresentation updatedMachineSet = machineSetService.updateMachineSet(machineSetIdLongValue, tenantIdLongValue, machineSetRepresentation);
      return ResponseEntity.ok(updatedMachineSet);
    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "updateMachineSet");
    }
  }

  @DeleteMapping("tenant/{tenantId}/machineSet/{machineSetId}")
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<?> deleteMachineSet(
      @PathVariable("tenantId") String tenantId,
      @PathVariable("machineSetId") String machineSetId) {
    try {
      if (tenantId == null || tenantId.isEmpty() || machineSetId == null || machineSetId.isEmpty()) {
        log.error("invalid input for deleteMachineSet!");
        throw new IllegalArgumentException("Invalid input for deleteMachineSet!");
      }

      Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
          .orElseThrow(() -> new IllegalArgumentException("Not valid tenant id!"));

      Long machineSetIdLongValue = GenericResourceUtils.convertResourceIdToLong(machineSetId)
          .orElseThrow(() -> new IllegalArgumentException("Not valid machineSetId!"));

      machineSetService.deleteMachineSet(machineSetIdLongValue, tenantIdLongValue);
      return ResponseEntity.ok().build();
    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "deleteMachineSet");
    }
  }

  @GetMapping(value = "tenant/{tenantId}/searchMachineSets", produces = MediaType.APPLICATION_JSON)
  public ResponseEntity<?> searchMachineSets(
      @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable("tenantId") String tenantId,
      @ApiParam(value = "Type of search", required = true, allowableValues = "MACHINE_SET_NAME,MACHINE_NAME") @RequestParam("searchType") String searchType,
      @ApiParam(value = "Search term", required = true) @RequestParam("searchTerm") String searchTerm,
      @ApiParam(value = "Page number (0-based)", required = false) @RequestParam(value = "page", defaultValue = "0") String pageParam,
      @ApiParam(value = "Page size", required = false) @RequestParam(value = "size", defaultValue = "10") String sizeParam) {

    try {
      Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
          .orElseThrow(() -> new IllegalArgumentException("Not valid tenantId!"));
      
      if (searchType == null || searchType.trim().isEmpty()) {
        throw new IllegalArgumentException("Search type cannot be empty");
      }
      
      if (searchTerm == null || searchTerm.trim().isEmpty()) {
        throw new IllegalArgumentException("Search term cannot be empty");
      }

      int pageNumber = GenericResourceUtils.convertResourceIdToInt(pageParam)
          .orElseThrow(() -> new IllegalArgumentException("Invalid page=" + pageParam));

      int pageSize = GenericResourceUtils.convertResourceIdToInt(sizeParam)
          .orElseThrow(() -> new IllegalArgumentException("Invalid size=" + sizeParam));

      if (pageNumber < 0) {
        pageNumber = 0;
      }

      if (pageSize <= 0) {
        pageSize = 10; // Default page size
      }

      Page<MachineSetRepresentation> searchResults = machineSetService.searchMachineSets(tenantIdLongValue, searchType.trim(), searchTerm.trim(), pageNumber, pageSize);
      return ResponseEntity.ok(searchResults);

    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "searchMachineSets");
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
