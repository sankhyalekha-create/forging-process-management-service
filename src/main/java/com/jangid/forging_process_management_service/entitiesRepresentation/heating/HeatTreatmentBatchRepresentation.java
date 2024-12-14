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

import jakarta.persistence.Column;

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

  @JsonProperty("batchItems")
  @ApiModelProperty(value = "List of batch items")
  private List<BatchItemSelectionRepresentation> batchItems;

  @JsonProperty(value = "totalWeight")
  @ApiModelProperty(value = "totalWeight", example = "123")
  private String totalWeight;

  @JsonProperty(value = "furnace")
  @ApiModelProperty(value = "furnace")
  private FurnaceRepresentation furnace;

  @JsonProperty("heatTreatmentBatchStatus")
  @ApiModelProperty(value = "Status of the heatTreatmentBatch")
  private String heatTreatmentBatchStatus;

  @JsonProperty(value = "labTestingReport")
  @ApiModelProperty(value = "labTestingReport")
  private String labTestingReport;

  @JsonProperty(value = "labTestingStatus")
  @ApiModelProperty(value = "labTestingStatus")
  private String labTestingStatus;

  @JsonProperty("startAt")
  @ApiModelProperty(value = "Timestamp at which the HeatTreatmentBatch starts at")
  private String startAt;

  @JsonProperty("endAt")
  @ApiModelProperty(value = "Timestamp at which the HeatTreatmentBatch ends at")
  private String endAt;

  @JsonProperty("createdAt")
  @ApiModelProperty(value = "Timestamp at which the HeatTreatmentBatch entity was created")
  private String createdAt;

  @JsonProperty("updatedAt")
  @ApiModelProperty(value = "Timestamp at which the HeatTreatmentBatch entity was updated")
  private String updatedAt;

  @JsonProperty("deletedAt")
  @ApiModelProperty(value = "Timestamp at which the HeatTreatmentBatch entity was deleted")
  private String deletedAt;

}
