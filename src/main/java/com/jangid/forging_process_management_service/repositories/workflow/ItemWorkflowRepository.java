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
    
    // Check if any ItemWorkflow is using a specific WorkflowTemplate
    @Query("SELECT COUNT(iw) > 0 FROM ItemWorkflow iw WHERE iw.workflowTemplate.id = :templateId AND iw.deleted = false")
    boolean existsByWorkflowTemplateIdAndDeletedFalse(@Param("templateId") Long templateId);
    
    // Find ItemWorkflows using a specific WorkflowTemplate
    @Query("SELECT iw FROM ItemWorkflow iw WHERE iw.workflowTemplate.id = :templateId AND iw.deleted = false ORDER BY iw.createdAt DESC")
    List<ItemWorkflow> findByWorkflowTemplateIdAndDeletedFalse(@Param("templateId") Long templateId);
    
    // Find ItemWorkflows by both item ID and workflow template ID (only NOT_STARTED workflows not associated with any order)
    @Query("SELECT iw FROM ItemWorkflow iw WHERE iw.item.id = :itemId AND iw.workflowTemplate.id = :workflowTemplateId AND iw.deleted = false AND iw.workflowStatus = com.jangid.forging_process_management_service.entities.workflow.ItemWorkflow.WorkflowStatus.NOT_STARTED AND NOT EXISTS (SELECT 1 FROM com.jangid.forging_process_management_service.entities.order.OrderItemWorkflow oiw WHERE oiw.itemWorkflow.id = iw.id) ORDER BY iw.createdAt DESC")
    List<ItemWorkflow> findByItemIdAndWorkflowTemplateIdAndDeletedFalse(@Param("itemId") Long itemId, @Param("workflowTemplateId") Long workflowTemplateId);
}   