package com.jangid.forging_process_management_service.assemblers.machining;

import com.jangid.forging_process_management_service.entities.machining.MachineSet;
import com.jangid.forging_process_management_service.entitiesRepresentation.machining.MachineSetRepresentation;
import com.jangid.forging_process_management_service.service.machining.MachineSetService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Slf4j
@Component
public class MachineSetAssembler {

  @Autowired
  private MachineAssembler machineAssembler;

  @Autowired
  @Lazy
  private MachineSetService machineSetService;

  public MachineSetRepresentation dissemble(MachineSet machineSet) {
    if(machineSet==null){
      return null;
    }
    return MachineSetRepresentation.builder()
        .id(machineSet.getId())
        .machineSetName(machineSet.getMachineSetName())
        .machineSetDescription(machineSet.getMachineSetDescription())
        .machineSetRunningJobType(machineSet.getMachineSetRunningJobType().name())
        .machines(machineSet.getMachines().stream().map(machine -> machineAssembler.dissemble(machine)).collect(Collectors.toSet()))
        .build();
  }

  public MachineSet assemble(MachineSetRepresentation machineSetRepresentation) {
    if (machineSetRepresentation.getId() != null) {
      return machineSetService.getMachineSetUsingMachineSetId(machineSetRepresentation.getId());
    }
    
    return MachineSet.builder()
        .machineSetName(machineSetRepresentation.getMachineSetName())
        .machineSetDescription(machineSetRepresentation.getMachineSetDescription())
        .machineSetStatus(machineSetRepresentation.getMachineSetStatus() != null ? MachineSet.MachineSetStatus.valueOf(machineSetRepresentation.getMachineSetStatus()) : null)
        .machineSetRunningJobType(machineSetRepresentation.getMachineSetRunningJobType() != null ? MachineSet.MachineSetRunningJobType.valueOf(machineSetRepresentation.getMachineSetRunningJobType()) : null)
        .machines(machineSetRepresentation.getMachines() != null ? machineSetRepresentation.getMachines().stream().map(machineRepresentation -> machineAssembler.assemble(machineRepresentation))
            .collect(Collectors.toSet()) : null)
        .build();
  }

  public MachineSet createAssemble(MachineSetRepresentation machineSetRepresentation) {
    return MachineSet.builder()
        .machineSetName(machineSetRepresentation.getMachineSetName())
        .machineSetDescription(machineSetRepresentation.getMachineSetDescription())
        .machineSetStatus(machineSetRepresentation.getMachineSetStatus() != null ? MachineSet.MachineSetStatus.valueOf(machineSetRepresentation.getMachineSetStatus()) : null)
        .machineSetRunningJobType(machineSetRepresentation.getMachineSetRunningJobType() != null ? MachineSet.MachineSetRunningJobType.valueOf(machineSetRepresentation.getMachineSetRunningJobType()) : null)
        .build();
  }

}
