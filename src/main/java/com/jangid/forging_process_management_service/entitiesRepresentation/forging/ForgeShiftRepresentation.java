package com.jangid.forging_process_management_service.entitiesRepresentation.forging;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

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
@ApiModel(description = "ForgeShift representation")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ForgeShiftRepresentation {

  @JsonProperty(value = "id")
  @ApiModelProperty(value = "Id of the forge shift", example = "123")
  private Long id;

  @JsonProperty(value = "forgeId")
  @ApiModelProperty(value = "Id of the parent forge", example = "456")
  private Long forgeId;

  @JsonProperty("startDateTime")
  @ApiModelProperty(value = "Start date and time of the forge shift")
  private String startDateTime;

  @JsonProperty("endDateTime")
  @ApiModelProperty(value = "End date and time of the forge shift")
  private String endDateTime;

  @JsonProperty("forgeShiftHeats")
  @ApiModelProperty(value = "Heat materials used in this forge shift")
  private List<ForgeShiftHeatRepresentation> forgeShiftHeats;

  @JsonProperty("actualForgedPiecesCount")
  @ApiModelProperty(value = "Number of pieces actually forged in this shift")
  private String actualForgedPiecesCount;

  @JsonProperty("rejectedForgePiecesCount")
  @ApiModelProperty(value = "Count of the pieces rejected during this forge shift")
  private String rejectedForgePiecesCount;

  @JsonProperty("otherForgeRejectionsKg")
  @ApiModelProperty(value = "Weight of other rejections during this forge shift in kg")
  private String otherForgeRejectionsKg;

  @JsonProperty("rejection")
  @ApiModelProperty(value = "Flag indicating if there were rejections in this forge shift")
  private Boolean rejection;

  @JsonProperty("createdAt")
  @ApiModelProperty(value = "Timestamp at which the forge shift entity was created")
  private String createdAt;

  @JsonProperty("updatedAt")
  @ApiModelProperty(value = "Timestamp at which the forge shift entity was updated")
  private String updatedAt;

  @JsonProperty("deletedAt")
  @ApiModelProperty(value = "Timestamp at which the forge shift entity was deleted")
  private String deletedAt;
} 