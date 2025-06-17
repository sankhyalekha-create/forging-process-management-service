package com.jangid.forging_process_management_service.entitiesRepresentation.quality;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jangid.forging_process_management_service.entitiesRepresentation.product.ItemRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.dispatch.DispatchProcessedItemInspectionRepresentation;

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
@ApiModel(description = "Processed Item Inspection Batch representation")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProcessedItemInspectionBatchRepresentation {

  @JsonProperty(value = "id")
  @ApiModelProperty(value = "Id of the Processed Item Inspection Batch", example = "123")
  private Long id;

  @JsonProperty(value = "inspectionBatch")
  @ApiModelProperty(value = "associated Inspection Batch", example = "456")
  private InspectionBatchRepresentation inspectionBatch;

  @JsonProperty(value = "item")
  @ApiModelProperty(value = "associated Item", example = "789")
  private ItemRepresentation item;

  @JsonProperty(value = "gaugeInspectionReports")
  @ApiModelProperty(value = "List of associated Gauge Inspection Report IDs", example = "[101, 102, 103]")
  private List<GaugeInspectionReportRepresentation> gaugeInspectionReports;

  @JsonProperty(value = "inspectionBatchPiecesCount")
  @ApiModelProperty(value = "Total pieces in the inspection batch", example = "1000")
  private Integer inspectionBatchPiecesCount;

  @JsonProperty(value = "availableInspectionBatchPiecesCount")
  @ApiModelProperty(value = "Available pieces in the inspection batch", example = "950")
  private Integer availableInspectionBatchPiecesCount;

  @JsonProperty(value = "finishedInspectionBatchPiecesCount")
  @ApiModelProperty(value = "Finished inspected pieces count", example = "940")
  private Integer finishedInspectionBatchPiecesCount;

  @JsonProperty(value = "rejectInspectionBatchPiecesCount")
  @ApiModelProperty(value = "Rejected pieces in the inspection batch", example = "10")
  private Integer rejectInspectionBatchPiecesCount;

  @JsonProperty(value = "reworkPiecesCount")
  @ApiModelProperty(value = "Pieces sent for rework", example = "5")
  private Integer reworkPiecesCount;

  @JsonProperty(value = "availableDispatchPiecesCount")
  @ApiModelProperty(value = "Pieces available for dispatch", example = "5")
  private Integer availableDispatchPiecesCount;

  @JsonProperty(value = "dispatchedPiecesCount")
  @ApiModelProperty(value = "Pieces which are dispatched", example = "5")
  private Integer dispatchedPiecesCount;

  @JsonProperty(value = "selectedDispatchPiecesCount")
  @ApiModelProperty(value = "Pieces selected for dispatch", example = "5")
  private Integer selectedDispatchPiecesCount;

  @JsonProperty(value = "itemStatus")
  @ApiModelProperty(value = "Status of the processed item", example = "COMPLETED")
  private String itemStatus;

  @JsonProperty(value = "dispatchProcessedItemInspections")
  @ApiModelProperty(value = "List of associated Dispatch Processed Item Inspections")
  private List<DispatchProcessedItemInspectionRepresentation> dispatchProcessedItemInspections;

  @JsonProperty(value = "dailyMachiningBatchInspectionDistribution")
  @ApiModelProperty(value = "Distribution of rejected and rework pieces among daily machining batches")
  private List<DailyMachiningBatchInspectionDistributionRepresentation> dailyMachiningBatchInspectionDistribution;

  @JsonProperty("workflowIdentifier")
  @ApiModelProperty(value = "Workflow identifier for this specific processed item inspection batch")
  private String workflowIdentifier;

  @JsonProperty("itemWorkflowId")
  @ApiModelProperty(value = "Item workflow ID for this specific processed item inspection batch")
  private Long itemWorkflowId;
}

