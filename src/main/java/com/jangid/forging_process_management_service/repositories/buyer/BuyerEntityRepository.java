package com.jangid.forging_process_management_service.repositories.buyer;

import com.jangid.forging_process_management_service.entities.buyer.BuyerEntity;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BuyerEntityRepository extends CrudRepository<BuyerEntity, Long> {
    Optional<BuyerEntity> findByIdAndDeletedFalse(long id);
}