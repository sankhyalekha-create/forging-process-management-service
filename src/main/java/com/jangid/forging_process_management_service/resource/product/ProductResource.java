package com.jangid.forging_process_management_service.resource.product;

import com.jangid.forging_process_management_service.entitiesRepresentation.error.ErrorResponse;
import com.jangid.forging_process_management_service.entitiesRepresentation.product.ProductListRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.product.ProductRepresentation;
import com.jangid.forging_process_management_service.exception.product.ProductNotFoundException;
import com.jangid.forging_process_management_service.service.product.ProductService;
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
public class ProductResource {

  @Autowired
  private ProductService productService;

  @PostMapping("tenant/{tenantId}/product")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<ProductRepresentation> addProduct(@PathVariable String tenantId, @RequestBody ProductRepresentation productRepresentation) {
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
      return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @GetMapping("tenant/{tenantId}/products")
  public ResponseEntity<?> getAllProductsOfTenant(
      @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable String tenantId,
      @RequestParam(value = "page") String page,
      @RequestParam(value = "size") String size) {
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
  }

  @GetMapping("tenant/{tenantId}/supplier/{supplierId}/products")
  public ResponseEntity<ProductListRepresentation> getAllProductsOfSupplier(
      @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable String tenantId,
      @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable String supplierId) {
    Long tId = GenericResourceUtils.convertResourceIdToLong(tenantId)
        .orElseThrow(() -> new RuntimeException("Invalid tenantId input=" + tenantId));

    Long sId = GenericResourceUtils.convertResourceIdToLong(supplierId)
        .orElseThrow(() -> new RuntimeException("Invalid supplierId input=" + supplierId));

    ProductListRepresentation productRepresentations = productService.getAllProductRepresentationsOfSupplier(tId, sId);
    return ResponseEntity.ok(productRepresentations);
  }

  @PostMapping("tenant/{tenantId}/product/{productId}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntity<ProductRepresentation> updateProduct(
      @PathVariable("tenantId") String tenantId, @PathVariable("productId") String productId,
      @RequestBody ProductRepresentation productRepresentation) {
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
      if (exception instanceof ProductNotFoundException) {
        return ResponseEntity.notFound().build();
      }
      if (exception instanceof IllegalStateException) {
        log.error("Error while deleting product: {}", exception.getMessage());
        return new ResponseEntity<>(new ErrorResponse(exception.getMessage()),
                                    HttpStatus.CONFLICT);
      }
      log.error("Error while deleting product: {}", exception.getMessage());
      return new ResponseEntity<>(new ErrorResponse("Error while deleting product"),
                                  HttpStatus.INTERNAL_SERVER_ERROR);
    }

  }


}
