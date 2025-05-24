package com.jangid.forging_process_management_service.entitiesRepresentation.forging;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jangid.forging_process_management_service.entitiesRepresentation.inventory.HeatRepresentation;

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
@ApiModel(description = "ForgeHeat representation")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ForgeHeatRepresentation {

  @JsonProperty(value = "id")
  @ApiModelProperty(value = "Id of the forgeHeat", example = "123")
  private Long id;

  @JsonProperty(value = "heatId")
  @ApiModelProperty(value = "Id of the heat for new forge heats", example = "456")
  private Long heatId;

  @JsonProperty(value = "forgeId")
  @ApiModelProperty(value = "forgeId")
  private String forgeId;

  @JsonProperty(value = "heat")
  @ApiModelProperty(value = "heat")
  private HeatRepresentation heat;

  @JsonProperty(value = "heatQuantityUsed")
  @ApiModelProperty(value = "heatQuantityUsed")
  private String heatQuantityUsed;

  @JsonProperty(value = "heatQuantityUsedInRejectedPieces")
  @ApiModelProperty(value = "Heat quantity used in rejected pieces during forging")
  private String heatQuantityUsedInRejectedPieces;

  @JsonProperty(value = "heatQuantityUsedInOtherRejections")
  @ApiModelProperty(value = "Heat quantity used in other rejections during forging")
  private String heatQuantityUsedInOtherRejections;

  @JsonProperty(value = "rejectedPieces")
  @ApiModelProperty(value = "Number of rejected pieces from this heat during forging")
  private String rejectedPieces;

  @JsonProperty("createdAt")
  @ApiModelProperty(value = "Timestamp at which the forgeHeat entity was created")
  private String createdAt;

  @JsonProperty("updatedAt")
  @ApiModelProperty(value = "Timestamp at which the forgeHeat entity was updated")
  private String updatedAt;

  @JsonProperty("deletedAt")
  @ApiModelProperty(value = "Timestamp at which the itemProduct entity was deleted")
  private String deletedAt;
}
