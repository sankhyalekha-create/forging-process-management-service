package com.jangid.forging_process_management_service.entitiesRepresentation;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ApiModel(description = "RawMaterial representation")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RawMaterialRepresentation {

  @JsonProperty(value = "id")
  @ApiModelProperty(value = "Id of the rawMaterial", example = "123")
  private Long id;

  @JsonProperty("rawMaterialReceivingDate")
  @ApiModelProperty(value = "rawMaterialReceivingDate")
  private String rawMaterialReceivingDate;

  @JsonProperty("rawMaterialInvoiceNumber")
  @ApiModelProperty(value = "rawMaterialInvoiceNumber")
  private String rawMaterialInvoiceNumber;

  @JsonProperty("rawMaterialTotalQuantity")
  @ApiModelProperty(value = "rawMaterialTotalQuantity")
  private float rawMaterialTotalQuantity;

  @JsonProperty("rawMaterialInputCode")
  @ApiModelProperty(value = "rawMaterialInputCode")
  private String rawMaterialInputCode;

  @JsonProperty("rawMaterialHsnCode")
  @ApiModelProperty(value = "rawMaterialHsnCode")
  private String rawMaterialHsnCode;

  @JsonProperty("rawMaterialGoodsDescription")
  @ApiModelProperty(value = "rawMaterialGoodsDescription")
  private String rawMaterialGoodsDescription;

  @JsonProperty("createdAt")
  @ApiModelProperty(value = "Timestamp at which the rawMaterial entity was created")
  private String createdAt;


  @JsonProperty("updatedAt")
  @ApiModelProperty(value = "Timestamp at which the rawMaterial entity was updated")
  private String updatedAt;

  @JsonProperty("heats")
  @ApiModelProperty(value = "Timestamp at which the rawMaterial entity was updated")
  private List<RawMaterialHeatRepresentation> heats;
}
