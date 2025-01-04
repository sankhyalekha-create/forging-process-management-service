package com.jangid.forging_process_management_service.repositories.machining;

import com.jangid.forging_process_management_service.entities.machining.MachineSet;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MachineSetRepository extends CrudRepository<MachineSet, Long> {
  Page<MachineSet> findByMachines_Tenant_IdOrderByCreatedAtDesc(long tenantId, Pageable pageable);

  Optional<MachineSet> findByIdAndDeletedFalse(long id);
}
