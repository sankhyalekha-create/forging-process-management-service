package com.jangid.forging_process_management_service.repositories.dispatch;

import com.jangid.forging_process_management_service.entities.dispatch.ProcessedItemDispatchBatch;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProcessedItemDispatchBatchRepository extends CrudRepository<ProcessedItemDispatchBatch, Long> {
  Optional<ProcessedItemDispatchBatch> findByIdAndDeletedFalse(long id);
}
