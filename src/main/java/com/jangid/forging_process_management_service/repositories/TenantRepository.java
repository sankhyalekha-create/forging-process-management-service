package com.jangid.forging_process_management_service.repositories;

import com.jangid.forging_process_management_service.entities.Tenant;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TenantRepository extends CrudRepository<Tenant, Long> {
}
