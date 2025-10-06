package com.jangid.forging_process_management_service.service.product;

import com.jangid.forging_process_management_service.assemblers.product.SupplierAssembler;
import com.jangid.forging_process_management_service.entities.Tenant;
import com.jangid.forging_process_management_service.entities.document.DocumentLink;
import com.jangid.forging_process_management_service.entities.product.Product;
import com.jangid.forging_process_management_service.entities.product.Supplier;
import com.jangid.forging_process_management_service.entitiesRepresentation.product.SupplierListRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.product.SupplierRepresentation;
import com.jangid.forging_process_management_service.exception.product.SupplierNotFoundException;
import com.jangid.forging_process_management_service.exception.ValidationException;
import com.jangid.forging_process_management_service.repositories.product.ProductRepository;
import com.jangid.forging_process_management_service.repositories.product.SupplierRepository;
import com.jangid.forging_process_management_service.service.TenantService;
import com.jangid.forging_process_management_service.service.document.DocumentService;
import com.jangid.forging_process_management_service.utils.ValidationUtils;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataAccessException;
import com.jangid.forging_process_management_service.exception.document.DocumentDeletionException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

@Slf4j
@Service
public class SupplierService {

  @Autowired
  private SupplierRepository supplierRepository;

  @Autowired
  private ProductRepository productRepository;

  @Autowired
  private TenantService tenantService;

  @Autowired
  private DocumentService documentService;

  @CacheEvict(value = "suppliers", allEntries = true)
  @Transactional
  public SupplierRepresentation createSupplier(long tenantId, SupplierRepresentation supplierRepresentation){
    // Validate field lengths first
    validateFieldLengths(supplierRepresentation);
    
    // Validate phone number
    if (!ValidationUtils.isValidPhoneNumber(supplierRepresentation.getPhoneNumber())) {
      throw new ValidationException("Invalid phone number format. Please provide a valid phone number.");
    }

    // Validate PAN number
    if (!ValidationUtils.isValidPanNumber(supplierRepresentation.getPanNumber())) {
      throw new ValidationException("Invalid PAN number format. PAN number should be in the format: ABCDE1234F");
    }

    // Validate GSTIN number
    if (!ValidationUtils.isValidGstinNumber(supplierRepresentation.getGstinNumber())) {
      throw new ValidationException("Invalid GSTIN number format. GSTIN number should be in the format: 22ABCDE1234F1Z5");
    }
    
    // Check for existing suppliers with same name, PAN, or GSTIN
    boolean existsByNameNotDeleted = supplierRepository.existsBySupplierNameAndTenantIdAndDeletedFalse(
        supplierRepresentation.getSupplierName(), tenantId);
    if (existsByNameNotDeleted) {
      log.error("Active supplier with name: {} already exists for tenant: {}!", 
                supplierRepresentation.getSupplierName(), tenantId);
      throw new IllegalStateException("Supplier with name=" + supplierRepresentation.getSupplierName() 
                                    + " already exists");
    }
    
    boolean existsByPanNotDeleted = supplierRepository.existsByPanNumberAndTenantIdAndDeletedFalse(
        supplierRepresentation.getPanNumber(), tenantId);
    if (existsByPanNotDeleted) {
      log.error("Active supplier with PAN: {} already exists for tenant: {}!", 
                supplierRepresentation.getPanNumber(), tenantId);
      throw new IllegalStateException("Supplier with PAN=" + supplierRepresentation.getPanNumber() 
                                    + " already exists");
    }
    
    boolean existsByGstinNotDeleted = supplierRepository.existsByGstinNumberAndTenantIdAndDeletedFalse(
        supplierRepresentation.getGstinNumber(), tenantId);
    if (existsByGstinNotDeleted) {
      log.error("Active supplier with GSTIN: {} already exists for tenant: {}!", 
                supplierRepresentation.getGstinNumber(), tenantId);
      throw new IllegalStateException("Supplier with GSTIN=" + supplierRepresentation.getGstinNumber() 
                                    + " already exists");
    }
    
    // Check if we're trying to revive a deleted supplier
    Supplier supplier = null;
    
    // Try to find a deleted supplier by name
    Optional<Supplier> deletedSupplierByName = supplierRepository.findBySupplierNameAndTenantIdAndDeletedTrue(
        supplierRepresentation.getSupplierName(), tenantId);
    
    if (deletedSupplierByName.isPresent()) {
      // We found a deleted supplier with the same name, reactivate it
      log.info("Reactivating previously deleted supplier with name: {}", supplierRepresentation.getSupplierName());
      supplier = deletedSupplierByName.get();
      supplier.setDeleted(false);
      supplier.setDeletedAt(null);
      
      // Update fields from representation
      updateSupplierFields(supplier, supplierRepresentation);
    } else {
      // Try to find by PAN number
      Optional<Supplier> deletedSupplierByPan = supplierRepository.findByPanNumberAndTenantIdAndDeletedTrue(
          supplierRepresentation.getPanNumber(), tenantId);
          
      if (deletedSupplierByPan.isPresent()) {
        // We found a deleted supplier with the same PAN, reactivate it
        log.info("Reactivating previously deleted supplier with PAN: {}", supplierRepresentation.getPanNumber());
        supplier = deletedSupplierByPan.get();
        supplier.setDeleted(false);
        supplier.setDeletedAt(null);
        
        // Update fields from representation
        updateSupplierFields(supplier, supplierRepresentation);
      } else {
        // Try to find by GSTIN number
        Optional<Supplier> deletedSupplierByGstin = supplierRepository.findByGstinNumberAndTenantIdAndDeletedTrue(
            supplierRepresentation.getGstinNumber(), tenantId);
            
        if (deletedSupplierByGstin.isPresent()) {
          // We found a deleted supplier with the same GSTIN, reactivate it
          log.info("Reactivating previously deleted supplier with GSTIN: {}", supplierRepresentation.getGstinNumber());
          supplier = deletedSupplierByGstin.get();
          supplier.setDeleted(false);
          supplier.setDeletedAt(null);
          
          // Update fields from representation
          updateSupplierFields(supplier, supplierRepresentation);
        } else {
          // Create new supplier
          Tenant tenant = tenantService.getTenantById(tenantId);
          supplier = SupplierAssembler.assemble(supplierRepresentation);
          supplier.setTenant(tenant);
          supplier.setCreatedAt(LocalDateTime.now());
        }
      }
    }
    
    Supplier savedSupplier = supplierRepository.save(supplier);
    return SupplierAssembler.dissemble(savedSupplier);
  }
  
  /**
   * Helper method to update supplier fields from SupplierRepresentation
   */
  private void updateSupplierFields(Supplier supplier, SupplierRepresentation representation) {
    if (representation.getSupplierName() != null) {
      supplier.setSupplierName(representation.getSupplierName());
    }
    
    if (representation.getSupplierDetail() != null) {
      supplier.setSupplierDetail(representation.getSupplierDetail());
    }
    
    if (representation.getPhoneNumber() != null) {
      supplier.setPhoneNumber(representation.getPhoneNumber());
    }
    
    if (representation.getPanNumber() != null) {
      supplier.setPanNumber(representation.getPanNumber());
    }
    
    if (representation.getGstinNumber() != null) {
      supplier.setGstinNumber(representation.getGstinNumber());
    }
  }

  @Cacheable(value = "suppliers", key = "'tenant_' + #tenantId + '_page_' + #page + '_size_' + #size")
  public Page<SupplierRepresentation> getAllSuppliersOfTenant(long tenantId, int page, int size){
    Pageable pageable = PageRequest.of(page, size);
    Page<Supplier> supplierPage = supplierRepository.findByTenantIdAndDeletedFalseOrderByCreatedAtDesc(tenantId, pageable);
    return supplierPage.map(SupplierAssembler::dissemble);
  }

  @Cacheable(value = "suppliers", key = "'tenant_' + #tenantId + '_all'")
  public SupplierListRepresentation getAllSuppliersOfTenantWithoutPagination(long tenantId){
    List<Supplier> suppliers = supplierRepository.findByTenantIdAndDeletedFalseOrderByCreatedAtDesc(tenantId);
    return SupplierListRepresentation.builder().supplierRepresentations(suppliers.stream().map(SupplierAssembler::dissemble).toList()).build();
  }

  @Cacheable(value = "suppliers", key = "'tenant_' + #tenantId + '_supplier_' + #supplierId")
  public SupplierRepresentation getSupplierOfTenant(long tenantId, long supplierId){
    Supplier supplier = getSupplierById(supplierId);
    if(supplier.getTenant().getId()!=tenantId){
      throw new SupplierNotFoundException("Supplier not found with supplierId="+supplierId);
    }
    return SupplierAssembler.dissemble(supplier);
  }

  @CacheEvict(value = "suppliers", allEntries = true)
  @Transactional
  public SupplierRepresentation updateSupplier(long tenantId, long supplierId, SupplierRepresentation representation){
    Tenant tenant = tenantService.getTenantById(tenantId);
    Supplier existingSupplier = getSupplierByIdAndTenantId(supplierId, tenant.getId());

    // Check if supplier is used in any products
    List<Product> associatedProducts = productRepository.findAllBySupplierAndTenant(tenantId, supplierId);
    boolean isUsedInProducts = !associatedProducts.isEmpty();

    // Validate and update fields based on usage status
    updateSupplierFields(existingSupplier, representation, isUsedInProducts);

    Supplier updatedSupplier = supplierRepository.save(existingSupplier);
    return SupplierAssembler.dissemble(updatedSupplier);
  }

  /**
   * Updates supplier fields based on whether the supplier is used in products
   * @param existingSupplier The existing supplier entity
   * @param representation The new supplier data
   * @param isUsedInProducts Whether this supplier is associated with any products
   */
  private void updateSupplierFields(Supplier existingSupplier, SupplierRepresentation representation, boolean isUsedInProducts) {
    // Always updatable fields (contact and descriptive information)
    
    // Validate and update supplier detail (no length constraint in DB)
    updateIfDifferent(existingSupplier::setSupplierDetail, existingSupplier.getSupplierDetail(), representation.getSupplierDetail());
    
    // Validate and update address (max 500 characters)
    if (representation.getAddress() != null && !representation.getAddress().equals(existingSupplier.getAddress())) {
      if (representation.getAddress().length() > 500) {
        throw new ValidationException("Address cannot be longer than 500 characters. Please provide a shorter address.");
      }
      existingSupplier.setAddress(representation.getAddress());
    }
    
    // Validate and update state code (max 2 characters, numeric only)
    if (representation.getStateCode() != null && !representation.getStateCode().equals(existingSupplier.getStateCode())) {
      if (representation.getStateCode().length() > 2) {
        throw new ValidationException("State code cannot be longer than 2 characters. Please provide a valid 2-digit state code (e.g., '22', '06').");
      }
      if (!representation.getStateCode().matches("^\\d{1,2}$")) {
        throw new ValidationException("State code must be numeric. Please provide a valid 2-digit numeric state code (e.g., '22', '06', '09').");
      }
      existingSupplier.setStateCode(representation.getStateCode());
    }
    
    // Validate and update pincode (max 6 characters)
    if (representation.getPincode() != null && !representation.getPincode().equals(existingSupplier.getPincode())) {
      if (representation.getPincode().length() > 6) {
        throw new ValidationException("Pincode cannot be longer than 6 characters. Please provide a valid 6-digit pincode (e.g., '110001').");
      }
      existingSupplier.setPincode(representation.getPincode());
    }
    
    // Validate and update phone number (max 15 characters)
    if (representation.getPhoneNumber() != null && !representation.getPhoneNumber().equals(existingSupplier.getPhoneNumber())) {
      if (representation.getPhoneNumber().length() > 15) {
        throw new ValidationException("Phone number cannot be longer than 15 characters. Please provide a shorter phone number.");
      }
      if (!ValidationUtils.isValidPhoneNumber(representation.getPhoneNumber())) {
        throw new ValidationException("Invalid phone number format. Please provide a valid phone number.");
      }
      existingSupplier.setPhoneNumber(representation.getPhoneNumber());
    }
    
    // Validate and update email (no length constraint in DB, but validate format)
    updateIfDifferent(existingSupplier::setEmail, existingSupplier.getEmail(), representation.getEmail());

    if (isUsedInProducts) {
      // Supplier is used in products - restrict critical identifier updates
      log.info("Supplier {} is used in {} products. Restricting updates to immutable fields.", 
               existingSupplier.getId(), productRepository.findAllBySupplierAndTenant(existingSupplier.getTenant().getId(), existingSupplier.getId()).size());
      
      // Check if user is trying to update restricted fields
      validateRestrictedFieldUpdates(existingSupplier, representation);
    } else {
      // Supplier is not used in products - allow all field updates
      log.info("Supplier {} is not used in any products. Allowing all field updates.", existingSupplier.getId());
      
      // Check for duplicates before updating critical fields
      validateUniqueFieldUpdates(existingSupplier, representation);
      
      // Update critical identifier fields
      updateIfDifferent(existingSupplier::setSupplierName, existingSupplier.getSupplierName(), representation.getSupplierName());
      
      // Validate and update PAN number (max 10 characters)
      if (representation.getPanNumber() != null && !representation.getPanNumber().equals(existingSupplier.getPanNumber())) {
        if (representation.getPanNumber().length() > 10) {
          throw new ValidationException("PAN number cannot be longer than 10 characters. Please provide a valid PAN number (e.g., 'ABCDE1234F').");
        }
        if (!ValidationUtils.isValidPanNumber(representation.getPanNumber())) {
          throw new ValidationException("Invalid PAN number format. PAN number should be in the format: ABCDE1234F");
        }
        existingSupplier.setPanNumber(representation.getPanNumber());
      }
      
      // Validate and update GSTIN number (max 15 characters)
      if (representation.getGstinNumber() != null && !representation.getGstinNumber().equals(existingSupplier.getGstinNumber())) {
        if (representation.getGstinNumber().length() > 15) {
          throw new ValidationException("GSTIN number cannot be longer than 15 characters. Please provide a valid GSTIN number (e.g., '22ABCDE1234F1Z5').");
        }
        if (!ValidationUtils.isValidGstinNumber(representation.getGstinNumber())) {
          throw new ValidationException("Invalid GSTIN number format. GSTIN number should be in the format: 22ABCDE1234F1Z5");
        }
        existingSupplier.setGstinNumber(representation.getGstinNumber());
      }
    }
  }

  /**
   * Helper method to update a field only if the new value is different from the existing value
   * Handles null safety and empty string checks
   */
  private void updateIfDifferent(Consumer<String> setter, String existingValue, String newValue) {
    if (newValue != null && !newValue.equals(existingValue)) {
      setter.accept(newValue);
    }
  }

  /**
   * Validates that restricted fields are not being updated for suppliers used in products
   */
  private void validateRestrictedFieldUpdates(Supplier existingSupplier, SupplierRepresentation representation) {
    if (representation.getSupplierName() != null && !representation.getSupplierName().equals(existingSupplier.getSupplierName())) {
      throw new ValidationException("Cannot update supplier name for suppliers that are used in products. " +
                                  "This supplier is associated with existing products and changing the name would affect historical data integrity.");
    }
    
    if (representation.getPanNumber() != null && !representation.getPanNumber().equals(existingSupplier.getPanNumber())) {
      throw new ValidationException("Cannot update PAN number for suppliers that are used in products. " +
                                  "This supplier is associated with existing products and changing the PAN would affect historical data integrity.");
    }
    
    if (representation.getGstinNumber() != null && !representation.getGstinNumber().equals(existingSupplier.getGstinNumber())) {
      throw new ValidationException("Cannot update GSTIN number for suppliers that are used in products. " +
                                  "This supplier is associated with existing products and changing the GSTIN would affect historical data integrity.");
    }
  }

  /**
   * Validates that unique fields don't conflict with existing suppliers when updating unused suppliers
   */
  private void validateUniqueFieldUpdates(Supplier existingSupplier, SupplierRepresentation representation) {
    long tenantId = existingSupplier.getTenant().getId();
    
    // Check supplier name uniqueness
    if (representation.getSupplierName() != null && !representation.getSupplierName().equals(existingSupplier.getSupplierName())) {
      boolean nameExists = supplierRepository.existsBySupplierNameAndTenantIdAndDeletedFalse(representation.getSupplierName(), tenantId);
      if (nameExists) {
        throw new ValidationException("Supplier with name '" + representation.getSupplierName() + "' already exists.");
      }
    }
    
    // Check PAN number uniqueness
    if (representation.getPanNumber() != null && !representation.getPanNumber().equals(existingSupplier.getPanNumber())) {
      boolean panExists = supplierRepository.existsByPanNumberAndTenantIdAndDeletedFalse(representation.getPanNumber(), tenantId);
      if (panExists) {
        throw new ValidationException("Supplier with PAN number '" + representation.getPanNumber() + "' already exists.");
      }
    }
    
    // Check GSTIN number uniqueness
    if (representation.getGstinNumber() != null && !representation.getGstinNumber().equals(existingSupplier.getGstinNumber())) {
      boolean gstinExists = supplierRepository.existsByGstinNumberAndTenantIdAndDeletedFalse(representation.getGstinNumber(), tenantId);
      if (gstinExists) {
        throw new ValidationException("Supplier with GSTIN number '" + representation.getGstinNumber() + "' already exists.");
      }
    }
  }

  public Supplier getSupplierByIdAndTenantId(long supplierId, long tenantId){
    Optional<Supplier> optionalSupplier = supplierRepository.findByIdAndTenantIdAndDeletedFalse(supplierId, tenantId);
    if (optionalSupplier.isEmpty()){
      log.error("Supplier with id="+supplierId+" having "+tenantId+" not found!");
      throw new SupplierNotFoundException("Supplier with id="+supplierId+" having "+tenantId+" not found!");
    }
    return optionalSupplier.get();
  }

  public Supplier getSupplierByNameAndTenantId(String supplierName, long tenantId){
    Optional<Supplier> optionalSupplier = supplierRepository.findBySupplierNameAndTenantIdAndDeletedFalse(supplierName, tenantId);
    if (optionalSupplier.isEmpty()){
      log.error("Supplier with name="+supplierName+" having "+tenantId+" not found!");
      throw new RuntimeException("Supplier with id="+supplierName+" having "+tenantId+" not found!");
    }
    return optionalSupplier.get();
  }

  public List<Supplier> getSuppliersByTenantId(long tenantId){
    List<Supplier> suppliers = supplierRepository.findByTenantIdAndDeletedFalse(tenantId);
    if (suppliers == null || suppliers.isEmpty()){
      log.error("Suppliers not found!");
      throw new RuntimeException("Suppliers not found!");
    }
    return suppliers;
  }

  @Transactional
  public void deleteSupplier(long tenantId, long supplierId) throws DocumentDeletionException {
    // Validate tenant exists
    tenantService.isTenantExists(tenantId);

    // Validate supplier exists and belongs to the tenant
    Supplier supplier = getSupplierByIdAndTenantId(supplierId, tenantId);

    // Check if supplier is associated with any products
    List<Product> associatedProducts = productRepository.findAllBySupplierAndTenant(tenantId, supplierId);
    if (!associatedProducts.isEmpty()) {
        throw new IllegalStateException("Cannot delete supplier as it is associated with products");
    }

    // Delete all documents attached to this supplier using batch deletion
    deleteSupplierDocuments(tenantId, supplierId);

    // Perform soft delete
    supplier.setDeleted(true);
    supplier.setDeletedAt(LocalDateTime.now());
    supplierRepository.save(supplier);
    log.info("Successfully deleted supplier with id={} and all associated documents for tenant={}", supplierId, tenantId);
  }

  public Supplier getSupplierById(long supplierId){
    Optional<Supplier> supplierOptional = supplierRepository.findByIdAndDeletedFalse(supplierId);
    if (supplierOptional.isEmpty()) {
      throw new SupplierNotFoundException("Supplier not found with supplierId="+supplierId);
    }
    return supplierOptional.get();
  }

  public boolean isSupplierExists(long supplierId){
    return supplierRepository.existsById(supplierId);
  }

  /**
   * Search for suppliers by supplier name substring with pagination
   * @param tenantId The tenant ID
   * @param supplierName The supplier name to search for (substring matching)
   * @param page The page number (0-based)
   * @param size The page size
   * @return Page of SupplierRepresentation containing the search results
   */
  public Page<SupplierRepresentation> searchSuppliersByNameWithPagination(Long tenantId, String supplierName, int page, int size) {
    if (supplierName == null || supplierName.trim().isEmpty()) {
      Pageable pageable = PageRequest.of(page, size);
      return Page.empty(pageable);
    }

    Pageable pageable = PageRequest.of(page, size);
    Page<Supplier> supplierPage = supplierRepository.findSuppliersBySupplierNameContainingIgnoreCase(tenantId, supplierName.trim(), pageable);
    
    return supplierPage.map(SupplierAssembler::dissemble);
  }

  @CacheEvict(value = "suppliers", allEntries = true)
  public void clearSupplierCache() {
    log.info("Clearing supplier cache");
  }

  /**
   * Check if a supplier is used in any products
   * @param tenantId The tenant ID
   * @param supplierId The supplier ID
   * @return true if supplier is used in products, false otherwise
   */
  public boolean isSupplierUsedInProducts(long tenantId, long supplierId) {
    List<Product> associatedProducts = productRepository.findAllBySupplierAndTenant(tenantId, supplierId);
    return !associatedProducts.isEmpty();
  }

  /**
   * Validates field lengths against database constraints
   * @param representation The supplier representation to validate
   */
  private void validateFieldLengths(SupplierRepresentation representation) {
    if (representation.getPhoneNumber() != null && representation.getPhoneNumber().length() > 15) {
      throw new ValidationException("Phone number cannot be longer than 15 characters. Please provide a shorter phone number.");
    }
    
    if (representation.getPanNumber() != null && representation.getPanNumber().length() > 10) {
      throw new ValidationException("PAN number cannot be longer than 10 characters. Please provide a valid PAN number (e.g., 'ABCDE1234F').");
    }
    
    if (representation.getGstinNumber() != null && representation.getGstinNumber().length() > 15) {
      throw new ValidationException("GSTIN number cannot be longer than 15 characters. Please provide a valid GSTIN number (e.g., '22ABCDE1234F1Z5').");
    }
    
    if (representation.getStateCode() != null && representation.getStateCode().length() > 2) {
      throw new ValidationException("State code cannot be longer than 2 characters. Please provide a valid 2-digit state code (e.g., '22', '06').");
    }
    
    if (representation.getStateCode() != null && !representation.getStateCode().isEmpty() && !representation.getStateCode().matches("^\\d{1,2}$")) {
      throw new ValidationException("State code must be numeric. Please provide a valid 2-digit numeric state code (e.g., '22', '06', '09').");
    }
    
    if (representation.getPincode() != null && representation.getPincode().length() > 6) {
      throw new ValidationException("Pincode cannot be longer than 6 characters. Please provide a valid 6-digit pincode (e.g., '110001').");
    }
    
    if (representation.getAddress() != null && representation.getAddress().length() > 500) {
      throw new ValidationException("Address cannot be longer than 500 characters. Please provide a shorter address.");
    }
  }

  /**
   * Delete all documents attached to a supplier using bulk delete for efficiency
   * Follows the same pattern as ItemService.deleteItem()
   */
  private void deleteSupplierDocuments(Long tenantId, Long supplierId) throws DocumentDeletionException {
    try {
      // Use bulk delete method from DocumentService for better performance
      documentService.deleteDocumentsForEntity(tenantId, DocumentLink.EntityType.SUPPLIER, supplierId);
      log.info("Successfully bulk deleted all documents attached to supplier {} for tenant {}", supplierId, tenantId);
    } catch (DataAccessException e) {
      log.error("Database error while deleting documents attached to supplier {}: {}", supplierId, e.getMessage(), e);
      throw new DocumentDeletionException("Database error occurred while deleting attached documents for supplier " + supplierId, e);
    } catch (RuntimeException e) {
      // Handle document service specific runtime exceptions (storage, file system errors, etc.)
      log.error("Document service error while deleting documents attached to supplier {}: {}", supplierId, e.getMessage(), e);
      throw new DocumentDeletionException("Document service error occurred while deleting attached documents for supplier " + supplierId + ": " + e.getMessage(), e);
    } catch (Exception e) {
      // Handle any other unexpected exceptions
      log.error("Unexpected error while deleting documents attached to supplier {}: {}", supplierId, e.getMessage(), e);
      throw new DocumentDeletionException("Unexpected error occurred while deleting attached documents for supplier " + supplierId, e);
    }
  }

}
