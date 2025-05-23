package com.jangid.forging_process_management_service.entitiesRepresentation.forging;

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

import jakarta.persistence.Column;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ApiModel(description = "Forge representation")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ForgeRepresentation {

  @JsonProperty(value = "id")
  @ApiModelProperty(value = "Id of the forge", example = "123")
  private Long id;

  @JsonProperty("forgeTraceabilityNumber")
  @ApiModelProperty(value = "forgeTraceabilityNumber")
  private String forgeTraceabilityNumber;

  @JsonProperty("processedItem")
  @ApiModelProperty(value = "processedItem")
  private ProcessedItemRepresentation processedItem;

  @JsonProperty("itemWeightType")
  @ApiModelProperty(value = "Type of weight to use for calculations", example = "ITEM_WEIGHT, ITEM_SLUG_WEIGHT, ITEM_FORGED_WEIGHT, ITEM_FINISHED_WEIGHT")
  private String itemWeightType;

  @JsonProperty("forgeHeats")
  @ApiModelProperty(value = "forgeHeats")
  private List<ForgeHeatRepresentation> forgeHeats;

  @JsonProperty("applyAt")
  @ApiModelProperty(value = "Timestamp at which the forge apply at")
  private String applyAt;

  @JsonProperty("startAt")
  @ApiModelProperty(value = "Timestamp at which the forge starts at")
  private String startAt;

  @JsonProperty("endAt")
  @ApiModelProperty(value = "Timestamp at which the forge ends at")
  private String endAt;

  @JsonProperty("forgingLine")
  @ApiModelProperty(value = "forgingLine on which forging is to be done")
  private ForgingLineRepresentation forgingLine;

  @JsonProperty("forgeCount")
  @ApiModelProperty(value = "ideal forge count")
  private String forgeCount;

  @JsonProperty("actualForgeCount")
  @ApiModelProperty(value = "Count of the pieces forged under a forging")
  private String actualForgeCount;
  
  @JsonProperty("rejectedForgePiecesCount")
  @ApiModelProperty(value = "Count of the pieces rejected during forging")
  private String rejectedForgePiecesCount;
  
  @JsonProperty("otherForgeRejectionsKg")
  @ApiModelProperty(value = "Weight of other rejections during forging in kg")
  private String otherForgeRejectionsKg;
  
  @JsonProperty("rejection")
  @ApiModelProperty(value = "Flag indicating if there were rejections in the forge process")
  private Boolean rejection;

  @JsonProperty("forgingStatus")
  @ApiModelProperty(value = "Status of the forging of the forgingLine on which forging is done")
  private String forgingStatus;

  @JsonProperty("createdAt")
  @ApiModelProperty(value = "Timestamp at which the forge entity was created")
  private String createdAt;

  @JsonProperty("updatedAt")
  @ApiModelProperty(value = "Timestamp at which the forge entity was updated")
  private String updatedAt;

  @JsonProperty("deletedAt")
  @ApiModelProperty(value = "Timestamp at which the forge entity was deleted")
  private String deletedAt;

  @JsonProperty(value = "tenantId")
  @ApiModelProperty(value = "tenantId of the Heat treatment Batch", example = "123")
  private Long tenantId;

}
