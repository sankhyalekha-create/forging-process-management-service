package com.jangid.forging_process_management_service.entities.workflow;

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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Column;
import jakarta.persistence.OneToMany;
import jakarta.persistence.CascadeType;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
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
@Table(name = "workflow_step")
@EntityListeners(AuditingEntityListener.class)
public class WorkflowStep {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "workflow_step_sequence")
    @SequenceGenerator(name = "workflow_step_sequence", sequenceName = "workflow_step_sequence", allocationSize = 1)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_template_id", nullable = false)
    private WorkflowTemplate workflowTemplate;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false)
    private OperationType operationType;

    // Parent-Child relationship for tree structure
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_step_id")
    private WorkflowStep parentStep;

    @OneToMany(mappedBy = "parentStep", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<WorkflowStep> childSteps = new ArrayList<>();

    @Column(name = "step_name", nullable = false)
    private String stepName;

    @Column(name = "step_description")
    private String stepDescription;

    @Column(name = "is_optional", nullable = false)
    @Builder.Default
    private Boolean isOptional = false;

    @Column(name = "is_parallel", nullable = false)
    @Builder.Default
    private Boolean isParallel = false;

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

    public enum OperationType {
        FORGING("Forging"),
        HEAT_TREATMENT("Heat Treatment"),
        MACHINING("Machining"),
        VENDOR("Vendor"),
        QUALITY("Quality"),
        DISPATCH("Dispatch");

        private final String displayName;

        OperationType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // NEW: Tree helper methods
    
    /**
     * Check if this is a root step (no parent)
     */
    public boolean isRootStep() {
        return parentStep == null;
    }

    /**
     * Check if this step can have multiple children (branching point)
     */
    public boolean isBranchingStep() {
        return getActiveChildSteps().size() > 1;
    }


    /**
     * Get immediate child steps (non-deleted)
     */
    public List<WorkflowStep> getActiveChildSteps() {
        if (childSteps == null) {
            return new ArrayList<>();
        }
        return childSteps.stream()
            .filter(step -> !step.getDeleted())
            .collect(Collectors.toList());
    }

    /**
     * Add a child step
     */
    public void addChildStep(WorkflowStep childStep) {
        if (childSteps == null) {
            childSteps = new ArrayList<>();
        }
        childStep.setParentStep(this);
        this.childSteps.add(childStep);
    }


    /**
     * Get the level/depth of this step in the tree (root = 0)
     */
    public int getTreeLevel() {
        int level = 0;
        WorkflowStep current = this.parentStep;
        while (current != null) {
            level++;
            current = current.parentStep;
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
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "WorkflowStep{" +
                "id=" + id +
                ", operationType=" + operationType +
                ", stepName='" + stepName + '\'' +
                ", treeLevel=" + getTreeLevel() +
                ", parentStepId=" + (parentStep != null ? parentStep.getId() : null) +
                ", childStepsCount=" + (childSteps != null ? childSteps.size() : 0) +
                '}';
    }
} 