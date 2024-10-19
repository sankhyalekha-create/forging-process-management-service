package com.jangid.forging_process_management_service.repositories.inventory;

import com.jangid.forging_process_management_service.entities.inventory.RawMaterialHeat;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RawMaterialInspectionReportRepository extends CrudRepository<RawMaterialHeat, Long> {
//  List<RawMaterial> findByName(String name);
}

