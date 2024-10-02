package com.jangid.forging_process_management_service.resource;

import com.jangid.forging_process_management_service.assemblers.RawMaterialAssembler;
import com.jangid.forging_process_management_service.entities.RawMaterial;
import com.jangid.forging_process_management_service.entitiesRepresentation.RawMaterialListRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.RawMaterialRepresentation;
import com.jangid.forging_process_management_service.exception.RawMaterialNotFoundException;
import com.jangid.forging_process_management_service.exception.TenantNotFoundException;
import com.jangid.forging_process_management_service.service.RawMaterialService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Component
public class RawMaterialResource {

  @Autowired
  private final RawMaterialService rawMaterialService;

  @GetMapping("/hello")
  public String getHello() {
    return "Hello, World!";
  }

  @GetMapping("/rawMaterial/{id}")
  public ResponseEntity<RawMaterial> getRawMaterialById(
      @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable("id") String id) {
    Long rawMaterialId = ResourceUtils.convertIdToLong(id)
        .orElseThrow(() -> new RuntimeException("Not valid id!"));

    RawMaterial rawMaterial = rawMaterialService.getRawMaterialById(Long.valueOf(id));
    return ResponseEntity.ok(rawMaterial);
  }

  @GetMapping(value = "tenant/{tenantId}/searchRawMaterials", produces = MediaType.APPLICATION_JSON)
  public ResponseEntity<RawMaterialListRepresentation> searchRawMaterials(
      @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable("tenantId") String tenantId,
      @ApiParam(value = "Identifier of the invoice") @QueryParam("invoiceNumber") String invoiceNumber,
      @ApiParam(value = "Identifier of the Heat") @QueryParam("heatNumber") String heatNumber,
      @ApiParam(value = "Start date") @QueryParam("startDate") String startDate,
      @ApiParam(value = "End date") @QueryParam("endDate") String endDate) {

    try {
      Long tenantIdLongValue = ResourceUtils.convertIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Not valid tenantId!"));
      List<RawMaterial> rawMaterials = new ArrayList<>();
      if (invoiceNumber != null) {
        RawMaterial rawMaterial = rawMaterialService.getRawMaterialByInvoiceNumber(tenantIdLongValue, invoiceNumber);
        rawMaterials.add(rawMaterial);
      } else if (heatNumber != null) {
        rawMaterials = rawMaterialService.getRawMaterialByHeatNumber(tenantIdLongValue, heatNumber);
      } else if (startDate != null && endDate != null){
        rawMaterials = rawMaterialService.getRawMaterialByStartAndEndDate(startDate, endDate, tenantIdLongValue);
      }
      RawMaterialListRepresentation rawMaterialListRepresentation = getRawMaterialListRepresentation(rawMaterials);
      return ResponseEntity.ok(rawMaterialListRepresentation);

    } catch (Exception e) {
      if (e instanceof RawMaterialNotFoundException) {
        return ResponseEntity.notFound().build();
      }
      throw e;
    }

  }

  @GetMapping("tenant/{tenantId}/rawMaterials")
  public ResponseEntity<List<RawMaterialRepresentation>> getAllRawMaterialsByTenantId(
      @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable String tenantId) {
    Long tId = ResourceUtils.convertIdToLong(tenantId)
        .orElseThrow(() -> new TenantNotFoundException(tenantId));

    List<RawMaterialRepresentation> rawMaterials = rawMaterialService.getAllRawMaterialsOfTenant(tId);
    return ResponseEntity.ok(rawMaterials);
  }

  @PostMapping("tenant/{tenantId}/rawMaterial")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<RawMaterialRepresentation> addRawMaterial(@PathVariable String tenantId, @RequestBody RawMaterialRepresentation rawMaterialRepresentation) {
    try {
      if (tenantId == null || tenantId.isEmpty() || rawMaterialRepresentation.getRawMaterialInvoiceNumber() == null ||
          rawMaterialRepresentation.getRawMaterialReceivingDate() == null ||
          rawMaterialRepresentation.getRawMaterialInputCode() == null ||
          rawMaterialRepresentation.getRawMaterialTotalQuantity() == 0 ||
          rawMaterialRepresentation.getRawMaterialHsnCode() == null ||
          rawMaterialRepresentation.getHeats() == null || rawMaterialRepresentation.getHeats().isEmpty()) {
        log.error("invalid input!");
        throw new RuntimeException("invalid input!");
      }
      Long tenantIdLongValue = ResourceUtils.convertIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Not valid id!"));
      RawMaterialRepresentation createdRawMaterial = rawMaterialService.addRawMaterial(tenantIdLongValue, rawMaterialRepresentation);
      return new ResponseEntity<>(createdRawMaterial, HttpStatus.CREATED);
    } catch (Exception exception) {
      return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
    }

  }

  @PutMapping("tenant/{tenantId}/rawMaterial/{rawMaterialId}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<RawMaterialRepresentation> updateRawMaterial(
      @PathVariable("tenantId") String tenantId, @PathVariable("rawMaterialId") String rawMaterialId,
      @RequestBody RawMaterialRepresentation rawMaterialRepresentation) {
    if (tenantId == null || tenantId.isEmpty() || rawMaterialId == null) {
      log.error("invalid input for update!");
      throw new RuntimeException("invalid input for update!");
    }
    Long tenantIdLongValue = ResourceUtils.convertIdToLong(tenantId)
        .orElseThrow(() -> new RuntimeException("Not valid tenant id!"));

    Long rawMaterialIdLongValue = ResourceUtils.convertIdToLong(rawMaterialId)
        .orElseThrow(() -> new RuntimeException("Not valid rawMaterialId!"));

    RawMaterialRepresentation updatedRawMaterial = rawMaterialService.updateRawMaterial(tenantIdLongValue, rawMaterialIdLongValue, rawMaterialRepresentation);
    return ResponseEntity.ok(updatedRawMaterial);
  }

  @DeleteMapping("tenant/{tenantId}/rawMaterial/{rawMaterialId}")
  public ResponseEntity<Void> deleteRawMaterial(@PathVariable("tenantId") String tenantId, @PathVariable("rawMaterialId") String rawMaterialId) {
    if (tenantId == null || tenantId.isEmpty() || rawMaterialId == null) {
      log.error("invalid input for update!");
      throw new RuntimeException("invalid input for update!");
    }
    Long tenantIdLongValue = ResourceUtils.convertIdToLong(tenantId)
        .orElseThrow(() -> new RuntimeException("Not valid tenant id!"));

    Long rawMaterialIdLongValue = ResourceUtils.convertIdToLong(rawMaterialId)
        .orElseThrow(() -> new RuntimeException("Not valid id!"));

    rawMaterialService.deleteRawMaterialByIdAndTenantId(rawMaterialIdLongValue, tenantIdLongValue);
    return ResponseEntity.noContent().build();
  }

  private RawMaterialListRepresentation getRawMaterialListRepresentation(List<RawMaterial> rawMaterials){
    if (rawMaterials == null){
      log.error("RawMaterial list is null!");
      return RawMaterialListRepresentation.builder().build();
    }
    List<RawMaterialRepresentation> rawMaterialRepresentations = new ArrayList<>();
    rawMaterials.forEach(rm -> rawMaterialRepresentations.add(RawMaterialAssembler.dissemble(rm)));
    return RawMaterialListRepresentation.builder()
        .rawMaterials(rawMaterialRepresentations).build();
  }
}
