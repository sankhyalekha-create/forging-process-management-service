package com.jangid.forging_process_management_service.entitiesRepresentation.machining;

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
@ApiModel(description = "Machine representation")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MachineRepresentation {

  @JsonProperty(value = "id")
  @ApiModelProperty(value = "Id of the Machine", example = "123")
  private Long id;

  @JsonProperty("machineName")
  @ApiModelProperty(value = "machineName")
  private String machineName;

  @JsonProperty("machineLocation")
  @ApiModelProperty(value = "machineLocation")
  private String machineLocation;

  @JsonProperty("machineDetails")
  @ApiModelProperty(value = "machineDetails")
  private String machineDetails;

  @JsonProperty(value = "tenantId")
  @ApiModelProperty(value = "Id of the tenant", example = "123")
  private Long tenantId;

}
