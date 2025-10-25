package com.jangid.forging_process_management_service.entitiesRepresentation.transporter;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Representation class for Transporter entity.
 * Used for API request/response serialization.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ApiModel(description = "Transporter representation for GST-compliant invoice generation")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransporterRepresentation {
  
  @JsonProperty(value = "id")
  @ApiModelProperty(value = "Unique identifier of the transporter", example = "123")
  private Long id;
  
  @JsonProperty("transporterName")
  @ApiModelProperty(value = "Legal/registered name of the transporter", required = true, example = "XYZ Transport Services Pvt Ltd")
  private String transporterName;
  
  @JsonProperty("gstin")
  @ApiModelProperty(value = "GST Identification Number (15 digits)", example = "22ABCDE1234F1Z5")
  private String gstin;
  
  @JsonProperty("transporterIdNumber")
  @ApiModelProperty(value = "Transporter ID for E-Way Bill (GSTIN or TIN)", example = "22ABCDE1234F1Z5")
  private String transporterIdNumber;
  
  @JsonProperty("panNumber")
  @ApiModelProperty(value = "PAN card number (10 characters)", example = "ABCDE1234F")
  private String panNumber;
  
  @JsonProperty("address")
  @ApiModelProperty(value = "Complete registered address", example = "123, Transport Nagar, Industrial Area")
  private String address;
  
  @JsonProperty("stateCode")
  @ApiModelProperty(value = "State code as per GST (2 digits)", example = "09")
  private String stateCode;
  
  @JsonProperty("pincode")
  @ApiModelProperty(value = "PIN code (6 digits)", example = "302001")
  private String pincode;
  
  @JsonProperty("phoneNumber")
  @ApiModelProperty(value = "Primary contact phone number", example = "9876543210")
  private String phoneNumber;
  
  @JsonProperty("alternatePhoneNumber")
  @ApiModelProperty(value = "Alternate contact phone number", example = "0141-2345678")
  private String alternatePhoneNumber;
  
  @JsonProperty("email")
  @ApiModelProperty(value = "Email address", example = "contact@xyztransport.com")
  private String email;
  
  @JsonProperty("isGstRegistered")
  @ApiModelProperty(value = "Whether the transporter is registered under GST", example = "true")
  private boolean isGstRegistered;
  
  @JsonProperty("bankAccountNumber")
  @ApiModelProperty(value = "Bank account number for payment processing", example = "1234567890123456")
  private String bankAccountNumber;
  
  @JsonProperty("ifscCode")
  @ApiModelProperty(value = "IFSC code of the bank", example = "SBIN0001234")
  private String ifscCode;
  
  @JsonProperty("bankName")
  @ApiModelProperty(value = "Bank name", example = "State Bank of India")
  private String bankName;
  
  @JsonProperty("notes")
  @ApiModelProperty(value = "Additional notes or remarks", example = "Preferred for long-distance transport")
  private String notes;
}

