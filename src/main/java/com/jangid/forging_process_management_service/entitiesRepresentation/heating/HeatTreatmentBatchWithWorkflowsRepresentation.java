package com.jangid.forging_process_management_service.entitiesRepresentation.heating;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jangid.forging_process_management_service.entitiesRepresentation.workflow.ItemWorkflowRepresentation;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ApiModel(description = "Comprehensive HeatTreatmentBatch representation with all associated ItemWorkflow details")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HeatTreatmentBatchWithWorkflowsRepresentation {

    @JsonProperty("heatTreatmentBatch")
    @ApiModelProperty(value = "Basic heat treatment batch details")
    private HeatTreatmentBatchRepresentation heatTreatmentBatch;

    @JsonProperty("associatedWorkflows")
    @ApiModelProperty(value = "Map of itemWorkflowId to complete ItemWorkflow details for all processed items in this batch")
    private Map<Long, ItemWorkflowRepresentation> associatedWorkflows;

    @JsonProperty("processedItemWorkflowMappings")
    @ApiModelProperty(value = "List of mappings between processed items and their workflow identifiers")
    private List<ProcessedItemWorkflowMapping> processedItemWorkflowMappings;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ProcessedItemWorkflowMapping {
        
        @JsonProperty("processedItemHeatTreatmentBatchId")
        @ApiModelProperty(value = "ID of the processed item heat treatment batch")
        private Long processedItemHeatTreatmentBatchId;
        
        @JsonProperty("itemWorkflowId")
        @ApiModelProperty(value = "ID of the associated item workflow")
        private Long itemWorkflowId;
        
        @JsonProperty("workflowIdentifier")
        @ApiModelProperty(value = "Workflow identifier")
        private String workflowIdentifier;
        
        @JsonProperty("itemName")
        @ApiModelProperty(value = "Name of the item")
        private String itemName;
        
        @JsonProperty("heatTreatBatchPiecesCount")
        @ApiModelProperty(value = "Number of pieces processed")
        private Integer heatTreatBatchPiecesCount;
        
        @JsonProperty("actualHeatTreatBatchPiecesCount")
        @ApiModelProperty(value = "Actual number of pieces processed")
        private Integer actualHeatTreatBatchPiecesCount;
    }
}
