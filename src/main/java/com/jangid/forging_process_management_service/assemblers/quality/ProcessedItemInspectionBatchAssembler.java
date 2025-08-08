package com.jangid.forging_process_management_service.assemblers.quality;

import com.jangid.forging_process_management_service.assemblers.product.ItemAssembler;
import com.jangid.forging_process_management_service.entities.product.Item;
import com.jangid.forging_process_management_service.entities.product.ItemStatus;
import com.jangid.forging_process_management_service.entities.quality.DailyMachiningBatchInspectionDistribution;
import com.jangid.forging_process_management_service.entities.quality.GaugeInspectionReport;
import com.jangid.forging_process_management_service.entities.quality.InspectionBatch;
import com.jangid.forging_process_management_service.entities.quality.ProcessedItemInspectionBatch;
import com.jangid.forging_process_management_service.entitiesRepresentation.product.ItemRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.quality.DailyMachiningBatchInspectionDistributionRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.quality.GaugeInspectionReportRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.quality.InspectionBatchRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.quality.InspectionHeatRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.quality.ProcessedItemInspectionBatchRepresentation;
import com.jangid.forging_process_management_service.service.product.ItemService;
import com.jangid.forging_process_management_service.service.quality.ProcessedItemInspectionBatchService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ProcessedItemInspectionBatchAssembler {

  @Autowired
  private ItemAssembler itemAssembler;

  @Autowired
  @Lazy
  private ItemService itemService;
  @Autowired
  private ProcessedItemInspectionBatchService processedItemInspectionBatchService;

  @Autowired
  private GaugeInspectionReportAssembler gaugeInspectionReportAssembler;

  @Autowired
  @Lazy
  private InspectionHeatAssembler inspectionHeatAssembler;

  public ProcessedItemInspectionBatchRepresentation dissemble(ProcessedItemInspectionBatch processedItemInspectionBatch) {
    Item item = processedItemInspectionBatch.getItem();
    ItemRepresentation itemRepresentation = itemAssembler.dissembleBasic(item); // Use dissembleBasic to exclude itemWorkflows

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

    // Dissemble inspection heats
    List<InspectionHeatRepresentation> inspectionHeatRepresentations = 
        processedItemInspectionBatch.getInspectionHeats() != null
        ? processedItemInspectionBatch.getInspectionHeats().stream()
            .map(inspectionHeatAssembler::dissemble)
            .collect(Collectors.toList())
        : new ArrayList<>();

    return ProcessedItemInspectionBatchRepresentation.builder()
        .id(processedItemInspectionBatch.getId())
        .item(itemRepresentation)
        .inspectionBatch(inspectionBatchRepresentation)
        .gaugeInspectionReports(gaugeInspectionReportRepresentations)
        .inspectionHeats(inspectionHeatRepresentations)
        .inspectionBatchPiecesCount(processedItemInspectionBatch.getInspectionBatchPiecesCount())
        .availableInspectionBatchPiecesCount(processedItemInspectionBatch.getAvailableInspectionBatchPiecesCount())
        .finishedInspectionBatchPiecesCount(processedItemInspectionBatch.getFinishedInspectionBatchPiecesCount())
        .rejectInspectionBatchPiecesCount(processedItemInspectionBatch.getRejectInspectionBatchPiecesCount())
        .reworkPiecesCount(processedItemInspectionBatch.getReworkPiecesCount())
        .dispatchedPiecesCount(processedItemInspectionBatch.getDispatchedPiecesCount())
        .itemStatus(processedItemInspectionBatch.getItemStatus().name())
        .workflowIdentifier(processedItemInspectionBatch.getWorkflowIdentifier())
        .itemWorkflowId(processedItemInspectionBatch.getItemWorkflowId())
        .previousOperationProcessedItemId(processedItemInspectionBatch.getPreviousOperationProcessedItemId())
        .dailyMachiningBatchInspectionDistribution(distributionDtos)
        .build();
  }

  public ProcessedItemInspectionBatch assemble(ProcessedItemInspectionBatchRepresentation representation) {
    if (representation.getId() != null) {
      return processedItemInspectionBatchService.getProcessedItemInspectionBatchById(representation.getId());
    }
    Item item = null;
    if (representation.getItem() != null) {
      if (representation.getItem().getId() != null) {
        item = itemService.getItemById(representation.getItem().getId());
      }
    }

    List<GaugeInspectionReport> gaugeInspectionReports = representation.getGaugeInspectionReports() != null
                                                         ? representation.getGaugeInspectionReports().stream()
                                                             .map(gaugeInspectionReportAssembler::assemble)
                                                             .toList()
                                                         : new ArrayList<>();

    // Build the ProcessedItemInspectionBatch first without inspectionHeats
    ProcessedItemInspectionBatch processedItemInspectionBatch = ProcessedItemInspectionBatch.builder()
        .id(representation.getId())
        .item(item)
        .gaugeInspectionReports(gaugeInspectionReports)
        .inspectionBatchPiecesCount(representation.getInspectionBatchPiecesCount())
        .availableInspectionBatchPiecesCount(representation.getAvailableInspectionBatchPiecesCount())
        .finishedInspectionBatchPiecesCount(representation.getFinishedInspectionBatchPiecesCount())
        .rejectInspectionBatchPiecesCount(representation.getRejectInspectionBatchPiecesCount())
        .reworkPiecesCount(representation.getReworkPiecesCount())
        .dispatchedPiecesCount(representation.getDispatchedPiecesCount())
        .itemStatus(representation.getItemStatus() != null
                    ? ItemStatus.valueOf(representation.getItemStatus())
                    : null)
        .workflowIdentifier(representation.getWorkflowIdentifier())
        .itemWorkflowId(representation.getItemWorkflowId())
        .previousOperationProcessedItemId(representation.getPreviousOperationProcessedItemId())
        .build();

    // Now convert and set inspectionHeats
    if (representation.getInspectionHeats() != null) {
      processedItemInspectionBatch.setInspectionHeats(
          representation.getInspectionHeats().stream()
              .map(heatRepresentation -> inspectionHeatAssembler.assemble(heatRepresentation, processedItemInspectionBatch))
              .collect(Collectors.toList())
      );
    }

    return processedItemInspectionBatch;
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

  public InspectionBatchRepresentation dissemble(InspectionBatch inspectionBatch) {
    return InspectionBatchRepresentation.builder()
        .id(inspectionBatch.getId())
        .inspectionBatchNumber(inspectionBatch.getInspectionBatchNumber())
        .processedItemInspectionBatch(null)
        .inspectionBatchStatus(inspectionBatch.getInspectionBatchStatus() != null
                               ? inspectionBatch.getInspectionBatchStatus().name()
                               : null)
        .startAt(inspectionBatch.getStartAt() != null ? inspectionBatch.getStartAt().toString() : null)
        .endAt(inspectionBatch.getEndAt() != null ? inspectionBatch.getEndAt().toString() : null)
        .tenantId(inspectionBatch.getTenant() != null ? inspectionBatch.getTenant().getId() : null)
        .build();
  }
}

