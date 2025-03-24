package com.jangid.forging_process_management_service.resource.overview;

import com.jangid.forging_process_management_service.entitiesRepresentation.overview.OperatorPerformanceRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.overview.ProductQuantityRepresentation;
import com.jangid.forging_process_management_service.exception.TenantNotFoundException;
import com.jangid.forging_process_management_service.service.operator.OperatorService;
import com.jangid.forging_process_management_service.service.product.ProductService;
import com.jangid.forging_process_management_service.utils.GenericResourceUtils;

import io.swagger.annotations.ApiParam;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.ws.rs.BadRequestException;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Component
public class OverviewResource {

  @Autowired
  private ProductService productService;

  @Autowired
  private OperatorService operatorService;

  @GetMapping("tenant/{tenantId}/product-highlights")
  public ResponseEntity<Page<ProductQuantityRepresentation>> getTopProductQuantities(
      @ApiParam(value = "Identifier of the tenant", required = true) @PathVariable String tenantId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "5") int size) {

    Long tId = GenericResourceUtils.convertResourceIdToLong(tenantId)
        .orElseThrow(() -> new TenantNotFoundException(tenantId));

    PageRequest pageRequest = PageRequest.of(page, size);
    Page<ProductQuantityRepresentation> productQuantities = productService.getProductQuantities(tId, pageRequest);
    return ResponseEntity.ok(productQuantities);
  }

  @GetMapping("tenant/{tenantId}/operators-performance/month/{month}/year/{year}")
  public ResponseEntity<Page<OperatorPerformanceRepresentation>> getOperatorsPerformanceByMonth(
          @PathVariable Long tenantId,
          @PathVariable int month,
          @PathVariable int year,
          @RequestParam(defaultValue = "0") int page,
          @RequestParam(defaultValue = "5") int size) {

      // Validate month
      if (month < 1 || month > 12) {
          throw new BadRequestException("Invalid month. Month should be between 1 and 12");
      }

      // Create start and end date for the specified month
      LocalDateTime startDate = LocalDateTime.of(year, month, 1, 0, 0);
      LocalDateTime endDate = startDate.plusMonths(1).minusSeconds(1);

      PageRequest pageRequest = PageRequest.of(page, size);

      Page<OperatorPerformanceRepresentation> performances = operatorService
          .getOperatorsPerformanceForPeriod(tenantId, startDate, endDate, pageRequest);

      return ResponseEntity.ok(performances);
  }

}
