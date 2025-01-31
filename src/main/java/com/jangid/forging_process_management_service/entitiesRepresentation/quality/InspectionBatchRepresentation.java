package com.jangid.forging_process_management_service.entitiesRepresentation.quality;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jangid.forging_process_management_service.entitiesRepresentation.machining.ProcessedItemMachiningBatchRepresentation;

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
@ApiModel(description = "Inspection Batch representation")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InspectionBatchRepresentation {

  @JsonProperty(value = "id")
  @ApiModelProperty(value = "Id of the Inspection Batch", example = "123")
  private Long id;

  @JsonProperty(value = "inspectionBatchNumber")
  @ApiModelProperty(value = "Unique number of the Inspection Batch", example = "INS-001")
  private String inspectionBatchNumber;

  @JsonProperty(value = "processedItemMachiningBatch")
  @ApiModelProperty(value = "associated Processed Item Machining Batch")
  private ProcessedItemMachiningBatchRepresentation processedItemMachiningBatch;

  @JsonProperty(value = "processedItemInspectionBatch")
  @ApiModelProperty(value = "associated Processed Item Inspection Batch")
  private ProcessedItemInspectionBatchRepresentation processedItemInspectionBatch;

  @JsonProperty(value = "inspectionBatchStatus")
  @ApiModelProperty(value = "Status of the Inspection Batch", example = "COMPLETED")
  private String inspectionBatchStatus;

  @JsonProperty(value = "startAt")
  @ApiModelProperty(value = "Timestamp when the batch started", example = "2025-01-01T09:00:00")
  private String startAt;

  @JsonProperty(value = "endAt")
  @ApiModelProperty(value = "Timestamp when the batch ended", example = "2025-01-01T18:00:00")
  private String endAt;

}

