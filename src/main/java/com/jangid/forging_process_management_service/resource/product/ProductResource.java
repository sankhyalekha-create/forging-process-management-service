package com.jangid.forging_process_management_service.resource.product;

import com.jangid.forging_process_management_service.entitiesRepresentation.product.ProductListRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.product.ProductRepresentation;
import com.jangid.forging_process_management_service.service.product.ProductService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
          productRepresentation.getProductCode() == null || productRepresentation.getProductSku() == null ||
          productRepresentation.getUnitOfMeasurement() == null || productRepresentation.getSuppliers() ==null || productRepresentation.getSuppliers().isEmpty()) {
        log.error("invalid product input!");
        throw new RuntimeException("invalid product input!");
      }

      Long tenantIdLongValue = ResourceUtils.convertIdToLong(tenantId)
          .orElseThrow(() -> new RuntimeException("Not valid id!"));

      ProductRepresentation createdProduct = productService.createProduct(tenantIdLongValue, productRepresentation);
      return new ResponseEntity<>(createdProduct, HttpStatus.CREATED);
    } catch (Exception exception) {
      return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @GetMapping("tenant/{tenantId}/supplier/{supplierId}/products")
  public ResponseEntity<ProductListRepresentation> getAllProductsOfSupplier(
      @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable String tenantId,
      @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable String supplierId) {
    Long tId = ResourceUtils.convertIdToLong(tenantId)
        .orElseThrow(() -> new RuntimeException("Invalid tenantId input="+tenantId));

    Long sId = ResourceUtils.convertIdToLong(supplierId)
        .orElseThrow(() -> new RuntimeException("Invalid supplierId input="+supplierId));

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
    Long tenantIdLongValue = ResourceUtils.convertIdToLong(tenantId)
        .orElseThrow(() -> new RuntimeException("Not valid tenant id!"));

    Long productIdLongValue = ResourceUtils.convertIdToLong(productId)
        .orElseThrow(() -> new RuntimeException("Not valid productId!"));

    ProductRepresentation updatedProduct = productService.updateProduct(tenantIdLongValue, productIdLongValue, productRepresentation);
    return ResponseEntity.ok(updatedProduct);
  }

  @DeleteMapping("tenant/{tenantId}/product/{productId}")
  public ResponseEntity<Void> deleteProduct(@PathVariable("tenantId") String tenantId, @PathVariable("productId") String productId) {
    if (tenantId == null || tenantId.isEmpty() || productId == null) {
      log.error("invalid input for product delete!");
      throw new RuntimeException("invalid input for product delete!");
    }
    Long tenantIdLongValue = ResourceUtils.convertIdToLong(tenantId)
        .orElseThrow(() -> new RuntimeException("Not valid tenant id!"));

    Long productIdLongValue = ResourceUtils.convertIdToLong(productId)
        .orElseThrow(() -> new RuntimeException("Not valid productId!"));

    productService.deleteProductById(productIdLongValue, tenantIdLongValue);
    return ResponseEntity.noContent().build();
  }


}
