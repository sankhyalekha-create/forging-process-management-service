package com.jangid.forging_process_management_service.repositories.dispatch;

import com.jangid.forging_process_management_service.entities.dispatch.DispatchBatch;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DispatchBatchRepository extends JpaRepository<DispatchBatch, Long> {

  boolean existsByDispatchBatchNumberAndTenantIdAndDeletedFalse(String dispatchBatchNumber, Long tenantId);

  Optional<DispatchBatch> findByIdAndDeletedFalse(Long id);
  List<DispatchBatch> findByTenantIdAndDeletedIsFalseOrderByUpdatedAtDesc(long tenantId);

  Page<DispatchBatch> findByTenantIdAndDeletedIsFalseOrderByUpdatedAtDesc(long tenantId, Pageable pageable);

  List<DispatchBatch> findByTenantIdAndDeletedIsFalseAndDispatchBatchStatusAndDispatchedAtBetween(
      long tenantId, DispatchBatch.DispatchBatchStatus status, LocalDateTime start, LocalDateTime end);
}
