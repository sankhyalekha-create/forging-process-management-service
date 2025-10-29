package com.jangid.forging_process_management_service.service.transporter;

import com.jangid.forging_process_management_service.assemblers.transporter.TransporterAssembler;
import com.jangid.forging_process_management_service.entities.Tenant;
import com.jangid.forging_process_management_service.entities.document.DocumentLink;
import com.jangid.forging_process_management_service.entities.transporter.Transporter;
import com.jangid.forging_process_management_service.entitiesRepresentation.transporter.TransporterListRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.transporter.TransporterRepresentation;
import com.jangid.forging_process_management_service.exception.ValidationException;
import com.jangid.forging_process_management_service.exception.transporter.TransporterNotFoundException;
import com.jangid.forging_process_management_service.repositories.transporter.TransporterRepository;
import com.jangid.forging_process_management_service.service.TenantService;
import com.jangid.forging_process_management_service.service.document.DocumentService;
import com.jangid.forging_process_management_service.utils.ValidationUtils;

import org.springframework.dao.DataAccessException;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service class for managing Transporter entities.
 * Handles business logic for transporter operations including creation, retrieval,
 * search, and soft deletion.
 */
@Slf4j
@Service
public class TransporterService {
  
  @Autowired
  private TransporterRepository transporterRepository;
  
  @Autowired
  private TransporterAssembler transporterAssembler;
  
  @Autowired
  private TenantService tenantService;
  
  @Autowired
  private DocumentService documentService;
  
  /**
   * Creates a new transporter or reactivates a previously deleted one.
   *
   * @param tenantId the tenant ID
   * @param transporterRepresentation the transporter data
   * @return created or reactivated transporter representation
   * @throws ValidationException if validation fails
   * @throws IllegalStateException if transporter with same name already exists
   */
  @CacheEvict(value = "transporters", allEntries = true)
  @Transactional
  public TransporterRepresentation createTransporter(long tenantId, TransporterRepresentation transporterRepresentation) {
    log.info("Creating transporter for tenant: {}, name: {}", tenantId, transporterRepresentation.getTransporterName());
    
    // Validate phone number if provided
    if (transporterRepresentation.getPhoneNumber() != null && 
        !ValidationUtils.isValidPhoneNumber(transporterRepresentation.getPhoneNumber())) {
      throw new ValidationException("Invalid phone number format. Please provide a valid phone number.");
    }
    
    // Validate alternate phone number if provided
    if (transporterRepresentation.getAlternatePhoneNumber() != null && 
        !ValidationUtils.isValidPhoneNumber(transporterRepresentation.getAlternatePhoneNumber())) {
      throw new ValidationException("Invalid alternate phone number format. Please provide a valid phone number.");
    }
    
    // Validate GSTIN if provided
    if (transporterRepresentation.getGstin() != null && 
        !ValidationUtils.isValidGstinNumber(transporterRepresentation.getGstin())) {
      throw new ValidationException("Invalid GSTIN format. GSTIN should be in the format: 22ABCDE1234F1Z5");
    }
    
    // Check if any non-deleted transporter has the same GSTIN
    if (transporterRepresentation.getGstin() != null) {
      boolean existsByGstinNotDeleted = transporterRepository.existsByGstinAndTenantIdAndDeletedFalse(
        transporterRepresentation.getGstin(), tenantId);
      if (existsByGstinNotDeleted) {
        log.error("Active transporter with GSTIN: {} already exists for tenant: {}", 
          transporterRepresentation.getGstin(), tenantId);
        throw new ValidationException("Transporter with GSTIN " + transporterRepresentation.getGstin() 
          + " already exists. GSTIN must be unique for each transporter.");
      }
    }
    
    // Check if any non-deleted transporter has the same Transporter ID Number
    if (transporterRepresentation.getTransporterIdNumber() != null && 
        !transporterRepresentation.getTransporterIdNumber().isBlank()) {
      boolean existsByTransporterIdNotDeleted = transporterRepository.existsByTransporterIdNumberAndTenantIdAndDeletedFalse(
        transporterRepresentation.getTransporterIdNumber(), tenantId);
      if (existsByTransporterIdNotDeleted) {
        log.error("Active transporter with Transporter ID Number: {} already exists for tenant: {}", 
          transporterRepresentation.getTransporterIdNumber(), tenantId);
        throw new ValidationException("Transporter with Transporter ID Number " + transporterRepresentation.getTransporterIdNumber() 
          + " already exists. Transporter ID Number must be unique for each transporter.");
      }
    }
    
    // Check if an active (not deleted) transporter with the same name exists
    boolean existsByNameNotDeleted = transporterRepository.existsByTransporterNameAndTenantIdAndDeletedFalse(
      transporterRepresentation.getTransporterName(), tenantId);
    if (existsByNameNotDeleted) {
      log.error("Active transporter with name: {} already exists for tenant: {}", 
        transporterRepresentation.getTransporterName(), tenantId);
      throw new IllegalStateException("Transporter with name=" + transporterRepresentation.getTransporterName() 
        + " already exists");
    }
    
    // Check if we're trying to revive a deleted transporter
    Transporter transporter = null;
    Optional<Transporter> deletedTransporterByName = transporterRepository.findByTransporterNameAndTenantIdAndDeletedTrue(
      transporterRepresentation.getTransporterName(), tenantId);
    
    if (deletedTransporterByName.isPresent()) {
      // We found a deleted transporter with the same name, reactivate it
      log.info("Reactivating previously deleted transporter with name: {}", transporterRepresentation.getTransporterName());
      transporter = deletedTransporterByName.get();
      transporter.setDeleted(false);
      transporter.setDeletedAt(null);
      
      // Update transporter fields from the representation
      updateTransporterFromRepresentation(transporter, transporterRepresentation);
    } else {
      // Create new transporter
      Tenant tenant = tenantService.getTenantById(tenantId);
      transporter = transporterAssembler.createAssemble(transporterRepresentation);
      transporter.setTenant(tenant);
      transporter.setCreatedAt(LocalDateTime.now());
    }
    
    Transporter createdTransporter = transporterRepository.save(transporter);
    log.info("Successfully created/reactivated transporter with id: {} for tenant: {}", createdTransporter.getId(), tenantId);
    return transporterAssembler.dissemble(createdTransporter);
  }
  
  /**
   * Updates transporter fields from representation.
   *
   * @param transporter the transporter entity to update
   * @param representation the new data
   */
  private void updateTransporterFromRepresentation(Transporter transporter, TransporterRepresentation representation) {
    if (representation.getGstin() != null) {
      transporter.setGstin(representation.getGstin());
    }
    if (representation.getTransporterIdNumber() != null) {
      transporter.setTransporterIdNumber(representation.getTransporterIdNumber());
    }
    if (representation.getPanNumber() != null) {
      transporter.setPanNumber(representation.getPanNumber());
    }
    if (representation.getAddress() != null) {
      transporter.setAddress(representation.getAddress());
    }
    if (representation.getStateCode() != null) {
      transporter.setStateCode(representation.getStateCode());
    }
    if (representation.getPincode() != null) {
      transporter.setPincode(representation.getPincode());
    }
    if (representation.getPhoneNumber() != null) {
      transporter.setPhoneNumber(representation.getPhoneNumber());
    }
    if (representation.getAlternatePhoneNumber() != null) {
      transporter.setAlternatePhoneNumber(representation.getAlternatePhoneNumber());
    }
    if (representation.getEmail() != null) {
      transporter.setEmail(representation.getEmail());
    }
    transporter.setGstRegistered(representation.isGstRegistered());
    if (representation.getBankAccountNumber() != null) {
      transporter.setBankAccountNumber(representation.getBankAccountNumber());
    }
    if (representation.getIfscCode() != null) {
      transporter.setIfscCode(representation.getIfscCode());
    }
    if (representation.getBankName() != null) {
      transporter.setBankName(representation.getBankName());
    }
    if (representation.getNotes() != null) {
      transporter.setNotes(representation.getNotes());
    }
  }
  
  /**
   * Updates an existing transporter.
   *
   * @param tenantId the tenant ID
   * @param transporterId the transporter ID to update
   * @param transporterRepresentation the updated transporter data
   * @return updated transporter representation
   * @throws ValidationException if validation fails
   * @throws TransporterNotFoundException if transporter not found
   */
  @CacheEvict(value = "transporters", allEntries = true)
  @Transactional
  public TransporterRepresentation updateTransporter(long tenantId, long transporterId, 
                                                     TransporterRepresentation transporterRepresentation) {
    log.info("Updating transporter id: {} for tenant: {}", transporterId, tenantId);
    
    // Validate tenant exists
    tenantService.isTenantExists(tenantId);
    
    // Get existing transporter
    Transporter existingTransporter = getTransporterByIdAndTenantId(transporterId, tenantId);
    
    // Validate phone number if provided
    if (transporterRepresentation.getPhoneNumber() != null && 
        !ValidationUtils.isValidPhoneNumber(transporterRepresentation.getPhoneNumber())) {
      throw new ValidationException("Invalid phone number format. Please provide a valid phone number.");
    }
    
    // Validate alternate phone number if provided
    if (transporterRepresentation.getAlternatePhoneNumber() != null && 
        !ValidationUtils.isValidPhoneNumber(transporterRepresentation.getAlternatePhoneNumber())) {
      throw new ValidationException("Invalid alternate phone number format. Please provide a valid phone number.");
    }
    
    // Validate GSTIN if provided
    if (transporterRepresentation.getGstin() != null && 
        !ValidationUtils.isValidGstinNumber(transporterRepresentation.getGstin())) {
      throw new ValidationException("Invalid GSTIN format. GSTIN should be in the format: 22ABCDE1234F1Z5");
    }
    
    // Check if GSTIN is being changed and if new GSTIN already exists for another transporter
    if (transporterRepresentation.getGstin() != null && 
        !transporterRepresentation.getGstin().equals(existingTransporter.getGstin())) {
      boolean existsByGstinNotDeleted = transporterRepository.existsByGstinAndTenantIdAndDeletedFalse(
        transporterRepresentation.getGstin(), tenantId);
      if (existsByGstinNotDeleted) {
        log.error("Active transporter with GSTIN: {} already exists for tenant: {}", 
          transporterRepresentation.getGstin(), tenantId);
        throw new ValidationException("Another transporter with GSTIN " + transporterRepresentation.getGstin() 
          + " already exists. GSTIN must be unique for each transporter.");
      }
    }
    
    // Check if Transporter ID Number is being changed and if new ID already exists for another transporter
    if (transporterRepresentation.getTransporterIdNumber() != null && 
        !transporterRepresentation.getTransporterIdNumber().isBlank() &&
        !transporterRepresentation.getTransporterIdNumber().equals(existingTransporter.getTransporterIdNumber())) {
      boolean existsByTransporterIdNotDeleted = transporterRepository.existsByTransporterIdNumberAndTenantIdAndDeletedFalse(
        transporterRepresentation.getTransporterIdNumber(), tenantId);
      if (existsByTransporterIdNotDeleted) {
        log.error("Active transporter with Transporter ID Number: {} already exists for tenant: {}", 
          transporterRepresentation.getTransporterIdNumber(), tenantId);
        throw new ValidationException("Another transporter with Transporter ID Number " + transporterRepresentation.getTransporterIdNumber() 
          + " already exists. Transporter ID Number must be unique for each transporter.");
      }
    }
    
    // Check if transporter name is being changed and if new name already exists for another transporter
    if (transporterRepresentation.getTransporterName() != null && 
        !transporterRepresentation.getTransporterName().equals(existingTransporter.getTransporterName())) {
      boolean existsByNameNotDeleted = transporterRepository.existsByTransporterNameAndTenantIdAndDeletedFalse(
        transporterRepresentation.getTransporterName(), tenantId);
      if (existsByNameNotDeleted) {
        log.error("Active transporter with name: {} already exists for tenant: {}", 
          transporterRepresentation.getTransporterName(), tenantId);
        throw new IllegalStateException("Another transporter with name=" + transporterRepresentation.getTransporterName() 
          + " already exists");
      }
    }
    
    // Update transporter fields
    updateTransporterFromRepresentation(existingTransporter, transporterRepresentation);
    
    // Update transporter name if provided
    if (transporterRepresentation.getTransporterName() != null) {
      existingTransporter.setTransporterName(transporterRepresentation.getTransporterName());
    }
    
    Transporter updatedTransporter = transporterRepository.save(existingTransporter);
    log.info("Successfully updated transporter with id: {} for tenant: {}", transporterId, tenantId);
    return transporterAssembler.dissemble(updatedTransporter);
  }
  
  /**
   * Retrieves all transporters for a tenant without pagination.
   *
   * @param tenantId the tenant ID
   * @return list representation of transporters
   */
  @Cacheable(value = "transporters", key = "'tenant_' + #tenantId + '_all'")
  public TransporterListRepresentation getAllTransportersOfTenantWithoutPagination(long tenantId) {
    log.debug("Fetching all transporters for tenant: {}", tenantId);
    List<Transporter> transporters = transporterRepository.findByTenantIdAndDeletedFalseOrderByCreatedAtDesc(tenantId);
    return TransporterListRepresentation.builder()
      .transporters(transporters.stream()
        .map(transporterAssembler::dissemble)
        .toList())
      .build();
  }
  
  /**
   * Retrieves all transporters for a tenant with pagination.
   *
   * @param tenantId the tenant ID
   * @param page the page number
   * @param size the page size
   * @return page of transporter representations
   */
  @Cacheable(value = "transporters", key = "'tenant_' + #tenantId + '_page_' + #page + '_size_' + #size")
  public Page<TransporterRepresentation> getAllTransportersOfTenant(long tenantId, int page, int size) {
    log.debug("Fetching transporters for tenant: {}, page: {}, size: {}", tenantId, page, size);
    Pageable pageable = PageRequest.of(page, size);
    Page<Transporter> transporterPage = transporterRepository.findByTenantIdAndDeletedFalseOrderByCreatedAtDesc(tenantId, pageable);
    return transporterPage.map(transporterAssembler::dissemble);
  }
  
  /**
   * Soft deletes a transporter and all associated documents.
   *
   * @param tenantId the tenant ID
   * @param transporterId the transporter ID
   * @throws TransporterNotFoundException if transporter not found
   */
  @CacheEvict(value = "transporters", allEntries = true)
  @Transactional
  public void deleteTransporter(long tenantId, long transporterId) {
    log.info("Deleting transporter with id: {} for tenant: {}", transporterId, tenantId);
    
    // Validate tenant exists
    tenantService.isTenantExists(tenantId);
    
    // Validate transporter exists and belongs to the tenant
    Transporter transporter = getTransporterByIdAndTenantId(transporterId, tenantId);
    
    // Delete all documents attached to this transporter using batch operation
    try {
      documentService.deleteDocumentsForEntity(tenantId, DocumentLink.EntityType.TRANSPORTER, transporterId);
      log.info("Successfully batch deleted all documents attached to transporter {}", transporterId);
    } catch (DataAccessException e) {
      log.error("Database error while deleting documents attached to transporter {}: {}", transporterId, e.getMessage(), e);
      throw new RuntimeException("Database error during document deletion: " + e.getMessage(), e);
    } catch (RuntimeException e) {
      log.error("Failed to delete documents attached to transporter {}: {}", transporterId, e.getMessage(), e);
      throw new RuntimeException("Failed to delete attached documents: " + e.getMessage(), e);
    }
    
    // Perform soft delete
    LocalDateTime now = LocalDateTime.now();
    transporter.setDeleted(true);
    transporter.setDeletedAt(now);
    transporterRepository.save(transporter);
    
    log.info("Successfully deleted transporter with id={} and all associated documents for tenant={}", transporterId, tenantId);
  }
  
  /**
   * Retrieves a transporter by ID and tenant ID.
   *
   * @param transporterId the transporter ID
   * @param tenantId the tenant ID
   * @return the transporter entity
   * @throws TransporterNotFoundException if transporter not found
   */
  public Transporter getTransporterByIdAndTenantId(long transporterId, long tenantId) {
    Optional<Transporter> optionalTransporter = transporterRepository.findByIdAndTenantIdAndDeletedFalse(transporterId, tenantId);
    if (optionalTransporter.isEmpty()) {
      log.error("Transporter with id={} for tenant={} not found!", transporterId, tenantId);
      throw new TransporterNotFoundException("Transporter with id=" + transporterId + " for tenant=" + tenantId + " not found!");
    }
    return optionalTransporter.get();
  }
  
  /**
   * Searches for transporters by different criteria.
   *
   * @param tenantId the tenant ID
   * @param searchType the search type (name, gstin, transporter_id)
   * @param searchQuery the search query
   * @return list of matching transporter representations
   * @throws IllegalArgumentException if invalid search type
   */
  public List<TransporterRepresentation> searchTransporters(Long tenantId, String searchType, String searchQuery) {
    log.debug("Searching transporters for tenant: {}, type: {}, query: {}", tenantId, searchType, searchQuery);
    
    List<Transporter> transporters;
    
    switch (searchType.toLowerCase()) {
      case "name":
        transporters = transporterRepository.findByTransporterNameContainingIgnoreCaseAndTenantIdAndDeletedFalse(searchQuery, tenantId);
        break;
      case "gstin":
        transporters = transporterRepository.findByGstinAndTenantIdAndDeletedFalse(searchQuery, tenantId);
        break;
      case "transporter_id":
        transporters = transporterRepository.findByTransporterIdNumberAndTenantIdAndDeletedFalse(searchQuery, tenantId);
        break;
      default:
        throw new IllegalArgumentException("Invalid search type. Must be 'name', 'gstin', or 'transporter_id'.");
    }
    
    return transporters.stream()
      .map(transporterAssembler::dissemble)
      .collect(Collectors.toList());
  }
  
  /**
   * Validates that a transporter exists.
   *
   * @param transporterId the transporter ID
   * @param tenantId the tenant ID
   * @throws TransporterNotFoundException if transporter not found
   */
  public void validateTransporterExists(long transporterId, long tenantId) {
    boolean isTransporterExists = isTransporterExists(transporterId, tenantId);
    if (!isTransporterExists) {
      log.error("Transporter with id={} not found!", transporterId);
      throw new TransporterNotFoundException("Transporter with id=" + transporterId + " not found!");
    }
  }
  
  /**
   * Checks if a transporter exists.
   *
   * @param transporterId the transporter ID
   * @param tenantId the tenant ID
   * @return true if transporter exists, false otherwise
   */
  public boolean isTransporterExists(long transporterId, long tenantId) {
    Optional<Transporter> optionalTransporter = transporterRepository.findByIdAndTenantIdAndDeletedFalse(transporterId, tenantId);
    if (optionalTransporter.isEmpty()) {
      log.error("Transporter with id={} not found!", transporterId);
      return false;
    }
    return true;
  }
}

