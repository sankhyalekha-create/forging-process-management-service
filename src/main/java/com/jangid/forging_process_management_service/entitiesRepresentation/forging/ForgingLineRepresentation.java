package com.jangid.forging_process_management_service.entitiesRepresentation.forging;

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
@ApiModel(description = "ForgingLine representation")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ForgingLineRepresentation {

  @JsonProperty(value = "id")
  @ApiModelProperty(value = "Id of the Furnace", example = "123")
  private Long id;

  @JsonProperty("forgingLineName")
  @ApiModelProperty(value = "forgingLineName")
  private String forgingLineName;

  @JsonProperty("forgingDetails")
  @ApiModelProperty(value = "forgingDetails")
  private String forgingDetails;

  @JsonProperty("forgingLineStatus")
  @ApiModelProperty(value = "forgingLineStatus")
  private String forgingLineStatus;

  @JsonProperty("createdAt")
  @ApiModelProperty(value = "Timestamp at which the forgingLine entity was created")
  private String createdAt;


  @JsonProperty("updatedAt")
  @ApiModelProperty(value = "Timestamp at which the forgingLine entity was updated")
  private String updatedAt;

}
