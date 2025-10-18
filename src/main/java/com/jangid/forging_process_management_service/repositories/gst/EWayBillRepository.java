package com.jangid.forging_process_management_service.repositories.gst;

import com.jangid.forging_process_management_service.entities.gst.EWayBill;
import com.jangid.forging_process_management_service.entities.gst.EWayBillStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EWayBillRepository extends JpaRepository<EWayBill, Long> {

    // Basic CRUD operations
    Optional<EWayBill> findByIdAndTenantIdAndDeletedFalse(Long id, Long tenantId);
    
    Page<EWayBill> findByTenantIdAndDeletedFalse(Long tenantId, Pageable pageable);
    
    // Find by EWay Bill number
    Optional<EWayBill> findByEwayBillNumberAndDeletedFalse(String ewayBillNumber);
    
    boolean existsByEwayBillNumberAndDeletedFalse(String ewayBillNumber);
    
    // Find by dispatch batch
    List<EWayBill> findByDispatchBatchIdAndTenantIdAndDeletedFalse(Long dispatchBatchId, Long tenantId);
    
    // Find by invoice
    Optional<EWayBill> findByInvoiceIdAndTenantIdAndDeletedFalse(Long invoiceId, Long tenantId);
    
    // Find by delivery challan
    Optional<EWayBill> findByDeliveryChallanIdAndTenantIdAndDeletedFalse(Long deliveryChallanId, Long tenantId);
    
    // Find by status
    List<EWayBill> findByTenantIdAndStatusAndDeletedFalse(Long tenantId, EWayBillStatus status);
    
    Page<EWayBill> findByTenantIdAndStatusAndDeletedFalse(Long tenantId, EWayBillStatus status, Pageable pageable);
    
    // Find by date range
    @Query("SELECT ew FROM EWayBill ew WHERE ew.tenant.id = :tenantId " +
           "AND ew.ewayBillDate BETWEEN :startDate AND :endDate " +
           "AND ew.deleted = false")
    List<EWayBill> findByTenantIdAndEwayBillDateBetween(
        @Param("tenantId") Long tenantId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate);
    
    // Find expired E-Way Bills
    @Query("SELECT ew FROM EWayBill ew WHERE ew.tenant.id = :tenantId " +
           "AND ew.validUntil < :currentDateTime " +
           "AND ew.status = 'ACTIVE' " +
           "AND ew.deleted = false")
    List<EWayBill> findExpiredEWayBills(@Param("tenantId") Long tenantId, @Param("currentDateTime") LocalDateTime currentDateTime);
    
    // Find E-Way Bills expiring soon
    @Query("SELECT ew FROM EWayBill ew WHERE ew.tenant.id = :tenantId " +
           "AND ew.validUntil BETWEEN :currentDateTime AND :expiryThreshold " +
           "AND ew.status = 'ACTIVE' " +
           "AND ew.deleted = false")
    List<EWayBill> findEWayBillsExpiringSoon(
        @Param("tenantId") Long tenantId,
        @Param("currentDateTime") LocalDateTime currentDateTime,
        @Param("expiryThreshold") LocalDateTime expiryThreshold);
    
    // Find by supplier GSTIN
    List<EWayBill> findBySupplierGstinAndTenantIdAndDeletedFalse(String supplierGstin, Long tenantId);
    
    // Find by recipient GSTIN
    List<EWayBill> findByRecipientGstinAndTenantIdAndDeletedFalse(String recipientGstin, Long tenantId);
    
    // Find by vehicle number
    List<EWayBill> findByVehicleNumberAndTenantIdAndDeletedFalse(String vehicleNumber, Long tenantId);
    
    // Count E-Way Bills by status
    @Query("SELECT COUNT(ew) FROM EWayBill ew WHERE ew.tenant.id = :tenantId " +
           "AND ew.status = :status AND ew.deleted = false")
    long countByTenantIdAndStatus(@Param("tenantId") Long tenantId, @Param("status") EWayBillStatus status);
    
    // Find active E-Way Bills for monitoring
    @Query("SELECT ew FROM EWayBill ew WHERE ew.tenant.id = :tenantId " +
           "AND ew.status = 'ACTIVE' " +
           "AND ew.validUntil > :currentDateTime " +
           "AND ew.deleted = false")
    List<EWayBill> findActiveEWayBills(@Param("tenantId") Long tenantId, @Param("currentDateTime") LocalDateTime currentDateTime);
}
