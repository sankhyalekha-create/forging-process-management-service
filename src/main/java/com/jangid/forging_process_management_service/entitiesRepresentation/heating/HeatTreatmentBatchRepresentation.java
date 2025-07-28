package com.jangid.forging_process_management_service.entitiesRepresentation.heating;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jangid.forging_process_management_service.entitiesRepresentation.forging.FurnaceRepresentation;

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
@ApiModel(description = "HeatTreatmentBatchRepresentation")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HeatTreatmentBatchRepresentation {

  @JsonProperty(value = "id")
  @ApiModelProperty(value = "Id of the HeatTreatmentBatch", example = "123")
  private Long id;

  @JsonProperty(value = "heatTreatmentBatchNumber")
  @ApiModelProperty(value = "Heat treatment batch number")
  private String heatTreatmentBatchNumber;

  @JsonProperty("processedItemHeatTreatmentBatches")
  @ApiModelProperty(value = "List of processed items in the heat treatment batch")
  private List<ProcessedItemHeatTreatmentBatchRepresentation> processedItemHeatTreatmentBatches;

  @JsonProperty(value = "totalWeight")
  @ApiModelProperty(value = "Total weight of the heat treatment batch", example = "123.45")
  private String totalWeight;

  @JsonProperty(value = "furnace")
  @ApiModelProperty(value = "Furnace associated with the heat treatment batch")
  private FurnaceRepresentation furnace;

  @JsonProperty("heatTreatmentBatchStatus")
  @ApiModelProperty(value = "Status of the heat treatment batch")
  private String heatTreatmentBatchStatus;

  @JsonProperty(value = "labTestingReport")
  @ApiModelProperty(value = "Lab testing report related to the heat treatment batch")
  private String labTestingReport;

  @JsonProperty(value = "labTestingStatus")
  @ApiModelProperty(value = "Status of the lab testing for the heat treatment batch")
  private String labTestingStatus;

  //applyAt
  @JsonProperty("applyAt")
  @ApiModelProperty(value = "Timestamp when the heat treatment batch appliedAt")
  private String applyAt;

  @JsonProperty("startAt")
  @ApiModelProperty(value = "Timestamp when the heat treatment batch started")
  private String startAt;

  @JsonProperty("endAt")
  @ApiModelProperty(value = "Timestamp when the heat treatment batch ended")
  private String endAt;

  @JsonProperty("createdAt")
  @ApiModelProperty(value = "Timestamp when the heat treatment batch was created")
  private String createdAt;

  @JsonProperty("updatedAt")
  @ApiModelProperty(value = "Timestamp when the heat treatment batch was last updated")
  private String updatedAt;

  @JsonProperty("deletedAt")
  @ApiModelProperty(value = "Timestamp when the heat treatment batch was deleted")
  private String deletedAt;

  @JsonProperty(value = "tenantId")
  @ApiModelProperty(value = "tenantId of the Heat treatment Batch", example = "123")
  private Long tenantId;
}
