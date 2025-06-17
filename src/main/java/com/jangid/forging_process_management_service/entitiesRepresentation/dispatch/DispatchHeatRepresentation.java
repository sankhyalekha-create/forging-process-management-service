package com.jangid.forging_process_management_service.entitiesRepresentation.dispatch;

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
@ApiModel(description = "DispatchHeat representation")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DispatchHeatRepresentation {

  @JsonProperty("id")
  @ApiModelProperty(value = "Id of the DispatchHeat", example = "123")
  private Long id;

  @JsonProperty("processedItemDispatchBatch")
  @ApiModelProperty(value = "ProcessedItemDispatchBatch associated with this heat consumption")
  private ProcessedItemDispatchBatchRepresentation processedItemDispatchBatch;

  @JsonProperty("heat")
  @ApiModelProperty(value = "Heat that was consumed")
  private HeatRepresentation heat;

  @JsonProperty("piecesUsed")
  @ApiModelProperty(value = "Number of pieces used from the heat", example = "100")
  private Integer piecesUsed;

  @JsonProperty("createdAt")
  @ApiModelProperty(value = "Timestamp when the heat consumption was recorded")
  private String createdAt;

  @JsonProperty("updatedAt")
  @ApiModelProperty(value = "Timestamp when the heat consumption was last updated")
  private String updatedAt;

  @JsonProperty("deletedAt")
  @ApiModelProperty(value = "Timestamp when the heat consumption was deleted")
  private String deletedAt;

  @JsonProperty("deleted")
  @ApiModelProperty(value = "Flag indicating if the heat consumption is deleted")
  private Boolean deleted;
} 