package com.jangid.forging_process_management_service.resource.product;

import com.jangid.forging_process_management_service.configuration.security.TenantContextHolder;
import com.jangid.forging_process_management_service.entitiesRepresentation.product.SupplierListRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.product.SupplierRepresentation;
import com.jangid.forging_process_management_service.exception.product.SupplierNotFoundException;
import com.jangid.forging_process_management_service.service.product.SupplierService;
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

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Component
public class SupplierResource {

  @Autowired
  private SupplierService supplierService;

  @PostMapping("supplier")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<?> addSupplier(@RequestBody SupplierRepresentation supplierRepresentation) {
    try {
      if (supplierRepresentation.getSupplierName() == null ||
          supplierRepresentation.getSupplierDetail() == null) {
        log.error("invalid supplier input!");
        throw new RuntimeException("invalid supplier input!");
      }

      // Get tenant ID from authenticated context
      Long tenantId = TenantContextHolder.getAuthenticatedTenantId();

      SupplierRepresentation createdSupplier = supplierService.createSupplier(tenantId, supplierRepresentation);
      return new ResponseEntity<>(createdSupplier, HttpStatus.CREATED);
    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "addSupplier");
    }
  }

  @GetMapping("suppliers")
  public ResponseEntity<?> getAllSuppliersOfTenant(
      @RequestParam(value = "page") String page,
      @RequestParam(value = "size") String size) {
    try {
      // Get tenant ID from authenticated context
      Long tenantId = TenantContextHolder.getAuthenticatedTenantId();

      Integer pageNumber = (page == null || page.isBlank()) ? -1
        : GenericResourceUtils.convertResourceIdToInt(page)
        .orElseThrow(() -> new RuntimeException("Invalid page=" + page));

      Integer sizeNumber = (size == null || size.isBlank()) ? -1
        : GenericResourceUtils.convertResourceIdToInt(size)
        .orElseThrow(() -> new RuntimeException("Invalid size=" + size));

      if (pageNumber == -1 || sizeNumber == -1) {
        SupplierListRepresentation supplierListRepresentation = supplierService.getAllSuppliersOfTenantWithoutPagination(tenantId);
        return ResponseEntity.ok(supplierListRepresentation);
      }

      Page<SupplierRepresentation> suppliers = supplierService.getAllSuppliersOfTenant(tenantId, pageNumber, sizeNumber);
      return ResponseEntity.ok(suppliers);
    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "getAllSuppliersOfTenant");
    }
  }

  @GetMapping("supplier/{supplierId}")
  public ResponseEntity<?> getSupplierOfTenant(
      @ApiParam(value = "Identifier of the supplier", required = true) @PathVariable String supplierId) {
    try {
      // Get tenant ID from authenticated context
      Long tenantId = TenantContextHolder.getAuthenticatedTenantId();

      Long supplierIdLong = GenericResourceUtils.convertResourceIdToLong(supplierId)
        .orElseThrow(() -> new SupplierNotFoundException("Supplier not found. supplierId=" + supplierId));

      SupplierRepresentation supplier = supplierService.getSupplierOfTenant(tenantId, supplierIdLong);
      return ResponseEntity.ok(supplier);
    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "getSupplierOfTenant");
    }
  }

  @GetMapping(value = "searchSuppliers", produces = MediaType.APPLICATION_JSON)
  public ResponseEntity<?> searchSuppliers(
      @ApiParam(value = "Supplier name to search for", required = true) @RequestParam("supplierName") String supplierName,
      @ApiParam(value = "Page number (0-based)", required = false) @RequestParam(value = "page", defaultValue = "0") String pageParam,
      @ApiParam(value = "Page size", required = false) @RequestParam(value = "size", defaultValue = "10") String sizeParam) {

    try {
      // Get tenant ID from authenticated context
      Long tenantId = TenantContextHolder.getAuthenticatedTenantId();
      
      if (supplierName == null || supplierName.trim().isEmpty()) {
        throw new IllegalArgumentException("Supplier name is required");
      }

      int pageNumber = GenericResourceUtils.convertResourceIdToInt(pageParam)
        .orElseThrow(() -> new RuntimeException("Invalid page=" + pageParam));

      int pageSize = GenericResourceUtils.convertResourceIdToInt(sizeParam)
        .orElseThrow(() -> new RuntimeException("Invalid size=" + sizeParam));

      if (pageNumber < 0) {
        pageNumber = 0;
      }

      if (pageSize <= 0) {
        pageSize = 10;
      }

      Page<SupplierRepresentation> searchResults = supplierService.searchSuppliersByNameWithPagination(tenantId, supplierName.trim(), pageNumber, pageSize);
      return ResponseEntity.ok(searchResults);

    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "searchSuppliers");
    }
  }

  @GetMapping("supplier/{supplierId}/usage")
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<?> isSupplierUsedInProducts(
      @ApiParam(value = "Identifier of the supplier", required = true) @PathVariable String supplierId) {
    try {
      // Get tenant ID from authenticated context
      Long tenantId = TenantContextHolder.getAuthenticatedTenantId();

      Long supplierIdLong = GenericResourceUtils.convertResourceIdToLong(supplierId)
        .orElseThrow(() -> new SupplierNotFoundException("Supplier not found. supplierId=" + supplierId));

      boolean isUsed = supplierService.isSupplierUsedInProducts(tenantId, supplierIdLong);
      return ResponseEntity.ok(Map.of("isUsedInProducts", isUsed));
    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "isSupplierUsedInProducts");
    }
  }

  @PostMapping("supplier/{supplierId}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<?> updateSupplier(
      @PathVariable("supplierId") String supplierId,
      @RequestBody SupplierRepresentation supplierRepresentation) {
    try {
      if (supplierId == null) {
        log.error("invalid input for Supplier update!");
        throw new RuntimeException("invalid input for Supplier update!");
      }

      // Get tenant ID from authenticated context
      Long tenantId = TenantContextHolder.getAuthenticatedTenantId();

      Long supplierIdLongValue = GenericResourceUtils.convertResourceIdToLong(supplierId)
        .orElseThrow(() -> new RuntimeException("Not valid supplierId!"));

      SupplierRepresentation updatedSupplier = supplierService.updateSupplier(tenantId, supplierIdLongValue, supplierRepresentation);
      return ResponseEntity.ok(updatedSupplier);
    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "updateSupplier");
    }
  }

  @DeleteMapping("supplier/{supplierId}")
  public ResponseEntity<?> deleteSupplier(@PathVariable("supplierId") String supplierId) {
    try {
      if (supplierId == null) {
        log.error("invalid input for supplier delete!");
        throw new RuntimeException("invalid input for supplier delete!");
      }

      // Get tenant ID from authenticated context
      Long tenantId = TenantContextHolder.getAuthenticatedTenantId();

      Long supplierIdLongValue = GenericResourceUtils.convertResourceIdToLong(supplierId)
        .orElseThrow(() -> new RuntimeException("Not valid supplierId!"));

      supplierService.deleteSupplier(tenantId, supplierIdLongValue);
      return ResponseEntity.noContent().build();
    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "deleteSupplier");
    }
  }
}
