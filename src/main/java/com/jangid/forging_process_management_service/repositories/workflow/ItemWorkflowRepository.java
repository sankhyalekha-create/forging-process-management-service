package com.jangid.forging_process_management_service.repositories.workflow;

import com.jangid.forging_process_management_service.entities.workflow.ItemWorkflow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ItemWorkflowRepository extends JpaRepository<ItemWorkflow, Long> {

    List<ItemWorkflow> findByItemIdAndDeletedFalse(Long itemId);
    
    List<ItemWorkflow> findByWorkflowTemplateIdAndDeletedFalse(Long workflowTemplateId);
    
    @Query("SELECT iw FROM ItemWorkflow iw WHERE iw.item.tenant.id = :tenantId AND iw.deleted = false")
    List<ItemWorkflow> findByTenantIdAndDeletedFalse(@Param("tenantId") Long tenantId);
    
    @Query("SELECT iw FROM ItemWorkflow iw WHERE iw.workflowStatus = :status AND iw.deleted = false")
    List<ItemWorkflow> findByWorkflowStatusAndDeletedFalse(@Param("status") ItemWorkflow.WorkflowStatus status);
    
    @Query("SELECT iw FROM ItemWorkflow iw WHERE iw.item.tenant.id = :tenantId AND iw.workflowStatus = :status AND iw.deleted = false")
    List<ItemWorkflow> findByTenantIdAndWorkflowStatusAndDeletedFalse(@Param("tenantId") Long tenantId, @Param("status") ItemWorkflow.WorkflowStatus status);
    
    // Batch-level workflow queries
    @Query("SELECT iw FROM ItemWorkflow iw WHERE iw.item.id = :itemId AND iw.workflowIdentifier = :workflowIdentifier AND iw.deleted = false")
    Optional<ItemWorkflow> findByItemIdAndWorkflowIdentifierAndDeletedFalse(@Param("itemId") Long itemId,
                                                                          @Param("workflowIdentifier") String workflowIdentifier);
    
    @Query("SELECT iw FROM ItemWorkflow iw WHERE iw.workflowIdentifier = :workflowIdentifier AND iw.deleted = false")
    List<ItemWorkflow> findByWorkflowIdentifierAndDeletedFalse(@Param("workflowIdentifier") String workflowIdentifier);
    
    // Item-level workflow queries (where workflowIdentifier is null)
    @Query("SELECT iw FROM ItemWorkflow iw WHERE iw.item.id = :itemId AND iw.workflowIdentifier IS NULL AND iw.deleted = false")
    Optional<ItemWorkflow> findItemLevelWorkflowByItemId(@Param("itemId") Long itemId);
    
    // Batch-level workflow queries (where workflowIdentifier is not null)
    @Query("SELECT iw FROM ItemWorkflow iw WHERE iw.item.id = :itemId AND iw.workflowIdentifier IS NOT NULL AND iw.deleted = false")
    List<ItemWorkflow> findBatchLevelWorkflowsByItemId(@Param("itemId") Long itemId);
}   