package com.jangid.forging_process_management_service.repositories.inventory;

import com.jangid.forging_process_management_service.entities.inventory.RawMaterialHeat;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RawMaterialHeatRepository extends CrudRepository<RawMaterialHeat, Long> {
  List<RawMaterialHeat> findByHeatNumberAndDeletedIsFalse(String heatNumber);
}

