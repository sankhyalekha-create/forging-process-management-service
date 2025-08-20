package com.jangid.forging_process_management_service.service.vendor;

import com.jangid.forging_process_management_service.assemblers.vendor.VendorAssembler;
import com.jangid.forging_process_management_service.assemblers.vendor.VendorEntityAssembler;
import com.jangid.forging_process_management_service.entities.Tenant;
import com.jangid.forging_process_management_service.entities.vendor.Vendor;
import com.jangid.forging_process_management_service.entities.vendor.VendorEntity;
import com.jangid.forging_process_management_service.entitiesRepresentation.vendor.VendorEntityRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.vendor.VendorListRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.vendor.VendorRepresentation;
import com.jangid.forging_process_management_service.exception.ValidationException;
import com.jangid.forging_process_management_service.exception.vendor.VendorNotFoundException;
import com.jangid.forging_process_management_service.repositories.vendor.VendorEntityRepository;
import com.jangid.forging_process_management_service.repositories.vendor.VendorRepository;
import com.jangid.forging_process_management_service.service.TenantService;
import com.jangid.forging_process_management_service.utils.ValidationUtils;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.CacheEvict;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class VendorService {

    @Autowired
    private VendorRepository vendorRepository;
    @Autowired
    private VendorEntityRepository vendorEntityRepository;
    @Autowired
    private VendorAssembler vendorAssembler;
    @Autowired
    private VendorEntityAssembler vendorEntityAssembler;

    @Autowired
    private TenantService tenantService;

    @CacheEvict(value = "vendors", allEntries = true)
    @Transactional
    public VendorRepresentation createVendor(long tenantId, VendorRepresentation vendorRepresentation) {
        // Validate phone number if provided
        if (vendorRepresentation.getPhoneNumber() != null && !ValidationUtils.isValidPhoneNumber(vendorRepresentation.getPhoneNumber())) {
            throw new ValidationException("Invalid phone number format. Please provide a valid phone number.");
        }

        // Validate GSTIN/UIN if provided
        if (vendorRepresentation.getGstinUin() != null && !ValidationUtils.isValidGstinNumber(vendorRepresentation.getGstinUin())) {
            throw new ValidationException("Invalid GSTIN/UIN number format. GSTIN number should be in the format: 22ABCDE1234F1Z5");
        }

        // Check if any non-deleted vendor has the same GSTIN number
        if (vendorRepresentation.getGstinUin() != null) {
            boolean existsByGstinNotDeleted = vendorRepository.existsByGstinUinAndTenantIdAndDeletedFalse(
                vendorRepresentation.getGstinUin(), tenantId);
            if (existsByGstinNotDeleted) {
                log.error("Active vendor with GSTIN/UIN: {} already exists for tenant: {}!", 
                        vendorRepresentation.getGstinUin(), tenantId);
                throw new ValidationException("Vendor with GSTIN/UIN " + vendorRepresentation.getGstinUin() 
                                         + " already exists. GSTIN/UIN must be unique for each vendor.");
            }
        }

        // Check if an active (not deleted) vendor with the same name exists
        boolean existsByNameNotDeleted = vendorRepository.existsByVendorNameAndTenantIdAndDeletedFalse(
            vendorRepresentation.getVendorName(), tenantId);
        if (existsByNameNotDeleted) {
            log.error("Active vendor with name: {} already exists for tenant: {}!", 
                    vendorRepresentation.getVendorName(), tenantId);
            throw new IllegalStateException("Vendor with name=" + vendorRepresentation.getVendorName() 
                                         + " already exists");
        }

        // Check if we're trying to revive a deleted vendor
        Vendor vendor = null;
        Optional<Vendor> deletedVendorByName = vendorRepository.findByVendorNameAndTenantIdAndDeletedTrue(
            vendorRepresentation.getVendorName(), tenantId);
        
        if (deletedVendorByName.isPresent()) {
            // We found a deleted vendor with the same name, reactivate it
            log.info("Reactivating previously deleted vendor with name: {}", vendorRepresentation.getVendorName());
            vendor = deletedVendorByName.get();
            vendor.setDeleted(false);
            vendor.setDeletedAt(null);
            
            // Update vendor fields from the representation
            updateVendorFromRepresentation(vendor, vendorRepresentation);
            
            // Handle vendor entities reactivation or creation
            if (vendorRepresentation.getEntities() != null && !vendorRepresentation.getEntities().isEmpty()) {
                handleVendorEntitiesReactivation(vendor, vendorRepresentation);
            }
        } else {
            // Create new vendor
            Tenant tenant = tenantService.getTenantById(tenantId);
            vendor = vendorAssembler.createAssemble(vendorRepresentation);
            Vendor finalVendor = vendor;
            vendor.getEntities().forEach(vendorEntity -> {
                if (vendorEntity.getPhoneNumber() != null && !ValidationUtils.isValidPhoneNumber(vendorEntity.getPhoneNumber())) {
                    throw new ValidationException("Invalid phone number format. Please provide a valid phone number.");
                }
                if (vendorEntity.getGstinUin() != null && !ValidationUtils.isValidGstinNumber(vendorEntity.getGstinUin())) {
                    throw new ValidationException("Invalid GSTIN/UIN number format. GSTIN number should be in the format: 22ABCDE1234F1Z5");
                }
                vendorEntity.setVendor(finalVendor);
                vendorEntity.setCreatedAt(LocalDateTime.now());
            });
            vendor.setTenant(tenant);
            vendor.setCreatedAt(LocalDateTime.now());
        }
        
        Vendor createdVendor = vendorRepository.save(vendor);
        return vendorAssembler.dissemble(createdVendor);
    }
    
    /**
     * Handles the reactivation of VendorEntity objects for a vendor being reactivated
     */
    private void handleVendorEntitiesReactivation(Vendor vendor, VendorRepresentation vendorRepresentation) {
        List<VendorEntity> existingEntities = vendor.getEntities();
        List<VendorEntityRepresentation> newEntityRepresentations = vendorRepresentation.getEntities();
        
        // Create a map of existing entities by name for easier lookup
        Map<String, VendorEntity> existingEntitiesByName = new HashMap<>();
        for (VendorEntity entity : existingEntities) {
            existingEntitiesByName.put(entity.getVendorEntityName(), entity);
        }
        
        // For each entity in the new representation
        List<VendorEntity> entitiesToKeep = new ArrayList<>();
        for (VendorEntityRepresentation entityRep : newEntityRepresentations) {
            // Check if an entity with this name already exists
            VendorEntity existingEntity = existingEntitiesByName.get(entityRep.getVendorEntityName());
            
            if (existingEntity != null) {
                // Reactivate and update the existing entity
                log.info("Reactivating and updating existing entity: {}", existingEntity.getVendorEntityName());
                existingEntity.setDeleted(false);
                existingEntity.setDeletedAt(null);
                
                // Update entity fields from representation
                updateVendorEntityFromRepresentation(existingEntity, entityRep);
                entitiesToKeep.add(existingEntity);
            } else {
                // Create a new entity
                log.info("Creating new entity for reactivated vendor: {}", entityRep.getVendorEntityName());
                VendorEntity newEntity = vendorEntityAssembler.assemble(entityRep);
                newEntity.setVendor(vendor);
                newEntity.setCreatedAt(LocalDateTime.now());
                
                // Validate the new entity
                if (newEntity.getPhoneNumber() != null && !ValidationUtils.isValidPhoneNumber(newEntity.getPhoneNumber())) {
                    throw new ValidationException("Invalid phone number format for entity. Please provide a valid phone number.");
                }
                if (newEntity.getGstinUin() != null && !ValidationUtils.isValidGstinNumber(newEntity.getGstinUin())) {
                    throw new ValidationException("Invalid GSTIN/UIN number format for entity. GSTIN number should be in the format: 22ABCDE1234F1Z5");
                }
                
                entitiesToKeep.add(newEntity);
            }
        }
        
        // Clear the current list and add only the ones we want to keep
        existingEntities.clear();
        existingEntities.addAll(entitiesToKeep);
    }
    
    /**
     * Updates VendorEntity fields from VendorEntityRepresentation
     */
    private void updateVendorEntityFromRepresentation(VendorEntity entity, VendorEntityRepresentation representation) {
        if (representation.getAddress() != null) {
            entity.setAddress(representation.getAddress());
        }
        if (representation.getGstinUin() != null) {
            entity.setGstinUin(representation.getGstinUin());
        }
        if (representation.getPhoneNumber() != null) {
            entity.setPhoneNumber(representation.getPhoneNumber());
        }
        entity.setBillingEntity(representation.isBillingEntity());
        entity.setShippingEntity(representation.isShippingEntity());
    }

    /**
     * Updates Vendor fields from VendorRepresentation
     */
    private void updateVendorFromRepresentation(Vendor vendor, VendorRepresentation representation) {
        if (representation.getAddress() != null) {
            vendor.setAddress(representation.getAddress());
        }
        if (representation.getGstinUin() != null) {
            vendor.setGstinUin(representation.getGstinUin());
        }
        if (representation.getPhoneNumber() != null) {
            vendor.setPhoneNumber(representation.getPhoneNumber());
        }
    }

    @Cacheable(value = "vendors", key = "'tenant_' + #tenantId + '_all'")
    public VendorListRepresentation getAllVendorsOfTenantWithoutPagination(long tenantId){
        List<Vendor> vendors = vendorRepository.findByTenantIdAndDeletedFalseOrderByCreatedAtDesc(tenantId);
        List<VendorRepresentation> vendorRepresentations = vendors.stream().map(vendorAssembler::dissemble).collect(Collectors.toList());
        return VendorListRepresentation.builder().vendorRepresentations(vendorRepresentations).build();
    }

    @Cacheable(value = "vendors", key = "'tenant_' + #tenantId + '_page_' + #page + '_size_' + #size")
    public Page<VendorRepresentation> getAllVendorsOfTenant(long tenantId, int page, int size){
        Pageable pageable = PageRequest.of(page, size);
        Page<Vendor> vendors = vendorRepository.findByTenantIdAndDeletedFalseOrderByCreatedAtDesc(tenantId, pageable);
        return vendors.map(vendorAssembler::dissemble);
    }

    @Transactional
    public void deleteVendor(long tenantId, long vendorId) {
        Vendor vendor = vendorRepository.findByIdAndTenantIdAndDeletedFalse(vendorId, tenantId)
                .orElseThrow(() -> new VendorNotFoundException(tenantId, vendorId));

        // Check if vendor has any active dispatch or receive batches
        boolean hasActiveDispatchBatches = vendor.getVendorDispatchBatches().stream()
                .anyMatch(batch -> !batch.isDeleted());
        boolean hasActiveReceiveBatches = vendor.getVendorReceiveBatches().stream()
                .anyMatch(batch -> !batch.isDeleted());
        
        if (hasActiveDispatchBatches || hasActiveReceiveBatches) {
            throw new IllegalStateException("Cannot delete vendor with active dispatch or receive batches. " +
                    "Please complete or cancel all associated batches first.");
        }

        // Soft delete the vendor and its entities
        vendor.setDeleted(true);
        vendor.setDeletedAt(LocalDateTime.now());
        vendor.getEntities().forEach(entity -> {
            entity.setDeleted(true);
            entity.setDeletedAt(LocalDateTime.now());
        });

        vendorRepository.save(vendor);
        log.info("Vendor with id: {} has been soft deleted for tenant: {}", vendorId, tenantId);
    }

    public Vendor getVendorByIdAndTenantId(long vendorId, long tenantId){
        return vendorRepository.findByIdAndTenantIdAndDeletedFalse(vendorId, tenantId)
                .orElseThrow(() -> new VendorNotFoundException(tenantId, vendorId));
    }

    public VendorEntity getVendorEntityById(long vendorEntityId){
        return vendorEntityRepository.findByIdAndDeletedFalse(vendorEntityId)
                .orElseThrow(() -> new RuntimeException("VendorEntity not found with id: " + vendorEntityId));
    }

    public List<VendorRepresentation> searchVendors(Long tenantId, String searchType, String searchQuery) {
        List<Vendor> vendors;
        
        switch (searchType.toLowerCase()) {
            case "name":
                vendors = vendorRepository.findByVendorNameContainingIgnoreCaseAndTenantIdAndDeletedFalse(searchQuery, tenantId);
                break;
            case "gstin":
                vendors = vendorRepository.findByGstinUinAndTenantIdAndDeletedFalse(searchQuery, tenantId);
                break;
            default:
                throw new IllegalArgumentException("Invalid search type: " + searchType + ". Supported types: name, gstin");
        }
        
        return vendors.stream()
                .map(vendorAssembler::dissemble)
                .collect(Collectors.toList());
    }

    public void validateVendorEntityExists(long id, long tenantId) {
        getVendorEntityById(id);
    }

    public boolean isVendorEntityExists(long id, long tenantId){
        return vendorEntityRepository.findByIdAndDeletedFalse(id).isPresent();
    }

    public void validateVendorExists(long vendorId, long tenantId) {
        getVendorByIdAndTenantId(vendorId, tenantId);
    }

    public boolean isVendorExists(long id, long tenantId){
        return vendorRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId).isPresent();
    }

    @Cacheable(value = "vendorBillingType", key = "'tenant_' + #tenantId + '_vendor_' + #vendorId")
    public List<VendorEntityRepresentation> getVendorBillingType(long tenantId, long vendorId) {
        Vendor vendor = getVendorByIdAndTenantId(vendorId, tenantId);
        
        List<VendorEntity> billingEntities = vendor.getEntities().stream()
            .filter(entity -> !entity.isDeleted() && entity.isBillingEntity())
            .collect(Collectors.toList());
        
        if (billingEntities.isEmpty()) {
            log.warn("No billing entities found for vendor: {} in tenant: {}", vendorId, tenantId);
            return new ArrayList<>();
        }
        
        return billingEntities.stream()
            .map(vendorEntityAssembler::dissemble)
            .collect(Collectors.toList());
    }

    @Cacheable(value = "vendorShippingType", key = "'tenant_' + #tenantId + '_vendor_' + #vendorId")
    public List<VendorEntityRepresentation> getVendorShippingType(long tenantId, long vendorId) {
        Vendor vendor = getVendorByIdAndTenantId(vendorId, tenantId);
        
        List<VendorEntity> shippingEntities = vendor.getEntities().stream()
            .filter(entity -> !entity.isDeleted() && entity.isShippingEntity())
            .collect(Collectors.toList());
        
        if (shippingEntities.isEmpty()) {
            log.warn("No shipping entities found for vendor: {} in tenant: {}", vendorId, tenantId);
            return new ArrayList<>();
        }
        
        return shippingEntities.stream()
            .map(vendorEntityAssembler::dissemble)
            .collect(Collectors.toList());
    }
} 