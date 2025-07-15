package com.jangid.forging_process_management_service.repositories.vendor;

import com.jangid.forging_process_management_service.entities.vendor.VendorDispatchBatch;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VendorDispatchBatchRepository extends CrudRepository<VendorDispatchBatch, Long> {
    Optional<VendorDispatchBatch> findByIdAndTenantIdAndDeletedFalse(long id, long tenantId);
    Optional<VendorDispatchBatch> findByIdAndDeletedFalse(long id);
    Page<VendorDispatchBatch> findByTenantIdAndDeletedFalseOrderByCreatedAtDesc(long tenantId, Pageable pageable);
    List<VendorDispatchBatch> findByTenantIdAndDeletedFalseOrderByCreatedAtDesc(long tenantId);
    
    // Find by vendor
    List<VendorDispatchBatch> findByVendorIdAndTenantIdAndDeletedFalse(long vendorId, long tenantId);
    Page<VendorDispatchBatch> findByVendorIdAndTenantIdAndDeletedFalse(long vendorId, long tenantId, Pageable pageable);
    // Check if batch number exists
    boolean existsByVendorDispatchBatchNumberAndTenantIdAndDeletedFalse(String batchNumber, long tenantId);
}