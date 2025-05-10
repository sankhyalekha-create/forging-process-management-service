package com.jangid.forging_process_management_service.service.machining;

import com.jangid.forging_process_management_service.assemblers.machining.MachineSetAssembler;
import com.jangid.forging_process_management_service.entities.machining.Machine;
import com.jangid.forging_process_management_service.entities.machining.MachineSet;
import com.jangid.forging_process_management_service.entitiesRepresentation.machining.MachineSetRepresentation;
import com.jangid.forging_process_management_service.exception.ResourceNotFoundException;
import com.jangid.forging_process_management_service.exception.machining.MachineSetNotFoundException;
import com.jangid.forging_process_management_service.repositories.machining.MachineSetRepository;
import com.jangid.forging_process_management_service.service.TenantService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
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

  @Transactional
  public MachineSetRepresentation createMachineSet(long tenantId, MachineSetRepresentation machineSetRepresentation) {
    tenantService.isTenantExists(tenantId);
    
    // First check if an active (not deleted) machine set with the same name exists
    boolean existsByNameNotDeleted = machineSetRepository.existsByMachineSetNameAndTenantIdAndDeletedFalse(
        machineSetRepresentation.getMachineSetName(), tenantId);
    if (existsByNameNotDeleted) {
      log.error("Active machine set with name: {} already exists for tenant: {}!", 
                machineSetRepresentation.getMachineSetName(), tenantId);
      throw new IllegalStateException("Machine set with name=" + machineSetRepresentation.getMachineSetName() 
                                     + " already exists for tenant=" + tenantId);
    }
    
    // Check if we're trying to revive a deleted machine set
    MachineSet machineSet = null;
    Optional<MachineSet> deletedMachineSet = machineSetRepository.findByMachineSetNameAndDeletedTrue(
        machineSetRepresentation.getMachineSetName());
    
    if (deletedMachineSet.isPresent()) {
      // We found a deleted machine set with the same name, reactivate it
      log.info("Reactivating previously deleted machine set with name: {}", 
               machineSetRepresentation.getMachineSetName());
      machineSet = deletedMachineSet.get();
      machineSet.setDeleted(false);
      machineSet.setDeletedAt(null);
      
      // Update machine set fields from the representation
      machineSet.setMachineSetName(machineSetRepresentation.getMachineSetName());
      machineSet.setMachineSetDescription(machineSetRepresentation.getMachineSetDescription());
      
      // Clear existing machines and add new ones
      machineSet.getMachines().clear();
      
      // Add machines from the representation
      Set<Machine> machines = new HashSet<>();
      machineSetRepresentation.getMachines().forEach(machineRepresentation -> {
        boolean isMachineExists = machineService.isMachineOfTenantHavingMatchineNameExists(tenantId, machineRepresentation.getMachineName());
        if (!isMachineExists) {
          throw new ResourceNotFoundException("Machine not found having name=" + machineRepresentation.getMachineName() + " for the tenant=" + tenantId);
        } else {
          machines.add(machineService.getMachineByNameAndTenantId(machineRepresentation.getMachineName(), tenantId));
        }
      });
      machineSet.setMachines(machines);
      
      // Reset status
      machineSet.setMachineSetStatus(MachineSet.MachineSetStatus.MACHINING_NOT_APPLIED);
      machineSet.setMachineSetRunningJobType(MachineSet.MachineSetRunningJobType.NONE);
    } else {
      // Create new machine set
      machineSet = machineSetAssembler.createAssemble(machineSetRepresentation);
      machineSet.setCreatedAt(LocalDateTime.now());
      Set<Machine> machines = new HashSet<>();
      machineSetRepresentation.getMachines().forEach(machineRepresentation -> {
        boolean isMachineExists = machineService.isMachineOfTenantHavingMatchineNameExists(tenantId, machineRepresentation.getMachineName());
        if (!isMachineExists) {
          throw new ResourceNotFoundException("Machine not found having name=" + machineRepresentation.getMachineName() + " for the tenant=" + tenantId);
        } else {
          machines.add(machineService.getMachineByNameAndTenantId(machineRepresentation.getMachineName(), tenantId));
        }
      });
      machineSet.setMachines(machines);
      machineSet.setMachineSetStatus(MachineSet.MachineSetStatus.MACHINING_NOT_APPLIED);
      machineSet.setMachineSetRunningJobType(MachineSet.MachineSetRunningJobType.NONE);
    }

    MachineSet createdMachineSet = machineSetRepository.save(machineSet);
    return machineSetAssembler.dissemble(createdMachineSet);
  }

  public Page<MachineSetRepresentation> getAllMachineSetPagesOfTenant(long tenantId, int page, int size) {
    Pageable pageable = PageRequest.of(page, size);
    Page<MachineSet> machineSetPage = machineSetRepository.findByMachines_Tenant_IdOrderByCreatedAtDesc(tenantId, pageable);
    return machineSetPage.map(machineSet -> machineSetAssembler.dissemble(machineSet));
  }

  public List<MachineSet> getAllMachineSetsOfTenant(long tenantId) {
    return machineSetRepository.findByMachines_Tenant_IdOrderByCreatedAtDesc(tenantId);
  }


  public boolean isMachineSetExistsUsingTenantIdAndMachineSetId(long tenantId, long machineSetId) {
    return machineSetRepository.existsByMachines_Tenant_IdAndIdAndDeletedFalse(tenantId, machineSetId);
  }

  public MachineSet getMachineSetUsingTenantIdAndMachineSetId(long tenantId, long machineSetId) {
    Optional<MachineSet> machineSetOptional = machineSetRepository.findByMachines_Tenant_IdAndIdAndDeletedFalse(tenantId, machineSetId);
    if (machineSetOptional.isEmpty()) {
      log.error("MachineSet={} for the tenant={} does not exist!", machineSetId, tenantId);
      throw new ResourceNotFoundException("MachineSet for the tenant does not exist!");
    }
    return machineSetOptional.get();
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
    machineSet.setMachines(machineSetRepresentation.getMachines().stream().map(machineRepresentation -> machineService.getMachineByNameAndTenantId(machineRepresentation.getMachineName(), tenantId))
                               .collect(java.util.stream.Collectors.toSet()));

    if (!machineSet.getMachineSetName().equals(machineSetRepresentation.getMachineSetName())) {
      machineSet.setMachineSetName(machineSetRepresentation.getMachineSetName());
    }

    if (!machineSet.getMachineSetDescription().equals(machineSetRepresentation.getMachineSetDescription())) {
      machineSet.setMachineSetDescription(machineSetRepresentation.getMachineSetDescription());
    }

    MachineSet updatedMachineSet = machineSetRepository.save(machineSet);
    return machineSetAssembler.dissemble(updatedMachineSet);
  }

  // updateMachineSetStatus
  @Transactional
  public void updateMachineSetStatus(MachineSet machineSet, MachineSet.MachineSetStatus machineSetStatus) {
    machineSet.setMachineSetStatus(machineSetStatus);
    machineSetRepository.save(machineSet);
  }

  @Transactional
  public void updateMachineSetRunningJobType(MachineSet machineSet, MachineSet.MachineSetRunningJobType machineSetRunningJobType) {
    machineSet.setMachineSetRunningJobType(machineSetRunningJobType);
    machineSetRepository.save(machineSet);
  }

  @Transactional
  public void deleteMachineSet(Long machineSetId, Long tenantId) {
    // Validate tenant exists
    tenantService.isTenantExists(tenantId);

    // Get and validate machineSet exists
    MachineSet machineSet = machineSetRepository.findByMachines_Tenant_IdAndIdAndDeletedFalse(tenantId, machineSetId)
        .orElseThrow(() -> new MachineSetNotFoundException("MachineSet not found with id=" + machineSetId + " for tenant=" + tenantId));

    // Validate machineSet status
    if (machineSet.getMachineSetStatus() != MachineSet.MachineSetStatus.MACHINING_NOT_APPLIED) {
      log.error("Cannot delete MachineSet={} as it is in {} status", machineSetId, machineSet.getMachineSetStatus());
      throw new IllegalStateException("Cannot delete MachineSet as it is not in MACHINING_NOT_APPLIED status");
    }

    LocalDateTime currentTimestamp = LocalDateTime.now();

    // Soft delete the MachineSet
    machineSet.setDeleted(true);
    machineSet.setDeletedAt(currentTimestamp);

    // Clear and soft delete machine associations
    machineSet.getMachines().clear();

    machineSetRepository.save(machineSet);
  }
}
