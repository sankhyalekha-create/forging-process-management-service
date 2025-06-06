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
  List<ForgingLine> findByTenantIdAndDeletedIsFalseOrderByUpdatedAtDesc(long tenantId);

  boolean existsByTenantIdAndDeletedFalse(long tenantId);
  
  // New methods for handling duplicate forgingLineName and reactivating deleted forging lines
  boolean existsByForgingLineNameAndTenantIdAndDeletedFalse(String forgingLineName, long tenantId);
  Optional<ForgingLine> findByForgingLineNameAndTenantIdAndDeletedTrue(String forgingLineName, long tenantId);
}

