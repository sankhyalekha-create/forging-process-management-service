package com.jangid.forging_process_management_service.entitiesRepresentation.product;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

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
@ApiModel(description = "Supplier representation")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductRepresentation {

  @JsonProperty(value = "id")
  @ApiModelProperty(value = "Id of the rawMaterial", example = "123")
  private Long id;

  @JsonProperty("productName")
  @ApiModelProperty(value = "productName")
  private String productName;

  @JsonProperty("productCode")
  @ApiModelProperty(value = "productCode")
  private String productCode;

  @JsonProperty("productSku")
  @ApiModelProperty(value = "productSku")
  private String productSku;

  @JsonProperty("unitOfMeasurement")
  @ApiModelProperty(value = "unitOfMeasurement")
  private String unitOfMeasurement;

  @JsonProperty("suppliers")
  @ApiModelProperty(value = "List of suppliers")
  private List<SupplierRepresentation> suppliers;

  @JsonProperty("createdAt")
  @ApiModelProperty(value = "Timestamp at which the supplier entity was created")
  private String createdAt;

  @JsonProperty("updatedAt")
  @ApiModelProperty(value = "Timestamp at which the supplier entity was updated")
  private String updatedAt;

  @JsonProperty("deletedAt")
  @ApiModelProperty(value = "Timestamp at which the supplier entity was deleted")
  private String deletedAt;

}
