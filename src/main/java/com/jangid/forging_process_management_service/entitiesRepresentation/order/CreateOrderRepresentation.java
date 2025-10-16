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

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ApiModel(description = "Create Order Representation")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateOrderRepresentation {

  @JsonProperty("poNumber")
  @ApiModelProperty(value = "Purchase Order Number", example = "PO-2024-001", required = true)
  @NotBlank(message = "PO Number is required")
  @Size(max = 100, message = "PO Number cannot exceed 100 characters")
  private String poNumber;

  @JsonProperty("orderDate")
  @ApiModelProperty(value = "Order Date", example = "2024-01-15", required = true)
  @NotNull(message = "Order date is required")
  private LocalDate orderDate;

  @JsonProperty("buyerId")
  @ApiModelProperty(value = "Buyer ID", example = "123")
  private Long buyerId;


  @JsonProperty("expectedProcessingDays")
  @ApiModelProperty(value = "Expected Processing Days", example = "30")
  private Integer expectedProcessingDays;

  @JsonProperty("userDefinedEtaDays")
  @ApiModelProperty(value = "User Defined ETA Days (overrides calculated ETA)", example = "25")
  private Integer userDefinedEtaDays;

  @JsonProperty("notes")
  @ApiModelProperty(value = "Order Notes")
  @Size(max = 2000, message = "Notes cannot exceed 2000 characters")
  private String notes;

  @JsonProperty("priority")
  @ApiModelProperty(value = "Order Priority (1=highest)", example = "1")
  @Min(value = 1, message = "Priority must be at least 1")
  private Integer priority;

  @JsonProperty("orderItems")
  @ApiModelProperty(value = "List of Order Items", required = true)
  @NotEmpty(message = "Order must have at least one item")
  @Valid
  private List<OrderItemRepresentation> orderItems;
}
