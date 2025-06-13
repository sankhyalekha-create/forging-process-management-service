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
@ApiModel(description = "Workflow template representation")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WorkflowTemplateRepresentation {

    @JsonProperty("id")
    @ApiModelProperty(value = "ID of the workflow template", example = "1")
    private Long id;

    @JsonProperty("workflowName")
    @ApiModelProperty(value = "Name of the workflow template", example = "Standard Manufacturing Process")
    private String workflowName;

    @JsonProperty("workflowDescription")
    @ApiModelProperty(value = "Description of the workflow template")
    private String workflowDescription;

    @JsonProperty("isDefault")
    @ApiModelProperty(value = "Whether this is a default workflow template")
    private Boolean isDefault;

    @JsonProperty("isActive")
    @ApiModelProperty(value = "Whether this workflow template is active")
    private Boolean isActive;

    @JsonProperty("workflowSteps")
    @ApiModelProperty(value = "List of workflow steps in order")
    private List<WorkflowStepRepresentation> workflowSteps;

    @JsonProperty("tenantId")
    @ApiModelProperty(value = "Tenant ID")
    private Long tenantId;

    @JsonProperty("createdAt")
    @ApiModelProperty(value = "Creation timestamp")
    private String createdAt;

    @JsonProperty("updatedAt")
    @ApiModelProperty(value = "Last update timestamp")
    private String updatedAt;
} 