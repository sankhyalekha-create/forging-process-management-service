package com.jangid.forging_process_management_service.dto.gst.gsp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Size;

/**
 * DTO for E-Way Bill credentials provided per-session
 * These credentials are NOT stored in database - only used for session authentication
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EwayBillSessionCredentialsDTO {

    /**
     * E-Way Bill Portal Username
     * Required for first request in session
     */
    @Size(min = 3, max = 100, message = "Username must be between 3 and 100 characters")
    private String ewbUsername;

    /**
     * E-Way Bill Portal Password (plain text - will be encrypted by backend)
     * Required for first request in session
     */
    @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
    private String ewbPassword;

    /**
     * Optional: Session token from previous authentication
     * If provided and valid, credentials are not required
     */
    private String sessionToken;

    /**
     * GSP Server ID (selected by user)
     * Example: "primary-sandbox", "backup1-mumbai", "backup2-delhi"
     * If not provided, defaults to primary server
     */
    private String gspServerId;
}
