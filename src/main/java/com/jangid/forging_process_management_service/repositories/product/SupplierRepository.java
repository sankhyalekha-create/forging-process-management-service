package com.jangid.forging_process_management_service.repositories.product;

import com.jangid.forging_process_management_service.entities.product.Supplier;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierRepository extends CrudRepository<Supplier, Long> {

  Page<Supplier> findByTenantIdAndDeletedFalseOrderByCreatedAtDesc(long tenantId, Pageable pageable);

  List<Supplier> findByTenantIdAndDeletedFalseOrderByCreatedAtDesc(long tenantId);
  Optional<Supplier> findByIdAndTenantIdAndDeletedFalse(long id, long tenantId);
  Optional<Supplier> findBySupplierNameAndTenantIdAndDeletedFalse(String supplierName, long tenantId);
  List<Supplier> findByTenantIdAndDeletedFalse(long tenantId);
  
  // Methods to check for duplicate suppliers
  boolean existsBySupplierNameAndTenantIdAndDeletedFalse(String supplierName, long tenantId);
  boolean existsByPanNumberAndTenantIdAndDeletedFalse(String panNumber, long tenantId);
  boolean existsByGstinNumberAndTenantIdAndDeletedFalse(String gstinNumber, long tenantId);
  
  // Methods to find deleted suppliers
  Optional<Supplier> findBySupplierNameAndTenantIdAndDeletedTrue(String supplierName, long tenantId);
  Optional<Supplier> findByPanNumberAndTenantIdAndDeletedTrue(String panNumber, long tenantId);
  Optional<Supplier> findByGstinNumberAndTenantIdAndDeletedTrue(String gstinNumber, long tenantId);

  Optional<Supplier> findByIdAndDeletedFalse(long id);

  // Paginated search method for supplier name substring
  @Query("""
        SELECT s
        FROM supplier s
        WHERE s.tenant.id = :tenantId
          AND LOWER(s.supplierName) LIKE LOWER(CONCAT('%', :supplierName, '%'))
          AND s.deleted = false
        ORDER BY s.supplierName ASC
    """)
  Page<Supplier> findSuppliersBySupplierNameContainingIgnoreCase(@Param("tenantId") Long tenantId, @Param("supplierName") String supplierName, Pageable pageable);
}
