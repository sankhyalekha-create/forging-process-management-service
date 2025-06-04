package com.jangid.forging_process_management_service.resource.product;

import com.jangid.forging_process_management_service.entitiesRepresentation.error.ErrorResponse;
import com.jangid.forging_process_management_service.entitiesRepresentation.product.SupplierListRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.product.SupplierRepresentation;
import com.jangid.forging_process_management_service.exception.TenantNotFoundException;
import com.jangid.forging_process_management_service.exception.ValidationException;
import com.jangid.forging_process_management_service.exception.product.SupplierNotFoundException;
import com.jangid.forging_process_management_service.service.product.SupplierService;
import com.jangid.forging_process_management_service.utils.GenericResourceUtils;

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
  public ResponseEntity<?> addSupplier(@PathVariable String tenantId, @RequestBody SupplierRepresentation supplierRepresentation) {
    try {
      if (tenantId == null || tenantId.isEmpty() || supplierRepresentation.getSupplierName() == null ||
          supplierRepresentation.getSupplierDetail() == null) {
        log.error("invalid supplier input!");
        throw new RuntimeException("invalid supplier input!");
      }

      Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Not valid id!"));

      SupplierRepresentation createdSupplier = supplierService.createSupplier(tenantIdLongValue, supplierRepresentation);
      return new ResponseEntity<>(createdSupplier, HttpStatus.CREATED);
    } catch (Exception exception) {
      if (exception instanceof IllegalStateException) {
        // Generate a more descriptive error message
        String errorMessage = exception.getMessage();
        log.error("Supplier creation failed: {}", errorMessage);
        
        if (errorMessage.contains("with name=")) {
          return new ResponseEntity<>(
              new ErrorResponse("A supplier with the name '" + supplierRepresentation.getSupplierName() + "' already exists for this tenant"),
              HttpStatus.CONFLICT);
        } else if (errorMessage.contains("with PAN=")) {
          return new ResponseEntity<>(
              new ErrorResponse("A supplier with the PAN '" + supplierRepresentation.getPanNumber() + "' already exists for this tenant"),
              HttpStatus.CONFLICT);
        } else if (errorMessage.contains("with GSTIN=")) {
          return new ResponseEntity<>(
              new ErrorResponse("A supplier with the GSTIN '" + supplierRepresentation.getGstinNumber() + "' already exists for this tenant"),
              HttpStatus.CONFLICT);
        } else {
          return new ResponseEntity<>(
              new ErrorResponse("A supplier with the same details already exists"),
              HttpStatus.CONFLICT);
        }
      } else if (exception instanceof ValidationException) {
        log.error("Validation error: {}", exception.getMessage());
        return new ResponseEntity<>(
            new ErrorResponse(exception.getMessage()),
            HttpStatus.BAD_REQUEST);
      } else if (exception instanceof IllegalArgumentException) {
        log.error("Invalid supplier data: {}", exception.getMessage());
        return new ResponseEntity<>(
            new ErrorResponse(exception.getMessage()),
            HttpStatus.BAD_REQUEST);
      }
      
      log.error("Error creating supplier: {}", exception.getMessage());
      return new ResponseEntity<>(new ErrorResponse("Error creating supplier: " + exception.getMessage()),
                                  HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @GetMapping("tenant/{tenantId}/suppliers")
  public ResponseEntity<?> getAllSuppliersOfTenant(
      @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable String tenantId,
      @RequestParam(value = "page") String page,
      @RequestParam(value = "size") String size) {
    Long tId = GenericResourceUtils.convertResourceIdToLong(tenantId)
        .orElseThrow(() -> new TenantNotFoundException(tenantId));

    Integer pageNumber = (page == null || page.isBlank()) ? -1
                                                          : GenericResourceUtils.convertResourceIdToInt(page)
                             .orElseThrow(() -> new RuntimeException("Invalid page=" + page));

    Integer sizeNumber = (size == null || size.isBlank()) ? -1
                                                          : GenericResourceUtils.convertResourceIdToInt(size)
                             .orElseThrow(() -> new RuntimeException("Invalid size=" + size));

    if (pageNumber == -1 || sizeNumber == -1) {
      SupplierListRepresentation supplierListRepresentation = supplierService.getAllSuppliersOfTenantWithoutPagination(tId);
      return ResponseEntity.ok(supplierListRepresentation); // Returning list instead of paged response
    }

    Page<SupplierRepresentation> suppliers = supplierService.getAllSuppliersOfTenant(tId, pageNumber, sizeNumber);
    return ResponseEntity.ok(suppliers);
  }

  @GetMapping("tenant/{tenantId}/supplier/{supplierId}")
  public ResponseEntity<SupplierRepresentation> getSupplierOfTenant(
      @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable String tenantId,
  @ApiParam(value = "Identifier of the supplier", required = true) @PathVariable String supplierId) {
    try {
      Long tId = GenericResourceUtils.convertResourceIdToLong(tenantId)
          .orElseThrow(() -> new TenantNotFoundException(tenantId));

      Long sId = GenericResourceUtils.convertResourceIdToLong(supplierId)
          .orElseThrow(() -> new SupplierNotFoundException("Supplier not found. supplierId="+supplierId));

      SupplierRepresentation supplier = supplierService.getSupplierOfTenant(tId, sId);
      return ResponseEntity.ok(supplier);
    } catch (Exception e) {
      if(e instanceof SupplierNotFoundException){
        return ResponseEntity.ok(SupplierRepresentation.builder().build());
      }
      throw e;
    }
  }

  @GetMapping(value = "tenant/{tenantId}/searchSuppliers", produces = MediaType.APPLICATION_JSON)
  public ResponseEntity<SupplierListRepresentation> searchSuppliers(
      @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable("tenantId") String tenantId,
      @ApiParam(value = "Supplier name to search for", required = true) @RequestParam("supplierName") String supplierName) {

    try {
      Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Not valid tenantId!"));
      
      if (supplierName == null || supplierName.trim().isEmpty()) {
        return ResponseEntity.badRequest().build();
      }

      SupplierListRepresentation searchResults = supplierService.searchSuppliersByName(tenantIdLongValue, supplierName.trim());
      return ResponseEntity.ok(searchResults);

    } catch (Exception e) {
      log.error("Error during supplier search: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
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
    Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
        .orElseThrow(() -> new RuntimeException("Not valid tenant id!"));

    Long supplierIdLongValue = GenericResourceUtils.convertResourceIdToLong(supplierId)
        .orElseThrow(() -> new RuntimeException("Not valid supplierId!"));

    SupplierRepresentation updatedSupplier = supplierService.updateSupplier(tenantIdLongValue, supplierIdLongValue, supplierRepresentation);
    return ResponseEntity.ok(updatedSupplier);
  }

  @DeleteMapping("tenant/{tenantId}/supplier/{supplierId}")
  public ResponseEntity<?> deleteSupplier(@PathVariable("tenantId") String tenantId, @PathVariable("supplierId") String supplierId) {
    try{
      if (tenantId == null || tenantId.isEmpty() || supplierId == null) {
        log.error("invalid input for supplier delete!");
        throw new RuntimeException("invalid input for supplier delete!");
      }
      Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Not valid tenant id!"));

      Long supplierIdLongValue = GenericResourceUtils.convertResourceIdToLong(supplierId)
          .orElseThrow(() -> new RuntimeException("Not valid supplierId!"));

      supplierService.deleteSupplier(tenantIdLongValue, supplierIdLongValue);
      return ResponseEntity.noContent().build();
    } catch (Exception exception) {
      if (exception instanceof SupplierNotFoundException) {
        return ResponseEntity.notFound().build();
      }
      if (exception instanceof IllegalStateException) {
        log.error("Error while deleting supplier: {}", exception.getMessage());
        return new ResponseEntity<>(new ErrorResponse(exception.getMessage()),
                                    HttpStatus.CONFLICT);
      }
      log.error("Error while deleting product: {}", exception.getMessage());
      return new ResponseEntity<>(new ErrorResponse("Error while deleting supplier"),
                                  HttpStatus.INTERNAL_SERVER_ERROR);
    }

  }
}
