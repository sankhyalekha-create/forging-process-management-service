package com.jangid.forging_process_management_service.resource.operator;

import com.jangid.forging_process_management_service.entitiesRepresentation.operator.MachineOperatorListRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.operator.MachineOperatorRepresentation;
import com.jangid.forging_process_management_service.exception.TenantNotFoundException;
import com.jangid.forging_process_management_service.service.operator.MachineOperatorService;
import com.jangid.forging_process_management_service.utils.ConvertorUtils;
import com.jangid.forging_process_management_service.utils.GenericResourceUtils;

import io.swagger.annotations.ApiParam;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MachineOperatorResource {

  private final MachineOperatorService machineOperatorService;

  @GetMapping("tenant/{tenantId}/machine-operators")
  public ResponseEntity<?> getAllMachineOperatorsOfTenant(@ApiParam(value = "Identifier of the tenant", required = true) @PathVariable String tenantId,
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
      MachineOperatorListRepresentation machineOperatorListRepresentation = machineOperatorService.getAllMachineOperatorsOfTenantWithoutPagination(tId);
      return ResponseEntity.ok(machineOperatorListRepresentation);
    }
    Page<MachineOperatorRepresentation> machineOperators = machineOperatorService.getAllMachineOperatorsOfTenant(tId, pageNumber, sizeNumber);
    return ResponseEntity.ok(machineOperators);
  }

  @GetMapping("tenant/{tenantId}/available-machine-operators-for-provided-time-period")
  public ResponseEntity<MachineOperatorListRepresentation> getAllMachineOperatorsAvailableForMachineSetOfTenantForProvidedTime(@ApiParam(value = "Identifier of the tenant", required = true) @PathVariable String tenantId,
                                                                 @RequestParam(value = "startDateTime", required = true) String startDateTime,
                                                                 @RequestParam(value = "endDateTime", required = true) String endDateTime) {
    Long tId = GenericResourceUtils.convertResourceIdToLong(tenantId)
        .orElseThrow(() -> new TenantNotFoundException(tenantId));

    LocalDateTime startTime = ConvertorUtils.convertStringToLocalDateTime(startDateTime);
    LocalDateTime endTime = ConvertorUtils.convertStringToLocalDateTime(endDateTime);

    MachineOperatorListRepresentation machineOperatorListRepresentation = machineOperatorService.getAllMachineOperatorsOfTenantAvailableForMachining(startTime, endTime, tId);
    return ResponseEntity.ok(machineOperatorListRepresentation);
  }
}
