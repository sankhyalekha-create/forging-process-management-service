package com.jangid.forging_process_management_service.repositories.machining;

import com.jangid.forging_process_management_service.entities.machining.DailyMachiningBatch;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DailyMachiningBatchRepository extends CrudRepository<DailyMachiningBatch, Long> {

}
