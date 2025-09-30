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

  @JsonProperty("address")
  @ApiModelProperty(value = "Address of the supplier")
  private String address;

  @JsonProperty("phoneNumber")
  @ApiModelProperty(value = "Phone number of the supplier", example = "+919876543210")
  private String phoneNumber;

  @JsonProperty("email")
  @ApiModelProperty(value = "Email of the supplier", example = "supplier@example.com")
  private String email;

  @JsonProperty("panNumber")
  @ApiModelProperty(value = "PAN number of the supplier", example = "ABCDE1234F")
  private String panNumber;

  @JsonProperty("gstinNumber")
  @ApiModelProperty(value = "GSTIN number of the supplier", example = "22ABCDE1234F1Z5")
  private String gstinNumber;

  @JsonProperty("stateCode")
  @ApiModelProperty(value = "State code (2 digits) for GST jurisdiction")
  private String stateCode;

  @JsonProperty("pincode")
  @ApiModelProperty(value = "Pincode (6 digits) for address identification")
  private String pincode;

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
