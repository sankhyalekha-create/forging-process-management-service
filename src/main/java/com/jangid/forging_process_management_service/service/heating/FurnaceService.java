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

  public Page<FurnaceRepresentation> getAllFurnacesOfTenant(long tenantId, int page, int size) {
    Pageable pageable = PageRequest.of(page, size);
    Page<Furnace> furnacePage = furnaceRepository.findByTenantIdAndDeletedIsFalseOrderByCreatedAtDesc(tenantId, pageable);
    return furnacePage.map(FurnaceAssembler::dissemble);
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
    Furnace furnace = FurnaceAssembler.assemble(furnaceRepresentation);
    furnace.setCreatedAt(LocalDateTime.now());
    Tenant tenant = tenantService.getTenantById(tenantId);
    furnace.setTenant(tenant);
    Furnace createdFurnace = furnaceRepository.save(furnace);
    return FurnaceAssembler.dissemble(createdFurnace);
  }

  public Furnace updateFurnace(Long id, Furnace furnaceDetails) {
    Furnace furnace = furnaceRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Furnace not found with id " + id));

    // Update fields
    furnace.setFurnaceName(furnaceDetails.getFurnaceName());
    furnace.setFurnaceCapacity(furnaceDetails.getFurnaceCapacity());
    furnace.setFurnaceLocation(furnaceDetails.getFurnaceLocation());
    furnace.setFurnaceDetails(furnaceDetails.getFurnaceDetails());
    furnace.setFurnaceStatus(furnaceDetails.getFurnaceStatus());

    return furnaceRepository.save(furnace);
  }

  public void deleteFurnace(Long id) {
    furnaceRepository.deleteById(id);
  }

  @Transactional
  public Furnace saveFurnace(Furnace furnace) {
    return furnaceRepository.save(furnace);
  }
}
