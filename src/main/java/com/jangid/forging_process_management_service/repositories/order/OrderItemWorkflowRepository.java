package com.jangid.forging_process_management_service.repositories.order;

import com.jangid.forging_process_management_service.entities.order.OrderItemWorkflow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderItemWorkflowRepository extends JpaRepository<OrderItemWorkflow, Long> {

  // Find workflows by order item
  
  /**
   * Find OrderItemWorkflow by ItemWorkflow ID
   */
  @Query("SELECT oiw FROM OrderItemWorkflow oiw WHERE oiw.itemWorkflow.id = :itemWorkflowId")
  Optional<OrderItemWorkflow> findByItemWorkflowId(@Param("itemWorkflowId") Long itemWorkflowId);
}
