package com.jangid.forging_process_management_service.service.common;

import com.jangid.forging_process_management_service.dto.HeatInfoDTO;
import com.jangid.forging_process_management_service.entities.dispatch.ProcessedItemDispatchBatch;
import com.jangid.forging_process_management_service.entities.vendor.ProcessedItemVendorDispatchBatch;
import com.jangid.forging_process_management_service.entities.forging.Forge;
import com.jangid.forging_process_management_service.entities.heating.ProcessedItemHeatTreatmentBatch;
import com.jangid.forging_process_management_service.entities.inventory.Heat;
import com.jangid.forging_process_management_service.entities.machining.ProcessedItemMachiningBatch;
import com.jangid.forging_process_management_service.entities.quality.ProcessedItemInspectionBatch;
import com.jangid.forging_process_management_service.entities.workflow.ItemWorkflow;
import com.jangid.forging_process_management_service.entities.workflow.ItemWorkflowStep;
import com.jangid.forging_process_management_service.entities.workflow.WorkflowStep;
import com.jangid.forging_process_management_service.repositories.dispatch.ProcessedItemDispatchBatchRepository;
import com.jangid.forging_process_management_service.repositories.vendor.ProcessedItemVendorDispatchBatchRepository;
import com.jangid.forging_process_management_service.repositories.forging.ForgeRepository;
import com.jangid.forging_process_management_service.repositories.heating.ProcessedItemHeatTreatmentBatchRepository;
import com.jangid.forging_process_management_service.repositories.machining.ProcessedItemMachiningBatchRepository;
import com.jangid.forging_process_management_service.repositories.quality.ProcessedItemInspectionBatchRepository;
import com.jangid.forging_process_management_service.service.workflow.ItemWorkflowService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for tracing heat information throughout the workflow operations.
 * This service helps retrieve heat information from the first operation of a workflow
 * when subsequent operations don't have heat data populated.
 */
@Slf4j
@Service
public class HeatTraceabilityService {

  @Autowired
  private ItemWorkflowService itemWorkflowService;

  @Autowired
  private ForgeRepository forgeRepository;

  @Autowired
  private ProcessedItemHeatTreatmentBatchRepository processedItemHeatTreatmentBatchRepository;

  @Autowired
  private ProcessedItemMachiningBatchRepository processedItemMachiningBatchRepository;

  @Autowired
  private ProcessedItemInspectionBatchRepository processedItemInspectionBatchRepository;

  @Autowired
  private ProcessedItemDispatchBatchRepository processedItemDispatchBatchRepository;

  @Autowired
  private ProcessedItemVendorDispatchBatchRepository processedItemVendorDispatchBatchRepository;

  /**
   * Retrieves heat information for an operation by tracing back to the first operation
   * if the current operation doesn't have heat data.
   *
   * @param itemWorkflowId The workflow ID to trace
   * @return List of HeatInfoDTO containing heat information
   */
  @Transactional(readOnly = true)
  public List<HeatInfoDTO> getHeatInfoForWorkflow(Long itemWorkflowId) {
    if (itemWorkflowId == null) {
      log.debug("itemWorkflowId is null, returning empty heat list");
      return Collections.emptyList();
    }

    try {
      ItemWorkflow workflow = itemWorkflowService.getItemWorkflowById(itemWorkflowId);
      return getHeatsFromFirstOperation(workflow);
    } catch (Exception e) {
      log.error("Error retrieving heat info for workflow {}: {}", itemWorkflowId, e.getMessage());
      return Collections.emptyList();
    }
  }

  /**
   * Gets heat information from the first operation of a workflow.
   * Identifies the first operation type and retrieves heat data accordingly.
   *
   * @param workflow The ItemWorkflow to analyze
   * @return List of HeatInfoDTO from the first operation
   */
  private List<HeatInfoDTO> getHeatsFromFirstOperation(ItemWorkflow workflow) {
    List<ItemWorkflowStep> rootSteps = workflow.getItemWorkflowSteps().stream()
        .filter(step -> step.getParentItemWorkflowStep() == null)
        .toList();

    if (rootSteps.isEmpty()) {
      log.debug("No root steps found in workflow {}", workflow.getId());
      return Collections.emptyList();
    }

    // Find the first root step (by creation order or ID)
    ItemWorkflowStep firstStep = rootSteps.stream()
        .min((s1, s2) -> Long.compare(s1.getId(), s2.getId()))
        .orElse(null);

    if (firstStep.getRelatedEntityIds() == null || firstStep.getRelatedEntityIds().isEmpty()) {
      log.debug("No valid first step with related entity IDs found in workflow {}", workflow.getId());
      return Collections.emptyList();
    }

    return extractHeatsFromOperation(firstStep);
  }

  /**
   * Extracts heat information from a specific operation step.
   * Uses relatedEntityIds which contains ProcessedItem IDs for the operation.
   *
   * @param step The ItemWorkflowStep containing operation details
   * @return List of HeatInfoDTO extracted from the operation
   */
  private List<HeatInfoDTO> extractHeatsFromOperation(ItemWorkflowStep step) {
    WorkflowStep.OperationType operationType = step.getOperationType();
    List<Long> relatedEntityIds = step.getRelatedEntityIds();

    if (relatedEntityIds == null || relatedEntityIds.isEmpty()) {
      log.debug("No related entity IDs found for operation type {} in step {}", operationType, step.getId());
      return Collections.emptyList();
    }

    // Collect heat information from all related entities (ProcessedItems) in this step
    List<HeatInfoDTO> allHeats = new ArrayList<>();
    
    for (Long processedItemId : relatedEntityIds) {
      try {
        List<HeatInfoDTO> heats = switch (operationType) {
          case FORGING -> getHeatsFromForgingByProcessedItemId(processedItemId);
          case HEAT_TREATMENT -> getHeatsFromHeatTreatmentByProcessedItemId(processedItemId);
          case MACHINING -> getHeatsFromMachiningByProcessedItemId(processedItemId);
          case QUALITY -> getHeatsFromInspectionByProcessedItemId(processedItemId);
          case DISPATCH -> getHeatsFromDispatchByProcessedItemId(processedItemId);
          case VENDOR -> getHeatsFromVendorDispatchByProcessedItemId(processedItemId);
          default -> {
            log.debug("Unsupported operation type {} for heat extraction", operationType);
            yield Collections.emptyList();
          }
        };
        allHeats.addAll(heats);
      } catch (Exception e) {
        log.warn("Failed to extract heats from {} operation with processedItemId {}: {}", 
                operationType, processedItemId, e.getMessage());
      }
    }

    // Remove duplicates based on heatId
    return new ArrayList<>(allHeats.stream()
                               .collect(Collectors.toMap(
                                   HeatInfoDTO::getHeatId,
                                   heat -> heat,
                                   (existing, _) -> existing)) // Keep first occurrence
                               .values());
  }

  /**
   * Retrieves heat information from a Forge operation using ProcessedItem ID.
   * CORRECTED: operationReferenceId points to ProcessedItem.id, not Forge.id
   */
  private List<HeatInfoDTO> getHeatsFromForgingByProcessedItemId(Long processedItemId) {
    try {
      // Find Forge by ProcessedItem ID
      Optional<Forge> forgeOpt = forgeRepository.findByProcessedItemIdAndDeletedFalse(processedItemId);
      if (forgeOpt.isPresent() && forgeOpt.get().getForgeHeats() != null) {
        return forgeOpt.get().getForgeHeats().stream()
            .filter(forgeHeat -> forgeHeat.getHeat() != null && !forgeHeat.isDeleted())
            .map(forgeHeat -> convertHeatToDTO(forgeHeat.getHeat()))
            .collect(Collectors.toList());
      }
      return Collections.emptyList();
    } catch (Exception e) {
      log.error("Error extracting heats from forging operation with processedItemId {}: {}", processedItemId, e.getMessage());
      return Collections.emptyList();
    }
  }

  /**
   * Retrieves heat information from a Heat Treatment operation using ProcessedItem ID.
   * CORRECTED: operationReferenceId points to ProcessedItemHeatTreatmentBatch.id
   */
  private List<HeatInfoDTO> getHeatsFromHeatTreatmentByProcessedItemId(Long processedItemHeatTreatmentBatchId) {
    try {
      Optional<ProcessedItemHeatTreatmentBatch> heatTreatmentOpt = 
          processedItemHeatTreatmentBatchRepository.findById(processedItemHeatTreatmentBatchId);
      if (heatTreatmentOpt.isPresent() && heatTreatmentOpt.get().getHeatTreatmentHeats() != null) {
        return heatTreatmentOpt.get().getHeatTreatmentHeats().stream()
            .filter(heatTreatmentHeat -> heatTreatmentHeat.getHeat() != null && !heatTreatmentHeat.isDeleted())
            .map(heatTreatmentHeat -> convertHeatToDTO(heatTreatmentHeat.getHeat()))
            .collect(Collectors.toList());
      }
      return Collections.emptyList();
    } catch (Exception e) {
      log.error("Error extracting heats from heat treatment operation with processedItemHeatTreatmentBatchId {}: {}", processedItemHeatTreatmentBatchId, e.getMessage());
      return Collections.emptyList();
    }
  }

  /**
   * Retrieves heat information from a Machining operation using ProcessedItem ID.
   * CORRECTED: operationReferenceId points to ProcessedItemMachiningBatch.id
   */
  private List<HeatInfoDTO> getHeatsFromMachiningByProcessedItemId(Long processedItemMachiningBatchId) {
    try {
      Optional<ProcessedItemMachiningBatch> machiningOpt = 
          processedItemMachiningBatchRepository.findById(processedItemMachiningBatchId);
      if (machiningOpt.isPresent() && machiningOpt.get().getMachiningHeats() != null) {
        return machiningOpt.get().getMachiningHeats().stream()
            .filter(machiningHeat -> machiningHeat.getHeat() != null && !machiningHeat.isDeleted())
            .map(machiningHeat -> convertHeatToDTO(machiningHeat.getHeat()))
            .collect(Collectors.toList());
      }
      return Collections.emptyList();
    } catch (Exception e) {
      log.error("Error extracting heats from machining operation with processedItemMachiningBatchId {}: {}", processedItemMachiningBatchId, e.getMessage());
      return Collections.emptyList();
    }
  }

  /**
   * Retrieves heat information from an Inspection operation using ProcessedItem ID.
   * CORRECTED: operationReferenceId points to ProcessedItemInspectionBatch.id
   */
  private List<HeatInfoDTO> getHeatsFromInspectionByProcessedItemId(Long processedItemInspectionBatchId) {
    try {
      Optional<ProcessedItemInspectionBatch> inspectionOpt = 
          processedItemInspectionBatchRepository.findById(processedItemInspectionBatchId);
      if (inspectionOpt.isPresent() && inspectionOpt.get().getInspectionHeats() != null) {
        return inspectionOpt.get().getInspectionHeats().stream()
            .filter(inspectionHeat -> inspectionHeat.getHeat() != null && !inspectionHeat.isDeleted())
            .map(inspectionHeat -> convertHeatToDTO(inspectionHeat.getHeat()))
            .collect(Collectors.toList());
      }
      return Collections.emptyList();
    } catch (Exception e) {
      log.error("Error extracting heats from inspection operation with processedItemInspectionBatchId {}: {}", processedItemInspectionBatchId, e.getMessage());
      return Collections.emptyList();
    }
  }

  /**
   * Retrieves heat information from a Dispatch operation using ProcessedItem ID.
   * CORRECTED: operationReferenceId points to ProcessedItemDispatchBatch.id
   */
  private List<HeatInfoDTO> getHeatsFromDispatchByProcessedItemId(Long processedItemDispatchBatchId) {
    try {
      Optional<ProcessedItemDispatchBatch> dispatchOpt = 
          processedItemDispatchBatchRepository.findById(processedItemDispatchBatchId);
      if (dispatchOpt.isPresent() && dispatchOpt.get().getDispatchHeats() != null) {
        return dispatchOpt.get().getDispatchHeats().stream()
            .filter(dispatchHeat -> dispatchHeat.getHeat() != null && !dispatchHeat.isDeleted())
            .map(dispatchHeat -> convertHeatToDTO(dispatchHeat.getHeat()))
            .collect(Collectors.toList());
      }
      return Collections.emptyList();
    } catch (Exception e) {
      log.error("Error extracting heats from dispatch operation with processedItemDispatchBatchId {}: {}", processedItemDispatchBatchId, e.getMessage());
      return Collections.emptyList();
    }
  }

  /**
   * Retrieves heat information from a Vendor Dispatch operation using ProcessedItem ID.
   * CORRECTED: operationReferenceId points to ProcessedItemVendorDispatchBatch.id
   */
  private List<HeatInfoDTO> getHeatsFromVendorDispatchByProcessedItemId(Long processedItemVendorDispatchBatchId) {
    try {
      Optional<ProcessedItemVendorDispatchBatch> vendorDispatchOpt = 
          processedItemVendorDispatchBatchRepository.findById(processedItemVendorDispatchBatchId);
      if (vendorDispatchOpt.isPresent() && vendorDispatchOpt.get().getVendorDispatchHeats() != null) {
        return vendorDispatchOpt.get().getVendorDispatchHeats().stream()
            .filter(vendorDispatchHeat -> vendorDispatchHeat.getHeat() != null && !vendorDispatchHeat.isDeleted())
            .map(vendorDispatchHeat -> convertHeatToDTO(vendorDispatchHeat.getHeat()))
            .collect(Collectors.toList());
      }
      return Collections.emptyList();
    } catch (Exception e) {
      log.error("Error extracting heats from vendor dispatch operation with processedItemVendorDispatchBatchId {}: {}", processedItemVendorDispatchBatchId, e.getMessage());
      return Collections.emptyList();
    }
  }

  /**
   * Converts a Heat entity to HeatInfoDTO.
   */
  private HeatInfoDTO convertHeatToDTO(Heat heat) {
    if (heat == null) {
      return null;
    }
    
    HeatInfoDTO dto = new HeatInfoDTO();
    dto.setHeatId(heat.getId());
    dto.setHeatNumber(heat.getHeatNumber());
    dto.setHeatQuantity(heat.getHeatQuantity());
    dto.setAvailableHeatQuantity(heat.getAvailableHeatQuantity());
    dto.setPiecesCount(heat.getPiecesCount());
    dto.setAvailablePiecesCount(heat.getAvailablePiecesCount());
    return dto;
  }


  /**
   * Helper method specifically for ProcessedItemHeatTreatmentBatch to get first operation heats
   * when heatTreatmentHeats is null.
   */
  @Transactional(readOnly = true)
  public List<HeatInfoDTO> getFirstOperationHeatsForHeatTreatment(Long itemWorkflowId) {
    return getHeatInfoForWorkflow(itemWorkflowId);
  }

  /**
   * Helper method specifically for ProcessedItemMachiningBatch to get first operation heats
   * when machiningHeats is null.
   */
  @Transactional(readOnly = true)
  public List<HeatInfoDTO> getFirstOperationHeatsForMachining(Long itemWorkflowId) {
    return getHeatInfoForWorkflow(itemWorkflowId);
  }

  /**
   * Helper method specifically for ProcessedItemInspectionBatch to get first operation heats
   * when inspectionHeats is null.
   */
  @Transactional(readOnly = true)
  public List<HeatInfoDTO> getFirstOperationHeatsForInspection(Long itemWorkflowId) {
    return getHeatInfoForWorkflow(itemWorkflowId);
  }

  /**
   * Helper method specifically for ProcessedItemDispatchBatch to get first operation heats
   * when dispatchHeats is null.
   */
  @Transactional(readOnly = true)
  public List<HeatInfoDTO> getFirstOperationHeatsForDispatch(Long itemWorkflowId) {
    return getHeatInfoForWorkflow(itemWorkflowId);
  }

  /**
   * Helper method specifically for ProcessedItemVendorDispatchBatch to get first operation heats
   * when vendorDispatchHeats is null.
   */
  @Transactional(readOnly = true)
  public List<HeatInfoDTO> getFirstOperationHeatsForVendorDispatch(Long itemWorkflowId) {
    return getHeatInfoForWorkflow(itemWorkflowId);
  }
}
