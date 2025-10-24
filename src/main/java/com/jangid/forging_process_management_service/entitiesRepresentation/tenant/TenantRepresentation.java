package com.jangid.forging_process_management_service.entitiesRepresentation.tenant;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ApiModel(description = "Tenant Representation")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TenantRepresentation {

  @JsonProperty("id")
  @ApiModelProperty(value = "Tenant ID", example = "1")
  private Long id;

  @JsonProperty("tenantName")
  @ApiModelProperty(value = "Tenant Name", example = "ABC Manufacturing Pvt Ltd")
  private String tenantName;

  @JsonProperty("tenantOrgId")
  @ApiModelProperty(value = "Tenant Organization ID", example = "abc_manufacturing@org")
  private String tenantOrgId;

  @JsonProperty("phoneNumber")
  @ApiModelProperty(value = "Phone Number", example = "9876543210")
  private String phoneNumber;

  @JsonProperty("gstin")
  @ApiModelProperty(value = "GSTIN", example = "29ABCDE1234F1Z5")
  private String gstin;

  @JsonProperty("email")
  @ApiModelProperty(value = "Email", example = "contact@abcmanufacturing.com")
  private String email;

  @JsonProperty("address")
  @ApiModelProperty(value = "Address")
  private String address;

  @JsonProperty("stateCode")
  @ApiModelProperty(value = "State Code (2 digits)", example = "29")
  private String stateCode;

  @JsonProperty("pincode")
  @ApiModelProperty(value = "Pincode (6 digits)", example = "560001")
  private String pincode;

  @JsonProperty("otherDetails")
  @ApiModelProperty(value = "Other Details")
  private String otherDetails;

  @JsonProperty("isInternal")
  @ApiModelProperty(value = "Is Internal Tenant")
  private Boolean isInternal;

  @JsonProperty("isActive")
  @ApiModelProperty(value = "Is Active Tenant")
  private Boolean isActive;

  @JsonProperty("createdAt")
  @ApiModelProperty(value = "Created At")
  private LocalDateTime createdAt;

  @JsonProperty("updatedAt")
  @ApiModelProperty(value = "Updated At")
  private LocalDateTime updatedAt;

  @JsonProperty("tenantConfigurations")
  @ApiModelProperty(value = "Tenant Configurations")
  private Map<String, Object> tenantConfigurations;
}

