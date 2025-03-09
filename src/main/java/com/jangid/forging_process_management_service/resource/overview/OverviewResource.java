package com.jangid.forging_process_management_service.resource.overview;

import com.jangid.forging_process_management_service.entitiesRepresentation.overview.ProductQuantityListRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.overview.ProductQuantityRepresentation;
import com.jangid.forging_process_management_service.exception.TenantNotFoundException;
import com.jangid.forging_process_management_service.service.product.ProductService;
import com.jangid.forging_process_management_service.utils.GenericResourceUtils;

import io.swagger.annotations.ApiParam;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Component
public class OverviewResource {

  @Autowired
  private ProductService productService;

  @GetMapping("tenant/{tenantId}/product-highlights")
  public ResponseEntity<ProductQuantityListRepresentation> getTopProductQuantities(@ApiParam(value = "Identifier of the tenant", required = true) @PathVariable String tenantId) {
    Long tId = GenericResourceUtils.convertResourceIdToLong(tenantId)
        .orElseThrow(() -> new TenantNotFoundException(tenantId));

    List<ProductQuantityRepresentation> productQuantities = productService.getProductQuantities(tId);
    return ResponseEntity.ok(ProductQuantityListRepresentation.builder()
        .productQuantities(productQuantities)
        .build());
  }

}
