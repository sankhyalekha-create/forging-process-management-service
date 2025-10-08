package com.jangid.forging_process_management_service.resource.overview;

import com.jangid.forging_process_management_service.configuration.security.TenantContextHolder;
import com.jangid.forging_process_management_service.entitiesRepresentation.overview.OperatorPerformanceRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.overview.ProductQuantityRepresentation;
import com.jangid.forging_process_management_service.service.operator.OperatorService;
import com.jangid.forging_process_management_service.service.product.ProductService;
import com.jangid.forging_process_management_service.utils.GenericExceptionHandler;

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

  @GetMapping("product-highlights")
  public ResponseEntity<?> getTopProductQuantities(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "5") int size) {

    try {
      Long tId = TenantContextHolder.getAuthenticatedTenantId();

      PageRequest pageRequest = PageRequest.of(page, size);
      Page<ProductQuantityRepresentation> productQuantities = productService.getProductQuantities(tId, pageRequest);
      return ResponseEntity.ok(productQuantities);
    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "getTopProductQuantities");
    }
  }

  @GetMapping("operators-performance/month/{month}/year/{year}")
  public ResponseEntity<?> getOperatorsPerformanceByMonth(
          @PathVariable int month,
          @PathVariable int year,
          @RequestParam(defaultValue = "0") int page,
          @RequestParam(defaultValue = "5") int size) {

      try {
          // Validate month
          if (month < 1 || month > 12) {
              throw new IllegalArgumentException("Invalid month. Month should be between 1 and 12");
          }
          Long tenantId = TenantContextHolder.getAuthenticatedTenantId();

          // Create start and end date for the specified month
          LocalDateTime startDate = LocalDateTime.of(year, month, 1, 0, 0);
          LocalDateTime endDate = startDate.plusMonths(1).minusSeconds(1);

          PageRequest pageRequest = PageRequest.of(page, size);

          Page<OperatorPerformanceRepresentation> performances = operatorService
              .getOperatorsPerformanceForPeriod(tenantId, startDate, endDate, pageRequest);

          return ResponseEntity.ok(performances);
      } catch (Exception exception) {
          return GenericExceptionHandler.handleException(exception, "getOperatorsPerformanceByMonth");
      }
  }

}
