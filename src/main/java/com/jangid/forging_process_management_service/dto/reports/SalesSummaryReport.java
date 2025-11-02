package com.jangid.forging_process_management_service.dto.reports;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for Sales Summary Report
 * Contains aggregated sales data with monthly and status breakdowns
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesSummaryReport {

  private LocalDateTime fromDate;
  private LocalDateTime toDate;
  private Double totalSalesValue;
  private Integer totalInvoiceCount;
  private Double averageInvoiceValue;
  private Double totalValueThisMonth;
  
  private List<MonthlyBreakdown> monthlyBreakdown;
  private List<StatusBreakdown> statusBreakdown;
  
  /**
   * Monthly breakdown of sales
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class MonthlyBreakdown {
    private String month; // YYYY-MM format
    private Double salesValue;
    private Integer invoiceCount;
    private Double averageInvoiceValue;
  }
  
  /**
   * Status-wise breakdown of invoices
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class StatusBreakdown {
    private String status; // DRAFT, GENERATED, SENT, PARTIALLY_PAID, PAID, CANCELLED
    private Integer count;
    private Double totalValue;
    private Double percentage;
  }
}

