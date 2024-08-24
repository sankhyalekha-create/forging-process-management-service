package com.jangid.forging_process_management_service.repositories;

import com.jangid.forging_process_management_service.entities.RawMaterialHeat;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RawMaterialInspectionReportRepository extends CrudRepository<RawMaterialHeat, Long> {
//  List<RawMaterial> findByName(String name);
}

