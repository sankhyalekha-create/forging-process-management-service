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
@ApiModel(description = "ForgeShiftHeat representation")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ForgeShiftHeatRepresentation {

  @JsonProperty(value = "id")
  @ApiModelProperty(value = "Id of the forge shift heat", example = "123")
  private Long id;

  @JsonProperty(value = "heatId")
  @ApiModelProperty(value = "Id of the heat for new forge shift heats", example = "456")
  private Long heatId;

  @JsonProperty(value = "forgeShiftId")
  @ApiModelProperty(value = "Id of the parent forge shift", example = "789")
  private Long forgeShiftId;

  @JsonProperty(value = "heat")
  @ApiModelProperty(value = "Heat material information")
  private HeatRepresentation heat;

  @JsonProperty(value = "heatQuantityUsed")
  @ApiModelProperty(value = "Quantity of heat material used in this shift")
  private String heatQuantityUsed;

  @JsonProperty(value = "heatPieces")
  @ApiModelProperty(value = "Number of pieces forged from this heat during this forge shift")
  private String heatPieces;

  @JsonProperty(value = "heatQuantityUsedInRejectedPieces")
  @ApiModelProperty(value = "Heat quantity used in rejected pieces during this forge shift")
  private String heatQuantityUsedInRejectedPieces;

  @JsonProperty(value = "heatQuantityUsedInOtherRejections")
  @ApiModelProperty(value = "Heat quantity used in other rejections during this forge shift")
  private String heatQuantityUsedInOtherRejections;

  @JsonProperty(value = "rejectedPieces")
  @ApiModelProperty(value = "Number of rejected pieces from this heat during this forge shift")
  private String rejectedPieces;

  @JsonProperty("createdAt")
  @ApiModelProperty(value = "Timestamp at which the forge shift heat entity was created")
  private String createdAt;

  @JsonProperty("updatedAt")
  @ApiModelProperty(value = "Timestamp at which the forge shift heat entity was updated")
  private String updatedAt;

  @JsonProperty("deletedAt")
  @ApiModelProperty(value = "Timestamp at which the forge shift heat entity was deleted")
  private String deletedAt;
} 