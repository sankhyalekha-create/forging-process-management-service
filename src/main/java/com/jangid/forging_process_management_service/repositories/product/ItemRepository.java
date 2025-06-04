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

  // Search methods for item name and code substring with pagination support
  @Query("""
        SELECT i
        FROM Item i
        WHERE i.tenant.id = :tenantId
          AND LOWER(i.itemName) LIKE LOWER(CONCAT('%', :itemName, '%'))
          AND i.deleted = false
        ORDER BY i.itemName ASC
    """)
  Page<Item> findItemsByItemNameContainingIgnoreCase(@Param("tenantId") Long tenantId, @Param("itemName") String itemName, Pageable pageable);

  @Query("""
        SELECT i
        FROM Item i
        WHERE i.tenant.id = :tenantId
          AND LOWER(i.itemCode) LIKE LOWER(CONCAT('%', :itemCode, '%'))
          AND i.deleted = false
        ORDER BY i.itemCode ASC
    """)
  Page<Item> findItemsByItemCodeContainingIgnoreCase(@Param("tenantId") Long tenantId, @Param("itemCode") String itemCode, Pageable pageable);

  // Legacy search methods without pagination (keep for backward compatibility if needed)
  @Query("""
        SELECT i
        FROM Item i
        WHERE i.tenant.id = :tenantId
          AND LOWER(i.itemName) LIKE LOWER(CONCAT('%', :itemName, '%'))
          AND i.deleted = false
        ORDER BY i.itemName ASC
    """)
  List<Item> findItemsByItemNameContainingIgnoreCaseList(@Param("tenantId") Long tenantId, @Param("itemName") String itemName);

  @Query("""
        SELECT i
        FROM Item i
        WHERE i.tenant.id = :tenantId
          AND LOWER(i.itemCode) LIKE LOWER(CONCAT('%', :itemCode, '%'))
          AND i.deleted = false
        ORDER BY i.itemCode ASC
    """)
  List<Item> findItemsByItemCodeContainingIgnoreCaseList(@Param("tenantId") Long tenantId, @Param("itemCode") String itemCode);
}
