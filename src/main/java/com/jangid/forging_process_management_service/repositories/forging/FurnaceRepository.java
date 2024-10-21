package com.jangid.forging_process_management_service.repositories.forging;

import com.jangid.forging_process_management_service.entities.forging.Furnace;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FurnaceRepository extends JpaRepository<Furnace, Long> {
  // Custom query methods (if needed) can be added here
  List<Furnace> findByTenantIdAndDeletedIsFalseOrderByCreatedAtDesc(long tenantId);
}

