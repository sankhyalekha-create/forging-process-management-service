package com.jangid.forging_process_management_service.entitiesRepresentation.buyer;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ApiModel(description = "Buyer representation")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BuyerRepresentation {
    @JsonProperty(value = "id")
    @ApiModelProperty(value = "Id of the buyer", example = "123")
    private Long id;

    @JsonProperty("buyerName")
    @ApiModelProperty(value = "Name of the buyer")
    private String buyerName;

    @JsonProperty("address")
    @ApiModelProperty(value = "Address of the buyer")
    private String address;

    @JsonProperty("gstinUin")
    @ApiModelProperty(value = "GSTIN/UIN of the buyer")
    private String gstinUin;

  @JsonProperty("phoneNumber")
  @ApiModelProperty(value = "Phone number of the buyer")
  private String phoneNumber;

  @JsonProperty("email")
  @ApiModelProperty(value = "Email of the buyer")
  private String email;

  @JsonProperty("panNumber")
  @ApiModelProperty(value = "PAN number of the buyer")
  private String panNumber;

  @JsonProperty("entities")
  @ApiModelProperty(value = "List of buyer entities (shipping/billing addresses)")
  private List<BuyerEntityRepresentation> entities = new ArrayList<>();

  @JsonProperty("stateCode")
  @ApiModelProperty(value = "State code (2 digits) for GST jurisdiction")
  private String stateCode;

  @JsonProperty("city")
  @ApiModelProperty(value = "City of the buyer")
  private String city;

  @JsonProperty("pincode")
  @ApiModelProperty(value = "Pincode (6 digits) for address identification")
  private String pincode;
} 