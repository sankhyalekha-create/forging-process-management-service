package com.jangid.forging_process_management_service.entitiesRepresentation.machining;

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

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ApiModel(description = "MachiningBatchRepresentation")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MachiningBatchRepresentation {

  @JsonProperty(value = "id")
  @ApiModelProperty(value = "Id of the MachiningBatch", example = "123")
  private Long id;

  @JsonProperty(value = "machiningBatchNumber")
  @ApiModelProperty(value = "machiningBatchNumber")
  private String machiningBatchNumber;

  @JsonProperty("processedItem")
  @ApiModelProperty(value = "processedItem")
  private ProcessedItemRepresentation processedItem;

  @JsonProperty(value = "machineSet")
  @ApiModelProperty(value = "machineSet")
  private MachineSetRepresentation machineSet;

  @JsonProperty("machiningBatchStatus")
  @ApiModelProperty(value = "Status of the machiningBatch")
  private String machiningBatchStatus;

  @JsonProperty("appliedMachiningBatchPiecesCount")
  @ApiModelProperty(value = "appliedMachiningBatchPiecesCount")
  private String appliedMachiningBatchPiecesCount;

  @JsonProperty("actualMachiningBatchPiecesCount")
  @ApiModelProperty(value = "actualMachiningBatchPiecesCount")
  private String actualMachiningBatchPiecesCount;

  @JsonProperty("rejectMachiningBatchPiecesCount")
  @ApiModelProperty(value = "rejectMachiningBatchPiecesCount")
  private String rejectMachiningBatchPiecesCount;

  @JsonProperty("reworkPiecesCount")
  @ApiModelProperty(value = "reworkPiecesCount")
  private String reworkPiecesCount;

  @JsonProperty("dailyMachiningBatchDetail")
  @ApiModelProperty(value = "dailyMachiningBatchDetail")
  private List<DailyMachiningBatchDetailRepresentation> dailyMachiningBatchDetail;

  @JsonProperty("startAt")
  @ApiModelProperty(value = "Timestamp at which the MachiningBatch starts at")
  private String startAt;

  @JsonProperty("endAt")
  @ApiModelProperty(value = "Timestamp at which the MachiningBatch ends at")
  private String endAt;
}
