package com.jangid.forging_process_management_service.service.operator;

import com.jangid.forging_process_management_service.assemblers.operator.MachineOperatorAssembler;
import com.jangid.forging_process_management_service.entities.operator.MachineOperator;
import com.jangid.forging_process_management_service.entitiesRepresentation.operator.MachineOperatorListRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.operator.MachineOperatorRepresentation;
import com.jangid.forging_process_management_service.exception.operator.MachineOperatorNotFoundException;
import com.jangid.forging_process_management_service.repositories.operator.MachineOperatorRepository;
import com.jangid.forging_process_management_service.service.TenantService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MachineOperatorService {

  private final MachineOperatorRepository machineOperatorRepository;
  private final MachineOperatorAssembler machineOperatorAssembler;


  public boolean isValidAadhaarNumber(String aadhaarNumber) {
    return aadhaarNumber != null && aadhaarNumber.matches("\\d{12}");
  }

  public MachineOperator getMachineOperatorById(long id){
    Optional<MachineOperator> machineOperatorOptional = machineOperatorRepository.findByIdAndDeletedFalse(id);
    if (machineOperatorOptional.isEmpty()) {
      log.error("MachineOperator does not exists for id={}", id);
      throw new MachineOperatorNotFoundException("MachineOperator does not exists for id=" + id);
    }
    return machineOperatorOptional.get();
  }

  public MachineOperatorListRepresentation getAllMachineOperatorsOfTenantWithoutPagination(long tenantId) {
    List<MachineOperator> machineOperators = machineOperatorRepository.findByTenantIdAndDeletedIsFalseOrderByUpdatedAtDesc(tenantId);
    return MachineOperatorListRepresentation.builder()
        .machineOperators(machineOperators.stream().map(machineOperator -> machineOperatorAssembler.dissemble(machineOperator)).toList()).build();
  }

  public Page<MachineOperatorRepresentation> getAllMachineOperatorsOfTenant(long tenantId, int page, int size) {
    Pageable pageable = PageRequest.of(page, size);
    Page<MachineOperator> machineOperatorPage = machineOperatorRepository.findByTenantIdAndDeletedIsFalseOrderByUpdatedAtDesc(tenantId, pageable);
    return machineOperatorPage.map(machineOperator -> machineOperatorAssembler.dissemble(machineOperator));
  }

  // getAllMachineOperatorsOfTenantAvailableForMachining
  public MachineOperatorListRepresentation getAllMachineOperatorsOfTenantAvailableForMachining(LocalDateTime startTime, LocalDateTime endTime, long tenantId) {
    List<MachineOperator> machineOperators = machineOperatorRepository.findNonDeletedMachineOperatorsWithoutDailyMachiningBatchForPeriod(startTime, endTime, tenantId);
    return MachineOperatorListRepresentation.builder()
        .machineOperators(machineOperators.stream().map(machineOperator -> machineOperatorAssembler.dissemble(machineOperator)).toList()).build();
  }

  private MachineOperator getMachineOperatorByIdAndTenantId(Long id, Long tenantId) {
    return machineOperatorRepository.findByIdAndTenantId(id, tenantId)
        .orElseThrow(() -> new RuntimeException("MachineOperator not found for the given tenant!"));
  }

  @Transactional
  public MachineOperator save(MachineOperator machineOperator){
    return machineOperatorRepository.save(machineOperator);
  }
}
