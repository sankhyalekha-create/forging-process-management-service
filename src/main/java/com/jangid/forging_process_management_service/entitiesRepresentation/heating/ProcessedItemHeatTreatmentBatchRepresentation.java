package com.jangid.forging_process_management_service.entitiesRepresentation.heating;

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
@ApiModel(description = "ProcessedItemHeatTreatmentBatch representation")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProcessedItemHeatTreatmentBatchRepresentation {

  @JsonProperty("id")
  @ApiModelProperty(value = "Id of the processedItemHeatTreatmentBatch", example = "123")
  private Long id;

  @JsonProperty("processedItem")
  @ApiModelProperty(value = "Processed item associated with the heat treatment batch")
  private ProcessedItemRepresentation processedItem;

  @JsonProperty("heatTreatmentBatch")
  @ApiModelProperty(value = "Heat treatment batch associated with the processed item")
  private HeatTreatmentBatchRepresentation heatTreatmentBatch;

  @JsonProperty("itemStatus")
  @ApiModelProperty(value = "Item status in the heat treatment batch")
  private String itemStatus;

  @JsonProperty("heatTreatBatchPiecesCount")
  @ApiModelProperty(value = "Number of pieces in the heat treatment batch")
  private Integer heatTreatBatchPiecesCount;

  @JsonProperty("actualHeatTreatBatchPiecesCount")
  @ApiModelProperty(value = "Number of pieces in the heat treatment batch which actually got processed")
  private Integer actualHeatTreatBatchPiecesCount;

  @JsonProperty("initialMachiningBatchPiecesCount")
  @ApiModelProperty(value = "Number of pieces whose heat treatment batch is completed and ready for machining")
  private Integer initialMachiningBatchPiecesCount;

  @JsonProperty("availableMachiningBatchPiecesCount")
  @ApiModelProperty(value = "Number of pieces whose heat treatment batch is completed and available to be deducted for machining")
  private Integer availableMachiningBatchPiecesCount;

  @JsonProperty("createdAt")
  @ApiModelProperty(value = "Timestamp at which the processedItemHeatTreatmentBatch entity was created")
  private String createdAt;

  @JsonProperty("updatedAt")
  @ApiModelProperty(value = "Timestamp at which the processedItemHeatTreatmentBatch entity was updated")
  private String updatedAt;

  @JsonProperty("deletedAt")
  @ApiModelProperty(value = "Timestamp at which the processedItemHeatTreatmentBatch entity was deleted")
  private String deletedAt;

  @JsonProperty("deleted")
  @ApiModelProperty(value = "Deleted status of the processedItemHeatTreatmentBatch")
  private Boolean deleted;
}

