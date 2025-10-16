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

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ApiModel(description = "Update Order Priority Request")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateOrderPriorityRequest {

  @JsonProperty("priority")
  @ApiModelProperty(value = "New Priority (1=highest)", example = "2", required = true)
  @NotNull(message = "Priority is required")
  @Min(value = 1, message = "Priority must be at least 1")
  private Integer priority;

  @JsonProperty("notes")
  @ApiModelProperty(value = "Priority change notes")
  private String notes;
}
