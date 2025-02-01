package com.jangid.forging_process_management_service.repositories.dispatch;

import com.jangid.forging_process_management_service.entities.dispatch.DispatchBatch;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DispatchBatchRepository extends CrudRepository<DispatchBatch, Long> {

  boolean existsByDispatchBatchNumberAndTenantIdAndDeletedFalse(String dispatchBatchNumber, long tenantId);

  Optional<DispatchBatch> findByIdAndDeletedFalse(long id);
  List<DispatchBatch> findByTenantIdAndDeletedIsFalseOrderByCreatedAtDesc(long tenantId);

  Page<DispatchBatch> findByTenantIdAndDeletedIsFalseOrderByCreatedAtDesc(long tenantId, Pageable pageable);
}
