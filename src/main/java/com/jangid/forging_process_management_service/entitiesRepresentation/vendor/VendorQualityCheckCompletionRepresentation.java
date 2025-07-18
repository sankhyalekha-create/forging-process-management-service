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

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ApiModel(description = "Vendor quality check completion representation")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VendorQualityCheckCompletionRepresentation {

    @JsonProperty("finalVendorRejectsCount")
    @ApiModelProperty(value = "Final vendor rejects count after quality check", example = "5", required = true)
    @NotNull(message = "Final vendor rejects count is required")
    @Min(value = 0, message = "Final vendor rejects count must be non-negative")
    private Integer finalVendorRejectsCount;

    @JsonProperty("finalTenantRejectsCount")
    @ApiModelProperty(value = "Final tenant rejects count after quality check", example = "3", required = true)
    @NotNull(message = "Final tenant rejects count is required")
    @Min(value = 0, message = "Final tenant rejects count must be non-negative")
    private Integer finalTenantRejectsCount;

    @JsonProperty("qualityCheckRemarks")
    @ApiModelProperty(value = "Quality check completion remarks", example = "Surface defects found in 5 pieces, dimensional issues in 3 pieces")
    private String qualityCheckRemarks;

    /**
     * Get total final rejects count
     */
    public Integer getTotalFinalRejectsCount() {
        int vendorRejects = this.finalVendorRejectsCount != null ? this.finalVendorRejectsCount : 0;
        int tenantRejects = this.finalTenantRejectsCount != null ? this.finalTenantRejectsCount : 0;
        return vendorRejects + tenantRejects;
    }
} 