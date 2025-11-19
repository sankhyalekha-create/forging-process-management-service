package com.jangid.forging_process_management_service.entitiesRepresentation.vendor;

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
@ApiModel(description = "VendorEntity representation")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VendorEntityRepresentation {
    @JsonProperty(value = "id")
    @ApiModelProperty(value = "Id of the vendor entity", example = "123")
    private Long id;

    @JsonProperty("vendorEntityName")
    @ApiModelProperty(value = "Name of the vendor entity")
    private String vendorEntityName;

    @JsonProperty("address")
    @ApiModelProperty(value = "Address of the vendor entity")
    private String address;

    @JsonProperty("gstinUin")
    @ApiModelProperty(value = "GSTIN/UIN of the vendor entity")
    private String gstinUin;

    @JsonProperty("phoneNumber")
    @ApiModelProperty(value = "Phone number of the vendor entity")
    private String phoneNumber;

    @JsonProperty("email")
    @ApiModelProperty(value = "Email of the vendor entity")
    private String email;

    @JsonProperty("panNumber")
    @ApiModelProperty(value = "PAN number of the vendor entity")
    private String panNumber;

    @JsonProperty("isBillingEntity")
    @ApiModelProperty(value = "Flag indicating if this is a billing entity")
    private boolean isBillingEntity;

    @JsonProperty("isShippingEntity")
    @ApiModelProperty(value = "Flag indicating if this is a shipping entity")
    private boolean isShippingEntity;

    @JsonProperty("vendorId")
    @ApiModelProperty(value = "Id of the associated vendor", example = "123")
    private Long vendorId;

    @JsonProperty("stateCode")
    @ApiModelProperty(value = "State code (2 digits) for GST jurisdiction")
    private String stateCode;

    @JsonProperty("city")
    @ApiModelProperty(value = "City of the vendor entity")
    private String city;

    @JsonProperty("pincode")
    @ApiModelProperty(value = "Pincode (6 digits) for address identification")
    private String pincode;
} 