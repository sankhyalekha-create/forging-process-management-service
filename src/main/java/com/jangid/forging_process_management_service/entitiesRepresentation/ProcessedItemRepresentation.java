package com.jangid.forging_process_management_service.entitiesRepresentation;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jangid.forging_process_management_service.entitiesRepresentation.forging.ForgeRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.heating.ProcessedItemHeatTreatmentBatchRepresentation;
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
  private Integer expectedForgePiecesCount;

  @JsonProperty("actualForgePiecesCount")
  @ApiModelProperty(value = "actualForgePiecesCount")
  private Integer actualForgePiecesCount;

  @JsonProperty("availableForgePiecesCountForHeat")
  @ApiModelProperty(value = "availableForgePiecesCountForHeat")
  private Integer availableForgePiecesCountForHeat;

  @JsonProperty("rejectedForgePiecesCount")
  @ApiModelProperty(value = "Count of pieces rejected during the forging process")
  private Integer rejectedForgePiecesCount;

  @JsonProperty("otherForgeRejectionsKg")
  @ApiModelProperty(value = "Other rejections measured in kilograms during the forging process")
  private Double otherForgeRejectionsKg;

  @JsonProperty("processedItemHeatTreatmentBatches")
  @ApiModelProperty(value = "processedItemHeatTreatmentBatches")
  private List<ProcessedItemHeatTreatmentBatchRepresentation> processedItemHeatTreatmentBatches;

  @JsonProperty("item")
  @ApiModelProperty(value = "item")
  private ItemRepresentation item;

  @JsonProperty("createdAt")
  @ApiModelProperty(value = "Timestamp at which the processedItem entity was created")
  private String createdAt;

  @JsonProperty("updatedAt")
  @ApiModelProperty(value = "Timestamp at which the processedItem entity was updated")
  private String updatedAt;

  @JsonProperty("deletedAt")
  @ApiModelProperty(value = "Timestamp at which the processedItem entity was deleted")
  private String deletedAt;

  @JsonProperty("deleted")
  @ApiModelProperty(value = "deleted status of the processedItem")
  private Boolean deleted;

}
