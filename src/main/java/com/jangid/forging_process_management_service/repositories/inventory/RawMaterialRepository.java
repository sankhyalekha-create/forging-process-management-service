package com.jangid.forging_process_management_service.repositories.inventory;

import com.jangid.forging_process_management_service.entities.inventory.RawMaterial;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
@Repository
public interface RawMaterialRepository extends CrudRepository<RawMaterial, Long> {

  Page<RawMaterial> findByTenantIdAndDeletedIsFalseOrderByRawMaterialReceivingDateDesc(long tenantId, Pageable pageable);
  
  @Query("SELECT DISTINCT rm FROM raw_material rm " +
         "LEFT JOIN FETCH rm.rawMaterialProducts rmp " +
         "LEFT JOIN FETCH rmp.product p " +
         "LEFT JOIN FETCH rmp.heats h " +
         "LEFT JOIN FETCH rm.supplier s " +
         "WHERE rm.tenant.id = :tenantId " +
         "AND rm.deleted = false " +
         "AND (rmp.deleted = false OR rmp.deleted IS NULL) " +
         "AND (h.deleted = false OR h.deleted IS NULL) " +
         "ORDER BY rm.rawMaterialReceivingDate DESC")
  List<RawMaterial> findByTenantIdAndDeletedIsFalseWithProductsAndHeats(@Param("tenantId") long tenantId);
  
  Optional<RawMaterial> findById(long id);

  Optional<RawMaterial> findByIdAndTenantIdAndDeletedFalse(long id, long tenantId);
  Optional<RawMaterial> findByTenantIdAndRawMaterialInvoiceNumberAndDeletedIsFalse(long tenantId, String rawMaterialInvoiceNumber);
  List<RawMaterial> findByTenantIdAndRawMaterialReceivingDateGreaterThanAndRawMaterialReceivingDateLessThanAndDeletedIsFalseOrderByRawMaterialReceivingDateDesc(long tenantId, LocalDateTime startDate, LocalDateTime endDate);
  List<RawMaterial> findByTenantIdAndDeletedIsFalseOrderByRawMaterialReceivingDateDesc(long tenantId);
  
  // New methods
  boolean existsByRawMaterialInvoiceNumberAndTenantIdAndDeletedFalse(String rawMaterialInvoiceNumber, long tenantId);
  Optional<RawMaterial> findByRawMaterialInvoiceNumberAndTenantIdAndDeletedTrue(String rawMaterialInvoiceNumber, long tenantId);
}
