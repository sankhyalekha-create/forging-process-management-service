package com.jangid.forging_process_management_service.entities.gst;

import com.jangid.forging_process_management_service.entities.Tenant;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity to store tenant-specific E-Invoice credentials
 * References shared GSP configuration for common ASP credentials
 * Similar pattern to TenantEwayBillCredentials
 */
@Entity
@Table(name = "tenant_einvoice_credentials")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantEInvoiceCredentials {

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
     * E-Invoice GSTIN (tenant-specific)
     */
    @Column(name = "einv_gstin", nullable = false, length = 15)
    private String einvGstin;

    /**
     * Current Auth Token
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
     * Threshold value for mandatory E-Invoice (default 5 crores = 5,00,00,000 as per GST regulations)
     */
    @Column(name = "einv_threshold")
    private Double einvThreshold = 50000000.0;

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
     */
    public boolean hasValidCredentials() {
        return gspConfiguration != null 
            && gspConfiguration.isValid()
            && einvGstin != null 
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
