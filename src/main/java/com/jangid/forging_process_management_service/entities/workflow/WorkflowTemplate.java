package com.jangid.forging_process_management_service.entities.workflow;

import com.jangid.forging_process_management_service.entities.Tenant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.CascadeType;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "workflow_template")
@EntityListeners(AuditingEntityListener.class)
public class WorkflowTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "workflow_template_sequence")
    @SequenceGenerator(name = "workflow_template_sequence", sequenceName = "workflow_template_sequence", allocationSize = 1)
    private Long id;

    @Column(name = "workflow_name", nullable = false)
    private String workflowName;

    @Column(name = "workflow_description")
    private String workflowDescription;

    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private Boolean isDefault = false;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @OneToMany(mappedBy = "workflowTemplate", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    // Note: Tree structure doesn't have a natural ordering, use helper methods for specific orderings
    @Builder.Default
    private List<WorkflowStep> workflowSteps = new ArrayList<>();

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted", nullable = false)
    @Builder.Default
    private Boolean deleted = false;

    // UPDATED: Tree-based helper methods
    
    /**
     * Add a workflow step to the template
     */
    public void addWorkflowStep(WorkflowStep step) {
        if (workflowSteps == null) {
            workflowSteps = new ArrayList<>();
        }
        step.setWorkflowTemplate(this);
        this.workflowSteps.add(step);
    }

    /**
     * Get all root steps (steps with no parent) - NEW tree-based method
     */
    public List<WorkflowStep> getRootSteps() {
        if (workflowSteps == null) {
            return new ArrayList<>();
        }
        return workflowSteps.stream()
            .filter(WorkflowStep::isRootStep)
            .filter(step -> !step.getDeleted())
            .collect(Collectors.toList());
    }


    public WorkflowStep getFirstStepByOperationType(WorkflowStep.OperationType operationType) {
        return getRootSteps().stream()
            .filter(step -> step.getOperationType() == operationType)
            .findFirst()
            .orElse(null);
    }

    public boolean isFirstOperationType(WorkflowStep.OperationType operationType) {
        return getFirstStepByOperationType(operationType) != null;
    }

    /**
     * Get all branching steps (steps with multiple children)
     */
    public List<WorkflowStep> getBranchingSteps() {
        if (workflowSteps == null) {
            return new ArrayList<>();
        }
        return workflowSteps.stream()
            .filter(step -> !step.getDeleted())
            .filter(WorkflowStep::isBranchingStep)
            .collect(Collectors.toList());
    }

    /**
     * Validate tree structure (no cycles, proper hierarchy, etc.)
     */
    public boolean isValidWorkflowTree() {
        if (workflowSteps == null || workflowSteps.isEmpty()) {
            return true;
        }

        List<WorkflowStep> activeSteps = workflowSteps.stream()
            .filter(step -> !step.getDeleted())
            .toList();

        // Check if we have at least one root step
        List<WorkflowStep> rootSteps = getRootSteps();
        if (rootSteps.isEmpty()) {
            return false;
        }

        // Check for cycles and validate each step
        return activeSteps.stream().allMatch(this::hasNoCircularDependency);
    }

    /**
     * Check if a step has circular dependency
     */
    private boolean hasNoCircularDependency(WorkflowStep step) {
        Set<WorkflowStep> visited = new HashSet<>();
        WorkflowStep current = step;
        
        while (current != null) {
            if (visited.contains(current)) {
                return false; // Cycle detected
            }
            visited.add(current);
            current = current.getParentStep();
        }
        return true;
    }

    /**
     * Check if this workflow template supports branching
     */
    public boolean supportsBranching() {
        return !getBranchingSteps().isEmpty();
    }

    /**
     * Get all possible workflow paths from root to leaf
     */
    public List<List<WorkflowStep>> getAllWorkflowPaths() {
        List<List<WorkflowStep>> allPaths = new ArrayList<>();
        List<WorkflowStep> rootSteps = getRootSteps();
        
        for (WorkflowStep root : rootSteps) {
            List<WorkflowStep> currentPath = new ArrayList<>();
            collectAllPaths(root, currentPath, allPaths);
        }
        
        return allPaths;
    }

    /**
     * Recursively collect all paths from a given step to leaf steps
     */
    private void collectAllPaths(WorkflowStep currentStep, List<WorkflowStep> currentPath, List<List<WorkflowStep>> allPaths) {
        currentPath.add(currentStep);
        
        List<WorkflowStep> children = currentStep.getActiveChildSteps();
        if (children.isEmpty()) {
            // Leaf step reached, add the complete path
            allPaths.add(new ArrayList<>(currentPath));
        } else {
            // Continue with each child
            for (WorkflowStep child : children) {
                collectAllPaths(child, currentPath, allPaths);
            }
        }
        
        // Backtrack
        currentPath.remove(currentPath.size() - 1);
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "WorkflowTemplate{" +
                "id=" + id +
                ", workflowName='" + workflowName + '\'' +
                ", stepsCount=" + (workflowSteps != null ? workflowSteps.size() : 0) +
                ", supportsBranching=" + supportsBranching() +
                '}';
    }
} 