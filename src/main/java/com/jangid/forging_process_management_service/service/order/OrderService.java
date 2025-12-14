package com.jangid.forging_process_management_service.service.order;

import com.jangid.forging_process_management_service.entities.Tenant;
import com.jangid.forging_process_management_service.entities.buyer.Buyer;
import com.jangid.forging_process_management_service.entities.order.Order;
import com.jangid.forging_process_management_service.entities.order.OrderItem;
import com.jangid.forging_process_management_service.entities.order.OrderItemWorkflow;
import com.jangid.forging_process_management_service.entities.product.Item;
import com.jangid.forging_process_management_service.entities.workflow.ItemWorkflow;
import com.jangid.forging_process_management_service.entitiesRepresentation.order.CreateOrderRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.order.OrderRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.order.OrderItemRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.order.OrderItemWorkflowRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.order.OrderStatisticsRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.order.UpdateOrderRequest;
import com.jangid.forging_process_management_service.exception.ResourceNotFoundException;
import com.jangid.forging_process_management_service.repositories.order.OrderRepository;
import com.jangid.forging_process_management_service.repositories.order.OrderItemRepository;
import com.jangid.forging_process_management_service.repositories.order.OrderItemWorkflowRepository;
import com.jangid.forging_process_management_service.repositories.workflow.ItemWorkflowRepository;
import com.jangid.forging_process_management_service.service.TenantService;
import com.jangid.forging_process_management_service.service.product.ItemService;
import com.jangid.forging_process_management_service.service.workflow.ItemWorkflowService;
import com.jangid.forging_process_management_service.service.buyer.BuyerService;
import com.jangid.forging_process_management_service.assemblers.order.OrderAssembler;
import com.jangid.forging_process_management_service.assemblers.order.OrderItemAssembler;
import com.jangid.forging_process_management_service.assemblers.order.OrderItemWorkflowAssembler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OrderService {

  @Autowired
  private OrderRepository orderRepository;

  @Autowired
  private OrderItemRepository orderItemRepository;

  @Autowired
  private OrderItemWorkflowRepository orderItemWorkflowRepository;

  @Autowired
  private ItemWorkflowRepository itemWorkflowRepository;

  @Autowired
  private TenantService tenantService;

  @Autowired
  private ItemService itemService;

  @Autowired
  private ItemWorkflowService itemWorkflowService;

  @Autowired
  private BuyerService buyerService;

  @Autowired
  private OrderAssembler orderAssembler;

  @Autowired
  private OrderItemAssembler orderItemAssembler;

  @Autowired
  private OrderItemWorkflowAssembler orderItemWorkflowAssembler;

  @Autowired
  private InventoryAvailabilityService inventoryAvailabilityService;

  /**
   * Create a new order with items and workflows
   */
  @Transactional
  public OrderRepresentation createOrder(Long tenantId, CreateOrderRepresentation request) {
    log.info("Creating order for tenant: {}, PO: {}", tenantId, request.getPoNumber());

    // Validate tenant and buyer
    Tenant tenant = tenantService.getTenantById(tenantId);
    
    // Get buyer if provided
    Buyer buyer = null;
    if (request.getBuyerId() != null) {
      buyer = buyerService.getBuyerByIdAndTenantId(request.getBuyerId(), tenantId);
    }

    // Check if PO number already exists
    if (orderRepository.findByPoNumberAndTenantIdAndDeletedFalse(request.getPoNumber(), tenantId).isPresent()) {
      throw new IllegalArgumentException("Order with PO Number " + request.getPoNumber() + " already exists");
    }

    // Convert CreateOrderRepresentation to OrderRepresentation for assembler
    OrderRepresentation orderRepresentation = convertCreateRequestToRepresentation(request, tenant, buyer);
    
    // Use assembler to create Order entity
    Order order = orderAssembler.createAssemble(orderRepresentation);
    order.setTenant(tenant);
    order.setBuyer(buyer);

    // Save order first to get ID
    order = orderRepository.save(order);

    // Check inventory availability for order items (only for WITH_MATERIAL work type)
    try {
      Map<String, Object> inventoryCheck = inventoryAvailabilityService
        .checkInventoryForOrderItems(tenantId, request.getOrderItems());
      
      Boolean hasShortage = (Boolean) inventoryCheck.get("hasShortage");
      if (hasShortage != null && hasShortage) {
        order.setHasInventoryShortage(true);
        log.warn("Order {} has inventory shortage", order.getPoNumber());
      } else {
        order.setHasInventoryShortage(false);
      }
    } catch (Exception e) {
      log.error("Error checking inventory availability for order {}: {}", order.getPoNumber(), e.getMessage());
      // Don't fail order creation if inventory check fails - set to false
      order.setHasInventoryShortage(false);
    }

    // Create order items using assemblers
    for (OrderItemRepresentation itemRequest : request.getOrderItems()) {
      createOrderItemWithAssembler(order, itemRequest);
    }

    log.info("Successfully created order with ID: {} for tenant: {}", order.getId(), tenantId);
    return orderAssembler.dissemble(order);
  }

  /**
   * Get order by ID
   */
  @Transactional(readOnly = true)
  public OrderRepresentation getOrderById(Long tenantId, Long orderId) {
    Order order = orderRepository.findByIdAndTenantIdAndDeletedFalse(orderId, tenantId)
      .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));
    
    return orderAssembler.dissemble(order);
  }

  /**
   * Get all orders for tenant with pagination
   */
  @Transactional(readOnly = true)
  public Page<OrderRepresentation> getOrders(Long tenantId, int page, int size) {
    Pageable pageable = PageRequest.of(page, size);
    Page<Order> orders = orderRepository.findByTenantIdAndDeletedFalse(tenantId, pageable);
    
    return orders.map(orderAssembler::dissemble);
  }

  /**
   * Search orders with filters
   */
  @Transactional(readOnly = true)
  public Page<OrderRepresentation> searchOrders(Long tenantId, String poNumber, String customerName, 
                                               String status, Integer priority, LocalDate startDate, 
                                               LocalDate endDate, int page, int size) {
    Pageable pageable = PageRequest.of(page, size);
    Order.OrderStatus orderStatus = status != null ? Order.OrderStatus.valueOf(status) : null;
    
    Page<Order> orders = orderRepository.searchOrders(tenantId, poNumber, customerName, 
                                                     orderStatus, priority, startDate, endDate, pageable);
    
    return orders.map(orderAssembler::dissemble);
  }

  /**
   * Update order status
   */
  @Transactional
  public OrderRepresentation updateOrderStatus(Long tenantId, Long orderId, Order.OrderStatus newStatus, String notes) {
    Order order = orderRepository.findByIdAndTenantIdAndDeletedFalse(orderId, tenantId)
      .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

    // Validate status transition
    if (!order.canTransitionTo(newStatus)) {
      throw new IllegalStateException(
        String.format("Cannot transition order from %s to %s", order.getOrderStatus(), newStatus));
    }

    order.updateStatus(newStatus);
    if (notes != null) {
      order.setNotes(order.getNotes() != null ? order.getNotes() + "\n" + notes : notes);
    }

    order = orderRepository.save(order);
    log.info("Updated order {} status to {} for tenant: {}", orderId, newStatus, tenantId);
    
    return orderAssembler.dissemble(order);
  }

  /**
   * Update order priority
   */
  @Transactional
  public OrderRepresentation updateOrderPriority(Long tenantId, Long orderId, Integer newPriority, String notes) {
    Order order = orderRepository.findByIdAndTenantIdAndDeletedFalse(orderId, tenantId)
      .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

    if (!order.canEdit()) {
      throw new IllegalStateException("Cannot update priority of order in " + order.getOrderStatus() + " status");
    }

    order.setPriority(newPriority);
    if (notes != null) {
      order.setNotes(order.getNotes() != null ? order.getNotes() + "\n" + notes : notes);
    }

    order = orderRepository.save(order);
    log.info("Updated order {} priority to {} for tenant: {}", orderId, newPriority, tenantId);
    
    return orderAssembler.dissemble(order);
  }

  /**
   * Update order priority and timeline fields
   */
  @Transactional
  public OrderRepresentation updateOrderPriority(Long tenantId, Long orderId, Integer newPriority, String notes, 
                                                 Integer expectedProcessingDays, Integer userDefinedEtaDays) {
    Order order = orderRepository.findByIdAndTenantIdAndDeletedFalse(orderId, tenantId)
      .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

    if (!order.canEdit()) {
      throw new IllegalStateException("Cannot update priority of order in " + order.getOrderStatus() + " status");
    }

    order.setPriority(newPriority);
    if (notes != null) {
      order.setNotes(order.getNotes() != null ? order.getNotes() + "\n" + notes : notes);
    }

    // Update timeline fields if provided
    if (expectedProcessingDays != null) {
      order.setExpectedProcessingDays(expectedProcessingDays);
    }

    if (userDefinedEtaDays != null) {
      order.setUserDefinedEtaDays(userDefinedEtaDays);
    }

    order = orderRepository.save(order);
    log.info("Updated order {} priority to {}, expectedProcessingDays: {}, userDefinedEtaDays: {} for tenant: {}", 
             orderId, newPriority, expectedProcessingDays, userDefinedEtaDays, tenantId);
    
    return orderAssembler.dissemble(order);
  }

  /**
   * Update order details (only allowed when status is RECEIVED)
   */
  @Transactional
  public OrderRepresentation updateOrder(Long tenantId, Long orderId, UpdateOrderRequest request) {
    Order order = orderRepository.findByIdAndTenantIdAndDeletedFalse(orderId, tenantId)
      .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

    // Only allow updates when order status is RECEIVED
    if (order.getOrderStatus() != Order.OrderStatus.RECEIVED) {
      throw new IllegalStateException("Order can only be edited when status is RECEIVED. Current status: " + order.getOrderStatus());
    }

    // Validate and get buyer
    Buyer buyer = buyerService.getBuyerByIdAndTenantId(request.getBuyerId(), tenantId);

    // Update order fields
    order.setPoNumber(request.getPoNumber());
    order.setOrderDate(request.getOrderDate());
    order.setBuyer(buyer);
    order.setExpectedProcessingDays(request.getExpectedProcessingDays());
    order.setUserDefinedEtaDays(request.getUserDefinedEtaDays());
    order.setPriority(request.getPriority());
    order.setNotes(request.getNotes());
    order.setUpdatedAt(LocalDateTime.now());

    // Update order items if provided
    if (request.getOrderItems() != null && !request.getOrderItems().isEmpty()) {
      // Check if we should preserve items with workflows (flag from frontend)
      Boolean preserveItemsWithWorkflows = request.getPreserveItemsWithWorkflows();
      
      // If preserveItemsWithWorkflows is false (or null), clear all existing items
      if (preserveItemsWithWorkflows == null || !preserveItemsWithWorkflows) {
        log.info("Replace mode: Clearing all order items for order {}", orderId);
        order.getOrderItems().clear();
      } else {
        log.info("Preserve mode: Keeping existing items with workflows for order {}", orderId);
      }
      
      // Add/update order items from request
      for (OrderItemRepresentation itemRep : request.getOrderItems()) {
        try {
          // Check if this is adding a workflow to an existing OrderItem
          if (itemRep.getId() != null && preserveItemsWithWorkflows != null && preserveItemsWithWorkflows) {
            // Find existing OrderItem and add workflow to it
            addWorkflowToExistingOrderItem(order, itemRep);
          } else {
            // Create new OrderItem
            createOrderItemWithAssembler(order, itemRep);
          }
        } catch (Exception e) {
          log.error("Error processing order item during update: {}", e.getMessage());
          throw new RuntimeException("Failed to update order item: " + e.getMessage());
        }
      }
    }

    order = orderRepository.save(order);
    log.info("Updated order {} for tenant: {}", orderId, tenantId);
    
    return orderAssembler.dissemble(order);
  }

  /**
   * Delete order (soft delete)
   */
  @Transactional
  public void deleteOrder(Long tenantId, Long orderId) {
    Order order = orderRepository.findByIdAndTenantIdAndDeletedFalse(orderId, tenantId)
      .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

    if (!order.canDelete()) {
      throw new IllegalStateException("Cannot delete order with workflows in progress");
    }

    order.setDeleted(true);
    order.setDeletedAt(LocalDateTime.now());
    orderRepository.save(order);
    
    log.info("Deleted order {} for tenant: {}", orderId, tenantId);
  }

  /**
   * Get orders by status
   */
  @Transactional(readOnly = true)
  public List<OrderRepresentation> getOrdersByStatus(Long tenantId, Order.OrderStatus status) {
    List<Order> orders = orderRepository.findByTenantIdAndOrderStatusAndDeletedFalse(tenantId, status);
    return orders.stream().map(orderAssembler::dissemble).collect(Collectors.toList());
  }

  /**
   * Get overdue orders
   */
  @Transactional(readOnly = true)
  public List<OrderRepresentation> getOverdueOrders(Long tenantId) {
    List<Order> orders = orderRepository.findOverdueOrders(tenantId);
    
    // Filter overdue orders in service layer using business logic
    List<Order> overdueOrders = orders.stream()
      .filter(Order::isOverdue) // Use the business method from Order entity
      .toList();
    
    return overdueOrders.stream().map(orderAssembler::dissemble).collect(Collectors.toList());
  }

  /**
   * Create order item with optional workflow using assemblers
   */
  private void createOrderItemWithAssembler(Order order, OrderItemRepresentation itemRequest) {
    Item item;
    
    // Get or create item
    if (itemRequest.getItemId() != null) {
      item = itemService.getItemById(itemRequest.getItemId());
    } else if (itemRequest.getNewItem() != null) {
      // Create new item - this would need to be implemented in ItemService
      throw new UnsupportedOperationException("Creating new items during order creation not yet implemented");
    } else {
      throw new IllegalArgumentException("Either itemId or newItem must be provided");
    }

    // Convert OrderItemRepresentation to OrderItemRepresentation for assembler
    OrderItemRepresentation orderItemRepresentation = convertCreateItemRequestToRepresentation(itemRequest, order, item);
    
    // Use assembler to create OrderItem entity
    OrderItem orderItem = orderItemAssembler.createAssemble(orderItemRepresentation);
    orderItem.setOrder(order);
    orderItem.setItem(item);

    orderItem = orderItemRepository.save(orderItem);

    // Create OrderItemWorkflows from the request
    if (itemRequest.getOrderItemWorkflows() != null && !itemRequest.getOrderItemWorkflows().isEmpty()) {
      for (OrderItemWorkflowRepresentation workflowRep : itemRequest.getOrderItemWorkflows()) {
        createOrderItemWorkflow(orderItem, workflowRep);
      }
    }
  }

  /**
   * Create an OrderItemWorkflow from a workflow representation
   * Optionally checks inventory before creating the workflow
   */
  private void createOrderItemWorkflow(OrderItem orderItem, OrderItemWorkflowRepresentation workflowRep) {
    // Fetch the ItemWorkflow
    ItemWorkflow itemWorkflow = itemWorkflowRepository.findById(workflowRep.getItemWorkflowId())
      .orElseThrow(() -> new ResourceNotFoundException(
        "ItemWorkflow not found with id: " + workflowRep.getItemWorkflowId()
      ));
    
    // Validate that the ItemWorkflow belongs to the same Item
    if (!itemWorkflow.getItem().getId().equals(orderItem.getItem().getId())) {
      throw new IllegalArgumentException(
        "ItemWorkflow " + itemWorkflow.getId() + " does not belong to Item " + orderItem.getItem().getId()
      );
    }

    // Check inventory availability for this workflow (optional, non-blocking)
    try {
      Map<String, Object> inventoryCheck = inventoryAvailabilityService.checkInventoryForWorkflow(
        orderItem.getOrder().getTenant().getId(),
        orderItem.getItem().getId(),
        workflowRep
      );
      
      Boolean hasShortage = (Boolean) inventoryCheck.get("hasShortage");
      if (hasShortage != null && hasShortage) {
        log.warn("Workflow has inventory shortage - OrderItem: {}, ItemWorkflow: {}, Quantity: {}", 
          orderItem.getId(), itemWorkflow.getId(), workflowRep.getQuantity());
        // Note: We don't block workflow creation, just log the warning
        // The order-level hasInventoryShortage flag will be set during order creation
      }
    } catch (Exception e) {
      log.error("Error checking inventory for workflow: {}", e.getMessage());
      // Don't fail workflow creation if inventory check fails
    }

    // Enrich workflow representation with ItemWorkflow details
    OrderItemWorkflowRepresentation enrichedWorkflowRep = OrderItemWorkflowRepresentation.builder()
      .orderItemId(orderItem.getId())
      .itemWorkflowId(itemWorkflow.getId())
      .workflowIdentifier(itemWorkflow.getWorkflowIdentifier())
      .workflowTemplateName(itemWorkflow.getWorkflowTemplate().getWorkflowName())
      .workflowStatus(itemWorkflow.getWorkflowStatus().name())
      // Quantity and pricing from request
      .quantity(workflowRep.getQuantity())
      .workType(workflowRep.getWorkType())
      .unitPrice(workflowRep.getUnitPrice())
      .materialCostPerUnit(workflowRep.getMaterialCostPerUnit())
      .jobWorkCostPerUnit(workflowRep.getJobWorkCostPerUnit())
      .specialInstructions(workflowRep.getSpecialInstructions())
      // Workflow scheduling
      .plannedDurationDays(workflowRep.getPlannedDurationDays())
      .priority(workflowRep.getPriority() != null ? workflowRep.getPriority() : 1)
      .build();
    
    // Use assembler to create OrderItemWorkflow entity
    OrderItemWorkflow orderItemWorkflow = orderItemWorkflowAssembler.createAssemble(enrichedWorkflowRep);
    orderItemWorkflow.setOrderItem(orderItem);
    orderItemWorkflow.setItemWorkflow(itemWorkflow);

    orderItemWorkflowRepository.save(orderItemWorkflow);
    
    log.info("Created OrderItemWorkflow for OrderItem {} with ItemWorkflow {} ({})", 
      orderItem.getId(), itemWorkflow.getId(), itemWorkflow.getWorkflowIdentifier());
  }

  /**
   * Add a workflow to an existing OrderItem (for incremental workflow assignment)
   */
  private void addWorkflowToExistingOrderItem(Order order, OrderItemRepresentation itemRep) {
    // Find the existing OrderItem in the order
    OrderItem existingOrderItem = order.getOrderItems().stream()
      .filter(oi -> oi.getId().equals(itemRep.getId()))
      .findFirst()
      .orElseThrow(() -> new ResourceNotFoundException(
        "OrderItem not found with id: " + itemRep.getId() + " in order: " + order.getId()
      ));
    
    // Validate that item IDs match
    if (!existingOrderItem.getItem().getId().equals(itemRep.getItemId())) {
      throw new IllegalArgumentException(
        "Item mismatch: OrderItem " + existingOrderItem.getId() + 
        " belongs to Item " + existingOrderItem.getItem().getId() + 
        " but request specifies Item " + itemRep.getItemId()
      );
    }
    
    // Add workflows from the request to this existing OrderItem
    if (itemRep.getOrderItemWorkflows() != null && !itemRep.getOrderItemWorkflows().isEmpty()) {
      for (OrderItemWorkflowRepresentation workflowRep : itemRep.getOrderItemWorkflows()) {
        createOrderItemWorkflow(existingOrderItem, workflowRep);
        log.info("Added workflow {} to existing OrderItem {}", 
          workflowRep.getItemWorkflowId(), existingOrderItem.getId());
      }
    } else {
      log.warn("No workflows provided for OrderItem {}, skipping workflow addition", 
        existingOrderItem.getId());
    }
  }

  /**
   * Convert CreateOrderRepresentation to OrderRepresentation for assembler
   */
  private OrderRepresentation convertCreateRequestToRepresentation(CreateOrderRepresentation request, Tenant tenant, Buyer buyer) {
    return OrderRepresentation.builder()
      .poNumber(request.getPoNumber())
      .orderDate(request.getOrderDate())
      .expectedProcessingDays(request.getExpectedProcessingDays())
      .userDefinedEtaDays(request.getUserDefinedEtaDays())
      .notes(request.getNotes())
      .priority(request.getPriority() != null ? request.getPriority() : 1)
      .orderStatus(Order.OrderStatus.RECEIVED.name())
      .tenantId(tenant.getId())
      .build();
  }

  /**
   * Convert OrderItemRepresentation to OrderItemRepresentation for assembler
   * Note: With the new structure, quantity/pricing are at workflow level
   */
  private OrderItemRepresentation convertCreateItemRequestToRepresentation(OrderItemRepresentation itemRequest, Order order, Item item) {
    // Use the workflows from the request directly if provided
    List<OrderItemWorkflowRepresentation> workflowRepresentations = itemRequest.getOrderItemWorkflows();
    
    return OrderItemRepresentation.builder()
      .orderId(order.getId())
      .itemId(item.getId())
      .itemName(item.getItemName())
      .itemCode(item.getItemCode())
      .orderItemWorkflows(workflowRepresentations)
      .build();
  }

  /**
   * Get order statistics for the specified date range
   */
  @Transactional(readOnly = true)
  public OrderStatisticsRepresentation getOrderStatistics(Long tenantId, LocalDate startDate, LocalDate endDate) {
    // If no date range provided, get all orders
    List<Order> orders;
    if (startDate != null && endDate != null) {
      orders = orderRepository.findByTenantIdAndOrderDateBetweenAndDeletedFalse(tenantId, startDate, endDate);
    } else {
      orders = orderRepository.findByTenantIdAndDeletedFalse(tenantId);
    }

    // Calculate statistics
    long total = orders.size();
    long received = orders.stream().mapToLong(o -> o.getOrderStatus() == Order.OrderStatus.RECEIVED ? 1 : 0).sum();
    long inProgress = orders.stream().mapToLong(o -> o.getOrderStatus() == Order.OrderStatus.IN_PROGRESS ? 1 : 0).sum();
    long completed = orders.stream().mapToLong(o -> o.getOrderStatus() == Order.OrderStatus.COMPLETED ? 1 : 0).sum();
    long cancelled = orders.stream().mapToLong(o -> o.getOrderStatus() == Order.OrderStatus.CANCELLED ? 1 : 0).sum();
    long overdue = orders.stream().mapToLong(o -> o.isOverdue() ? 1 : 0).sum();

    return OrderStatisticsRepresentation.builder()
      .total(total)
      .received(received)
      .inProgress(inProgress)
      .completed(completed)
      .cancelled(cancelled)
      .overdue(overdue)
      .startDate(startDate != null ? startDate.toString() : null)
      .endDate(endDate != null ? endDate.toString() : null)
      .build();
  }

  /**
   * Get orders filtered by date range with pagination
   */
  @Transactional(readOnly = true)
  public Page<OrderRepresentation> getOrdersByDateRange(Long tenantId, LocalDate startDate, LocalDate endDate, int page, int size) {
    Pageable pageable = PageRequest.of(page, size);
    Page<Order> orderPage = orderRepository.findByTenantIdAndOrderDateBetweenAndDeletedFalse(tenantId, startDate, endDate, pageable);
    
    return orderPage.map(orderAssembler::dissemble);
  }

  /**
   * Get critical orders overview for dashboard
   * Returns orders requiring immediate attention grouped by category
   */
  @Transactional(readOnly = true)
  public Map<String, Object> getCriticalOrdersOverview(Long tenantId, int limit) {
    Map<String, Object> overview = new HashMap<>();
    
    // 1. Get overdue orders
    List<Order> overdueOrders = orderRepository.findOverdueOrders(tenantId).stream()
      .filter(Order::isOverdue)
      .limit(limit)
      .toList();
    overview.put("overdueOrders", overdueOrders.stream().map(orderAssembler::dissemble).toList());
    
    // 2. Get orders due soon (within next 2 days)
    LocalDate today = LocalDate.now();
    LocalDate twoDaysFromNow = today.plusDays(2);
    List<Order> ordersDueSoon = orderRepository.findByTenantIdAndDeletedFalse(tenantId).stream()
      .filter(order -> order.getOrderStatus() != Order.OrderStatus.COMPLETED && 
                      order.getOrderStatus() != Order.OrderStatus.CANCELLED &&
                      !order.isOverdue())
      .filter(order -> {
        Integer expectedEta = order.calculateExpectedEta();
        if (expectedEta == null) return false;
        
        LocalDate expectedCompletion = order.getOrderDate().plusDays(expectedEta);
        return !expectedCompletion.isBefore(today) && 
               !expectedCompletion.isAfter(twoDaysFromNow);
      })
      .limit(limit)
      .toList();
    overview.put("dueSoon", ordersDueSoon.stream().map(orderAssembler::dissemble).toList());
    
    // 3. Get high priority orders in progress (priority 1)
    List<Order> highPriorityOrders = orderRepository
      .findByTenantIdAndOrderStatusAndDeletedFalse(tenantId, Order.OrderStatus.IN_PROGRESS).stream()
      .filter(order -> order.getPriority() == 1)
      .limit(limit)
      .toList();
    overview.put("highPriorityInProgress", highPriorityOrders.stream().map(orderAssembler::dissemble).toList());
    
    // 4. Get orders with inventory shortage
    List<Order> inventoryShortageOrders = orderRepository.findByTenantIdAndDeletedFalse(tenantId).stream()
      .filter(order -> Boolean.TRUE.equals(order.getHasInventoryShortage()) &&
                      order.getOrderStatus() != Order.OrderStatus.COMPLETED &&
                      order.getOrderStatus() != Order.OrderStatus.CANCELLED)
      .limit(limit)
      .toList();
    overview.put("inventoryShortage", inventoryShortageOrders.stream().map(orderAssembler::dissemble).toList());
    
    // Add counts for each category
    overview.put("counts", Map.of(
      "overdue", overdueOrders.size(),
      "dueSoon", ordersDueSoon.size(),
      "highPriority", highPriorityOrders.size(),
      "inventoryShortage", inventoryShortageOrders.size()
    ));
    
    log.info("Retrieved critical orders overview for tenant: {}", tenantId);
    return overview;
  }

  @Transactional(readOnly = true)
  public OrderItemWorkflow getOrderItemWorkflowByItemWorkflowId(Long itemWorkflowId) {
    log.debug("Fetching OrderItemWorkflow for itemWorkflowId: {}", itemWorkflowId);
    
    return orderItemWorkflowRepository.findByItemWorkflowId(itemWorkflowId)
        .orElseThrow(() -> new ResourceNotFoundException(
            "OrderItemWorkflow not found for itemWorkflowId: " + itemWorkflowId));
  }

  /**
   * Check inventory availability for a workflow before adding it to an order item
   * This can be called by the frontend before submitting a workflow
   * 
   * @param tenantId The tenant ID
   * @param itemId The item ID
   * @param workflow The workflow to check (must include quantity and workType)
   * @return Map containing inventory check results
   */
  @Transactional(readOnly = true)
  public Map<String, Object> checkInventoryForWorkflow(Long tenantId, Long itemId, OrderItemWorkflowRepresentation workflow) {
    log.info("Checking inventory for workflow - Tenant: {}, Item: {}, Quantity: {}", 
      tenantId, itemId, workflow.getQuantity());
    
    return inventoryAvailabilityService.checkInventoryForWorkflow(tenantId, itemId, workflow);
  }

  /**
   * Get Order ID from Item Workflow ID
   * Used when we need to trace back from a workflow to its order
   */
  @Transactional(readOnly = true)
  public Long getOrderIdByItemWorkflowId(Long itemWorkflowId) {
    log.debug("Fetching orderId for itemWorkflowId: {}", itemWorkflowId);
    
    OrderItemWorkflow orderItemWorkflow = orderItemWorkflowRepository.findByItemWorkflowId(itemWorkflowId)
        .orElseThrow(() -> new ResourceNotFoundException(
            "OrderItemWorkflow not found for itemWorkflowId: " + itemWorkflowId));
    
    Long orderId = orderItemWorkflow.getOrderItem().getOrder().getId();
    log.debug("Found orderId: {} for itemWorkflowId: {}", orderId, itemWorkflowId);
    
    return orderId;
  }

  @Transactional(readOnly = true)
  public Order getOrderByItemWorkflowId(Long itemWorkflowId) {
    log.debug("Fetching order for itemWorkflowId: {}", itemWorkflowId);

    OrderItemWorkflow orderItemWorkflow = orderItemWorkflowRepository.findByItemWorkflowId(itemWorkflowId)
        .orElseThrow(() -> new ResourceNotFoundException(
            "OrderItemWorkflow not found for itemWorkflowId: " + itemWorkflowId));

    Order order = orderItemWorkflow.getOrderItem().getOrder();
    log.debug("Found order PO No.: {} for itemWorkflowId: {}", order.getPoNumber(), itemWorkflowId);

    return order;
  }

}
