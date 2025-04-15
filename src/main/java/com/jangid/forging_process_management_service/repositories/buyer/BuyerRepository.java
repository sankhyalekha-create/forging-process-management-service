package com.jangid.forging_process_management_service.repositories.buyer;

import com.jangid.forging_process_management_service.entities.buyer.Buyer;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BuyerRepository extends CrudRepository<Buyer, Long> {
    Optional<Buyer> findByIdAndTenantIdAndDeletedFalse(long id, long tenantId);
    Optional<Buyer> findByIdAndDeletedFalse(long id);
} 