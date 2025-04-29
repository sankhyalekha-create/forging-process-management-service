package com.jangid.forging_process_management_service.repositories.product;

import com.jangid.forging_process_management_service.entities.product.Item;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {
  Page<Item> findByTenantIdAndDeletedFalseOrderByCreatedAtDesc(long tenantId, Pageable pageable);

  List<Item> findByTenantIdAndDeletedFalseOrderByCreatedAtDesc(long tenantId);

  Optional<Item> findByIdAndTenantIdAndDeletedFalse(long id, long tenantId);

  Optional<Item> findByIdAndDeletedFalse(long id);

  boolean existsByIdAndTenantIdAndDeletedFalse(long id, long tenantId);

  // Check if item exists by name or code, only for non-deleted items
  boolean existsByItemNameAndTenantIdAndDeletedFalse(String itemName, long tenantId);
  boolean existsByItemCodeAndTenantIdAndDeletedFalse(String itemCode, long tenantId);
  
  // Find deleted items by name or code
  Optional<Item> findByItemNameAndTenantIdAndDeletedTrue(String itemName, long tenantId);
  Optional<Item> findByItemCodeAndTenantIdAndDeletedTrue(String itemCode, long tenantId);
  
  // Check if item exists by name or code, regardless of deletion status
  @Query("SELECT COUNT(i) > 0 FROM Item i WHERE i.itemName = :itemName AND i.tenant.id = :tenantId")
  boolean existsByItemNameAndTenantId(@Param("itemName") String itemName, @Param("tenantId") long tenantId);
  
  @Query("SELECT COUNT(i) > 0 FROM Item i WHERE i.itemCode = :itemCode AND i.tenant.id = :tenantId")
  boolean existsByItemCodeAndTenantId(@Param("itemCode") String itemCode, @Param("tenantId") long tenantId);
}
