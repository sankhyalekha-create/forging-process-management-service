package com.jangid.forging_process_management_service.repositories.order;

import com.jangid.forging_process_management_service.entities.order.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

  // Find order items by order ID
  @Query("SELECT oi FROM OrderItem oi WHERE oi.order.id = :orderId")
  List<OrderItem> findByOrderId(@Param("orderId") Long orderId);

  // Find order item by ID and order ID
  @Query("SELECT oi FROM OrderItem oi WHERE oi.id = :orderItemId AND oi.order.id = :orderId")
  Optional<OrderItem> findByIdAndOrderId(@Param("orderItemId") Long orderItemId, @Param("orderId") Long orderId);

  // Find order items by item ID
  @Query("SELECT oi FROM OrderItem oi WHERE oi.item.id = :itemId AND oi.order.deleted = false")
  List<OrderItem> findByItemIdAndOrderNotDeleted(@Param("itemId") Long itemId);

  // Find order items by order and tenant
  @Query("SELECT oi FROM OrderItem oi WHERE oi.order.id = :orderId AND oi.order.tenant.id = :tenantId AND oi.order.deleted = false")
  List<OrderItem> findByOrderIdAndTenantId(@Param("orderId") Long orderId, @Param("tenantId") Long tenantId);

  // Find order items with workflows in progress
  @Query("SELECT DISTINCT oi FROM OrderItem oi " +
    "JOIN oi.orderItemWorkflows oiw " +
    "WHERE oi.order.tenant.id = :tenantId " +
    "AND oiw.itemWorkflow.workflowStatus = 'IN_PROGRESS' " +
    "AND oi.order.deleted = false")
  List<OrderItem> findOrderItemsWithWorkflowsInProgress(@Param("tenantId") Long tenantId);

  // Find order items without workflows
  @Query("SELECT oi FROM OrderItem oi WHERE oi.order.tenant.id = :tenantId " +
    "AND oi.orderItemWorkflows IS EMPTY " +
    "AND oi.order.deleted = false")
  List<OrderItem> findOrderItemsWithoutWorkflows(@Param("tenantId") Long tenantId);

  // Count order items by order
  @Query("SELECT COUNT(oi) FROM OrderItem oi WHERE oi.order.id = :orderId")
  Long countByOrderId(@Param("orderId") Long orderId);

  // Find order items by item name pattern
  @Query("SELECT oi FROM OrderItem oi WHERE oi.order.tenant.id = :tenantId " +
    "AND LOWER(oi.item.itemName) LIKE LOWER(CONCAT('%', :itemName, '%')) " +
    "AND oi.order.deleted = false")
  List<OrderItem> findByTenantIdAndItemNameContaining(@Param("tenantId") Long tenantId, @Param("itemName") String itemName);

  // Calculate total quantity for an item across all orders
  @Query("SELECT COALESCE(SUM(oi.quantity), 0) FROM OrderItem oi WHERE oi.item.id = :itemId " +
    "AND oi.order.orderStatus NOT IN ('CANCELLED') " +
    "AND oi.order.deleted = false")
  Long getTotalQuantityForItem(@Param("itemId") Long itemId);


}
