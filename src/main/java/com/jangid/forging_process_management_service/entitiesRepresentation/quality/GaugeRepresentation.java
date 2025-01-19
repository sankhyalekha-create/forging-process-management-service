package com.jangid.forging_process_management_service.entitiesRepresentation.quality;

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
@ApiModel(description = "Gauge representation")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GaugeRepresentation {

  @JsonProperty(value = "id")
  @ApiModelProperty(value = "Id of the Gauge", example = "123")
  private Long id;

  @JsonProperty("gaugeName")
  @ApiModelProperty(value = "gaugeName")
  private String gaugeName;

  @JsonProperty("gaugeLocation")
  @ApiModelProperty(value = "gaugeLocation")
  private String gaugeLocation;

  @JsonProperty("gaugeDetails")
  @ApiModelProperty(value = "gaugeDetails")
  private String gaugeDetails;

  @JsonProperty(value = "tenantId")
  @ApiModelProperty(value = "Id of the tenant", example = "123")
  private Long tenantId;

}

