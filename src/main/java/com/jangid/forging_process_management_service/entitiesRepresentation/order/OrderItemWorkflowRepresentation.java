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

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ApiModel(description = "Order Item Workflow Representation")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderItemWorkflowRepresentation {

  @JsonProperty("id")
  @ApiModelProperty(value = "Order Item Workflow ID", example = "789")
  private Long id;

  @JsonProperty("orderItemId")
  @ApiModelProperty(value = "Order Item ID", example = "456")
  private Long orderItemId;

  // Pricing fields from OrderItem (to avoid circular reference with OrderItemRepresentation)
  @JsonProperty("unitPrice")
  @ApiModelProperty(value = "Unit Price", example = "1500.00")
  private Double unitPrice;

  @JsonProperty("materialCostPerUnit")
  @ApiModelProperty(value = "Material Cost Per Unit", example = "800.00")
  private Double materialCostPerUnit;

  @JsonProperty("jobWorkCostPerUnit")
  @ApiModelProperty(value = "Job Work Cost Per Unit", example = "700.00")
  private Double jobWorkCostPerUnit;

  @JsonProperty("workType")
  @ApiModelProperty(value = "Work Type", example = "WITH_MATERIAL")
  private String workType;

  @JsonProperty("itemWorkflowId")
  @ApiModelProperty(value = "Item Workflow ID", example = "101112")
  private Long itemWorkflowId;

  @JsonProperty("workflowIdentifier")
  @ApiModelProperty(value = "Workflow Identifier", example = "WF-2024-001")
  private String workflowIdentifier;

  @JsonProperty("workflowTemplateName")
  @ApiModelProperty(value = "Workflow Template Name", example = "Standard Forging Process")
  private String workflowTemplateName;

  @JsonProperty("workflowStatus")
  @ApiModelProperty(value = "Workflow Status", example = "IN_PROGRESS")
  private String workflowStatus;

  @JsonProperty("plannedDurationDays")
  @ApiModelProperty(value = "Planned Duration in Days", example = "15")
  private Integer plannedDurationDays;

  @JsonProperty("actualStartDate")
  @ApiModelProperty(value = "Actual Start Date", example = "2024-01-22")
  private LocalDate actualStartDate;

  @JsonProperty("actualCompletionDate")
  @ApiModelProperty(value = "Actual Completion Date", example = "2024-02-07")
  private LocalDate actualCompletionDate;

  @JsonProperty("actualDurationDays")
  @ApiModelProperty(value = "Actual Duration in Days", example = "17")
  private Integer actualDurationDays;

  @JsonProperty("notes")
  @ApiModelProperty(value = "Workflow Notes")
  private String notes;

  @JsonProperty("priority")
  @ApiModelProperty(value = "Priority (1=highest)", example = "1")
  private Integer priority;

  @JsonProperty("createdAt")
  @ApiModelProperty(value = "Created At")
  private LocalDateTime createdAt;

  @JsonProperty("updatedAt")
  @ApiModelProperty(value = "Updated At")
  private LocalDateTime updatedAt;

  // Additional computed fields for UI
  @JsonProperty("isStarted")
  @ApiModelProperty(value = "Whether workflow has started")
  private Boolean isStarted;

  @JsonProperty("isCompleted")
  @ApiModelProperty(value = "Whether workflow is completed")
  private Boolean isCompleted;

  @JsonProperty("isInProgress")
  @ApiModelProperty(value = "Whether workflow is in progress")
  private Boolean isInProgress;

  @JsonProperty("isDelayed")
  @ApiModelProperty(value = "Whether workflow is delayed")
  private Boolean isDelayed;

  @JsonProperty("remainingDays")
  @ApiModelProperty(value = "Remaining days to completion")
  private Integer remainingDays;

  @JsonProperty("statusSummary")
  @ApiModelProperty(value = "Status Summary", example = "In Progress")
  private String statusSummary;

  @JsonProperty("durationVariance")
  @ApiModelProperty(value = "Duration variance (actual - planned)", example = "2")
  private Integer durationVariance;

  @JsonProperty("currentOperation")
  @ApiModelProperty(value = "Current operation in progress")
  private String currentOperation;

  @JsonProperty("nextOperation")
  @ApiModelProperty(value = "Next available operation")
  private String nextOperation;

  @JsonProperty("completionPercentage")
  @ApiModelProperty(value = "Completion percentage", example = "65.5")
  private Double completionPercentage;
}
