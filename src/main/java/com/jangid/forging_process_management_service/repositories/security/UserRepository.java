package com.jangid.forging_process_management_service.repositories.security;

import com.jangid.forging_process_management_service.entities.security.Usr;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends CrudRepository<Usr, Long> {
  Optional<Usr> findByUsernameAndTenant_TenantNameAndDeletedFalse(String username, String tenantName);
}
