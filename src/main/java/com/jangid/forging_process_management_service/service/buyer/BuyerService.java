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
import java.util.List;
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

        Tenant tenant = tenantService.getTenantById(tenantId);
        Buyer buyer = buyerAssembler.createAssemble(buyerRepresentation);
        buyer.getEntities().forEach(buyerEntity -> {
            if (buyerEntity.getPhoneNumber() != null && !ValidationUtils.isValidPhoneNumber(buyerEntity.getPhoneNumber())) {
                throw new ValidationException("Invalid phone number format. Please provide a valid phone number.");
            }
            if (buyerEntity.getGstinUin() != null && !ValidationUtils.isValidGstinNumber(buyerEntity.getGstinUin())) {
                throw new ValidationException("Invalid GSTIN/UIN number format. GSTIN number should be in the format: 22ABCDE1234F1Z5");
            }
            buyerEntity.setBuyer(buyer);
            buyerEntity.setCreatedAt(LocalDateTime.now());
        });
        buyer.setTenant(tenant);
        buyer.setCreatedAt(LocalDateTime.now());
        Buyer createdBuyer = buyerRepository.save(buyer);
        return buyerAssembler.dissemble(createdBuyer);
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