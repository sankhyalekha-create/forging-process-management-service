package com.jangid.forging_process_management_service.dto.gst.gsp;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for GSP Authentication
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GspAuthResponse {

    /**
     * Status: "1" = Success, "0" = Failure
     */
    @JsonProperty("status")
    private String status;

    /**
     * Auth token (valid for 6 hours typically)
     */
    @JsonProperty("authtoken")
    private String authtoken;

    /**
     * Session Encryption Key (empty in v1.03)
     */
    @JsonProperty("sek")
    private String sek;

    /**
     * Error message
     */
    @JsonProperty("message")
    private String message;

    /**
     * Check if authentication was successful
     */
    public boolean isSuccess() {
        return "1".equals(status);
    }
}
