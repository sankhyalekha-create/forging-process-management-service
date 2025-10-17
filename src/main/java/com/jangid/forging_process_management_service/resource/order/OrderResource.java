package com.jangid.forging_process_management_service.resource.order;

import com.jangid.forging_process_management_service.configuration.security.TenantContextHolder;
import com.jangid.forging_process_management_service.entities.order.Order;
import com.jangid.forging_process_management_service.entitiesRepresentation.order.CreateOrderRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.order.OrderRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.order.OrderStatisticsRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.order.OrderItemRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.order.UpdateOrderStatusRequest;
import com.jangid.forging_process_management_service.entitiesRepresentation.order.UpdateOrderPriorityRequest;
import com.jangid.forging_process_management_service.entitiesRepresentation.order.UpdateOrderRequest;
import com.jangid.forging_process_management_service.utils.GenericExceptionHandler;
import com.jangid.forging_process_management_service.service.order.OrderService;
import com.jangid.forging_process_management_service.service.order.InventoryAvailabilityService;
import com.jangid.forging_process_management_service.utils.GenericResourceUtils;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api")
@Api(tags = "Order Management", description = "Operations for managing orders")
public class OrderResource {

  @Autowired
  private OrderService orderService;

  @Autowired
  private InventoryAvailabilityService inventoryAvailabilityService;

  @PostMapping("/orders")
  @ApiOperation(value = "Create a new order",
               notes = "Creates a new order with items and optional workflows")
  public ResponseEntity<?> createOrder(
      @Valid @RequestBody CreateOrderRepresentation request) {
    try {
      Long tenantIdLong = TenantContextHolder.getAuthenticatedTenantId();

      OrderRepresentation createdOrder = orderService.createOrder(tenantIdLong, request);
      return ResponseEntity.status(HttpStatus.CREATED).body(createdOrder);

    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "createOrder");
    }
  }

  @GetMapping("/orders")
  @ApiOperation(value = "Get all orders for a tenant", 
               notes = "Returns paginated list of orders with optional filtering")
  public ResponseEntity<?> getOrders(
      @ApiParam(value = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
      @ApiParam(value = "Page size") @RequestParam(defaultValue = "20") int size,
      @ApiParam(value = "PO Number filter") @RequestParam(required = false) String poNumber,
      @ApiParam(value = "Customer name filter") @RequestParam(required = false) String customerName,
      @ApiParam(value = "Order status filter") @RequestParam(required = false) String status,
      @ApiParam(value = "Priority filter") @RequestParam(required = false) Integer priority,
      @ApiParam(value = "Start date filter (yyyy-mm-dd)") @RequestParam(required = false) String startDate,
      @ApiParam(value = "End date filter (yyyy-mm-dd)") @RequestParam(required = false) String endDate) {
    try {
      Long tenantIdLong = TenantContextHolder.getAuthenticatedTenantId();

      LocalDate startDateParsed = startDate != null ? LocalDate.parse(startDate) : null;
      LocalDate endDateParsed = endDate != null ? LocalDate.parse(endDate) : null;

      Page<OrderRepresentation> orders;
      
      if (poNumber != null || customerName != null || status != null || priority != null || startDateParsed != null || endDateParsed != null) {
        // Use search with filters
        orders = orderService.searchOrders(tenantIdLong, poNumber, customerName, status, priority,
                                         startDateParsed, endDateParsed, page, size);
      } else {
        // Get all orders
        orders = orderService.getOrders(tenantIdLong, page, size);
      }

      return ResponseEntity.ok(orders);

    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "getOrders");
    }
  }

  @GetMapping("/orders/{orderId}")
  @ApiOperation(value = "Get order by ID", 
               notes = "Returns detailed order information including items and workflows")
  public ResponseEntity<?> getOrderById(
      @ApiParam(value = "Order ID", required = true) @PathVariable String orderId) {
    try {
      Long tenantIdLong = TenantContextHolder.getAuthenticatedTenantId();

      Long orderIdLong = GenericResourceUtils.convertResourceIdToLong(orderId)
        .orElseThrow(() -> new IllegalArgumentException("Invalid order ID"));

      OrderRepresentation order = orderService.getOrderById(tenantIdLong, orderIdLong);
      return ResponseEntity.ok(order);

    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "getOrderById");
    }
  }

  @PutMapping("/orders/{orderId}")
  @ApiOperation(value = "Update order", 
               notes = "Updates order details. Only allowed when order status is RECEIVED")
  public ResponseEntity<?> updateOrder(
      @ApiParam(value = "Order ID", required = true) @PathVariable String orderId,
      @Valid @RequestBody UpdateOrderRequest request) {
    try {
      Long tenantIdLong = TenantContextHolder.getAuthenticatedTenantId();

      Long orderIdLong = GenericResourceUtils.convertResourceIdToLong(orderId)
        .orElseThrow(() -> new IllegalArgumentException("Invalid order ID"));

      OrderRepresentation updatedOrder = orderService.updateOrder(tenantIdLong, orderIdLong, request);
      return ResponseEntity.ok(updatedOrder);

    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "updateOrder");
    }
  }

  @PutMapping("/orders/{orderId}/status")
  @ApiOperation(value = "Update order status", 
               notes = "Updates the order status with validation of allowed transitions")
  public ResponseEntity<?> updateOrderStatus(
      @ApiParam(value = "Order ID", required = true) @PathVariable String orderId,
      @Valid @RequestBody UpdateOrderStatusRequest request) {
    try {
      Long tenantIdLong = TenantContextHolder.getAuthenticatedTenantId();

      Long orderIdLong = GenericResourceUtils.convertResourceIdToLong(orderId)
        .orElseThrow(() -> new IllegalArgumentException("Invalid order ID"));

      Order.OrderStatus newStatus = Order.OrderStatus.valueOf(request.getOrderStatus());
      OrderRepresentation updatedOrder = orderService.updateOrderStatus(tenantIdLong, orderIdLong, 
                                                                       newStatus, request.getNotes());
      return ResponseEntity.ok(updatedOrder);

    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "updateOrderStatus");
    }
  }

  @PutMapping("/orders/{orderId}/priority")
  @ApiOperation(value = "Update order priority", 
               notes = "Updates the priority of an order")
  public ResponseEntity<?> updateOrderPriority(
      @ApiParam(value = "Order ID", required = true) @PathVariable String orderId,
      @Valid @RequestBody UpdateOrderPriorityRequest request) {
    try {
      Long tenantIdLong = TenantContextHolder.getAuthenticatedTenantId();

      Long orderIdLong = GenericResourceUtils.convertResourceIdToLong(orderId)
        .orElseThrow(() -> new IllegalArgumentException("Invalid order ID"));

      OrderRepresentation updatedOrder = orderService.updateOrderPriority(tenantIdLong, orderIdLong, 
                                                                         request.getPriority(), request.getNotes());
      return ResponseEntity.ok(updatedOrder);

    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "updateOrderPriority");
    }
  }

  @PostMapping("/orders-check-inventory")
  @ApiOperation(value = "Check inventory availability for order items", 
               notes = "Checks if raw material inventory is sufficient for the order items")
  public ResponseEntity<?> checkInventoryAvailability(
      @Valid @RequestBody List<OrderItemRepresentation> orderItems) {
    try {
      Long tenantIdLong = TenantContextHolder.getAuthenticatedTenantId();

      Map<String, Object> result = inventoryAvailabilityService
        .checkInventoryForOrderItems(tenantIdLong, orderItems);
      
      return ResponseEntity.ok(result);

    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "checkInventoryAvailability");
    }
  }

  @DeleteMapping("/orders/{orderId}")
  @ApiOperation(value = "Delete order",
               notes = "Soft deletes an order if no workflows are in progress")
  public ResponseEntity<?> deleteOrder(
      @ApiParam(value = "Order ID", required = true) @PathVariable String orderId) {
    try {
      Long tenantIdLong = TenantContextHolder.getAuthenticatedTenantId();

      Long orderIdLong = GenericResourceUtils.convertResourceIdToLong(orderId)
        .orElseThrow(() -> new IllegalArgumentException("Invalid order ID"));

      orderService.deleteOrder(tenantIdLong, orderIdLong);
      return ResponseEntity.noContent().build();

    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "deleteOrder");
    }
  }

  @GetMapping("/orders/status/{status}")
  @ApiOperation(value = "Get orders by status", 
               notes = "Returns all orders with the specified status")
  public ResponseEntity<?> getOrdersByStatus(
      @ApiParam(value = "Order Status", required = true) @PathVariable String status) {
    try {
      Long tenantIdLong = TenantContextHolder.getAuthenticatedTenantId();

      Order.OrderStatus orderStatus = Order.OrderStatus.valueOf(status.toUpperCase());
      List<OrderRepresentation> orders = orderService.getOrdersByStatus(tenantIdLong, orderStatus);
      return ResponseEntity.ok(orders);

    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "getOrdersByStatus");
    }
  }

  @GetMapping("/orders/overdue")
  @ApiOperation(value = "Get overdue orders", 
               notes = "Returns orders that are past their expected delivery date")
  public ResponseEntity<?> getOverdueOrders() {
    try {
      Long tenantIdLong = TenantContextHolder.getAuthenticatedTenantId();

      List<OrderRepresentation> orders = orderService.getOverdueOrders(tenantIdLong);
      return ResponseEntity.ok(orders);

    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "getOverdueOrders");
    }
  }

  @GetMapping("/orders/dashboard")
  @ApiOperation(value = "Get order dashboard data", 
               notes = "Returns summary statistics for order management dashboard")
  public ResponseEntity<?> getOrderDashboard() {
    try {
      Long tenantIdLong = TenantContextHolder.getAuthenticatedTenantId();

      // Get orders by different statuses for dashboard
      List<OrderRepresentation> receivedOrders = orderService.getOrdersByStatus(tenantIdLong, Order.OrderStatus.RECEIVED);
      List<OrderRepresentation> inProgressOrders = orderService.getOrdersByStatus(tenantIdLong, Order.OrderStatus.IN_PROGRESS);
      List<OrderRepresentation> completedOrders = orderService.getOrdersByStatus(tenantIdLong, Order.OrderStatus.COMPLETED);
      List<OrderRepresentation> overdueOrders = orderService.getOverdueOrders(tenantIdLong);

      // Create dashboard response
      var dashboardData = new HashMap<>();
      dashboardData.put("receivedCount", receivedOrders.size());
      dashboardData.put("inProgressCount", inProgressOrders.size());
      dashboardData.put("completedCount", completedOrders.size());
      dashboardData.put("overdueCount", overdueOrders.size());
      dashboardData.put("totalOrders", receivedOrders.size() + inProgressOrders.size() + completedOrders.size());
      dashboardData.put("overdueOrders", overdueOrders);

      return ResponseEntity.ok(dashboardData);

    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "getOrderDashboard");
    }
  }

  @GetMapping("/orders/statistics")
  @ApiOperation(value = "Get order statistics", 
               notes = "Returns order counts by status for the specified date range")
  public ResponseEntity<?> getOrderStatistics(
      @ApiParam(value = "Start date (YYYY-MM-DD)", required = false) @RequestParam(required = false) String startDate,
      @ApiParam(value = "End date (YYYY-MM-DD)", required = false) @RequestParam(required = false) String endDate) {
    try {
      Long tenantIdLong = TenantContextHolder.getAuthenticatedTenantId();

      LocalDate start = null;
      LocalDate end = null;
      
      if (startDate != null && !startDate.isEmpty()) {
        start = LocalDate.parse(startDate);
      }
      if (endDate != null && !endDate.isEmpty()) {
        end = LocalDate.parse(endDate);
      }

      OrderStatisticsRepresentation statistics = orderService.getOrderStatistics(tenantIdLong, start, end);
      return ResponseEntity.ok(statistics);

    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "getOrderStatistics");
    }
  }

  @GetMapping("/orders/by-date-range")
  @ApiOperation(value = "Get orders by date range", 
               notes = "Returns paginated orders filtered by date range")
  public ResponseEntity<?> getOrdersByDateRange(
      @ApiParam(value = "Start date (YYYY-MM-DD)", required = true) @RequestParam String startDate,
      @ApiParam(value = "End date (YYYY-MM-DD)", required = true) @RequestParam String endDate,
      @ApiParam(value = "Page number (0-based)", required = false) @RequestParam(defaultValue = "0") int page,
      @ApiParam(value = "Page size", required = false) @RequestParam(defaultValue = "10") int size) {
    try {
      Long tenantIdLong = TenantContextHolder.getAuthenticatedTenantId();

      LocalDate start = LocalDate.parse(startDate);
      LocalDate end = LocalDate.parse(endDate);

      Page<OrderRepresentation> orders = orderService.getOrdersByDateRange(tenantIdLong, start, end, page, size);
      return ResponseEntity.ok(orders);

    } catch (Exception exception) {
      return GenericExceptionHandler.handleException(exception, "getOrdersByDateRange");
    }
  }
}
