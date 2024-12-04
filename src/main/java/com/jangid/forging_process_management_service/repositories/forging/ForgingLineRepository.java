package com.jangid.forging_process_management_service.repositories.forging;

import com.jangid.forging_process_management_service.entities.forging.ForgingLine;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ForgingLineRepository extends CrudRepository<ForgingLine, Long> {
  // Custom query methods (if needed) can be added here

  Optional<ForgingLine> findByIdAndTenantIdAndDeletedFalse(long forgingLineId, long tenantId);
  Optional<ForgingLine> findByIdAndDeletedFalse(long forgingLineId);

  Page<ForgingLine> findByTenantIdAndDeletedIsFalseOrderByUpdatedAtDesc(long tenantId, Pageable pageable);
  List<ForgingLine> findByTenantIdAndDeletedIsFalseOrderByCreatedAtDesc(long tenantId);

  boolean existsByTenantIdAndDeletedFalse(long tenantId);
}

