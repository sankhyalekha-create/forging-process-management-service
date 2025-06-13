package com.jangid.forging_process_management_service.entitiesRepresentation.workflow;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jangid.forging_process_management_service.dto.workflow.OperationOutcomeData;
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
@ApiModel(description = "Item Workflow Step Representation")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ItemWorkflowStepRepresentation {

    @JsonProperty("id")
    @ApiModelProperty(value = "Workflow Step ID", example = "123")
    private Long id;

    @JsonProperty("itemWorkflowId")
    @ApiModelProperty(value = "Item Workflow ID", example = "456")
    private Long itemWorkflowId;

    @JsonProperty("workflowStepId")
    @ApiModelProperty(value = "Workflow Step Template ID", example = "789")
    private Long workflowStepId;

    @JsonProperty("operationType")
    @ApiModelProperty(value = "Operation type")
    private String operationType;

    @JsonProperty("stepStatus")
    @ApiModelProperty(value = "Step status")
    private String stepStatus;

    @JsonProperty("startedAt")
    @ApiModelProperty(value = "Step started timestamp")
    private String startedAt;

    @JsonProperty("completedAt")
    @ApiModelProperty(value = "Step completed timestamp")
    private String completedAt;

    @JsonProperty("operationReferenceId")
    @ApiModelProperty(value = "Reference to operation entity")
    private Long operationReferenceId;

    @JsonProperty("operationOutcomeData")
    @ApiModelProperty(value = "Operation outcome data as structured object")
    private OperationOutcomeData operationOutcomeData;

    @JsonProperty("relatedEntityIds")
    @ApiModelProperty(value = "List of IDs of operation-specific related entities")
    private List<Long> relatedEntityIds;

    @JsonProperty("initialPiecesCount")
    @ApiModelProperty(value = "Initial pieces produced by this operation")
    private Integer initialPiecesCount;

    @JsonProperty("piecesAvailableForNext")
    @ApiModelProperty(value = "Pieces available for next operation")
    private Integer piecesAvailableForNext;

    @JsonProperty("consumedPiecesCount")
    @ApiModelProperty(value = "Number of pieces consumed from this operation")
    private Integer consumedPiecesCount;

    @JsonProperty("piecesUtilizationPercentage")
    @ApiModelProperty(value = "Utilization percentage of pieces (0.0 to 1.0)")
    private Double piecesUtilizationPercentage;

    @JsonProperty("notes")
    @ApiModelProperty(value = "Additional notes for this step")
    private String notes;

    @JsonProperty("createdAt")
    @ApiModelProperty(value = "Step created timestamp")
    private String createdAt;

    @JsonProperty("updatedAt")
    @ApiModelProperty(value = "Step updated timestamp")
    private String updatedAt;

    // Workflow Step Template Information
    @JsonProperty("stepOrder")
    @ApiModelProperty(value = "Step order in workflow")
    private Integer stepOrder;

    @JsonProperty("isOptional")
    @ApiModelProperty(value = "Whether this step is optional")
    private Boolean isOptional;

    @JsonProperty("stepDescription")
    @ApiModelProperty(value = "Step description from template")
    private String stepDescription;
} 