package com.jangid.forging_process_management_service.entitiesRepresentation.operator;

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
@ApiModel(description = "OperatorMachiningDetailsRepresentation")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OperatorMachiningDetailsRepresentation {

  @JsonProperty("totalMachiningHours")
  @ApiModelProperty(value = "totalMachiningHours")
  private double totalMachiningHours;

  @JsonProperty(value = "totalCompletedPieces")
  @ApiModelProperty(value = "totalCompletedPieces")
  private int totalCompletedPieces;

  @JsonProperty(value = "totalRejectedPieces")
  @ApiModelProperty(value = "totalRejectedPieces")
  private int totalRejectedPieces;

  @JsonProperty(value = "totalReworkPieces")
  @ApiModelProperty(value = "totalReworkPieces")
  private int totalReworkPieces;

  @JsonProperty("id")
  @ApiModelProperty(value = "Id of the Machine Operator", example = "123")
  private Long id;

  @JsonProperty("fullName")
  @ApiModelProperty(value = "Full name of the Machine Operator", example = "John Doe")
  private String fullName;

  @JsonProperty("aadhaarNumber")
  @ApiModelProperty(value = "Aadhaar number of the Machine Operator", example = "1234-5678-9012")
  private String aadhaarNumber;

  @JsonProperty("startTime")
  @ApiModelProperty(value = "startTime")
  private String startTime;

  @JsonProperty("endTime")
  @ApiModelProperty(value = "endTime")
  private String endTime;
}
