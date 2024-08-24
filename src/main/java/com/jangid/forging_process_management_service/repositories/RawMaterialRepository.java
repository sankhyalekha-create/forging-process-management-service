package com.jangid.forging_process_management_service.repositories;

import com.jangid.forging_process_management_service.entities.RawMaterial;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
@Repository
public interface RawMaterialRepository extends CrudRepository<RawMaterial, Long> {

  List<RawMaterial> findByTenantId(long tenantId);
  Optional<RawMaterial> findById(long id);
//  List<RawMaterial> findByName(String name);
//  List<RawMaterial> findByTenantId(long tenantId);
}
