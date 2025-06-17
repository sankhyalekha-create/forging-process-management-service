package com.jangid.forging_process_management_service.entitiesRepresentation.heating;

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
@ApiModel(description = "HeatTreatmentHeatRepresentation")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HeatTreatmentHeatRepresentation {

  @JsonProperty(value = "id")
  @ApiModelProperty(value = "Id of the HeatTreatmentHeat", example = "123")
  private Long id;

  @JsonProperty(value = "heatTreatmentBatchId")
  @ApiModelProperty(value = "Id of the HeatTreatmentBatch", example = "123")
  private String heatTreatmentBatchId;

  @JsonProperty(value = "heat")
  @ApiModelProperty(value = "Heat used in the heat treatment")
  private HeatRepresentation heat;

  @JsonProperty(value = "piecesUsed")
  @ApiModelProperty(value = "Number of pieces used from this heat", example = "10")
  private Integer piecesUsed;

} 