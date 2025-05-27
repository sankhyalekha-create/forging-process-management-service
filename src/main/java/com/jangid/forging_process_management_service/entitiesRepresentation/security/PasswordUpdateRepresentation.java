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

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ApiModel(description = "Password update representation")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PasswordUpdateRepresentation {

  @JsonProperty("username")
  @ApiModelProperty(value = "username")
  private String username;

  @JsonProperty("currentPassword")
  @ApiModelProperty(value = "current password")
  private String currentPassword;

  @JsonProperty("newPassword")
  @ApiModelProperty(value = "new password")
  private String newPassword;

  @JsonProperty("tenantId")
  @ApiModelProperty(value = "tenant ID")
  private Long tenantId;

  @JsonProperty("tenantName")
  @ApiModelProperty(value = "tenant name")
  private String tenantName;
} 