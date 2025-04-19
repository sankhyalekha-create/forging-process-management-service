package com.jangid.forging_process_management_service.entitiesRepresentation.security;

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
@ApiModel(description = "UserRegistration representation")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserRegistrationRepresentation {

  @JsonProperty("username")
  @ApiModelProperty(value = "username")
  private String username;

  @JsonProperty("password")
  @ApiModelProperty(value = "password")
  private String password;

  @JsonProperty("roles")
  @ApiModelProperty(value = "roles")
  private Set<String> roles;

  @JsonProperty("tenantId")
  @ApiModelProperty(value = "tenantId")
  private Long tenantId;

}
