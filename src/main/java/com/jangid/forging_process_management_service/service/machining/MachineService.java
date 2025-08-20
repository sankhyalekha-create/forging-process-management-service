package com.jangid.forging_process_management_service.service.machining;

import com.jangid.forging_process_management_service.assemblers.machining.MachineAssembler;
import com.jangid.forging_process_management_service.entities.Tenant;
import com.jangid.forging_process_management_service.entities.machining.Machine;
import com.jangid.forging_process_management_service.entitiesRepresentation.machining.MachineListRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.machining.MachineRepresentation;
import com.jangid.forging_process_management_service.exception.machining.MachineNotFoundException;
import com.jangid.forging_process_management_service.repositories.machining.MachineRepository;
import com.jangid.forging_process_management_service.service.TenantService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
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
public class MachineService {

  @Autowired
  private MachineRepository machineRepository;

  @Autowired
  private TenantService tenantService;

  @Autowired
  private MachineAssembler machineAssembler;

  @Transactional
  public MachineRepresentation createMachine(Long tenantId, MachineRepresentation machineRepresentation) {
    // First check if an active (not deleted) machine with the same name exists
    boolean existsByNameNotDeleted = machineRepository.existsMachineByMachineNameAndTenantIdAndDeletedFalse(
        machineRepresentation.getMachineName(), tenantId);
    if (existsByNameNotDeleted) {
      log.error("Active machine with name: {} already exists for tenant: {}!", 
                machineRepresentation.getMachineName(), tenantId);
      throw new IllegalStateException("Machine with name=" + machineRepresentation.getMachineName() 
                                     + " already exists");
    }
    
    // Check if we're trying to revive a deleted machine
    Machine machine = null;
    Optional<Machine> deletedMachine = machineRepository.findByMachineNameAndTenantIdAndDeletedTrue(
        machineRepresentation.getMachineName(), tenantId);
    
    if (deletedMachine.isPresent()) {
      // We found a deleted machine with the same name, reactivate it
      log.info("Reactivating previously deleted machine with name: {}", 
               machineRepresentation.getMachineName());
      machine = deletedMachine.get();
      machine.setDeleted(false);
      machine.setDeletedAt(null);
      
      // Update machine fields from the representation
      machine.setMachineName(machineRepresentation.getMachineName());
      machine.setMachineLocation(machineRepresentation.getMachineLocation());
      machine.setMachineDetails(machineRepresentation.getMachineDetails());
    } else {
      // Create new machine
      machine = machineAssembler.assemble(machineRepresentation);
      machine.setCreatedAt(LocalDateTime.now());
      Tenant tenant = tenantService.getTenantById(tenantId);
      machine.setTenant(tenant);
    }
    
    Machine savedMachine = machineRepository.save(machine);
    return machineAssembler.dissemble(savedMachine);
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
        .orElseThrow(() -> new MachineNotFoundException("Machine not found with id " + machineId + " of tenantId=" + tenantId));
  }

  public Machine getMachineByNameAndTenantId(String machineName, long tenantId) {
    return machineRepository.findByMachineNameAndTenantIdAndDeletedFalse(machineName, tenantId)
        .orElseThrow(() -> new MachineNotFoundException("Machine not found with machineName " + machineName + " of tenantId=" + tenantId));
  }

  public MachineListRepresentation getAllMachinesOfTenantWithoutPagination(long tenantId) {
    List<Machine> machines = machineRepository.findByTenantIdAndDeletedIsFalseOrderByCreatedAtDesc(tenantId);
    return MachineListRepresentation.builder()
        .machines(machines.stream().map(machine -> machineAssembler.dissemble(machine)).toList()).build();
  }

  @Transactional
  public void deleteMachine(Long machineId, Long tenantId) {
    // 1. Validate if tenant exists
    tenantService.validateTenantExists(tenantId);

    // 2. Validate if machine exists
    Machine machine = getMachineByIdAndTenantId(machineId, tenantId);

    // 3. Validate machine is not part of any MachineSet
    if (machineRepository.isMachineInAnyMachineSet(machineId)) {
      throw new IllegalStateException("Cannot delete machine with id " + machineId +
          " as it is part of a MachineSet. Remove it from the MachineSet first.");
    }

    // 4. Soft delete the machine
    machine.setDeleted(true);
    machine.setDeletedAt(LocalDateTime.now());
    machineRepository.save(machine);

    log.info("Machine with id {} of tenant {} has been soft deleted", machineId, tenantId);
  }

  /**
   * Search for machines by machine name with pagination
   * @param tenantId The tenant ID
   * @param searchType The type of search (currently only MACHINE_NAME is supported)
   * @param searchTerm The search term (substring matching for machine name)
   * @param page The page number (0-based)
   * @param size The page size
   * @return Page of MachineRepresentation containing the search results
   */
  @Transactional(readOnly = true)
  public Page<MachineRepresentation> searchMachines(Long tenantId, String searchType, String searchTerm, int page, int size) {
    if (searchTerm == null || searchTerm.trim().isEmpty() || searchType == null || searchType.trim().isEmpty()) {
      Pageable pageable = PageRequest.of(page, size);
      return Page.empty(pageable);
    }

    tenantService.validateTenantExists(tenantId);
    Pageable pageable = PageRequest.of(page, size);
    Page<Machine> machinePage;
    
    switch (searchType.toUpperCase()) {
      case "MACHINE_NAME":
        machinePage = machineRepository.findMachinesByMachineNameContainingIgnoreCase(tenantId, searchTerm.trim(), pageable);
        break;
      default:
        log.error("Invalid search type: {}", searchType);
        throw new IllegalArgumentException("Invalid search type: " + searchType + ". Valid types are: MACHINE_NAME");
    }
    
    return machinePage.map(machineAssembler::dissemble);
  }
}
