package com.jangid.forging_process_management_service.repositories.quality;

import com.jangid.forging_process_management_service.entities.quality.GaugeInspectionReport;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GaugeInspectionReportRepository extends CrudRepository<GaugeInspectionReport, Long> {

  boolean existsByGaugeIdAndDeletedFalse(long gaugeId);

}
