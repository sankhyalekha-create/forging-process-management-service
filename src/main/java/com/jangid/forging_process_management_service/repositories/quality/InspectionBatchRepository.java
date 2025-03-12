package com.jangid.forging_process_management_service.repositories.quality;

import com.jangid.forging_process_management_service.entities.quality.InspectionBatch;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InspectionBatchRepository extends CrudRepository<InspectionBatch, Long> {

  boolean existsByInspectionBatchNumberAndTenantIdAndDeletedFalse(String inspectionBatchNumber, long tenantId);

  List<InspectionBatch> findByTenantIdAndDeletedIsFalseOrderByCreatedAtDesc(long tenantId);

  Page<InspectionBatch> findByTenantIdAndDeletedIsFalseOrderByCreatedAtDesc(long tenantId, Pageable pageable);

}
