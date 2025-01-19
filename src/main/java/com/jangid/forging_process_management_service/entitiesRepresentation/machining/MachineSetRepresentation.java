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

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ApiModel(description = "Machine Set Representation")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MachineSetRepresentation {

  @JsonProperty(value = "id")
  @ApiModelProperty(value = "Id of the Machine", example = "123")
  private Long id;

  @JsonProperty("machineSetName")
  @ApiModelProperty(value = "machineSetName")
  private String machineSetName;

  @JsonProperty("machineSetDescription")
  @ApiModelProperty(value = "machineSetDescription")
  private String machineSetDescription;

  @JsonProperty("machines")
  @ApiModelProperty(value = "machines")
  private Set<MachineRepresentation> machines;

  @JsonProperty("machineSetStatus")
  @ApiModelProperty(value = "machineSetStatus")
  private String machineSetStatus;

  @JsonProperty("machineSetRunningJobType")
  @ApiModelProperty(value = "machineSetRunningJobType")
  private String machineSetRunningJobType;

}
