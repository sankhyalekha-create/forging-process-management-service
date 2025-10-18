package com.jangid.forging_process_management_service.repositories.gst;

import com.jangid.forging_process_management_service.entities.gst.Invoice;
import com.jangid.forging_process_management_service.entities.gst.InvoiceStatus;
import com.jangid.forging_process_management_service.entities.gst.InvoiceType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    // Basic CRUD operations
    Optional<Invoice> findByIdAndTenantIdAndDeletedFalse(Long id, Long tenantId);
    
    Page<Invoice> findByTenantIdAndDeletedFalse(Long tenantId, Pageable pageable);
    
    // Find by invoice number
    Optional<Invoice> findByInvoiceNumberAndTenantIdAndDeletedFalse(String invoiceNumber, Long tenantId);
    
    boolean existsByInvoiceNumberAndTenantIdAndDeletedFalse(String invoiceNumber, Long tenantId);
    
    // Find by dispatch batch
    List<Invoice> findByDispatchBatchIdAndTenantIdAndDeletedFalse(Long dispatchBatchId, Long tenantId);
    
    Page<Invoice> findByDispatchBatchIdAndTenantIdAndDeletedFalse(Long dispatchBatchId, Long tenantId, Pageable pageable);
    
    // Find by delivery challan
    Optional<Invoice> findByDeliveryChallanIdAndTenantIdAndDeletedFalse(Long deliveryChallanId, Long tenantId);
    
    // Find by status
    List<Invoice> findByTenantIdAndStatusAndDeletedFalse(Long tenantId, InvoiceStatus status);
    
    Page<Invoice> findByTenantIdAndStatusAndDeletedFalse(Long tenantId, InvoiceStatus status, Pageable pageable);
    
    // Find by invoice type
    List<Invoice> findByTenantIdAndInvoiceTypeAndDeletedFalse(Long tenantId, InvoiceType invoiceType);
    
    // Find by date range
    @Query("SELECT i FROM Invoice i WHERE i.tenant.id = :tenantId " +
           "AND i.invoiceDate BETWEEN :startDate AND :endDate " +
           "AND i.deleted = false")
    List<Invoice> findByTenantIdAndInvoiceDateBetween(
        @Param("tenantId") Long tenantId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate);
    
    // Find by recipient GSTIN
    List<Invoice> findByRecipientGstinAndTenantIdAndDeletedFalse(String recipientGstin, Long tenantId);
    
    // Find overdue invoices
    @Query("SELECT i FROM Invoice i WHERE i.tenant.id = :tenantId " +
           "AND i.dueDate < :currentDate " +
           "AND i.status NOT IN ('PAID', 'CANCELLED') " +
           "AND i.deleted = false")
    List<Invoice> findOverdueInvoices(@Param("tenantId") Long tenantId, @Param("currentDate") LocalDate currentDate);
    
    // Find invoices by value range
    @Query("SELECT i FROM Invoice i WHERE i.tenant.id = :tenantId " +
           "AND i.totalInvoiceValue BETWEEN :minValue AND :maxValue " +
           "AND i.deleted = false")
    List<Invoice> findByTenantIdAndValueRange(
        @Param("tenantId") Long tenantId,
        @Param("minValue") BigDecimal minValue,
        @Param("maxValue") BigDecimal maxValue);
    
    // Count invoices by status
    @Query("SELECT COUNT(i) FROM Invoice i WHERE i.tenant.id = :tenantId " +
           "AND i.status = :status AND i.deleted = false")
    long countByTenantIdAndStatus(@Param("tenantId") Long tenantId, @Param("status") InvoiceStatus status);
    
    // Calculate total invoice value by status
    @Query("SELECT COALESCE(SUM(i.totalInvoiceValue), 0) FROM Invoice i WHERE i.tenant.id = :tenantId " +
           "AND i.status = :status AND i.deleted = false")
    BigDecimal sumTotalValueByTenantIdAndStatus(@Param("tenantId") Long tenantId, @Param("status") InvoiceStatus status);
    
    // Find amended invoices for original invoice
    List<Invoice> findByOriginalInvoiceIdAndTenantIdAndDeletedFalse(Long originalInvoiceId, Long tenantId);
}
