package com.jangid.forging_process_management_service.entitiesRepresentation.vendor;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jangid.forging_process_management_service.entitiesRepresentation.inventory.HeatRepresentation;
import com.jangid.forging_process_management_service.entitiesRepresentation.inventory.RawMaterialProductRepresentation;

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
@ApiModel(description = "Vendor inventory representation")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VendorInventoryRepresentation {

    @JsonProperty(value = "id")
    @ApiModelProperty(value = "Id of the vendor inventory", example = "123")
    private Long id;

    @JsonProperty("vendor")
    @ApiModelProperty(value = "Vendor representation")
    private VendorRepresentation vendor;

    @JsonProperty("originalHeat")
    @ApiModelProperty(value = "Original heat representation")
    private HeatRepresentation originalHeat;

    @JsonProperty("rawMaterialProduct")
    @ApiModelProperty(value = "Raw material product representation")
    private RawMaterialProductRepresentation rawMaterialProduct;

    @JsonProperty("heatNumber")
    @ApiModelProperty(value = "Heat number")
    private String heatNumber;

    @JsonProperty("totalDispatchedQuantity")
    @ApiModelProperty(value = "Total quantity originally dispatched")
    private Double totalDispatchedQuantity;

    @JsonProperty("availableQuantity")
    @ApiModelProperty(value = "Current available quantity at vendor")
    private Double availableQuantity;

    @JsonProperty("isInPieces")
    @ApiModelProperty(value = "Whether this inventory is tracked in pieces")
    private Boolean isInPieces;

    @JsonProperty("totalDispatchedPieces")
    @ApiModelProperty(value = "Total pieces originally dispatched")
    private Integer totalDispatchedPieces;

    @JsonProperty("availablePiecesCount")
    @ApiModelProperty(value = "Current available pieces at vendor")
    private Integer availablePiecesCount;

    @JsonProperty("testCertificateNumber")
    @ApiModelProperty(value = "Test certificate number")
    private String testCertificateNumber;

    @JsonProperty("createdAt")
    @ApiModelProperty(value = "Timestamp at which the vendor inventory was created")
    private String createdAt;

    @JsonProperty("updatedAt")
    @ApiModelProperty(value = "Timestamp at which the vendor inventory was updated")
    private String updatedAt;
} 