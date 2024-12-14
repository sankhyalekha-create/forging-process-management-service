package com.jangid.forging_process_management_service.entitiesRepresentation.heating;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jangid.forging_process_management_service.entitiesRepresentation.forging.ForgeRepresentation;

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
@ApiModel(description = "BatchItemSelection representation")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BatchItemSelectionRepresentation {

  @JsonProperty(value = "id")
  @ApiModelProperty(value = "Id of the BatchItemSelection", example = "123")
  private Long id;

  @JsonProperty("forge")
  @ApiModelProperty(value = "forge")
  private ForgeRepresentation forge;

  @JsonProperty("availableForgedPiecesCount")
  @ApiModelProperty(value = "availableForgedPiecesCount")
  private String availableForgedPiecesCount;

  @JsonProperty("heatTreatBatchPiecesCount")
  @ApiModelProperty(value = "heatTreatBatchPiecesCount")
  private String heatTreatBatchPiecesCount;

  @JsonProperty("actualHeatTreatBatchPiecesCount")
  @ApiModelProperty(value = "actualHeatTreatBatchPiecesCount")
  private String actualHeatTreatBatchPiecesCount;

  @JsonProperty("heatTreatBatchId")
  @ApiModelProperty(value = "heatTreatBatchId")
  private Long heatTreatBatchId;
}
