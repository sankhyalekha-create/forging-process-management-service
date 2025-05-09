package com.jangid.forging_process_management_service.repositories.buyer;

import com.jangid.forging_process_management_service.entities.buyer.Buyer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BuyerRepository extends CrudRepository<Buyer, Long> {
    Optional<Buyer> findByIdAndTenantIdAndDeletedFalse(long id, long tenantId);
    Optional<Buyer> findByIdAndDeletedFalse(long id);
    Page<Buyer> findByTenantIdAndDeletedFalseOrderByCreatedAtDesc(long tenantId, Pageable pageable);
    List<Buyer> findByTenantIdAndDeletedFalseOrderByCreatedAtDesc(long tenantId);
    
    // Search buyers by name (case-insensitive partial match)
    List<Buyer> findByBuyerNameContainingIgnoreCaseAndTenantIdAndDeletedFalse(String buyerName, long tenantId);
    
    // Search buyers by GSTIN/UIN (exact match)
    List<Buyer> findByGstinUinAndTenantIdAndDeletedFalse(String gstinUin, long tenantId);
    
    // Check if active buyer exists with name
    boolean existsByBuyerNameAndTenantIdAndDeletedFalse(String buyerName, long tenantId);
    
    // Check if active buyer exists with GSTIN/UIN
    boolean existsByGstinUinAndTenantIdAndDeletedFalse(String gstinUin, long tenantId);
    
    // Find deleted buyer by name for reactivation
    Optional<Buyer> findByBuyerNameAndTenantIdAndDeletedTrue(String buyerName, long tenantId);
} 