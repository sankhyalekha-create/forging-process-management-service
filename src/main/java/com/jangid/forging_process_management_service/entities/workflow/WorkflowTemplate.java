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

import jakarta.persistence.*;
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
    @OrderBy("stepOrder ASC")
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

    // Helper methods
    public void addWorkflowStep(WorkflowStep step) {
        if (workflowSteps == null) {
            workflowSteps = new ArrayList<>();
        }
        step.setWorkflowTemplate(this);
        this.workflowSteps.add(step);
    }

    public WorkflowStep getFirstStep() {
        if (workflowSteps == null) {
            return null;
        }
        return workflowSteps.stream()
                .filter(step -> step.getStepOrder() == 1)
                .findFirst()
                .orElse(null);
    }

    public WorkflowStep getNextStep(WorkflowStep currentStep) {
        if (workflowSteps == null) {
            return null;
        }
        return workflowSteps.stream()
                .filter(step -> step.getStepOrder() == currentStep.getStepOrder() + 1)
                .findFirst()
                .orElse(null);
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
} 