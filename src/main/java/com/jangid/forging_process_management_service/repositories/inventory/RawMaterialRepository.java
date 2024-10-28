package com.jangid.forging_process_management_service.repositories.inventory;

import com.jangid.forging_process_management_service.entities.inventory.RawMaterial;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
@Repository
public interface RawMaterialRepository extends CrudRepository<RawMaterial, Long> {

  Page<RawMaterial> findByTenantIdAndDeletedIsFalseOrderByRawMaterialReceivingDateDesc(long tenantId, Pageable pageable);
  Optional<RawMaterial> findById(long id);

  Optional<RawMaterial> findByIdAndTenantIdAndDeletedFalse(long id, long tenantId);
  Optional<RawMaterial> findByTenantIdAndRawMaterialInvoiceNumberAndDeletedIsFalse(long tenantId, String rawMaterialInvoiceNumber);
  List<RawMaterial> findByTenantIdAndRawMaterialReceivingDateGreaterThanAndRawMaterialReceivingDateLessThanAndDeletedIsFalseOrderByRawMaterialReceivingDateDesc(long tenantId, LocalDateTime startDate, LocalDateTime endDate);
  List<RawMaterial> findByTenantIdAndDeletedIsFalseOrderByRawMaterialReceivingDateDesc(long tenantId);

}
