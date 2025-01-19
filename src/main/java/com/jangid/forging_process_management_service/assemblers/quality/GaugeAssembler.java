package com.jangid.forging_process_management_service.assemblers.quality;

import com.jangid.forging_process_management_service.entities.quality.Gauge;
import com.jangid.forging_process_management_service.entitiesRepresentation.quality.GaugeRepresentation;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

@Slf4j
@Component
public class GaugeAssembler {

  public GaugeRepresentation dissemble(Gauge gauge){
    return GaugeRepresentation.builder()
        .id(gauge.getId())
        .gaugeName(gauge.getGaugeName())
        .gaugeLocation(gauge.getGaugeLocation())
        .gaugeDetails(gauge.getGaugeDetails())
        .build();
  }

  public Gauge assemble(GaugeRepresentation gaugeRepresentation){
    return Gauge.builder()
        .gaugeName(gaugeRepresentation.getGaugeName())
        .gaugeLocation(gaugeRepresentation.getGaugeLocation())
        .gaugeDetails(gaugeRepresentation.getGaugeDetails())
        .build();
  }

}
