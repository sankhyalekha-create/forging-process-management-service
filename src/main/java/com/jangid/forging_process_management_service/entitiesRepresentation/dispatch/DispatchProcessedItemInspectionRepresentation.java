package com.jangid.forging_process_management_service.entitiesRepresentation.dispatch;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jangid.forging_process_management_service.entitiesRepresentation.quality.ProcessedItemInspectionBatchRepresentation;

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
@ApiModel(description = "Dispatch Processed Item Inspection Representation")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DispatchProcessedItemInspectionRepresentation {

  @JsonProperty(value = "id")
  @ApiModelProperty(value = "Id of the Dispatch Processed Item Inspection", example = "123")
  private Long id;

  @JsonProperty(value = "dispatchBatch")
  @ApiModelProperty(value = "Associated Dispatch Batch")
  private DispatchBatchRepresentation dispatchBatch;

  @JsonProperty(value = "processedItemInspectionBatch")
  @ApiModelProperty(value = "Associated Processed Item Inspection Batch")
  private ProcessedItemInspectionBatchRepresentation processedItemInspectionBatch;

  @JsonProperty(value = "dispatchedPiecesCount")
  @ApiModelProperty(value = "Count of dispatched pieces", example = "50")
  private Integer dispatchedPiecesCount;
}

