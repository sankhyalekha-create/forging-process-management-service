package com.jangid.forging_process_management_service.assemblers.quality;

import com.jangid.forging_process_management_service.assemblers.forging.ForgeAssembler;
import com.jangid.forging_process_management_service.assemblers.machining.ProcessedItemMachiningBatchAssembler;
import com.jangid.forging_process_management_service.assemblers.product.ItemAssembler;
import com.jangid.forging_process_management_service.entities.ProcessedItem;
import com.jangid.forging_process_management_service.entities.product.ItemStatus;
import com.jangid.forging_process_management_service.entities.quality.DailyMachiningBatchInspectionDistribution;
import com.jangid.forging_process_management_service.entities.quality.GaugeInspectionReport;
import com.jangid.forging_process_management_service.entities.quality.InspectionBatch;
import com.jangid.forging_process_management_service.entities.quality.ProcessedItemInspectionBatch;
import com.jangid.forging_process_management_service.entitiesRepresentation.ProcessedItemRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.quality.DailyMachiningBatchInspectionDistributionRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.quality.GaugeInspectionReportRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.quality.InspectionBatchRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.quality.ProcessedItemInspectionBatchRepresentation;
import com.jangid.forging_process_management_service.service.ProcessedItemService;
import com.jangid.forging_process_management_service.service.quality.ProcessedItemInspectionBatchService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class ProcessedItemInspectionBatchAssembler {

  @Autowired
  private ForgeAssembler forgeAssembler;

  @Autowired
  private ItemAssembler itemAssembler;

  @Autowired
  private ProcessedItemService processedItemService;
  @Autowired
  private ProcessedItemInspectionBatchService processedItemInspectionBatchService;

  @Autowired
  private ProcessedItemMachiningBatchAssembler processedItemMachiningBatchAssembler;

//  @Autowired
//  private ProcessedItemInspectionBatchAssembler processedItemInspectionBatchAssembler;

  @Autowired
  private GaugeInspectionReportAssembler gaugeInspectionReportAssembler;

  public ProcessedItemInspectionBatchRepresentation dissemble(ProcessedItemInspectionBatch processedItemInspectionBatch) {
    ProcessedItem processedItem = processedItemInspectionBatch.getProcessedItem();
    ProcessedItemRepresentation processedItemRepresentation = processedItem != null ? dissemble(processedItem) : null;

    InspectionBatch inspectionBatch = processedItemInspectionBatch.getInspectionBatch();
    InspectionBatchRepresentation inspectionBatchRepresentation = inspectionBatch != null ? dissemble(inspectionBatch) : null;

    List<GaugeInspectionReportRepresentation> gaugeInspectionReportRepresentations =
        processedItemInspectionBatch.getGaugeInspectionReports() != null
        ? processedItemInspectionBatch.getGaugeInspectionReports().stream()
            .map(gaugeInspectionReportAssembler::dissemble)
            .toList()
        : new ArrayList<>();

    List<DailyMachiningBatchInspectionDistributionRepresentation> distributionDtos =
        processedItemInspectionBatch.getDailyMachiningBatchInspectionDistributions() != null
        ? processedItemInspectionBatch.getDailyMachiningBatchInspectionDistributions().stream()
            .map(this::convertToDto)
            .toList()
        : new ArrayList<>();

    return ProcessedItemInspectionBatchRepresentation.builder()
        .id(processedItemInspectionBatch.getId())
        .processedItem(processedItemRepresentation)
        .inspectionBatch(inspectionBatchRepresentation)
        .gaugeInspectionReports(gaugeInspectionReportRepresentations)
        .inspectionBatchPiecesCount(processedItemInspectionBatch.getInspectionBatchPiecesCount())
        .availableInspectionBatchPiecesCount(processedItemInspectionBatch.getAvailableInspectionBatchPiecesCount())
        .finishedInspectionBatchPiecesCount(processedItemInspectionBatch.getFinishedInspectionBatchPiecesCount())
        .rejectInspectionBatchPiecesCount(processedItemInspectionBatch.getRejectInspectionBatchPiecesCount())
        .reworkPiecesCount(processedItemInspectionBatch.getReworkPiecesCount())
        .availableDispatchPiecesCount(processedItemInspectionBatch.getAvailableDispatchPiecesCount())
        .dispatchedPiecesCount(processedItemInspectionBatch.getDispatchedPiecesCount())
        .itemStatus(processedItemInspectionBatch.getItemStatus().name())
        .dailyMachiningBatchInspectionDistribution(distributionDtos)
        .build();
  }

  public ProcessedItemInspectionBatch assemble(ProcessedItemInspectionBatchRepresentation representation) {
    if (representation.getId() != null) {
      return processedItemInspectionBatchService.getProcessedItemInspectionBatchById(representation.getId());
    }
    ProcessedItem processedItem = representation.getProcessedItem() != null
                                  ? processedItemService.getProcessedItemById(representation.getProcessedItem().getId())
                                  : null;

//    InspectionBatch inspectionBatch = representation.getInspectionBatch() != null
//                                      ? processedItemInspectionBatchAssembler.assemble(representation.getInspectionBatch())
//                                      : null;

    List<GaugeInspectionReport> gaugeInspectionReports = representation.getGaugeInspectionReports() != null
                                                         ? representation.getGaugeInspectionReports().stream()
                                                             .map(gaugeInspectionReportAssembler::assemble)
                                                             .toList()
                                                         : new ArrayList<>();

    return ProcessedItemInspectionBatch.builder()
        .id(representation.getId())
        .processedItem(processedItem)
//        .inspectionBatch(inspectionBatch)
        .gaugeInspectionReports(gaugeInspectionReports)
        .inspectionBatchPiecesCount(representation.getInspectionBatchPiecesCount())
        .availableInspectionBatchPiecesCount(representation.getAvailableInspectionBatchPiecesCount())
        .finishedInspectionBatchPiecesCount(representation.getFinishedInspectionBatchPiecesCount())
        .rejectInspectionBatchPiecesCount(representation.getRejectInspectionBatchPiecesCount())
        .reworkPiecesCount(representation.getReworkPiecesCount())
        .availableDispatchPiecesCount(representation.getAvailableDispatchPiecesCount())
        .dispatchedPiecesCount(representation.getDispatchedPiecesCount())
        .itemStatus(representation.getItemStatus() != null
                    ? ItemStatus.valueOf(representation.getItemStatus())
                    : null)
        .build();
  }

  private DailyMachiningBatchInspectionDistributionRepresentation convertToDto(DailyMachiningBatchInspectionDistribution distribution) {
    return DailyMachiningBatchInspectionDistributionRepresentation.builder()
        .dailyMachiningBatchId(distribution.getDailyMachiningBatch().getId())
        .rejectedPiecesCount(distribution.getRejectedPiecesCount())
        .reworkPiecesCount(distribution.getReworkPiecesCount())
        .actualCompletedPiecesCount(distribution.getDailyMachiningBatch().getCompletedPiecesCount())
        .build();
  }

  public ProcessedItemInspectionBatch createAssemble(ProcessedItemInspectionBatchRepresentation representation) {
    ProcessedItemInspectionBatch processedItemInspectionBatch = assemble(representation);
    processedItemInspectionBatch.setCreatedAt(LocalDateTime.now());
    return processedItemInspectionBatch;
  }

  private ProcessedItemRepresentation dissemble(ProcessedItem processedItem) {
    return ProcessedItemRepresentation.builder()
        .id(processedItem.getId())
        .forge(processedItem.getForge() != null ? forgeAssembler.dissemble(processedItem.getForge()) : null)
        .item(itemAssembler.dissemble(processedItem.getItem()))
        .expectedForgePiecesCount(processedItem.getExpectedForgePiecesCount())
        .actualForgePiecesCount(processedItem.getActualForgePiecesCount())
//        .availableForgePiecesCountForHeat(processedItem.getAvailableForgePiecesCountForHeat())
        .deleted(processedItem.isDeleted())
        .build();
  }

  public InspectionBatchRepresentation dissemble(InspectionBatch inspectionBatch) {
    return InspectionBatchRepresentation.builder()
        .id(inspectionBatch.getId())
        .inspectionBatchNumber(inspectionBatch.getInspectionBatchNumber())
        .processedItemMachiningBatch(inspectionBatch.getInputProcessedItemMachiningBatch() != null
                                     ? processedItemMachiningBatchAssembler.dissemble(inspectionBatch.getInputProcessedItemMachiningBatch())
                                     : null)
        .inspectionBatchStatus(inspectionBatch.getInspectionBatchStatus() != null
                               ? inspectionBatch.getInspectionBatchStatus().name()
                               : null)
        .startAt(inspectionBatch.getStartAt() != null ? inspectionBatch.getStartAt().toString() : null)
        .endAt(inspectionBatch.getEndAt() != null ? inspectionBatch.getEndAt().toString() : null)
        .build();
  }

}

