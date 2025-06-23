package com.jangid.forging_process_management_service.entitiesRepresentation.machining;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jangid.forging_process_management_service.entitiesRepresentation.product.ItemRepresentation;

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
@ApiModel(description = "ProcessedItemMachiningBatchRepresentation")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProcessedItemMachiningBatchRepresentation {

  @JsonProperty("id")
  @ApiModelProperty(value = "ID of the processed item machining batch")
  private Long id;

  @JsonProperty("item")
  @ApiModelProperty(value = "Item being machined")
  private ItemRepresentation item;

  @JsonProperty("machiningBatch")
  @ApiModelProperty(value = "Associated machining batch")
  private MachiningBatchRepresentation machiningBatch;

  @JsonProperty("machiningBatchesForRework")
  @ApiModelProperty(value = "List of machining batches for rework")
  private List<MachiningBatchRepresentation> machiningBatchesForRework;

  @JsonProperty("itemStatus")
  @ApiModelProperty(value = "Status of the item")
  private String itemStatus;

  @JsonProperty("machiningHeats")
  @ApiModelProperty(value = "List of heat consumption records for this processed item")
  private List<MachiningHeatRepresentation> machiningHeats;

  @JsonProperty("machiningBatchPiecesCount")
  @ApiModelProperty(value = "Number of pieces in the machining batch")
  private Integer machiningBatchPiecesCount;

  @JsonProperty("availableMachiningBatchPiecesCount")
  @ApiModelProperty(value = "Available pieces count")
  private Integer availableMachiningBatchPiecesCount;

  @JsonProperty("actualMachiningBatchPiecesCount")
  @ApiModelProperty(value = "Actual pieces count after machining")
  private Integer actualMachiningBatchPiecesCount;

  @JsonProperty("rejectMachiningBatchPiecesCount")
  @ApiModelProperty(value = "Number of rejected pieces")
  private Integer rejectMachiningBatchPiecesCount;

  @JsonProperty("reworkPiecesCount")
  @ApiModelProperty(value = "Number of pieces for rework")
  private Integer reworkPiecesCount;

  @JsonProperty("reworkPiecesCountAvailableForRework")
  @ApiModelProperty(value = "Number of pieces available for rework")
  private Integer reworkPiecesCountAvailableForRework;

  @JsonProperty("initialInspectionBatchPiecesCount")
  @ApiModelProperty(value = "Initial number of pieces for inspection batch")
  private Integer initialInspectionBatchPiecesCount;

  @JsonProperty("availableInspectionBatchPiecesCount")
  @ApiModelProperty(value = "Number of pieces available after inspection")
  private Integer availableInspectionBatchPiecesCount;

  @JsonProperty("createdAt")
  @ApiModelProperty(value = "Timestamp when the processed item machining batch was created")
  private String createdAt;

  @JsonProperty("updatedAt")
  @ApiModelProperty(value = "Timestamp when the processed item machining batch was last updated")
  private String updatedAt;

  @JsonProperty("deleted")
  @ApiModelProperty(value = "Flag indicating if the processed item machining batch is deleted")
  private boolean deleted;

  @JsonProperty("workflowIdentifier")
  @ApiModelProperty(value = "Workflow identifier for this specific processed item machining batch")
  private String workflowIdentifier;

  @JsonProperty("itemWorkflowId")
  @ApiModelProperty(value = "Item workflow ID for this specific processed item machining batch")
  private Long itemWorkflowId;

  @JsonProperty("previousOperationProcessedItemId")
  @ApiModelProperty(value = "ID of the processed item from the previous operation that was used for this machining")
  private Long previousOperationProcessedItemId;
}
