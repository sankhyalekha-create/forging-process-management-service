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

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ApiModel(description = "Workflow step representation")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WorkflowStepRepresentation {

    @JsonProperty("id")
    @ApiModelProperty(value = "ID of the workflow step", example = "1")
    private Long id;

    @JsonProperty("operationType")
    @ApiModelProperty(value = "Type of operation", example = "FORGING", 
            allowableValues = "FORGING, HEAT_TREATMENT, MACHINING, QUALITY, DISPATCH")
    private String operationType;

    @JsonProperty("parentStepId")
    @ApiModelProperty(value = "ID of the parent workflow step for tree structure")
    private Long parentStepId;

    @JsonProperty("treeLevel")
    @ApiModelProperty(value = "Level/depth in the workflow tree (root = 0)")
    private Integer treeLevel;

    @JsonProperty("stepName")
    @ApiModelProperty(value = "Name of the step", example = "Forging")
    private String stepName;

    @JsonProperty("stepDescription")
    @ApiModelProperty(value = "Description of the step")
    private String stepDescription;

    @JsonProperty("isOptional")
    @ApiModelProperty(value = "Whether this step is optional")
    private Boolean isOptional;

    @JsonProperty("isParallel")
    @ApiModelProperty(value = "Whether this step can be executed in parallel")
    private Boolean isParallel;

    @JsonProperty("createdAt")
    @ApiModelProperty(value = "Creation timestamp")
    private String createdAt;

    @JsonProperty("updatedAt")
    @ApiModelProperty(value = "Last update timestamp")
    private String updatedAt;
} 