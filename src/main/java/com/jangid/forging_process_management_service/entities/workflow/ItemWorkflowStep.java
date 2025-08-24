package com.jangid.forging_process_management_service.entities.workflow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
@Table(name = "item_workflow_step")
@EntityListeners(AuditingEntityListener.class)
public class ItemWorkflowStep {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "item_workflow_step_sequence")
    @SequenceGenerator(name = "item_workflow_step_sequence", sequenceName = "item_workflow_step_sequence", allocationSize = 1)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_workflow_id", nullable = false)
    private ItemWorkflow itemWorkflow;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_step_id", nullable = false)
    private WorkflowStep workflowStep;

    // Parent ItemWorkflowStep for tree navigation (mirrors WorkflowStep.parentStep)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_item_workflow_step_id")
    private ItemWorkflowStep parentItemWorkflowStep;

    @OneToMany(mappedBy = "parentItemWorkflowStep", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ItemWorkflowStep> childItemWorkflowSteps = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false)
    private WorkflowStep.OperationType operationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "step_status", nullable = false)
    @Builder.Default
    private StepStatus stepStatus = StepStatus.PENDING;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "operation_reference_id")
    private Long operationReferenceId; // References the actual operation entity (forge_id, heat_treatment_id, etc.)

    @Column(name = "notes", length = 1000)
    private String notes;

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

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "operation_outcome_data")
    private String operationOutcomeData; // Optimized JSON storage for workflow flow control

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "related_entity_ids")
    private List<Long> relatedEntityIds; // List of IDs of operation-specific entities

    @Column(name = "initial_pieces_count")
    private Integer initialPiecesCount; // Original pieces produced by this operation (never changes)

    @Column(name = "pieces_available_for_next")
    private Integer piecesAvailableForNext; // Current remaining pieces available for next operation (gets decremented)

    public enum StepStatus {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        SKIPPED,
        FAILED
    }

    // Helper methods
    public void startStep() {
        this.stepStatus = StepStatus.IN_PROGRESS;
    }


    public boolean isCompleted() {
        return stepStatus == StepStatus.COMPLETED || stepStatus == StepStatus.SKIPPED;
    }


    /**
     * Gets the number of pieces that have been consumed from this operation
     * @return consumed pieces count, or null if initial count is not set
     */
    public Integer getConsumedPiecesCount() {
        if (initialPiecesCount == null) {
            return null;
        }
        
        if (piecesAvailableForNext == null) {
            return initialPiecesCount; // All pieces consumed
        }
        
        return initialPiecesCount - piecesAvailableForNext;
    }
    

    /**
     * Gets the utilization percentage of pieces from this operation
     * @return percentage of pieces consumed (0.0 to 1.0), or null if no initial count
     */
    public Double getPiecesUtilizationPercentage() {
        if (initialPiecesCount == null || initialPiecesCount == 0) {
            return null;
        }
        
        Integer consumed = getConsumedPiecesCount();
        if (consumed == null) {
            return null;
        }
        
        return (double) consumed / initialPiecesCount;
    }

    /**
     * Add a child ItemWorkflowStep
     */
    public void addChildItemWorkflowStep(ItemWorkflowStep childStep) {
        if (childItemWorkflowSteps == null) {
            childItemWorkflowSteps = new ArrayList<>();
        }
        childStep.setParentItemWorkflowStep(this);
        this.childItemWorkflowSteps.add(childStep);
    }
    
    /**
     * Get the tree level/depth of this ItemWorkflowStep (root = 0)
     */
    public int getItemWorkflowTreeLevel() {
        int level = 0;
        ItemWorkflowStep current = this.parentItemWorkflowStep;
        while (current != null) {
            level++;
            current = current.parentItemWorkflowStep;
        }
        return level;
    }


    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        // Set operationType from workflowStep if not already set
        if (operationType == null && workflowStep != null) {
            operationType = workflowStep.getOperationType();
        }
        // Auto-set parent ItemWorkflowStep based on WorkflowStep parent relationship
        // This will be handled by the service layer when creating ItemWorkflowSteps
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        // Keep operationType in sync with workflowStep
        if (operationType == null && workflowStep != null) {
            operationType = workflowStep.getOperationType();
        }
    }
} 