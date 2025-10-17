package com.jangid.forging_process_management_service.entitiesRepresentation.order;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jangid.forging_process_management_service.entitiesRepresentation.buyer.BuyerRepresentation;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ApiModel(description = "Order Representation")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderRepresentation {

  @JsonProperty("id")
  @ApiModelProperty(value = "Order ID", example = "123")
  private Long id;

  @JsonProperty("poNumber")
  @ApiModelProperty(value = "Purchase Order Number", example = "PO-2024-001", required = true)
  private String poNumber;

  @JsonProperty("orderDate")
  @ApiModelProperty(value = "Order Date", example = "2024-01-15", required = true)
  private LocalDate orderDate;

  @JsonProperty("buyer")
  @ApiModelProperty(value = "Buyer Information", required = true)
  private BuyerRepresentation buyer;

  @JsonProperty("expectedProcessingDays")
  @ApiModelProperty(value = "Expected Processing Days", example = "30")
  private Integer expectedProcessingDays;

  @JsonProperty("userDefinedEtaDays")
  @ApiModelProperty(value = "User Defined ETA Days (overrides calculated ETA)", example = "25")
  private Integer userDefinedEtaDays;

  @JsonProperty("calculatedEtaDays")
  @ApiModelProperty(value = "Calculated ETA Days", example = "30")
  private Integer calculatedEtaDays;

  @JsonProperty("expectedCompletionDate")
  @ApiModelProperty(value = "Expected Completion Date", example = "2024-02-15")
  private LocalDate expectedCompletionDate;

  @JsonProperty("orderStatus")
  @ApiModelProperty(value = "Order Status", example = "RECEIVED")
  private String orderStatus;

  @JsonProperty("totalProcessingDays")
  @ApiModelProperty(value = "Total Processing Days", example = "30")
  private Integer totalProcessingDays;

  @JsonProperty("totalOrderValue")
  @ApiModelProperty(value = "Total Order Value", example = "150000.00")
  private BigDecimal totalOrderValue;

  @JsonProperty("notes")
  @ApiModelProperty(value = "Order Notes")
  private String notes;

  @JsonProperty("priority")
  @ApiModelProperty(value = "Order Priority (1=highest)", example = "1")
  private Integer priority;

  @JsonProperty("orderItems")
  @ApiModelProperty(value = "List of Order Items")
  private List<OrderItemRepresentation> orderItems;

  @JsonProperty("tenantId")
  @ApiModelProperty(value = "Tenant ID", example = "1")
  private Long tenantId;

  @JsonProperty("createdAt")
  @ApiModelProperty(value = "Created At")
  private LocalDateTime createdAt;

  @JsonProperty("updatedAt")
  @ApiModelProperty(value = "Updated At")
  private LocalDateTime updatedAt;

  // Actual completion tracking fields
  @JsonProperty("actualStartDate")
  @ApiModelProperty(value = "Actual Start Date")
  private LocalDate actualStartDate;

  @JsonProperty("actualCompletionDate")
  @ApiModelProperty(value = "Actual Completion Date")
  private LocalDate actualCompletionDate;

  @JsonProperty("actualDurationDays")
  @ApiModelProperty(value = "Actual Duration in Days")
  private Integer actualDurationDays;

  // Additional computed fields for UI
  @JsonProperty("canEdit")
  @ApiModelProperty(value = "Whether order can be edited")
  private Boolean canEdit;

  @JsonProperty("canDelete")
  @ApiModelProperty(value = "Whether order can be deleted")
  private Boolean canDelete;

  @JsonProperty("totalItems")
  @ApiModelProperty(value = "Total number of items in order")
  private Integer totalItems;

  @JsonProperty("totalQuantity")
  @ApiModelProperty(value = "Total quantity across all items")
  private Integer totalQuantity;

  @JsonProperty("workflowsInProgress")
  @ApiModelProperty(value = "Number of workflows in progress")
  private Integer workflowsInProgress;

  @JsonProperty("workflowsCompleted")
  @ApiModelProperty(value = "Number of workflows completed")
  private Integer workflowsCompleted;

  @JsonProperty("overallProgress")
  @ApiModelProperty(value = "Overall progress percentage", example = "65.5")
  private Double overallProgress;

  @JsonProperty("isOverdue")
  @ApiModelProperty(value = "Whether order is overdue")
  private Boolean isOverdue;

  @JsonProperty("daysUntilCompletion")
  @ApiModelProperty(value = "Days until expected completion")
  private Integer daysUntilCompletion;

  @JsonProperty("hasInventoryShortage")
  @ApiModelProperty(value = "Whether order has inventory shortage")
  private Boolean hasInventoryShortage;
}
