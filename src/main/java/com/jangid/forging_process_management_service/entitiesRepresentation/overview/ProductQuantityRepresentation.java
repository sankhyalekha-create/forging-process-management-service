package com.jangid.forging_process_management_service.entitiesRepresentation.overview;

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
@ApiModel(description = "ProductQuantity Representation")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductQuantityRepresentation {

  @JsonProperty("productName")
  @ApiModelProperty(value = "productName")
  private String productName;

  @JsonProperty("totalQuantity")
  @ApiModelProperty(value = "totalQuantity")
  private Double totalQuantity;

}
