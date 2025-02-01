package com.jangid.forging_process_management_service.repositories.quality;

import com.jangid.forging_process_management_service.entities.quality.ProcessedItemInspectionBatch;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProcessedItemInspectionBatchRepository extends CrudRepository<ProcessedItemInspectionBatch, Long> {
  Optional<ProcessedItemInspectionBatch> findByIdAndDeletedFalse(long id);
}
