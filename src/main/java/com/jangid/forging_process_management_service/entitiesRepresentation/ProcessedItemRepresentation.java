package com.jangid.forging_process_management_service.entitiesRepresentation;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jangid.forging_process_management_service.entitiesRepresentation.forging.ForgeRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.heating.HeatTreatmentBatchRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.machining.MachiningBatchRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.product.ItemRepresentation;

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
@ApiModel(description = "ProcessedItem representation")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProcessedItemRepresentation {

  @JsonProperty(value = "id")
  @ApiModelProperty(value = "Id of the processedItem", example = "123")
  private Long id;

  @JsonProperty("forge")
  @ApiModelProperty(value = "forge")
  private ForgeRepresentation forge;

  @JsonProperty("expectedForgePiecesCount")
  @ApiModelProperty(value = "expectedForgePiecesCount")
  private String expectedForgePiecesCount;

  @JsonProperty("actualForgePiecesCount")
  @ApiModelProperty(value = "actualForgePiecesCount")
  private String actualForgePiecesCount;

  @JsonProperty("availableForgePiecesCountForHeat")
  @ApiModelProperty(value = "availableForgePiecesCountForHeat")
  private String availableForgePiecesCountForHeat;

  @JsonProperty("heatTreatmentBatch")
  @ApiModelProperty(value = "heatTreatmentBatch")
  private HeatTreatmentBatchRepresentation heatTreatmentBatch;

  @JsonProperty("heatTreatBatchPiecesCount")
  @ApiModelProperty(value = "heatTreatBatchPiecesCount")
  private String heatTreatBatchPiecesCount;

  @JsonProperty("actualHeatTreatBatchPiecesCount")
  @ApiModelProperty(value = "actualHeatTreatBatchPiecesCount")
  private String actualHeatTreatBatchPiecesCount;

  @JsonProperty("machiningBatchRepresentation")
  @ApiModelProperty(value = "machiningBatchRepresentation")
  private MachiningBatchRepresentation machiningBatchRepresentation;

  @JsonProperty("initialMachiningBatchPiecesCount")
  @ApiModelProperty(value = "initialMachiningBatchPiecesCount")
  private String initialMachiningBatchPiecesCount;

  @JsonProperty("availableMachiningBatchPiecesCount")
  @ApiModelProperty(value = "availableMachiningBatchPiecesCount")
  private String availableMachiningBatchPiecesCount;

  @JsonProperty("item")
  @ApiModelProperty(value = "item")
  private ItemRepresentation item;

  @JsonProperty("itemStatus")
  @ApiModelProperty(value = "itemStatus")
  private String itemStatus;

  @JsonProperty("createdAt")
  @ApiModelProperty(value = "Timestamp at which the processedItem entity was created")
  private String createdAt;

  @JsonProperty("updatedAt")
  @ApiModelProperty(value = "Timestamp at which the processedItem entity was updated")
  private String updatedAt;

  @JsonProperty("deletedAt")
  @ApiModelProperty(value = "Timestamp at which the processedItem entity was deleted")
  private String deletedAt;
}
