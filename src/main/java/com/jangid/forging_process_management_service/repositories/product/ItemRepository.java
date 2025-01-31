package com.jangid.forging_process_management_service.repositories.product;

import com.jangid.forging_process_management_service.entities.product.Item;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ItemRepository extends CrudRepository<Item, Long> {
  Optional<Item> findByIdAndTenantIdAndDeletedFalse(long id, long tenantId);
  List<Item> findByTenantIdAndDeletedFalseOrderByCreatedAtDesc(long tenantId);
  Page<Item> findByTenantIdAndDeletedFalseOrderByCreatedAtDesc(long tenantId, Pageable pageable);

  boolean existsByIdAndTenantIdAndDeletedFalse(long id, long tenantId);
}
