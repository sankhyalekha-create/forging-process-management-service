package com.jangid.forging_process_management_service.entitiesRepresentation.vendor;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jangid.forging_process_management_service.entitiesRepresentation.inventory.HeatRepresentation;

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
@ApiModel(description = "VendorDispatchHeat representation")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VendorDispatchHeatRepresentation {

    @JsonProperty("id")
    @ApiModelProperty(value = "Id of the VendorDispatchHeat", example = "123")
    private Long id;

    @JsonProperty("processedItemVendorDispatchBatch")
    @ApiModelProperty(value = "ProcessedItemVendorDispatchBatch associated with this heat consumption")
    private ProcessedItemVendorDispatchBatchRepresentation processedItemVendorDispatchBatch;

    @JsonProperty("heat")
    @ApiModelProperty(value = "Heat that was consumed", required = true)
    private HeatRepresentation heat;

    @JsonProperty("consumptionType")
    @ApiModelProperty(value = "Type of consumption: QUANTITY or PIECES", example = "QUANTITY", required = true, allowableValues = "QUANTITY,PIECES")
    private String consumptionType;

    @JsonProperty("quantityUsed")
    @ApiModelProperty(value = "Quantity used from the heat (for QUANTITY consumption type)", example = "15.5")
    private Double quantityUsed;

    @JsonProperty("piecesUsed")
    @ApiModelProperty(value = "Number of pieces used from the heat (for PIECES consumption type)", example = "100")
    private Integer piecesUsed;

    @JsonProperty("createdAt")
    @ApiModelProperty(value = "Timestamp when the heat consumption was recorded")
    private String createdAt;

    @JsonProperty("updatedAt")
    @ApiModelProperty(value = "Timestamp when the heat consumption was last updated")
    private String updatedAt;

    @JsonProperty("deletedAt")
    @ApiModelProperty(value = "Timestamp when the heat consumption was deleted")
    private String deletedAt;

    @JsonProperty("deleted")
    @ApiModelProperty(value = "Flag indicating if the heat consumption is deleted")
    private Boolean deleted;
} 