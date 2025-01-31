package com.jangid.forging_process_management_service.assemblers.quality;

import com.jangid.forging_process_management_service.entities.quality.Gauge;
import com.jangid.forging_process_management_service.entitiesRepresentation.quality.GaugeRepresentation;
import com.jangid.forging_process_management_service.service.TenantService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class GaugeAssembler {

  @Autowired
  private TenantService tenantService;

  public GaugeRepresentation dissemble(Gauge gauge){
    return GaugeRepresentation.builder()
        .id(gauge.getId())
        .gaugeName(gauge.getGaugeName())
        .gaugeLocation(gauge.getGaugeLocation())
        .gaugeDetails(gauge.getGaugeDetails())
        .tenantId(gauge.getTenant().getId())
        .build();
  }

  public Gauge assemble(GaugeRepresentation gaugeRepresentation){
    return Gauge.builder()
        .gaugeName(gaugeRepresentation.getGaugeName())
        .gaugeLocation(gaugeRepresentation.getGaugeLocation())
        .gaugeDetails(gaugeRepresentation.getGaugeDetails())
        .tenant(tenantService.getTenantById(gaugeRepresentation.getTenantId()))
        .build();
  }

}
