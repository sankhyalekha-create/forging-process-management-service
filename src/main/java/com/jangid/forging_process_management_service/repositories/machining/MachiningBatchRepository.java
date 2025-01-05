package com.jangid.forging_process_management_service.repositories.machining;

import com.jangid.forging_process_management_service.entities.machining.MachiningBatch;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MachiningBatchRepository extends CrudRepository<MachiningBatch, Long> {
// findAppliedMachiningBatchOnMachineSet

  @Query(value = "select * FROM machining_batch mb "
                 + "where mb.machine_set = :machineSetId and mb.deleted=false and mb.machining_batch_status != '2'"
                 + "order by mb.created_at desc LIMIT 1", nativeQuery = true)
  Optional<MachiningBatch> findAppliedMachiningBatchOnMachineSet(@Param("machineSetId") long machineSetId);

  // findByIdAndDeletedFalse
  Optional<MachiningBatch> findByIdAndDeletedFalse(long id);
}
