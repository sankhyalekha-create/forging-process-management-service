package com.jangid.forging_process_management_service.resource.quality;

import com.jangid.forging_process_management_service.entitiesRepresentation.quality.EquipmentGroupListRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.quality.EquipmentGroupRepresentation;
import com.jangid.forging_process_management_service.exception.TenantNotFoundException;
import com.jangid.forging_process_management_service.service.quality.EquipmentGroupService;
import com.jangid.forging_process_management_service.utils.GenericResourceUtils;
import com.jangid.forging_process_management_service.utils.GenericExceptionHandler;

import io.swagger.annotations.ApiParam;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Component
public class EquipmentGroupResource {

  private final EquipmentGroupService equipmentGroupService;

  @PostMapping("tenant/{tenantId}/equipment-group")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<?> createEquipmentGroup(@PathVariable String tenantId, @RequestBody EquipmentGroupRepresentation equipmentGroupRepresentation) {
    try {
      if (tenantId == null || tenantId.isEmpty() || isInvalidEquipmentGroupRepresentation(equipmentGroupRepresentation)) {
        log.error("invalid createEquipmentGroup input!");
        throw new RuntimeException("invalid createEquipmentGroup input!");
      }
      Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
        .orElseThrow(() -> new RuntimeException("Not valid tenantId!"));
      EquipmentGroupRepresentation createdEquipmentGroup = equipmentGroupService.createEquipmentGroup(tenantIdLongValue, equipmentGroupRepresentation);
      return new ResponseEntity<>(createdEquipmentGroup, HttpStatus.CREATED);
    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "createEquipmentGroup");
    }
  }

  @GetMapping("tenant/{tenantId}/equipment-groups")
  public ResponseEntity<?> getAllEquipmentGroupsOfTenant(@ApiParam(value = "Identifier of the tenant", required = true) @PathVariable String tenantId,
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
        EquipmentGroupListRepresentation equipmentGroupListRepresentation = equipmentGroupService.getAllEquipmentGroupsOfTenantWithoutPagination(tId);
        return ResponseEntity.ok(equipmentGroupListRepresentation);
      }
      Page<EquipmentGroupRepresentation> equipmentGroups = equipmentGroupService.getAllEquipmentGroupsOfTenant(tId, pageNumber, sizeNumber);
      return ResponseEntity.ok(equipmentGroups);
    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "getAllEquipmentGroupsOfTenant");
    }
  }

  @GetMapping("tenant/{tenantId}/equipment-group/{equipmentGroupId}")
  public ResponseEntity<?> getEquipmentGroupById(@PathVariable String tenantId, @PathVariable String equipmentGroupId) {
    try {
      Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
        .orElseThrow(() -> new RuntimeException("Not valid tenantId!"));

      Long equipmentGroupIdLongValue = GenericResourceUtils.convertResourceIdToLong(equipmentGroupId)
        .orElseThrow(() -> new RuntimeException("Not valid equipmentGroupId!"));

      EquipmentGroupRepresentation equipmentGroup = equipmentGroupService.getEquipmentGroupById(equipmentGroupIdLongValue, tenantIdLongValue);
      return ResponseEntity.ok(equipmentGroup);
    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "getEquipmentGroupById");
    }
  }

  @PostMapping("tenant/{tenantId}/equipment-group/{equipmentGroupId}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<?> updateEquipmentGroup(
    @PathVariable("tenantId") String tenantId, @PathVariable("equipmentGroupId") String equipmentGroupId,
    @RequestBody EquipmentGroupRepresentation equipmentGroupRepresentation) {
    try {
      if (tenantId == null || tenantId.isEmpty() || equipmentGroupId == null || equipmentGroupId.isEmpty() || isInvalidEquipmentGroupRepresentation(equipmentGroupRepresentation)) {
        log.error("invalid input for updateEquipmentGroup!");
        throw new RuntimeException("invalid input for updateEquipmentGroup!");
      }
      Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
        .orElseThrow(() -> new RuntimeException("Not valid tenant id!"));

      Long equipmentGroupIdLongValue = GenericResourceUtils.convertResourceIdToLong(equipmentGroupId)
        .orElseThrow(() -> new RuntimeException("Not valid equipmentGroupId!"));

      EquipmentGroupRepresentation updatedEquipmentGroup = equipmentGroupService.updateEquipmentGroup(equipmentGroupIdLongValue, tenantIdLongValue, equipmentGroupRepresentation);
      return ResponseEntity.ok(updatedEquipmentGroup);
    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "updateEquipmentGroup");
    }
  }

  @DeleteMapping("tenant/{tenantId}/equipment-group/{equipmentGroupId}")
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<?> deleteEquipmentGroup(
    @PathVariable("tenantId") String tenantId,
    @PathVariable("equipmentGroupId") String equipmentGroupId) {
    try {
      if (tenantId == null || tenantId.isEmpty() || equipmentGroupId == null || equipmentGroupId.isEmpty()) {
        log.error("invalid input for deleteEquipmentGroup!");
        throw new RuntimeException("invalid input for deleteEquipmentGroup!");
      }

      Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
        .orElseThrow(() -> new RuntimeException("Not valid tenant id!"));

      Long equipmentGroupIdLongValue = GenericResourceUtils.convertResourceIdToLong(equipmentGroupId)
        .orElseThrow(() -> new RuntimeException("Not valid equipmentGroupId!"));

      equipmentGroupService.deleteEquipmentGroup(equipmentGroupIdLongValue, tenantIdLongValue);
      return ResponseEntity.ok().build();
    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "deleteEquipmentGroup");
    }
  }

  @PostMapping("tenant/{tenantId}/equipment-group/{equipmentGroupId}/add-gauges")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<?> addGaugesToGroup(
    @PathVariable("tenantId") String tenantId,
    @PathVariable("equipmentGroupId") String equipmentGroupId,
    @RequestBody List<Long> gaugeIds) {
    try {
      if (tenantId == null || tenantId.isEmpty() || equipmentGroupId == null || equipmentGroupId.isEmpty() || gaugeIds == null || gaugeIds.isEmpty()) {
        log.error("invalid input for addGaugesToGroup!");
        throw new RuntimeException("invalid input for addGaugesToGroup!");
      }

      Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
        .orElseThrow(() -> new RuntimeException("Not valid tenant id!"));

      Long equipmentGroupIdLongValue = GenericResourceUtils.convertResourceIdToLong(equipmentGroupId)
        .orElseThrow(() -> new RuntimeException("Not valid equipmentGroupId!"));

      EquipmentGroupRepresentation updatedEquipmentGroup = equipmentGroupService.addGaugesToGroup(equipmentGroupIdLongValue, tenantIdLongValue, gaugeIds);
      return ResponseEntity.ok(updatedEquipmentGroup);
    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "addGaugesToGroup");
    }
  }

  @DeleteMapping("tenant/{tenantId}/equipment-group/{equipmentGroupId}/gauge/{gaugeId}")
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<?> removeGaugeFromGroup(
    @PathVariable("tenantId") String tenantId,
    @PathVariable("equipmentGroupId") String equipmentGroupId,
    @PathVariable("gaugeId") String gaugeId) {
    try {
      if (tenantId == null || tenantId.isEmpty() || equipmentGroupId == null || equipmentGroupId.isEmpty() || gaugeId == null || gaugeId.isEmpty()) {
        log.error("invalid input for removeGaugeFromGroup!");
        throw new RuntimeException("invalid input for removeGaugeFromGroup!");
      }

      Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
        .orElseThrow(() -> new RuntimeException("Not valid tenant id!"));

      Long equipmentGroupIdLongValue = GenericResourceUtils.convertResourceIdToLong(equipmentGroupId)
        .orElseThrow(() -> new RuntimeException("Not valid equipmentGroupId!"));

      Long gaugeIdLongValue = GenericResourceUtils.convertResourceIdToLong(gaugeId)
        .orElseThrow(() -> new RuntimeException("Not valid gaugeId!"));

      EquipmentGroupRepresentation updatedEquipmentGroup = equipmentGroupService.removeGaugeFromGroup(equipmentGroupIdLongValue, tenantIdLongValue, gaugeIdLongValue);
      return ResponseEntity.ok(updatedEquipmentGroup);
    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "removeGaugeFromGroup");
    }
  }

  private boolean isInvalidEquipmentGroupRepresentation(EquipmentGroupRepresentation equipmentGroupRepresentation) {
    return equipmentGroupRepresentation == null ||
      equipmentGroupRepresentation.getGroupName() == null || equipmentGroupRepresentation.getGroupName().isEmpty();
  }
}
