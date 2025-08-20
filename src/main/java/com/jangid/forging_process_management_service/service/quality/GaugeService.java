package com.jangid.forging_process_management_service.service.quality;

import com.jangid.forging_process_management_service.assemblers.quality.GaugeAssembler;
import com.jangid.forging_process_management_service.entities.Tenant;
import com.jangid.forging_process_management_service.entities.quality.Gauge;
import com.jangid.forging_process_management_service.entitiesRepresentation.quality.GaugeListRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.quality.GaugeRepresentation;
import com.jangid.forging_process_management_service.exception.quality.GaugeNotFoundException;
import com.jangid.forging_process_management_service.repositories.quality.GaugeInspectionReportRepository;
import com.jangid.forging_process_management_service.repositories.quality.GaugeRepository;
import com.jangid.forging_process_management_service.service.TenantService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class GaugeService {

  @Autowired
  private GaugeRepository gaugeRepository;
  @Autowired
  private GaugeInspectionReportRepository gaugeInspectionReportRepository;

  @Autowired
  private TenantService tenantService;

  @Autowired
  private GaugeAssembler gaugeAssembler;

  @Transactional
  public GaugeRepresentation createGauge(Long tenantId, GaugeRepresentation gaugeRepresentation) {
    // First check if an active (not deleted) gauge with the same name exists
    boolean existsByNameNotDeleted = gaugeRepository.existsGaugeByGaugeNameAndTenantIdAndDeletedFalse(
        gaugeRepresentation.getGaugeName(), tenantId);
    if (existsByNameNotDeleted) {
      log.error("Active gauge with name: {} already exists for tenant: {}!", 
                gaugeRepresentation.getGaugeName(), tenantId);
      throw new IllegalStateException("Gauge with name=" + gaugeRepresentation.getGaugeName() 
                                     + " already exists");
    }
    
    // Check if we're trying to revive a deleted gauge
    Gauge gauge = null;
    Optional<Gauge> deletedGauge = gaugeRepository.findByGaugeNameAndTenantIdAndDeletedTrue(
        gaugeRepresentation.getGaugeName(), tenantId);
    
    if (deletedGauge.isPresent()) {
      // We found a deleted gauge with the same name, reactivate it
      log.info("Reactivating previously deleted gauge with name: {}", 
               gaugeRepresentation.getGaugeName());
      gauge = deletedGauge.get();
      gauge.setDeleted(false);
      gauge.setDeletedAt(null);
      
      // Update gauge fields from the representation
      gauge.setGaugeName(gaugeRepresentation.getGaugeName());
      gauge.setGaugeLocation(gaugeRepresentation.getGaugeLocation());
      gauge.setGaugeDetails(gaugeRepresentation.getGaugeDetails());
    } else {
      // Create new gauge
      gauge = gaugeAssembler.assemble(gaugeRepresentation);
      gauge.setCreatedAt(LocalDateTime.now());
      Tenant tenant = tenantService.getTenantById(tenantId);
      gauge.setTenant(tenant);
    }
    
    Gauge createdGauge = gaugeRepository.save(gauge);
    return gaugeAssembler.dissemble(createdGauge);
  }

  public Page<GaugeRepresentation> getAllGaugesOfTenant(long tenantId, int page, int size) {
    Pageable pageable = PageRequest.of(page, size);
    Page<Gauge> gaugePage = gaugeRepository.findByTenantIdAndDeletedIsFalseOrderByCreatedAtDesc(tenantId, pageable);
    return gaugePage.map(gauge -> gaugeAssembler.dissemble(gauge));
  }

  @Transactional
  public GaugeRepresentation updateGauge(Long id, Long tenantId, GaugeRepresentation gaugeRepresentation) {
    Gauge gauge = getGaugeByIdAndTenantId(id, tenantId);
    // Update fields
    if (!gauge.getGaugeName().equals(gaugeRepresentation.getGaugeName())) {
      gauge.setGaugeName(gaugeRepresentation.getGaugeName());
    }

    if (!gauge.getGaugeLocation().equals(gaugeRepresentation.getGaugeLocation())) {
      gauge.setGaugeLocation(gaugeRepresentation.getGaugeLocation());
    }

    if (!gauge.getGaugeDetails().equals(gaugeRepresentation.getGaugeDetails())) {
      gauge.setGaugeDetails(gaugeRepresentation.getGaugeDetails());
    }

    Gauge updatedGauge = gaugeRepository.save(gauge);
    return gaugeAssembler.dissemble(updatedGauge);
  }

  public boolean isGaugeOfTenantHavingMatchineNameExists(long tenantId, String matchineName) {
    return gaugeRepository.existsGaugeByGaugeNameAndTenantIdAndDeletedFalse(matchineName, tenantId);
  }

//  getGaugeByIdAndTenantId

  public Gauge getGaugeByIdAndTenantId(long gaugeId, long tenantId) {
    return gaugeRepository.findByIdAndTenantIdAndDeletedFalse(gaugeId, tenantId)
        .orElseThrow(() -> new GaugeNotFoundException("Gauge not found with id " + gaugeId + " of tenantId=" + tenantId));
  }

  public Gauge getGaugeByNameAndTenantId(String gaugeName, long tenantId) {
    return gaugeRepository.findByGaugeNameAndTenantIdAndDeletedFalse(gaugeName, tenantId)
        .orElseThrow(() -> new GaugeNotFoundException("Gauge not found with gaugeName " + gaugeName + " of tenantId=" + tenantId));
  }

  public GaugeListRepresentation getAllGaugesOfTenantWithoutPagination(long tenantId) {
    List<Gauge> gauges = gaugeRepository.findByTenantIdAndDeletedIsFalseOrderByCreatedAtDesc(tenantId);
    return GaugeListRepresentation.builder()
        .gauges(gauges.stream().map(gauge -> gaugeAssembler.dissemble(gauge)).toList()).build();
  }

  @Transactional
  public void deleteGauge(Long gaugeId, Long tenantId) {
    tenantService.isTenantExists(tenantId);
    Gauge gauge = getGaugeByIdAndTenantId(gaugeId, tenantId);
    gauge.setDeleted(true);
    gauge.setDeletedAt(LocalDateTime.now());
    gaugeRepository.save(gauge);
  }
}
