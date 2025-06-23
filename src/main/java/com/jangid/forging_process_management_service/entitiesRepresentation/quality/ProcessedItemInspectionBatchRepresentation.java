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
  @ApiModelProperty(value = "Id of the processed item", example = "123")
  private Long id;

  @JsonProperty(value = "inspectionBatch")
  @ApiModelProperty(value = "inspectionBatch")
  private InspectionBatchRepresentation inspectionBatch;

  @JsonProperty(value = "item")
  @ApiModelProperty(value = "item")
  private ItemRepresentation item;

  @JsonProperty(value = "gaugeInspectionReports")
  @ApiModelProperty(value = "List of associated Gauge Inspection Report IDs", example = "[101, 102, 103]")
  private List<GaugeInspectionReportRepresentation> gaugeInspectionReports;

  @JsonProperty("inspectionHeats")
  @ApiModelProperty(value = "List of heat consumption records for this processed item")
  private List<InspectionHeatRepresentation> inspectionHeats;

  @JsonProperty(value = "inspectionBatchPiecesCount")
  @ApiModelProperty(value = "Pieces count", example = "50")
  private Integer inspectionBatchPiecesCount;

  @JsonProperty(value = "availableInspectionBatchPiecesCount")
  @ApiModelProperty(value = "Available pieces count", example = "40")
  private Integer availableInspectionBatchPiecesCount;

  @JsonProperty(value = "finishedInspectionBatchPiecesCount")
  @ApiModelProperty(value = "Finished pieces count", example = "35")
  private Integer finishedInspectionBatchPiecesCount;

  @JsonProperty(value = "rejectInspectionBatchPiecesCount")
  @ApiModelProperty(value = "Rejected pieces count", example = "3")
  private Integer rejectInspectionBatchPiecesCount;

  @JsonProperty(value = "reworkPiecesCount")
  @ApiModelProperty(value = "Rework pieces count", example = "2")
  private Integer reworkPiecesCount;

  @JsonProperty(value = "availableDispatchPiecesCount")
  @ApiModelProperty(value = "Available dispatch pieces count", example = "35")
  private Integer availableDispatchPiecesCount;

  @JsonProperty(value = "dispatchedPiecesCount")
  @ApiModelProperty(value = "Dispatched pieces count", example = "10")
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

  @JsonProperty("previousOperationProcessedItemId")
  @ApiModelProperty(value = "ID of the processed item from the previous operation that was used for this inspection")
  private Long previousOperationProcessedItemId;
}

