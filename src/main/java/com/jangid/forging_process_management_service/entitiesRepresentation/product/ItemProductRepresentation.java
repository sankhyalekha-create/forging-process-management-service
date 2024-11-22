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

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ApiModel(description = "ItemProduct representation")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ItemProductRepresentation {

  @JsonProperty(value = "id")
  @ApiModelProperty(value = "Id of the itemProduct", example = "123")
  private Long id;

  @JsonProperty(value = "item")
  @ApiModelProperty(value = "item")
  private ItemRepresentation item;

  @JsonProperty(value = "product")
  @ApiModelProperty(value = "product")
  private ProductRepresentation product;

  @JsonProperty("createdAt")
  @ApiModelProperty(value = "Timestamp at which the itemProduct entity was created")
  private String createdAt;

  @JsonProperty("updatedAt")
  @ApiModelProperty(value = "Timestamp at which the itemProduct entity was updated")
  private String updatedAt;

  @JsonProperty("deletedAt")
  @ApiModelProperty(value = "Timestamp at which the itemProduct entity was deleted")
  private String deletedAt;

}
