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
@ApiModel(description = "Furnace representation")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FurnaceRepresentation {

  @JsonProperty(value = "id")
  @ApiModelProperty(value = "Id of the Furnace", example = "123")
  private Long id;

  @JsonProperty("furnaceName")
  @ApiModelProperty(value = "furnaceName")
  private String furnaceName;

  @JsonProperty("furnaceCapacity")
  @ApiModelProperty(value = "furnaceCapacity")
  private float furnaceCapacity;

  @JsonProperty("furnaceLocation")
  @ApiModelProperty(value = "furnaceLocation")
  private String furnaceLocation;

  @JsonProperty("furnaceDetails")
  @ApiModelProperty(value = "furnaceDetails")
  private String furnaceDetails;

  @JsonProperty("furnaceStatus")
  @ApiModelProperty(value = "furnaceStatus")
  private String furnaceStatus;

  @JsonProperty("createdAt")
  @ApiModelProperty(value = "Timestamp at which the furnace entity was created")
  private String createdAt;


  @JsonProperty("updatedAt")
  @ApiModelProperty(value = "Timestamp at which the furnace entity was updated")
  private String updatedAt;

}
