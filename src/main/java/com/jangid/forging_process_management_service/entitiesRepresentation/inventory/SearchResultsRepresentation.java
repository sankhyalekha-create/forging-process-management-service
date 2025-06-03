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

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ApiModel(description = "Search results representation")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SearchResultsRepresentation {

  @JsonProperty("productsWithHeats")
  @ApiModelProperty(value = "Products with their associated heats")
  private List<ProductWithHeatsRepresentation> productsWithHeats;

  @JsonProperty("heats")
  @ApiModelProperty(value = "Standalone heats (for heat number search)")
  private List<HeatRepresentation> heats;

  @JsonProperty("searchType")
  @ApiModelProperty(value = "Type of search performed", allowableValues = "PRODUCT_NAME,PRODUCT_CODE,HEAT_NUMBER")
  private String searchType;

  @JsonProperty("searchTerm")
  @ApiModelProperty(value = "Search term used")
  private String searchTerm;
} 