package com.jangid.forging_process_management_service.entitiesRepresentation.workflow;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ApiModel(description = "Item Workflow Representation")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ItemWorkflowRepresentation {

    @JsonProperty("id")
    @ApiModelProperty(value = "Workflow ID", example = "123")
    private Long id;

    @JsonProperty("workflowIdentifier")
    @ApiModelProperty(value = "Universal workflow identifier for tracking", example = "WORKFLOW_2024_001")
    private String workflowIdentifier;

    @JsonProperty("itemId")
    @ApiModelProperty(value = "Item ID", example = "456")
    private Long itemId;

    @JsonProperty("itemName")
    @ApiModelProperty(value = "Item name")
    private String itemName;

    @JsonProperty("workflowTemplateId")
    @ApiModelProperty(value = "Workflow template ID", example = "789")
    private Long workflowTemplateId;

    @JsonProperty("workflowTemplateName")
    @ApiModelProperty(value = "Workflow template name")
    private String workflowTemplateName;

    @JsonProperty("workflowStatus")
    @ApiModelProperty(value = "Overall workflow status")
    private String workflowStatus;

    @JsonProperty("currentOperation")
    @ApiModelProperty(value = "Current operation in progress")
    private String currentOperation;

    @JsonProperty("nextOperation")
    @ApiModelProperty(value = "Next available operation")
    private String nextOperation;

    @JsonProperty("startedAt")
    @ApiModelProperty(value = "Workflow start timestamp")
    private String startedAt;

    @JsonProperty("completedAt")
    @ApiModelProperty(value = "Workflow completion timestamp")
    private String completedAt;

    @JsonProperty("createdAt")
    @ApiModelProperty(value = "Creation timestamp")
    private String createdAt;

    @JsonProperty("updatedAt")
    @ApiModelProperty(value = "Last update timestamp")
    private String updatedAt;

    @JsonProperty("workflowSteps")
    @ApiModelProperty(value = "List of workflow steps")
    private List<ItemWorkflowStepRepresentation> workflowSteps;
} 