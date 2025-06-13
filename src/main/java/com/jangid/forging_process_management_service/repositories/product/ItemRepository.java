package com.jangid.forging_process_management_service.repositories.product;

import com.jangid.forging_process_management_service.entities.product.Item;
import com.jangid.forging_process_management_service.entities.workflow.WorkflowStep;

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

  // Methods with workflow information
  @Query("""
        SELECT DISTINCT i
        FROM Item i
        LEFT JOIN FETCH i.itemWorkflows iw
        LEFT JOIN FETCH iw.workflowTemplate wt
        WHERE i.tenant.id = :tenantId AND i.deleted = false
        ORDER BY i.createdAt DESC
    """)
  Page<Item> findByTenantIdAndDeletedFalseWithWorkflowOrderByCreatedAtDesc(@Param("tenantId") long tenantId, Pageable pageable);

  @Query("""
        SELECT DISTINCT i
        FROM Item i
        LEFT JOIN FETCH i.itemWorkflows iw
        LEFT JOIN FETCH iw.workflowTemplate wt
        WHERE i.tenant.id = :tenantId AND i.deleted = false
        ORDER BY i.createdAt DESC
    """)
  List<Item> findByTenantIdAndDeletedFalseWithWorkflowOrderByCreatedAtDesc(@Param("tenantId") long tenantId);

  // Methods to find items by operation type in workflow steps
  @Query("""
        SELECT DISTINCT i
        FROM Item i
        LEFT JOIN FETCH i.itemWorkflows iw
        LEFT JOIN FETCH iw.workflowTemplate wt
        WHERE i.tenant.id = :tenantId 
          AND i.deleted = false 
          AND EXISTS (
            SELECT 1 FROM WorkflowStep ws2 
            WHERE ws2.workflowTemplate.id = wt.id 
              AND ws2.operationType = :operationType
          )
        ORDER BY i.createdAt DESC
    """)
  Page<Item> findByTenantIdAndOperationTypeWithWorkflow(@Param("tenantId") Long tenantId, 
                                                        @Param("operationType") WorkflowStep.OperationType operationType, 
                                                        Pageable pageable);

  @Query("""
        SELECT DISTINCT i
        FROM Item i
        LEFT JOIN FETCH i.itemWorkflows iw
        LEFT JOIN FETCH iw.workflowTemplate wt
        WHERE i.tenant.id = :tenantId 
          AND i.deleted = false 
          AND EXISTS (
            SELECT 1 FROM WorkflowStep ws2 
            WHERE ws2.workflowTemplate.id = wt.id 
              AND ws2.operationType = :operationType
          )
        ORDER BY i.createdAt DESC
    """)
  List<Item> findByTenantIdAndOperationTypeWithWorkflow(@Param("tenantId") Long tenantId, 
                                                        @Param("operationType") WorkflowStep.OperationType operationType);
}
