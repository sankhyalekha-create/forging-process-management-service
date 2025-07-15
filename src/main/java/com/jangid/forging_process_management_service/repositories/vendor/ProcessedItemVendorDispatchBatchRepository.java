package com.jangid.forging_process_management_service.repositories.vendor;

import com.jangid.forging_process_management_service.entities.vendor.ProcessedItemVendorDispatchBatch;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProcessedItemVendorDispatchBatchRepository extends JpaRepository<ProcessedItemVendorDispatchBatch, Long> {

    /**
     * Find processed item vendor dispatch batch by ID and ensure it's not deleted
     */
    Optional<ProcessedItemVendorDispatchBatch> findByIdAndDeletedFalse(Long id);

} 