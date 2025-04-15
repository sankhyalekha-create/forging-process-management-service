package com.jangid.forging_process_management_service.service.buyer;

import com.jangid.forging_process_management_service.assemblers.buyer.BuyerAssembler;
import com.jangid.forging_process_management_service.entities.Tenant;
import com.jangid.forging_process_management_service.entities.buyer.Buyer;
import com.jangid.forging_process_management_service.entitiesRepresentation.BuyerRepresentation;
import com.jangid.forging_process_management_service.exception.ValidationException;
import com.jangid.forging_process_management_service.repositories.buyer.BuyerRepository;
import com.jangid.forging_process_management_service.service.TenantService;
import com.jangid.forging_process_management_service.utils.ValidationUtils;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.CacheEvict;

import java.time.LocalDateTime;

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
            buyerEntity.setBuyer(buyer);
            buyerEntity.setCreatedAt(LocalDateTime.now());
        });
        buyer.setTenant(tenant);
        buyer.setCreatedAt(LocalDateTime.now());
        Buyer createdBuyer = buyerRepository.save(buyer);
        return buyerAssembler.dissemble(createdBuyer);
    }
} 