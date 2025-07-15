package com.jangid.forging_process_management_service.repositories.vendor;

import com.jangid.forging_process_management_service.entities.vendor.Vendor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VendorRepository extends CrudRepository<Vendor, Long> {
    Optional<Vendor> findByIdAndTenantIdAndDeletedFalse(long id, long tenantId);
    Optional<Vendor> findByIdAndDeletedFalse(long id);
    Page<Vendor> findByTenantIdAndDeletedFalseOrderByCreatedAtDesc(long tenantId, Pageable pageable);
    List<Vendor> findByTenantIdAndDeletedFalseOrderByCreatedAtDesc(long tenantId);
    
    // Search vendors by name (case-insensitive partial match)
    List<Vendor> findByVendorNameContainingIgnoreCaseAndTenantIdAndDeletedFalse(String vendorName, long tenantId);
    
    // Search vendors by GSTIN/UIN (exact match)
    List<Vendor> findByGstinUinAndTenantIdAndDeletedFalse(String gstinUin, long tenantId);
    
    // Check if active vendor exists with name
    boolean existsByVendorNameAndTenantIdAndDeletedFalse(String vendorName, long tenantId);
    
    // Check if active vendor exists with GSTIN/UIN
    boolean existsByGstinUinAndTenantIdAndDeletedFalse(String gstinUin, long tenantId);
    
    // Find deleted vendor by name for reactivation
    Optional<Vendor> findByVendorNameAndTenantIdAndDeletedTrue(String vendorName, long tenantId);
} 