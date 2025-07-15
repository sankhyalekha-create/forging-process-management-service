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

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ApiModel(description = "Vendor representation")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VendorRepresentation {
    @JsonProperty(value = "id")
    @ApiModelProperty(value = "Id of the vendor", example = "123")
    private Long id;

    @JsonProperty("vendorName")
    @ApiModelProperty(value = "Name of the vendor")
    private String vendorName;

    @JsonProperty("address")
    @ApiModelProperty(value = "Address of the vendor")
    private String address;

    @JsonProperty("gstinUin")
    @ApiModelProperty(value = "GSTIN/UIN of the vendor")
    private String gstinUin;

    @JsonProperty("phoneNumber")
    @ApiModelProperty(value = "Phone number of the vendor")
    private String phoneNumber;

    @JsonProperty("panNumber")
    @ApiModelProperty(value = "PAN number of the vendor")
    private String panNumber;

    @JsonProperty("entities")
    @ApiModelProperty(value = "List of vendor entities (shipping/billing addresses)")
    private List<VendorEntityRepresentation> entities = new ArrayList<>();
} 