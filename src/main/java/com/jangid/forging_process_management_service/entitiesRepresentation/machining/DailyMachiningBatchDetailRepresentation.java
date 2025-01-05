package com.jangid.forging_process_management_service.entitiesRepresentation.machining;

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
@ApiModel(description = "DailyMachiningBatchDetailRepresentation")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DailyMachiningBatchDetailRepresentation {

  @JsonProperty(value = "id")
  @ApiModelProperty(value = "Id of the DailyMachiningBatchDetail", example = "123")
  private Long id;

  @JsonProperty("machiningBatch")
  @ApiModelProperty(value = "machiningBatch")
  private MachiningBatchRepresentation machiningBatch;

  @JsonProperty("operationDate")
  @ApiModelProperty(value = "Daily operation date")
  private String operationDate;

  @JsonProperty("startDateTime")
  @ApiModelProperty(value = "Start date time of the operation")
  private String startDateTime;

  @JsonProperty("endDateTime")
  @ApiModelProperty(value = "End date time of the operation")
  private String endDateTime;

  @JsonProperty("completedPiecesCount")
  @ApiModelProperty(value = "Daily Completed Pieces Count")
  private Integer completedPiecesCount;

  @JsonProperty("rejectedPiecesCount")
  @ApiModelProperty(value = "Daily Rejected Pieces Count")
  private Integer rejectedPiecesCount;

  @JsonProperty("reworkPiecesCount")
  @ApiModelProperty(value = "Daily Rework Pieces Count")
  private Integer reworkPiecesCount;

}
