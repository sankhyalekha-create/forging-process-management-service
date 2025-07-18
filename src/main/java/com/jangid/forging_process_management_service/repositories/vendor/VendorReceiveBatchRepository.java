package com.jangid.forging_process_management_service.repositories.vendor;

import com.jangid.forging_process_management_service.entities.vendor.VendorReceiveBatch;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VendorReceiveBatchRepository extends CrudRepository<VendorReceiveBatch, Long> {
    Optional<VendorReceiveBatch> findByIdAndTenantIdAndDeletedFalse(long id, long tenantId);
    Optional<VendorReceiveBatch> findByIdAndDeletedFalse(long id);
    Page<VendorReceiveBatch> findByTenantIdAndDeletedFalseOrderByCreatedAtDesc(long tenantId, Pageable pageable);
    List<VendorReceiveBatch> findByTenantIdAndDeletedFalseOrderByCreatedAtDesc(long tenantId);
    
    // Find by vendor
    // Check if batch number exists
    boolean existsByVendorReceiveBatchNumberAndTenantIdAndDeletedFalse(String batchNumber, long tenantId);
    
    // Find batches pending quality check
    List<VendorReceiveBatch> findByTenantIdAndQualityCheckRequiredTrueAndQualityCheckCompletedFalseAndDeletedFalse(Long tenantId);
    
}