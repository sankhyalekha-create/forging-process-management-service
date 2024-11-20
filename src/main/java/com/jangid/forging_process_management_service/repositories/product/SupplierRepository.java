package com.jangid.forging_process_management_service.repositories.product;

import com.jangid.forging_process_management_service.entities.product.Supplier;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
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

  Optional<Supplier> findByIdAndDeletedFalse(long id);
}
