package com.jangid.forging_process_management_service.entitiesRepresentation.machining;

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
@ApiModel(description = "MachiningHeat representation")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MachiningHeatRepresentation {

  @JsonProperty(value = "id")
  @ApiModelProperty(value = "Id of the MachiningHeat", example = "123")
  private Long id;

  @JsonProperty(value = "machiningBatchId")
  @ApiModelProperty(value = "machiningBatchId")
  private String machiningBatchId;

  @JsonProperty(value = "heat")
  @ApiModelProperty(value = "heat")
  private HeatRepresentation heat;

  @JsonProperty(value = "piecesUsed")
  @ApiModelProperty(value = "piecesUsed")
  private Integer piecesUsed;

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
