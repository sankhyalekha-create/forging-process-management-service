package com.jangid.forging_process_management_service.entities.order;

import com.jangid.forging_process_management_service.entities.Tenant;
import com.jangid.forging_process_management_service.entities.buyer.Buyer;
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
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Index;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "orders", indexes = {
  @Index(name = "idx_order_po_number_tenant_id", columnList = "po_number, tenant_id", unique = true),
  @Index(name = "idx_order_tenant_id", columnList = "tenant_id"),
  @Index(name = "idx_order_status_tenant_id", columnList = "order_status, tenant_id"),
  @Index(name = "idx_order_date_tenant_id", columnList = "order_date, tenant_id"),
  @Index(name = "idx_order_buyer_id", columnList = "buyer_id")
})
@EntityListeners(AuditingEntityListener.class)
public class Order {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO, generator = "order_sequence")
  @SequenceGenerator(name = "order_sequence", sequenceName = "order_sequence", allocationSize = 1)
  private Long id;

  @NotBlank(message = "PO Number is required")
  @Size(max = 100, message = "PO Number cannot exceed 100 characters")
  @Column(name = "po_number", nullable = false, length = 100)
  private String poNumber;

  @NotNull(message = "Order date is required")
  @Column(name = "order_date", nullable = false)
  private LocalDate orderDate;

  @NotNull(message = "Buyer is required")
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "buyer_id", nullable = false)
  private Buyer buyer;


  @Column(name = "expected_processing_days")
  private Integer expectedProcessingDays;

  @Column(name = "user_defined_eta_days")
  private Integer userDefinedEtaDays;

  @Enumerated(EnumType.STRING)
  @Column(name = "order_status", nullable = false)
  @Builder.Default
  private OrderStatus orderStatus = OrderStatus.RECEIVED;

  @Column(name = "notes", length = 2000)
  private String notes;

  @Column(name = "priority", nullable = false)
  @Builder.Default
  private Integer priority = 1; // 1 = highest priority, higher numbers = lower priority

  @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  @Builder.Default
  private List<OrderItem> orderItems = new ArrayList<>();

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

  // Actual completion tracking fields
  @Column(name = "actual_start_date")
  private LocalDate actualStartDate;

  @Column(name = "actual_completion_date")
  private LocalDate actualCompletionDate;

  @Column(name = "actual_duration_days")
  private Integer actualDurationDays;

  // Inventory shortage tracking
  @Column(name = "has_inventory_shortage")
  @Builder.Default
  private Boolean hasInventoryShortage = false;

  // Business methods
  public void addOrderItem(OrderItem orderItem) {
    orderItems.add(orderItem);
    orderItem.setOrder(this);
  }

  public void removeOrderItem(OrderItem orderItem) {
    orderItems.remove(orderItem);
    orderItem.setOrder(null);
  }

  public boolean canEdit() {
    return orderStatus.canEdit();
  }

  public boolean canDelete() {
    return orderStatus.canDelete() && orderItems.stream()
      .flatMap(item -> item.getOrderItemWorkflows().stream())
      .noneMatch(workflow -> workflow.getItemWorkflow().getWorkflowStatus() == 
        com.jangid.forging_process_management_service.entities.workflow.ItemWorkflow.WorkflowStatus.IN_PROGRESS);
  }

  public boolean canTransitionTo(OrderStatus newStatus) {
    return orderStatus.canTransitionTo(newStatus);
  }

  public void updateStatus(OrderStatus newStatus) {
    if (!canTransitionTo(newStatus)) {
      throw new IllegalStateException(
        String.format("Cannot transition from %s to %s", orderStatus, newStatus));
    }
    this.orderStatus = newStatus;
  }

  // Calculate total order value from order items
  public BigDecimal calculateTotalOrderValue() {
    return orderItems.stream()
      .map(OrderItem::calculateTotalValue)
      .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  // Calculate expected ETA based on item workflows or user-defined value
  public Integer calculateExpectedEta() {
    if (userDefinedEtaDays != null) {
      return userDefinedEtaDays;
    }
    
    if (expectedProcessingDays != null) {
      return expectedProcessingDays;
    }
    
    // Calculate from item workflows if available
    return orderItems.stream()
      .flatMap(item -> item.getOrderItemWorkflows().stream())
      .mapToInt(workflow -> workflow.getPlannedDurationDays() != null ? workflow.getPlannedDurationDays() : 0)
      .max()
      .orElse(0);
  }

  // Update actual dates when order status changes
  public void updateActualDates() {
    if (orderStatus == OrderStatus.IN_PROGRESS && actualStartDate == null) {
      actualStartDate = LocalDate.now();
    }
    
    if (orderStatus == OrderStatus.COMPLETED && actualCompletionDate == null) {
      actualCompletionDate = LocalDate.now();
      if (actualStartDate != null) {
        actualDurationDays = (int) java.time.temporal.ChronoUnit.DAYS.between(actualStartDate, actualCompletionDate) + 1;
      }
    }
  }

  // Check if order is overdue based on expected ETA
  public boolean isOverdue() {
    Integer expectedEta = calculateExpectedEta();
    if (expectedEta == null || orderStatus.isFinalState()) {
      return false;
    }
    
    LocalDate expectedCompletionDate = orderDate.plusDays(expectedEta);
    // Consider order overdue if today is on or after the expected completion date
    return !LocalDate.now().isBefore(expectedCompletionDate);
  }

  // Check if all order item workflows are completed
  public boolean areAllWorkflowsCompleted() {
    return orderItems.stream()
      .flatMap(item -> item.getOrderItemWorkflows().stream())
      .allMatch(workflow -> workflow.getItemWorkflow().getWorkflowStatus() == 
        ItemWorkflow.WorkflowStatus.COMPLETED);
  }

  // Check if any workflow has started
  public boolean hasAnyWorkflowStarted() {
    return orderItems.stream()
      .flatMap(item -> item.getOrderItemWorkflows().stream())
      .anyMatch(workflow -> workflow.getItemWorkflow().getWorkflowStatus() !=
                            ItemWorkflow.WorkflowStatus.NOT_STARTED);
  }

  public enum OrderStatus {
    RECEIVED(true, true),           // Order just received - can edit and delete
    IN_PROGRESS(false, false),      // At least one ItemWorkflow started - read-only
    COMPLETED(false, false),        // All ItemWorkflows completed - read-only
    CANCELLED(false, false);        // Order cancelled - read-only

    private final boolean canEdit;
    private final boolean canDelete;

    OrderStatus(boolean canEdit, boolean canDelete) {
      this.canEdit = canEdit;
      this.canDelete = canDelete;
    }

    public boolean canEdit() {
      return canEdit;
    }

    public boolean canDelete() {
      return canDelete;
    }

    public boolean canTransitionTo(OrderStatus newStatus) {
      return switch (this) {
        case RECEIVED -> newStatus == IN_PROGRESS || newStatus == CANCELLED;
        case IN_PROGRESS -> newStatus == COMPLETED || newStatus == CANCELLED;
        case COMPLETED, CANCELLED -> false; // Final states
      };
    }

    public boolean isFinalState() {
      return this == COMPLETED || this == CANCELLED;
    }
  }
}
