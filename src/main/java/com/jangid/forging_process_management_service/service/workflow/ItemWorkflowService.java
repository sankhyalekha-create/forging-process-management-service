package com.jangid.forging_process_management_service.service.workflow;

import com.jangid.forging_process_management_service.dto.ItemWorkflowTrackingResultDTO;
import com.jangid.forging_process_management_service.dto.HeatInfoDTO;
import com.jangid.forging_process_management_service.entities.dispatch.DispatchBatch;
import com.jangid.forging_process_management_service.entities.forging.Forge;
import com.jangid.forging_process_management_service.entities.heating.HeatTreatmentBatch;
import com.jangid.forging_process_management_service.entities.machining.MachiningBatch;
import com.jangid.forging_process_management_service.entities.product.Item;
import com.jangid.forging_process_management_service.entities.quality.InspectionBatch;
import com.jangid.forging_process_management_service.entities.vendor.VendorDispatchBatch;
import com.jangid.forging_process_management_service.entities.workflow.ItemWorkflow;
import com.jangid.forging_process_management_service.entities.workflow.ItemWorkflowStep;
import com.jangid.forging_process_management_service.entities.workflow.WorkflowStep;
import com.jangid.forging_process_management_service.entities.workflow.WorkflowTemplate;
import com.jangid.forging_process_management_service.entities.heating.ProcessedItemHeatTreatmentBatch;
import com.jangid.forging_process_management_service.entities.machining.ProcessedItemMachiningBatch;
import com.jangid.forging_process_management_service.entities.quality.ProcessedItemInspectionBatch;
import com.jangid.forging_process_management_service.entities.dispatch.ProcessedItemDispatchBatch;
import com.jangid.forging_process_management_service.entities.inventory.Heat;
import com.jangid.forging_process_management_service.entities.forging.ForgeHeat;
import com.jangid.forging_process_management_service.entities.heating.HeatTreatmentHeat;
import com.jangid.forging_process_management_service.entities.machining.MachiningHeat;
import com.jangid.forging_process_management_service.entities.quality.InspectionHeat;
import com.jangid.forging_process_management_service.entities.dispatch.DispatchHeat;
import com.jangid.forging_process_management_service.repositories.workflow.ItemWorkflowRepository;
import com.jangid.forging_process_management_service.entitiesRepresentation.workflow.ItemWorkflowRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.workflow.ItemWorkflowStepRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.workflow.ItemWorkflowListRepresentation;
import com.jangid.forging_process_management_service.assemblers.workflow.ItemWorkflowAssembler;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

// Additional imports for tracking functionality
import com.jangid.forging_process_management_service.entitiesRepresentation.forging.ForgeRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.heating.HeatTreatmentBatchRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.machining.MachiningBatchRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.quality.InspectionBatchRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.vendor.VendorDispatchBatchRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.dispatch.DispatchBatchRepresentation;

import com.jangid.forging_process_management_service.repositories.forging.ForgeRepository;
import com.jangid.forging_process_management_service.repositories.heating.HeatTreatmentBatchRepository;
import com.jangid.forging_process_management_service.repositories.machining.MachiningBatchRepository;
import com.jangid.forging_process_management_service.repositories.quality.InspectionBatchRepository;
import com.jangid.forging_process_management_service.repositories.vendor.VendorDispatchBatchRepository;
import com.jangid.forging_process_management_service.repositories.dispatch.DispatchBatchRepository;
import com.jangid.forging_process_management_service.repositories.heating.ProcessedItemHeatTreatmentBatchRepository;
import com.jangid.forging_process_management_service.repositories.machining.ProcessedItemMachiningBatchRepository;
import com.jangid.forging_process_management_service.repositories.quality.ProcessedItemInspectionBatchRepository;
import com.jangid.forging_process_management_service.repositories.dispatch.ProcessedItemDispatchBatchRepository;

import com.jangid.forging_process_management_service.assemblers.forging.ForgeAssembler;
import com.jangid.forging_process_management_service.assemblers.heating.HeatTreatmentBatchAssembler;
import com.jangid.forging_process_management_service.assemblers.machining.MachiningBatchAssembler;
import com.jangid.forging_process_management_service.assemblers.quality.InspectionBatchAssembler;
import com.jangid.forging_process_management_service.assemblers.vendor.VendorDispatchBatchAssembler;
import com.jangid.forging_process_management_service.assemblers.dispatch.DispatchBatchAssembler;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.jangid.forging_process_management_service.dto.workflow.OperationOutcomeData;
import com.jangid.forging_process_management_service.service.heating.ProcessedItemHeatTreatmentBatchService;
import com.jangid.forging_process_management_service.service.machining.ProcessedItemMachiningBatchService;
import com.jangid.forging_process_management_service.service.quality.ProcessedItemInspectionBatchService;
import com.jangid.forging_process_management_service.service.dispatch.ProcessedItemDispatchBatchService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jangid.forging_process_management_service.service.vendor.ProcessedItemVendorDispatchBatchService;

import java.time.LocalDateTime;

@Slf4j
@Service
public class ItemWorkflowService {

  @Autowired
  private ItemWorkflowRepository itemWorkflowRepository;

  @Autowired
  private WorkflowTemplateService workflowTemplateService;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private ItemWorkflowAssembler itemWorkflowAssembler;

  @Autowired
  private ProcessedItemHeatTreatmentBatchService processedItemHeatTreatmentBatchService;

  @Autowired
  private ProcessedItemMachiningBatchService processedItemMachiningBatchService;

  @Autowired
  private ProcessedItemInspectionBatchService processedItemInspectionBatchService;

  @Autowired
  private ProcessedItemDispatchBatchService processedItemDispatchBatchService;

  @Autowired
  private ProcessedItemVendorDispatchBatchService processedItemVendorDispatchBatchService;

  // Additional dependencies for tracking functionality
  @Autowired
  private ForgeRepository forgeRepository;

  @Autowired
  private HeatTreatmentBatchRepository heatTreatmentBatchRepository;

  @Autowired
  private MachiningBatchRepository machiningBatchRepository;

  @Autowired
  private InspectionBatchRepository inspectionBatchRepository;

  @Autowired
  private VendorDispatchBatchRepository vendorDispatchBatchRepository;

  @Autowired
  private DispatchBatchRepository dispatchBatchRepository;

  @Autowired
  private ForgeAssembler forgeAssembler;

  @Autowired
  private HeatTreatmentBatchAssembler heatTreatmentBatchAssembler;

  @Autowired
  private MachiningBatchAssembler machiningBatchAssembler;

  @Autowired
  private InspectionBatchAssembler inspectionBatchAssembler;

  @Autowired
  private VendorDispatchBatchAssembler vendorDispatchBatchAssembler;

  @Autowired
  private DispatchBatchAssembler dispatchBatchAssembler;

  @Autowired
  private ProcessedItemHeatTreatmentBatchRepository processedItemHeatTreatmentBatchRepository;

  @Autowired
  private ProcessedItemMachiningBatchRepository processedItemMachiningBatchRepository;

  @Autowired
  private ProcessedItemInspectionBatchRepository processedItemInspectionBatchRepository;

  @Autowired
  private ProcessedItemDispatchBatchRepository processedItemDispatchBatchRepository;


  /**
   * Creates a batch-level workflow (for specific operation batches)
   */
  @Transactional
  public ItemWorkflow createItemWorkflow(Item item, Long workflowTemplateId, String workflowIdentifier) {
    // Check if this batch already has a workflow
    List<ItemWorkflow> existingWorkflows = itemWorkflowRepository.findByItemIdAndDeletedFalse(item.getId());
    Optional<ItemWorkflow> existingBatchWorkflow = existingWorkflows.stream()
        .filter(workflow -> workflowIdentifier != null &&
                            workflowIdentifier.equals(workflow.getWorkflowIdentifier()))
        .findFirst();

    if (existingBatchWorkflow.isPresent()) {
      throw new RuntimeException("Batch already has an assigned workflow: " + workflowIdentifier);
    }

    // Workflow template ID is now mandatory
    if (workflowTemplateId == null) {
      throw new RuntimeException("Workflow template ID is required. Please create workflow templates first and select one during item creation.");
    }

    WorkflowTemplate template = workflowTemplateService.getWorkflowTemplateById(workflowTemplateId);

    // Validate that template belongs to the same tenant as the item
    if (template.getTenant().getId() != item.getTenant().getId()) {
      throw new RuntimeException("Workflow template does not belong to the same tenant as the item");
    }

    ItemWorkflow itemWorkflow = ItemWorkflow.builder()
        .item(item)
        .workflowTemplate(template)
        .workflowStatus(ItemWorkflow.WorkflowStatus.NOT_STARTED)
        .workflowIdentifier(workflowIdentifier)
        .build();

    // Create workflow step tracking entries with proper parent-child relationships
    Map<Long, ItemWorkflowStep> workflowStepToItemStepMap = new HashMap<>();

    // First pass: Create all ItemWorkflowSteps
    for (WorkflowStep step : template.getWorkflowSteps()) {
      ItemWorkflowStep itemStep = ItemWorkflowStep.builder()
          .itemWorkflow(itemWorkflow)
          .workflowStep(step)
          .operationType(step.getOperationType())
          .stepStatus(ItemWorkflowStep.StepStatus.PENDING)
          .build();
      itemWorkflow.getItemWorkflowSteps().add(itemStep);
      workflowStepToItemStepMap.put(step.getId(), itemStep);
    }

    // Second pass: Establish parent-child relationships between ItemWorkflowSteps
    for (WorkflowStep step : template.getWorkflowSteps()) {
      if (step.getParentStep() != null) {
        ItemWorkflowStep childItemStep = workflowStepToItemStepMap.get(step.getId());
        ItemWorkflowStep parentItemStep = workflowStepToItemStepMap.get(step.getParentStep().getId());

        if (childItemStep != null && parentItemStep != null) {
          childItemStep.setParentItemWorkflowStep(parentItemStep);
          parentItemStep.addChildItemWorkflowStep(childItemStep);
        }
      }
    }

    // Establish bidirectional relationship
    item.addWorkflow(itemWorkflow);

    // Save the workflow first to get IDs for all ItemWorkflowSteps
    ItemWorkflow savedWorkflow = itemWorkflowRepository.save(itemWorkflow);

    // Now update the parent-child relationships using the saved entities with IDs
    Map<Long, ItemWorkflowStep> savedItemStepMap = new HashMap<>();
    for (ItemWorkflowStep step : savedWorkflow.getItemWorkflowSteps()) {
      savedItemStepMap.put(step.getWorkflowStep().getId(), step);
    }

    // Re-establish parent-child relationships on saved entities
    boolean relationshipsUpdated = false;
    for (WorkflowStep step : template.getWorkflowSteps()) {
      if (step.getParentStep() != null) {
        ItemWorkflowStep childItemStep = savedItemStepMap.get(step.getId());
        ItemWorkflowStep parentItemStep = savedItemStepMap.get(step.getParentStep().getId());

        if (childItemStep != null && parentItemStep != null &&
            childItemStep.getParentItemWorkflowStep() == null) {
          childItemStep.setParentItemWorkflowStep(parentItemStep);
          relationshipsUpdated = true;
        }
      }
    }

    // Save again if relationships were updated
    if (relationshipsUpdated) {
      savedWorkflow = itemWorkflowRepository.save(savedWorkflow);
    }

    return savedWorkflow;
  }

  /**
   * Checks if a workflow identifier already exists for a specific tenant
   *
   * @param tenantId           The tenant ID to check within
   * @param workflowIdentifier The workflow identifier to check for uniqueness
   * @return true if the workflow identifier already exists, false otherwise
   */
  public boolean isWorkflowIdentifierExistsForTenant(Long tenantId, String workflowIdentifier) {
    try {
      List<ItemWorkflow> existingWorkflows = itemWorkflowRepository.findByWorkflowIdentifierAndDeletedFalse(workflowIdentifier);

      // Check if any of the found workflows belong to the specified tenant
      boolean exists = existingWorkflows.stream()
          .anyMatch(workflow -> workflow.getItem().getTenant().getId() == tenantId);

      if (exists) {
        log.info("Workflow identifier '{}' already exists for tenant {}", workflowIdentifier, tenantId);
      }

      return exists;
    } catch (Exception e) {
      log.error("Error checking workflow identifier existence for tenant {}: {}", tenantId, e.getMessage());
      return false; // Return false on error to allow the operation (fail-safe)
    }
  }


  public ItemWorkflow getItemWorkflowById(Long itemWorkflowId) {
    Optional<ItemWorkflow> itemWorkflowOptional = itemWorkflowRepository.findByIdWithEagerLoading(itemWorkflowId);

    if (itemWorkflowOptional.isPresent() && !itemWorkflowOptional.get().getDeleted()) {
      return itemWorkflowOptional.get();
    }

    throw new RuntimeException("No itemWorkflow found for id: " + itemWorkflowId);
  }

  /**
   * Gets a specific workflowIdentifier workflow
   */
  public ItemWorkflow getItemWorkflowByWorkflowIdentifier(Long itemId, String workflowIdentifier) {
    List<ItemWorkflow> workflows = itemWorkflowRepository.findByItemIdAndDeletedFalse(itemId);
    return workflows.stream()
        .filter(workflow -> workflowIdentifier != null &&
                            workflowIdentifier.equals(workflow.getWorkflowIdentifier()))
        .findFirst()
        .orElseThrow(() ->
                         new RuntimeException("No workflow found for itemWorkflow Name: " + workflowIdentifier));
  }

  /**
   * Gets all workflows for an item
   */
  public List<ItemWorkflow> getAllItemWorkflowsForItem(Long itemId) {
    return itemWorkflowRepository.findByItemIdAndDeletedFalse(itemId);
  }

  /**
   * Checks if an operation is the first step in a workflow template
   * For tree-based workflows, checks if the operation is a root step
   */
  public boolean isFirstOperationInWorkflow(Long workflowTemplateId, WorkflowStep.OperationType operationType) {
    WorkflowTemplate template = workflowTemplateService.getWorkflowTemplateById(workflowTemplateId);

    // For tree-based workflows, check if the operation is among the root steps
    return template.getRootSteps().stream()
        .anyMatch(step -> step.getOperationType() == operationType);
  }

  /**
   * Gets the initial item-level workflow without batch identifier
   * Used during first-time forging operation to update with batch identifier
   */
  public ItemWorkflow getInitialItemWorkflow(Long itemId) {
    List<ItemWorkflow> workflows = itemWorkflowRepository.findByItemIdAndDeletedFalse(itemId);
    return workflows.stream()
        .filter(workflow -> workflow.getWorkflowIdentifier() == null)
        .findFirst()
        .orElse(null);
  }



  @Transactional
  public void updateWorkflowStepForOperation(Long itemWorkflowId,
                                             Long parentEntityId,
                                             WorkflowStep.OperationType operationType,
                                             OperationOutcomeData outcomeData) {
    try {
      ItemWorkflow workflow = getItemWorkflowById(itemWorkflowId);
      ItemWorkflowStep operationStep = findItemWorkflowStepByParentEntityId(itemWorkflowId, parentEntityId, operationType);

      if (operationStep == null) {
        log.warn("No workflow step found for operation {} on workflow {}", operationType, itemWorkflowId);
        return;
      }

      // Start the step if it's still pending
      if (operationStep.getStepStatus() == ItemWorkflowStep.StepStatus.PENDING) {
        operationStep.startStep();
        log.info("Started workflow step {} for workflow {}", operationType, itemWorkflowId);
      }

      // Update with complete outcome data (this takes precedence over incremental calculation)
      operationStep.setOperationOutcomeData(objectMapper.writeValueAsString(outcomeData));

      // Calculate total pieces based on operation type - handle both forging and batch operations
      int totalInitialPieces = 0;
      int totalAvailablePieces = 0;

      if (operationType == WorkflowStep.OperationType.FORGING) {
        // For forging operations, get data from forgingData
        if (outcomeData.getForgingData() != null) {
          totalInitialPieces = outcomeData.getForgingData().getInitialPiecesCount();
          totalAvailablePieces = outcomeData.getForgingData().getPiecesAvailableForNext();
        }
      } else {
        // For batch operations (heat treatment, machining, etc.), sum up all batch data
        if (outcomeData.getBatchData() != null && !outcomeData.getBatchData().isEmpty()) {
          totalInitialPieces = outcomeData.getBatchData().stream()
              .mapToInt(OperationOutcomeData.BatchOutcome::getInitialPiecesCount)
              .sum();
          totalAvailablePieces = outcomeData.getBatchData().stream()
              .mapToInt(OperationOutcomeData.BatchOutcome::getPiecesAvailableForNext)
              .sum();
        }
      }

      operationStep.setInitialPiecesCount(totalInitialPieces);
      operationStep.setPiecesAvailableForNext(totalAvailablePieces);

      itemWorkflowRepository.save(workflow);
      log.info("Updated workflow step {} for workflow {} with data: {} total initial pieces, {} total available pieces",
               operationType, itemWorkflowId, totalInitialPieces, totalAvailablePieces);

    } catch (Exception e) {
      log.error("Error updating workflow step for operation {} on workflow {}: {}",
                operationType, itemWorkflowId, e.getMessage());
      // Re-throw the exception since this is now a consolidated critical operation
      throw new RuntimeException("Failed to update workflow step for operation: " + e.getMessage(), e);
    }
  }


  @Transactional
  public void updateWorkflowStepForForgingOperation(Long itemWorkflowId,
                                             Long entityId,
                                             OperationOutcomeData outcomeData) {
    try {
      ItemWorkflow workflow = getItemWorkflowById(itemWorkflowId);
      ItemWorkflowStep operationStep = findItemWorkflowStepByRelatedEntityId(itemWorkflowId, entityId, WorkflowStep.OperationType.FORGING);

      if (operationStep == null) {
        log.warn("No workflow step found for FORGING operation on workflow {}", itemWorkflowId);
        return;
      }

      if (operationStep.getStepStatus() == ItemWorkflowStep.StepStatus.PENDING) {
        operationStep.startStep();
        log.info("Started FORGING workflow step for workflow {}", itemWorkflowId);
      }

      operationStep.setOperationOutcomeData(objectMapper.writeValueAsString(outcomeData));

      int totalInitialPieces = 0;
      int totalAvailablePieces = 0;

      if (outcomeData.getForgingData() != null) {
        totalInitialPieces = outcomeData.getForgingData().getInitialPiecesCount();
        totalAvailablePieces = outcomeData.getForgingData().getPiecesAvailableForNext();
      }

      operationStep.setInitialPiecesCount(totalInitialPieces);
      operationStep.setPiecesAvailableForNext(totalAvailablePieces);

      itemWorkflowRepository.save(workflow);
      log.info("Updated FORGING workflow step: {} for workflow with data: {} total initial pieces, {} total available pieces", itemWorkflowId, totalInitialPieces, totalAvailablePieces);

    } catch (Exception e) {
      log.error("Error updating workflow step for operation FORGING on workflow {}: {}", itemWorkflowId, e.getMessage());
      throw new RuntimeException("Failed to update workflow step for FORGING operation: " + e.getMessage(), e);
    }
  }

  /**
   * Gets active workflows for an item where next operations can be performed
   * Returns workflows where current operations are IN_PROGRESS or COMPLETED
   * and next operations are PENDING or IN_PROGRESS
   */
  public List<ItemWorkflowRepresentation> getActiveWorkflowsForItem(Long itemId, Long tenantId) {
    try {
      List<ItemWorkflow> workflows = itemWorkflowRepository.findByItemIdAndDeletedFalse(itemId);

      // Validate that item belongs to the tenant
      if (!workflows.isEmpty()) {
        ItemWorkflow firstWorkflow = workflows.get(0);
        if (firstWorkflow.getItem().getTenant().getId() != tenantId) {
          throw new RuntimeException("Item does not belong to the specified tenant");
        }
      }

      return workflows.stream()
          .filter(workflow -> workflow.getWorkflowStatus() == ItemWorkflow.WorkflowStatus.IN_PROGRESS)
          .filter(workflow -> workflow.getWorkflowIdentifier() != null) // Only batch-level workflows
          .map(itemWorkflowAssembler::dissembleActiveWorkflow)
          .collect(Collectors.toList());

    } catch (Exception e) {
      log.error("Error fetching active workflows for item {} in tenant {}: {}", itemId, tenantId, e.getMessage());
      throw new RuntimeException("Failed to fetch active workflows: " + e.getMessage(), e);
    }
  }


  /**
   * Utility method to get pieces available for next operation from a workflow step
   *
   * @param itemWorkflowId The workflow ID
   * @param operationType  The operation type to get pieces from
   * @return The number of pieces available for next operation, or 0 if not found/error
   */
  public int getPiecesAvailableForNextFromOperation(Long itemWorkflowId, WorkflowStep.OperationType operationType, long relatedEntityId) {
    try {
      ItemWorkflowStep workflowStep = findItemWorkflowStepByRelatedEntityId(itemWorkflowId, relatedEntityId, operationType);

      if (workflowStep == null) {
        log.warn("No workflow step found for operation {} in workflow {}", operationType, itemWorkflowId);
        return 0;
      }

      if (workflowStep.getOperationOutcomeData() == null || workflowStep.getOperationOutcomeData().trim().isEmpty()) {
        log.warn("No operation outcome data found for operation {} in workflow {}", operationType, itemWorkflowId);
        return 0;
      }

      OperationOutcomeData outcomeData = objectMapper.readValue(workflowStep.getOperationOutcomeData(), OperationOutcomeData.class);

      // For FORGING operation, get pieces from forgingData
      if (operationType == WorkflowStep.OperationType.FORGING) {
        if (outcomeData.getForgingData() != null) {
          int piecesAvailable = outcomeData.getForgingData().getPiecesAvailableForNext();
          log.info("Fetched {} pieces available for next from {} operation in workflow {}",
                   piecesAvailable, operationType, itemWorkflowId);
          return piecesAvailable;
        } else {
          log.warn("No forging data found in operation outcome data for workflow {}", itemWorkflowId);
          return 0;
        }
      }

      // For other operations, you can extend this method as needed
      // For now, returning 0 for non-FORGING operations
      log.warn("getPiecesAvailableForNextFromOperation not implemented for operation type: {}", operationType);
      return 0;

    } catch (Exception e) {
      log.error("Failed to get pieces available for next from operation {} in workflow {}: {}",
                operationType, itemWorkflowId, e.getMessage());
      return 0;
    }
  }

  public int getAvailablePiecesFromSpecificPreviousOperationOfItemWorkflowStep(ItemWorkflowStep previousOperationStep, Long specificOperationId) {
    try {
      if (previousOperationStep.getOperationOutcomeData() == null || previousOperationStep.getOperationOutcomeData().trim().isEmpty()) {
        log.warn("No operation outcome data found in previous operation {}",
                 previousOperationStep.getOperationType());
        return 0;
      }

      OperationOutcomeData outcomeData = objectMapper.readValue(previousOperationStep.getOperationOutcomeData(), OperationOutcomeData.class);

      // Handle different operation types
      if (previousOperationStep.getOperationType() == WorkflowStep.OperationType.FORGING) {
        if (outcomeData.getForgingData() != null) {
          // For forging operations, if specificOperationId is provided, check if it matches
          if (specificOperationId != null && !specificOperationId.equals(outcomeData.getForgingData().getId())) {
            log.warn("Specific operation ID {} does not match forging data ID {} in workflow {}",
                     specificOperationId, outcomeData.getForgingData().getId(), previousOperationStep.getItemWorkflow().getId());
            return 0;
          }

          int piecesAvailable = outcomeData.getForgingData().getPiecesAvailableForNext();
          log.info("Fetched {} pieces available from forging operation {} in workflow {}",
                   piecesAvailable, outcomeData.getForgingData().getId(), previousOperationStep.getItemWorkflow().getId());
          return piecesAvailable;
        }
      } else {
        // For batch operations (heat treatment, machining, etc.)
        if (outcomeData.getBatchData() != null && !outcomeData.getBatchData().isEmpty()) {
          if (specificOperationId != null) {
            // Find specific batch by ID
            return outcomeData.getBatchData().stream()
                .filter(batch -> specificOperationId.equals(batch.getId()))
                .mapToInt(OperationOutcomeData.BatchOutcome::getPiecesAvailableForNext)
                .findFirst()
                .orElse(0);
          } else {
            // Return total pieces from all batches
            int totalPieces = outcomeData.getBatchData().stream()
                .mapToInt(OperationOutcomeData.BatchOutcome::getPiecesAvailableForNext)
                .sum();
            log.info("Fetched {} total pieces available from {} batch operations in workflow {}",
                     totalPieces, outcomeData.getBatchData().size(), previousOperationStep.getItemWorkflow().getId());
            return totalPieces;
          }
        }
      }

      log.warn("No suitable operation data found for operation type {} in workflow {}",
               previousOperationStep.getOperationType(), previousOperationStep.getItemWorkflow().getId());
      return 0;

    } catch (Exception e) {
      log.error("Error getting available pieces from specific previous operation for workflow {}: {}", previousOperationStep.getItemWorkflow().getId(), e.getMessage());
      return 0;
    }
  }


  /**
   * Checks if all batches in ALL next operations (children) after the specified operation are marked as deleted
   * This method is designed for tree-based workflows where a single operation can have multiple child operations
   * It finds all child steps and ensures all batches in each child are deleted before allowing deletion
   *
   * @param itemWorkflowId       The workflow ID containing the operation step
   * @param entityId             The entity ID to find the specific current operation step
   * @param currentOperationType The current operation type to find next operations for
   * @return true if all batches in ALL next operations are deleted, false otherwise
   */
  public boolean areAllNextOperationBatchesDeleted(Long itemWorkflowId, Long entityId, WorkflowStep.OperationType currentOperationType) {
    try {
      if (itemWorkflowId == null) {
        log.warn("itemWorkflowId is null, cannot check next operation batches");
        return true; // Allow deletion if no workflow
      }

      // Find the current operation step using the specific entity ID
      ItemWorkflowStep currentStep = findItemWorkflowStepByRelatedEntityId(itemWorkflowId, entityId, currentOperationType);
      
      if (currentStep == null) {
        log.warn("Current operation step {} not found in workflow {}", currentOperationType, itemWorkflowId);
        return true; // Allow deletion if current step not found
      }

      // Get all child ItemWorkflowSteps for tree-based workflows
      List<ItemWorkflowStep> childSteps = currentStep.getChildItemWorkflowSteps();
      
      if (childSteps == null || childSteps.isEmpty()) {
        log.info("No child operation steps found after {} in workflow {}, allowing deletion",
                 currentOperationType, itemWorkflowId);
        return true; // No child operations, safe to delete
      }

      // Check each child operation step - ALL must have all batches deleted
      for (ItemWorkflowStep childStep : childSteps) {
        if (!areAllBatchesDeletedInStep(childStep)) {
          log.info("Found active batches in child operation {} for workflow {}, preventing deletion",
                   childStep.getOperationType(), itemWorkflowId);
          return false; // Found active batches in a child step
        }
      }

      log.info("All batches in {} child operations are deleted for workflow {}, allowing deletion",
               childSteps.size(), itemWorkflowId);
      return true; // All child operations have all batches deleted

    } catch (Exception e) {
      log.error("Error checking if next operation batches are deleted for {} in workflow {}: {}",
                currentOperationType, itemWorkflowId, e.getMessage());
      // In case of error, be conservative and don't allow deletion
      return false;
    }
  }

  /**
   * Helper method to check if all batches in a specific ItemWorkflowStep are deleted
   * This method handles both batch operations (HEAT_TREATMENT, MACHINING, etc.) and forging operations
   *
   * @param operationStep The ItemWorkflowStep to check for deleted batches
   * @return true if all batches in the step are deleted, false otherwise
   */
  private boolean areAllBatchesDeletedInStep(ItemWorkflowStep operationStep) {
    // Get the related entity IDs for the operation
    List<Long> relatedEntityIds = operationStep.getRelatedEntityIds();

    if (relatedEntityIds == null || relatedEntityIds.isEmpty()) {
      log.debug("No related entities found in operation {} for workflow {}, allowing deletion",
               operationStep.getOperationType(), operationStep.getItemWorkflow().getId());
      return true; // No related entities, safe to delete
    }

    // Check the operation outcome data to see if all batches are deleted
    if (operationStep.getOperationOutcomeData() == null ||
        operationStep.getOperationOutcomeData().trim().isEmpty()) {
      log.debug("No operation outcome data found in operation {} for workflow {}, allowing deletion",
               operationStep.getOperationType(), operationStep.getItemWorkflow().getId());
      return true; // No outcome data, safe to delete
    }

    try {
      OperationOutcomeData outcomeData = objectMapper.readValue(
          operationStep.getOperationOutcomeData(), OperationOutcomeData.class);

      // For batch operations, check if all batches are deleted
      if (operationStep.getOperationType() != WorkflowStep.OperationType.FORGING) {
        if (outcomeData.getBatchData() != null && !outcomeData.getBatchData().isEmpty()) {
          boolean allBatchesDeleted = outcomeData.getBatchData().stream()
              .allMatch(batch -> batch.getDeleted() != null && batch.getDeleted());

          log.debug("Checked {} batches in operation {} for workflow {}: all deleted = {}",
                   outcomeData.getBatchData().size(), operationStep.getOperationType(),
                   operationStep.getItemWorkflow().getId(), allBatchesDeleted);

          return allBatchesDeleted;
        }
      } else {
        // For forging operations, check if the forging data is deleted
        if (outcomeData.getForgingData() != null) {
          boolean forgingDeleted = outcomeData.getForgingData().getDeleted() != null &&
                                 outcomeData.getForgingData().getDeleted();

          log.debug("Checked forging data in operation {} for workflow {}: deleted = {}",
                   operationStep.getOperationType(), operationStep.getItemWorkflow().getId(), forgingDeleted);

          return forgingDeleted;
        }
      }

      log.debug("No operation data found in operation {} for workflow {}, allowing deletion",
               operationStep.getOperationType(), operationStep.getItemWorkflow().getId());
      return true; // No operation data, safe to delete

    } catch (Exception e) {
      log.error("Error parsing operation outcome data for {} in workflow {}: {}",
                operationStep.getOperationType(), operationStep.getItemWorkflow().getId(), e.getMessage());
      return false; // In case of error, be conservative
    }
  }

  /**
   * Returns pieces back to a specific operation/batch within the ItemWorkflowStep that contains the entity.
   * This is the reverse of updateAvailablePiecesInSpecificPreviousOperation - used when operations are deleted/cancelled
   * In tree-based workflows, finds the ItemWorkflowStep that contains the specificOperationId in its relatedEntityIds.
   *
   * @param itemWorkflowId       The workflow ID
   * @param currentOperationType The current operation type (kept for backward compatibility, not used in tree logic)
   * @param specificOperationId  The specific operation/batch ID to return pieces to (should match relatedEntityIds)
   * @param piecesToReturn       Number of pieces to return to the specific operation/batch
   */
  @Transactional
  public void returnPiecesToSpecificPreviousOperation(Long itemWorkflowId, WorkflowStep.OperationType currentOperationType, Long specificOperationId, int piecesToReturn, Long processedItemId) {
    if (itemWorkflowId == null) {
      log.warn("itemWorkflowId is null, cannot return pieces to previous operation");
      return;
    }

    try {
      // Get the workflow once at the beginning to ensure consistency
      ItemWorkflow workflow = getItemWorkflowById(itemWorkflowId);

      // Find the previous operation step that contains the specificOperationId in its relatedEntityIds
      ItemWorkflowStep previousOperationStep = null;
      for (ItemWorkflowStep step : workflow.getItemWorkflowSteps()) {
        if (step.getRelatedEntityIds() != null && step.getRelatedEntityIds().contains(specificOperationId)) {
          previousOperationStep = step;
          break;
        }
      }

      if (previousOperationStep == null) {
        log.warn("No ItemWorkflowStep found containing entity {} in workflow {}", specificOperationId, itemWorkflowId);
        return;
      }

      log.info("Found ItemWorkflowStep {} ({}) containing entity {} for pieces return",
               previousOperationStep.getId(), previousOperationStep.getOperationType(), specificOperationId);

      if (previousOperationStep.getOperationOutcomeData() == null || previousOperationStep.getOperationOutcomeData().trim().isEmpty()) {
        log.warn("No operation outcome data found to return pieces in workflow {}", itemWorkflowId);
        return;
      }

      OperationOutcomeData previousOutcomeData = objectMapper.readValue(previousOperationStep.getOperationOutcomeData(), OperationOutcomeData.class);
      boolean previousUpdated = false;

      // Handle different operation types for previous operation step
      if (previousOperationStep.getOperationType() == WorkflowStep.OperationType.FORGING) {
        if (previousOutcomeData.getForgingData() != null && specificOperationId.equals(previousOutcomeData.getForgingData().getId())) {
          int currentAvailable = previousOutcomeData.getForgingData().getPiecesAvailableForNext();
          previousOutcomeData.getForgingData().setPiecesAvailableForNext(currentAvailable + piecesToReturn);
          previousOutcomeData.getForgingData().setUpdatedAt(LocalDateTime.now());
          previousUpdated = true;
          log.info("Returned {} pieces to forging operation {} in workflow {}, new total: {}",
                   piecesToReturn, specificOperationId, itemWorkflowId, currentAvailable + piecesToReturn);
        }
      } else {
        // For batch operations
        if (previousOutcomeData.getBatchData() != null) {
          for (OperationOutcomeData.BatchOutcome batch : previousOutcomeData.getBatchData()) {
            if (specificOperationId.equals(batch.getId())) {
              int currentAvailable = batch.getPiecesAvailableForNext();
              batch.setPiecesAvailableForNext(currentAvailable + piecesToReturn);
              batch.setUpdatedAt(LocalDateTime.now());
              previousUpdated = true;
              log.info("Returned {} pieces to batch operation {} in workflow {}, new total: {}",
                       piecesToReturn, specificOperationId, itemWorkflowId, currentAvailable + piecesToReturn);
              break;
            }
          }
        }
      }

      if (!previousUpdated) {
        log.warn("No matching operation ID {} found to return pieces in workflow {}", specificOperationId, itemWorkflowId);
        throw new IllegalArgumentException("Operation ID " + specificOperationId + " not found in previous operation data");
      }

      // Update the previous operation step outcome data and total pieces available for next
      previousOperationStep.setOperationOutcomeData(objectMapper.writeValueAsString(previousOutcomeData));

      // Recalculate total pieces available for next in previous operation
      int totalPiecesAvailableInPrevious = 0;
      if (previousOperationStep.getOperationType() == WorkflowStep.OperationType.FORGING) {
        if (previousOutcomeData.getForgingData() != null) {
          totalPiecesAvailableInPrevious = previousOutcomeData.getForgingData().getPiecesAvailableForNext();
        }
      } else {
        if (previousOutcomeData.getBatchData() != null) {
          totalPiecesAvailableInPrevious = previousOutcomeData.getBatchData().stream()
              .mapToInt(OperationOutcomeData.BatchOutcome::getPiecesAvailableForNext)
              .sum();
        }
      }

      previousOperationStep.setPiecesAvailableForNext(totalPiecesAvailableInPrevious);

      // Find and update the current operation step within the same workflow instance
      ItemWorkflowStep currentOperationStep = null;
      for (ItemWorkflowStep step : workflow.getItemWorkflowSteps()) {
        if (step.getRelatedEntityIds() != null && step.getRelatedEntityIds().contains(processedItemId) && step.getOperationType() == currentOperationType) {
          currentOperationStep = step;
          break;
        }
      }

      if (currentOperationStep != null) {
        // Update the current operation step to subtract the returned pieces
        updateCurrentOperationStepForReturnedPiecesInMemory(currentOperationStep, piecesToReturn, processedItemId);
      } else {
        log.warn("Current operation step not found for operation {} with processedItemId {} in workflow {}", 
                 currentOperationType, processedItemId, itemWorkflowId);
      }

      // Save the updated workflow once at the end
      itemWorkflowRepository.save(workflow);

      log.info("Successfully returned {} pieces to previous operation and subtracted from current operation in workflow {}, previous operation total pieces available: {}",
               piecesToReturn, itemWorkflowId, totalPiecesAvailableInPrevious);

    } catch (Exception e) {
      log.error("Error returning pieces to specific previous operation for workflow {}: {}", itemWorkflowId, e.getMessage());
      throw new RuntimeException("Failed to return pieces to specific previous operation: " + e.getMessage(), e);
    }
  }

  /**
   * In-memory version of updateCurrentOperationStepForReturnedPieces that works with an already loaded ItemWorkflowStep.
   * This avoids additional database lookups and ensures consistency when working with a single workflow instance.
   *
   * @param currentOperationStep The current operation step (already loaded)
   * @param piecesToSubtract    Number of pieces to subtract from the operation
   * @param processedItemId     The processed item ID being deleted
   */
  private void updateCurrentOperationStepForReturnedPiecesInMemory(ItemWorkflowStep currentOperationStep, int piecesToSubtract, Long processedItemId) {
    try {
      WorkflowStep.OperationType operationType = currentOperationStep.getOperationType();

      // Update the step-level totals first
      int currentInitialPieces = currentOperationStep.getInitialPiecesCount() != null ? currentOperationStep.getInitialPiecesCount() : 0;
      int currentAvailablePieces = currentOperationStep.getPiecesAvailableForNext() != null ? currentOperationStep.getPiecesAvailableForNext() : 0;

      if (currentInitialPieces < piecesToSubtract) {
        log.warn("Current operation step has {} initial pieces but trying to subtract {} pieces. Setting to 0.",
                 currentInitialPieces, piecesToSubtract);
        currentOperationStep.setInitialPiecesCount(0);
      } else {
        currentOperationStep.setInitialPiecesCount(currentInitialPieces - piecesToSubtract);
      }

      if (currentAvailablePieces < piecesToSubtract) {
        log.warn("Current operation step has {} available pieces but trying to subtract {} pieces. Setting to 0.",
                 currentAvailablePieces, piecesToSubtract);
        currentOperationStep.setPiecesAvailableForNext(0);
      } else {
        currentOperationStep.setPiecesAvailableForNext(currentAvailablePieces - piecesToSubtract);
      }

      // Update the operation outcome data if it exists
      if (currentOperationStep.getOperationOutcomeData() != null && !currentOperationStep.getOperationOutcomeData().trim().isEmpty()) {
        OperationOutcomeData currentOutcomeData = objectMapper.readValue(currentOperationStep.getOperationOutcomeData(), OperationOutcomeData.class);
        boolean outcomeDataUpdated = false;
        LocalDateTime deletionTime = LocalDateTime.now();

        if (operationType == WorkflowStep.OperationType.FORGING) {
          // For forging operation, update the forging data and mark it as deleted
          if (currentOutcomeData.getForgingData() != null) {
            int forgingInitialPieces = currentOutcomeData.getForgingData().getInitialPiecesCount();
            int forgingAvailablePieces = currentOutcomeData.getForgingData().getPiecesAvailableForNext();

            currentOutcomeData.getForgingData().setInitialPiecesCount(Math.max(0, forgingInitialPieces - piecesToSubtract));
            currentOutcomeData.getForgingData().setPiecesAvailableForNext(Math.max(0, forgingAvailablePieces - piecesToSubtract));

            // Mark forging data as deleted since pieces are being returned (indicates deletion of the operation)
            currentOutcomeData.getForgingData().setDeleted(true);
            currentOutcomeData.getForgingData().setDeletedAt(deletionTime);
            currentOutcomeData.getForgingData().setUpdatedAt(deletionTime);

            outcomeDataUpdated = true;
            log.info("Marked forging data as deleted and updated piece counts for workflow {}", currentOperationStep.getItemWorkflow().getId());
          }
        } else {
          // For batch operations, subtract from batches and mark the affected ones as deleted
          if (currentOutcomeData.getBatchData() != null && !currentOutcomeData.getBatchData().isEmpty()) {
            for (OperationOutcomeData.BatchOutcome batch : currentOutcomeData.getBatchData()) {
              if (processedItemId.equals(batch.getId())) {
                int batchInitialPieces = batch.getInitialPiecesCount();
                int batchAvailablePieces = batch.getPiecesAvailableForNext();

                batch.setInitialPiecesCount(Math.max(0, batchInitialPieces - piecesToSubtract));
                batch.setPiecesAvailableForNext(Math.max(0, batchAvailablePieces - piecesToSubtract));

                // Mark batch as deleted since pieces are being returned (indicates deletion of the batch)
                batch.setDeleted(true);
                batch.setDeletedAt(deletionTime);
                batch.setUpdatedAt(deletionTime);

                outcomeDataUpdated = true;

                log.debug("Marked batch {} as deleted and subtracted {} pieces in current operation",
                          batch.getId(), piecesToSubtract);
                break;
              }
            }
          }
        }

        if (outcomeDataUpdated) {
          currentOperationStep.setOperationOutcomeData(objectMapper.writeValueAsString(currentOutcomeData));
          log.info("Updated current operation outcome data after subtracting {} pieces and marking as deleted for {} operation in workflow {}",
                   piecesToSubtract, operationType, currentOperationStep.getItemWorkflow().getId());
        }
      }

      log.info("Updated current operation step totals: initial pieces {} -> {}, available pieces {} -> {} for {} operation in workflow {}",
               currentInitialPieces, currentOperationStep.getInitialPiecesCount(),
               currentAvailablePieces, currentOperationStep.getPiecesAvailableForNext(),
               operationType, currentOperationStep.getItemWorkflow().getId());

    } catch (Exception e) {
      log.error("Error updating current operation step for returned pieces in memory: {}", e.getMessage());
      // Don't throw exception here as we want to continue with the transaction
    }
  }

  /**
   * Updates the operation step when pieces are returned to previous operation (used during operation deletion)
   * This subtracts the returned pieces from the current operation's initial pieces and available pieces
   * and marks the corresponding batch data as deleted in the workflow outcome data
   * This method can be used by any operation service (ForgeService, HeatTreatmentBatchService, etc.) when deleting operations
   *
   * @param itemWorkflowId   The workflow ID
   * @param operationType    The operation type being deleted
   * @param piecesToSubtract Number of pieces to subtract from the operation (total pieces that were produced by this operation)
   */
  public void updateCurrentOperationStepForReturnedPieces(Long itemWorkflowId, WorkflowStep.OperationType operationType, int piecesToSubtract, Long processedItemId) {
    try {
      // Get the current operation step
      ItemWorkflowStep currentOperationStep = findItemWorkflowStepByRelatedEntityId(itemWorkflowId, processedItemId, operationType);

      if (currentOperationStep == null) {
        log.error("No current operation step found for {} in workflow {}", operationType, itemWorkflowId);
        throw new RuntimeException("No current operation step found for " + operationType);
      }

      // Update the step-level totals first
      int currentInitialPieces = currentOperationStep.getInitialPiecesCount() != null ? currentOperationStep.getInitialPiecesCount() : 0;
      int currentAvailablePieces = currentOperationStep.getPiecesAvailableForNext() != null ? currentOperationStep.getPiecesAvailableForNext() : 0;

      if (currentInitialPieces < piecesToSubtract) {
        log.warn("Current operation step has {} initial pieces but trying to subtract {} pieces. Setting to 0.",
                 currentInitialPieces, piecesToSubtract);
        currentOperationStep.setInitialPiecesCount(0);
      } else {
        currentOperationStep.setInitialPiecesCount(currentInitialPieces - piecesToSubtract);
      }

      if (currentAvailablePieces < piecesToSubtract) {
        log.warn("Current operation step has {} available pieces but trying to subtract {} pieces. Setting to 0.",
                 currentAvailablePieces, piecesToSubtract);
        currentOperationStep.setPiecesAvailableForNext(0);
      } else {
        currentOperationStep.setPiecesAvailableForNext(currentAvailablePieces - piecesToSubtract);
      }

      // Update the operation outcome data if it exists
      if (currentOperationStep.getOperationOutcomeData() != null && !currentOperationStep.getOperationOutcomeData().trim().isEmpty()) {
        OperationOutcomeData currentOutcomeData = objectMapper.readValue(currentOperationStep.getOperationOutcomeData(), OperationOutcomeData.class);
        boolean outcomeDataUpdated = false;
        LocalDateTime deletionTime = LocalDateTime.now();

        if (operationType == WorkflowStep.OperationType.FORGING) {
          // For forging operation, update the forging data and mark it as deleted
          if (currentOutcomeData.getForgingData() != null) {
            int forgingInitialPieces = currentOutcomeData.getForgingData().getInitialPiecesCount();
            int forgingAvailablePieces = currentOutcomeData.getForgingData().getPiecesAvailableForNext();

            currentOutcomeData.getForgingData().setInitialPiecesCount(Math.max(0, forgingInitialPieces - piecesToSubtract));
            currentOutcomeData.getForgingData().setPiecesAvailableForNext(Math.max(0, forgingAvailablePieces - piecesToSubtract));

            // Mark forging data as deleted since pieces are being returned (indicates deletion of the operation)
            currentOutcomeData.getForgingData().setDeleted(true);
            currentOutcomeData.getForgingData().setDeletedAt(deletionTime);
            currentOutcomeData.getForgingData().setUpdatedAt(deletionTime);

            outcomeDataUpdated = true;
            log.info("Marked forging data as deleted and updated piece counts for workflow {}", itemWorkflowId);
          }
        } else {
          // For batch operations, subtract from batches and mark the affected ones as deleted
          if (currentOutcomeData.getBatchData() != null && !currentOutcomeData.getBatchData().isEmpty()) {
            int remainingToSubtract = piecesToSubtract;

            for (OperationOutcomeData.BatchOutcome batch : currentOutcomeData.getBatchData()) {
              if (remainingToSubtract <= 0) {
                break;
              }

              if (processedItemId.equals(batch.getId())) {
                int batchInitialPieces = batch.getInitialPiecesCount();
                int batchAvailablePieces = batch.getPiecesAvailableForNext();

                int toSubtractFromBatch = Math.min(remainingToSubtract, batchInitialPieces);

                batch.setInitialPiecesCount(batchInitialPieces - toSubtractFromBatch);
                batch.setPiecesAvailableForNext(Math.max(0, batchAvailablePieces - toSubtractFromBatch));

                // Mark batch as deleted since pieces are being returned (indicates deletion of the batch)
                batch.setDeleted(true);
                batch.setDeletedAt(deletionTime);
                batch.setUpdatedAt(deletionTime);

                remainingToSubtract -= toSubtractFromBatch;
                outcomeDataUpdated = true;

                log.debug("Marked batch {} as deleted and subtracted {} pieces in current operation, remaining to subtract: {}",
                          batch.getId(), toSubtractFromBatch, remainingToSubtract);
              } else {
                log.error("Batch id " + batch.getId() + " is not matched with processedItemId: " + processedItemId);
                throw new RuntimeException("Batch id " + batch.getId() + " is not matched with processedItemId: " + processedItemId);
              }
            }
          }
        }

        if (outcomeDataUpdated) {
          currentOperationStep.setOperationOutcomeData(objectMapper.writeValueAsString(currentOutcomeData));
          log.info("Updated current operation outcome data after subtracting {} pieces and marking as deleted for {} operation in workflow {}",
                   piecesToSubtract, operationType, itemWorkflowId);
        }
      }

      log.info("Updated current operation step totals: initial pieces {} -> {}, available pieces {} -> {} for {} operation in workflow {}",
               currentInitialPieces, currentOperationStep.getInitialPiecesCount(),
               currentAvailablePieces, currentOperationStep.getPiecesAvailableForNext(),
               operationType, itemWorkflowId);

    } catch (Exception e) {
      log.error("Error updating current operation step for returned pieces in workflow {}: {}", itemWorkflowId, e.getMessage());
      // Don't throw exception here as the previous operation update was successful
      // We don't want to roll back the entire transaction
    }
  }

  /**
   * Generic method to mark an operation as deleted and update workflow step piece counts
   * This is an alias for updateCurrentOperationStepForReturnedPieces with a more descriptive name
   * Use this method when deleting operations to properly update the workflow state
   *
   * @param itemWorkflowId      The workflow ID
   * @param operationType       The operation type being deleted
   * @param totalPiecesProduced Total pieces that were produced by this operation (to be subtracted)
   */
  public void markOperationAsDeletedAndUpdatePieceCounts(Long itemWorkflowId, WorkflowStep.OperationType operationType, int totalPiecesProduced, Long processedItemId) {
    updateCurrentOperationStepForReturnedPieces(itemWorkflowId, operationType, totalPiecesProduced, processedItemId);
  }

  /**
   * Generic method to handle workflow creation or retrieval for any operation type
   * This method can be used by all operation services (Forge, Heat Treatment, Machining, Quality, Dispatch)
   *
   * @param item                   The item for which to handle the workflow
   * @param operationType          The operation type (FORGING, HEAT_TREATMENT, MACHINING, QUALITY, DISPATCH)
   * @param workflowIdentifier     The workflow identifier for this operation
   * @param itemWorkflowId Optional existing workflow ID to update
   * @return The ItemWorkflow (either existing or newly created)
   */
  @Transactional
  public ItemWorkflow startItemWorkflowStepOperation(Item item,
                                                     WorkflowStep.OperationType operationType,
                                                     String workflowIdentifier,
                                                     Long itemWorkflowId,
                                                     Long relatedEntityId) {
    if (itemWorkflowId == null) {
      log.error("itemWorkflowId is null, cannot handle operation {}", operationType);
      throw new RuntimeException("itemWorkflowId is null, cannot handle operation");
    }

    log.info("Handling workflow for {} operation on item {} with workflow identifier: {}",
             operationType, item.getId(), workflowIdentifier);

    ItemWorkflow workflow = getItemWorkflowById(itemWorkflowId);
    ItemWorkflowStep operationStep;
    if(operationType.equals(WorkflowStep.OperationType.FORGING)) {
      operationStep = workflow.getItemWorkflowSteps().stream().filter(itemWorkflowStep -> itemWorkflowStep.getOperationType().equals(operationType)).findFirst().get();
    } else {
      operationStep = findItemWorkflowStepByRelatedEntityId(itemWorkflowId, relatedEntityId, operationType);
    }

    if (operationStep != null && operationStep.getStepStatus() == ItemWorkflowStep.StepStatus.PENDING) {
      workflow.startOperationStep(operationStep);
      itemWorkflowRepository.save(workflow);
      log.info("Started {} operation step for workflow {}", operationType, workflow.getId());
    } else if (operationStep != null) {
      log.info("{} operation step is already started or completed for workflow {}. Current status: {}",
               operationType, workflow.getId(), operationStep.getStepStatus());
    } else {
      log.warn("No {} operation step found in workflow {}", operationType, workflow.getId());
    }

    log.info("Successfully handled workflow for {} operation. Workflow ID: {}, Workflow Identifier: {}",
             operationType, workflow.getId(), workflow.getWorkflowIdentifier());

    return workflow;
  }



  @Transactional
  public ItemWorkflow startItemWorkflowStepOperationForDispatch(
                                                     Long itemWorkflowId,
                                                     Long parentEntityId) {

    ItemWorkflow workflow = getItemWorkflowById(itemWorkflowId);
    ItemWorkflowStep operationStep = findItemWorkflowStepByParentEntityId(itemWorkflowId, parentEntityId, WorkflowStep.OperationType.DISPATCH);

    if (operationStep != null && operationStep.getStepStatus() == ItemWorkflowStep.StepStatus.PENDING) {
      workflow.startOperationStep(operationStep);
      itemWorkflowRepository.save(workflow);
      log.info("Started DISPATCH operation step for workflow {}", workflow.getId());
    } else if (operationStep != null) {
      log.info("DISPATCH operation step is already started or completed for workflow {}. Current status: {}",
                workflow.getId(), operationStep.getStepStatus());
    } else {
      log.warn("No DISPATCH operation step found in workflow {}", workflow.getId());
    }

    log.info("Successfully handled workflow for DISPATCH operation. Workflow ID: {}, Workflow Identifier: {}",
             workflow.getId(), workflow.getWorkflowIdentifier());

    return workflow;
  }

  public List<OperationOutcomeData.BatchOutcome> getAccumulatedBatchOutcomeData(Long workflowId, WorkflowStep.OperationType operationType, long parentEntityId) {
    List<OperationOutcomeData.BatchOutcome> accumulatedBatchData = new ArrayList<>();

    try {
      ItemWorkflowStep existingItemWorkflowStep = findItemWorkflowStepByParentEntityId(workflowId, parentEntityId, operationType);

      if (existingItemWorkflowStep != null &&
          existingItemWorkflowStep.getOperationOutcomeData() != null &&
          !existingItemWorkflowStep.getOperationOutcomeData().trim().isEmpty()) {

        // Parse existing outcome data and get existing batch data
        OperationOutcomeData existingOutcomeData = objectMapper.readValue(
            existingItemWorkflowStep.getOperationOutcomeData(), OperationOutcomeData.class);

        if (existingOutcomeData.getBatchData() != null) {
          accumulatedBatchData.addAll(existingOutcomeData.getBatchData());
        }
      }
    } catch (Exception e) {
      log.warn("Failed to parse existing workflow outcome data for machining step in workflow {}: {}",
               workflowId, e.getMessage());
    }

    return accumulatedBatchData;
  }

  /**
   * Generic method to determine the previous operation batch ID based on workflow step information
   * This method can be used by any operation service (HEAT_TREATMENT, MACHINING, INSPECTION, DISPATCH)
   *
   * @param itemWorkflowId                   The workflow ID
   * @param currentOperationType             The current operation type (kept for backward compatibility, not used in tree logic)
   * @param previousOperationProcessedItemId The previous operation processed item ID (should match relatedEntityIds)
   * @return The previous operation batch ID, or null if not found
   */
  public Long getPreviousOperationBatchId(Long itemWorkflowId,
                                          WorkflowStep.OperationType currentOperationType,
                                          Long previousOperationProcessedItemId) {
    try {
      // In tree-based workflow, find the ItemWorkflowStep that contains the previousOperationProcessedItemId in its relatedEntityIds
      ItemWorkflowStep previousOperationStep = findItemWorkflowStepByRelatedEntityId(itemWorkflowId, previousOperationProcessedItemId);

      if (previousOperationStep == null) {
        log.warn("No ItemWorkflowStep found containing entity {} in workflow {}",
                 previousOperationProcessedItemId, itemWorkflowId);
        return null;
      }

      log.info("Found ItemWorkflowStep {} ({}) containing entity {} for batch ID retrieval",
               previousOperationStep.getId(), previousOperationStep.getOperationType(), previousOperationProcessedItemId);

      WorkflowStep.OperationType previousOperationType = previousOperationStep.getOperationType();
      log.info("Previous operation type is {} for workflow {} (current operation: {})",
               previousOperationType, itemWorkflowId, currentOperationType);

      if (WorkflowStep.OperationType.FORGING.equals(previousOperationType)) {
        // For FORGING, get the related entity ID from the previous step's operationOutcomeData
        return getPreviousOperationBatchIdFromForging(previousOperationStep);
      } else {
        // For other operations (like HEAT_TREATMENT, MACHINING, etc.), use the previousOperationProcessedItemId
        return getPreviousOperationBatchIdFromNonForging(previousOperationProcessedItemId, previousOperationType);
      }

    } catch (Exception e) {
      log.error("Error determining previous operation batch ID for workflow {} and current operation {}: {}",
                itemWorkflowId, currentOperationType, e.getMessage());
      return null;
    }
  }

  /**
   * Gets the batch ID from a FORGING previous operation step
   * For FORGING, the operationOutcomeData.forgingData.id contains the forge ID
   */
  private Long getPreviousOperationBatchIdFromForging(ItemWorkflowStep previousOperationStep) {
    try {
      if (previousOperationStep.getOperationOutcomeData() == null ||
          previousOperationStep.getOperationOutcomeData().trim().isEmpty()) {
        log.warn("No operation outcome data found in previous FORGING operation step");
        return null;
      }

      OperationOutcomeData outcomeData = objectMapper.readValue(
          previousOperationStep.getOperationOutcomeData(), OperationOutcomeData.class);

      if (outcomeData.getForgingData() != null) {
        Long forgeId = outcomeData.getForgingData().getId();
        log.info("Found forge ID {} from previous FORGING operation", forgeId);
        return forgeId;
      } else {
        log.warn("No forging data found in previous FORGING operation outcome");
        return null;
      }

    } catch (Exception e) {
      log.error("Error extracting forge ID from previous FORGING operation: {}", e.getMessage());
      return null;
    }
  }

  /**
   * Gets the batch ID from a non-FORGING previous operation step
   * For non-FORGING operations, we need to find the batch that has the provided previousOperationProcessedItemId
   */
  private Long getPreviousOperationBatchIdFromNonForging(Long previousOperationProcessedItemId,
                                                         WorkflowStep.OperationType previousOperationType) {
    try {
      if (previousOperationProcessedItemId == null) {
        log.warn("No previousOperationProcessedItemId provided for previous operation type {}",
                 previousOperationType);
        return null;
      }

      log.info("Looking for {} batch with previousOperationProcessedItemId={}",
               previousOperationType, previousOperationProcessedItemId);

      // Use the appropriate service based on the previous operation type
      switch (previousOperationType) {
        case HEAT_TREATMENT:
          return processedItemHeatTreatmentBatchService
              .getProcessedItemHeatTreatmentBatchIdByProcessedItemId(previousOperationProcessedItemId);
        case MACHINING:
          return processedItemMachiningBatchService
              .getProcessedItemMachiningBatchIdByProcessedItemId(previousOperationProcessedItemId);
        case QUALITY:
          return processedItemInspectionBatchService
              .getProcessedItemInspectionBatchIdByProcessedItemId(previousOperationProcessedItemId);
        case DISPATCH:
          return processedItemDispatchBatchService
              .getProcessedItemDispatchBatchIdByProcessedItemId(previousOperationProcessedItemId);
        case VENDOR:
          return processedItemVendorDispatchBatchService
              .getProcessedItemVendorDispatchBatchIdByProcessedItemId(previousOperationProcessedItemId);
        default:
          log.warn("Unsupported previous operation type {} for non-forging batch ID lookup", previousOperationType);
          return null;
      }

    } catch (Exception e) {
      log.error("Error getting batch ID from previous {} operation: {}", previousOperationType, e.getMessage());
      return null;
    }
  }

  /**
   * Gets paginated list of all ItemWorkflows for a tenant, ordered by updatedAt DESC
   * Only includes workflows for non-deleted items
   *
   * @param tenantId The tenant ID
   * @param pageable Pagination information
   * @return Page of ItemWorkflow entities
   */
  public Page<ItemWorkflow> getAllItemWorkflowsForTenant(Long tenantId, Pageable pageable) {
    return itemWorkflowRepository.findByTenantIdAndItemNotDeletedOrderByUpdatedAtDesc(tenantId, pageable);
  }

  /**
   * Gets all ItemWorkflows for a tenant without pagination, ordered by updatedAt DESC
   * Only includes workflows for non-deleted items
   *
   * @param tenantId The tenant ID
   * @return List of ItemWorkflow entities
   */
  public List<ItemWorkflow> getAllItemWorkflowsForTenantWithoutPagination(Long tenantId) {
    return itemWorkflowRepository.findByTenantIdAndItemNotDeletedOrderByUpdatedAtDesc(tenantId);
  }

  /**
   * Gets all ItemWorkflows for a tenant without pagination as a list representation
   * Only includes workflows for non-deleted items, ordered by updatedAt DESC
   *
   * @param tenantId The tenant ID
   * @return ItemWorkflowListRepresentation containing all workflows
   */
  public ItemWorkflowListRepresentation getAllItemWorkflowsForTenantWithoutPaginationAsRepresentation(Long tenantId) {
    List<ItemWorkflow> workflows = getAllItemWorkflowsForTenantWithoutPagination(tenantId);

    List<ItemWorkflowRepresentation> workflowRepresentations = workflows.stream()
        .map(itemWorkflowAssembler::dissemble)
        .collect(Collectors.toList());

    return ItemWorkflowListRepresentation.builder()
        .itemWorkflows(workflowRepresentations)
        .build();
  }

  /**
   * Gets all ItemWorkflows for a specific item
   * Only includes non-deleted workflows, ordered by createdAt DESC
   *
   * @param itemId The item ID
   * @return List of ItemWorkflow entities for the specified item
   */
  public List<ItemWorkflow> getItemWorkflowsByItemId(Long itemId) {
    return itemWorkflowRepository.findByItemIdAndDeletedFalseOrderByCreatedAtDesc(itemId);
  }

  /**
   * Searches ItemWorkflows by item name with pagination
   *
   * @param tenantId The tenant ID
   * @param itemName The item name to search for (partial match, case insensitive)
   * @param pageable Pagination information
   * @return Page of matching ItemWorkflow entities
   */
  public Page<ItemWorkflow> searchItemWorkflowsByItemName(Long tenantId, String itemName, Pageable pageable) {
    return itemWorkflowRepository.findByTenantIdAndItemNameContainingIgnoreCaseOrderByUpdatedAtDesc(tenantId, itemName, pageable);
  }

  /**
   * Searches ItemWorkflows by workflow identifier with pagination
   *
   * @param tenantId           The tenant ID
   * @param workflowIdentifier The workflow identifier to search for (partial match, case insensitive)
   * @param pageable           Pagination information
   * @return Page of matching ItemWorkflow entities
   */
  public Page<ItemWorkflow> searchItemWorkflowsByWorkflowIdentifier(Long tenantId, String workflowIdentifier, Pageable pageable) {
    return itemWorkflowRepository.findByTenantIdAndWorkflowIdentifierContainingIgnoreCaseOrderByUpdatedAtDesc(tenantId, workflowIdentifier, pageable);
  }

  /**
   * Gets comprehensive tracking information for an ItemWorkflow by workflow identifier
   * This includes all related batches (forge, heat treatment, machining, inspection, dispatch)
   * organized by workflow step order
   *
   * @param tenantId           The tenant ID for validation
   * @param workflowIdentifier The workflow identifier to search for
   * @return ItemWorkflowTrackingResultDTO containing workflow and all related batches
   * @throws RuntimeException if workflow is not found or doesn't belong to tenant
   */
  @Transactional(readOnly = true)
  public ItemWorkflowTrackingResultDTO getItemWorkflowTrackingByWorkflowIdentifier(Long tenantId, String workflowIdentifier) {
    try {
      // Find the workflow by workflow identifier (single lookup instead of 6)
      List<ItemWorkflow> workflows = itemWorkflowRepository.findByWorkflowIdentifierAndDeletedFalse(workflowIdentifier);
      if (workflows.isEmpty()) {
        throw new RuntimeException("No workflow found with identifier: " + workflowIdentifier);
      }

      // Get the first workflow (there should typically be only one)
      ItemWorkflow itemWorkflow = workflows.get(0);

      // Validate that workflow belongs to the tenant
      if (itemWorkflow.getItem().getTenant().getId() != tenantId.longValue()) {
        throw new RuntimeException("Workflow does not belong to the specified tenant");
      }

      // Convert workflow to representation
      ItemWorkflowRepresentation workflowRepresentation = itemWorkflowAssembler.dissemble(itemWorkflow);
      
      // Consolidate duplicate workflow steps (e.g., multiple DISPATCH steps with same operation type)
      workflowRepresentation = consolidateDuplicateWorkflowSteps(workflowRepresentation);

      // Get the ItemWorkflow database ID for optimized batch lookups
      Long itemWorkflowId = itemWorkflow.getId();
      log.info("Found ItemWorkflow ID {} for workflow identifier {}", itemWorkflowId, workflowIdentifier);

      // Find all related batches by itemWorkflowId (more efficient than repeated workflow lookups)
      List<ForgeRepresentation> forges = findForgesByItemWorkflowId(itemWorkflowId);
      List<HeatTreatmentBatchRepresentation> heatTreatmentBatches = findHeatTreatmentBatchesByItemWorkflowId(itemWorkflowId);
      List<MachiningBatchRepresentation> machiningBatches = findMachiningBatchesByItemWorkflowId(itemWorkflowId);
      List<InspectionBatchRepresentation> inspectionBatches = findInspectionBatchesByItemWorkflowId(itemWorkflowId);
      List<VendorDispatchBatchRepresentation> vendorDispatchBatches = findVendorDispatchBatchesByItemWorkflowId(itemWorkflowId);
      List<DispatchBatchRepresentation> dispatchBatches = findDispatchBatchesByItemWorkflowId(itemWorkflowId);

      return ItemWorkflowTrackingResultDTO.builder()
          .itemWorkflow(workflowRepresentation)
          .forges(forges)
          .heatTreatmentBatches(heatTreatmentBatches)
          .machiningBatches(machiningBatches)
          .inspectionBatches(inspectionBatches)
          .vendorDispatchBatches(vendorDispatchBatches)
          .dispatchBatches(dispatchBatches)
          .build();

    } catch (Exception e) {
      log.error("Error fetching workflow tracking for identifier {} in tenant {}: {}", workflowIdentifier, tenantId, e.getMessage());
      throw new RuntimeException("Failed to fetch workflow tracking: " + e.getMessage(), e);
    }
  }

  /**
   * Completes an item workflow and all its steps with the provided completion time
   *
   * @param itemWorkflowId The workflow ID to complete
   * @param tenantId       The tenant ID for validation
   * @param completedAt    The completion date and time
   * @return The completed ItemWorkflow
   * @throws RuntimeException if the workflow is not found or doesn't belong to the tenant
   */
  @Transactional
  public ItemWorkflow completeItemWorkflow(Long itemWorkflowId, Long tenantId, LocalDateTime completedAt) {
    try {
      // Get the workflow
      ItemWorkflow itemWorkflow = getItemWorkflowById(itemWorkflowId);

      // Validate that workflow belongs to the tenant
      if (itemWorkflow.getItem().getTenant().getId() != tenantId.longValue()) {
        throw new RuntimeException("Workflow does not belong to the specified tenant");
      }

      // Validate that completedAt is after startedAt
      if (itemWorkflow.getStartedAt() != null && completedAt.isBefore(itemWorkflow.getStartedAt())) {
        throw new RuntimeException("Completion time must be after the workflow start time");
      }

      // Check if already completed
      if (itemWorkflow.getWorkflowStatus() == ItemWorkflow.WorkflowStatus.COMPLETED) {
        log.info("Workflow {} is already completed", itemWorkflowId);
        return itemWorkflow;
      }

      // Mark workflow as completed
      itemWorkflow.setWorkflowStatus(ItemWorkflow.WorkflowStatus.COMPLETED);
      itemWorkflow.setCompletedAt(completedAt);

      // If not started yet, mark as started
      if (itemWorkflow.getStartedAt() == null) {
        itemWorkflow.setStartedAt(completedAt);
      }

      // Mark all workflow steps as completed (without individual completion times as per requirement)
      for (ItemWorkflowStep step : itemWorkflow.getItemWorkflowSteps()) {
        if (step.getStepStatus() != ItemWorkflowStep.StepStatus.COMPLETED) {
          step.setStepStatus(ItemWorkflowStep.StepStatus.COMPLETED);

          // Start the step if not started yet
          if (step.getStartedAt() == null) {
            step.setStartedAt(completedAt);
          }

          // Note: We don't set individual step completion times as per requirement
          log.info("Marked workflow step {} ({}) as completed for workflow {}",
                   step.getId(), step.getOperationType(), itemWorkflowId);
        }
      }

      // Save the workflow
      ItemWorkflow savedWorkflow = itemWorkflowRepository.save(itemWorkflow);

      log.info("Successfully completed workflow {} with completion time: {}", itemWorkflowId, completedAt);

      return savedWorkflow;

    } catch (Exception e) {
      log.error("Error completing workflow {} for tenant {}: {}", itemWorkflowId, tenantId, e.getMessage());
      throw new RuntimeException("Failed to complete workflow: " + e.getMessage(), e);
    }
  }

  // Helper methods for finding batches by workflow identifier

  /**
   * Find forges by ItemWorkflow ID
   *
   * @param itemWorkflowId The ItemWorkflow database ID
   * @return List of ForgeRepresentation
   */
  private List<ForgeRepresentation> findForgesByItemWorkflowId(Long itemWorkflowId) {
    try {
      // Find forges by ItemWorkflow ID (the processed_item.item_workflow_id should match this)
      List<Forge> forges = forgeRepository.findByProcessedItemItemWorkflowIdAndDeletedFalse(itemWorkflowId);
      log.info("Found {} forges for ItemWorkflow ID: {}", forges.size(), itemWorkflowId);

      return forges.stream()
          .map(forgeAssembler::dissemble)
          .collect(Collectors.toList());
    } catch (Exception e) {
      log.error("Error finding forges for ItemWorkflow ID {}: {}", itemWorkflowId, e.getMessage());
      return new ArrayList<>();
    }
  }

  /**
   * Find heat treatment batches by ItemWorkflow ID
   *
   * @param itemWorkflowId The ItemWorkflow database ID
   * @return List of HeatTreatmentBatchRepresentation
   */
  private List<HeatTreatmentBatchRepresentation> findHeatTreatmentBatchesByItemWorkflowId(Long itemWorkflowId) {
    try {
      // Find heat treatment batches by ItemWorkflow ID
      List<HeatTreatmentBatch> batches =
          heatTreatmentBatchRepository.findByProcessedItemHeatTreatmentBatchesItemWorkflowIdAndDeletedFalse(itemWorkflowId);
      log.info("Found {} heat treatment batches for ItemWorkflow ID: {}", batches.size(), itemWorkflowId);

      return batches.stream()
          .map(heatTreatmentBatchAssembler::dissemble)
          .collect(Collectors.toList());
    } catch (Exception e) {
      log.error("Error finding heat treatment batches for ItemWorkflow ID {}: {}", itemWorkflowId, e.getMessage());
      return new ArrayList<>();
    }
  }

  /**
   * Find machining batches by ItemWorkflow ID
   *
   * @param itemWorkflowId The ItemWorkflow database ID
   * @return List of MachiningBatchRepresentation
   */
  private List<MachiningBatchRepresentation> findMachiningBatchesByItemWorkflowId(Long itemWorkflowId) {
    try {
      // Find machining batches by ItemWorkflow ID
      List<MachiningBatch> batches =
          machiningBatchRepository.findByProcessedItemMachiningBatchItemWorkflowIdAndDeletedFalse(itemWorkflowId);
      log.info("Found {} machining batches for ItemWorkflow ID: {}", batches.size(), itemWorkflowId);

      return batches.stream()
          .map(machiningBatchAssembler::dissemble)
          .collect(Collectors.toList());
    } catch (Exception e) {
      log.error("Error finding machining batches for ItemWorkflow ID {}: {}", itemWorkflowId, e.getMessage());
      return new ArrayList<>();
    }
  }

  /**
   * Find inspection batches by ItemWorkflow ID
   *
   * @param itemWorkflowId The ItemWorkflow database ID
   * @return List of InspectionBatchRepresentation
   */
  private List<InspectionBatchRepresentation> findInspectionBatchesByItemWorkflowId(Long itemWorkflowId) {
    try {
      // Find inspection batches by ItemWorkflow ID
      List<InspectionBatch> batches =
          inspectionBatchRepository.findByProcessedItemInspectionBatchItemWorkflowIdAndDeletedFalse(itemWorkflowId);
      log.info("Found {} inspection batches for ItemWorkflow ID: {}", batches.size(), itemWorkflowId);

      return batches.stream()
          .map(inspectionBatchAssembler::dissemble)
          .collect(Collectors.toList());
    } catch (Exception e) {
      log.error("Error finding inspection batches for ItemWorkflow ID {}: {}", itemWorkflowId, e.getMessage());
      return new ArrayList<>();
    }
  }

  /**
   * Find dispatch batches by ItemWorkflow ID
   *
   * @param itemWorkflowId The ItemWorkflow database ID
   * @return List of DispatchBatchRepresentation
   */
  private List<DispatchBatchRepresentation> findDispatchBatchesByItemWorkflowId(Long itemWorkflowId) {
    try {
      // Find dispatch batches by ItemWorkflow ID
      List<DispatchBatch> batches =
          dispatchBatchRepository.findByProcessedItemDispatchBatchItemWorkflowIdAndDeletedFalse(itemWorkflowId);
      log.info("Found {} dispatch batches for ItemWorkflow ID: {}", batches.size(), itemWorkflowId);

      return batches.stream()
          .map(dispatchBatchAssembler::dissemble)
          .collect(Collectors.toList());
    } catch (Exception e) {
      log.error("Error finding dispatch batches for ItemWorkflow ID {}: {}", itemWorkflowId, e.getMessage());
      return new ArrayList<>();
    }
  }

  /**
   * Find vendor dispatch batches by ItemWorkflow ID
   *
   * @param itemWorkflowId The ItemWorkflow database ID
   * @return List of VendorDispatchBatchRepresentation
   */
  private List<VendorDispatchBatchRepresentation> findVendorDispatchBatchesByItemWorkflowId(Long itemWorkflowId) {
    try {
      // Find vendor dispatch batches by ItemWorkflow ID
      List<VendorDispatchBatch> batches =
          vendorDispatchBatchRepository.findByProcessedItemItemWorkflowIdAndDeletedFalse(itemWorkflowId);
      log.info("Found {} vendor dispatch batches for ItemWorkflow ID: {}", batches.size(), itemWorkflowId);

      return batches.stream()
          .map(batch -> vendorDispatchBatchAssembler.dissemble(batch, true))
          .collect(Collectors.toList());
    } catch (Exception e) {
      log.error("Error finding vendor dispatch batches for ItemWorkflow ID {}: {}", itemWorkflowId, e.getMessage());
      return new ArrayList<>();
    }
  }

  /**
   * Gets available heats from the first operation of an item workflow.
   * This method finds the first workflow step and gets the heats from the corresponding processed item.
   *
   * @param itemWorkflowId the ID of the item workflow
   * @return List of HeatInfoDTO objects representing available heats
   */
  public List<HeatInfoDTO> getAvailableHeatsFromFirstOperation(Long itemWorkflowId) {
    log.debug("Getting available heats from first operation for workflow ID: {}", itemWorkflowId);

    try {
      // Get the item workflow
      ItemWorkflow itemWorkflow = getItemWorkflowById(itemWorkflowId);

      // Get workflow steps and find the first one (root step)
      ItemWorkflowStep firstStep = itemWorkflow.getItemWorkflowSteps().stream()
          .filter(step -> step.getWorkflowStep().isRootStep())
          .findFirst()
          .orElse(null);

      if (firstStep == null) {
        log.warn("No first step found for workflow ID: {}", itemWorkflowId);
        return new ArrayList<>();
      }

      // Get the related entity IDs from the first step
      List<Long> relatedEntityIds = firstStep.getRelatedEntityIds();
      if (relatedEntityIds == null || relatedEntityIds.isEmpty()) {
        log.warn("No related entity IDs found for first step of workflow ID: {}", itemWorkflowId);
        return new ArrayList<>();
      }

      // Based on the operation type, get the appropriate entity and extract heats
      WorkflowStep.OperationType operationType = firstStep.getOperationType();

      switch (operationType) {
        case FORGING:
          return getHeatsFromForging(relatedEntityIds);

        case HEAT_TREATMENT:
          return getHeatsFromHeatTreatment(relatedEntityIds);

        case MACHINING:
          return getHeatsFromMachining(relatedEntityIds);

        case QUALITY:
          return getHeatsFromQuality(relatedEntityIds);

        case DISPATCH:
          return getHeatsFromDispatch(relatedEntityIds);

        default:
          log.warn("Operation type {} is not supported for heat extraction", operationType);
          return new ArrayList<>();
      }

    } catch (Exception e) {
      log.error("Error getting available heats from first operation for workflow ID {}: {}", itemWorkflowId, e.getMessage());
      return new ArrayList<>();
    }
  }

  private List<HeatInfoDTO> getHeatsFromForging(List<Long> relatedEntityIds) {
    try {
      Long processItemId = relatedEntityIds.get(0);
      Forge forge = forgeRepository.findByProcessedItemIdAndDeletedFalse(processItemId).orElse(null);

      if (forge != null && forge.getForgeHeats() != null) {
        return forge.getForgeHeats().stream()
            .map(ForgeHeat::getHeat)
            .filter(heat -> heat != null)
            .map(this::convertHeatToDTO)
            .collect(Collectors.toList());
      }
      return new ArrayList<>();
    } catch (Exception e) {
      log.error("Error extracting heats from forging operation: {}", e.getMessage());
      return new ArrayList<>();
    }
  }

  private List<HeatInfoDTO> getHeatsFromHeatTreatment(List<Long> relatedEntityIds) {
    try {
      Long heatTreatmentBatchId = relatedEntityIds.get(0);
      ProcessedItemHeatTreatmentBatch heatTreatmentBatch = processedItemHeatTreatmentBatchRepository
          .findById(heatTreatmentBatchId).orElse(null);

      if (heatTreatmentBatch != null && heatTreatmentBatch.getHeatTreatmentHeats() != null) {
        return heatTreatmentBatch.getHeatTreatmentHeats().stream()
            .map(HeatTreatmentHeat::getHeat)
            .filter(heat -> heat != null)
            .map(this::convertHeatToDTO)
            .collect(Collectors.toList());
      }
      return new ArrayList<>();
    } catch (Exception e) {
      log.error("Error extracting heats from heat treatment operation: {}", e.getMessage());
      return new ArrayList<>();
    }
  }

  private List<HeatInfoDTO> getHeatsFromMachining(List<Long> relatedEntityIds) {
    try {
      Long machiningBatchId = relatedEntityIds.get(0);
      ProcessedItemMachiningBatch machiningBatch = processedItemMachiningBatchRepository
          .findById(machiningBatchId).orElse(null);

      if (machiningBatch != null && machiningBatch.getMachiningHeats() != null) {
        return machiningBatch.getMachiningHeats().stream()
            .map(MachiningHeat::getHeat)
            .filter(heat -> heat != null)
            .map(this::convertHeatToDTO)
            .collect(Collectors.toList());
      }
      return new ArrayList<>();
    } catch (Exception e) {
      log.error("Error extracting heats from machining operation: {}", e.getMessage());
      return new ArrayList<>();
    }
  }

  private List<HeatInfoDTO> getHeatsFromQuality(List<Long> relatedEntityIds) {
    try {
      Long inspectionBatchId = relatedEntityIds.get(0);
      ProcessedItemInspectionBatch inspectionBatch = processedItemInspectionBatchRepository
          .findById(inspectionBatchId).orElse(null);

      if (inspectionBatch != null && inspectionBatch.getInspectionHeats() != null) {
        return inspectionBatch.getInspectionHeats().stream()
            .map(InspectionHeat::getHeat)
            .filter(heat -> heat != null)
            .map(this::convertHeatToDTO)
            .collect(Collectors.toList());
      }
      return new ArrayList<>();
    } catch (Exception e) {
      log.error("Error extracting heats from quality/inspection operation: {}", e.getMessage());
      return new ArrayList<>();
    }
  }

  private List<HeatInfoDTO> getHeatsFromDispatch(List<Long> relatedEntityIds) {
    try {
      Long dispatchBatchId = relatedEntityIds.get(0);
      ProcessedItemDispatchBatch dispatchBatch = processedItemDispatchBatchRepository
          .findById(dispatchBatchId).orElse(null);

      if (dispatchBatch != null && dispatchBatch.getDispatchHeats() != null) {
        return dispatchBatch.getDispatchHeats().stream()
            .map(DispatchHeat::getHeat)
            .filter(heat -> heat != null)
            .map(this::convertHeatToDTO)
            .collect(Collectors.toList());
      }
      return new ArrayList<>();
    } catch (Exception e) {
      log.error("Error extracting heats from dispatch operation: {}", e.getMessage());
      return new ArrayList<>();
    }
  }

  private HeatInfoDTO convertHeatToDTO(Heat heat) {
    HeatInfoDTO dto = new HeatInfoDTO();
    dto.setHeatId(heat.getId());
    dto.setHeatNumber(heat.getHeatNumber());
    dto.setHeatQuantity(heat.getHeatQuantity());
    dto.setAvailableHeatQuantity(heat.getAvailableHeatQuantity());
    dto.setPiecesCount(heat.getPiecesCount());
    dto.setAvailablePiecesCount(heat.getAvailablePiecesCount());
    return dto;
  }

  public ItemWorkflowStep findItemWorkflowStepByParentEntityId(Long itemWorkflowId, Long parentEntityId, WorkflowStep.OperationType operationType) {
    try {
      ItemWorkflow workflow = getItemWorkflowById(itemWorkflowId);
      if (workflow == null) {
        log.warn("ItemWorkflow with ID {} not found", itemWorkflowId);
        return null;
      }

      for (ItemWorkflowStep step : workflow.getItemWorkflowSteps()) {
        if (step.getRelatedEntityIds() != null && step.getRelatedEntityIds().contains(parentEntityId)) {
          log.debug("Found Parent ItemWorkflowStep {} containing entity {} in workflow {}",
                    step.getOperationType(), parentEntityId, itemWorkflowId);
          return step.getChildItemWorkflowSteps().stream().filter(childItemWorkflowStep -> operationType==childItemWorkflowStep.getOperationType())
              .findFirst().orElse(null);
        }
      }

      log.warn("No ItemWorkflowStep found containing parentEntityId {} in workflow {}", parentEntityId, itemWorkflowId);
      return null;
    } catch (Exception e) {
      log.error("Error finding ItemWorkflowStep by parentEntityId {} in workflow {}: {}",
                parentEntityId, itemWorkflowId, e.getMessage());
      return null;
    }
  }


  /**
   * Finds an ItemWorkflowStep that contains the specified entity ID in its relatedEntityIds
   * This is used in tree-based workflows to find the specific parent step for a given entity
   */
  public ItemWorkflowStep findItemWorkflowStepByRelatedEntityId(Long itemWorkflowId, Long entityId, WorkflowStep.OperationType operationType) {
    try {
      ItemWorkflow workflow = getItemWorkflowById(itemWorkflowId);
      if (workflow == null) {
        log.warn("ItemWorkflow with ID {} not found", itemWorkflowId);
        return null;
      }

      for (ItemWorkflowStep step : workflow.getItemWorkflowSteps()) {
        if (step.getRelatedEntityIds() != null && step.getRelatedEntityIds().contains(entityId) && step.getOperationType() == operationType) {
          log.debug("Found ItemWorkflowStep {} containing entity {} in workflow {}",
                    step.getOperationType(), entityId, itemWorkflowId);
          return step;
        }
      }

      log.warn("No ItemWorkflowStep found containing entity {} in workflow {}", entityId, itemWorkflowId);
      return null;
    } catch (Exception e) {
      log.error("Error finding ItemWorkflowStep by entity ID {} in workflow {}: {}",
                entityId, itemWorkflowId, e.getMessage());
      return null;
    }
  }

  public ItemWorkflowStep findForgingItemWorkflowStep(ItemWorkflow workflow) {
    try {
      for (ItemWorkflowStep step : workflow.getItemWorkflowSteps()) {
        if (step.getOperationType() == WorkflowStep.OperationType.FORGING) {
          log.debug("Found FORGING ItemWorkflowStep in workflow {}", workflow.getWorkflowIdentifier());
          return step;
        }
      }

      log.warn("No ItemWorkflowStep found containing in workflow {}", workflow.getWorkflowIdentifier());
      return null;
    } catch (Exception e) {
      log.error("Error finding ItemWorkflowStep in workflow {}: {}", workflow.getWorkflowIdentifier(), e.getMessage());
      return null;
    }
  }

  /**
   * Overloaded method that finds an ItemWorkflowStep by related entity ID without specifying operation type.
   * This is useful when the operation type is not known or when searching across all operation types.
   * Returns the first ItemWorkflowStep that contains the specified entity ID.
   *
   * @param itemWorkflowId The workflow ID
   * @param entityId       The entity ID to find in relatedEntityIds
   * @return The first ItemWorkflowStep containing the entity ID, or null if not found
   */
  public ItemWorkflowStep findItemWorkflowStepByRelatedEntityId(Long itemWorkflowId, Long entityId) {
    try {
      ItemWorkflow workflow = getItemWorkflowById(itemWorkflowId);
      if (workflow == null) {
        log.warn("ItemWorkflow with ID {} not found", itemWorkflowId);
        return null;
      }

      for (ItemWorkflowStep step : workflow.getItemWorkflowSteps()) {
        if (step.getRelatedEntityIds() != null && step.getRelatedEntityIds().contains(entityId)) {
          log.debug("Found ItemWorkflowStep {} containing entity {} in workflow {}",
                    step.getOperationType(), entityId, itemWorkflowId);
          return step;
        }
      }

      log.warn("No ItemWorkflowStep found containing entity {} in workflow {}", entityId, itemWorkflowId);
      return null;
    } catch (Exception e) {
      log.error("Error finding ItemWorkflowStep by entity ID {} in workflow {}: {}",
                entityId, itemWorkflowId, e.getMessage());
      return null;
    }
  }

  /**
   * Overloaded version that takes the specific ItemWorkflowStep directly
   * instead of searching for it by operation type
   */
  @Transactional
  public void updateAvailablePiecesInSpecificPreviousOperation(ItemWorkflowStep previousOperationStep, Long specificOperationId, int piecesToConsume) {
    if (previousOperationStep == null) {
      log.warn("previousOperationStep is null, cannot update available pieces");
      return;
    }

    try {
      if (previousOperationStep.getOperationOutcomeData() == null || previousOperationStep.getOperationOutcomeData().trim().isEmpty()) {
        log.warn("No operation outcome data found to update in step {}", previousOperationStep.getId());
        return;
      }

      OperationOutcomeData outcomeData = objectMapper.readValue(previousOperationStep.getOperationOutcomeData(), OperationOutcomeData.class);

      boolean updated = false;

      // Handle different operation types and update pieces in the specific operation
      if (previousOperationStep.getOperationType() == WorkflowStep.OperationType.FORGING) {
        if (outcomeData.getForgingData() != null && specificOperationId.equals(outcomeData.getForgingData().getId())) {
          int currentAvailable = outcomeData.getForgingData().getPiecesAvailableForNext();
          outcomeData.getForgingData().setPiecesAvailableForNext(Math.max(0, currentAvailable - piecesToConsume));
          updated = true;
        }
      } else {
        // For batch operations
        if (outcomeData.getBatchData() != null) {
          for (OperationOutcomeData.BatchOutcome batch : outcomeData.getBatchData()) {
            if (specificOperationId.equals(batch.getId())) {
              int currentAvailable = batch.getPiecesAvailableForNext();
              if (currentAvailable >= piecesToConsume) {
                batch.setPiecesAvailableForNext(currentAvailable - piecesToConsume);
                updated = true;
                log.info("Consumed {} pieces from batch operation {}, remaining: {}",
                         piecesToConsume, specificOperationId, currentAvailable - piecesToConsume);
                break;
              } else {
                throw new IllegalArgumentException("Insufficient pieces available in batch operation " + specificOperationId);
              }
            }
          }
        }
      }

      if (updated) {
        String updatedOutcomeData = objectMapper.writeValueAsString(outcomeData);
        previousOperationStep.setOperationOutcomeData(updatedOutcomeData);

        // Recalculate total pieces available for next
        int totalPiecesAvailable = 0;
        if (previousOperationStep.getOperationType() == WorkflowStep.OperationType.FORGING) {
          if (outcomeData.getForgingData() != null) {
            totalPiecesAvailable = outcomeData.getForgingData().getPiecesAvailableForNext();
          }
        } else {
          if (outcomeData.getBatchData() != null) {
            totalPiecesAvailable = outcomeData.getBatchData().stream()
                .mapToInt(OperationOutcomeData.BatchOutcome::getPiecesAvailableForNext)
                .sum();
          }
        }
        previousOperationStep.setPiecesAvailableForNext(totalPiecesAvailable);

        // Save the updated workflow (which will cascade to save the step)
        ItemWorkflow workflow = previousOperationStep.getItemWorkflow();
        itemWorkflowRepository.save(workflow);

        log.info("Successfully updated available pieces for operation {} in step {} (consumed {})",
                 specificOperationId, previousOperationStep.getOperationType(), piecesToConsume);
      } else {
        log.warn("Could not find specific operation {} to update pieces in step {}",
                 specificOperationId, previousOperationStep.getOperationType());
      }

    } catch (Exception e) {
      log.error("Error updating available pieces for operation {} in step {}: {}",
                specificOperationId, previousOperationStep.getOperationType(), e.getMessage());
      throw new RuntimeException("Failed to update available pieces: " + e.getMessage());
    }
  }

  /**
   * Updates the relatedEntityIds for a specific ItemWorkflowStep (used in tree-based workflows)
   */
  @Transactional
  public void updateRelatedEntityIdsForSpecificStep(ItemWorkflowStep itemWorkflowStep, Long entityId) {
    try {
      if (itemWorkflowStep == null) {
        log.warn("itemWorkflowStep is null, cannot update relatedEntityIds");
        return;
      }

      List<Long> relatedEntityIds = itemWorkflowStep.getRelatedEntityIds();
      if (relatedEntityIds == null) {
        relatedEntityIds = new ArrayList<>();
      }

      if (!relatedEntityIds.contains(entityId)) {
        relatedEntityIds.add(entityId);
        itemWorkflowStep.setRelatedEntityIds(relatedEntityIds);

        // Save the updated workflow (which will cascade to save the step)
        ItemWorkflow workflow = itemWorkflowStep.getItemWorkflow();
        itemWorkflowRepository.save(workflow);

        log.info("Successfully added entity {} to relatedEntityIds of ItemWorkflowStep {} ({})",
                 entityId, itemWorkflowStep.getId(), itemWorkflowStep.getOperationType());
      } else {
        log.debug("Entity {} already exists in relatedEntityIds of ItemWorkflowStep {}",
                  entityId, itemWorkflowStep.getId());
      }

    } catch (Exception e) {
      log.error("Error updating relatedEntityIds for ItemWorkflowStep {}: {}",
                itemWorkflowStep.getId(), e.getMessage());
      throw new RuntimeException("Failed to update relatedEntityIds: " + e.getMessage(), e);
    }
  }

  /**
   * Updates the operation outcome data for a specific ItemWorkflowStep (used in tree-based workflows)
   */
  @Transactional
  public void updateWorkflowStepForSpecificStep(ItemWorkflowStep itemWorkflowStep, OperationOutcomeData operationOutcomeData) {
    try {
      if (itemWorkflowStep == null) {
        log.warn("itemWorkflowStep is null, cannot update operation outcome data");
        return;
      }

      String outcomeDataJson = objectMapper.writeValueAsString(operationOutcomeData);
      itemWorkflowStep.setOperationOutcomeData(outcomeDataJson);

      // Save the updated workflow (which will cascade to save the step)
      ItemWorkflow workflow = itemWorkflowStep.getItemWorkflow();
      itemWorkflowRepository.save(workflow);

      log.info("Successfully updated operation outcome data for ItemWorkflowStep {} ({})",
               itemWorkflowStep.getId(), itemWorkflowStep.getOperationType());

    } catch (Exception e) {
      log.error("Error updating operation outcome data for ItemWorkflowStep {}: {}",
                itemWorkflowStep.getId(), e.getMessage());
      throw new RuntimeException("Failed to update operation outcome data: " + e.getMessage(), e);
    }
  }

  /**
   * Efficiently validates and consumes pieces from a parent operation in a single optimized call.
   * This method combines the functionality of finding parent step, validating available pieces,
   * and consuming pieces to eliminate redundant database lookups and workflow traversals.
   *
   * @param itemWorkflowId The workflow ID
   * @param currentOperationType The current operation type that's consuming pieces
   * @param parentEntityId The entity ID that should exist in the parent step's relatedEntityIds
   * @param piecesToConsume The number of pieces to consume from the parent operation
   * @return The parent ItemWorkflowStep that was found and updated
   * @throws IllegalArgumentException if insufficient pieces are available or parent step not found
   */
  @Transactional
  public ItemWorkflowStep validateAndConsumePiecesFromParentOperation(Long itemWorkflowId,
                                                                       WorkflowStep.OperationType currentOperationType,
                                                                       Long parentEntityId,
                                                                       int piecesToConsume) {
    try {
      log.debug("Efficiently validating and consuming {} pieces from parent operation for entity {} in workflow {}",
                piecesToConsume, parentEntityId, itemWorkflowId);

      // Step 1: Find the parent operation step that contains the specified entity (single lookup)
      // Use the overloaded method without operation type since we want to find any step containing this entity
      ItemWorkflowStep parentOperationStep = findItemWorkflowStepByRelatedEntityId(itemWorkflowId, parentEntityId);

      if (parentOperationStep == null) {
        log.error("No parent operation step found containing entity {} in workflow {}", parentEntityId, itemWorkflowId);
        throw new IllegalArgumentException("No parent operation step found containing entity " + parentEntityId);
      }

      log.info("Found parent operation step: {} for entity: {} in workflow: {}",
               parentOperationStep.getOperationType(), parentEntityId, itemWorkflowId);

      // Step 2: Get and validate available pieces (using the already found step)
      int currentAvailablePieces = getAvailablePiecesFromSpecificPreviousOperationOfItemWorkflowStep(
          parentOperationStep, parentEntityId);

      if (currentAvailablePieces < piecesToConsume) {
        log.error("Insufficient pieces available. Available: {}, Required: {}, Parent Entity: {}, Operation: {}",
                  currentAvailablePieces, piecesToConsume, parentEntityId, parentOperationStep.getOperationType());
        throw new IllegalArgumentException("Piece count (" + piecesToConsume + ") exceeds available pieces (" + 
                                           currentAvailablePieces + ") from parent operation " + parentEntityId);
      }

      // Step 3: Consume pieces from the parent operation (using the already found step)
      updateAvailablePiecesInSpecificPreviousOperation(parentOperationStep, parentEntityId, piecesToConsume);

      log.info("Successfully consumed {} pieces from {} operation {} for {} in workflow {}",
               piecesToConsume, parentOperationStep.getOperationType(), parentEntityId, 
               currentOperationType, itemWorkflowId);

      return parentOperationStep;

    } catch (Exception e) {
      log.error("Failed to validate and consume pieces from parent operation for {} in workflow {}: {}",
                currentOperationType, itemWorkflowId, e.getMessage());
      throw e; // Re-throw to maintain transaction rollback behavior
    }
  }

  /**
   * Consolidates duplicate workflow steps with the same operation type by merging their related entity IDs.
   * This prevents UI duplication when multiple workflow steps of the same type exist (e.g., multiple DISPATCH steps).
   * 
   * @param workflowRepresentation The original workflow representation
   * @return Consolidated workflow representation with merged duplicate steps
   */
  private ItemWorkflowRepresentation consolidateDuplicateWorkflowSteps(ItemWorkflowRepresentation workflowRepresentation) {
    if (workflowRepresentation == null || workflowRepresentation.getWorkflowSteps() == null) {
      return workflowRepresentation;
    }

    List<ItemWorkflowStepRepresentation> originalSteps = workflowRepresentation.getWorkflowSteps();
    Map<String, ItemWorkflowStepRepresentation> consolidatedStepsMap = new LinkedHashMap<>();

    for (ItemWorkflowStepRepresentation step : originalSteps) {
      String operationType = step.getOperationType();
      
      if (consolidatedStepsMap.containsKey(operationType)) {
        // Merge with existing step of the same operation type
        ItemWorkflowStepRepresentation existingStep = consolidatedStepsMap.get(operationType);
        mergeWorkflowSteps(existingStep, step);
        log.debug("Merged duplicate {} workflow step {} into step {}", 
                 operationType, step.getId(), existingStep.getId());
      } else {
        // First occurrence of this operation type
        consolidatedStepsMap.put(operationType, deepCopyWorkflowStep(step));
      }
    }

    // Create new workflow representation with consolidated steps
    List<ItemWorkflowStepRepresentation> consolidatedSteps = new ArrayList<>(consolidatedStepsMap.values());
    
    ItemWorkflowRepresentation consolidatedWorkflow = ItemWorkflowRepresentation.builder()
        .id(workflowRepresentation.getId())
        .workflowIdentifier(workflowRepresentation.getWorkflowIdentifier())
        .itemId(workflowRepresentation.getItemId())
        .itemName(workflowRepresentation.getItemName())
        .workflowTemplateId(workflowRepresentation.getWorkflowTemplateId())
        .workflowTemplateName(workflowRepresentation.getWorkflowTemplateName())
        .workflowStatus(workflowRepresentation.getWorkflowStatus())
        .currentOperation(workflowRepresentation.getCurrentOperation())
        .nextOperation(workflowRepresentation.getNextOperation())
        .startedAt(workflowRepresentation.getStartedAt())
        .completedAt(workflowRepresentation.getCompletedAt())
        .createdAt(workflowRepresentation.getCreatedAt())
        .updatedAt(workflowRepresentation.getUpdatedAt())
        .workflowSteps(consolidatedSteps)
        .build();

    log.info("Consolidated workflow steps: {} original steps -> {} consolidated steps", 
             originalSteps.size(), consolidatedSteps.size());
    
    return consolidatedWorkflow;
  }

  /**
   * Merges the second workflow step into the first one, combining related entity IDs and updating piece counts.
   */
  private void mergeWorkflowSteps(ItemWorkflowStepRepresentation targetStep, ItemWorkflowStepRepresentation sourceStep) {
    // Merge related entity IDs (avoid duplicates)
    if (targetStep.getRelatedEntityIds() == null) {
      targetStep.setRelatedEntityIds(new ArrayList<>());
    }
    if (sourceStep.getRelatedEntityIds() != null) {
      for (Long entityId : sourceStep.getRelatedEntityIds()) {
        if (!targetStep.getRelatedEntityIds().contains(entityId)) {
          targetStep.getRelatedEntityIds().add(entityId);
        }
      }
    }

    // Update piece counts by taking the sum or latest values as appropriate
    if (sourceStep.getInitialPiecesCount() != null) {
      Integer currentInitial = targetStep.getInitialPiecesCount() != null ? targetStep.getInitialPiecesCount() : 0;
      targetStep.setInitialPiecesCount(currentInitial + sourceStep.getInitialPiecesCount());
    }

    if (sourceStep.getPiecesAvailableForNext() != null) {
      Integer currentAvailable = targetStep.getPiecesAvailableForNext() != null ? targetStep.getPiecesAvailableForNext() : 0;
      targetStep.setPiecesAvailableForNext(currentAvailable + sourceStep.getPiecesAvailableForNext());
    }

    if (sourceStep.getConsumedPiecesCount() != null) {
      Integer currentConsumed = targetStep.getConsumedPiecesCount() != null ? targetStep.getConsumedPiecesCount() : 0;
      targetStep.setConsumedPiecesCount(currentConsumed + sourceStep.getConsumedPiecesCount());
    }

    // Use the earliest started time and latest completed time
    if (sourceStep.getStartedAt() != null && 
        (targetStep.getStartedAt() == null || sourceStep.getStartedAt().compareTo(targetStep.getStartedAt()) < 0)) {
      targetStep.setStartedAt(sourceStep.getStartedAt());
    }

    if (sourceStep.getCompletedAt() != null && 
        (targetStep.getCompletedAt() == null || sourceStep.getCompletedAt().compareTo(targetStep.getCompletedAt()) > 0)) {
      targetStep.setCompletedAt(sourceStep.getCompletedAt());
    }

    // Update status to the most progressed status
    if (sourceStep.getStepStatus() != null) {
      String currentStatus = targetStep.getStepStatus();
      String sourceStatus = sourceStep.getStepStatus();
      
      // Status priority: COMPLETED > IN_PROGRESS > PENDING
      if ("COMPLETED".equals(sourceStatus) || 
          ("IN_PROGRESS".equals(sourceStatus) && !"COMPLETED".equals(currentStatus))) {
        targetStep.setStepStatus(sourceStatus);
      }
    }

    // Merge operation outcome data if needed
    if (sourceStep.getOperationOutcomeData() != null && targetStep.getOperationOutcomeData() == null) {
      targetStep.setOperationOutcomeData(sourceStep.getOperationOutcomeData());
    }
  }

  /**
   * Creates a deep copy of a workflow step to avoid modifying the original.
   */
  private ItemWorkflowStepRepresentation deepCopyWorkflowStep(ItemWorkflowStepRepresentation original) {
    ItemWorkflowStepRepresentation copy = new ItemWorkflowStepRepresentation();
    copy.setId(original.getId());
    copy.setItemWorkflowId(original.getItemWorkflowId());
    copy.setWorkflowStepId(original.getWorkflowStepId());
    copy.setParentItemWorkflowStepId(original.getParentItemWorkflowStepId());
    copy.setTreeLevel(original.getTreeLevel());
    copy.setOperationType(original.getOperationType());
    copy.setStepStatus(original.getStepStatus());
    copy.setStartedAt(original.getStartedAt());
    copy.setCompletedAt(original.getCompletedAt());
    copy.setOperationReferenceId(original.getOperationReferenceId());
    copy.setOperationOutcomeData(original.getOperationOutcomeData());
    copy.setInitialPiecesCount(original.getInitialPiecesCount());
    copy.setPiecesAvailableForNext(original.getPiecesAvailableForNext());
    copy.setConsumedPiecesCount(original.getConsumedPiecesCount());
    copy.setPiecesUtilizationPercentage(original.getPiecesUtilizationPercentage());
    copy.setNotes(original.getNotes());
    copy.setCreatedAt(original.getCreatedAt());
    copy.setUpdatedAt(original.getUpdatedAt());
    copy.setParentWorkflowStepId(original.getParentWorkflowStepId());
    copy.setWorkflowTreeLevel(original.getWorkflowTreeLevel());
    copy.setIsOptional(original.getIsOptional());
    copy.setStepDescription(original.getStepDescription());
    
    // Deep copy related entity IDs list
    if (original.getRelatedEntityIds() != null) {
      copy.setRelatedEntityIds(new ArrayList<>(original.getRelatedEntityIds()));
    }
    
    return copy;
  }
}