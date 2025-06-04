package com.jangid.forging_process_management_service.repositories.product;

import com.jangid.forging_process_management_service.dto.HeatInfoDTO;
import com.jangid.forging_process_management_service.entities.product.Product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
                 "WHERE ps.supplier_id = :supplierId AND s.tenant_id = :tenantId and p.deleted=false and s.deleted=false " +
                 "ORDER BY p.created_at DESC",
         nativeQuery = true)
  List<Product> findAllBySupplierAndTenant(@Param("tenantId") long tenantId, @Param("supplierId") long supplierId);

  Optional<Product> findByIdAndDeletedFalse(long id);

  // Check if product exists by name or code, only for non-deleted products
  boolean existsByProductNameAndTenantIdAndDeletedFalse(String productName, long tenantId);
  boolean existsByProductCodeAndTenantIdAndDeletedFalse(String productCode, long tenantId);
  
  // Find deleted products by name or code
  Optional<Product> findByProductNameAndTenantIdAndDeletedTrue(String productName, long tenantId);
  Optional<Product> findByProductCodeAndTenantIdAndDeletedTrue(String productCode, long tenantId);

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

  List<Product> findByTenantIdAndDeletedFalse(Long tenantId);


  @Query("SELECT NEW com.jangid.forging_process_management_service.dto.HeatInfoDTO(" +
         "h.id, " +
         "h.heatNumber, " +
         "CASE WHEN rmp.product.unitOfMeasurement != com.jangid.forging_process_management_service.entities.product.UnitOfMeasurement.PIECES THEN h.heatQuantity ELSE null END, " +
         "CASE WHEN rmp.product.unitOfMeasurement != com.jangid.forging_process_management_service.entities.product.UnitOfMeasurement.PIECES THEN h.availableHeatQuantity ELSE null END, " +
         "null, " +
         "CASE WHEN rmp.product.unitOfMeasurement = com.jangid.forging_process_management_service.entities.product.UnitOfMeasurement.PIECES THEN h.piecesCount ELSE null END, " +
         "CASE WHEN rmp.product.unitOfMeasurement = com.jangid.forging_process_management_service.entities.product.UnitOfMeasurement.PIECES THEN h.availablePiecesCount ELSE null END, " +
         "null) " +
         "FROM com.jangid.forging_process_management_service.entities.inventory.Heat h " +
         "JOIN h.rawMaterialProduct rmp " +
         "WHERE rmp.product.id = :productId AND h.deleted = false AND rmp.deleted = false " +
         "ORDER BY " +
         "CASE WHEN rmp.product.unitOfMeasurement != com.jangid.forging_process_management_service.entities.product.UnitOfMeasurement.PIECES THEN h.availableHeatQuantity ELSE h.availablePiecesCount END DESC")
  List<HeatInfoDTO> findHeatsByProductId(@Param("productId") Long productId);

  // New search methods for the search API with pagination support
  @Query("""
        SELECT DISTINCT p
        FROM product p
        JOIN raw_material_product rmp ON p.id = rmp.product.id
        JOIN raw_material rm ON rmp.rawMaterial.id = rm.id
        WHERE rm.tenant.id = :tenantId
          AND LOWER(p.productName) LIKE LOWER(CONCAT('%', :productName, '%'))
          AND p.deleted = false
          AND rmp.deleted = false
          AND rm.deleted = false
        ORDER BY p.productName ASC
    """)
  Page<Product> findProductsByProductNameContainingIgnoreCase(@Param("tenantId") Long tenantId, @Param("productName") String productName, Pageable pageable);

  @Query("""
        SELECT DISTINCT p
        FROM product p
        JOIN raw_material_product rmp ON p.id = rmp.product.id
        JOIN raw_material rm ON rmp.rawMaterial.id = rm.id
        WHERE rm.tenant.id = :tenantId
          AND LOWER(p.productCode) LIKE LOWER(CONCAT('%', :productCode, '%'))
          AND p.deleted = false
          AND rmp.deleted = false
          AND rm.deleted = false
        ORDER BY p.productCode ASC
    """)
  Page<Product> findProductsByProductCodeContainingIgnoreCase(@Param("tenantId") Long tenantId, @Param("productCode") String productCode, Pageable pageable);

}
