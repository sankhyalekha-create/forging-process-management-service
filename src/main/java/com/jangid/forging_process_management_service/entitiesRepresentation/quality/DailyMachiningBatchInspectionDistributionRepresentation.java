package com.jangid.forging_process_management_service.entitiesRepresentation.quality;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

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
@ApiModel(description = "Distribution of inspection results among daily machining batches")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DailyMachiningBatchInspectionDistributionRepresentation {

  @JsonProperty("dailyMachiningBatchId")
  @ApiModelProperty(value = "ID of the daily machining batch", example = "123", required = true)
  private Long dailyMachiningBatchId;

  @JsonProperty("rejectedPiecesCount")
  @ApiModelProperty(value = "Number of rejected pieces to distribute to this daily batch", example = "5")
  private Integer rejectedPiecesCount;

  @JsonProperty("reworkPiecesCount")
  @ApiModelProperty(value = "Number of rework pieces to distribute to this daily batch", example = "3")
  private Integer reworkPiecesCount;

  @JsonProperty("actualCompletedPiecesCount")
  @ApiModelProperty(value = "Actual completed pieces count after inspection (will be calculated based on distribution)", example = "92")
  private Integer actualCompletedPiecesCount;
} 