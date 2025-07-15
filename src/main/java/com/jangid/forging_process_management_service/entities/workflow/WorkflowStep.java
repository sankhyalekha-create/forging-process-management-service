package com.jangid.forging_process_management_service.entities.workflow;

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

    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;

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