package com.jangid.forging_process_management_service.resource.reports;

import com.jangid.forging_process_management_service.dto.reports.SalesSummaryReport;
import com.jangid.forging_process_management_service.entities.gst.InvoiceStatus;
import com.jangid.forging_process_management_service.service.reports.ReportService;
import com.jangid.forging_process_management_service.utils.ConvertorUtils;
import com.jangid.forging_process_management_service.utils.GenericExceptionHandler;
import com.jangid.forging_process_management_service.configuration.security.TenantContextHolder;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * REST controller for accounting reports
 * Provides endpoints for various financial and operational reports
 */
@Slf4j
@RestController
@RequestMapping("/api/accounting/reports")
@RequiredArgsConstructor
@Api(tags = "Accounting Reports")
public class ReportResource {

  private final ReportService reportService;

  /**
   * Get Sales Summary Report
   * 
   * Provides aggregated sales data with monthly trends and status breakdowns
   * 
   * @param fromDate Start date (format: yyyy-MM-ddTHH:mm)
   * @param toDate End date (format: yyyy-MM-ddTHH:mm)
   * @param buyerId Optional buyer/customer filter
   * @param status Optional invoice status filter
   * @return Sales Summary Report with monthly and status breakdowns
   */
  @GetMapping("/sales-summary")
  @ApiOperation(value = "Get Sales Summary Report", 
                notes = "Generate sales summary with monthly trends and status breakdowns")
  public ResponseEntity<?> getSalesSummaryReport(
      @ApiParam(value = "Start date (yyyy-MM-ddTHH:mm)", required = true, example = "2024-01-01T00:00")
      @RequestParam String fromDate,
      
      @ApiParam(value = "End date (yyyy-MM-ddTHH:mm)", required = true, example = "2024-12-31T23:59")
      @RequestParam String toDate,
      
      @ApiParam(value = "Buyer/Customer ID filter", required = false)
      @RequestParam(required = false) Long buyerId,
      
      @ApiParam(value = "Invoice status filter (DRAFT, GENERATED, SENT, PARTIALLY_PAID, PAID, CANCELLED)", required = false)
      @RequestParam(required = false) String status) {
    
    try {
      Long tenantId = TenantContextHolder.getAuthenticatedTenantId();
      log.info("Generating sales summary report for tenant: {}, from: {}, to: {}", tenantId, fromDate, toDate);
      
      // Parse dates
      LocalDateTime fromDateTime = ConvertorUtils.convertStringToLocalDateTime(fromDate);
      LocalDateTime toDateTime = ConvertorUtils.convertStringToLocalDateTime(toDate);
      
      // Validate date range (max 2 years for performance)
      if (fromDateTime.plusYears(2).isBefore(toDateTime)) {
        return new ResponseEntity<>("Date range cannot exceed 2 years for performance reasons", 
                                    HttpStatus.BAD_REQUEST);
      }
      
      // Parse status
      InvoiceStatus invoiceStatus = null;
      if (status != null && !status.isEmpty()) {
        try {
          invoiceStatus = InvoiceStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
          return new ResponseEntity<>("Invalid status value. Must be one of: DRAFT, GENERATED, SENT, PARTIALLY_PAID, PAID, CANCELLED", 
                                      HttpStatus.BAD_REQUEST);
        }
      }
      
      // Generate report
      SalesSummaryReport report = reportService.getSalesSummaryReport(
          tenantId, fromDateTime, toDateTime, buyerId, invoiceStatus);
      
      log.info("Sales summary report generated successfully for tenant: {}", tenantId);
      return new ResponseEntity<>(report, HttpStatus.OK);
      
    } catch (Exception exception) {
      log.error("Error generating sales summary report", exception);
      return GenericExceptionHandler.handleException(exception, "getSalesSummaryReport");
    }
  }
}

