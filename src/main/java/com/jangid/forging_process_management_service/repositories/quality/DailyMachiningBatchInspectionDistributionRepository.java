package com.jangid.forging_process_management_service.repositories.quality;

import com.jangid.forging_process_management_service.entities.quality.DailyMachiningBatchInspectionDistribution;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DailyMachiningBatchInspectionDistributionRepository extends CrudRepository<DailyMachiningBatchInspectionDistribution, Long> {

  List<DailyMachiningBatchInspectionDistribution> findByProcessedItemInspectionBatchIdAndDeletedFalse(Long processedItemInspectionBatchId);
  
  void deleteByProcessedItemInspectionBatchId(Long processedItemInspectionBatchId);
} 