package com.jangid.forging_process_management_service.service.product;

import com.jangid.forging_process_management_service.assemblers.product.SupplierAssembler;
import com.jangid.forging_process_management_service.entities.Tenant;
import com.jangid.forging_process_management_service.entities.product.Product;
import com.jangid.forging_process_management_service.entities.product.Supplier;
import com.jangid.forging_process_management_service.entitiesRepresentation.product.SupplierListRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.product.SupplierRepresentation;
import com.jangid.forging_process_management_service.exception.product.SupplierNotFoundException;
import com.jangid.forging_process_management_service.exception.ValidationException;
import com.jangid.forging_process_management_service.repositories.product.ProductRepository;
import com.jangid.forging_process_management_service.repositories.product.SupplierRepository;
import com.jangid.forging_process_management_service.service.TenantService;
import com.jangid.forging_process_management_service.utils.ValidationUtils;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class SupplierService {

  @Autowired
  private SupplierRepository supplierRepository;

  @Autowired
  private ProductRepository productRepository;

  @Autowired
  private TenantService tenantService;

  @CacheEvict(value = "suppliers", allEntries = true)
  @Transactional
  public SupplierRepresentation createSupplier(long tenantId, SupplierRepresentation supplierRepresentation){
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

  @Transactional
  public SupplierRepresentation updateSupplier(long tenantId, long supplierId, SupplierRepresentation representation){
    Tenant tenant = tenantService.getTenantById(tenantId);
    Supplier existingSupplier = getSupplierByIdAndTenantId(supplierId, tenant.getId());

    if (!existingSupplier.getSupplierName().equals(representation.getSupplierName())) {
      existingSupplier.setSupplierName(representation.getSupplierName());
    }
    if (!existingSupplier.getSupplierDetail().equals(representation.getSupplierDetail())) {
      existingSupplier.setSupplierDetail(representation.getSupplierDetail());
    }
    Supplier updatedSupplier = supplierRepository.save(existingSupplier);
    return SupplierAssembler.dissemble(updatedSupplier);
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
  public void deleteSupplier(long tenantId, long supplierId) {
    // Validate tenant exists
    tenantService.isTenantExists(tenantId);

    // Validate supplier exists and belongs to the tenant
    Supplier supplier = getSupplierByIdAndTenantId(supplierId, tenantId);

    // Check if supplier is associated with any products
    List<Product> associatedProducts = productRepository.findAllBySupplierAndTenant(tenantId, supplierId);
    if (!associatedProducts.isEmpty()) {
        throw new IllegalStateException("Cannot delete supplier as it is associated with products");
    }

    // Perform soft delete
    supplier.setDeleted(true);
    supplier.setDeletedAt(LocalDateTime.now());
    supplierRepository.save(supplier);
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

}
