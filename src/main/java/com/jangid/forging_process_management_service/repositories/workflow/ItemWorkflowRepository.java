package com.jangid.forging_process_management_service.repositories.workflow;

import com.jangid.forging_process_management_service.entities.workflow.ItemWorkflow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ItemWorkflowRepository extends JpaRepository<ItemWorkflow, Long> {

    List<ItemWorkflow> findByItemIdAndDeletedFalse(Long itemId);

    @Query("SELECT iw FROM ItemWorkflow iw WHERE iw.workflowIdentifier = :workflowIdentifier AND iw.deleted = false")
    List<ItemWorkflow> findByWorkflowIdentifierAndDeletedFalse(@Param("workflowIdentifier") String workflowIdentifier);

    // Paginated queries for listing ItemWorkflows
    @Query("SELECT iw FROM ItemWorkflow iw WHERE iw.item.tenant.id = :tenantId AND iw.item.deleted = false AND iw.deleted = false ORDER BY iw.updatedAt DESC")
    Page<ItemWorkflow> findByTenantIdAndItemNotDeletedOrderByUpdatedAtDesc(@Param("tenantId") Long tenantId, Pageable pageable);
    
    // Non-paginated query for listing all ItemWorkflows
    @Query("SELECT iw FROM ItemWorkflow iw WHERE iw.item.tenant.id = :tenantId AND iw.item.deleted = false AND iw.deleted = false ORDER BY iw.updatedAt DESC")
    List<ItemWorkflow> findByTenantIdAndItemNotDeletedOrderByUpdatedAtDesc(@Param("tenantId") Long tenantId);
    
    // Query for getting workflows by item ID
    @Query("SELECT iw FROM ItemWorkflow iw WHERE iw.item.id = :itemId AND iw.deleted = false ORDER BY iw.createdAt DESC")
    List<ItemWorkflow> findByItemIdAndDeletedFalseOrderByCreatedAtDesc(@Param("itemId") Long itemId);
    
    // Search ItemWorkflows by item name
    @Query("SELECT iw FROM ItemWorkflow iw WHERE iw.item.tenant.id = :tenantId AND iw.item.deleted = false AND iw.deleted = false AND LOWER(iw.item.itemName) LIKE LOWER(CONCAT('%', :itemName, '%')) ORDER BY iw.updatedAt DESC")
    Page<ItemWorkflow> findByTenantIdAndItemNameContainingIgnoreCaseOrderByUpdatedAtDesc(@Param("tenantId") Long tenantId, @Param("itemName") String itemName, Pageable pageable);
    
    // Search ItemWorkflows by workflow identifier
    @Query("SELECT iw FROM ItemWorkflow iw WHERE iw.item.tenant.id = :tenantId AND iw.item.deleted = false AND iw.deleted = false AND LOWER(iw.workflowIdentifier) LIKE LOWER(CONCAT('%', :workflowIdentifier, '%')) ORDER BY iw.updatedAt DESC")
    Page<ItemWorkflow> findByTenantIdAndWorkflowIdentifierContainingIgnoreCaseOrderByUpdatedAtDesc(@Param("tenantId") Long tenantId, @Param("workflowIdentifier") String workflowIdentifier, Pageable pageable);
    
    // Eager loading method for ItemWorkflow with all relationships
    @Query("SELECT iw FROM ItemWorkflow iw " +
           "LEFT JOIN FETCH iw.itemWorkflowSteps iws " +
           "LEFT JOIN FETCH iws.workflowStep ws " +
           "LEFT JOIN FETCH iws.parentItemWorkflowStep " +
           "WHERE iw.id = :itemWorkflowId AND iw.deleted = false")
    Optional<ItemWorkflow> findByIdWithEagerLoading(@Param("itemWorkflowId") Long itemWorkflowId);
}   