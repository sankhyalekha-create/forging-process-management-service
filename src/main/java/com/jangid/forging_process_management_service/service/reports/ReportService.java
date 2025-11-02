package com.jangid.forging_process_management_service.service.reports;

import com.jangid.forging_process_management_service.dto.reports.SalesSummaryReport;
import com.jangid.forging_process_management_service.entities.gst.InvoiceStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for generating accounting reports
 * Uses native SQL queries for optimal performance with aggregations
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

  private final EntityManager entityManager;

  /**
   * Generate Sales Summary Report with monthly and status breakdowns
   * 
   * @param tenantId Tenant ID
   * @param fromDate Start date
   * @param toDate End date
   * @param buyerId Optional buyer filter
   * @param status Optional status filter
   * @return Sales Summary Report
   */
  @Transactional(readOnly = true)
  public SalesSummaryReport getSalesSummaryReport(Long tenantId, LocalDateTime fromDate, 
                                                   LocalDateTime toDate, Long buyerId, InvoiceStatus status) {
    log.info("Generating sales summary report for tenant: {}, from: {}, to: {}", tenantId, fromDate, toDate);
    
    // Overall statistics
    Map<String, Object> overallStats = getOverallSalesStats(tenantId, fromDate, toDate, buyerId, status);
    
    // Monthly breakdown
    List<SalesSummaryReport.MonthlyBreakdown> monthlyBreakdown = 
        getMonthlyBreakdown(tenantId, fromDate, toDate, buyerId, status);
    
    // Status breakdown
    List<SalesSummaryReport.StatusBreakdown> statusBreakdown = 
        getStatusBreakdown(tenantId, fromDate, toDate, buyerId);
    
    // Current month sales
    LocalDateTime monthStart = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
    LocalDateTime monthEnd = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);
    Map<String, Object> currentMonthStats = getOverallSalesStats(tenantId, monthStart, monthEnd, null, null);
    
    return SalesSummaryReport.builder()
        .fromDate(fromDate)
        .toDate(toDate)
        .totalSalesValue((Double) overallStats.get("totalSales"))
        .totalInvoiceCount(((Number) overallStats.get("invoiceCount")).intValue())
        .averageInvoiceValue((Double) overallStats.get("averageInvoice"))
        .totalValueThisMonth((Double) currentMonthStats.get("totalSales"))
        .monthlyBreakdown(monthlyBreakdown)
        .statusBreakdown(statusBreakdown)
        .build();
  }
  
  /**
   * Get overall sales statistics
   */
  private Map<String, Object> getOverallSalesStats(Long tenantId, LocalDateTime fromDate, 
                                                    LocalDateTime toDate, Long buyerId, InvoiceStatus status) {
    // Build dynamic SQL based on optional parameters
    StringBuilder sql = new StringBuilder("""
        SELECT 
          COALESCE(COUNT(*), 0) as invoice_count,
          COALESCE(SUM(total_invoice_value), 0.0) as total_sales,
          COALESCE(AVG(total_invoice_value), 0.0) as average_invoice
        FROM invoice
        WHERE tenant_id = :tenantId
          AND deleted = false
          AND invoice_date BETWEEN :fromDate AND :toDate
        """);
    
    if (buyerId != null) {
      sql.append(" AND recipient_buyer_entity_id = :buyerId");
    }
    
    if (status != null) {
      sql.append(" AND status = :status");
    }
    
    Query query = entityManager.createNativeQuery(sql.toString());
    query.setParameter("tenantId", tenantId);
    query.setParameter("fromDate", fromDate);
    query.setParameter("toDate", toDate);
    
    if (buyerId != null) {
      query.setParameter("buyerId", buyerId);
    }
    
    if (status != null) {
      query.setParameter("status", status.name());
    }
    
    Object[] result = (Object[]) query.getSingleResult();
    
    Map<String, Object> stats = new HashMap<>();
    stats.put("invoiceCount", ((Number) result[0]).longValue());
    stats.put("totalSales", ((Number) result[1]).doubleValue());
    stats.put("averageInvoice", ((Number) result[2]).doubleValue());
    
    return stats;
  }
  
  /**
   * Get monthly breakdown of sales
   */
  private List<SalesSummaryReport.MonthlyBreakdown> getMonthlyBreakdown(
      Long tenantId, LocalDateTime fromDate, LocalDateTime toDate, Long buyerId, InvoiceStatus status) {
    
    // Build dynamic SQL based on optional parameters
    StringBuilder sql = new StringBuilder("""
        SELECT 
          TO_CHAR(invoice_date, 'YYYY-MM') as month,
          COALESCE(SUM(total_invoice_value), 0.0) as sales_value,
          COALESCE(COUNT(*), 0) as invoice_count,
          COALESCE(AVG(total_invoice_value), 0.0) as average_invoice
        FROM invoice
        WHERE tenant_id = :tenantId
          AND deleted = false
          AND invoice_date BETWEEN :fromDate AND :toDate
        """);
    
    if (buyerId != null) {
      sql.append(" AND recipient_buyer_entity_id = :buyerId");
    }
    
    if (status != null) {
      sql.append(" AND status = :status");
    }
    
    sql.append("""
        
        GROUP BY TO_CHAR(invoice_date, 'YYYY-MM')
        ORDER BY TO_CHAR(invoice_date, 'YYYY-MM') DESC
        """);
    
    Query query = entityManager.createNativeQuery(sql.toString());
    query.setParameter("tenantId", tenantId);
    query.setParameter("fromDate", fromDate);
    query.setParameter("toDate", toDate);
    
    if (buyerId != null) {
      query.setParameter("buyerId", buyerId);
    }
    
    if (status != null) {
      query.setParameter("status", status.name());
    }
    
    @SuppressWarnings("unchecked")
    List<Object[]> results = query.getResultList();
    
    List<SalesSummaryReport.MonthlyBreakdown> breakdown = new ArrayList<>();
    for (Object[] row : results) {
      breakdown.add(SalesSummaryReport.MonthlyBreakdown.builder()
          .month((String) row[0])
          .salesValue(((Number) row[1]).doubleValue())
          .invoiceCount(((Number) row[2]).intValue())
          .averageInvoiceValue(((Number) row[3]).doubleValue())
          .build());
    }
    
    return breakdown;
  }
  
  /**
   * Get status-wise breakdown of invoices
   */
  private List<SalesSummaryReport.StatusBreakdown> getStatusBreakdown(
      Long tenantId, LocalDateTime fromDate, LocalDateTime toDate, Long buyerId) {
    
    // Build dynamic SQL based on optional parameters
    StringBuilder sql = new StringBuilder("""
        SELECT 
          status,
          COALESCE(COUNT(*), 0) as count,
          COALESCE(SUM(total_invoice_value), 0.0) as total_value
        FROM invoice
        WHERE tenant_id = :tenantId
          AND deleted = false
          AND invoice_date BETWEEN :fromDate AND :toDate
        """);
    
    if (buyerId != null) {
      sql.append(" AND recipient_buyer_entity_id = :buyerId");
    }
    
    sql.append("""
        
        GROUP BY status
        ORDER BY total_value DESC
        """);
    
    Query query = entityManager.createNativeQuery(sql.toString());
    query.setParameter("tenantId", tenantId);
    query.setParameter("fromDate", fromDate);
    query.setParameter("toDate", toDate);
    
    if (buyerId != null) {
      query.setParameter("buyerId", buyerId);
    }
    
    @SuppressWarnings("unchecked")
    List<Object[]> results = query.getResultList();
    
    // Calculate total for percentage
    double totalValue = results.stream()
        .mapToDouble(row -> ((Number) row[2]).doubleValue())
        .sum();
    
    List<SalesSummaryReport.StatusBreakdown> breakdown = new ArrayList<>();
    for (Object[] row : results) {
      double value = ((Number) row[2]).doubleValue();
      double percentage = totalValue > 0 ? (value / totalValue) * 100 : 0;
      
      breakdown.add(SalesSummaryReport.StatusBreakdown.builder()
          .status((String) row[0])
          .count(((Number) row[1]).intValue())
          .totalValue(value)
          .percentage(Math.round(percentage * 100.0) / 100.0) // Round to 2 decimals
          .build());
    }
    
    return breakdown;
  }
}

