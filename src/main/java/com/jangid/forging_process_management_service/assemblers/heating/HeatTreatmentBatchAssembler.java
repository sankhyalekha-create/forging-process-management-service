package com.jangid.forging_process_management_service.assemblers.heating;

import com.jangid.forging_process_management_service.entities.heating.HeatTreatmentBatch;
import com.jangid.forging_process_management_service.entities.heating.HeatTreatmentHeat;
import com.jangid.forging_process_management_service.entities.heating.ProcessedItemHeatTreatmentBatch;
import com.jangid.forging_process_management_service.entitiesRepresentation.heating.HeatTreatmentBatchRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.heating.HeatTreatmentHeatRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.heating.ProcessedItemHeatTreatmentBatchRepresentation;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class HeatTreatmentBatchAssembler {

  @Autowired
  private ProcessedItemHeatTreatmentBatchAssembler processedItemHeatTreatmentBatchAssembler;
  @Autowired
  private FurnaceAssembler furnaceAssembler;
  @Autowired
  private HeatTreatmentHeatAssembler heatTreatmentHeatAssembler;

  // Assemble method for creating a HeatTreatmentBatch from representation
  public HeatTreatmentBatch createAssemble(HeatTreatmentBatchRepresentation heatTreatmentBatchRepresentation) {
    HeatTreatmentBatch heatTreatmentBatch = assemble(heatTreatmentBatchRepresentation);
    
    List<ProcessedItemHeatTreatmentBatch> processedItemHeatTreatmentBatches = getCreateProcessedItemHeatTreatmentBatches(heatTreatmentBatchRepresentation.getProcessedItemHeatTreatmentBatches());
    heatTreatmentBatch.setProcessedItemHeatTreatmentBatches(processedItemHeatTreatmentBatches);
    
    // Handle heat treatment heats if provided
    if (heatTreatmentBatchRepresentation.getHeatTreatmentHeats() != null && !heatTreatmentBatchRepresentation.getHeatTreatmentHeats().isEmpty()) {
      List<HeatTreatmentHeat> heatTreatmentHeats = getCreateHeatTreatmentHeats(heatTreatmentBatchRepresentation.getHeatTreatmentHeats(), heatTreatmentBatch);
      heatTreatmentBatch.setHeatTreatmentHeats(heatTreatmentHeats);
    }
    
    heatTreatmentBatch.calculateTotalWeight();
    heatTreatmentBatch.setCreatedAt(LocalDateTime.now());
    heatTreatmentBatch.setHeatTreatmentBatchStatus(HeatTreatmentBatch.HeatTreatmentBatchStatus.IDLE);
    return heatTreatmentBatch;
  }

  // Assemble method for converting HeatTreatmentBatchRepresentation to HeatTreatmentBatch entity
  public HeatTreatmentBatch assemble(HeatTreatmentBatchRepresentation heatTreatmentBatchRepresentation) {
    HeatTreatmentBatch heatTreatmentBatch = HeatTreatmentBatch.builder()
        .heatTreatmentBatchNumber(heatTreatmentBatchRepresentation.getHeatTreatmentBatchNumber())
        .labTestingReport(heatTreatmentBatchRepresentation.getLabTestingReport())
        .labTestingStatus(heatTreatmentBatchRepresentation.getLabTestingStatus())
        .furnace(furnaceAssembler.assemble(heatTreatmentBatchRepresentation.getFurnace()))
//        .processedItemHeatTreatmentBatches(getProcessedItemHeatTreatmentBatches(heatTreatmentBatchRepresentation.getProcessedItemHeatTreatmentBatches()))
        .applyAt(parseDate(heatTreatmentBatchRepresentation.getApplyAt()))
        .startAt(parseDate(heatTreatmentBatchRepresentation.getStartAt()))
        .endAt(parseDate(heatTreatmentBatchRepresentation.getEndAt()))
        .build();

    // Calculate total weight after assembly
    heatTreatmentBatch.calculateTotalWeight();

    return heatTreatmentBatch;
  }

  // Convert HeatTreatmentBatch entity to HeatTreatmentBatchRepresentation
  public HeatTreatmentBatchRepresentation dissemble(HeatTreatmentBatch heatTreatmentBatch) {
    return HeatTreatmentBatchRepresentation.builder()
        .id(heatTreatmentBatch.getId())
        .heatTreatmentBatchNumber(heatTreatmentBatch.getHeatTreatmentBatchNumber())
        .furnace(furnaceAssembler.dissemble(heatTreatmentBatch.getFurnace()))
        .totalWeight(String.valueOf(heatTreatmentBatch.getTotalWeight()))
        .heatTreatmentBatchStatus(heatTreatmentBatch.getHeatTreatmentBatchStatus().name())
        .labTestingReport(heatTreatmentBatch.getLabTestingReport())
        .labTestingStatus(heatTreatmentBatch.getLabTestingStatus())
        .processedItemHeatTreatmentBatches(getProcessedItemHeatTreatmentBatchesRepresentation(heatTreatmentBatch.getProcessedItemHeatTreatmentBatches()))
        .heatTreatmentHeats(getHeatTreatmentHeatsRepresentation(heatTreatmentBatch.getHeatTreatmentHeats()))
        .applyAt(heatTreatmentBatch.getApplyAt()!=null?formatDate(heatTreatmentBatch.getApplyAt()): null)
        .startAt(heatTreatmentBatch.getStartAt()!=null?formatDate(heatTreatmentBatch.getStartAt()): null)
        .endAt(heatTreatmentBatch.getEndAt()!=null?formatDate(heatTreatmentBatch.getEndAt()):null)
        .createdAt(heatTreatmentBatch.getCreatedAt()!=null?formatDate(heatTreatmentBatch.getCreatedAt()):null)
        .updatedAt(heatTreatmentBatch.getUpdatedAt()!=null?formatDate(heatTreatmentBatch.getUpdatedAt()):null)
        .deletedAt(heatTreatmentBatch.getDeletedAt()!=null?formatDate(heatTreatmentBatch.getDeletedAt()):null)
        .build();
  }

  // Helper method to convert processed item heat treatment batches from representation
  private List<ProcessedItemHeatTreatmentBatch> getProcessedItemHeatTreatmentBatches(List<ProcessedItemHeatTreatmentBatchRepresentation> processedItemHeatTreatmentBatchRepresentations) {
    return processedItemHeatTreatmentBatchRepresentations.stream()
        .map(processedItemHeatTreatmentBatchAssembler::assemble)
        .collect(Collectors.toList());
  }

  private List<ProcessedItemHeatTreatmentBatch> getCreateProcessedItemHeatTreatmentBatches(List<ProcessedItemHeatTreatmentBatchRepresentation> processedItemHeatTreatmentBatchRepresentations) {
    return processedItemHeatTreatmentBatchRepresentations.stream()
        .map(processedItemHeatTreatmentBatchAssembler::createAssemble)
        .collect(Collectors.toList());
  }

  // Helper method to convert processed item heat treatment batches to representation
  private List<ProcessedItemHeatTreatmentBatchRepresentation> getProcessedItemHeatTreatmentBatchesRepresentation(List<ProcessedItemHeatTreatmentBatch> processedItemHeatTreatmentBatches) {
    return processedItemHeatTreatmentBatches.stream()
        .map(processedItemHeatTreatmentBatchAssembler::dissemble)
        .collect(Collectors.toList());
  }

  // Helper method to convert heat treatment heats from representation for creation
  private List<HeatTreatmentHeat> getCreateHeatTreatmentHeats(List<HeatTreatmentHeatRepresentation> heatTreatmentHeatRepresentations, HeatTreatmentBatch heatTreatmentBatch) {
    return heatTreatmentHeatRepresentations.stream()
        .map(representation -> {
          HeatTreatmentHeat heatTreatmentHeat = heatTreatmentHeatAssembler.createAssemble(representation);
          heatTreatmentHeat.setHeatTreatmentBatch(heatTreatmentBatch);
          return heatTreatmentHeat;
        })
        .collect(Collectors.toList());
  }

  // Helper method to convert heat treatment heats to representation
  private List<HeatTreatmentHeatRepresentation> getHeatTreatmentHeatsRepresentation(List<HeatTreatmentHeat> heatTreatmentHeats) {
    if (heatTreatmentHeats == null || heatTreatmentHeats.isEmpty()) {
      return Collections.emptyList();
    }
    return heatTreatmentHeats.stream()
        .map(heatTreatmentHeatAssembler::dissemble)
        .collect(Collectors.toList());
  }

  // Helper method to parse date strings (could use a custom formatter if needed)
  private LocalDateTime parseDate(String dateString) {
    return dateString != null ? LocalDateTime.parse(dateString) : null;
  }

  // Helper method to format dates for representation
  private String formatDate(LocalDateTime dateTime) {
    return dateTime != null ? dateTime.toString() : null;
  }
}
