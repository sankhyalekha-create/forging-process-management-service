package com.jangid.forging_process_management_service.entitiesRepresentation.order;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;

/**
 * Lightweight request representation for checking inventory availability
 * Used specifically for the inventory check endpoint to avoid requiring
 * unnecessary fields like itemWorkflowId
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "Request for checking inventory availability for order items")
public class InventoryCheckRequest {

  @JsonProperty("itemId")
  @ApiModelProperty(value = "Item ID to check inventory for", example = "789", required = true)
  @NotNull(message = "Item ID is required")
  private Long itemId;

  @JsonProperty("quantity")
  @ApiModelProperty(value = "Quantity of items needed", example = "100", required = true)
  @NotNull(message = "Quantity is required")
  @Min(value = 1, message = "Quantity must be at least 1")
  private Integer quantity;

  @JsonProperty("workType")
  @ApiModelProperty(value = "Work Type: JOB_WORK_ONLY or WITH_MATERIAL", example = "WITH_MATERIAL")
  private String workType;
}

