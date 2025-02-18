package com.jangid.forging_process_management_service.repositories.operator;

import com.jangid.forging_process_management_service.entities.operator.MachineOperator;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MachineOperatorRepository extends CrudRepository<MachineOperator, Long> {

  boolean existsByAadhaarNumberAndDeletedFalse(String aadhaarNumber);
  Optional<MachineOperator> findByAadhaarNumberAndDeletedFalse(String aadhaarNumber);

}
