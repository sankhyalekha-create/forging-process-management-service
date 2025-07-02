package com.jangid.forging_process_management_service.entitiesRepresentation.workflow;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "Request payload for completing an item workflow")
public class CompleteWorkflowRequestRepresentation {

    @ApiModelProperty(value = "Completion date and time in format 'yyyy-MM-dd'T'HH:mm'", 
                     example = "2025-06-22T06:47", 
                     required = true)
    private String completedAt;
} 