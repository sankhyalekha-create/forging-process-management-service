package com.jangid.forging_process_management_service.entitiesRepresentation.order;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.jangid.forging_process_management_service.entitiesRepresentation.product.ItemRepresentation;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ApiModel(description = "Order Item Representation")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderItemRepresentation {

  @JsonProperty("id")
  @ApiModelProperty(value = "Order Item ID", example = "456")
  private Long id;

  @JsonProperty("orderId")
  @ApiModelProperty(value = "Order ID", example = "123")
  private Long orderId;

  @JsonProperty("itemId")
  @ApiModelProperty(value = "Item ID", example = "789", required = true)
  private Long itemId;

  @JsonProperty("itemName")
  @ApiModelProperty(value = "Item Name", example = "Forged Component A")
  private String itemName;

  @JsonProperty("itemCode")
  @ApiModelProperty(value = "Item Code", example = "FCA-001")
  private String itemCode;

  // Fields for item creation during order creation
  @JsonProperty("newItem")
  @ApiModelProperty(value = "New Item details (if creating new item during order creation)")
  private ItemRepresentation newItem;

  @JsonProperty("quantity")
  @ApiModelProperty(value = "Quantity", example = "100", required = true)
  private Integer quantity;

  @JsonProperty("workType")
  @ApiModelProperty(value = "Work Type: JOB_WORK_ONLY or WITH_MATERIAL", example = "WITH_MATERIAL", required = true)
  private String workType;

  @JsonProperty("unitPrice")
  @ApiModelProperty(value = "Total Unit Price (calculated based on work type)", example = "1500.00")
  private BigDecimal unitPrice;

  @JsonProperty("materialCostPerUnit")
  @ApiModelProperty(value = "Material Cost Per Unit (for WITH_MATERIAL)", example = "1000.00")
  private BigDecimal materialCostPerUnit;

  @JsonProperty("jobWorkCostPerUnit")
  @ApiModelProperty(value = "Job Work (Processing) Cost Per Unit", example = "500.00")
  private BigDecimal jobWorkCostPerUnit;

  @JsonProperty("costBreakdown")
  @ApiModelProperty(value = "Cost Breakdown Description", example = "Material: 1000.00 + Job Work: 500.00 = Total: 1500.00 per unit")
  private String costBreakdown;

  @JsonProperty("totalValue")
  @ApiModelProperty(value = "Total Value (quantity * unitPrice)", example = "150000.00")
  private BigDecimal totalValue;

  @JsonProperty("specialInstructions")
  @ApiModelProperty(value = "Special Instructions")
  private String specialInstructions;

  // Fields for linking to existing ItemWorkflow during order creation
  @JsonProperty("itemWorkflowId")
  @ApiModelProperty(value = "Item Workflow ID (link to existing ItemWorkflow) - REQUIRED", example = "789", required = true)
  @NotNull(message = "ItemWorkflow ID is required. Please create or select an ItemWorkflow before creating the order.")
  private Long itemWorkflowId;

  @JsonProperty("plannedDurationDays")
  @ApiModelProperty(value = "Planned Duration in Days (for OrderItemWorkflow)", example = "15")
  private Integer plannedDurationDays;

  @JsonProperty("priority")
  @ApiModelProperty(value = "Priority (1=highest, for OrderItemWorkflow)", example = "1")
  private Integer priority;

  @JsonProperty("orderItemWorkflows")
  @ApiModelProperty(value = "List of Order Item Workflows")
  private List<OrderItemWorkflowRepresentation> orderItemWorkflows;

  @JsonProperty("createdAt")
  @ApiModelProperty(value = "Created At")
  private LocalDateTime createdAt;

  @JsonProperty("updatedAt")
  @ApiModelProperty(value = "Updated At")
  private LocalDateTime updatedAt;

  // Additional computed fields for UI
  @JsonProperty("workflowCount")
  @ApiModelProperty(value = "Number of workflows for this item")
  private Integer workflowCount;

  @JsonProperty("completedWorkflowCount")
  @ApiModelProperty(value = "Number of completed workflows")
  private Integer completedWorkflowCount;

  @JsonProperty("workflowProgress")
  @ApiModelProperty(value = "Workflow progress percentage", example = "75.0")
  private Double workflowProgress;

  @JsonProperty("hasWorkflowsInProgress")
  @ApiModelProperty(value = "Whether item has workflows in progress")
  private Boolean hasWorkflowsInProgress;

  @JsonProperty("allWorkflowsCompleted")
  @ApiModelProperty(value = "Whether all workflows are completed")
  private Boolean allWorkflowsCompleted;

  @JsonProperty("workflowProgressSummary")
  @ApiModelProperty(value = "Workflow progress summary", example = "3/4 completed")
  private String workflowProgressSummary;

  // Step-level progress tracking for more granular insights
  @JsonProperty("totalStepCount")
  @ApiModelProperty(value = "Total number of workflow steps across all workflows")
  private Integer totalStepCount;

  @JsonProperty("completedStepCount")
  @ApiModelProperty(value = "Number of completed workflow steps")
  private Integer completedStepCount;

  @JsonProperty("inProgressStepCount")
  @ApiModelProperty(value = "Number of in-progress workflow steps")
  private Integer inProgressStepCount;

  @JsonProperty("pendingStepCount")
  @ApiModelProperty(value = "Number of pending workflow steps")
  private Integer pendingStepCount;

  @JsonProperty("stepProgress")
  @ApiModelProperty(value = "Step-level progress percentage", example = "65.5")
  private Double stepProgress;

  @JsonProperty("stepProgressSummary")
  @ApiModelProperty(value = "Step-level progress summary", example = "12/18 steps completed")
  private String stepProgressSummary;
}
