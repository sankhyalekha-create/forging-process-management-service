package com.jangid.forging_process_management_service.repositories.machining;

import com.jangid.forging_process_management_service.entities.machining.MachineSet;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MachineSetRepository extends CrudRepository<MachineSet, Long> {
  @Query("SELECT DISTINCT ms FROM MachineSet ms JOIN ms.machines m WHERE m.tenant.id = :tenantId AND ms.deleted = false ORDER BY ms.createdAt DESC")
  Page<MachineSet> findByMachines_Tenant_IdOrderByCreatedAtDesc(@Param("tenantId") long tenantId, Pageable pageable);
  
  @Query("SELECT DISTINCT ms FROM MachineSet ms JOIN ms.machines m WHERE m.tenant.id = :tenantId AND ms.deleted = false ORDER BY ms.createdAt DESC")
  List<MachineSet> findByMachines_Tenant_IdOrderByCreatedAtDesc(long tenantId);

  boolean existsByMachines_Tenant_IdAndIdAndDeletedFalse(long tenantId, long id);

  Optional<MachineSet> findByMachines_Tenant_IdAndIdAndDeletedFalse(long tenantId, long id);

  Optional<MachineSet> findByIdAndDeletedFalse(long id);
}
