package com.jangid.forging_process_management_service.service.operator;

import com.jangid.forging_process_management_service.assemblers.operator.MachineOperatorAssembler;
import com.jangid.forging_process_management_service.entities.operator.MachineOperator;
import com.jangid.forging_process_management_service.entitiesRepresentation.operator.MachineOperatorRepresentation;
import com.jangid.forging_process_management_service.repositories.operator.MachineOperatorRepository;
import com.jangid.forging_process_management_service.service.TenantService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MachineOperatorService {

  private final MachineOperatorRepository machineOperatorRepository;
  private final TenantService tenantService;
  private final MachineOperatorAssembler machineOperatorAssembler;

  public MachineOperatorRepresentation createMachineOperator(Long tenantId, MachineOperatorRepresentation machineOperatorRepresentation) {
    if (tenantId == null || machineOperatorRepresentation == null) {
      log.error("Invalid input: tenantId={}, machineOperatorRepresentation={}", tenantId, machineOperatorRepresentation);
      throw new IllegalArgumentException("Tenant ID and MachineOperatorRepresentation must not be null.");
    }

    String aadhaarNumber = machineOperatorRepresentation.getAadhaarNumber();
    if (!isValidAadhaarNumber(aadhaarNumber)) {
      log.error("Invalid Aadhaar number: {}", aadhaarNumber);
      throw new IllegalArgumentException("Invalid Aadhaar number. It must be a 12-digit numeric value.");
    }

    boolean exists = machineOperatorRepository.existsByAadhaarNumberAndDeletedFalse(aadhaarNumber);
    if (exists) {
      MachineOperator machineOperator = machineOperatorRepository.findByAadhaarNumberAndDeletedFalse(aadhaarNumber).get();
      log.error("MachineOperator with Aadhaar number {} already associated with the tenant {}!", aadhaarNumber, machineOperator.getTenant().getTenantName());
      throw new IllegalStateException("MachineOperator with this aadhaar number lready associated with the tenant="+machineOperator.getTenant().getTenantName());
    }

    MachineOperator machineOperator = machineOperatorAssembler.createAssemble(machineOperatorRepresentation);
    machineOperator.setTenant(tenantService.getTenantById(tenantId));

    MachineOperator savedMachineOperator = machineOperatorRepository.save(machineOperator);
    log.info("MachineOperator created successfully with ID {}", savedMachineOperator.getId());

    return machineOperatorAssembler.dissemble(savedMachineOperator);
  }


  public boolean isValidAadhaarNumber(String aadhaarNumber) {
    return aadhaarNumber != null && aadhaarNumber.matches("\\d{12}");
  }
}
