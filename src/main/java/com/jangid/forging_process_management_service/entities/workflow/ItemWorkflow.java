package com.jangid.forging_process_management_service.entities.workflow;

import com.jangid.forging_process_management_service.entities.product.Item;
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
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.CascadeType;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "item_workflow")
@EntityListeners(AuditingEntityListener.class)
public class ItemWorkflow {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "item_workflow_sequence")
    @SequenceGenerator(name = "item_workflow_sequence", sequenceName = "item_workflow_sequence", allocationSize = 1)
    private Long id;

  /**
   * -- SETTER --
   *  Sets the batch identifier for this workflow
   */
  // Universal Master Workflow Identifier - generated when first operation starts
    @Setter
    @Column(name = "workflow_identifier")
    private String workflowIdentifier;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_template_id", nullable = false)
    private WorkflowTemplate workflowTemplate;

    @Enumerated(EnumType.STRING)
    @Column(name = "workflow_status", nullable = false)
    @Builder.Default
    private WorkflowStatus workflowStatus = WorkflowStatus.NOT_STARTED;

    @OneToMany(mappedBy = "itemWorkflow", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ItemWorkflowStep> itemWorkflowSteps = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted", nullable = false)
    @Builder.Default
    private Boolean deleted = false;

    public enum WorkflowStatus {
        NOT_STARTED,
        IN_PROGRESS,
        COMPLETED,
        CANCELLED,
        ON_HOLD
    }


    /**
     * Gets workflow step by operation type
     */
    public ItemWorkflowStep getStepByOperationType(WorkflowStep.OperationType operationType) {
        return itemWorkflowSteps.stream()
            .filter(step -> step.getWorkflowStep().getOperationType() == operationType)
            .findFirst()
            .orElse(null);
    }

    /**
     * Checks if dependencies for an operation are satisfied using tree structure
     * An operation can start if its parent step has started (IN_PROGRESS or COMPLETED)
     */
    private boolean checkOperationDependencies(ItemWorkflowStep targetStep) {
        if (targetStep == null) {
            return false;
        }

        WorkflowStep workflowStep = targetStep.getWorkflowStep();
        
        // If this is a root step (no parent), it can start
        if (workflowStep.isRootStep()) {
            return true;
        }

        // Get the parent step
        WorkflowStep parentStep = workflowStep.getParentStep();
        if (parentStep == null) {
            return true; // Should not happen, but safe fallback
        }

        // Get the parent's execution status
        ItemWorkflowStep parentItemStep = targetStep.getParentItemWorkflowStep();
        if (parentItemStep == null) {
            return false;
        }

        // Can start if parent has started (IN_PROGRESS or COMPLETED)
        return parentItemStep.getStepStatus() == ItemWorkflowStep.StepStatus.IN_PROGRESS ||
               parentItemStep.getStepStatus() == ItemWorkflowStep.StepStatus.COMPLETED;
    }

    /**
     * Starts a specific operation step if it can be started
     */
    public void startOperationStep(ItemWorkflowStep step) {
        if (!canStartOperation(step)) {
            throw new IllegalStateException("Cannot start operation step" + step.getOperationType() +
                ". Dependencies not met or step in wrong state.");
        }

        step.startStep();

        // Update workflow status
        if (workflowStatus == WorkflowStatus.NOT_STARTED) {
            this.workflowStatus = WorkflowStatus.IN_PROGRESS;
        }
    }


    public boolean isActive() {
        return workflowStatus == WorkflowStatus.NOT_STARTED || workflowStatus == WorkflowStatus.IN_PROGRESS;
    }

    public boolean isCompleted() {
        return workflowStatus == WorkflowStatus.COMPLETED;
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


  /**
     * Checks if a specific operation can start based on dependencies
     */
    public boolean canStartOperation(ItemWorkflowStep step) {

        // Operation can start if it's PENDING and dependencies are met
        return step.getStepStatus() == ItemWorkflowStep.StepStatus.PENDING && 
               checkOperationDependencies(step);
    }


    public ItemWorkflowStep getFirstRootStep(WorkflowStep.OperationType operationType) {
        if (itemWorkflowSteps == null || itemWorkflowSteps.isEmpty()) {
            return null;
        }

        return itemWorkflowSteps.stream()
            .filter(step -> step.getParentItemWorkflowStep() == null && step.getOperationType()==operationType)
            .findFirst()
            .orElse(null);
    }
} 