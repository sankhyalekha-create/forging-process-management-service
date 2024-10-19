package com.jangid.forging_process_management_service.entitiesRepresentation.inventory;

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
@ApiModel(description = "RawMaterialHeat representation")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RawMaterialHeatRepresentation {

  @JsonProperty(value = "id")
  @ApiModelProperty(value = "Id of the rawMaterialHeat", example = "123")
  private long id;

  @JsonProperty("heatNumber")
  @ApiModelProperty(value = "heatNumber")
  private String heatNumber;

  @JsonProperty("heatQuantity")
  @ApiModelProperty(value = "heatQuantity")
  private String heatQuantity;

  @JsonProperty("rawMaterialTestCertificateNumber")
  @ApiModelProperty(value = "rawMaterialTestCertificateNumber")
  public String rawMaterialTestCertificateNumber;

  @JsonProperty("barDiameter")
  @ApiModelProperty(value = "barDiameter")
  public String barDiameter;

  @JsonProperty("rawMaterialReceivingInspectionReportNumber")
  @ApiModelProperty(value = "rawMaterialReceivingInspectionReportNumber")
  public String rawMaterialReceivingInspectionReportNumber; //mandatory

  @JsonProperty("rawMaterialInspectionSource")
  @ApiModelProperty(value = "rawMaterialInspectionSource")
  public String rawMaterialInspectionSource;

  @JsonProperty("rawMaterialLocation")
  @ApiModelProperty(value = "rawMaterialLocation")
  public String rawMaterialLocation;

}
