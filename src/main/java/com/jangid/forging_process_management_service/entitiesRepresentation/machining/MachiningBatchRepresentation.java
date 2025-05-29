package com.jangid.forging_process_management_service.entitiesRepresentation.machining;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jangid.forging_process_management_service.entitiesRepresentation.heating.ProcessedItemHeatTreatmentBatchRepresentation;
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
@ApiModel(description = "MachiningBatchRepresentation")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MachiningBatchRepresentation {

  @JsonProperty(value = "id")
  @ApiModelProperty(value = "Id of the MachiningBatch", example = "123")
  private Long id;

  @JsonProperty(value = "machiningBatchNumber")
  @ApiModelProperty(value = "Machining batch number")
  private String machiningBatchNumber;

  @JsonProperty("processedItemHeatTreatmentBatch")
  @ApiModelProperty(value = "selected processedItemHeatTreatmentBatch for machining from HeatTreatmentBatch")
  private ProcessedItemHeatTreatmentBatchRepresentation processedItemHeatTreatmentBatch;

  @JsonProperty("processedItemMachiningBatch")
  @ApiModelProperty(value = "Processed item machining batches")
  private ProcessedItemMachiningBatchRepresentation processedItemMachiningBatch;

  @JsonProperty("inputProcessedItemMachiningBatch")
  @ApiModelProperty(value = "Input processed item machining batch for rework")
  private ProcessedItemMachiningBatchRepresentation inputProcessedItemMachiningBatch;

  @JsonProperty("machiningBatchStatus")
  @ApiModelProperty(value = "Status of the machining batch", allowableValues = "IDLE, IN_PROGRESS, COMPLETED")
  private String machiningBatchStatus;

  @JsonProperty("machiningBatchType")
  @ApiModelProperty(value = "Type of the machining batch", allowableValues = "FRESH, REWORK")
  private String machiningBatchType;

  @JsonProperty("dailyMachiningBatchDetail")
  @ApiModelProperty(value = "Details of the daily machining batches")
  private List<DailyMachiningBatchRepresentation> dailyMachiningBatchDetail;

  @JsonProperty("createAt")
  @ApiModelProperty(value = "Create at time of the machining batch")
  private String createAt;

  @JsonProperty("startAt")
  @ApiModelProperty(value = "Timestamp when the machining batch starts")
  private String startAt;

  @JsonProperty("endAt")
  @ApiModelProperty(value = "Timestamp when the machining batch ends")
  private String endAt;

  @JsonProperty("createdAt")
  @ApiModelProperty(value = "Timestamp when the machining batch was created")
  private String createdAt;

  @JsonProperty("updatedAt")
  @ApiModelProperty(value = "Timestamp when the machining batch was last updated")
  private String updatedAt;

  @JsonProperty("deleted")
  @ApiModelProperty(value = "Flag indicating if the machining batch is deleted")
  private boolean deleted;

  @JsonProperty(value = "tenantId")
  @ApiModelProperty(value = "tenantId of the machining Batch", example = "123")
  private Long tenantId;

  @JsonProperty("machiningHeats")
  @ApiModelProperty(value = "List of machining heats associated with this batch")
  private List<MachiningHeatRepresentation> machiningHeats;

  @JsonProperty("item")
  @ApiModelProperty(value = "item")
  private ItemRepresentation item;
}
