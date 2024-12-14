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
@ApiModel(description = "Item representation")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ItemRepresentation {

  @JsonProperty(value = "id")
  @ApiModelProperty(value = "Id of the rawMaterial", example = "123")
  private Long id;

  @JsonProperty("itemName")
  @ApiModelProperty(value = "itemName")
  private String itemName;

  @JsonProperty("itemCode")
  @ApiModelProperty(value = "itemCode")
  private String itemCode;

  @JsonProperty("itemWeight")
  @ApiModelProperty(value = "Weight of the item")
  private String itemWeight;

  @JsonProperty("itemProducts")
  @ApiModelProperty(value = "List of products of item")
  private List<ItemProductRepresentation> itemProducts;

  @JsonProperty("tenantId")
  @ApiModelProperty(value = "tenantId")
  private Long tenantId;

  @JsonProperty("createdAt")
  @ApiModelProperty(value = "Timestamp at which the item entity was created")
  private String createdAt;

  @JsonProperty("updatedAt")
  @ApiModelProperty(value = "Timestamp at which the item entity was updated")
  private String updatedAt;

  @JsonProperty("deletedAt")
  @ApiModelProperty(value = "Timestamp at which the item entity was deleted")
  private String deletedAt;
}
