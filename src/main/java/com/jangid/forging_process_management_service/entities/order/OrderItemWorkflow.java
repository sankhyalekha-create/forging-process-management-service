package com.jangid.forging_process_management_service.entities.order;

import com.jangid.forging_process_management_service.entities.workflow.ItemWorkflow;
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
import jakarta.persistence.Index;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "order_item_workflows", indexes = {
  @Index(name = "idx_order_item_workflow_order_item_id", columnList = "order_item_id"),
  @Index(name = "idx_order_item_workflow_item_workflow_id", columnList = "item_workflow_id")
})
@EntityListeners(AuditingEntityListener.class)
public class OrderItemWorkflow {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO, generator = "order_item_workflow_sequence")
  @SequenceGenerator(name = "order_item_workflow_sequence", sequenceName = "order_item_workflow_sequence", allocationSize = 1)
  private Long id;

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "order_item_id", nullable = false)
  private OrderItem orderItem;

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "item_workflow_id", nullable = false)
  private ItemWorkflow itemWorkflow;

  // ========== WORKFLOW EXECUTION DETAILS ==========
  // These fields represent the specific quantity, pricing, and work type for THIS workflow execution
  // Multiple OrderItemWorkflows for the same OrderItem can have different values
  
  @NotNull
  @Min(value = 1, message = "Quantity must be at least 1")
  @Column(name = "quantity", nullable = false)
  private Integer quantity;

  @Enumerated(EnumType.STRING)
  @Column(name = "work_type", nullable = false)
  @Builder.Default
  private WorkType workType = WorkType.WITH_MATERIAL;

  @Column(name = "unit_price", precision = 10, scale = 2)
  private BigDecimal unitPrice; // Total unit price based on work type

  @Column(name = "material_cost_per_unit", precision = 10, scale = 2)
  private BigDecimal materialCostPerUnit; // Material cost per unit (for WITH_MATERIAL)

  @Column(name = "job_work_cost_per_unit", precision = 10, scale = 2)
  private BigDecimal jobWorkCostPerUnit; // Job work (processing) cost per unit

  @Column(name = "special_instructions", length = 1000)
  private String specialInstructions;

  // ========== WORKFLOW SCHEDULING & TRACKING ==========

  @Min(value = 1, message = "Planned duration must be at least 1 day")
  @Column(name = "planned_duration_days")
  private Integer plannedDurationDays;

  @Column(name = "actual_start_date")
  private LocalDate actualStartDate;

  @Column(name = "actual_completion_date")
  private LocalDate actualCompletionDate;

  @Column(name = "actual_duration_days")
  private Integer actualDurationDays;

  @Column(name = "notes", length = 1000)
  private String notes;

  @Column(name = "priority", nullable = false)
  @Builder.Default
  private Integer priority = 1; // 1 = highest priority, higher numbers = lower priority

  @CreatedDate
  @Column(name = "created_at", updatable = false, nullable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  // Business methods
  public boolean isStarted() {
    return actualStartDate != null || 
      itemWorkflow.getWorkflowStatus() != 
        com.jangid.forging_process_management_service.entities.workflow.ItemWorkflow.WorkflowStatus.NOT_STARTED;
  }

  public boolean isCompleted() {
    return actualCompletionDate != null || 
      itemWorkflow.getWorkflowStatus() == 
        com.jangid.forging_process_management_service.entities.workflow.ItemWorkflow.WorkflowStatus.COMPLETED;
  }

  public boolean isInProgress() {
    return isStarted() && !isCompleted();
  }

  public boolean isDelayed() {
    if (plannedDurationDays == null || isCompleted()) {
      return false;
    }
    
    // Check if actual duration exceeds planned duration
    if (isCompleted() && actualDurationDays != null) {
      return actualDurationDays > plannedDurationDays;
    }
    
    // Check if current duration (from order start) exceeds planned duration
    if (actualStartDate != null) {
      LocalDate currentDate = LocalDate.now();
      int currentDuration = (int) java.time.temporal.ChronoUnit.DAYS.between(actualStartDate, currentDate) + 1;
      return currentDuration > plannedDurationDays;
    }
    
    return false;
  }

  public Integer calculateActualDurationDays() {
    if (actualStartDate != null && actualCompletionDate != null) {
      return (int) java.time.temporal.ChronoUnit.DAYS.between(actualStartDate, actualCompletionDate) + 1;
    }
    return null;
  }

  public Integer calculateRemainingDays() {
    if (isCompleted() || plannedDurationDays == null) {
      return 0;
    }
    
    if (actualStartDate != null) {
      LocalDate currentDate = LocalDate.now();
      int elapsedDays = (int) java.time.temporal.ChronoUnit.DAYS.between(actualStartDate, currentDate) + 1;
      return Math.max(0, plannedDurationDays - elapsedDays);
    }
    
    return plannedDurationDays;
  }

  public String getStatusSummary() {
    if (isCompleted()) {
      return "Completed";
    } else if (isInProgress()) {
      return "In Progress";
    } else {
      return "Not Started";
    }
  }

  public void updateActualDates() {
    // Sync actual dates with ItemWorkflow dates
    if (itemWorkflow.getStartedAt() != null) {
      this.actualStartDate = itemWorkflow.getStartedAt().toLocalDate();
    }
    if (itemWorkflow.getCompletedAt() != null) {
      this.actualCompletionDate = itemWorkflow.getCompletedAt().toLocalDate();
      this.actualDurationDays = calculateActualDurationDays();
    }
  }

  // ========== PRICING & COST METHODS ==========

  /**
   * Calculate total value for this workflow execution (quantity * unitPrice)
   */
  public BigDecimal calculateTotalValue() {
    if (unitPrice == null || quantity == null) {
      return BigDecimal.ZERO;
    }
    return unitPrice.multiply(BigDecimal.valueOf(quantity));
  }

  /**
   * Calculate effective unit price based on work type and cost components
   */
  public void calculateAndSetUnitPrice() {
    if (workType == WorkType.JOB_WORK_ONLY) {
      // For job work only, unit price is just the job work cost
      this.unitPrice = jobWorkCostPerUnit != null ? jobWorkCostPerUnit : BigDecimal.ZERO;
    } else if (workType == WorkType.WITH_MATERIAL) {
      // For with material, unit price is material cost + job work cost
      BigDecimal materialCost = materialCostPerUnit != null ? materialCostPerUnit : BigDecimal.ZERO;
      BigDecimal jobWorkCost = jobWorkCostPerUnit != null ? jobWorkCostPerUnit : BigDecimal.ZERO;
      this.unitPrice = materialCost.add(jobWorkCost);
    }
  }

  /**
   * Get breakdown of costs for display
   */
  public String getCostBreakdown() {
    if (workType == WorkType.JOB_WORK_ONLY) {
      return String.format("Job Work Only: %s per unit", 
        jobWorkCostPerUnit != null ? jobWorkCostPerUnit : "0.00");
    } else {
      return String.format("Material: %s + Job Work: %s = Total: %s per unit",
        materialCostPerUnit != null ? materialCostPerUnit : "0.00",
        jobWorkCostPerUnit != null ? jobWorkCostPerUnit : "0.00",
        unitPrice != null ? unitPrice : "0.00");
    }
  }
}
