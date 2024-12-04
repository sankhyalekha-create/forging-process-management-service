package com.jangid.forging_process_management_service.repositories.forging;

import com.jangid.forging_process_management_service.entities.forging.Furnace;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FurnaceRepository extends CrudRepository<Furnace, Long> {
  // Custom query methods (if needed) can be added here
  Page<Furnace> findByTenantIdAndDeletedIsFalseOrderByCreatedAtDesc(long tenantId, Pageable pageable);
}

