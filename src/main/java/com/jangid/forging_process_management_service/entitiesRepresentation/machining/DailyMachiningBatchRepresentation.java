package com.jangid.forging_process_management_service.entitiesRepresentation.machining;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jangid.forging_process_management_service.entitiesRepresentation.operator.MachineOperatorRepresentation;

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
@ApiModel(description = "DailyMachiningBatchRepresentation")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DailyMachiningBatchRepresentation {

  @JsonProperty("id")
  @ApiModelProperty(value = "Id of the DailyMachiningBatch", example = "123")
  private Long id;

  @JsonProperty("machiningBatchId")
  @ApiModelProperty(value = "Id of the associated MachiningBatch", example = "456")
  private Long machiningBatchId;

  @JsonProperty("machineOperator")
  @ApiModelProperty(value = "Machine operator assigned to this machining batch")
  private MachineOperatorRepresentation machineOperator;

  @JsonProperty("dailyMachiningBatchStatus")
  @ApiModelProperty(
      value = "Status of the Daily Machining Batch",
      allowableValues = "IDLE, IN_PROGRESS, COMPLETED"
  )
  private String dailyMachiningBatchStatus;

  @JsonProperty("startDateTime")
  @ApiModelProperty(value = "Start date and time of the operation", example = "2025-01-01T08:00:00")
  private String startDateTime;

  @JsonProperty("endDateTime")
  @ApiModelProperty(value = "End date and time of the operation", example = "2025-01-01T16:00:00")
  private String endDateTime;

  @JsonProperty("completedPiecesCount")
  @ApiModelProperty(value = "Number of pieces completed during the operation", example = "100")
  private Integer completedPiecesCount;

  @JsonProperty("rejectedPiecesCount")
  @ApiModelProperty(value = "Number of pieces rejected during the operation", example = "10")
  private Integer rejectedPiecesCount;

  @JsonProperty("reworkPiecesCount")
  @ApiModelProperty(value = "Number of pieces requiring rework during the operation", example = "5")
  private Integer reworkPiecesCount;

  @JsonProperty("createdAt")
  @ApiModelProperty(value = "Timestamp when the DailyMachiningBatch was created", example = "2025-01-01T07:00:00")
  private String createdAt;

  @JsonProperty("updatedAt")
  @ApiModelProperty(value = "Timestamp when the DailyMachiningBatch was last updated", example = "2025-01-01T17:00:00")
  private String updatedAt;

  @JsonProperty("deleted")
  @ApiModelProperty(value = "Indicates whether the DailyMachiningBatch is deleted", example = "false")
  private boolean deleted;
}
