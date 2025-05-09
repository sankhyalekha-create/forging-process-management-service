package com.jangid.forging_process_management_service.service.buyer;

import com.jangid.forging_process_management_service.assemblers.buyer.BuyerAssembler;
import com.jangid.forging_process_management_service.assemblers.buyer.BuyerEntityAssembler;
import com.jangid.forging_process_management_service.entities.Tenant;
import com.jangid.forging_process_management_service.entities.buyer.Buyer;
import com.jangid.forging_process_management_service.entities.buyer.BuyerEntity;
import com.jangid.forging_process_management_service.entitiesRepresentation.buyer.BuyerEntityRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.buyer.BuyerListRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.buyer.BuyerRepresentation;
import com.jangid.forging_process_management_service.exception.ValidationException;
import com.jangid.forging_process_management_service.exception.buyer.BuyerNotFoundException;
import com.jangid.forging_process_management_service.repositories.buyer.BuyerEntityRepository;
import com.jangid.forging_process_management_service.repositories.buyer.BuyerRepository;
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
public class BuyerService {

    @Autowired
    private BuyerRepository buyerRepository;
    @Autowired
    private BuyerEntityRepository buyerEntityRepository;
    @Autowired
    private BuyerAssembler buyerAssembler;
    @Autowired
    private BuyerEntityAssembler buyerEntityAssembler;

    @Autowired
    private TenantService tenantService;

    @CacheEvict(value = "buyers", allEntries = true)
    @Transactional
    public BuyerRepresentation createBuyer(long tenantId, BuyerRepresentation buyerRepresentation) {
        // Validate phone number if provided
        if (buyerRepresentation.getPhoneNumber() != null && !ValidationUtils.isValidPhoneNumber(buyerRepresentation.getPhoneNumber())) {
            throw new ValidationException("Invalid phone number format. Please provide a valid phone number.");
        }

        // Validate GSTIN/UIN if provided
        if (buyerRepresentation.getGstinUin() != null && !ValidationUtils.isValidGstinNumber(buyerRepresentation.getGstinUin())) {
            throw new ValidationException("Invalid GSTIN/UIN number format. GSTIN number should be in the format: 22ABCDE1234F1Z5");
        }

        // Check if any non-deleted buyer has the same GSTIN number
        if (buyerRepresentation.getGstinUin() != null) {
            boolean existsByGstinNotDeleted = buyerRepository.existsByGstinUinAndTenantIdAndDeletedFalse(
                buyerRepresentation.getGstinUin(), tenantId);
            if (existsByGstinNotDeleted) {
                log.error("Active buyer with GSTIN/UIN: {} already exists for tenant: {}!", 
                        buyerRepresentation.getGstinUin(), tenantId);
                throw new ValidationException("Buyer with GSTIN/UIN " + buyerRepresentation.getGstinUin() 
                                         + " already exists. GSTIN/UIN must be unique for each buyer.");
            }
        }

        // Check if an active (not deleted) buyer with the same name exists
        boolean existsByNameNotDeleted = buyerRepository.existsByBuyerNameAndTenantIdAndDeletedFalse(
            buyerRepresentation.getBuyerName(), tenantId);
        if (existsByNameNotDeleted) {
            log.error("Active buyer with name: {} already exists for tenant: {}!", 
                    buyerRepresentation.getBuyerName(), tenantId);
            throw new IllegalStateException("Buyer with name=" + buyerRepresentation.getBuyerName() 
                                         + " already exists for tenant=" + tenantId);
        }

        // Check if we're trying to revive a deleted buyer
        Buyer buyer = null;
        Optional<Buyer> deletedBuyerByName = buyerRepository.findByBuyerNameAndTenantIdAndDeletedTrue(
            buyerRepresentation.getBuyerName(), tenantId);
        
        if (deletedBuyerByName.isPresent()) {
            // We found a deleted buyer with the same name, reactivate it
            log.info("Reactivating previously deleted buyer with name: {}", buyerRepresentation.getBuyerName());
            buyer = deletedBuyerByName.get();
            buyer.setDeleted(false);
            buyer.setDeletedAt(null);
            
            // Update buyer fields from the representation
            updateBuyerFromRepresentation(buyer, buyerRepresentation);
            
            // Handle buyer entities reactivation or creation
            if (buyerRepresentation.getEntities() != null && !buyerRepresentation.getEntities().isEmpty()) {
                handleBuyerEntitiesReactivation(buyer, buyerRepresentation);
            }
        } else {
            // Create new buyer
            Tenant tenant = tenantService.getTenantById(tenantId);
            buyer = buyerAssembler.createAssemble(buyerRepresentation);
            Buyer finalBuyer = buyer;
            buyer.getEntities().forEach(buyerEntity -> {
                if (buyerEntity.getPhoneNumber() != null && !ValidationUtils.isValidPhoneNumber(buyerEntity.getPhoneNumber())) {
                    throw new ValidationException("Invalid phone number format. Please provide a valid phone number.");
                }
                if (buyerEntity.getGstinUin() != null && !ValidationUtils.isValidGstinNumber(buyerEntity.getGstinUin())) {
                    throw new ValidationException("Invalid GSTIN/UIN number format. GSTIN number should be in the format: 22ABCDE1234F1Z5");
                }
                buyerEntity.setBuyer(finalBuyer);
                buyerEntity.setCreatedAt(LocalDateTime.now());
            });
            buyer.setTenant(tenant);
            buyer.setCreatedAt(LocalDateTime.now());
        }
        
        Buyer createdBuyer = buyerRepository.save(buyer);
        return buyerAssembler.dissemble(createdBuyer);
    }
    
    /**
     * Handles the reactivation of BuyerEntity objects for a buyer being reactivated
     * For each entity in the representation, we either reactivate a deleted entity with the same name
     * or create a new entity if no match exists
     */
    private void handleBuyerEntitiesReactivation(Buyer buyer, BuyerRepresentation buyerRepresentation) {
        List<BuyerEntity> existingEntities = buyer.getEntities();
        List<BuyerEntityRepresentation> newEntityRepresentations = buyerRepresentation.getEntities();
        
        // Create a map of existing entities by name for easier lookup
        Map<String, BuyerEntity> existingEntitiesByName = new HashMap<>();
        for (BuyerEntity entity : existingEntities) {
            existingEntitiesByName.put(entity.getBuyerEntityName(), entity);
        }
        
        // For each entity in the new representation
        List<BuyerEntity> entitiesToKeep = new ArrayList<>();
        for (BuyerEntityRepresentation entityRep : newEntityRepresentations) {
            // Check if an entity with this name already exists
            BuyerEntity existingEntity = existingEntitiesByName.get(entityRep.getBuyerEntityName());
            
            if (existingEntity != null) {
                // Reactivate and update the existing entity
                log.info("Reactivating and updating existing entity: {}", existingEntity.getBuyerEntityName());
                existingEntity.setDeleted(false);
                existingEntity.setDeletedAt(null);
                
                // Update entity fields from representation
                updateBuyerEntityFromRepresentation(existingEntity, entityRep);
                entitiesToKeep.add(existingEntity);
            } else {
                // Create a new entity
                log.info("Creating new entity for reactivated buyer: {}", entityRep.getBuyerEntityName());
                BuyerEntity newEntity = buyerEntityAssembler.assemble(entityRep);
                newEntity.setBuyer(buyer);
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
     * Updates BuyerEntity fields from BuyerEntityRepresentation
     */
    private void updateBuyerEntityFromRepresentation(BuyerEntity entity, BuyerEntityRepresentation representation) {
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
     * Helper method to update buyer fields from BuyerRepresentation
     */
    private void updateBuyerFromRepresentation(Buyer buyer, BuyerRepresentation representation) {
        if (representation.getAddress() != null) {
            buyer.setAddress(representation.getAddress());
        }
        if (representation.getPhoneNumber() != null) {
            buyer.setPhoneNumber(representation.getPhoneNumber());
        }
        if (representation.getGstinUin() != null) {
            buyer.setGstinUin(representation.getGstinUin());
        }
    }

    @Cacheable(value = "buyers", key = "'tenant_' + #tenantId + '_all'")
    public BuyerListRepresentation getAllBuyersOfTenantWithoutPagination(long tenantId){
        List<Buyer> buyers = buyerRepository.findByTenantIdAndDeletedFalseOrderByCreatedAtDesc(tenantId);
        return BuyerListRepresentation.builder().buyerRepresentations(buyers.stream().map(buyerAssembler::dissemble).toList()).build();
    }

    @Cacheable(value = "buyers", key = "'tenant_' + #tenantId + '_page_' + #page + '_size_' + #size")
    public Page<BuyerRepresentation> getAllBuyersOfTenant(long tenantId, int page, int size){
        Pageable pageable = PageRequest.of(page, size);
        Page<Buyer> buyerPage = buyerRepository.findByTenantIdAndDeletedFalseOrderByCreatedAtDesc(tenantId, pageable);
        return buyerPage.map(buyerAssembler::dissemble);
    }

    @Transactional
    public void deleteBuyer(long tenantId, long buyerId) {
        // Validate tenant exists
        tenantService.isTenantExists(tenantId);

        // Validate Buyer exists and belongs to the tenant
        Buyer buyer = getBuyerByIdAndTenantId(buyerId, tenantId);

        // Perform soft delete
        LocalDateTime now = LocalDateTime.now();
        buyer.getEntities().forEach(buyerEntity -> {
            buyerEntity.setDeleted(true);
            buyerEntity.setDeletedAt(now);
        });
        buyer.setDeleted(true);
        buyer.setDeletedAt(now);
        buyerRepository.save(buyer);
    }

    public Buyer getBuyerByIdAndTenantId(long buyerId, long tenantId){
        Optional<Buyer> optionalBuyer = buyerRepository.findByIdAndTenantIdAndDeletedFalse(buyerId, tenantId);
        if (optionalBuyer.isEmpty()){
            log.error("Buyer with id=" + buyerId + " having " + tenantId + " not found!");
            throw new BuyerNotFoundException("Buyer with id=" + buyerId + " having " + tenantId + " not found!");
        }
        return optionalBuyer.get();
    }

    public BuyerEntity getBuyerEntityById(long buyerEntityId){
        Optional<BuyerEntity> optionalBuyerEntity = buyerEntityRepository.findByIdAndDeletedFalse(buyerEntityId);
        if (optionalBuyerEntity.isEmpty()){
            log.error("BuyerEntity with id=" + buyerEntityId + " not found!");
            throw new BuyerNotFoundException("BuyerEntity with id=" + buyerEntityId + " not found!");
        }
        return optionalBuyerEntity.get();
    }

    public List<BuyerRepresentation> searchBuyers(Long tenantId, String searchType, String searchQuery) {
        List<Buyer> buyers;

        switch (searchType.toLowerCase()) {
            case "name":
                buyers = buyerRepository.findByBuyerNameContainingIgnoreCaseAndTenantIdAndDeletedFalse(searchQuery, tenantId);
                break;
            case "gstin":
                buyers = buyerRepository.findByGstinUinAndTenantIdAndDeletedFalse(searchQuery, tenantId);
                break;
            default:
                throw new IllegalArgumentException("Invalid search type. Must be 'name' or 'gstin'.");
        }

        return buyers.stream()
                .map(buyerAssembler::dissemble)
                .collect(Collectors.toList());
    }

    public void validateBuyerEntityExists(long id, long tenantId) {
        boolean isBuyerEntityExists = isBuyerEntityExists(id, tenantId);
        if (!isBuyerEntityExists) {
            log.error("BuyerEntity with id=" + id + " not found!");
            throw new BuyerNotFoundException("BuyerEntity with id=" + id + " not found!");
        }
    }

    public boolean isBuyerEntityExists(long id, long tenantId){
        Optional<BuyerEntity> optionalBuyerEntity = buyerEntityRepository.findByIdAndDeletedFalse(id);
        if (optionalBuyerEntity.isEmpty()){
            log.error("BuyerEntity with id="+id+" not found!");
            return false;
        }
        return true;
    }

    public void validateBuyerExists(long buyerId, long tenantId) {
        boolean isBuyerExists = isBuyerExists(buyerId, tenantId);
        if (!isBuyerExists) {
            log.error("Buyer with id=" + buyerId + " not found!");
            throw new BuyerNotFoundException("Buyer with id=" + buyerId + " not found!");
        }
    }

    public boolean isBuyerExists(long id, long tenantId){
        Optional<Buyer> optionalBuyer = buyerRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId);
        if (optionalBuyer.isEmpty()){
            log.error("Buyer with id="+id+" not found!");
            return false;
        }
        return true;
    }

    @Cacheable(value = "buyerBillingType", key = "'tenant_' + #tenantId + '_buyer_' + #buyerId")
    public List<BuyerEntityRepresentation> getBuyerBillingType(long tenantId, long buyerId) {
        // Validate buyer exists and belongs to the tenant
        Buyer buyer = getBuyerByIdAndTenantId(buyerId, tenantId);

        // Get the billing entities
        List<BuyerEntity> billingEntities = buyer.getEntities().stream()
                .filter(BuyerEntity::isBillingEntity)
                .toList();

        if (billingEntities.isEmpty()) {
            log.error("No billing entities found for buyer with id=" + buyerId);
            throw new BuyerNotFoundException("No billing entities found for buyer with id=" + buyerId);
        }

        return billingEntities.stream()
                .map(buyerEntityAssembler::dissemble)
                .toList();
    }

    @Cacheable(value = "buyerShippingType", key = "'tenant_' + #tenantId + '_buyer_' + #buyerId")
    public List<BuyerEntityRepresentation> getBuyerShippingType(long tenantId, long buyerId) {
        // Validate buyer exists and belongs to the tenant
        Buyer buyer = getBuyerByIdAndTenantId(buyerId, tenantId);

        // Get the shipping entities
        List<BuyerEntity> shippingEntities = buyer.getEntities().stream()
                .filter(BuyerEntity::isShippingEntity)
                .toList();

        if (shippingEntities.isEmpty()) {
            log.error("No shipping entities found for buyer with id=" + buyerId);
            throw new BuyerNotFoundException("No shipping entities found for buyer with id=" + buyerId);
        }

        return shippingEntities.stream()
                .map(buyerEntityAssembler::dissemble)
                .toList();
    }
} 