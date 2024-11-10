package com.jangid.forging_process_management_service.repositories.inventory;

import com.jangid.forging_process_management_service.entities.inventory.Heat;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RawMaterialHeatRepository extends CrudRepository<Heat, Long> {
  List<Heat> findByHeatNumberAndDeletedIsFalse(String heatNumber);
  Optional<Heat> findByIdAndDeletedFalse(long heatId);
  Optional<Heat> findByHeatNumberAndRawMaterialIdAndDeletedFalse(String heatNumber, long tenantId);
}

