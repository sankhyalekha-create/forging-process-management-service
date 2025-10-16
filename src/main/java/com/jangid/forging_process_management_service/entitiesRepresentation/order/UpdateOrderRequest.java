package com.jangid.forging_process_management_service.entitiesRepresentation.order;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ApiModel(description = "Update Order Request")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateOrderRequest {

    @JsonProperty("poNumber")
    @ApiModelProperty(value = "Purchase Order Number", example = "PO-2024-001", required = true)
    @NotBlank(message = "PO Number is required")
    @Size(max = 100, message = "PO Number cannot exceed 100 characters")
    private String poNumber;

    @JsonProperty("orderDate")
    @ApiModelProperty(value = "Order Date (YYYY-MM-DD)", example = "2024-01-15", required = true)
    @NotNull(message = "Order Date is required")
    private LocalDate orderDate;

    @JsonProperty("buyerId")
    @ApiModelProperty(value = "Buyer ID", example = "123", required = true)
    @NotNull(message = "Buyer ID is required")
    private Long buyerId;

    @JsonProperty("expectedProcessingDays")
    @ApiModelProperty(value = "Expected Processing Days", example = "30")
    @Min(value = 1, message = "Expected Processing Days must be at least 1")
    private Integer expectedProcessingDays;

    @JsonProperty("userDefinedEtaDays")
    @ApiModelProperty(value = "User Defined ETA Days", example = "45")
    @Min(value = 1, message = "User Defined ETA Days must be at least 1")
    private Integer userDefinedEtaDays;

    @JsonProperty("priority")
    @ApiModelProperty(value = "Order Priority (1=highest)", example = "1", required = true)
    @NotNull(message = "Priority is required")
    @Min(value = 1, message = "Priority must be at least 1")
    private Integer priority;

    @JsonProperty("notes")
    @ApiModelProperty(value = "Order Notes")
    @Size(max = 500, message = "Notes cannot exceed 500 characters")
    private String notes;

    @JsonProperty("orderItems")
    @ApiModelProperty(value = "List of order items to be updated")
    private List<OrderItemRepresentation> orderItems;
}
