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

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

    // Universal Master Workflow Identifier - generated when first operation starts
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
     * Gets all workflow steps that can be started based on dependencies
     * Any operation can start if its immediate predecessor has started
     */
    public List<ItemWorkflowStep> getAvailableSteps() {
        return itemWorkflowSteps.stream()
            .filter(step -> step.getStepStatus() == ItemWorkflowStep.StepStatus.PENDING || step.getStepStatus() == ItemWorkflowStep.StepStatus.IN_PROGRESS)
            .filter(step -> checkOperationDependencies(step.getWorkflowStep().getOperationType()))
            .collect(Collectors.toList());
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
     * Checks if dependencies for an operation are satisfied
     * An operation can start if its immediate predecessor has started
     */
    private boolean checkOperationDependencies(WorkflowStep.OperationType operationType) {
        ItemWorkflowStep targetStep = getStepByOperationType(operationType);
        if (targetStep == null) {
            return false;
        }

        // Get the immediate predecessor step
        WorkflowStep prevStep = workflowTemplate.getWorkflowSteps().stream()
            .filter(step -> step.getStepOrder() == targetStep.getWorkflowStep().getStepOrder() - 1)
            .findFirst()
            .orElse(null);

        // If no predecessor (first step), can start
        if (prevStep == null) {
            return true;
        }

        // Get the predecessor's execution status
        ItemWorkflowStep prevItemStep = getStepByOperationType(prevStep.getOperationType());
        if (prevItemStep == null) {
            return false;
        }

        // Can start if predecessor has started (IN_PROGRESS or COMPLETED)
        return prevItemStep.getStepStatus() == ItemWorkflowStep.StepStatus.IN_PROGRESS ||
               prevItemStep.getStepStatus() == ItemWorkflowStep.StepStatus.COMPLETED;
    }

    /**
     * Starts a specific operation step if it can be started
     */
    public void startOperationStep(WorkflowStep.OperationType operationType) {
        if (!canStartOperation(operationType)) {
            throw new IllegalStateException("Cannot start operation " + operationType + 
                ". Dependencies not met or step in wrong state.");
        }

        ItemWorkflowStep step = getStepByOperationType(operationType);
        step.startStep();

        // Update workflow status
        if (workflowStatus == WorkflowStatus.NOT_STARTED) {
            this.workflowStatus = WorkflowStatus.IN_PROGRESS;
            this.startedAt = LocalDateTime.now();
        }
    }

    /**
     * Checks if all required workflow steps are completed and updates workflow status
     */
    private void checkAndUpdateWorkflowCompletion() {
        boolean allRequiredCompleted = itemWorkflowSteps.stream()
            .filter(step -> !step.getWorkflowStep().getIsOptional())
            .allMatch(step -> step.isCompleted());
            
        if (allRequiredCompleted) {
            this.workflowStatus = WorkflowStatus.COMPLETED;
            this.completedAt = LocalDateTime.now();
        }
    }

    /**
     * Consumes pieces from a completed operation for use in another operation
     */
    public boolean consumePiecesFromOperation(WorkflowStep.OperationType sourceOperation, 
                                            WorkflowStep.OperationType targetOperation, 
                                            Integer piecesToConsume) {
        ItemWorkflowStep sourceStep = getStepByOperationType(sourceOperation);
        if (sourceStep == null || !sourceStep.isCompleted()) {
            return false;
        }
        
        Integer availablePieces = sourceStep.getPiecesAvailableForNext();
        if (availablePieces == null || availablePieces < piecesToConsume) {
            return false;
        }
        
        // Update available pieces
        sourceStep.setPiecesAvailableForNext(availablePieces - piecesToConsume);
        
        // Record consumption in target step if it exists
        ItemWorkflowStep targetStep = getStepByOperationType(targetOperation);
        if (targetStep != null) {
            String note = String.format("Consumed %d pieces from %s operation", 
                                      piecesToConsume, sourceOperation);
            targetStep.setNotes(note);
        }
        
        return true;
    }

    // Remove legacy methods that assume sequential processing
    @Deprecated
    public void startWorkflow() {
        if (this.workflowStatus == WorkflowStatus.NOT_STARTED) {
            this.workflowStatus = WorkflowStatus.IN_PROGRESS;
            this.startedAt = LocalDateTime.now();
        }
    }

    @Deprecated
    public void completeWorkflow() {
        this.workflowStatus = WorkflowStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    @Deprecated
    public void cancelWorkflow(String reason) {
        this.workflowStatus = WorkflowStatus.CANCELLED;
        this.completedAt = LocalDateTime.now();
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
     * Checks if this is an item-level workflow (no batch information)
     * @return true if this is an item-level workflow, false if it's a batch-level workflow
     */
    public boolean isItemLevelWorkflow() {
        return workflowIdentifier == null;
    }

    /**
     * Checks if this is a batch-level workflow (has batch information)
     * @return true if this is a batch-level workflow, false if it's an item-level workflow
     */
    public boolean isBatchLevelWorkflow() {
        return workflowIdentifier != null;
    }

    /**
     * Sets the batch identifier for this workflow
     */
    public void setWorkflowIdentifier(String workflowIdentifier) {
        this.workflowIdentifier = workflowIdentifier;
    }

    /**
     * Checks if a specific operation can start based on dependencies
     */
    public boolean canStartOperation(WorkflowStep.OperationType operationType) {
        ItemWorkflowStep step = getStepByOperationType(operationType);
        if (step == null) {
            return false;
        }

        // Operation can start if it's PENDING and dependencies are met
        return step.getStepStatus() == ItemWorkflowStep.StepStatus.PENDING && 
               checkOperationDependencies(operationType);
    }

    /**
     * Moves to the next step in the workflow sequence
     * In parallel operations design, this updates workflow status if all steps are complete
     */
    public void moveToNextStep() {
        checkAndUpdateWorkflowCompletion();
    }

    /**
     * Checks if the workflow is completed
     */
    public boolean isWorkflowCompleted() {
        return workflowStatus == WorkflowStatus.COMPLETED;
    }
} 