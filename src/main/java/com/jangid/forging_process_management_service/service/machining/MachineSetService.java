package com.jangid.forging_process_management_service.service.machining;

import com.jangid.forging_process_management_service.assemblers.machining.MachineSetAssembler;
import com.jangid.forging_process_management_service.entities.machining.Machine;
import com.jangid.forging_process_management_service.entities.machining.MachineSet;
import com.jangid.forging_process_management_service.entitiesRepresentation.machining.MachineSetRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.machining.MachineSetListRepresentation;
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
          throw new ResourceNotFoundException("Machine not found having name=" + machineRepresentation.getMachineName());
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
          throw new ResourceNotFoundException("Machine not found having name=" + machineRepresentation.getMachineName());
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

  /**
   * Get all machine sets that are available for the given time period
   * A machine set is considered available if it has no overlapping daily machining batches
   * during the specified time period
   */
  public MachineSetListRepresentation getAvailableMachineSetsForTimeRange(long tenantId, LocalDateTime startDateTime, LocalDateTime endDateTime) {
    tenantService.validateTenantExists(tenantId);
    
    List<MachineSet> availableMachineSets = machineSetRepository.findAvailableMachineSetsByTenantIdAndTimeRange(
        tenantId, startDateTime, endDateTime);
    
    List<MachineSetRepresentation> machineSetRepresentations = availableMachineSets.stream()
        .map(machineSetAssembler::dissemble)
        .toList();
    
    return MachineSetListRepresentation.builder()
        .machineSets(machineSetRepresentations)
        .build();
  }

  public boolean isMachineSetExistsUsingTenantIdAndMachineSetId(long tenantId, long machineSetId) {
    return machineSetRepository.existsByMachines_Tenant_IdAndIdAndDeletedFalse(tenantId, machineSetId);
  }

  public MachineSet getMachineSetUsingMachineSetId(long machineSetId) {
    Optional<MachineSet> machineSetOptional = machineSetRepository.findByIdAndDeletedFalse(machineSetId);
    if (machineSetOptional.isEmpty()) {
      log.error("MachineSet={} does not exist!", machineSetId);
      throw new ResourceNotFoundException("MachineSet does not exist!");
    }
    return machineSetOptional.get();
  }

  public MachineSet getMachineSetUsingTenantIdAndMachineSetId(long tenantId, long machineSetId) {
    Optional<MachineSet> machineSetOptional = machineSetRepository.findByMachines_Tenant_IdAndIdAndDeletedFalse(tenantId, machineSetId);
    if (machineSetOptional.isEmpty()) {
      log.error("MachineSet={} does not exist!", machineSetId);
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
        throw new ResourceNotFoundException("Machine not found having name=" + machineRepresentation.getMachineName());
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

  /**
   * Search for machine sets by various criteria with pagination
   * @param tenantId The tenant ID
   * @param searchType The type of search (MACHINE_SET_NAME or MACHINE_NAME)
   * @param searchTerm The search term (substring matching for both search types)
   * @param page The page number (0-based)
   * @param size The page size
   * @return Page of MachineSetRepresentation containing the search results
   */
  @Transactional(readOnly = true)
  public Page<MachineSetRepresentation> searchMachineSets(Long tenantId, String searchType, String searchTerm, int page, int size) {
    if (searchTerm == null || searchTerm.trim().isEmpty() || searchType == null || searchType.trim().isEmpty()) {
      Pageable pageable = PageRequest.of(page, size);
      return Page.empty(pageable);
    }

    tenantService.validateTenantExists(tenantId);
    Pageable pageable = PageRequest.of(page, size);
    Page<MachineSet> machineSetPage;
    
    switch (searchType.toUpperCase()) {
      case "MACHINE_SET_NAME":
        machineSetPage = machineSetRepository.findMachineSetsByMachineSetNameContainingIgnoreCase(tenantId, searchTerm.trim(), pageable);
        break;
      case "MACHINE_NAME":
        machineSetPage = machineSetRepository.findMachineSetsByMachineNameContainingIgnoreCase(tenantId, searchTerm.trim(), pageable);
        break;
      default:
        log.error("Invalid search type: {}", searchType);
        throw new IllegalArgumentException("Invalid search type: " + searchType + ". Valid types are: MACHINE_SET_NAME, MACHINE_NAME");
    }
    
    return machineSetPage.map(machineSetAssembler::dissemble);
  }
}
