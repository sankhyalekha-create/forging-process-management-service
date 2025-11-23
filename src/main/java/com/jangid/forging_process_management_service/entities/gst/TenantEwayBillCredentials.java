package com.jangid.forging_process_management_service.entities.gst;

import com.jangid.forging_process_management_service.entities.Tenant;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity to store tenant-specific E-Way Bill credentials
 * References shared GSP configuration for common ASP credentials
 */
@Entity
@Table(name = "tenant_eway_bill_credentials")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantEwayBillCredentials {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false, unique = true)
    private Tenant tenant;

    /**
     * Reference to shared GSP Configuration
     * Contains ASP credentials, URLs, and integration mode
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "gsp_config_id", nullable = false)
    private GspConfiguration gspConfiguration;

    /**
     * E-Way Bill GSTIN (tenant-specific)
     * NOTE: Username and password are NO LONGER stored in database.
     * They are provided per-session for enhanced security.
     */
    @Column(name = "ewb_gstin", nullable = false, length = 15)
    private String ewbGstin;

    /**
     * Current Auth Token
     * Generated when user provides credentials during session
     */
    @Column(name = "auth_token", length = 1000)
    private String authToken;

    /**
     * Session Encryption Key (SEK)
     */
    @Column(name = "sek", length = 500)
    private String sek;

    /**
     * Token expiry timestamp
     */
    @Column(name = "token_expiry")
    private LocalDateTime tokenExpiry;

    /**
     * Threshold value for mandatory E-Way Bill (default 50000)
     */
    @Column(name = "ewb_threshold", precision = 15, scale = 2)
    private BigDecimal ewbThreshold = new BigDecimal("50000.00");

    /**
     * Is this tenant configuration active
     */
    @Column(name = "is_active")
    private Boolean isActive = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Check if auth token is valid
     */
    public boolean isTokenValid() {
        if (authToken == null || tokenExpiry == null) {
            return false;
        }
        return LocalDateTime.now().isBefore(tokenExpiry);
    }

    /**
     * Check if credentials are configured
     * NOTE: Now only checks GSP configuration and GSTIN
     * Username/password are provided per-session
     */
    public boolean hasValidCredentials() {
        return gspConfiguration != null 
            && gspConfiguration.isValid()
            && ewbGstin != null 
            && isActive != null 
            && isActive;
    }

    /**
     * Check if GSP API mode is enabled
     */
    public boolean isGspApiMode() {
        if (gspConfiguration == null) {
            return false;
        }
        String mode = gspConfiguration.getIntegrationMode();
        return "GSP_API".equalsIgnoreCase(mode) || "HYBRID".equalsIgnoreCase(mode);
    }

    /**
     * Get ASP User ID from GSP Configuration
     */
    public String getAspUserId() {
        return gspConfiguration != null ? gspConfiguration.getAspUserId() : null;
    }

    /**
     * Get ASP Password from GSP Configuration
     */
    public String getAspPassword() {
        return gspConfiguration != null ? gspConfiguration.getAspPassword() : null;
    }
}
