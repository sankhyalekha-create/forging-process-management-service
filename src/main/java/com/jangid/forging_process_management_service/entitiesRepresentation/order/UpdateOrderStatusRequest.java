package com.jangid.forging_process_management_service.entitiesRepresentation.order;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ApiModel(description = "Update Order Status Request")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateOrderStatusRequest {

  @JsonProperty("orderStatus")
  @ApiModelProperty(value = "New Order Status", example = "IN_PROGRESS", required = true)
  @NotNull(message = "Order status is required")
  private String orderStatus;

  @JsonProperty("notes")
  @ApiModelProperty(value = "Status change notes")
  private String notes;
}
