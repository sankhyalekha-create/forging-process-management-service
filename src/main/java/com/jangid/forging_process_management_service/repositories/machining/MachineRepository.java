package com.jangid.forging_process_management_service.repositories.machining;

import com.jangid.forging_process_management_service.entities.machining.Machine;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MachineRepository extends CrudRepository<Machine, Long> {
  Page<Machine> findByTenantIdAndDeletedIsFalseOrderByCreatedAtDesc(long tenantId, Pageable pageable);
  List<Machine> findByTenantIdAndDeletedIsFalseOrderByCreatedAtDesc(long tenantId);

  Optional<Machine> findByIdAndTenantIdAndDeletedFalse(long id, long tenantId);
  Optional<Machine> findByMachineNameAndTenantIdAndDeletedFalse(String machineName, long tenantId);
  boolean existsMachineByMachineNameAndTenantIdAndDeletedFalse(String machineName, long tenantId);

  @Query("SELECT m FROM Machine m " +
         "WHERE m.tenant.id = :tenantId " +
         "AND m.deleted = false " +
         "AND m.id NOT IN (" +
         "   SELECT m2.id " +
         "   FROM MachineSet ms " +
         "   JOIN ms.machines m2" +
         ")")
  List<Machine> findMachinesNotInAnyMachineSetForTenantAndNotDeleted(@Param("tenantId") Long tenantId);

  @Query("SELECT CASE WHEN COUNT(ms) > 0 THEN true ELSE false END " +
         "FROM MachineSet ms " +
         "JOIN ms.machines m " +
         "WHERE m.id = :machineId")
  boolean isMachineInAnyMachineSet(@Param("machineId") Long machineId);

}
