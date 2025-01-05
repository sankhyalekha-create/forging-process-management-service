package com.jangid.forging_process_management_service.repositories.machining;

import com.jangid.forging_process_management_service.entities.machining.DailyMachiningBatchDetail;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DailyMachiningBatchDetailRepository extends CrudRepository<DailyMachiningBatchDetail, Long> {

}
