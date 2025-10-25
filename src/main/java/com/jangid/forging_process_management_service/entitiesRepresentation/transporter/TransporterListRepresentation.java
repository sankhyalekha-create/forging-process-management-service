package com.jangid.forging_process_management_service.entitiesRepresentation.transporter;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Wrapper representation for a list of transporters.
 * Used for API responses that return multiple transporter records.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ApiModel(description = "List of transporter representations")
public class TransporterListRepresentation {
  
  @JsonProperty("transporterRepresentations")
  @ApiModelProperty(value = "List of transporters")
  private List<TransporterRepresentation> transporterRepresentations;
}

