package com.jangid.forging_process_management_service.repositories.forging;

import com.jangid.forging_process_management_service.entities.forging.ForgingLine;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ForgingLineRepository extends JpaRepository<ForgingLine, Long> {
  // Custom query methods (if needed) can be added here
  List<ForgingLine> findByTenantIdAndDeletedIsFalseOrderByCreatedAtDesc(long tenantId);
}

