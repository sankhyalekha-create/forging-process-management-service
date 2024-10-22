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

import jakarta.persistence.Column;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ApiModel(description = "ForgeTraceability representation")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ForgeTraceabilityRepresentation {

  @JsonProperty(value = "id")
  @ApiModelProperty(value = "Id of the forgeTraceability", example = "123")
  private long id;

  @JsonProperty("heatNumber")
  @ApiModelProperty(value = "heatNumber")
  private String heatNumber;

  @JsonProperty("heatId")
  @ApiModelProperty(value = "heatId")
  private String heatId;

  @Column(name = "heat_id_quantity_used")
  private String heatIdQuantityUsed;

  @JsonProperty("startAt")
  @ApiModelProperty(value = "Timestamp at which the forgingLine entity starts at")
  private String startAt;

  @JsonProperty("endAt")
  @ApiModelProperty(value = "Timestamp at which the forgingLine entity ends at")
  private String endAt;

  @JsonProperty("forgingLineName")
  @ApiModelProperty(value = "Name of the forgingLine on which forging is to be done")
  private String forgingLineName;

  @JsonProperty("forgePieceWeight")
  @ApiModelProperty(value = "Weight of the single piece which is forged")
  private String forgePieceWeight;

  @JsonProperty("actualForgeCount")
  @ApiModelProperty(value = "Count of the pieces forged under a forging")
  private Integer actualForgeCount;

  @JsonProperty("forgingStatus")
  @ApiModelProperty(value = "Status of the forging of the forgingLine on which forging is done")
  private String forgingStatus;

}
