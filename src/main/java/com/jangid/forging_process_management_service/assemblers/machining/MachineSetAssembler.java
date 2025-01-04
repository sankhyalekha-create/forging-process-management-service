package com.jangid.forging_process_management_service.assemblers.machining;

import com.jangid.forging_process_management_service.entities.machining.MachineSet;
import com.jangid.forging_process_management_service.entitiesRepresentation.machining.MachineSetRepresentation;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Slf4j
@Component
public class MachineSetAssembler {

  @Autowired
  private MachineAssembler machineAssembler;

  public MachineSetRepresentation dissemble(MachineSet machineSet) {
    return MachineSetRepresentation.builder()
        .id(machineSet.getId())
        .machineSetName(machineSet.getMachineSetName())
        .machineSetDescription(machineSet.getMachineSetDescription())
        .machineSetStatus(machineSet.getMachineSetStatus().name())
        .machines(machineSet.getMachines().stream().map(machine -> machineAssembler.dissemble(machine)).collect(Collectors.toSet()))
        .build();
  }

  public MachineSet assemble(MachineSetRepresentation machineSetRepresentation) {
    return MachineSet.builder()
        .machineSetName(machineSetRepresentation.getMachineSetName())
        .machineSetDescription(machineSetRepresentation.getMachineSetDescription())
        .machineSetStatus(machineSetRepresentation.getMachineSetStatus() != null ? MachineSet.MachineSetStatus.valueOf(machineSetRepresentation.getMachineSetStatus()) : null)
        .machines(machineSetRepresentation.getMachines().stream().map(machineRepresentation -> machineAssembler.assemble(machineRepresentation)).collect(Collectors.toSet()))
        .build();
  }

  public MachineSet createAssemble(MachineSetRepresentation machineSetRepresentation) {
    return MachineSet.builder()
        .machineSetName(machineSetRepresentation.getMachineSetName())
        .machineSetDescription(machineSetRepresentation.getMachineSetDescription())
        .machineSetStatus(machineSetRepresentation.getMachineSetStatus() != null ? MachineSet.MachineSetStatus.valueOf(machineSetRepresentation.getMachineSetStatus()) : null)
        .build();
  }

}
