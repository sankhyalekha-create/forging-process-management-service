package com.jangid.forging_process_management_service.repositories.dispatch;

import com.jangid.forging_process_management_service.entities.dispatch.DispatchProcessedItemInspection;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DispatchProcessedItemInspectionRepository extends CrudRepository<DispatchProcessedItemInspection, Long> {

  boolean existsByProcessedItemInspectionBatchIdAndDeletedFalse(Long processedItemInspectionBatchId);

}
