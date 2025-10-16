package com.jangid.forging_process_management_service.assemblers.order;

import com.jangid.forging_process_management_service.entities.order.Order;
import com.jangid.forging_process_management_service.entities.workflow.ItemWorkflow;
import com.jangid.forging_process_management_service.entitiesRepresentation.order.OrderRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.order.OrderItemRepresentation;
import com.jangid.forging_process_management_service.assemblers.buyer.BuyerAssembler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class OrderAssembler {

  private final OrderItemAssembler orderItemAssembler;
  private final BuyerAssembler buyerAssembler;

  @Autowired
  public OrderAssembler(OrderItemAssembler orderItemAssembler, BuyerAssembler buyerAssembler) {
    this.orderItemAssembler = orderItemAssembler;
    this.buyerAssembler = buyerAssembler;
  }

  public Order createAssemble(OrderRepresentation representation) {
    Order order = assemble(representation);
    order.setCreatedAt(LocalDateTime.now());
    return order;
  }

  public Order assemble(OrderRepresentation representation) {
    return Order.builder()
      .id(representation.getId())
      .poNumber(representation.getPoNumber())
      .orderDate(representation.getOrderDate())
      .expectedProcessingDays(representation.getExpectedProcessingDays())
      .userDefinedEtaDays(representation.getUserDefinedEtaDays())
      .orderStatus(representation.getOrderStatus() != null ? 
        Order.OrderStatus.valueOf(representation.getOrderStatus()) : Order.OrderStatus.RECEIVED)
      .notes(representation.getNotes())
      .priority(representation.getPriority() != null ? representation.getPriority() : 1)
      .actualStartDate(representation.getActualStartDate())
      .actualCompletionDate(representation.getActualCompletionDate())
      .actualDurationDays(representation.getActualDurationDays())
      .build();
  }

  public OrderRepresentation dissemble(Order order) {
    List<OrderItemRepresentation> orderItemRepresentations = order.getOrderItems().stream()
      .map(orderItemAssembler::dissemble)
      .collect(Collectors.toList());

    int totalItems = order.getOrderItems().size();
    int totalQuantity = order.getOrderItems().stream()
      .mapToInt(orderItem -> orderItem.getQuantity())
      .sum();

    long totalWorkflows = order.getOrderItems().stream()
      .flatMap(item -> item.getOrderItemWorkflows().stream())
      .count();

    long completedWorkflows = order.getOrderItems().stream()
      .flatMap(item -> item.getOrderItemWorkflows().stream())
      .filter(workflow -> workflow.getItemWorkflow().getWorkflowStatus() == 
        ItemWorkflow.WorkflowStatus.COMPLETED)
      .count();

    long inProgressWorkflows = order.getOrderItems().stream()
      .flatMap(item -> item.getOrderItemWorkflows().stream())
      .filter(workflow -> workflow.getItemWorkflow().getWorkflowStatus() ==
                          ItemWorkflow.WorkflowStatus.IN_PROGRESS)
      .count();

    double overallProgress = totalWorkflows > 0 ? (double) completedWorkflows / totalWorkflows * 100 : 0;

    Integer expectedEta = order.calculateExpectedEta();
    LocalDate expectedCompletionDate = expectedEta != null ? order.getOrderDate().plusDays(expectedEta) : null;
    boolean isOverdue = order.isOverdue();
    
    Integer daysUntilCompletion = null;
    if (expectedCompletionDate != null && !order.getOrderStatus().isFinalState()) {
      daysUntilCompletion = (int) ChronoUnit.DAYS.between(LocalDate.now(), expectedCompletionDate);
    }

    BigDecimal totalOrderValue = order.calculateTotalOrderValue();

    return OrderRepresentation.builder()
      .id(order.getId())
      .poNumber(order.getPoNumber())
      .orderDate(order.getOrderDate())
      .buyer(buyerAssembler.dissemble(order.getBuyer()))
      .expectedProcessingDays(order.getExpectedProcessingDays())
      .userDefinedEtaDays(order.getUserDefinedEtaDays())
      .calculatedEtaDays(expectedEta)
      .expectedCompletionDate(expectedCompletionDate)
      .orderStatus(order.getOrderStatus().name())
      .totalOrderValue(totalOrderValue)
      .notes(order.getNotes())
      .priority(order.getPriority())
      .orderItems(orderItemRepresentations)
      .tenantId(order.getTenant().getId())
      .createdAt(order.getCreatedAt())
      .updatedAt(order.getUpdatedAt())
      .actualStartDate(order.getActualStartDate())
      .actualCompletionDate(order.getActualCompletionDate())
      .actualDurationDays(order.getActualDurationDays())
      .canEdit(order.canEdit())
      .canDelete(order.canDelete())
      .totalItems(totalItems)
      .totalQuantity(totalQuantity)
      .workflowsInProgress((int) inProgressWorkflows)
      .workflowsCompleted((int) completedWorkflows)
      .overallProgress(overallProgress)
      .isOverdue(isOverdue)
      .daysUntilCompletion(daysUntilCompletion)
      .build();
  }
}
