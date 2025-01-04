package com.jangid.forging_process_management_service.service.machining;

import com.jangid.forging_process_management_service.assemblers.machining.MachineSetAssembler;
import com.jangid.forging_process_management_service.entities.machining.Machine;
import com.jangid.forging_process_management_service.entities.machining.MachineSet;
import com.jangid.forging_process_management_service.entitiesRepresentation.machining.MachineSetRepresentation;
import com.jangid.forging_process_management_service.exception.ResourceNotFoundException;
import com.jangid.forging_process_management_service.repositories.machining.MachineSetRepository;
import com.jangid.forging_process_management_service.service.TenantService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Service
public class MachineSetService {

  @Autowired
  private MachineSetRepository machineSetRepository;

  @Autowired
  private TenantService tenantService;

  @Autowired
  private MachineService machineService;

  @Autowired
  private MachineSetAssembler machineSetAssembler;

  public MachineSetRepresentation createMachineSet(long tenantId, MachineSetRepresentation machineSetRepresentation) {
    tenantService.isTenantExists(tenantId);
    MachineSet machineSet = machineSetAssembler.createAssemble(machineSetRepresentation);
    machineSet.setCreatedAt(LocalDateTime.now());
    Set<Machine> machines = new HashSet<>();
    machineSetRepresentation.getMachines().forEach(machineRepresentation -> {
      boolean isMachineExists = machineService.isMachineOfTenantHavingMatchineNameExists(tenantId, machineRepresentation.getMachineName());
      if (!isMachineExists) {
        throw new ResourceNotFoundException("Machine not found having name=" + machineRepresentation.getMachineName() + " for the tenant=" + tenantId);
      }else {
        machines.add(machineService.getMachineByNameAndTenantId(machineRepresentation.getMachineName(), tenantId));
      }
    });
    machineSet.setMachines(machines);
    machineSet.setMachineSetStatus(MachineSet.MachineSetStatus.MACHINING_NOT_APPLIED);

    MachineSet createdMachineSet = machineSetRepository.save(machineSet);
    return machineSetAssembler.dissemble(createdMachineSet);
  }

  public Page<MachineSetRepresentation> getAllMachineSetsOfTenant(long tenantId, int page, int size) {
    Pageable pageable = PageRequest.of(page, size);
    Page<MachineSet> machineSetPage = machineSetRepository.findByMachines_Tenant_IdOrderByCreatedAtDesc(tenantId, pageable);
    return machineSetPage.map(machineSet -> machineSetAssembler.dissemble(machineSet));
  }

  public MachineSetRepresentation updateMachineSet(Long machineSetId, Long tenantId, MachineSetRepresentation machineSetRepresentation) {
    MachineSet machineSet = machineSetRepository.findByIdAndDeletedFalse(machineSetId)
        .orElseThrow(() -> new ResourceNotFoundException("Machine not found with machineSetId " + machineSetId + " of tenantId=" + tenantId));

    machineSetRepresentation.getMachines().forEach(machineRepresentation -> {
      boolean isMachineExists = machineService.isMachineOfTenantHavingMatchineNameExists(tenantId, machineRepresentation.getMachineName());
      if (!isMachineExists) {
        throw new ResourceNotFoundException("Machine not found having name=" + machineRepresentation.getMachineName() + " for the tenant=" + tenantId);
      }
    });
    // Update fields
    machineSet.getMachines().clear();
    machineSet.setMachines(machineSetRepresentation.getMachines().stream().map(machineRepresentation -> machineService.getMachineByNameAndTenantId(machineRepresentation.getMachineName(), tenantId)).collect(java.util.stream.Collectors.toSet()));

    if (!machineSet.getMachineSetName().equals(machineSetRepresentation.getMachineSetName())) {
      machineSet.setMachineSetName(machineSetRepresentation.getMachineSetName());
    }

    if (!machineSet.getMachineSetDescription().equals(machineSetRepresentation.getMachineSetDescription())) {
      machineSet.setMachineSetDescription(machineSetRepresentation.getMachineSetDescription());
    }

    MachineSet updatedMachineSet = machineSetRepository.save(machineSet);
    return machineSetAssembler.dissemble(updatedMachineSet);
  }
}
