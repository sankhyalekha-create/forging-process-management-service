package com.jangid.forging_process_management_service.repositories.gst;

import com.jangid.forging_process_management_service.entities.gst.Invoice;
import com.jangid.forging_process_management_service.entities.gst.InvoiceStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    // Find all non-deleted invoices for a tenant
    Page<Invoice> findByTenantIdAndDeletedFalse(Long tenantId, Pageable pageable);

    Optional<Invoice> findByIdAndTenantIdAndDeletedFalse(Long id, Long tenantId);

    // Find by invoice number and tenant
    Optional<Invoice> findByInvoiceNumberAndTenantIdAndDeletedFalse(String invoiceNumber, Long tenantId);

    // Find by status and tenant
    Page<Invoice> findByTenantIdAndStatusAndDeletedFalse(Long tenantId, InvoiceStatus status, Pageable pageable);

    // Find by buyer entity and tenant
    @Query("SELECT i FROM Invoice i WHERE i.tenant.id = :tenantId AND i.recipientBuyerEntity.id = :buyerEntityId AND i.deleted = false")
    Page<Invoice> findByTenantIdAndBuyerEntityIdAndDeletedFalse(@Param("tenantId") Long tenantId, @Param("buyerEntityId") Long buyerEntityId, Pageable pageable);

    // Find invoices due for payment
    @Query("SELECT i FROM Invoice i WHERE i.tenant.id = :tenantId AND i.dueDate <= :currentDate " +
           "AND i.status != 'PAID' AND i.deleted = false")
    List<Invoice> findOverdueInvoices(@Param("tenantId") Long tenantId, @Param("currentDate") LocalDate currentDate);

    // Find pending invoices for approval
    @Query("SELECT i FROM Invoice i WHERE i.tenant.id = :tenantId AND i.status = 'DRAFT' " +
           "AND i.deleted = false ORDER BY i.createdAt ASC")
    List<Invoice> findPendingApprovalInvoices(@Param("tenantId") Long tenantId);

    // Search invoices by multiple criteria
    @Query("SELECT i FROM Invoice i WHERE i.tenant.id = :tenantId AND i.deleted = false " +
           "AND (:status IS NULL OR i.status = :status) " +
           "AND (:buyerEntityId IS NULL OR i.recipientBuyerEntity.id = :buyerEntityId) " +
           "AND (:fromDate IS NULL OR i.invoiceDate >= :fromDate) " +
           "AND (:toDate IS NULL OR i.invoiceDate <= :toDate) " +
           "AND (:searchTerm IS NULL OR LOWER(i.invoiceNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<Invoice> searchInvoices(@Param("tenantId") Long tenantId,
                                 @Param("status") InvoiceStatus status,
                                 @Param("buyerEntityId") Long buyerEntityId,
                                 @Param("fromDate") LocalDateTime fromDate,
                                 @Param("toDate") LocalDateTime toDate,
                                 @Param("searchTerm") String searchTerm,
                                 Pageable pageable);

    // Count by status and tenant
    long countByTenantIdAndStatusAndDeletedFalse(Long tenantId, InvoiceStatus status);

    // Sum total amount by status and date range
    @Query("SELECT COALESCE(SUM(i.totalInvoiceValue), 0) FROM Invoice i WHERE i.tenant.id = :tenantId " +
           "AND i.status = :status AND i.invoiceDate BETWEEN :fromDate AND :toDate " +
           "AND i.deleted = false")
    Double sumTotalAmountByStatusAndDateRange(@Param("tenantId") Long tenantId,
                                              @Param("status") InvoiceStatus status,
                                              @Param("fromDate") LocalDateTime fromDate,
                                              @Param("toDate") LocalDateTime toDate);
}