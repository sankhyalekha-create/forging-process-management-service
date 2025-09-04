package com.jangid.forging_process_management_service.dto.workflow;

import com.jangid.forging_process_management_service.entities.workflow.ItemWorkflowStep;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

/**
 * Context object to hold workflow operation information for batch processing.
 * This class encapsulates common workflow operation data that can be reused
 * across different service classes for consistent workflow integration.
 * 
 * <p>This class is designed to be used across various operation services such as:
 * <ul>
 *   <li>MachiningBatchService</li>
 *   <li>ForgingBatchService</li>
 *   <li>HeatTreatmentBatchService</li>
 *   <li>InspectionBatchService</li>
 *   <li>DispatchBatchService</li>
 * </ul>
 * 
 * <p>Usage examples:
 * <pre>{@code
 * // Example 1: Creating context for first operation
 * WorkflowOperationContext context = new WorkflowOperationContext(
 *     true,  // isFirstOperation
 *     firstWorkflowStep
 * );
 * 
 * // Example 2: Creating context for subsequent operation
 * WorkflowOperationContext context = new WorkflowOperationContext(
 *     false, // isFirstOperation
 *     targetWorkflowStep,
 *     previousOperationId
 * );
 * 
 * // Example 3: Using builder pattern
 * WorkflowOperationContext context = WorkflowOperationContext.builder()
 *     .setFirstOperation(false)
 *     .setTargetWorkflowStep(workflowStep)
 *     .setPreviousOperationProcessedItemId(previousId)
 *     .build();
 * }</pre>
 * 
 * @author FOPMAS Development Team
 * @since 1.0
 */

@Data
@Builder
@Getter
@Setter
public class WorkflowOperationContext {
    
    /**
     * Indicates whether this is the first operation in the workflow
     */
    private final boolean isFirstOperation;
    
    /**
     * The target workflow step for the operation
     */
    private final ItemWorkflowStep targetWorkflowStep;
    
    /**
     * The previous operation's processed item ID (null for first operations)
     */
    private final Long previousOperationProcessedItemId;

    /**
     * Constructor for creating a workflow operation context
     * 
     * @param isFirstOperation whether this is the first operation in the workflow
     * @param targetWorkflowStep the target workflow step for the operation
     * @param previousOperationProcessedItemId the previous operation's processed item ID (null for first operations)
     */
    public WorkflowOperationContext(boolean isFirstOperation, 
                                   ItemWorkflowStep targetWorkflowStep, 
                                   Long previousOperationProcessedItemId) {
        this.isFirstOperation = isFirstOperation;
        this.targetWorkflowStep = targetWorkflowStep;
        this.previousOperationProcessedItemId = previousOperationProcessedItemId;
    }

    /**
     * Simplified constructor for cases where previousOperationProcessedItemId is not needed
     * 
     * @param isFirstOperation whether this is the first operation in the workflow
     * @param targetWorkflowStep the target workflow step for the operation
     */
    public WorkflowOperationContext(boolean isFirstOperation, ItemWorkflowStep targetWorkflowStep) {
        this(isFirstOperation, targetWorkflowStep, null);
    }

    /**
     * @return true if this is the first operation in the workflow
     */
    public boolean isFirstOperation() {
        return isFirstOperation;
    }

    /**
     * @return the target workflow step for the operation
     */
    public ItemWorkflowStep getTargetWorkflowStep() {
        return targetWorkflowStep;
    }

    /**
     * @return the previous operation's processed item ID (null for first operations)
     */
    public Long getPreviousOperationProcessedItemId() {
        return previousOperationProcessedItemId;
    }

    /**
     * @return true if this context has a valid target workflow step
     */
    public boolean hasValidWorkflowStep() {
        return targetWorkflowStep != null;
    }

    /**
     * @return true if this is not the first operation and has a previous operation processed item ID
     */
    public boolean hasPreviousOperation() {
        return !isFirstOperation && previousOperationProcessedItemId != null;
    }

    @Override
    public String toString() {
        return "WorkflowOperationContext{" +
                "isFirstOperation=" + isFirstOperation +
                ", targetWorkflowStep=" + (targetWorkflowStep != null ? targetWorkflowStep.getId() : "null") +
                ", previousOperationProcessedItemId=" + previousOperationProcessedItemId +
                '}';
    }

}
