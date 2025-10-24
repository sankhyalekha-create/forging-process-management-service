package com.jangid.forging_process_management_service.assemblers.order;

import com.jangid.forging_process_management_service.entities.order.OrderItemWorkflow;
import com.jangid.forging_process_management_service.entitiesRepresentation.order.OrderItemWorkflowRepresentation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class OrderItemWorkflowAssembler {

  public OrderItemWorkflow createAssemble(OrderItemWorkflowRepresentation representation) {
    OrderItemWorkflow orderItemWorkflow = assemble(representation);
    orderItemWorkflow.setCreatedAt(LocalDateTime.now());
    return orderItemWorkflow;
  }

  public OrderItemWorkflow assemble(OrderItemWorkflowRepresentation representation) {
    return OrderItemWorkflow.builder()
      .id(representation.getId())
      .plannedDurationDays(representation.getPlannedDurationDays())
      .actualStartDate(representation.getActualStartDate())
      .actualCompletionDate(representation.getActualCompletionDate())
      .actualDurationDays(representation.getActualDurationDays())
      .notes(representation.getNotes())
      .priority(representation.getPriority())
      .build();
  }

  public OrderItemWorkflowRepresentation dissemble(OrderItemWorkflow orderItemWorkflow) {
    // Update actual dates from ItemWorkflow
    orderItemWorkflow.updateActualDates();

    return OrderItemWorkflowRepresentation.builder()
      .id(orderItemWorkflow.getId())
      .orderItemId(orderItemWorkflow.getOrderItem().getId())
      // Include pricing fields directly to avoid circular reference (convert BigDecimal to Double)
      .unitPrice(orderItemWorkflow.getOrderItem().getUnitPrice() != null ? 
        orderItemWorkflow.getOrderItem().getUnitPrice().doubleValue() : null)
      .materialCostPerUnit(orderItemWorkflow.getOrderItem().getMaterialCostPerUnit() != null ? 
        orderItemWorkflow.getOrderItem().getMaterialCostPerUnit().doubleValue() : null)
      .jobWorkCostPerUnit(orderItemWorkflow.getOrderItem().getJobWorkCostPerUnit() != null ? 
        orderItemWorkflow.getOrderItem().getJobWorkCostPerUnit().doubleValue() : null)
      .workType(orderItemWorkflow.getOrderItem().getWorkType().name())
      .itemWorkflowId(orderItemWorkflow.getItemWorkflow().getId())
      .workflowIdentifier(orderItemWorkflow.getItemWorkflow().getWorkflowIdentifier())
      .workflowTemplateName(orderItemWorkflow.getItemWorkflow().getWorkflowTemplate().getWorkflowName())
      .workflowStatus(orderItemWorkflow.getItemWorkflow().getWorkflowStatus().name())
      .plannedDurationDays(orderItemWorkflow.getPlannedDurationDays())
      .actualStartDate(orderItemWorkflow.getActualStartDate())
      .actualCompletionDate(orderItemWorkflow.getActualCompletionDate())
      .actualDurationDays(orderItemWorkflow.getActualDurationDays())
      .notes(orderItemWorkflow.getNotes())
      .priority(orderItemWorkflow.getPriority())
      .createdAt(orderItemWorkflow.getCreatedAt())
      .updatedAt(orderItemWorkflow.getUpdatedAt())
      .isStarted(orderItemWorkflow.isStarted())
      .isCompleted(orderItemWorkflow.isCompleted())
      .isInProgress(orderItemWorkflow.isInProgress())
      .isDelayed(orderItemWorkflow.isDelayed())
      .remainingDays(orderItemWorkflow.calculateRemainingDays())
      .statusSummary(orderItemWorkflow.getStatusSummary())
      .durationVariance(orderItemWorkflow.getActualDurationDays() != null && orderItemWorkflow.getPlannedDurationDays() != null ?
        orderItemWorkflow.getActualDurationDays() - orderItemWorkflow.getPlannedDurationDays() : null)
      .completionPercentage(calculateCompletionPercentage(orderItemWorkflow))
      .build();
  }

  private Double calculateCompletionPercentage(OrderItemWorkflow orderItemWorkflow) {
    if (orderItemWorkflow.isCompleted()) {
      return 100.0;
    }
    
    if (!orderItemWorkflow.isStarted() || orderItemWorkflow.getPlannedDurationDays() == null) {
      return 0.0;
    }
    
    if (orderItemWorkflow.getActualStartDate() != null) {
      int elapsedDays = (int) java.time.temporal.ChronoUnit.DAYS.between(
        orderItemWorkflow.getActualStartDate(), 
        java.time.LocalDate.now()
      ) + 1;
      
      double percentage = (double) elapsedDays / orderItemWorkflow.getPlannedDurationDays() * 100;
      return Math.min(percentage, 100.0);
    }
    
    return 0.0;
  }
}
