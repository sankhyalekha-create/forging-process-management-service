package com.jangid.forging_process_management_service.resource.machining;

import com.jangid.forging_process_management_service.entitiesRepresentation.error.ErrorResponse;
import com.jangid.forging_process_management_service.entitiesRepresentation.machining.MachineListRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.machining.MachineRepresentation;
import com.jangid.forging_process_management_service.exception.TenantNotFoundException;
import com.jangid.forging_process_management_service.exception.machining.MachineNotFoundException;
import com.jangid.forging_process_management_service.service.machining.MachineService;
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
public class MachineResource {

  @Autowired
  private MachineService machineService;

  @PostMapping("tenant/{tenantId}/machine")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<?> createMachine(@PathVariable String tenantId, @RequestBody MachineRepresentation machineRepresentation) {
    try {
      if (tenantId == null || tenantId.isEmpty() || isInvalidMachineRepresentation(machineRepresentation)) {
        log.error("invalid createMachine input!");
        throw new RuntimeException("invalid createMachine input!");
      }
      Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Not valid tenantId!"));
      MachineRepresentation createdMachine = machineService.createMachine(tenantIdLongValue, machineRepresentation);
      return new ResponseEntity<>(createdMachine, HttpStatus.CREATED);
    } catch (Exception exception) {
      if (exception instanceof IllegalStateException) {
        // Generate a more descriptive error message
        String errorMessage = exception.getMessage();
        log.error("Machine creation failed: {}", errorMessage);
        
        if (errorMessage.contains("with name=")) {
          return new ResponseEntity<>(
              new ErrorResponse("A machine with the name '" + machineRepresentation.getMachineName() + "' already exists"),
              HttpStatus.CONFLICT);
        } else {
          return new ResponseEntity<>(
              new ErrorResponse(errorMessage),
              HttpStatus.CONFLICT);
        }
      } else if (exception instanceof IllegalArgumentException) {
        log.error("Invalid machine data: {}", exception.getMessage());
        return new ResponseEntity<>(
            new ErrorResponse(exception.getMessage()),
            HttpStatus.BAD_REQUEST);
      }
      
      log.error("Error creating machine: {}", exception.getMessage());
      return new ResponseEntity<>(
          new ErrorResponse("Error creating machine: " + exception.getMessage()),
          HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @GetMapping("tenant/{tenantId}/machines")
  public ResponseEntity<?> getAllMachinesOfTenant(@ApiParam(value = "Identifier of the tenant", required = true) @PathVariable String tenantId,
                                                  @RequestParam(value = "page", required = false) String page,
                                                  @RequestParam(value = "size", required = false) String size) {
      Long tId = GenericResourceUtils.convertResourceIdToLong(tenantId)
          .orElseThrow(() -> new TenantNotFoundException(tenantId));

    Integer pageNumber = (page == null || page.isBlank()) ? -1
                                                          : GenericResourceUtils.convertResourceIdToInt(page)
                             .orElseThrow(() -> new RuntimeException("Invalid page=" + page));

    Integer sizeNumber = (size == null || size.isBlank()) ? -1
                                                          : GenericResourceUtils.convertResourceIdToInt(size)
                             .orElseThrow(() -> new RuntimeException("Invalid size=" + size));

    if (pageNumber == -1 || sizeNumber == -1) {
      MachineListRepresentation machineListRepresentation = machineService.getAllMachinesOfTenantWithoutPagination(tId);
      return ResponseEntity.ok(machineListRepresentation);
    }
      Page<MachineRepresentation> machines = machineService.getAllMachinesOfTenant(tId, pageNumber, sizeNumber);
      return ResponseEntity.ok(machines);
    }

  @GetMapping("tenant/{tenantId}/available-machines")
  public ResponseEntity<MachineListRepresentation> getAllMachinesAvailableForMachineSetOfTenant(@ApiParam(value = "Identifier of the tenant", required = true) @PathVariable String tenantId) {
    Long tId = GenericResourceUtils.convertResourceIdToLong(tenantId)
        .orElseThrow(() -> new TenantNotFoundException(tenantId));

      MachineListRepresentation machineListRepresentation = machineService.getAllMachinesAvailableForMachineSetOfTenant(tId);
      return ResponseEntity.ok(machineListRepresentation);
  }

  @PostMapping("tenant/{tenantId}/machine/{machineId}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<MachineRepresentation> updateMachine(
      @PathVariable("tenantId") String tenantId, @PathVariable("machineId") String machineId,
      @RequestBody MachineRepresentation machineRepresentation) {
    if (tenantId == null || tenantId.isEmpty() || machineId == null || machineId.isEmpty() || isInvalidMachineRepresentation(machineRepresentation)) {
      log.error("invalid input for updateMachine!");
      throw new RuntimeException("invalid input for updateMachine!");
    }
    Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
        .orElseThrow(() -> new RuntimeException("Not valid tenant id!"));

    Long machineIdLongValue = GenericResourceUtils.convertResourceIdToLong(machineId)
        .orElseThrow(() -> new RuntimeException("Not valid machineId!"));

    MachineRepresentation updatedMachine = machineService.updateMachine(machineIdLongValue, tenantIdLongValue, machineRepresentation);
    return ResponseEntity.ok(updatedMachine);
  }

  @DeleteMapping("tenant/{tenantId}/machine/{machineId}")
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<?> deleteMachine(
      @PathVariable("tenantId") String tenantId, @PathVariable("machineId") String machineId) {
    try {
      if (tenantId == null || tenantId.isEmpty() || machineId == null || machineId.isEmpty()) {
        log.error("invalid input for deleteMachine!");
        throw new RuntimeException("invalid input for deleteMachine!");
      }
      Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Not valid tenant id!"));

      Long machineIdLongValue = GenericResourceUtils.convertResourceIdToLong(machineId)
          .orElseThrow(() -> new RuntimeException("Not valid machineId!"));

      machineService.deleteMachine(machineIdLongValue, tenantIdLongValue);

      return ResponseEntity.ok().build();
    } catch (Exception exception) {
      if (exception instanceof MachineNotFoundException) {
        return ResponseEntity.notFound().build();
      }
      if (exception instanceof IllegalStateException) {
        if (exception.getMessage().contains("part of a MachineSet")) {
          log.error("This machine is part of a MachineSet. Please remove it from the MachineSet before proceeding.");
          return new ResponseEntity<>(new ErrorResponse("This machine is part of a MachineSet. Please remove it from the MachineSet before proceeding."), HttpStatus.CONFLICT);
        }
      }
      log.error("Error while deleting machine: {}", exception.getMessage());
      return new ResponseEntity<>(new ErrorResponse("Error while deleting machine"),
                                  HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @GetMapping(value = "tenant/{tenantId}/searchMachines", produces = MediaType.APPLICATION_JSON)
  public ResponseEntity<Page<MachineRepresentation>> searchMachines(
      @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable("tenantId") String tenantId,
      @ApiParam(value = "Type of search", required = true, allowableValues = "MACHINE_NAME") @RequestParam("searchType") String searchType,
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

      Page<MachineRepresentation> searchResults = machineService.searchMachines(tenantIdLongValue, searchType.trim(), searchTerm.trim(), pageNumber, pageSize);
      return ResponseEntity.ok(searchResults);

    } catch (IllegalArgumentException e) {
      log.error("Invalid search parameters: {}", e.getMessage());
      return ResponseEntity.badRequest().build();
    } catch (Exception e) {
      log.error("Error during machine search: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  private boolean isInvalidMachineRepresentation(MachineRepresentation machineRepresentation) {
    if (machineRepresentation == null ||
        machineRepresentation.getMachineName() == null || machineRepresentation.getMachineName().isEmpty()) {
      return true;
    }
    return false;
  }
}
