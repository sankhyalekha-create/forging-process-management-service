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
    
    // Find by dispatch batch (using junction table)
    @Query("SELECT dc FROM DeliveryChallan dc " +
           "JOIN dc.challanDispatchBatches cdb " +
           "WHERE cdb.dispatchBatch.id = :dispatchBatchId " +
           "AND dc.deleted = false")
    Optional<DeliveryChallan> findByDispatchBatchId(@Param("dispatchBatchId") Long dispatchBatchId);
    
    @Query("SELECT dc FROM DeliveryChallan dc " +
           "JOIN dc.challanDispatchBatches cdb " +
           "WHERE cdb.dispatchBatch.id = :dispatchBatchId " +
           "AND dc.tenant.id = :tenantId " +
           "AND dc.deleted = false")
    List<DeliveryChallan> findByDispatchBatchIdAndTenantIdAndDeletedFalse(
        @Param("dispatchBatchId") Long dispatchBatchId, 
        @Param("tenantId") Long tenantId);
    
    @Query("SELECT dc FROM DeliveryChallan dc " +
           "JOIN dc.challanDispatchBatches cdb " +
           "WHERE cdb.dispatchBatch.id = :dispatchBatchId " +
           "AND dc.tenant.id = :tenantId " +
           "AND dc.deleted = false")
    Page<DeliveryChallan> findByDispatchBatchIdAndTenantIdAndDeletedFalse(
        @Param("dispatchBatchId") Long dispatchBatchId, 
        @Param("tenantId") Long tenantId, 
        Pageable pageable);
    
    // Find by status
    List<DeliveryChallan> findByTenantIdAndStatusAndDeletedFalse(Long tenantId, ChallanStatus status);
    
    Page<DeliveryChallan> findByTenantIdAndStatusAndDeletedFalse(Long tenantId, ChallanStatus status, Pageable pageable);
    
    // Find by date range
    @Query("SELECT dc FROM DeliveryChallan dc WHERE dc.tenant.id = :tenantId " +
           "AND dc.challanDateTime BETWEEN :startDate AND :endDate " +
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
    
    // Find by consignee GSTIN (checking both buyer and vendor entities)
    @Query("SELECT dc FROM DeliveryChallan dc " +
           "WHERE dc.tenant.id = :tenantId " +
           "AND dc.deleted = false " +
           "AND ((dc.consigneeBuyerEntity.gstinUin = :consigneeGstin) " +
           "     OR (dc.consigneeVendorEntity.gstinUin = :consigneeGstin))")
    List<DeliveryChallan> findByConsigneeGstinAndTenantIdAndDeletedFalse(
        @Param("consigneeGstin") String consigneeGstin, 
        @Param("tenantId") Long tenantId);
    
    // Count challans by status
    @Query("SELECT COUNT(dc) FROM DeliveryChallan dc WHERE dc.tenant.id = :tenantId " +
           "AND dc.status = :status AND dc.deleted = false")
    long countByTenantIdAndStatus(@Param("tenantId") Long tenantId, @Param("status") ChallanStatus status);
    
    // Find deleted challans ordered by deletion date (oldest first) for challan number reuse
    @Query("SELECT dc FROM DeliveryChallan dc WHERE dc.tenant.id = :tenantId " +
           "AND dc.deleted = true ORDER BY dc.deletedAt ASC")
    List<DeliveryChallan> findDeletedChallansByTenantOrderByDeletedAtAsc(@Param("tenantId") Long tenantId);
}
