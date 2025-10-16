package com.jangid.forging_process_management_service.entitiesRepresentation.workflow;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "Request payload for completing an item workflow with individual step completion times")
public class CompleteWorkflowRequestRepresentation {

    @ApiModelProperty(value = "Completion date and time for the workflow in format 'yyyy-MM-dd'T'HH:mm'", 
                     example = "2025-06-22T06:47", 
                     required = true,
                     notes = "This will be set as the workflow completion time. It should be after all step completion times.")
    private String completedAt;

    @ApiModelProperty(value = "Map of ItemWorkflowStep ID to completion time", 
                     example = "{'123': '2025-06-22T05:30', '124': '2025-06-22T06:15'}",
                     required = true,
                     notes = "Each step must have a completion time. The time must be after the operation's batch completion time.")
    private Map<Long, String> stepCompletionTimes;
} 