package com.jangid.forging_process_management_service.entitiesRepresentation.inventory;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jangid.forging_process_management_service.entitiesRepresentation.product.ProductRepresentation;

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
@ApiModel(description = "Product with associated heats representation")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductWithHeatsRepresentation {

  @JsonProperty("product")
  @ApiModelProperty(value = "Product information")
  private ProductRepresentation product;

  @JsonProperty("heats")
  @ApiModelProperty(value = "List of associated heats")
  private List<HeatRepresentation> heats;
} 