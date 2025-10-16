package com.jangid.forging_process_management_service.repositories.workflow;

import com.jangid.forging_process_management_service.entities.workflow.ItemWorkflowStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemWorkflowStepRepository extends JpaRepository<ItemWorkflowStep, Long> {
  
}

