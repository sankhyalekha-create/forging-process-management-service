package com.jangid.forging_process_management_service.service.heating;

import com.jangid.forging_process_management_service.assemblers.heating.FurnaceAssembler;
import com.jangid.forging_process_management_service.entities.Tenant;
import com.jangid.forging_process_management_service.entities.forging.Furnace;
import com.jangid.forging_process_management_service.entitiesRepresentation.forging.FurnaceRepresentation;
import com.jangid.forging_process_management_service.exception.ResourceNotFoundException;
import com.jangid.forging_process_management_service.repositories.forging.FurnaceRepository;
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
public class FurnaceService {

  @Autowired
  private FurnaceRepository furnaceRepository;

  @Autowired
  private TenantService tenantService;

  @Autowired
  private FurnaceAssembler furnaceAssembler;

  public Page<FurnaceRepresentation> getAllFurnacesOfTenant(long tenantId, int page, int size) {
    Pageable pageable = PageRequest.of(page, size);
    Page<Furnace> furnacePage = furnaceRepository.findByTenantIdAndDeletedIsFalseOrderByCreatedAtDesc(tenantId, pageable);
    return furnacePage.map(furnaceAssembler::dissemble);
  }

  public List<Furnace> getAllFurnacesOfTenant(long tenantId) {
    return furnaceRepository.findByTenantIdAndDeletedIsFalseOrderByCreatedAtDesc(tenantId);
  }

  public Optional<Furnace> getFurnaceById(Long id) {
    return furnaceRepository.findById(id);
  }
  public boolean isFurnaceByTenantExists(Long tenantId) {
    return furnaceRepository.existsByTenantIdAndDeletedFalse(tenantId);
  }

  public Furnace getFurnaceByIdAndTenantId(long furnaceIdLongValue, long tenantLongId) {
    Optional<Furnace> furnaceOptional = furnaceRepository.findByIdAndTenantIdAndDeletedFalse(furnaceIdLongValue, tenantLongId);
    if (furnaceOptional.isEmpty()) {
      log.error("Furnace with id=" + furnaceIdLongValue + " having " + tenantLongId + " not found!");
      throw new ResourceNotFoundException("Furnace with id=" + furnaceIdLongValue + " having " + tenantLongId + " not found!");
    }
    return furnaceOptional.get();
  }

  public FurnaceRepresentation createFurnace(Long tenantId, FurnaceRepresentation furnaceRepresentation) {
    Furnace furnace = furnaceAssembler.assemble(furnaceRepresentation);
    furnace.setCreatedAt(LocalDateTime.now());
    Tenant tenant = tenantService.getTenantById(tenantId);
    furnace.setTenant(tenant);
    Furnace createdFurnace = furnaceRepository.save(furnace);
    return furnaceAssembler.dissemble(createdFurnace);
  }

  public FurnaceRepresentation updateFurnace(Long id, Long tenantId, FurnaceRepresentation furnaceRepresentation) {
    Furnace furnace = furnaceRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
        .orElseThrow(() -> new ResourceNotFoundException("Furnace not found with id " + id +" of tenantId="+tenantId));

    // Update fields
    furnace.setFurnaceName(furnaceRepresentation.getFurnaceName());
    furnace.setFurnaceCapacity(Double.valueOf(furnaceRepresentation.getFurnaceCapacity()));
    furnace.setFurnaceLocation(furnaceRepresentation.getFurnaceLocation());
    furnace.setFurnaceDetails(furnaceRepresentation.getFurnaceDetails());
    Furnace updatedFurnace = furnaceRepository.save(furnace);
    return furnaceAssembler.dissemble(updatedFurnace);
  }

  @Transactional
  public Furnace saveFurnace(Furnace furnace) {
    return furnaceRepository.save(furnace);
  }

  @Transactional
  public void deleteFurnace(Long tenantId, Long furnaceId) {
    // 1. Validate tenant exists
    tenantService.isTenantExists(tenantId);

    // 2. Validate furnace exists
    Furnace furnace = getFurnaceByIdAndTenantId(furnaceId, tenantId);

    // 3. Validate furnace status
    if (furnace.getFurnaceStatus() != Furnace.FurnaceStatus.HEAT_TREATMENT_BATCH_NOT_APPLIED) {
      throw new IllegalStateException("This furnace cannot be deleted as it is not in the HEAT_TREATMENT_BATCH_NOT_APPLIED status.");
    }

    // Finally, soft delete the furnace
    furnace.setDeleted(true);
    furnace.setDeletedAt(LocalDateTime.now());
    furnaceRepository.save(furnace);
  }
}
