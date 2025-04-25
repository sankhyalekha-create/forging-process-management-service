package com.jangid.forging_process_management_service.entitiesRepresentation.dispatch;

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
@ApiModel(description = "Dispatch Statistics representation for a given month")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DispatchStatisticsRepresentation {

    @JsonProperty(value = "year")
    @ApiModelProperty(value = "Year of the dispatch statistic", example = "2024")
    private int year;

    @JsonProperty(value = "month")
    @ApiModelProperty(value = "Month of the dispatch statistic (1-12)", example = "7")
    private int month;

    @JsonProperty(value = "totalDispatchedPieces")
    @ApiModelProperty(value = "Total pieces dispatched in this month", example = "5000")
    private long totalDispatchedPieces;
} 