package com.jangid.forging_process_management_service.repositories.product;

import com.jangid.forging_process_management_service.entities.product.Product;
import com.jangid.forging_process_management_service.entitiesRepresentation.overview.ProductQuantityRepresentation;

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

  @Query(value = "SELECT p.product_name AS productName, SUM(h.available_heat_quantity) AS totalQuantity "
                 + "FROM product p "
                 + "JOIN raw_material_product rmp ON p.id = rmp.product_id "
                 + "JOIN raw_material rm ON rmp.raw_material_id = rm.id "
                 + "JOIN heat h ON rmp.id = h.raw_material_product_id "
                 + "WHERE rm.tenant_id = :tenantId "
                 + "GROUP BY p.product_name "
                 + "ORDER BY totalQuantity DESC",
         nativeQuery = true)
  List<Object[]> findProductQuantitiesNative(@Param("tenantId") long tenantId);


}
