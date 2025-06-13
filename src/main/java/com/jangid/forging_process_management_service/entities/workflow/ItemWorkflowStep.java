package com.jangid.forging_process_management_service.entities.workflow;

import com.jangid.forging_process_management_service.dto.workflow.OperationOutcomeData;

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

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
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
        this.startedAt = LocalDateTime.now();
    }

    public void completeStep() {
        this.stepStatus = StepStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    public void skipStep(String reason) {
        this.stepStatus = StepStatus.SKIPPED;
        this.completedAt = LocalDateTime.now();
        this.notes = reason;
    }

    public void failStep(String reason) {
        this.stepStatus = StepStatus.FAILED;
        this.completedAt = LocalDateTime.now();
        this.notes = reason;
    }

    public boolean isCompleted() {
        return stepStatus == StepStatus.COMPLETED || stepStatus == StepStatus.SKIPPED;
    }

    // Pieces tracking helper methods
    
    /**
     * Sets the initial pieces count when an operation completes
     * This value should never change after being set
     */
    public void setInitialPiecesProduced(Integer initialCount) {
        this.initialPiecesCount = initialCount;
        this.piecesAvailableForNext = initialCount; // Initially, all pieces are available
    }
    
    /**
     * Consumes pieces for the next operation and updates available count
     * @param piecesToConsume Number of pieces to consume
     * @return true if consumption was successful, false if insufficient pieces
     */
    public boolean consumePieces(Integer piecesToConsume) {
        if (piecesToConsume == null || piecesToConsume <= 0) {
            return false;
        }
        
        if (piecesAvailableForNext == null || piecesAvailableForNext < piecesToConsume) {
            return false; // Insufficient pieces available
        }
        
        piecesAvailableForNext = piecesAvailableForNext - piecesToConsume;
        return true;
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
     * Checks if all pieces from this operation have been consumed
     */
    public boolean areAllPiecesConsumed() {
        return piecesAvailableForNext != null && piecesAvailableForNext == 0;
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