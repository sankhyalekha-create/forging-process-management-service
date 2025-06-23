package com.jangid.forging_process_management_service.entitiesRepresentation.dispatch;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jangid.forging_process_management_service.entitiesRepresentation.product.ItemRepresentation;

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
@ApiModel(description = "Processed Item Dispatch Batch representation")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProcessedItemDispatchBatchRepresentation {

  @JsonProperty(value = "id")
  @ApiModelProperty(value = "Id of the Processed Item Dispatch Batch", example = "123")
  private Long id;

  @JsonProperty(value = "dispatchBatch")
  @ApiModelProperty(value = "Associated Dispatch Batch")
  private DispatchBatchRepresentation dispatchBatch;

  @JsonProperty(value = "item")
  @ApiModelProperty(value = "associated Item", example = "789")
  private ItemRepresentation item;

  @JsonProperty("dispatchHeats")
  @ApiModelProperty(value = "List of heat consumption records for this processed item")
  private List<DispatchHeatRepresentation> dispatchHeats;

  @JsonProperty(value = "totalDispatchPiecesCount")
  @ApiModelProperty(value = "Total pieces dispatched in the batch", example = "500")
  private Integer totalDispatchPiecesCount;

  @JsonProperty(value = "itemStatus")
  @ApiModelProperty(value = "Status of the processed item", example = "DISPATCHED")
  private String itemStatus;

  @JsonProperty("workflowIdentifier")
  @ApiModelProperty(value = "Workflow identifier for this specific processed item dispatch batch")
  private String workflowIdentifier;

  @JsonProperty("itemWorkflowId")
  @ApiModelProperty(value = "Item workflow ID for this specific processed item dispatch batch")
  private Long itemWorkflowId;

  @JsonProperty("previousOperationProcessedItemId")
  @ApiModelProperty(value = "ID of the processed item from the previous operation that was used for this dispatch")
  private Long previousOperationProcessedItemId;
}

