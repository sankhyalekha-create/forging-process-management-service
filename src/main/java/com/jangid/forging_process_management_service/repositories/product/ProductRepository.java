package com.jangid.forging_process_management_service.repositories.product;

import com.jangid.forging_process_management_service.entities.product.Product;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends CrudRepository<Product, Long> {
  @Query(value = "SELECT p.* FROM product p " +
                 "JOIN product_supplier ps ON p.id = ps.product_id " +
                 "JOIN supplier s ON ps.supplier_id = s.id " +
                 "WHERE ps.supplier_id = :supplierId AND s.tenant_id = :tenantId and p.deleted=false and s.deleted=false",
         nativeQuery = true)
  List<Product> findAllBySupplierAndTenant(@Param("tenantId") long tenantId, @Param("supplierId") long supplierId);

  Optional<Product> findByIdAndDeletedFalse(long id);
}
