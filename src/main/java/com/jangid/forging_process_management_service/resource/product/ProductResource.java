package com.jangid.forging_process_management_service.resource.product;

import com.jangid.forging_process_management_service.entitiesRepresentation.product.ProductListRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.product.ProductRepresentation;
import com.jangid.forging_process_management_service.service.product.ProductService;
import com.jangid.forging_process_management_service.utils.GenericResourceUtils;
import com.jangid.forging_process_management_service.utils.GenericExceptionHandler;

import com.jangid.forging_process_management_service.dto.ProductWithHeatsDTO;

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
public class ProductResource {

  @Autowired
  private ProductService productService;

  @PostMapping("tenant/{tenantId}/product")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<?> addProduct(@PathVariable String tenantId, @RequestBody ProductRepresentation productRepresentation) {
    try {
      if (tenantId == null || tenantId.isEmpty() || productRepresentation.getProductName() == null ||
          productRepresentation.getProductCode() == null ||
          productRepresentation.getUnitOfMeasurement() == null || productRepresentation.getSuppliers() == null || productRepresentation.getSuppliers().isEmpty()) {
        log.error("invalid product input!");
        throw new RuntimeException("invalid product input!");
      }

      Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Not valid id!"));

      ProductRepresentation createdProduct = productService.createProduct(tenantIdLongValue, productRepresentation);
      return new ResponseEntity<>(createdProduct, HttpStatus.CREATED);
    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "addProduct");
    }
  }

  @GetMapping("tenant/{tenantId}/products")
  public ResponseEntity<?> getAllProductsOfTenant(
      @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable String tenantId,
      @RequestParam(value = "page") String page,
      @RequestParam(value = "size") String size) {
    try {
      Long tId = GenericResourceUtils.convertResourceIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Invalid tenantId input=" + tenantId));

      Integer pageNumber = (page == null || page.isBlank()) ? -1
                                                            : GenericResourceUtils.convertResourceIdToInt(page)
                               .orElseThrow(() -> new RuntimeException("Invalid page=" + page));

      Integer sizeNumber = (size == null || size.isBlank()) ? -1
                                                            : GenericResourceUtils.convertResourceIdToInt(size)
                               .orElseThrow(() -> new RuntimeException("Invalid size=" + size));

      if (pageNumber == -1 || sizeNumber == -1) {
        ProductListRepresentation productListRepresentation = productService.getAllDistinctProductsOfTenantWithoutPagination(tId);
        return ResponseEntity.ok(productListRepresentation); // Returning list instead of paged response
      }

      Page<ProductRepresentation> products = productService.getAllProductsOfTenant(tId, pageNumber, sizeNumber);
      return ResponseEntity.ok(products);
    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "getAllProductsOfTenant");
    }
  }

  @GetMapping("tenant/{tenantId}/supplier/{supplierId}/products")
  public ResponseEntity<?> getAllProductsOfSupplier(
      @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable String tenantId,
      @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable String supplierId) {
    try {
      Long tId = GenericResourceUtils.convertResourceIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Invalid tenantId input=" + tenantId));

      Long sId = GenericResourceUtils.convertResourceIdToLong(supplierId)
          .orElseThrow(() -> new RuntimeException("Invalid supplierId input=" + supplierId));

      ProductListRepresentation productRepresentations = productService.getAllProductRepresentationsOfSupplier(tId, sId);
      return ResponseEntity.ok(productRepresentations);
    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "getAllProductsOfSupplier");
    }
  }

  @PostMapping("tenant/{tenantId}/product/{productId}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<?> updateProduct(
      @PathVariable("tenantId") String tenantId, @PathVariable("productId") String productId,
      @RequestBody ProductRepresentation productRepresentation) {
    try {
      if (tenantId == null || tenantId.isEmpty() || productId == null) {
        log.error("invalid input for Product update!");
        throw new RuntimeException("invalid input for Product update!");
      }
      Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Not valid tenant id!"));

      Long productIdLongValue = GenericResourceUtils.convertResourceIdToLong(productId)
          .orElseThrow(() -> new RuntimeException("Not valid productId!"));

      ProductRepresentation updatedProduct = productService.updateProduct(tenantIdLongValue, productIdLongValue, productRepresentation);
      return ResponseEntity.ok(updatedProduct);
    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "updateProduct");
    }
  }

  @DeleteMapping("tenant/{tenantId}/product/{productId}")
  public ResponseEntity<?> deleteProduct(@PathVariable("tenantId") String tenantId, @PathVariable("productId") String productId) {
    try {
      if (tenantId == null || tenantId.isEmpty() || productId == null) {
        log.error("invalid input for product delete!");
        throw new RuntimeException("invalid input for product delete!");
      }
      Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Not valid tenant id!"));

      Long productIdLongValue = GenericResourceUtils.convertResourceIdToLong(productId)
          .orElseThrow(() -> new RuntimeException("Not valid productId!"));

      productService.deleteProduct(productIdLongValue, tenantIdLongValue);
      return ResponseEntity.noContent().build();
    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "deleteProduct");
    }

  }

  // Endpoint to get products with their associated heats (paginated)
  @GetMapping("tenant/{tenantId}/products-with-heats")
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<?> getProductsWithHeats(
      @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable String tenantId,
      @ApiParam(value = "Page number (0-indexed)", required = true) @RequestParam int page,
      // Size parameter is optional, service defaults to 5 if not provided or invalid
      @ApiParam(value = "Number of items per page (defaults to 5)") @RequestParam(required = false, defaultValue = "5") int size) {
    try {
      Long tId = GenericResourceUtils.convertResourceIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Invalid tenantId input=" + tenantId));

      if (page < 0) {
         log.error("Invalid page number requested: {}", page);
         throw new IllegalArgumentException("Page number cannot be negative.");
      }

      Page<ProductWithHeatsDTO> productsWithHeatsPage = productService.findProductsWithHeats(tId, page, size);
      return ResponseEntity.ok(productsWithHeatsPage);
    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "getProductsWithHeats");
    }
  }

  @GetMapping(value = "tenant/{tenantId}/searchProducts", produces = MediaType.APPLICATION_JSON)
  public ResponseEntity<?> searchProducts(
      @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable("tenantId") String tenantId,
      @ApiParam(value = "Type of search", required = true, allowableValues = "PRODUCT_NAME,PRODUCT_CODE") @RequestParam("searchType") String searchType,
      @ApiParam(value = "Search term", required = true) @RequestParam("searchTerm") String searchTerm,
      @ApiParam(value = "Page number (0-based)", required = false) @RequestParam(value = "page", defaultValue = "0") String pageParam,
      @ApiParam(value = "Page size", required = false) @RequestParam(value = "size", defaultValue = "10") String sizeParam) {

    try {
      Long tenantIdLongValue = GenericResourceUtils.convertResourceIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Not valid tenantId!"));
      
      if (searchType == null || searchType.trim().isEmpty()) {
        throw new IllegalArgumentException("Search type is required");
      }
      
      if (searchTerm == null || searchTerm.trim().isEmpty()) {
        throw new IllegalArgumentException("Search term is required");
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

      Page<ProductRepresentation> searchResults = productService.searchProducts(tenantIdLongValue, searchType.trim(), searchTerm.trim(), pageNumber, pageSize);
      return ResponseEntity.ok(searchResults);

    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "searchProducts");
    }
  }
}
