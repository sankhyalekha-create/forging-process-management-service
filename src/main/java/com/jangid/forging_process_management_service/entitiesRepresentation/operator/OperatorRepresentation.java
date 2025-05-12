package com.jangid.forging_process_management_service.entitiesRepresentation.operator;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ApiModel(description = "OperatorRepresentation")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OperatorRepresentation {

  @JsonProperty("id")
  @ApiModelProperty(value = "Id of the Machine Operator", example = "123")
  private Long id;

  @JsonProperty("fullName")
  @ApiModelProperty(value = "Full name of the Machine Operator", example = "John Doe")
  private String fullName;

  @JsonProperty("address")
  @ApiModelProperty(value = "Address of the Machine Operator", example = "123 Street, City")
  private String address;

  @JsonProperty("aadhaarNumber")
  @ApiModelProperty(value = "Aadhaar number of the Machine Operator", example = "1234-5678-9012")
  private String aadhaarNumber;

  @JsonProperty("phoneNumber")
  @ApiModelProperty(value = "Phone number of the Machine Operator", example = "+919876543210")
  private String phoneNumber;

  @JsonProperty("dateOfBirth")
  @ApiModelProperty(value = "Date of birth of the Machine Operator", example = "1990-01-01")
  private LocalDate dateOfBirth;

  @JsonProperty("dateOfJoining")
  @ApiModelProperty(value = "Date when the Machine Operator joined", example = "2023-01-01")
  private LocalDate dateOfJoining;

  @JsonProperty("dateOfLeaving")
  @ApiModelProperty(value = "Date when the Machine Operator left, null if still active", example = "2023-12-31")
  private LocalDate dateOfLeaving;

  @JsonProperty("hourlyWages")
  @ApiModelProperty(value = "Hourly wages paid to the Machine Operator", example = "120.50")
  private BigDecimal hourlyWages;

  @JsonProperty("tenantId")
  @ApiModelProperty(value = "Id of the associated Tenant", example = "1")
  private Long tenantId;

  @JsonProperty("previousTenantIds")
  @ApiModelProperty(value = "List of previous tenant IDs", example = "[2,3]")
  private List<Long> previousTenantIds;

  @JsonProperty("operatorType")
  @ApiModelProperty(value = "operatorType")
  private OperatorType operatorType;
}
