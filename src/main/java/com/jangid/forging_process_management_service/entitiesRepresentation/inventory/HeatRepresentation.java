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
@ApiModel(description = "heat representation")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HeatRepresentation {

  @JsonProperty(value = "id")
  @ApiModelProperty(value = "Id of the heat", example = "123")
  private Long id;

  @JsonProperty("heatNumber")
  @ApiModelProperty(value = "heatNumber")
  private String heatNumber;

  @JsonProperty("heatQuantity")
  @ApiModelProperty(value = "heatQuantity")
  private String heatQuantity;

  @JsonProperty("availableHeatQuantity")
  @ApiModelProperty(value = "availableHeatQuantity")
  private String availableHeatQuantity;

  @JsonProperty("isInPieces")
  @ApiModelProperty(value = "isInPieces")
  private Boolean isInPieces;

  @JsonProperty("piecesCount")
  @ApiModelProperty(value = "piecesCount")
  private Integer piecesCount;

  @JsonProperty("availablePiecesCount")
  @ApiModelProperty(value = "availablePiecesCount")
  private Integer availablePiecesCount;

  @JsonProperty("testCertificateNumber")
  @ApiModelProperty(value = "testCertificateNumber")
  private String testCertificateNumber;

  @JsonProperty("location")
  @ApiModelProperty(value = "location")
  private String location;

  @JsonProperty("rawMaterialProduct")
  @ApiModelProperty(value = "rawMaterialProduct")
  private RawMaterialProductRepresentation rawMaterialProduct;

  @JsonProperty("createdAt")
  @ApiModelProperty(value = "Timestamp at which the heat entity was created")
  private String createdAt;

  @JsonProperty("updatedAt")
  @ApiModelProperty(value = "Timestamp at which the heat entity was updated")
  private String updatedAt;

  @JsonProperty("active")
  @ApiModelProperty(value = "Indicates if the heat is active and should be included in regular inventory lists")
  private Boolean active;

}
