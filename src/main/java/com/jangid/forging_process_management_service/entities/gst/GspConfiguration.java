package com.jangid.forging_process_management_service.entities.gst;

import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Shared GSP Configuration for E-Way Bill integration
 * Stores common ASP credentials and GSP provider details
 * Multiple tenants can reference the same GSP configuration
 */
@Entity
@Table(name = "gsp_configuration")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GspConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Configuration name (e.g., "Sandbox", "Production")
     */
    @Column(name = "config_name", nullable = false, unique = true, length = 100)
    private String configName;

    /**
     * GSP Provider name
     */
    @Column(name = "gsp_provider", nullable = false, length = 100)
    private String gspProvider;

    /**
     * Integration mode: GSP_API, OFFLINE, HYBRID
     */
    @Column(name = "integration_mode", length = 20, nullable = false)
    private String integrationMode = "GSP_API";

    /**
     * ASP User ID (shared across tenants using this GSP)
     */
    @Column(name = "asp_user_id", nullable = false, length = 100)
    private String aspUserId;

    /**
     * ASP Password (encrypted)
     */
    @Column(name = "asp_password", nullable = false, length = 500)
    private String aspPassword;

    /**
     * Is this a production configuration
     */
    @Column(name = "is_production")
    private Boolean isProduction = false;

    /**
     * Is this configuration active
     */
    @Column(name = "is_active")
    private Boolean isActive = true;

    /**
     * Description/notes about this configuration
     */
    @Column(name = "description", length = 500)
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Check if configuration is valid and active
     */
    public boolean isValid() {
        return isActive && aspUserId != null && aspPassword != null;
    }

    /**
     * Get environment type (Sandbox/Production)
     */
    public String getEnvironmentType() {
        return isProduction ? "Production" : "Sandbox";
    }
}
