package com.jangid.forging_process_management_service.assemblers.quality;

import com.jangid.forging_process_management_service.entities.quality.GaugeInspectionReport;
import com.jangid.forging_process_management_service.entitiesRepresentation.quality.GaugeInspectionReportRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.quality.GaugeRepresentation;
import com.jangid.forging_process_management_service.service.quality.GaugeService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class GaugeInspectionReportAssembler {

  @Autowired
  private GaugeAssembler gaugeAssembler;

  @Autowired
  private GaugeService gaugeService;

  public GaugeInspectionReportRepresentation dissemble(GaugeInspectionReport gaugeInspectionReport) {
    GaugeRepresentation gaugeRepresentation = gaugeAssembler.dissemble(gaugeInspectionReport.getGauge());

    return GaugeInspectionReportRepresentation.builder()
        .id(gaugeInspectionReport.getId())
        .processedItemInspectionBatchId(
            gaugeInspectionReport.getProcessedItemInspectionBatch() != null
            ? gaugeInspectionReport.getProcessedItemInspectionBatch().getId()
            : null)
        .gauge(gaugeRepresentation)
        .finishedPiecesCount(gaugeInspectionReport.getFinishedPiecesCount())
        .rejectedPiecesCount(gaugeInspectionReport.getRejectedPiecesCount())
        .reworkPiecesCount(gaugeInspectionReport.getReworkPiecesCount())
        .build();
  }

  public GaugeInspectionReport assemble(GaugeInspectionReportRepresentation representation) {
    return GaugeInspectionReport.builder()
        .id(representation.getId())
        .gauge(representation.getGauge() != null && representation.getGauge().getId() != null && representation.getGauge().getTenantId() != null ? gaugeService.getGaugeByIdAndTenantId(
            representation.getGauge().getId(), representation.getGauge().getTenantId()) : null)
        .finishedPiecesCount(representation.getFinishedPiecesCount())
        .rejectedPiecesCount(representation.getRejectedPiecesCount())
        .reworkPiecesCount(representation.getReworkPiecesCount())
        .build();
  }

  public GaugeInspectionReport createAssemble(GaugeInspectionReportRepresentation representation) {
    GaugeInspectionReport gaugeInspectionReport = assemble(representation);
    gaugeInspectionReport.setCreatedAt(LocalDateTime.now());
    return gaugeInspectionReport;
  }
}

