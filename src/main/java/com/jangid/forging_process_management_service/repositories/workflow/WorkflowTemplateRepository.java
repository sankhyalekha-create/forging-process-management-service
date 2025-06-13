package com.jangid.forging_process_management_service.repositories.workflow;

import com.jangid.forging_process_management_service.entities.workflow.WorkflowTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkflowTemplateRepository extends JpaRepository<WorkflowTemplate, Long> {

    List<WorkflowTemplate> findByTenantIdAndIsActiveTrueAndDeletedFalse(Long tenantId);
    
    List<WorkflowTemplate> findByTenantIdAndDeletedFalse(Long tenantId);
    
    Optional<WorkflowTemplate> findByIdAndDeletedFalse(Long id);
    
    Optional<WorkflowTemplate> findByTenantIdAndWorkflowNameAndDeletedFalse(Long tenantId, String workflowName);
    
    List<WorkflowTemplate> findByTenantIdAndIsDefaultTrueAndDeletedFalse(Long tenantId);
    
    @Query("SELECT wt FROM WorkflowTemplate wt WHERE wt.tenant.id = :tenantId AND wt.isActive = true AND wt.deleted = false ORDER BY wt.isDefault DESC, wt.workflowName ASC")
    List<WorkflowTemplate> findActiveWorkflowTemplatesOrderedByDefault(@Param("tenantId") Long tenantId);
} 