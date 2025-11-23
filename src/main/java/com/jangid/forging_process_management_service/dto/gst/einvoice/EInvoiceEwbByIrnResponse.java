package com.jangid.forging_process_management_service.dto.gst.einvoice;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for Generate E-Way Bill by IRN API
 * Maps to response from: POST /eiewb/dec/v1.03/ewaybill
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class EInvoiceEwbByIrnResponse {

    /**
     * Success status
     */
    @JsonProperty("Success")
    private String success;

    /**
     * E-Way Bill Number
     */
    @JsonProperty("EwbNo")
    private Long ewbNo;

    /**
     * E-Way Bill Date (format: dd/MM/yyyy HH:mm:ss)
     */
    @JsonProperty("EwbDt")
    private String ewbDt;

    /**
     * E-Way Bill Valid Until (format: dd/MM/yyyy HH:mm:ss)
     */
    @JsonProperty("EwbValidTill")
    private String ewbValidTill;

    /**
     * Alert message
     */
    @JsonProperty("alert")
    private String alert;

    /**
     * Error message if generation fails
     */
    @JsonProperty("ErrorMessage")
    private String errorMessage;

    /**
     * Error code if generation fails
     */
    @JsonProperty("ErrorCode")
    private String errorCode;

    /**
     * Info details
     */
    @JsonProperty("InfoDtls")
    private String infoDtls;

    /**
     * Check if E-Way Bill generation was successful
     */
    public boolean isSuccess() {
        return "Y".equalsIgnoreCase(success) && ewbNo != null && ewbNo > 0;
    }

    /**
     * Get error details for failed generation
     */
    public String getErrorDetails() {
        if (errorMessage != null) {
            return String.format("Error [%s]: %s", errorCode, errorMessage);
        }
        return "E-Way Bill generation failed";
    }
}
