package com.jangid.forging_process_management_service.service.buyer;

import com.jangid.forging_process_management_service.assemblers.buyer.BuyerAssembler;
import com.jangid.forging_process_management_service.entities.Tenant;
import com.jangid.forging_process_management_service.entities.buyer.Buyer;
import com.jangid.forging_process_management_service.entitiesRepresentation.buyer.BuyerListRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.buyer.BuyerRepresentation;
import com.jangid.forging_process_management_service.exception.ValidationException;
import com.jangid.forging_process_management_service.exception.buyer.BuyerNotFoundException;
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
    private BuyerAssembler buyerAssembler;

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
        List<Buyer> suppliers = buyerRepository.findByTenantIdAndDeletedFalseOrderByCreatedAtDesc(tenantId);
        return BuyerListRepresentation.builder().buyerRepresentations(suppliers.stream().map(buyerAssembler::dissemble).toList()).build();
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
        buyer.setDeleted(true);
        buyer.setDeletedAt(LocalDateTime.now());
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
} 