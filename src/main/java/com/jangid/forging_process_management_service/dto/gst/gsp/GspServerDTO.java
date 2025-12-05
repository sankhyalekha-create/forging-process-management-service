package com.jangid.forging_process_management_service.dto.gst.gsp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing a GSP server configuration option
 * Used by frontend to display server selection dropdown
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GspServerDTO {

    /**
     * Unique identifier for the server
     * Example: "primary-sandbox", "backup1-mumbai", "backup2-delhi"
     */
    private String id;

    /**
     * Display name for the server
     * Example: "Primary - Sandbox", "Backup 1 - Mumbai"
     */
    private String name;

    /**
     * Description of the server
     * Example: "TaxPro GSP Sandbox Server (Testing)"
     */
    private String description;

    /**
     * Flag indicating if this server is currently enabled
     * Set to false if URLs are empty/not configured
     */
    private boolean enabled;
}
