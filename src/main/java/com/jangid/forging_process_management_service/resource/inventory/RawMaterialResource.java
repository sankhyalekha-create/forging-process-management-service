package com.jangid.forging_process_management_service.resource.inventory;

import com.jangid.forging_process_management_service.configuration.security.TenantContextHolder;
import com.jangid.forging_process_management_service.entities.inventory.Heat;
import com.jangid.forging_process_management_service.entitiesRepresentation.inventory.HeatRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.inventory.HeatListRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.inventory.RawMaterialProductRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.inventory.RawMaterialRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.inventory.SearchResultsRepresentation;
import com.jangid.forging_process_management_service.service.inventory.RawMaterialHeatService;
import com.jangid.forging_process_management_service.service.inventory.RawMaterialService;
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

  @Autowired
  private final RawMaterialHeatService rawMaterialHeatService;

  @GetMapping("/hello")
  public String getHello() {
    return "Hello, World!";
  }

  @GetMapping("rawMaterial/{id}")
  public ResponseEntity<?> getTenantRawMaterialById(
      @ApiParam(value = "Identifier of the rawMaterial", required = true) @PathVariable("id") String id
  ) {
    try {
      Long tenantIdLongValue = TenantContextHolder.getAuthenticatedTenantId();
      Long rawMaterialId = GenericResourceUtils.convertResourceIdToLong(id)
          .orElseThrow(() -> new RuntimeException("Not valid id!"));

      RawMaterialRepresentation rawMaterialRepresentation = rawMaterialService.getTenantRawMaterialById(tenantIdLongValue, rawMaterialId);
      return ResponseEntity.ok(rawMaterialRepresentation);
    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "getTenantRawMaterialById");
    }
  }

  @GetMapping("rawMaterial/{rawMaterialId}/check-editability")
  public ResponseEntity<?> checkRawMaterialEditability(
      @ApiParam(value = "Identifier of the raw material", required = true) @PathVariable("rawMaterialId") String rawMaterialId
  ) {
    try {
      Long tenantIdLongValue = TenantContextHolder.getAuthenticatedTenantId();
      Long rawMaterialIdLongValue = GenericResourceUtils.convertResourceIdToLong(rawMaterialId)
          .orElseThrow(() -> new RuntimeException("Not valid rawMaterialId!"));

      boolean isFullyEditable = rawMaterialService.isRawMaterialFullyEditable(rawMaterialIdLongValue, tenantIdLongValue);
      
      return ResponseEntity.ok(new EditabilityResponse(isFullyEditable));
    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "checkRawMaterialEditability");
    }
  }

  @GetMapping(value = "searchRawMaterials", produces = MediaType.APPLICATION_JSON)
  public ResponseEntity<?> searchRawMaterials(
      @ApiParam(value = "Identifier of the invoice") @QueryParam("invoiceNumber") String invoiceNumber,
      @ApiParam(value = "Identifier of the Heat") @QueryParam("heatNumber") String heatNumber,
      @ApiParam(value = "Start date") @QueryParam("startDate") String startDate,
      @ApiParam(value = "End date") @QueryParam("endDate") String endDate,
      @ApiParam(value = "Page number (0-based)", required = false) @QueryParam(value = "page") String page,
      @ApiParam(value = "Page size", required = false) @QueryParam(value = "size") String size) {

    try {
      Long tenantIdLongValue = TenantContextHolder.getAuthenticatedTenantId();

      int pageNumber = (page == null || page.trim().isEmpty()) ? 0 :
                       GenericResourceUtils.convertResourceIdToInt(page)
                           .orElseThrow(() -> new RuntimeException("Invalid page=" + page));

      int pageSize = (size == null || size.trim().isEmpty()) ? 10 :
                     GenericResourceUtils.convertResourceIdToInt(size)
                         .orElseThrow(() -> new RuntimeException("Invalid size=" + size));

      if (pageNumber < 0) {
        pageNumber = 0;
      }

      if (pageSize <= 0) {
        pageSize = 10; // Default page size
      }

      Page<RawMaterialRepresentation> rawMaterialsPage = rawMaterialService.searchRawMaterials(tenantIdLongValue, invoiceNumber, heatNumber, startDate, endDate, pageNumber, pageSize);
      return ResponseEntity.ok(rawMaterialsPage);

    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "getAllRawMaterialOfTenant");
    }

  }

  @GetMapping(value = "availableRawMaterialHeats", produces = MediaType.APPLICATION_JSON)
  public ResponseEntity<?> getAvailableRawMaterialHeatListOfTenant(
      ) {

    try {
      Long tenantIdLongValue = TenantContextHolder.getAuthenticatedTenantId();
      List<Heat> heats = rawMaterialService.getAvailableRawMaterialByTenantId(tenantIdLongValue);

      HeatListRepresentation heatListRepresentation = rawMaterialHeatService.getRawMaterialHeatListRepresentation(heats);
      return ResponseEntity.ok(heatListRepresentation);

    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "getAvailableRawMaterialHeatListOfTenant");
    }

  }

  @GetMapping(value = "searchProductsAndHeats", produces = MediaType.APPLICATION_JSON)
  public ResponseEntity<?> searchProductsAndHeats(
      @ApiParam(value = "Type of search", required = true, allowableValues = "PRODUCT_NAME,PRODUCT_CODE,HEAT_NUMBER") @QueryParam("searchType") String searchType,
      @ApiParam(value = "Search term", required = true) @QueryParam("searchTerm") String searchTerm,
      @ApiParam(value = "Page number (0-based)", required = false) @QueryParam(value = "page") String page,
      @ApiParam(value = "Page size", required = false) @QueryParam(value = "size") String size) {

    try {
      Long tenantIdLongValue = TenantContextHolder.getAuthenticatedTenantId();

      if (searchType == null || searchType.trim().isEmpty()) {
        return ResponseEntity.badRequest().build();
      }

      if (searchTerm == null || searchTerm.trim().isEmpty()) {
        return ResponseEntity.badRequest().build();
      }

      int pageNumber = (page == null || page.trim().isEmpty()) ? 0 :
                       GenericResourceUtils.convertResourceIdToInt(page)
                           .orElseThrow(() -> new RuntimeException("Invalid page=" + page));

      int pageSize = (size == null || size.trim().isEmpty()) ? 10 :
                     GenericResourceUtils.convertResourceIdToInt(size)
                         .orElseThrow(() -> new RuntimeException("Invalid size=" + size));

      if (pageNumber < 0) {
        pageNumber = 0;
      }

      if (pageSize <= 0) {
        pageSize = 10; // Default page size
      }

      Page<SearchResultsRepresentation> searchResults = rawMaterialService.searchProductsAndHeats(tenantIdLongValue, searchType.trim(), searchTerm.trim(), pageNumber, pageSize);
      return ResponseEntity.ok(searchResults);

    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "searchProductsAndHeats");
    }
  }

  @GetMapping("rawMaterials")
  public ResponseEntity<?> getAllRawMaterialsByTenantId(
      @RequestParam(value = "page", defaultValue = "0") String page,
      @RequestParam(value = "size", defaultValue = "5") String size,
      @ApiParam(value = "Include products and heats in the response. When true, uses optimized query to avoid N+1 database queries. Default is true.",
                defaultValue = "true")
      @RequestParam(value = "includeProductsAndHeats", defaultValue = "true") boolean includeProductsAndHeats) {
    try {
      Long tId = TenantContextHolder.getAuthenticatedTenantId();

      int pageNumber = GenericResourceUtils.convertResourceIdToInt(page)
          .orElseThrow(() -> new RuntimeException("Invalid page=" + page));

      int sizeNumber = GenericResourceUtils.convertResourceIdToInt(size)
          .orElseThrow(() -> new RuntimeException("Invalid size=" + size));

      Page<RawMaterialRepresentation> rawMaterials;
      if (includeProductsAndHeats) {
        // Use optimized method that eagerly loads products and heats to avoid N+1 queries
        rawMaterials = rawMaterialService.getAllRawMaterialsOfTenantWithProductsAndHeats(tId, pageNumber, sizeNumber);
      } else {
        // Use regular method for backward compatibility
        rawMaterials = rawMaterialService.getAllRawMaterialsOfTenant(tId, pageNumber, sizeNumber);
      }

      return ResponseEntity.ok(rawMaterials);
    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "getAllRawMaterialsByTenantId");
    }
  }

  @PostMapping("rawMaterial")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<?> addRawMaterial(@RequestBody RawMaterialRepresentation rawMaterialRepresentation) {
    try {
      if (isInValidRawMaterialRepresentation(rawMaterialRepresentation)) {
        log.error("invalid input!");
        throw new RuntimeException("invalid input!");
      }
      Long tenantIdLongValue = TenantContextHolder.getAuthenticatedTenantId();;
      RawMaterialRepresentation createdRawMaterial = rawMaterialService.addRawMaterial(tenantIdLongValue, rawMaterialRepresentation);
      return new ResponseEntity<>(createdRawMaterial, HttpStatus.CREATED);
    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "addRawMaterial");
    }
  }

  @PostMapping("rawMaterial/{rawMaterialId}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<?> updateRawMaterial(
      @PathVariable("rawMaterialId") String rawMaterialId,
      @RequestBody RawMaterialRepresentation rawMaterialRepresentation) {
    try {
      if (rawMaterialId == null || isInValidRawMaterialRepresentation(rawMaterialRepresentation)) {
        log.error("invalid input for update!");
        throw new RuntimeException("invalid input for update!");
      }
      Long tenantIdLongValue = TenantContextHolder.getAuthenticatedTenantId();

      Long rawMaterialIdLongValue = GenericResourceUtils.convertResourceIdToLong(rawMaterialId)
          .orElseThrow(() -> new RuntimeException("Not valid rawMaterialId!"));

      RawMaterialRepresentation updatedRawMaterial = rawMaterialService.updateRawMaterial(tenantIdLongValue, rawMaterialIdLongValue, rawMaterialRepresentation);
      return ResponseEntity.ok(updatedRawMaterial);
    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "updateRawMaterial");
    }
  }

  @DeleteMapping("rawMaterial/{rawMaterialId}")
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<?> deleteRawMaterial(
      @ApiParam(value = "Identifier of the raw material", required = true) @PathVariable String rawMaterialId) {

    try {
      Long tenantIdLongValue = TenantContextHolder.getAuthenticatedTenantId();
      Long rawMaterialIdLongValue = GenericResourceUtils.convertResourceIdToLong(rawMaterialId)
          .orElseThrow(() -> new RuntimeException("Not valid rawMaterialId!"));

      rawMaterialService.deleteRawMaterial(rawMaterialIdLongValue, tenantIdLongValue);
      return ResponseEntity.ok().build();

    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "deleteRawMaterial");
    }
  }

  private boolean isInValidRawMaterialRepresentation(RawMaterialRepresentation rawMaterialRepresentation) {
    if (rawMaterialRepresentation.getRawMaterialInvoiceNumber() == null ||
        rawMaterialRepresentation.getRawMaterialReceivingDate() == null ||
        rawMaterialRepresentation.getRawMaterialInvoiceDate() == null ||
        rawMaterialRepresentation.getPoNumber() == null ||
        rawMaterialRepresentation.getSupplier() == null ||
        rawMaterialRepresentation.getSupplier().getId() == null ||
        rawMaterialRepresentation.getUnitOfMeasurement() == null ||
        rawMaterialRepresentation.getRawMaterialHsnCode() == null ||
        rawMaterialRepresentation.getRawMaterialProducts() == null ||
        rawMaterialRepresentation.getRawMaterialProducts().isEmpty()) {
      return true;
    }

    // Validate quantities based on unit of measurement
    if (rawMaterialRepresentation.getUnitOfMeasurement().equals("KGS")) {
      if (rawMaterialRepresentation.getRawMaterialTotalQuantity() == null) {
        return true;
      }
    } else if (rawMaterialRepresentation.getUnitOfMeasurement().equals("PIECES")) {
      if (rawMaterialRepresentation.getRawMaterialTotalPieces() == null) {
        return true;
      }
    } else {
      return true; // Invalid unit of measurement
    }

    for (RawMaterialProductRepresentation product : rawMaterialRepresentation.getRawMaterialProducts()) {
      if (product.getProduct() == null ||
          product.getHeats() == null ||
          product.getHeats().isEmpty()) {
        return true;
      }

      // Validate each heat in the heats list
      for (HeatRepresentation heat : product.getHeats()) {
        if (heat.getHeatNumber() == null ||
            heat.getTestCertificateNumber() == null ||
            heat.getLocation() == null ||
            heat.getIsInPieces() == null) {
          return true;
        }

        // Validate heat quantities based on isInPieces flag
        if (heat.getIsInPieces()) {
          if (heat.getPiecesCount() == null || heat.getAvailablePiecesCount() == null) {
            return true;
          }
        } else {
          if (heat.getHeatQuantity() == null || heat.getAvailableHeatQuantity() == null) {
            return true;
          }
        }
      }
    }
    return false;
  }

  /**
   * Response class for editability check
   */
  private static class EditabilityResponse {
    private final boolean isFullyEditable;

    public EditabilityResponse(boolean isFullyEditable) {
      this.isFullyEditable = isFullyEditable;
    }

    public boolean isFullyEditable() {
      return isFullyEditable;
    }
  }
}
