package com.jangid.forging_process_management_service.dto.gst.einvoice;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for E-Invoice Authentication API
 * Maps to response from: GET /eivital/dec/v1.04/auth
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class EInvoiceAuthResponse {

    /**
     * Status code: "1" for success, "0" for failure
     */
    @JsonProperty("Status")
    private String status;

    /**
     * Authentication token for subsequent API calls
     */
    @JsonProperty("AuthToken")
    private String authToken;

    /**
     * Session Encryption Key (SEK) - Used for encrypting sensitive data
     */
    @JsonProperty("Sek")
    private String sek;

    /**
     * Token expiry timestamp (milliseconds since epoch)
     */
    @JsonProperty("TokenExpiry")
    private Long tokenExpiry;

    /**
     * Error message if authentication fails
     */
    @JsonProperty("ErrorMessage")
    private String errorMessage;

    /**
     * Error code if authentication fails
     */
    @JsonProperty("ErrorCode")
    private String errorCode;

    /**
     * Check if authentication was successful
     */
    public boolean isSuccess() {
        return "1".equals(status) && authToken != null && !authToken.isEmpty();
    }

    /**
     * Get error details for failed authentication
     */
    public String getErrorDetails() {
        if (errorMessage != null) {
            return String.format("Error [%s]: %s", errorCode, errorMessage);
        }
        return "Authentication failed";
    }
}
