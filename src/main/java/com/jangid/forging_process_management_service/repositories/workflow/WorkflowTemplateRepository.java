package com.jangid.forging_process_management_service.repositories.workflow;

import com.jangid.forging_process_management_service.entities.workflow.WorkflowStep;
import com.jangid.forging_process_management_service.entities.workflow.WorkflowTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkflowTemplateRepository extends JpaRepository<WorkflowTemplate, Long> {

    @Query("SELECT wt FROM WorkflowTemplate wt WHERE wt.tenant.id = :tenantId AND wt.isActive = true AND wt.deleted = false ORDER BY wt.isDefault DESC, wt.createdAt DESC")
    List<WorkflowTemplate> findByTenantIdAndIsActiveTrueAndDeletedFalse(@Param("tenantId") Long tenantId);
    

    @Query("SELECT wt FROM WorkflowTemplate wt WHERE wt.tenant.id = :tenantId AND wt.deleted = false ORDER BY wt.isDefault DESC, wt.createdAt DESC")
    List<WorkflowTemplate> findByTenantIdAndDeletedFalse(@Param("tenantId") Long tenantId);

    Optional<WorkflowTemplate> findByIdAndDeletedFalse(Long id);
    
    Optional<WorkflowTemplate> findByTenantIdAndWorkflowNameAndDeletedFalse(Long tenantId, String workflowName);
    
    List<WorkflowTemplate> findByTenantIdAndIsDefaultTrueAndDeletedFalse(Long tenantId);

    @Query("SELECT wt FROM WorkflowTemplate wt WHERE wt.tenant.id = :tenantId AND wt.isActive = true AND wt.deleted = false ORDER BY wt.isDefault DESC, wt.createdAt DESC")
    Page<WorkflowTemplate> findActiveWorkflowTemplatesOrderedByDefault(@Param("tenantId") Long tenantId, Pageable pageable);
    
    @Query("SELECT wt FROM WorkflowTemplate wt WHERE wt.tenant.id = :tenantId AND wt.deleted = false ORDER BY wt.isDefault DESC, wt.createdAt DESC")
    Page<WorkflowTemplate> findAllWorkflowTemplatesOrderedByDefault(@Param("tenantId") Long tenantId, Pageable pageable);

    // Search workflow templates by name (case-insensitive partial match)
    @Query("SELECT wt FROM WorkflowTemplate wt WHERE wt.tenant.id = :tenantId AND LOWER(wt.workflowName) LIKE LOWER(CONCAT('%', :workflowName, '%')) AND wt.deleted = false ORDER BY wt.isDefault DESC, wt.createdAt DESC")
    Page<WorkflowTemplate> findByTenantIdAndWorkflowNameContainingIgnoreCaseAndDeletedFalse(@Param("tenantId") Long tenantId, @Param("workflowName") String workflowName, Pageable pageable);

    // Search workflow templates by operation type
    @Query("SELECT DISTINCT wt FROM WorkflowTemplate wt JOIN wt.workflowSteps ws WHERE wt.tenant.id = :tenantId AND ws.operationType = :operationType AND wt.deleted = false ORDER BY wt.isDefault DESC, wt.createdAt DESC")
    Page<WorkflowTemplate> findByTenantIdAndWorkflowStepsOperationTypeAndDeletedFalse(@Param("tenantId") Long tenantId, @Param("operationType") WorkflowStep.OperationType operationType, Pageable pageable);
} 