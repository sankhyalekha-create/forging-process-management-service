package com.jangid.forging_process_management_service.dto.gst.gsp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO containing session token for subsequent E-Way Bill operations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EwayBillSessionTokenResponse {

    /**
     * Session token for subsequent API calls
     * Valid for 30 minutes of inactivity
     */
    private String sessionToken;

    /**
     * GSP auth token (for reference)
     */
    private String authToken;

    /**
     * Session expiry timestamp
     */
    private LocalDateTime expiresAt;

    /**
     * Session creation timestamp
     */
    private LocalDateTime createdAt;

    /**
     * Success message
     */
    private String message;

    /**
     * GSTIN associated with this session
     */
    private String gstin;
}
