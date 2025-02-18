package com.jangid.forging_process_management_service.assemblers.operator;

import com.jangid.forging_process_management_service.entities.operator.MachineOperator;
import com.jangid.forging_process_management_service.entitiesRepresentation.operator.MachineOperatorRepresentation;
import com.jangid.forging_process_management_service.service.machining.MachiningBatchService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;


@Slf4j
@Component
public class MachineOperatorAssembler {

  @Autowired
  private MachiningBatchService machiningBatchService;

  public MachineOperator createAssemble(MachineOperatorRepresentation representation) {
    MachineOperator machineOperator = assemble(representation);

    machineOperator.setCreatedAt(LocalDateTime.now());
    return machineOperator;
  }

  public MachineOperator assemble(MachineOperatorRepresentation representation) {
     MachineOperator machineOperator = MachineOperator.builder()
        .id(representation.getId())
        .fullName(representation.getFullName())
        .address(representation.getAddress())
        .aadhaarNumber(representation.getAadhaarNumber())
        .previousTenantIds(representation.getPreviousTenantIds() != null ? new ArrayList<>(representation.getPreviousTenantIds()) : new ArrayList<>())
        .build();
    machineOperator.setMachiningBatch(representation.getMachiningBatchId() != null ? machiningBatchService.getMachiningBatchById(representation.getMachiningBatchId()) : null);
    return machineOperator;
  }

  public MachineOperatorRepresentation dissemble(MachineOperator machineOperator) {
    return MachineOperatorRepresentation.builder()
        .id(machineOperator.getId())
        .fullName(machineOperator.getFullName())
        .address(machineOperator.getAddress())
        .aadhaarNumber(machineOperator.getAadhaarNumber())
        .tenantId(machineOperator.getTenant().getId())
        .previousTenantIds(machineOperator.getPreviousTenantIds() != null ? new ArrayList<>(machineOperator.getPreviousTenantIds()) : new ArrayList<>())
        .machiningBatchId(machineOperator.getMachiningBatch() != null ? machineOperator.getMachiningBatch().getId() : null)
        .build();
  }
}

