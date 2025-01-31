package com.jangid.forging_process_management_service.repositories.quality;

import com.jangid.forging_process_management_service.entities.quality.Gauge;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GaugeRepository extends CrudRepository<Gauge, Long> {
  Page<Gauge> findByTenantIdAndDeletedIsFalseOrderByCreatedAtDesc(long tenantId, Pageable pageable);
  List<Gauge> findByTenantIdAndDeletedIsFalseOrderByCreatedAtDesc(long tenantId);

  Optional<Gauge> findByIdAndTenantIdAndDeletedFalse(long id, long tenantId);
  Optional<Gauge> findByGaugeNameAndTenantIdAndDeletedFalse(String gaugeName, long tenantId);
  boolean existsGaugeByGaugeNameAndTenantIdAndDeletedFalse(String gaugeName, long tenantId);

}
