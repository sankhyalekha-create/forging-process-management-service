package com.jangid.forging_process_management_service.repositories.gst;

import com.jangid.forging_process_management_service.entities.gst.DeliveryChallan;
import com.jangid.forging_process_management_service.entities.gst.ChallanStatus;

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
public interface DeliveryChallanRepository extends JpaRepository<DeliveryChallan, Long> {

    // Basic CRUD operations
    Optional<DeliveryChallan> findByIdAndTenantIdAndDeletedFalse(Long id, Long tenantId);
    
    Page<DeliveryChallan> findByTenantIdAndDeletedFalse(Long tenantId, Pageable pageable);
    
    // Find by challan number
    Optional<DeliveryChallan> findByChallanNumberAndTenantIdAndDeletedFalse(String challanNumber, Long tenantId);
    
    boolean existsByChallanNumberAndTenantIdAndDeletedFalse(String challanNumber, Long tenantId);
    
    // Find by dispatch batch
    List<DeliveryChallan> findByDispatchBatchIdAndTenantIdAndDeletedFalse(Long dispatchBatchId, Long tenantId);
    
    Page<DeliveryChallan> findByDispatchBatchIdAndTenantIdAndDeletedFalse(Long dispatchBatchId, Long tenantId, Pageable pageable);
    
    // Find by status
    List<DeliveryChallan> findByTenantIdAndStatusAndDeletedFalse(Long tenantId, ChallanStatus status);
    
    Page<DeliveryChallan> findByTenantIdAndStatusAndDeletedFalse(Long tenantId, ChallanStatus status, Pageable pageable);
    
    // Find by date range
    @Query("SELECT dc FROM DeliveryChallan dc WHERE dc.tenant.id = :tenantId " +
           "AND dc.challanDate BETWEEN :startDate AND :endDate " +
           "AND dc.deleted = false")
    List<DeliveryChallan> findByTenantIdAndChallanDateBetween(
        @Param("tenantId") Long tenantId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate);
    
    // Find challans ready for conversion to invoice
    @Query("SELECT dc FROM DeliveryChallan dc WHERE dc.tenant.id = :tenantId " +
           "AND dc.status = 'DELIVERED' " +
           "AND dc.convertedToInvoice IS NULL " +
           "AND dc.deleted = false")
    List<DeliveryChallan> findChallansReadyForInvoiceConversion(@Param("tenantId") Long tenantId);
    
    // Find by consignee GSTIN
    List<DeliveryChallan> findByConsigneeGstinAndTenantIdAndDeletedFalse(String consigneeGstin, Long tenantId);
    
    // Count challans by status
    @Query("SELECT COUNT(dc) FROM DeliveryChallan dc WHERE dc.tenant.id = :tenantId " +
           "AND dc.status = :status AND dc.deleted = false")
    long countByTenantIdAndStatus(@Param("tenantId") Long tenantId, @Param("status") ChallanStatus status);
}
