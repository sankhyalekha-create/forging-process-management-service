package com.jangid.forging_process_management_service.dto.gst.einvoice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Size;

/**
 * DTO for E-Invoice credentials provided per-session
 * These credentials are NOT stored in database - only used for session authentication
 * Similar to EwayBillSessionCredentialsDTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EInvoiceSessionCredentialsDTO {

    /**
     * E-Invoice Portal Username
     * Required for first request in session
     */
    @Size(min = 3, max = 100, message = "Username must be between 3 and 100 characters")
    private String einvUsername;

    /**
     * E-Invoice Portal Password (plain text - will be encrypted by backend)
     * Required for first request in session
     */
    @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
    private String einvPassword;

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
