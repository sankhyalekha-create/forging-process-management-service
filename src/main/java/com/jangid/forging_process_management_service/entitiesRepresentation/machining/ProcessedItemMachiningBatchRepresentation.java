package com.jangid.forging_process_management_service.entitiesRepresentation.machining;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jangid.forging_process_management_service.entitiesRepresentation.ProcessedItemRepresentation;

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
@ApiModel(description = "ProcessedItemMachiningBatchRepresentation")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProcessedItemMachiningBatchRepresentation {

  @JsonProperty("id")
  @ApiModelProperty(value = "Id of the processed item machining batch", example = "123")
  private Long id;

  @JsonProperty("processedItem")
  @ApiModelProperty(value = "Details of the processed item")
  private ProcessedItemRepresentation processedItem;

  @JsonProperty("itemStatus")
  @ApiModelProperty(value = "Status of the item", allowableValues = "NEW, IN_PROGRESS, COMPLETED, REJECTED")
  private String itemStatus;

  @JsonProperty("machiningBatchPiecesCount")
  @ApiModelProperty(value = "Number of pieces in the machining batch")
  private Integer machiningBatchPiecesCount;

  @JsonProperty("availableMachiningBatchPiecesCount")
  @ApiModelProperty(value = "Number of pieces available for machining batch daily")
  private Integer availableMachiningBatchPiecesCount;

  @JsonProperty("actualMachiningBatchPiecesCount")
  @ApiModelProperty(value = "Actual number of pieces machined")
  private Integer actualMachiningBatchPiecesCount;

  @JsonProperty("rejectMachiningBatchPiecesCount")
  @ApiModelProperty(value = "Number of rejected pieces in the machining batch")
  private Integer rejectMachiningBatchPiecesCount;

  @JsonProperty("reworkPiecesCount")
  @ApiModelProperty(value = "Number of pieces that require rework")
  private Integer reworkPiecesCount;

  @JsonProperty("initialInspectionBatchPiecesCount")
  @ApiModelProperty(value = "Number of pieces inspected initially")
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
}
