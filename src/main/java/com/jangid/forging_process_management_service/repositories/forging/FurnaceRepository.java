package com.jangid.forging_process_management_service.repositories.forging;

import com.jangid.forging_process_management_service.entities.forging.Furnace;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FurnaceRepository extends CrudRepository<Furnace, Long> {
  // Custom query methods (if needed) can be added here
  Page<Furnace> findByTenantIdAndDeletedIsFalseOrderByCreatedAtDesc(long tenantId, Pageable pageable);
  List<Furnace> findByTenantIdAndDeletedIsFalseOrderByCreatedAtDesc(long tenantId);
//  existsByTenantIdAndDeletedFalse
  boolean existsByTenantIdAndDeletedFalse(long tenantId);
  Optional<Furnace> findByIdAndTenantIdAndDeletedFalse(long id, long tenantId);
  
  // New methods for handling duplicate furnaceName and reactivating deleted furnaces
  boolean existsByFurnaceNameAndTenantIdAndDeletedFalse(String furnaceName, long tenantId);
  Optional<Furnace> findByFurnaceNameAndTenantIdAndDeletedTrue(String furnaceName, long tenantId);
}

