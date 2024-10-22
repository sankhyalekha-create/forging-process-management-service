package com.jangid.forging_process_management_service.repositories.forging;

import com.jangid.forging_process_management_service.entities.forging.ForgeTraceability;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ForgeTraceabilityRepository extends JpaRepository<ForgeTraceability, Long> {
  // Additional query methods if needed, for example, find by status or forging line
  List<ForgeTraceability> findByForgingLineIdAndDeletedFalse(long forgingLineId);
}
