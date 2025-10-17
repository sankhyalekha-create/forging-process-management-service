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
    // Parse work type using the public enum
    WorkType workType = WorkType.WITH_MATERIAL; // Default
    if (representation.getWorkType() != null) {
      try {
        workType = WorkType.valueOf(representation.getWorkType());
      } catch (IllegalArgumentException e) {
        log.warn("Invalid work type '{}', using default WITH_MATERIAL", representation.getWorkType());
      }
    }
    
    OrderItem orderItem = OrderItem.builder()
      .id(representation.getId())
      .quantity(representation.getQuantity())
      .workType(workType)
      .unitPrice(representation.getUnitPrice())
      .materialCostPerUnit(representation.getMaterialCostPerUnit())
      .jobWorkCostPerUnit(representation.getJobWorkCostPerUnit())
      .specialInstructions(representation.getSpecialInstructions())
      .build();
    
    // Calculate unit price if not provided but cost components are available
    if (orderItem.getUnitPrice() == null && 
        (orderItem.getMaterialCostPerUnit() != null || orderItem.getJobWorkCostPerUnit() != null)) {
      orderItem.calculateAndSetUnitPrice();
    }
    
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

    return OrderItemRepresentation.builder()
      .id(orderItem.getId())
      .orderId(orderItem.getOrder().getId())
      .itemId(orderItem.getItem().getId())
      .itemName(orderItem.getItem().getItemName())
      .itemCode(orderItem.getItem().getItemCode())
      .quantity(orderItem.getQuantity())
      .workType(orderItem.getWorkType().name())
      .unitPrice(orderItem.getUnitPrice())
      .materialCostPerUnit(orderItem.getMaterialCostPerUnit())
      .jobWorkCostPerUnit(orderItem.getJobWorkCostPerUnit())
      .costBreakdown(orderItem.getCostBreakdown())
      .totalValue(orderItem.calculateTotalValue())
      .specialInstructions(orderItem.getSpecialInstructions())
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
