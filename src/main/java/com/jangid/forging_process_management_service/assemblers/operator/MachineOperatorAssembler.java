package com.jangid.forging_process_management_service.assemblers.operator;

import com.jangid.forging_process_management_service.assemblers.machining.DailyMachiningBatchAssembler;
import com.jangid.forging_process_management_service.entities.operator.MachineOperator;
import com.jangid.forging_process_management_service.entities.operator.Operator;
import com.jangid.forging_process_management_service.entitiesRepresentation.operator.MachineOperatorRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.operator.OperatorRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.operator.OperatorType;
import com.jangid.forging_process_management_service.service.operator.MachineOperatorService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;


@Slf4j
@Component
public class MachineOperatorAssembler {

  private final MachineOperatorService machineOperatorService;
  private final OperatorAssembler operatorAssembler;
  private final DailyMachiningBatchAssembler dailyMachiningBatchAssembler;

  @Autowired
  public MachineOperatorAssembler(@Lazy MachineOperatorService machineOperatorService, OperatorAssembler operatorAssembler, @Lazy DailyMachiningBatchAssembler dailyMachiningBatchAssembler) {
    this.machineOperatorService = machineOperatorService;
    this.operatorAssembler = operatorAssembler;
    this.dailyMachiningBatchAssembler = dailyMachiningBatchAssembler;
  }

  public MachineOperator createAssemble(MachineOperatorRepresentation representation) {
    MachineOperator machineOperator = assemble(representation);

    machineOperator.setCreatedAt(LocalDateTime.now());
    return machineOperator;
  }

  public MachineOperator assemble(MachineOperatorRepresentation representation) {
    if (representation.getOperator().getId() != null) {
      return machineOperatorService.getMachineOperatorById(representation.getOperator().getId());
    }
    Operator operator = operatorAssembler.assemble(representation.getOperator());
    return MachineOperator.builder()
        .id(operator.getId())
        .fullName(operator.getFullName())
        .phoneNumber(operator.getPhoneNumber())
        .address(operator.getAddress())
        .aadhaarNumber(operator.getAadhaarNumber())
        .dateOfBirth(operator.getDateOfBirth())
        .dateOfJoining(operator.getDateOfJoining())
        .dateOfLeaving(operator.getDateOfLeaving())
        .hourlyWages(operator.getHourlyWages())
        .dailyMachiningBatches(
            representation.getDailyMachiningBatches().stream().map(dailyMachiningBatchRepresentation -> dailyMachiningBatchAssembler.assemble(dailyMachiningBatchRepresentation)).toList())
        .build();
  }

  public MachineOperator createAssemble(OperatorRepresentation representation) {
    MachineOperator machineOperator = assemble(representation);

    machineOperator.setCreatedAt(LocalDateTime.now());
    return machineOperator;
  }

  public MachineOperator assemble(OperatorRepresentation operatorRepresentation) {
    return MachineOperator.builder()
        .id(operatorRepresentation.getId())
        .fullName(operatorRepresentation.getFullName())
        .phoneNumber(operatorRepresentation.getPhoneNumber())
        .address(operatorRepresentation.getAddress())
        .aadhaarNumber(operatorRepresentation.getAadhaarNumber())
        .dateOfBirth(operatorRepresentation.getDateOfBirth())
        .dateOfJoining(operatorRepresentation.getDateOfJoining())
        .dateOfLeaving(operatorRepresentation.getDateOfLeaving())
        .hourlyWages(operatorRepresentation.getHourlyWages())
        .build();
  }

  public MachineOperatorRepresentation dissemble(MachineOperator machineOperator) {
    OperatorRepresentation operatorRepresentation = operatorAssembler.dissemble(machineOperator);
    operatorRepresentation.setOperatorType(OperatorType.MACHINING);
    return MachineOperatorRepresentation.builder()
        .operator(operatorRepresentation)
        .dailyMachiningBatches(machineOperator.getDailyMachiningBatches() != null ? machineOperator.getDailyMachiningBatches().stream().map(dailyMachiningBatch -> dailyMachiningBatchAssembler.dissemble(dailyMachiningBatch)).toList() : null)
        .build();
  }

  public MachineOperatorRepresentation dissembleWithoutDailyMachiningBatches(MachineOperator machineOperator) {
    OperatorRepresentation operatorRepresentation = operatorAssembler.dissemble(machineOperator);
    operatorRepresentation.setOperatorType(OperatorType.MACHINING);
    return MachineOperatorRepresentation.builder()
        .operator(operatorRepresentation)
        .build();
  }
}

