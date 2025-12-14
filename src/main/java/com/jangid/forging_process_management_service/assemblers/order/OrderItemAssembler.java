package com.jangid.forging_process_management_service.assemblers.order;

import com.jangid.forging_process_management_service.entities.order.OrderItem;
import com.jangid.forging_process_management_service.entities.order.WorkType;
import com.jangid.forging_process_management_service.entitiesRepresentation.order.OrderItemRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.order.OrderItemWorkflowRepresentation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class OrderItemAssembler {

  private final OrderItemWorkflowAssembler orderItemWorkflowAssembler;

  @Autowired
  public OrderItemAssembler(OrderItemWorkflowAssembler orderItemWorkflowAssembler) {
    this.orderItemWorkflowAssembler = orderItemWorkflowAssembler;
  }

  public OrderItem createAssemble(OrderItemRepresentation representation) {
    OrderItem orderItem = assemble(representation);
    orderItem.setCreatedAt(LocalDateTime.now());
    return orderItem;
  }

  public OrderItem assemble(OrderItemRepresentation representation) {
    // Note: OrderItem no longer has quantity/pricing fields
    // These are now at the OrderItemWorkflow level
    // This method just creates the OrderItem shell
    
    OrderItem orderItem = OrderItem.builder()
      .id(representation.getId())
      .build();
    
    return orderItem;
  }

  public OrderItemRepresentation dissemble(OrderItem orderItem) {
    List<OrderItemWorkflowRepresentation> workflowRepresentations = orderItem.getOrderItemWorkflows().stream()
      .map(orderItemWorkflowAssembler::dissemble)
      .collect(Collectors.toList());

    // Workflow-level progress
    int workflowCount = orderItem.getOrderItemWorkflows().size();
    int completedWorkflowCount = orderItem.getCompletedWorkflowCount();
    double workflowProgress = workflowCount > 0 ? (double) completedWorkflowCount / workflowCount * 100 : 0;

    // Step-level progress (more granular)
    int totalStepCount = orderItem.getTotalStepCount();
    int completedStepCount = orderItem.getCompletedStepCount();
    int inProgressStepCount = orderItem.getInProgressStepCount();
    int pendingStepCount = orderItem.getPendingStepCount();
    double stepProgress = orderItem.getStepProgress();
    String stepProgressSummary = orderItem.getStepProgressSummary();

    // Get totals from workflows
    Integer totalQuantity = orderItem.getTotalQuantity();
    
    return OrderItemRepresentation.builder()
      .id(orderItem.getId())
      .orderId(orderItem.getOrder().getId())
      .itemId(orderItem.getItem().getId())
      .itemName(orderItem.getItem().getItemName())
      .itemCode(orderItem.getItem().getItemCode())
      // Computed totals (new way)
      .totalQuantity(totalQuantity)
      .totalValue(orderItem.calculateTotalValue())
      // Workflows (actual data)
      .orderItemWorkflows(workflowRepresentations)
      .createdAt(orderItem.getCreatedAt())
      .updatedAt(orderItem.getUpdatedAt())
      // Workflow-level progress
      .workflowCount(workflowCount)
      .completedWorkflowCount(completedWorkflowCount)
      .workflowProgress(workflowProgress)
      .hasWorkflowsInProgress(orderItem.hasWorkflowsInProgress())
      .allWorkflowsCompleted(orderItem.areAllWorkflowsCompleted())
      .workflowProgressSummary(orderItem.getWorkflowProgressSummary())
      // Step-level progress (granular insights)
      .totalStepCount(totalStepCount)
      .completedStepCount(completedStepCount)
      .inProgressStepCount(inProgressStepCount)
      .pendingStepCount(pendingStepCount)
      .stepProgress(stepProgress)
      .stepProgressSummary(stepProgressSummary)
      .build();
  }
}
