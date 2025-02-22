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
@ApiModel(description = "RawMaterialProduct representation")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RawMaterialProductRepresentation {

  @JsonProperty(value = "id")
  @ApiModelProperty(value = "Id of the rawMaterialProduct", example = "123")
  private Long id;

  @JsonProperty("rawMaterial")
  @ApiModelProperty(value = "rawMaterial")
  public RawMaterialRepresentation rawMaterial;

  @JsonProperty(value = "product")
  @ApiModelProperty(value = "product")
  private ProductRepresentation product;

  @JsonProperty("heats")
  @ApiModelProperty(value = "List of heats")
  private List<HeatRepresentation> heats;

  @JsonProperty("createdAt")
  @ApiModelProperty(value = "Timestamp at which the rawMaterialProduct entity was created")
  private String createdAt;

  @JsonProperty("updatedAt")
  @ApiModelProperty(value = "Timestamp at which the rawMaterialProduct entity was updated")
  private String updatedAt;
}
