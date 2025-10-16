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

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ApiModel(description = "Order Statistics")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderStatisticsRepresentation {

    @JsonProperty("total")
    @ApiModelProperty(value = "Total number of orders", example = "150")
    private Long total;

    @JsonProperty("received")
    @ApiModelProperty(value = "Number of received orders", example = "25")
    private Long received;

    @JsonProperty("inProgress")
    @ApiModelProperty(value = "Number of orders in progress", example = "45")
    private Long inProgress;

    @JsonProperty("completed")
    @ApiModelProperty(value = "Number of completed orders", example = "35")
    private Long completed;

    @JsonProperty("cancelled")
    @ApiModelProperty(value = "Number of cancelled orders", example = "5")
    private Long cancelled;

    @JsonProperty("overdue")
    @ApiModelProperty(value = "Number of overdue orders", example = "8")
    private Long overdue;

    @JsonProperty("startDate")
    @ApiModelProperty(value = "Start date for statistics period", example = "2024-10-01")
    private String startDate;

    @JsonProperty("endDate")
    @ApiModelProperty(value = "End date for statistics period", example = "2024-10-31")
    private String endDate;
}
