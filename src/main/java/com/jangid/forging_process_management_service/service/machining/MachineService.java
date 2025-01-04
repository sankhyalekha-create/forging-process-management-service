package com.jangid.forging_process_management_service.service.machining;

import com.jangid.forging_process_management_service.assemblers.machining.MachineAssembler;
import com.jangid.forging_process_management_service.entities.Tenant;
import com.jangid.forging_process_management_service.entities.machining.Machine;
import com.jangid.forging_process_management_service.entitiesRepresentation.machining.MachineListRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.machining.MachineRepresentation;
import com.jangid.forging_process_management_service.exception.ResourceNotFoundException;
import com.jangid.forging_process_management_service.repositories.machining.MachineRepository;
import com.jangid.forging_process_management_service.service.TenantService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class MachineService {

  @Autowired
  private MachineRepository machineRepository;

  @Autowired
  private TenantService tenantService;

  @Autowired
  private MachineAssembler machineAssembler;

  public MachineRepresentation createMachine(Long tenantId, MachineRepresentation machineRepresentation) {
    Machine machine = machineAssembler.assemble(machineRepresentation);
    machine.setCreatedAt(LocalDateTime.now());
    Tenant tenant = tenantService.getTenantById(tenantId);
    machine.setTenant(tenant);
    Machine createdMachine = machineRepository.save(machine);
    return machineAssembler.dissemble(createdMachine);
  }

  public Page<MachineRepresentation> getAllMachinesOfTenant(long tenantId, int page, int size) {
    Pageable pageable = PageRequest.of(page, size);
    Page<Machine> machinePage = machineRepository.findByTenantIdAndDeletedIsFalseOrderByCreatedAtDesc(tenantId, pageable);
    return machinePage.map(machine -> machineAssembler.dissemble(machine));
  }

  public MachineListRepresentation getAllMachinesAvailableForMachineSetOfTenant(long tenantId) {
    List<Machine> machines = machineRepository.findMachinesNotInAnyMachineSetForTenantAndNotDeleted(tenantId);
    return MachineListRepresentation.builder()
        .machines(machines.stream().map(machine -> machineAssembler.dissemble(machine)).toList()).build();
  }

  public MachineRepresentation updateMachine(Long id, Long tenantId, MachineRepresentation machineRepresentation) {
    Machine machine = getMachineByIdAndTenantId(id, tenantId);
    // Update fields
    if (!machine.getMachineName().equals(machineRepresentation.getMachineName())) {
      machine.setMachineName(machineRepresentation.getMachineName());
    }

    if (!machine.getMachineLocation().equals(machineRepresentation.getMachineLocation())) {
      machine.setMachineLocation(machineRepresentation.getMachineLocation());
    }

    if (!machine.getMachineDetails().equals(machineRepresentation.getMachineDetails())) {
      machine.setMachineDetails(machineRepresentation.getMachineDetails());
    }

    Machine updatedMachine = machineRepository.save(machine);
    return machineAssembler.dissemble(updatedMachine);
  }

  public boolean isMachineOfTenantHavingMatchineNameExists(long tenantId, String matchineName) {
    return machineRepository.existsMachineByMachineNameAndTenantIdAndDeletedFalse(matchineName, tenantId);
  }

//  getMachineByIdAndTenantId

  public Machine getMachineByIdAndTenantId(long machineId, long tenantId) {
    return machineRepository.findByIdAndTenantIdAndDeletedFalse(machineId, tenantId)
        .orElseThrow(() -> new ResourceNotFoundException("Machine not found with id " + machineId + " of tenantId=" + tenantId));
  }

  public Machine getMachineByNameAndTenantId(String machineName, long tenantId) {
    return machineRepository.findByMachineNameAndTenantIdAndDeletedFalse(machineName, tenantId)
        .orElseThrow(() -> new ResourceNotFoundException("Machine not found with machineName " + machineName + " of tenantId=" + tenantId));
  }

  public MachineListRepresentation getAllMachinesOfTenantWithoutPagination(long tenantId) {
    List<Machine> machines = machineRepository.findByTenantIdAndDeletedIsFalseOrderByCreatedAtDesc(tenantId);
    return MachineListRepresentation.builder()
        .machines(machines.stream().map(machine -> machineAssembler.dissemble(machine)).toList()).build();
  }
}
