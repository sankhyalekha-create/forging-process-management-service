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
@ApiModel(description = "Supplier representation")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SupplierRepresentation {

  @JsonProperty(value = "id")
  @ApiModelProperty(value = "Id of the Supplier", example = "123")
  private Long id;

  @JsonProperty("supplierName")
  @ApiModelProperty(value = "supplierName")
  private String supplierName;

  @JsonProperty("supplierDetail")
  @ApiModelProperty(value = "supplierDetail")
  private String supplierDetail;

  @JsonProperty("tenantId")
  @ApiModelProperty(value = "tenantId")
  private Long tenantId;

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
