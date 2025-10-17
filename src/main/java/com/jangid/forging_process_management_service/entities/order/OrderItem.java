package com.jangid.forging_process_management_service.entities.order;

import com.jangid.forging_process_management_service.entities.product.Item;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Index;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "order_items", indexes = {
  @Index(name = "idx_order_item_order_id", columnList = "order_id"),
  @Index(name = "idx_order_item_item_id", columnList = "item_id")
})
@EntityListeners(AuditingEntityListener.class)
public class OrderItem {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO, generator = "order_item_sequence")
  @SequenceGenerator(name = "order_item_sequence", sequenceName = "order_item_sequence", allocationSize = 1)
  private Long id;

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "order_id", nullable = false)
  private Order order;

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "item_id", nullable = false)
  private Item item;

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

  @OneToMany(mappedBy = "orderItem", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  @Builder.Default
  private List<OrderItemWorkflow> orderItemWorkflows = new ArrayList<>();

  @CreatedDate
  @Column(name = "created_at", updatable = false, nullable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  public BigDecimal calculateTotalValue() {
    if (unitPrice == null) {
      return BigDecimal.ZERO;
    }
    return unitPrice.multiply(BigDecimal.valueOf(quantity));
  }

  public boolean hasWorkflowsInProgress() {
    return orderItemWorkflows.stream()
      .anyMatch(workflow -> workflow.getItemWorkflow().getWorkflowStatus() ==
                            ItemWorkflow.WorkflowStatus.IN_PROGRESS);
  }

  public boolean areAllWorkflowsCompleted() {
    return !orderItemWorkflows.isEmpty() && orderItemWorkflows.stream()
      .allMatch(workflow -> workflow.getItemWorkflow().getWorkflowStatus() == 
        ItemWorkflow.WorkflowStatus.COMPLETED);
  }

  public int getTotalWorkflowCount() {
    return orderItemWorkflows.size();
  }

  public int getCompletedWorkflowCount() {
    return (int) orderItemWorkflows.stream()
      .filter(workflow -> workflow.getItemWorkflow().getWorkflowStatus() == 
        ItemWorkflow.WorkflowStatus.COMPLETED)
      .count();
  }

  public String getWorkflowProgressSummary() {
    if (orderItemWorkflows.isEmpty()) {
      return "No workflows";
    }
    return String.format("%d/%d completed", getCompletedWorkflowCount(), getTotalWorkflowCount());
  }

  // Step-level progress tracking methods
  public int getTotalStepCount() {
    return orderItemWorkflows.stream()
      .mapToInt(workflow -> workflow.getItemWorkflow().getItemWorkflowSteps().size())
      .sum();
  }

  public int getCompletedStepCount() {
    return orderItemWorkflows.stream()
      .mapToInt(workflow -> (int) workflow.getItemWorkflow().getItemWorkflowSteps().stream()
        .filter(step -> step.getStepStatus() == com.jangid.forging_process_management_service.entities.workflow.ItemWorkflowStep.StepStatus.COMPLETED)
        .count())
      .sum();
  }

  public int getInProgressStepCount() {
    return orderItemWorkflows.stream()
      .mapToInt(workflow -> (int) workflow.getItemWorkflow().getItemWorkflowSteps().stream()
        .filter(step -> step.getStepStatus() == com.jangid.forging_process_management_service.entities.workflow.ItemWorkflowStep.StepStatus.IN_PROGRESS)
        .count())
      .sum();
  }

  public int getPendingStepCount() {
    return orderItemWorkflows.stream()
      .mapToInt(workflow -> (int) workflow.getItemWorkflow().getItemWorkflowSteps().stream()
        .filter(step -> step.getStepStatus() == com.jangid.forging_process_management_service.entities.workflow.ItemWorkflowStep.StepStatus.PENDING)
        .count())
      .sum();
  }

  public double getStepProgress() {
    int totalSteps = getTotalStepCount();
    if (totalSteps == 0) {
      return 0.0;
    }
    return (double) getCompletedStepCount() / totalSteps * 100;
  }

  public String getStepProgressSummary() {
    if (orderItemWorkflows.isEmpty()) {
      return "No workflow steps";
    }
    int totalSteps = getTotalStepCount();
    int completedSteps = getCompletedStepCount();
    return String.format("%d/%d steps completed", completedSteps, totalSteps);
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
