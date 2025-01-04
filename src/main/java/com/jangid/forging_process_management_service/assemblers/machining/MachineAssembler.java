package com.jangid.forging_process_management_service.assemblers.machining;

import com.jangid.forging_process_management_service.entities.machining.Machine;
import com.jangid.forging_process_management_service.entitiesRepresentation.machining.MachineRepresentation;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MachineAssembler {

  public MachineRepresentation dissemble(Machine machine){
    return MachineRepresentation.builder()
        .id(machine.getId())
        .machineName(machine.getMachineName())
        .machineLocation(machine.getMachineLocation())
        .machineDetails(machine.getMachineDetails())
        .build();
  }

  public Machine assemble(MachineRepresentation machineRepresentation){
    return Machine.builder()
        .machineName(machineRepresentation.getMachineName())
        .machineLocation(machineRepresentation.getMachineLocation())
        .machineDetails(machineRepresentation.getMachineDetails())
        .build();
  }

}
