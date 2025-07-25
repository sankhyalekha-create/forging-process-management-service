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
    Optional<VendorDispatchBatch> findByProcessedItemIdAndDeletedFalse(long id);
    Page<VendorDispatchBatch> findByTenantIdAndDeletedFalseOrderByCreatedAtDesc(long tenantId, Pageable pageable);
    List<VendorDispatchBatch> findByTenantIdAndDeletedFalseOrderByCreatedAtDesc(long tenantId);
    
    // Find by vendor
    List<VendorDispatchBatch> findByVendorIdAndTenantIdAndDeletedFalse(long vendorId, long tenantId);
    Page<VendorDispatchBatch> findByVendorIdAndTenantIdAndDeletedFalse(long vendorId, long tenantId, Pageable pageable);
    
    // Find by workflow
    List<VendorDispatchBatch> findByProcessedItemItemWorkflowIdAndDeletedFalse(long itemWorkflowId);
    
    // Check if batch number exists
    boolean existsByVendorDispatchBatchNumberAndTenantIdAndDeletedFalse(String batchNumber, long tenantId);
    
    // Find by processed item vendor dispatch batch IDs
    List<VendorDispatchBatch> findByProcessedItemIdInAndDeletedFalse(List<Long> processedItemIds);

    // Search methods
    Page<VendorDispatchBatch> findByVendorDispatchBatchNumberContainingIgnoreCaseAndTenantIdAndDeletedFalse(String batchNumber, long tenantId, Pageable pageable);
    Page<VendorDispatchBatch> findByProcessedItemItemItemNameContainingIgnoreCaseAndTenantIdAndDeletedFalse(String itemName, long tenantId, Pageable pageable);
    Page<VendorDispatchBatch> findByProcessedItemWorkflowIdentifierContainingIgnoreCaseAndTenantIdAndDeletedFalse(String workflowIdentifier, long tenantId, Pageable pageable);
    Page<VendorDispatchBatch> findByVendorReceiveBatchesVendorReceiveBatchNumberContainingIgnoreCaseAndTenantIdAndDeletedFalse(String receiveBatchNumber, long tenantId, Pageable pageable);
}