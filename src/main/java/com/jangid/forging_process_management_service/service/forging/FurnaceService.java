package com.jangid.forging_process_management_service.service.forging;

import com.jangid.forging_process_management_service.assemblers.forging.FurnaceAssembler;
import com.jangid.forging_process_management_service.entities.Tenant;
import com.jangid.forging_process_management_service.entities.forging.Furnace;
import com.jangid.forging_process_management_service.entitiesRepresentation.forging.FurnaceRepresentation;
import com.jangid.forging_process_management_service.exception.ResourceNotFoundException;
import com.jangid.forging_process_management_service.repositories.forging.FurnaceRepository;
import com.jangid.forging_process_management_service.service.TenantService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

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

  public Optional<Furnace> getFurnaceById(Long id) {
    return furnaceRepository.findById(id);
  }

  public FurnaceRepresentation createFurnace(Long tenantId, FurnaceRepresentation furnaceRepresentation) {
    Furnace furnace = FurnaceAssembler.assemble(furnaceRepresentation);
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
}
