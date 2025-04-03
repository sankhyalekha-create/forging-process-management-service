package com.jangid.forging_process_management_service.entitiesRepresentation.inventory;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jangid.forging_process_management_service.entitiesRepresentation.product.SupplierRepresentation;

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
@ApiModel(description = "RawMaterial representation")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RawMaterialRepresentation {

  @JsonProperty(value = "id")
  @ApiModelProperty(value = "Id of the rawMaterial", example = "123")
  private Long id;

  @JsonProperty(value = "tenantId")
  @ApiModelProperty(value = "Id of the tenant", example = "123")
  private Long tenantId;

  @JsonProperty("rawMaterialInvoiceDate")
  @ApiModelProperty(value = "rawMaterialInvoiceDate")
  private String rawMaterialInvoiceDate;

  @JsonProperty("poNumber")
  @ApiModelProperty(value = "poNumber")
  private String poNumber;

  @JsonProperty("rawMaterialReceivingDate")
  @ApiModelProperty(value = "rawMaterialReceivingDate")
  private String rawMaterialReceivingDate;

  @JsonProperty("rawMaterialInvoiceNumber")
  @ApiModelProperty(value = "rawMaterialInvoiceNumber")
  private String rawMaterialInvoiceNumber;

  @JsonProperty("unitOfMeasurement")
  @ApiModelProperty(value = "unitOfMeasurement")
  private String unitOfMeasurement;

  @JsonProperty("rawMaterialTotalQuantity")
  @ApiModelProperty(value = "rawMaterialTotalQuantity")
  private String rawMaterialTotalQuantity;

  @JsonProperty("rawMaterialTotalPieces")
  @ApiModelProperty(value = "rawMaterialTotalPieces")
  private Integer rawMaterialTotalPieces;

  @JsonProperty("rawMaterialHsnCode")
  @ApiModelProperty(value = "rawMaterialHsnCode")
  private String rawMaterialHsnCode;

  @JsonProperty("rawMaterialGoodsDescription")
  @ApiModelProperty(value = "rawMaterialGoodsDescription")
  private String rawMaterialGoodsDescription;

  @JsonProperty("supplier")
  @ApiModelProperty(value = "supplier")
  private SupplierRepresentation supplier;

  @JsonProperty("rawMaterialProducts")
  @ApiModelProperty(value = "List of raw material products")
  private List<RawMaterialProductRepresentation> rawMaterialProducts;

  @JsonProperty("createdAt")
  @ApiModelProperty(value = "Timestamp at which the rawMaterial entity was created")
  private String createdAt;

  @JsonProperty("updatedAt")
  @ApiModelProperty(value = "Timestamp at which the rawMaterial entity was updated")
  private String updatedAt;
}
