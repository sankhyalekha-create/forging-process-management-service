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
import jakarta.validation.constraints.NotNull;

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

  // Note: Quantity, workType, pricing fields have been moved to OrderItemWorkflow
  // Each workflow execution can have different quantities and prices
  // This allows the same item to be produced multiple times with different parameters

  @OneToMany(mappedBy = "orderItem", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  @Builder.Default
  private List<OrderItemWorkflow> orderItemWorkflows = new ArrayList<>();

  @CreatedDate
  @Column(name = "created_at", updatable = false, nullable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  // Business methods

  /**
   * Calculate total value across all workflows for this order item
   */
  public BigDecimal calculateTotalValue() {
    return orderItemWorkflows.stream()
      .map(OrderItemWorkflow::calculateTotalValue)
      .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  /**
   * Get total quantity across all workflows for this order item
   */
  public Integer getTotalQuantity() {
    return orderItemWorkflows.stream()
      .mapToInt(workflow -> workflow.getQuantity() != null ? workflow.getQuantity() : 0)
      .sum();
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
}
