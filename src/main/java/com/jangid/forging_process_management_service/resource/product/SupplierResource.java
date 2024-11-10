package com.jangid.forging_process_management_service.resource.product;

import com.jangid.forging_process_management_service.entitiesRepresentation.product.SupplierRepresentation;
import com.jangid.forging_process_management_service.exception.TenantNotFoundException;
import com.jangid.forging_process_management_service.service.product.SupplierService;
import com.jangid.forging_process_management_service.utils.ResourceUtils;

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
public class SupplierResource {

  @Autowired
  private SupplierService supplierService;

  @PostMapping("tenant/{tenantId}/supplier")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<SupplierRepresentation> addSupplier(@PathVariable String tenantId, @RequestBody SupplierRepresentation supplierRepresentation) {
    try {
      if (tenantId == null || tenantId.isEmpty() || supplierRepresentation.getSupplierName() == null ||
          supplierRepresentation.getSupplierDetail() == null) {
        log.error("invalid supplier input!");
        throw new RuntimeException("invalid supplier input!");
      }

      Long tenantIdLongValue = ResourceUtils.convertIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Not valid id!"));

      SupplierRepresentation createdSupplier = supplierService.createSupplier(tenantIdLongValue, supplierRepresentation);
      return new ResponseEntity<>(createdSupplier, HttpStatus.CREATED);
    } catch (Exception exception) {
      return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @GetMapping("tenant/{tenantId}/suppliers")
  public ResponseEntity<Page<SupplierRepresentation>> getAllSuppliersOfTenant(
      @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable String tenantId,
      @RequestParam(value = "page", defaultValue = "1") String page,
      @RequestParam(value = "size", defaultValue = "5") String size) {
    Long tId = ResourceUtils.convertIdToLong(tenantId)
        .orElseThrow(() -> new TenantNotFoundException(tenantId));

    int pageNumber = ResourceUtils.convertIdToInt(page)
        .orElseThrow(() -> new RuntimeException("Invalid page="+page));

    int sizeNumber = ResourceUtils.convertIdToInt(size)
        .orElseThrow(() -> new RuntimeException("Invalid size="+size));

    Page<SupplierRepresentation> suppliers = supplierService.getAllSuppliersOfTenant(tId, pageNumber, sizeNumber);
    return ResponseEntity.ok(suppliers);
  }

  @PostMapping("tenant/{tenantId}/supplier/{supplierId}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<SupplierRepresentation> updateSupplier(
      @PathVariable("tenantId") String tenantId, @PathVariable("supplierId") String supplierId,
      @RequestBody SupplierRepresentation supplierRepresentation) {
    if (tenantId == null || tenantId.isEmpty() || supplierId == null) {
      log.error("invalid input for Supplier update!");
      throw new RuntimeException("invalid input for Supplier update!");
    }
    Long tenantIdLongValue = ResourceUtils.convertIdToLong(tenantId)
        .orElseThrow(() -> new RuntimeException("Not valid tenant id!"));

    Long supplierIdLongValue = ResourceUtils.convertIdToLong(supplierId)
        .orElseThrow(() -> new RuntimeException("Not valid supplierId!"));

    SupplierRepresentation updatedSupplier = supplierService.updateSupplier(tenantIdLongValue, supplierIdLongValue, supplierRepresentation);
    return ResponseEntity.ok(updatedSupplier);
  }

  @DeleteMapping("tenant/{tenantId}/supplier/{supplierId}")
  public ResponseEntity<Void> deleteSupplier(@PathVariable("tenantId") String tenantId, @PathVariable("supplierId") String supplierId) {
    if (tenantId == null || tenantId.isEmpty() || supplierId == null) {
      log.error("invalid input for supplier delete!");
      throw new RuntimeException("invalid input for supplier delete!");
    }
    Long tenantIdLongValue = ResourceUtils.convertIdToLong(tenantId)
        .orElseThrow(() -> new RuntimeException("Not valid tenant id!"));

    Long supplierIdLongValue = ResourceUtils.convertIdToLong(supplierId)
        .orElseThrow(() -> new RuntimeException("Not valid supplierId!"));

    supplierService.deleteSupplierByIdAndTenantId(supplierIdLongValue, tenantIdLongValue);
    return ResponseEntity.noContent().build();
  }
}
