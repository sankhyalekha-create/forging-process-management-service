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
@ApiModel(description = "Gauge Inspection Report representation")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GaugeInspectionReportRepresentation {

  @JsonProperty(value = "id")
  @ApiModelProperty(value = "Id of the Gauge Inspection Report", example = "123")
  private Long id;

  @JsonProperty(value = "processedItemInspectionBatchId")
  @ApiModelProperty(value = "Id of the associated Processed Item Inspection Batch", example = "456")
  private Long processedItemInspectionBatchId;

  @JsonProperty(value = "gauge")
  @ApiModelProperty(value = "associated Gauge", example = "789")
  private GaugeRepresentation gauge;

  @JsonProperty(value = "finishedPiecesCount")
  @ApiModelProperty(value = "Count of finished pieces", example = "100")
  private Integer finishedPiecesCount;

  @JsonProperty(value = "rejectedPiecesCount")
  @ApiModelProperty(value = "Count of rejected pieces", example = "5")
  private Integer rejectedPiecesCount;

  @JsonProperty(value = "reworkPiecesCount")
  @ApiModelProperty(value = "Count of pieces sent for rework", example = "10")
  private Integer reworkPiecesCount;
}

