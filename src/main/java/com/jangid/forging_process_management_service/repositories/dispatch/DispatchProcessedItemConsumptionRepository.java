package com.jangid.forging_process_management_service.repositories.dispatch;

import com.jangid.forging_process_management_service.entities.dispatch.DispatchProcessedItemConsumption;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for DispatchProcessedItemConsumption entity.
 */
@Repository
public interface DispatchProcessedItemConsumptionRepository extends JpaRepository<DispatchProcessedItemConsumption, Long> {

}