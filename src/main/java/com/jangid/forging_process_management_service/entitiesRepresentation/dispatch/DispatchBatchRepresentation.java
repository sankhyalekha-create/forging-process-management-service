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

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ApiModel(description = "Dispatch Batch representation")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DispatchBatchRepresentation {

  @JsonProperty(value = "id")
  @ApiModelProperty(value = "Id of the Dispatch Batch", example = "123")
  private Long id;

  @JsonProperty(value = "dispatchBatchNumber")
  @ApiModelProperty(value = "Unique number of the Dispatch Batch", example = "DISP-001")
  private String dispatchBatchNumber;

  @JsonProperty(value = "dispatchBatchStatus")
  @ApiModelProperty(value = "Status of the Dispatch Batch", example = "DISPATCHED")
  private String dispatchBatchStatus;

  @JsonProperty(value = "dispatchReadyAt")
  @ApiModelProperty(value = "Timestamp when the batch is ready for dispatch", example = "2025-01-01T10:00:00")
  private String dispatchReadyAt;

  @JsonProperty(value = "dispatchedAt")
  @ApiModelProperty(value = "Timestamp when the batch was dispatched", example = "2025-01-01T12:00:00")
  private String dispatchedAt;

  @JsonProperty(value = "processedItemInspectionBatches")
  @ApiModelProperty(value = "List of associated Processed Item Inspection Batches")
  private List<ProcessedItemInspectionBatchRepresentation> processedItemInspectionBatches;

  @JsonProperty(value = "processedItemDispatchBatch")
  @ApiModelProperty(value = "Associated Processed Item Dispatch Batch")
  private ProcessedItemDispatchBatchRepresentation processedItemDispatchBatch;

  @JsonProperty(value = "tenantId")
  @ApiModelProperty(value = "tenantId of the Dispatch Batch", example = "123")
  private Long tenantId;
}
