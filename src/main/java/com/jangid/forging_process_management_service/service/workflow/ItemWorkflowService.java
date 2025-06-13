package com.jangid.forging_process_management_service.service.workflow;

import com.jangid.forging_process_management_service.entities.product.Item;
import com.jangid.forging_process_management_service.entities.workflow.ItemWorkflow;
import com.jangid.forging_process_management_service.entities.workflow.ItemWorkflowStep;
import com.jangid.forging_process_management_service.entities.workflow.WorkflowStep;
import com.jangid.forging_process_management_service.entities.workflow.WorkflowTemplate;
import com.jangid.forging_process_management_service.repositories.workflow.ItemWorkflowRepository;
import com.jangid.forging_process_management_service.entitiesRepresentation.workflow.ItemWorkflowRepresentation;
import com.jangid.forging_process_management_service.assemblers.workflow.ItemWorkflowAssembler;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.jangid.forging_process_management_service.dto.workflow.OperationOutcomeData;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Comparator;

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


  /**
   * Creates an item-level workflow (for overall item tracking)
   */
  @Transactional
  public ItemWorkflow createItemWorkflow(Item item, Long workflowTemplateId) {
    // Check if item already has an item-level workflow
    List<ItemWorkflow> existingWorkflows = itemWorkflowRepository.findByItemIdAndDeletedFalse(item.getId());
    Optional<ItemWorkflow> existingItemLevelWorkflow = existingWorkflows.stream()
        .filter(ItemWorkflow::isItemLevelWorkflow)
        .findFirst();

    if (existingItemLevelWorkflow.isPresent()) {
      throw new RuntimeException("Item already has an assigned workflow");
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
        .build();

    // This is an item-level workflow (workflowIdentifier and batchType remain null)

    // Create workflow step tracking entries
    for (WorkflowStep step : template.getWorkflowSteps()) {
      ItemWorkflowStep itemStep = ItemWorkflowStep.builder()
          .itemWorkflow(itemWorkflow)
          .workflowStep(step)
          .operationType(step.getOperationType())
          .stepStatus(ItemWorkflowStep.StepStatus.PENDING)
          .build();
      itemWorkflow.getItemWorkflowSteps().add(itemStep);
    }

    // Establish bidirectional relationship
    item.addWorkflow(itemWorkflow);

    return itemWorkflowRepository.save(itemWorkflow);
  }

  /**
   * Creates a batch-level workflow (for specific operation batches)
   */
  @Transactional
  public ItemWorkflow createItemWorkflow(Item item, Long workflowTemplateId, String workflowIdentifier) {
    // Check if this batch already has a workflow
    List<ItemWorkflow> existingWorkflows = itemWorkflowRepository.findByItemIdAndDeletedFalse(item.getId());
    Optional<ItemWorkflow> existingBatchWorkflow = existingWorkflows.stream()
        .filter(workflow -> workflow.isBatchLevelWorkflow() &&
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

    // Create workflow step tracking entries
    for (WorkflowStep step : template.getWorkflowSteps()) {
      ItemWorkflowStep itemStep = ItemWorkflowStep.builder()
          .itemWorkflow(itemWorkflow)
          .workflowStep(step)
          .operationType(step.getOperationType())
          .stepStatus(ItemWorkflowStep.StepStatus.PENDING)
          .build();
      itemWorkflow.getItemWorkflowSteps().add(itemStep);
    }

    // Establish bidirectional relationship
    item.addWorkflow(itemWorkflow);

    return itemWorkflowRepository.save(itemWorkflow);
  }

  /**
   * Gets the primary item-level workflow for an item
   */
  public ItemWorkflow getItemWorkflow(Long itemId) {
    List<ItemWorkflow> workflows = itemWorkflowRepository.findByItemIdAndDeletedFalse(itemId);
    Optional<ItemWorkflow> itemLevelWorkflow = workflows.stream()
        .filter(ItemWorkflow::isItemLevelWorkflow)
        .findFirst();

    if (itemLevelWorkflow.isPresent()) {
      return itemLevelWorkflow.get();
    }

    // No item-level workflow found - this is expected for items that have already been converted to batch workflows
    throw new RuntimeException("No item-level workflow found for item: " + itemId +
                               ". This item may have already been converted to batch-level workflows.");
  }

  public ItemWorkflow getItemWorkflowById(Long itemWorkflowId) {
    Optional<ItemWorkflow> itemWorkflowOptional = itemWorkflowRepository.findById(itemWorkflowId);

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
        .filter(workflow -> workflow.isBatchLevelWorkflow() &&
                            workflowIdentifier.equals(workflow.getWorkflowIdentifier()))
        .findFirst()
        .orElseThrow(() ->
                         new RuntimeException("No workflow found for batch: " + workflowIdentifier));
  }

  /**
   * Gets all workflows for an item (both item-level and batch-level)
   */
  public List<ItemWorkflow> getAllWorkflowsForItem(Long itemId) {
    return itemWorkflowRepository.findByItemIdAndDeletedFalse(itemId);
  }

  /**
   * Consumes pieces from a previous operation
   */
  @Transactional
  public boolean consumePiecesFromPreviousOperation(Long itemId, String workflowIdentifier,
                                                    WorkflowStep.OperationType sourceOperation,
                                                    WorkflowStep.OperationType targetOperation,
                                                    Integer piecesToConsume) {
    try {
      ItemWorkflow workflow = getItemWorkflowByWorkflowIdentifier(itemId, workflowIdentifier);
      boolean success = workflow.consumePiecesFromOperation(sourceOperation, targetOperation, piecesToConsume);

      if (success) {
        itemWorkflowRepository.save(workflow);
        log.info("Successfully consumed {} pieces from {} operation for {} operation in batch {}",
                 piecesToConsume, sourceOperation, targetOperation, workflowIdentifier);
      } else {
        log.warn("Failed to consume {} pieces from {} operation for {} operation in batch {}",
                 piecesToConsume, sourceOperation, targetOperation, workflowIdentifier);
      }

      return success;

    } catch (RuntimeException e) {
      log.error("Error consuming pieces from previous operation: {}", e.getMessage());
      return false;
    }
  }

  /**
   * Checks if an operation can be processed based on workflow dependencies
   * For batch-level operations, use the batch-specific methods
   */
  public boolean canProcessOperation(Long itemId, WorkflowStep.OperationType operationType) {
    try {
      // Try to get item-level workflow first
      List<ItemWorkflow> workflows = itemWorkflowRepository.findByItemIdAndDeletedFalse(itemId);
      if (workflows.isEmpty()) {
        // If no workflow exists, use legacy behavior for backward compatibility
        log.info("No workflow found for item {}, allowing operation {} for backward compatibility",
                 itemId, operationType);
        return true;
      }

      // Use first available workflow to check if operation can be processed
      ItemWorkflow workflow = workflows.get(0);
      return workflow.canStartOperation(operationType);
    } catch (RuntimeException e) {
      // If no workflow exists, use legacy behavior for backward compatibility
      log.info("No workflow found for item {}, allowing operation {} for backward compatibility",
               itemId, operationType);
      return true;
    }
  }


  public WorkflowStep.OperationType getNextAllowedOperation(Long itemId) {
    try {
      ItemWorkflow workflow = getItemWorkflow(itemId);
      List<ItemWorkflowStep> availableSteps = workflow.getAvailableSteps();
      if (!availableSteps.isEmpty()) {
        return availableSteps.get(0).getWorkflowStep().getOperationType();
      }
      return null;
    } catch (RuntimeException e) {
      // Legacy behavior - return null to indicate any operation is allowed
      return null;
    }
  }


  /**
   * Checks if an operation is the first step in a workflow template
   */
  public boolean isFirstOperationInWorkflow(Long workflowTemplateId, WorkflowStep.OperationType operationType) {
    WorkflowTemplate template = workflowTemplateService.getWorkflowTemplateById(workflowTemplateId);
    return template.getWorkflowSteps().stream()
        .min(Comparator.comparing(WorkflowStep::getStepOrder))
        .map(step -> step.getOperationType() == operationType)
        .orElse(false);
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


  /**
   * Consolidated workflow update method that combines incremental pieces update
   * and complete outcome data update into a single transaction
   *
   * @param itemWorkflowId    The workflow ID to update
   * @param operationType     The operation type (FORGING, HEAT_TREATMENT, etc.)
   * @param outcomeData       Complete operation outcome data with totals
   */
  @Transactional
  public void updateWorkflowStepForOperation(Long itemWorkflowId,
                                             WorkflowStep.OperationType operationType,
                                             OperationOutcomeData outcomeData) {
    try {
      ItemWorkflow workflow = getItemWorkflowById(itemWorkflowId);

      // Use the new operationType column for more efficient querying
      ItemWorkflowStep operationStep = workflow.getItemWorkflowSteps().stream()
          .filter(step -> step.getOperationType() == operationType)
          .findFirst()
          .orElse(null);

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
        if (firstWorkflow.getItem().getTenant().getId() != tenantId.longValue()) {
          throw new RuntimeException("Item does not belong to the specified tenant");
        }
      }
      
      return workflows.stream()
          .filter(workflow -> workflow.getWorkflowStatus() == ItemWorkflow.WorkflowStatus.IN_PROGRESS)
          .filter(workflow -> workflow.getWorkflowIdentifier() != null) // Only batch-level workflows
          .map(itemWorkflowAssembler::dissembleActiveWorkflow)
          .filter(representation -> representation.getCurrentOperation() != null && representation.getNextOperation() != null)
          .collect(Collectors.toList());
          
    } catch (Exception e) {
      log.error("Error fetching active workflows for item {} in tenant {}: {}", itemId, tenantId, e.getMessage());
      throw new RuntimeException("Failed to fetch active workflows: " + e.getMessage(), e);
    }
  }

  /**
   * Utility method to get ItemWorkflowStep by itemWorkflowId and operationType
   * @param itemWorkflowId The workflow ID
   * @param operationType The operation type to find
   * @return The ItemWorkflowStep or null if not found
   */
  public ItemWorkflowStep getWorkflowStepByOperation(Long itemWorkflowId, WorkflowStep.OperationType operationType) {
    try {
      if (itemWorkflowId == null) {
        log.warn("itemWorkflowId is null, cannot fetch workflow step");
        return null;
      }
      
      ItemWorkflow workflow = getItemWorkflowById(itemWorkflowId);
      
      return workflow.getItemWorkflowSteps().stream()
          .filter(step -> step.getOperationType() == operationType)
          .findFirst()
          .orElse(null);
          
    } catch (Exception e) {
      log.error("Failed to fetch workflow step for operation {} in workflow {}: {}", 
               operationType, itemWorkflowId, e.getMessage());
      return null;
    }
  }

  /**
   * Utility method to get pieces available for next operation from a workflow step
   * @param itemWorkflowId The workflow ID
   * @param operationType The operation type to get pieces from
   * @return The number of pieces available for next operation, or 0 if not found/error
   */
  public int getPiecesAvailableForNextFromOperation(Long itemWorkflowId, WorkflowStep.OperationType operationType) {
    try {
      ItemWorkflowStep workflowStep = getWorkflowStepByOperation(itemWorkflowId, operationType);
      
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
   * Gets the previous operation type and its available operation IDs
   * @param itemWorkflowId The workflow ID
   * @param currentOperationType The current operation type
   * @return A map containing operation type and list of available operation IDs
   */
  public Map<String, Object> getPreviousOperationInfo(Long itemWorkflowId, WorkflowStep.OperationType currentOperationType) {
    Map<String, Object> result = new HashMap<>();
    
    try {
      ItemWorkflowStep previousOperationStep = getPreviousOperationStep(itemWorkflowId, currentOperationType);
      
      if (previousOperationStep == null) {
        return result;
      }

      result.put("operationType", previousOperationStep.getOperationType());
      
      if (previousOperationStep.getOperationOutcomeData() == null || previousOperationStep.getOperationOutcomeData().trim().isEmpty()) {
        return result;
      }

      OperationOutcomeData outcomeData = objectMapper.readValue(previousOperationStep.getOperationOutcomeData(), OperationOutcomeData.class);
      java.util.List<Long> operationIds = new java.util.ArrayList<>();
      
      if (previousOperationStep.getOperationType() == WorkflowStep.OperationType.FORGING) {
        if (outcomeData.getForgingData() != null) {
          operationIds.add(outcomeData.getForgingData().getId());
        }
      } else {
        if (outcomeData.getBatchData() != null) {
          operationIds = outcomeData.getBatchData().stream()
              .map(batch -> batch.getId())
              .collect(Collectors.toList());
        }
      }
      
      result.put("operationIds", operationIds);
      return result;
      
    } catch (Exception e) {
      log.error("Error getting previous operation info for workflow {}: {}", itemWorkflowId, e.getMessage());
      return result;
    }
  }

  /**
   * Gets the previous operation step before the specified operation type in the workflow
   * @param itemWorkflowId The workflow ID
   * @param currentOperationType The current operation type to find previous operation for
   * @return The previous operation step or null if not found
   */
  public ItemWorkflowStep getPreviousOperationStep(Long itemWorkflowId, WorkflowStep.OperationType currentOperationType) {
    try {
      // Get the workflow to access workflow template and steps
      ItemWorkflow workflow = getItemWorkflowById(itemWorkflowId);
      
      // Find current operation step to get its step order
      ItemWorkflowStep currentStep = workflow.getItemWorkflowSteps().stream()
          .filter(step -> step.getOperationType() == currentOperationType)
          .findFirst()
          .orElse(null);

      if (currentStep == null) {
        log.warn("Current operation step {} not found in workflow {}", currentOperationType, itemWorkflowId);
        return null;
      }

      // Get the step order of current operation from workflow template
      int currentStepOrder = currentStep.getWorkflowStep().getStepOrder();
      
      // Find the previous step (step order = currentStepOrder - 1)
      return workflow.getItemWorkflowSteps().stream()
          .filter(step -> step.getWorkflowStep().getStepOrder() == currentStepOrder - 1)
          .filter(step -> step.getStepStatus() == ItemWorkflowStep.StepStatus.COMPLETED || 
                         step.getStepStatus() == ItemWorkflowStep.StepStatus.IN_PROGRESS)
          .findFirst()
          .orElse(null);

    } catch (Exception e) {
      log.error("Error getting previous operation step for {} in workflow {}: {}", currentOperationType, itemWorkflowId, e.getMessage());
      return null;
    }
  }

  /**
   * Updates available pieces in a specific operation/batch within the previous operation step
   * @param itemWorkflowId The workflow ID
   * @param currentOperationType The current operation type to find previous operation for
   * @param specificOperationId The specific operation/batch ID to consume pieces from
   * @param piecesToConsume Number of pieces to consume from the specific operation/batch
   */
  public void updateAvailablePiecesInSpecificPreviousOperation(Long itemWorkflowId, WorkflowStep.OperationType currentOperationType, Long specificOperationId, int piecesToConsume) {
    if (itemWorkflowId == null) {
      log.warn("itemWorkflowId is null, cannot update available pieces in previous operation");
      return;
    }

    try {
      // Get the previous operation step
      ItemWorkflowStep previousOperationStep = getPreviousOperationStep(itemWorkflowId, currentOperationType);
      
      if (previousOperationStep == null) {
        log.warn("No previous operation step found to update pieces for workflow {}", itemWorkflowId);
        return;
      }

      if (previousOperationStep.getOperationOutcomeData() == null || previousOperationStep.getOperationOutcomeData().trim().isEmpty()) {
        log.warn("No operation outcome data found to update in workflow {}", itemWorkflowId);
        return;
      }

      OperationOutcomeData outcomeData = objectMapper.readValue(previousOperationStep.getOperationOutcomeData(), OperationOutcomeData.class);
      boolean updated = false;
      
      // Handle different operation types
      if (previousOperationStep.getOperationType() == WorkflowStep.OperationType.FORGING) {
        if (outcomeData.getForgingData() != null && specificOperationId.equals(outcomeData.getForgingData().getId())) {
          int currentAvailable = outcomeData.getForgingData().getPiecesAvailableForNext();
          if (currentAvailable >= piecesToConsume) {
            outcomeData.getForgingData().setPiecesAvailableForNext(currentAvailable - piecesToConsume);
            updated = true;
            log.info("Consumed {} pieces from forging operation {} in workflow {}, remaining: {}", 
                     piecesToConsume, specificOperationId, itemWorkflowId, currentAvailable - piecesToConsume);
          } else {
            throw new IllegalArgumentException("Insufficient pieces available in forging operation " + specificOperationId);
          }
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
                log.info("Consumed {} pieces from batch operation {} in workflow {}, remaining: {}", 
                         piecesToConsume, specificOperationId, itemWorkflowId, currentAvailable - piecesToConsume);
                break;
              } else {
                throw new IllegalArgumentException("Insufficient pieces available in batch operation " + specificOperationId);
              }
            }
          }
        }
      }
      
      if (updated) {
        // Update the operation outcome data and total pieces available for next
        previousOperationStep.setOperationOutcomeData(objectMapper.writeValueAsString(outcomeData));
        
        // Recalculate total pieces available for next
        int totalPiecesAvailable = 0;
        if (previousOperationStep.getOperationType() == WorkflowStep.OperationType.FORGING) {
          if (outcomeData.getForgingData() != null) {
            totalPiecesAvailable = outcomeData.getForgingData().getPiecesAvailableForNext();
          }
        } else {
          if (outcomeData.getBatchData() != null) {
            totalPiecesAvailable = outcomeData.getBatchData().stream()
                .mapToInt(batch -> batch.getPiecesAvailableForNext())
                .sum();
          }
        }
        
        previousOperationStep.setPiecesAvailableForNext(totalPiecesAvailable);
        
        // Save the updated workflow
        ItemWorkflow workflow = getItemWorkflowById(itemWorkflowId);
        itemWorkflowRepository.save(workflow);
        
        log.info("Successfully updated operation outcome data for workflow {}, total pieces available: {}", 
                 itemWorkflowId, totalPiecesAvailable);
      } else {
        log.warn("No matching operation ID {} found to update in workflow {}", specificOperationId, itemWorkflowId);
        throw new IllegalArgumentException("Operation ID " + specificOperationId + " not found in previous operation data");
      }

    } catch (Exception e) {
      log.error("Error updating available pieces in specific previous operation for workflow {}: {}", itemWorkflowId, e.getMessage());
      throw new RuntimeException("Failed to update available pieces in specific previous operation: " + e.getMessage(), e);
    }
  }

  /**
   * Returns pieces back to a specific operation/batch within the previous operation step
   * This is the reverse of updateAvailablePiecesInSpecificPreviousOperation - used when operations are deleted/cancelled
   * @param itemWorkflowId The workflow ID
   * @param currentOperationType The current operation type to find previous operation for
   * @param specificOperationId The specific operation/batch ID to return pieces to
   * @param piecesToReturn Number of pieces to return to the specific operation/batch
   */
  public void returnPiecesToSpecificPreviousOperation(Long itemWorkflowId, WorkflowStep.OperationType currentOperationType, Long specificOperationId, int piecesToReturn) {
    if (itemWorkflowId == null) {
      log.warn("itemWorkflowId is null, cannot return pieces to previous operation");
      return;
    }

    try {
      // Get the previous operation step
      ItemWorkflowStep previousOperationStep = getPreviousOperationStep(itemWorkflowId, currentOperationType);
      
      if (previousOperationStep == null) {
        log.warn("No previous operation step found to return pieces for workflow {}", itemWorkflowId);
        return;
      }

      if (previousOperationStep.getOperationOutcomeData() == null || previousOperationStep.getOperationOutcomeData().trim().isEmpty()) {
        log.warn("No operation outcome data found to return pieces in workflow {}", itemWorkflowId);
        return;
      }

      OperationOutcomeData outcomeData = objectMapper.readValue(previousOperationStep.getOperationOutcomeData(), OperationOutcomeData.class);
      boolean updated = false;
      
      // Handle different operation types
      if (previousOperationStep.getOperationType() == WorkflowStep.OperationType.FORGING) {
        if (outcomeData.getForgingData() != null && specificOperationId.equals(outcomeData.getForgingData().getId())) {
          int currentAvailable = outcomeData.getForgingData().getPiecesAvailableForNext();
          outcomeData.getForgingData().setPiecesAvailableForNext(currentAvailable + piecesToReturn);
          updated = true;
          log.info("Returned {} pieces to forging operation {} in workflow {}, new total: {}", 
                   piecesToReturn, specificOperationId, itemWorkflowId, currentAvailable + piecesToReturn);
        }
      } else {
        // For batch operations
        if (outcomeData.getBatchData() != null) {
          for (OperationOutcomeData.BatchOutcome batch : outcomeData.getBatchData()) {
            if (specificOperationId.equals(batch.getId())) {
              int currentAvailable = batch.getPiecesAvailableForNext();
              batch.setPiecesAvailableForNext(currentAvailable + piecesToReturn);
              updated = true;
              log.info("Returned {} pieces to batch operation {} in workflow {}, new total: {}", 
                       piecesToReturn, specificOperationId, itemWorkflowId, currentAvailable + piecesToReturn);
              break;
            }
          }
        }
      }
      
      if (updated) {
        // Update the operation outcome data and total pieces available for next
        previousOperationStep.setOperationOutcomeData(objectMapper.writeValueAsString(outcomeData));
        
        // Recalculate total pieces available for next
        int totalPiecesAvailable = 0;
        if (previousOperationStep.getOperationType() == WorkflowStep.OperationType.FORGING) {
          if (outcomeData.getForgingData() != null) {
            totalPiecesAvailable = outcomeData.getForgingData().getPiecesAvailableForNext();
          }
        } else {
          if (outcomeData.getBatchData() != null) {
            totalPiecesAvailable = outcomeData.getBatchData().stream()
                .mapToInt(batch -> batch.getPiecesAvailableForNext())
                .sum();
          }
        }
        
        previousOperationStep.setPiecesAvailableForNext(totalPiecesAvailable);
        
        // Save the updated workflow
        ItemWorkflow workflow = getItemWorkflowById(itemWorkflowId);
        itemWorkflowRepository.save(workflow);
        
        log.info("Successfully returned pieces to operation outcome data for workflow {}, total pieces available: {}", 
                 itemWorkflowId, totalPiecesAvailable);
      } else {
        log.warn("No matching operation ID {} found to return pieces in workflow {}", specificOperationId, itemWorkflowId);
        throw new IllegalArgumentException("Operation ID " + specificOperationId + " not found in previous operation data");
      }

    } catch (Exception e) {
      log.error("Error returning pieces to specific previous operation for workflow {}: {}", itemWorkflowId, e.getMessage());
      throw new RuntimeException("Failed to return pieces to specific previous operation: " + e.getMessage(), e);
    }
  }

  /**
   * Generic method to update relatedEntityIds for any operation type in ItemWorkflowStep
   * This method can be used by all services (forge, heat treatment, machining, quality, dispatch)
   * to add related entity IDs to their respective workflow steps
   * 
   * @param itemWorkflowId The workflow ID containing the operation step to update
   * @param operationType The operation type (FORGING, HEAT_TREATMENT, MACHINING, QUALITY, DISPATCH)
   * @param entityIds List of entity IDs to add to relatedEntityIds (e.g., ProcessedItem.id, BatchId, etc.)
   */
  @Transactional
  public void updateRelatedEntityIds(Long itemWorkflowId, WorkflowStep.OperationType operationType, List<Long> entityIds) {
    try {
      if (itemWorkflowId == null) {
        log.warn("itemWorkflowId is null, cannot update relatedEntityIds");
        return;
      }
      
      if (entityIds == null || entityIds.isEmpty()) {
        log.warn("entityIds is null or empty, nothing to update for operation {} in workflow {}", 
                operationType, itemWorkflowId);
        return;
      }
      
      log.info("Updating relatedEntityIds for {} step in workflow {} with entity IDs: {}", 
               operationType, itemWorkflowId, entityIds);
      
      ItemWorkflow workflow = getItemWorkflowById(itemWorkflowId);
      
      // Find the operation step using the operationType column for efficient filtering
      ItemWorkflowStep operationStep = workflow.getItemWorkflowSteps().stream()
          .filter(step -> step.getOperationType() == operationType)
          .findFirst()
          .orElse(null);
        
      if (operationStep != null) {
        // Get existing relatedEntityIds or create new list if null
        List<Long> existingRelatedEntityIds = operationStep.getRelatedEntityIds();
        List<Long> updatedRelatedEntityIds = new ArrayList<>();
        
        if (existingRelatedEntityIds != null) {
          updatedRelatedEntityIds.addAll(existingRelatedEntityIds);
        }
        
        // Add new entity IDs if they're not already present (avoid duplicates)
        int addedCount = 0;
        for (Long entityId : entityIds) {
          if (!updatedRelatedEntityIds.contains(entityId)) {
            updatedRelatedEntityIds.add(entityId);
            addedCount++;
            log.debug("Adding entity ID {} to relatedEntityIds list for {} step", entityId, operationType);
          } else {
            log.debug("Entity ID {} already exists in relatedEntityIds list for {} step", entityId, operationType);
          }
        }
        
        // Set the updated list
        operationStep.setRelatedEntityIds(updatedRelatedEntityIds);
        
        // Save the updated workflow
        itemWorkflowRepository.save(workflow);
        
        log.info("Successfully updated relatedEntityIds for {} step in workflow {}. Added {} new entities. Total related entities: {}", 
                 operationType, itemWorkflowId, addedCount, updatedRelatedEntityIds.size());
      } else {
        log.warn("No {} step found in workflow {}", operationType, itemWorkflowId);
        throw new IllegalArgumentException("No " + operationType + " step found in workflow " + itemWorkflowId);
      }
    } catch (Exception e) {
      log.error("Failed to update relatedEntityIds for {} step in workflow {}: {}", 
               operationType, itemWorkflowId, e.getMessage());
      throw new RuntimeException("Failed to update relatedEntityIds for " + operationType + " step: " + e.getMessage(), e);
    }
  }

  /**
   * Convenience method to update relatedEntityIds with a single entity ID
   * 
   * @param itemWorkflowId The workflow ID containing the operation step to update
   * @param operationType The operation type (FORGING, HEAT_TREATMENT, MACHINING, QUALITY, DISPATCH)
   * @param entityId Single entity ID to add to relatedEntityIds
   */
  @Transactional
  public void updateRelatedEntityIds(Long itemWorkflowId, WorkflowStep.OperationType operationType, Long entityId) {
    if (entityId != null) {
      updateRelatedEntityIds(itemWorkflowId, operationType, List.of(entityId));
    }
  }

  /**
   * Generic method to handle workflow creation or retrieval for any operation type
   * This method can be used by all operation services (Forge, Heat Treatment, Machining, Quality, Dispatch)
   * 
   * @param item The item for which to handle the workflow
   * @param operationType The operation type (FORGING, HEAT_TREATMENT, MACHINING, QUALITY, DISPATCH)
   * @param operationId The ID of the operation entity (forge ID, batch ID, etc.)
   * @param workflowIdentifier The workflow identifier for this operation
   * @param existingItemWorkflowId Optional existing workflow ID to update
   * @return The ItemWorkflow (either existing or newly created)
   */
  @Transactional
  public ItemWorkflow handleWorkflowForOperation(Item item, 
                                                WorkflowStep.OperationType operationType, 
                                                Long operationId, 
                                                String workflowIdentifier, 
                                                Long existingItemWorkflowId) {
    Long itemId = item.getId();
    
    log.info("Handling workflow for {} operation on item {} with workflow identifier: {}", 
             operationType, itemId, workflowIdentifier);
    
    ItemWorkflow workflow;
    
    if (existingItemWorkflowId != null) {
      // Update existing workflow
      workflow = handleExistingWorkflow(existingItemWorkflowId, operationType, operationId, itemId);
    } else {
      // Handle first operation or new batch
      workflow = handleFirstOperationOrNewItemWorkflow(item, operationType, operationId, workflowIdentifier, itemId);
    }
    
    log.info("Successfully handled workflow for {} operation. Workflow ID: {}, Workflow Identifier: {}", 
             operationType, workflow.getId(), workflow.getWorkflowIdentifier());
    
    return workflow;
  }

  /**
   * Handles existing workflow case - validates and updates the workflow step
   */
  private ItemWorkflow handleExistingWorkflow(Long existingItemWorkflowId, 
                                             WorkflowStep.OperationType operationType, 
                                             Long operationId, 
                                             Long itemId) {
    ItemWorkflow workflow = itemWorkflowRepository.findById(existingItemWorkflowId)
        .orElseThrow(() -> new RuntimeException("ItemWorkflow not found with ID: " + existingItemWorkflowId));
    
    // Validate the workflow belongs to the item
    if (!workflow.getItem().getId().equals(itemId)) {
      throw new IllegalArgumentException("ItemWorkflow " + workflow.getId() + " does not belong to item " + itemId);
    }
    
    // Update the operation step status
    workflow.startOperationStep(operationType, operationId);
    workflow = itemWorkflowRepository.save(workflow);
    
    log.info("Updated existing workflow {} with {} operation for item {}", 
             workflow.getId(), operationType, itemId);
             
    return workflow;
  }

  /**
   * Handles first operation or new batch case - creates new workflow or updates initial workflow
   */
  private ItemWorkflow handleFirstOperationOrNewItemWorkflow(Item item,
                                                             WorkflowStep.OperationType operationType,
                                                             Long operationId,
                                                             String workflowIdentifier,
                                                             Long itemId) {
    // Validate workflow identifier is provided
    if (workflowIdentifier == null || workflowIdentifier.trim().isEmpty()) {
      log.error("Workflow identifier is required for {} operation", operationType);
      throw new IllegalArgumentException("Workflow identifier is required for " + operationType + " operation");
    }
    
    // Check if workflow identifier is already in use
    List<ItemWorkflow> existingWorkflows = getAllWorkflowsForItem(itemId);
    boolean workflowIdentifierExists = existingWorkflows.stream()
        .anyMatch(w -> workflowIdentifier.equals(w.getWorkflowIdentifier()));
    
    if (workflowIdentifierExists) {
      log.error("Workflow identifier {} is already in use for item {}", workflowIdentifier, itemId);
      throw new IllegalArgumentException("Workflow identifier " + workflowIdentifier + " is already in use");
    }

    ItemWorkflow workflow = null;
    
    // Check if this is the initial workflow without workflow identifier
    ItemWorkflow initialWorkflow = getInitialItemWorkflow(itemId);
    
    if (initialWorkflow != null) {
      // First-time item creation case - update existing workflow with workflow identifier
      workflow = updateInitialWorkflowWithWorkflowIdentifier(initialWorkflow, operationType, operationId, workflowIdentifier);
    } else {
      // Create new workflow for subsequent executions
      workflow = createNewItemWorkflow(item, operationType, operationId, workflowIdentifier, existingWorkflows);
    }
    
    return workflow;
  }

  /**
   * Updates initial workflow with workflow identifier and operation
   */
  private ItemWorkflow updateInitialWorkflowWithWorkflowIdentifier(ItemWorkflow workflow,
                                                                   WorkflowStep.OperationType operationType,
                                                                   Long operationId,
                                                                   String workflowIdentifier) {
    log.info("Found initial workflow without workflow identifier for item {}", workflow.getItem().getId());
    workflow.setWorkflowIdentifier(workflowIdentifier);
    workflow.startOperationStep(operationType, operationId);
    workflow = itemWorkflowRepository.save(workflow);
    
    log.info("Updated initial workflow with workflow identifier {} for item {} with {} operation", 
             workflowIdentifier, workflow.getItem().getId(), operationType);
             
    return workflow;
  }

  /**
   * Creates new workflow for subsequent executions
   */
  private ItemWorkflow createNewItemWorkflow(Item item,
                                             WorkflowStep.OperationType operationType,
                                             Long operationId,
                                             String workflowIdentifier,
                                             List<ItemWorkflow> existingWorkflows) {
    log.info("Creating new workflow for item {} with workflow identifier {} and {} operation", 
             item.getId(), workflowIdentifier, operationType);
    
    // Get workflow template ID from existing workflows
    Long workflowTemplateId = null;
    if (!existingWorkflows.isEmpty()) {
      workflowTemplateId = existingWorkflows.get(0).getWorkflowTemplate().getId();
    }
    
    if (workflowTemplateId == null) {
      log.error("No workflow template found for item {}", item.getId());
      throw new RuntimeException("No workflow template found for item " + item.getId());
    }
    
    // Create new workflow with provided workflow identifier
    ItemWorkflow workflow = createItemWorkflow(item, workflowTemplateId, workflowIdentifier);
    workflow.startOperationStep(operationType, operationId);
    workflow = itemWorkflowRepository.save(workflow);
    
    log.info("Created new workflow with workflow identifier {} for item {} with {} operation", 
             workflowIdentifier, item.getId(), operationType);
             
    return workflow;
  }

}