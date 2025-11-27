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
 * 
 * Sample Response:
 * {
 *   "Status": 1,
 *   "Data": {
 *     "ClientId": "AACCC29GSPR5CM0",
 *     "UserName": "TaxProEnvPON",
 *     "AuthToken": "1e69D8U6fuq38FtIGSuH4Bayw",
 *     "Sek": "",
 *     "TokenExpiry": "2025-11-24 15:52:12"
 *   },
 *   "ErrorDetails": null,
 *   "InfoDtls": null
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class EInvoiceAuthResponse {

    /**
     * Status code: 1 for success, 0 for failure
     */
    @JsonProperty("Status")
    private Integer status;

    /**
     * Authentication data containing token and user details
     */
    @JsonProperty("Data")
    private AuthData data;

    /**
     * Error details if authentication fails
     */
    @JsonProperty("ErrorDetails")
    private Object errorDetails;

    /**
     * Additional information details
     */
    @JsonProperty("InfoDtls")
    private Object infoDtls;

    /**
     * Nested authentication data
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AuthData {
        
        /**
         * Client ID (GSTIN-based identifier)
         */
        @JsonProperty("ClientId")
        private String clientId;

        /**
         * Username used for authentication
         */
        @JsonProperty("UserName")
        private String userName;

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
         * Token expiry timestamp in format "YYYY-MM-DD HH:mm:ss"
         */
        @JsonProperty("TokenExpiry")
        private String tokenExpiry;
    }

    /**
     * Check if authentication was successful
     */
    public boolean isSuccess() {
        return status != null && status == 1 && data != null && 
               data.getAuthToken() != null && !data.getAuthToken().isEmpty();
    }

    /**
     * Get authentication token (convenience method)
     */
    public String getAuthToken() {
        return data != null ? data.getAuthToken() : null;
    }

    /**
     * Get session encryption key (convenience method)
     */
    public String getSek() {
        return data != null ? data.getSek() : null;
    }

    /**
     * Get token expiry (convenience method)
     */
    public String getTokenExpiry() {
        return data != null ? data.getTokenExpiry() : null;
    }

    /**
     * Get error details for failed authentication
     */
    public String getErrorDetails() {
        if (errorDetails != null) {
            return errorDetails.toString();
        }
        if (status != null && status == 0) {
            return "Authentication failed - Status: 0";
        }
        return "Authentication failed";
    }
}
