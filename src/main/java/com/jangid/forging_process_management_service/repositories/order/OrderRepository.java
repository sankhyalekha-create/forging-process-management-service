package com.jangid.forging_process_management_service.repositories.order;

import com.jangid.forging_process_management_service.entities.order.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

  // Find orders by tenant
  @Query("SELECT o FROM Order o WHERE o.tenant.id = :tenantId AND o.deleted = false ORDER BY o.createdAt DESC")
  List<Order> findByTenantIdAndDeletedFalse(@Param("tenantId") Long tenantId);

  @Query("SELECT o FROM Order o WHERE o.tenant.id = :tenantId AND o.deleted = false ORDER BY o.createdAt DESC")
  Page<Order> findByTenantIdAndDeletedFalse(@Param("tenantId") Long tenantId, Pageable pageable);

  // Find order by ID and tenant
  @Query("SELECT o FROM Order o WHERE o.id = :orderId AND o.tenant.id = :tenantId AND o.deleted = false")
  Optional<Order> findByIdAndTenantIdAndDeletedFalse(@Param("orderId") Long orderId, @Param("tenantId") Long tenantId);

  // Find by PO number and tenant (for uniqueness validation)
  @Query("SELECT o FROM Order o WHERE o.poNumber = :poNumber AND o.tenant.id = :tenantId AND o.deleted = false")
  Optional<Order> findByPoNumberAndTenantIdAndDeletedFalse(@Param("poNumber") String poNumber, @Param("tenantId") Long tenantId);

  // Find orders by status
  @Query("SELECT o FROM Order o WHERE o.tenant.id = :tenantId AND o.orderStatus = :status AND o.deleted = false ORDER BY o.createdAt DESC")
  List<Order> findByTenantIdAndOrderStatusAndDeletedFalse(@Param("tenantId") Long tenantId, @Param("status") Order.OrderStatus status);

  @Query("SELECT o FROM Order o WHERE o.tenant.id = :tenantId AND o.orderStatus = :status AND o.deleted = false ORDER BY o.createdAt DESC")
  Page<Order> findByTenantIdAndOrderStatusAndDeletedFalse(@Param("tenantId") Long tenantId, @Param("status") Order.OrderStatus status, Pageable pageable);

  // Find orders by customer name
  @Query("SELECT o FROM Order o WHERE o.tenant.id = :tenantId AND LOWER(o.buyer.buyerName) LIKE LOWER(CONCAT('%', :customerName, '%')) AND o.deleted = false ORDER BY o.createdAt DESC")
  List<Order> findByTenantIdAndCustomerNameContainingIgnoreCaseAndDeletedFalse(@Param("tenantId") Long tenantId, @Param("customerName") String customerName);

  // Find orders by date range
  @Query("SELECT o FROM Order o WHERE o.tenant.id = :tenantId AND o.orderDate BETWEEN :startDate AND :endDate AND o.deleted = false ORDER BY o.orderDate DESC")
  List<Order> findByTenantIdAndOrderDateBetweenAndDeletedFalse(@Param("tenantId") Long tenantId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

  @Query("SELECT o FROM Order o WHERE o.tenant.id = :tenantId AND o.orderDate BETWEEN :startDate AND :endDate AND o.deleted = false ORDER BY o.orderDate DESC")
  Page<Order> findByTenantIdAndOrderDateBetweenAndDeletedFalse(@Param("tenantId") Long tenantId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, Pageable pageable);

  // Find orders with delivery date approaching (returns all candidates for service-layer filtering)
  @Query("SELECT o FROM Order o WHERE o.tenant.id = :tenantId " +
    "AND o.orderStatus NOT IN ('COMPLETED', 'CANCELLED') AND o.deleted = false " +
    "ORDER BY o.orderDate ASC")
  List<Order> findOrdersWithDeliveryDateApproaching(@Param("tenantId") Long tenantId);

  // Find overdue orders (returns all candidates for service-layer filtering)
  @Query("SELECT o FROM Order o WHERE o.tenant.id = :tenantId " +
    "AND o.orderStatus NOT IN ('COMPLETED', 'CANCELLED') AND o.deleted = false " +
    "ORDER BY o.orderDate ASC")
  List<Order> findOverdueOrders(@Param("tenantId") Long tenantId);

  // Count orders by status for dashboard
  @Query("SELECT o.orderStatus, COUNT(o) FROM Order o WHERE o.tenant.id = :tenantId AND o.deleted = false GROUP BY o.orderStatus")
  List<Object[]> countOrdersByStatusForTenant(@Param("tenantId") Long tenantId);

  // Find orders that can be deleted (not in progress)
  @Query("SELECT o FROM Order o WHERE o.tenant.id = :tenantId AND o.orderStatus = 'RECEIVED' AND o.deleted = false")
  List<Order> findDeletableOrdersByTenantId(@Param("tenantId") Long tenantId);

  // Search orders with multiple criteria
  @Query("SELECT o FROM Order o WHERE o.tenant.id = :tenantId " +
    "AND (:poNumber IS NULL OR :poNumber = '' OR LOWER(o.poNumber) LIKE LOWER(CONCAT('%', :poNumber, '%'))) " +
    "AND (:customerName IS NULL OR :customerName = '' OR LOWER(o.buyer.buyerName) LIKE LOWER(CONCAT('%', :customerName, '%'))) " +
    "AND (:status IS NULL OR o.orderStatus = :status) " +
    "AND (:priority IS NULL OR o.priority = :priority) " +
    "AND (:startDate IS NULL OR o.orderDate >= :startDate) " +
    "AND (:endDate IS NULL OR o.orderDate <= :endDate) " +
    "AND o.deleted = false " +
    "ORDER BY o.priority ASC, o.createdAt DESC")
  Page<Order> searchOrders(@Param("tenantId") Long tenantId,
                          @Param("poNumber") String poNumber,
                          @Param("customerName") String customerName,
                          @Param("status") Order.OrderStatus status,
                          @Param("priority") Integer priority,
                          @Param("startDate") LocalDate startDate,
                          @Param("endDate") LocalDate endDate,
                          Pageable pageable);
}
