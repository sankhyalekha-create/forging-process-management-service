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
import org.springframework.dao.DataAccessException;
import com.jangid.forging_process_management_service.exception.document.DocumentDeletionException;
import com.jangid.forging_process_management_service.entities.document.DocumentLink;
import com.jangid.forging_process_management_service.service.document.DocumentService;

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

  @Autowired
  private DocumentService documentService;

  public Page<FurnaceRepresentation> getAllFurnacesOfTenant(long tenantId, int page, int size) {
    Pageable pageable = PageRequest.of(page, size);
    Page<Furnace> furnacePage = furnaceRepository.findByTenantIdAndDeletedIsFalseOrderByUpdatedAtDesc(tenantId, pageable);
    return furnacePage.map(furnaceAssembler::dissemble);
  }

  public List<Furnace> getAllFurnacesOfTenant(long tenantId) {
    return furnaceRepository.findByTenantIdAndDeletedIsFalseOrderByUpdatedAtDesc(tenantId);
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
    // First check if an active (not deleted) furnace with the same name exists
    boolean existsByNameNotDeleted = furnaceRepository.existsByFurnaceNameAndTenantIdAndDeletedFalse(
        furnaceRepresentation.getFurnaceName(), tenantId);
    if (existsByNameNotDeleted) {
      log.error("Active furnace with name: {} already exists for tenant: {}!", 
                furnaceRepresentation.getFurnaceName(), tenantId);
      throw new IllegalStateException("Furnace with name=" + furnaceRepresentation.getFurnaceName() 
                                     + " already exists");
    }
    
    // Check if we're trying to revive a deleted furnace
    Furnace furnace = null;
    Optional<Furnace> deletedFurnace = furnaceRepository.findByFurnaceNameAndTenantIdAndDeletedTrue(
        furnaceRepresentation.getFurnaceName(), tenantId);
    
    if (deletedFurnace.isPresent()) {
      // We found a deleted furnace with the same name, reactivate it
      log.info("Reactivating previously deleted furnace with name: {}", 
               furnaceRepresentation.getFurnaceName());
      furnace = deletedFurnace.get();
      furnace.setDeleted(false);
      furnace.setDeletedAt(null);
      
      // Update furnace fields from the representation
      furnace.setFurnaceName(furnaceRepresentation.getFurnaceName());
      furnace.setFurnaceCapacity(Double.valueOf(furnaceRepresentation.getFurnaceCapacity()));
      furnace.setFurnaceLocation(furnaceRepresentation.getFurnaceLocation());
      furnace.setFurnaceDetails(furnaceRepresentation.getFurnaceDetails());
      furnace.setFurnaceStatus(Furnace.FurnaceStatus.HEAT_TREATMENT_BATCH_NOT_APPLIED);
    } else {
      // Create new furnace
      furnace = furnaceAssembler.assemble(furnaceRepresentation);
      furnace.setCreatedAt(LocalDateTime.now());
      Tenant tenant = tenantService.getTenantById(tenantId);
      furnace.setTenant(tenant);
    }
    
    Furnace savedFurnace = furnaceRepository.save(furnace);
    return furnaceAssembler.dissemble(savedFurnace);
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
  public void deleteFurnace(Long tenantId, Long furnaceId) throws DocumentDeletionException {
    // 1. Validate tenant exists
    tenantService.isTenantExists(tenantId);

    // 2. Validate furnace exists
    Furnace furnace = getFurnaceByIdAndTenantId(furnaceId, tenantId);

    // 3. Validate furnace status
    if (furnace.getFurnaceStatus() != Furnace.FurnaceStatus.HEAT_TREATMENT_BATCH_NOT_APPLIED) {
      throw new IllegalStateException("This furnace cannot be deleted as it is not in the HEAT_TREATMENT_BATCH_NOT_APPLIED status.");
    }

    // 4. Delete all documents attached to this furnace using bulk delete for efficiency
    try {
        // Use bulk delete method from DocumentService for better performance
        documentService.deleteDocumentsForEntity(tenantId, DocumentLink.EntityType.FURNACE, furnaceId);
        log.info("Successfully bulk deleted all documents attached to furnace {} for tenant {}", furnaceId, tenantId);
    } catch (DataAccessException e) {
        log.error("Database error while deleting documents attached to furnace {}: {}", furnaceId, e.getMessage(), e);
        throw new DocumentDeletionException("Database error occurred while deleting attached documents for furnace " + furnaceId, e);
    } catch (RuntimeException e) {
        // Handle document service specific runtime exceptions (storage, file system errors, etc.)
        log.error("Document service error while deleting documents attached to furnace {}: {}", furnaceId, e.getMessage(), e);
        throw new DocumentDeletionException("Document service error occurred while deleting attached documents for furnace " + furnaceId + ": " + e.getMessage(), e);
    } catch (Exception e) {
        // Handle any other unexpected exceptions
        log.error("Unexpected error while deleting documents attached to furnace {}: {}", furnaceId, e.getMessage(), e);
        throw new DocumentDeletionException("Unexpected error occurred while deleting attached documents for furnace " + furnaceId, e);
    }

    // Finally, soft delete the furnace
    furnace.setDeleted(true);
    furnace.setDeletedAt(LocalDateTime.now());
    furnaceRepository.save(furnace);
    
    log.info("Successfully deleted furnace with id={} and all associated documents for tenant={}", furnaceId, tenantId);
  }
}
