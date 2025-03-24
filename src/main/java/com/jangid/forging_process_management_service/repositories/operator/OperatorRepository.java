package com.jangid.forging_process_management_service.repositories.operator;

import com.jangid.forging_process_management_service.entities.operator.MachineOperator;
import com.jangid.forging_process_management_service.entities.operator.Operator;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OperatorRepository extends CrudRepository<Operator, Long> {

  Optional<Operator> findByIdAndTenantId(Long id, Long tenantId);

//  Page<MachineOperator> findByTenantIdAndDeletedFalse(Long tenantId, PageRequest pageRequest);
//  List<MachineOperator> findByTenantIdAndDeletedFalse(Long tenantId);

  boolean existsByIdAndTenantId(Long id, Long tenantId);
  boolean existsByAadhaarNumberAndDeletedFalse(String aadhaarNumber);
  Optional<Operator> findByAadhaarNumberAndDeletedFalse(String aadhaarNumber);
  Optional<Operator> findByIdAndDeletedFalse(long id);
  List<Operator> findByTenantIdAndDeletedIsFalseOrderByCreatedAtDesc(long tenantId);
  Page<Operator> findByTenantIdAndDeletedIsFalseOrderByCreatedAtDesc(long tenantId, Pageable pageable);

  List<Operator> findByFullNameContainingIgnoreCaseAndTenantId(String fullName, Long tenantId);

  List<Operator> findByAadhaarNumberAndTenantId(String aadhaarNumber, Long tenantId);

  List<MachineOperator> findAllByTenantIdAndDeletedFalse(Long tenantId);

}
