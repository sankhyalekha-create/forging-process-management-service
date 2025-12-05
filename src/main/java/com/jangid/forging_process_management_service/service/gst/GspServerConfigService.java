package com.jangid.forging_process_management_service.service.gst;

import com.jangid.forging_process_management_service.dto.gst.gsp.GspServerDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for managing GSP server configurations
 * Loads server configurations from application.properties and provides them to the frontend
 * Maps are initialized once during bean creation for performance
 */
@Service
@Slf4j
public class GspServerConfigService {

    // E-Way Bill Primary Server Configuration
    @Value("${app.eway-bill.gsp.servers.primary.id:primary}")
    private String ewbPrimaryId;
    @Value("${app.eway-bill.gsp.servers.primary.name:Primary}")
    private String ewbPrimaryName;
    @Value("${app.eway-bill.gsp.servers.primary.description:TaxPro GSP Sandbox Server}")
    private String ewbPrimaryDescription;
    @Value("${app.eway-bill.gsp.servers.primary.auth-url:}")
    private String ewbPrimaryAuthUrl;
    @Value("${app.eway-bill.gsp.servers.primary.ewb-url:}")
    private String ewbPrimaryEwbUrl;
    @Value("${app.eway-bill.gsp.servers.primary.print-url:}")
    private String ewbPrimaryPrintUrl;

    // E-Way Bill Backup1 Server Configuration
    @Value("${app.eway-bill.gsp.servers.backup1.id:backup1-mumbai}")
    private String ewbBackup1Id;
    @Value("${app.eway-bill.gsp.servers.backup1.name:Backup 1 - Mumbai}")
    private String ewbBackup1Name;
    @Value("${app.eway-bill.gsp.servers.backup1.description:TaxPro GSP Production Server (Mumbai)}")
    private String ewbBackup1Description;
    @Value("${app.eway-bill.gsp.servers.backup1.auth-url:}")
    private String ewbBackup1AuthUrl;
    @Value("${app.eway-bill.gsp.servers.backup1.ewb-url:}")
    private String ewbBackup1EwbUrl;
    @Value("${app.eway-bill.gsp.servers.backup1.print-url:}")
    private String ewbBackup1PrintUrl;

    // E-Way Bill Backup2 Server Configuration
    @Value("${app.eway-bill.gsp.servers.backup2.id:backup2-delhi}")
    private String ewbBackup2Id;
    @Value("${app.eway-bill.gsp.servers.backup2.name:Backup 2 - Delhi}")
    private String ewbBackup2Name;
    @Value("${app.eway-bill.gsp.servers.backup2.description:TaxPro GSP Production Server (Delhi)}")
    private String ewbBackup2Description;
    @Value("${app.eway-bill.gsp.servers.backup2.auth-url:}")
    private String ewbBackup2AuthUrl;
    @Value("${app.eway-bill.gsp.servers.backup2.ewb-url:}")
    private String ewbBackup2EwbUrl;
    @Value("${app.eway-bill.gsp.servers.backup2.print-url:}")
    private String ewbBackup2PrintUrl;

    // E-Invoice Primary Server Configuration
    @Value("${app.einvoice.gsp.servers.primary.id:primary}")
    private String einvPrimaryId;
    @Value("${app.einvoice.gsp.servers.primary.name:Primary}")
    private String einvPrimaryName;
    @Value("${app.einvoice.gsp.servers.primary.description:TaxPro GSP Sandbox Server}")
    private String einvPrimaryDescription;
    @Value("${app.einvoice.gsp.servers.primary.auth-url:}")
    private String einvPrimaryAuthUrl;
    @Value("${app.einvoice.gsp.servers.primary.generate-url:}")
    private String einvPrimaryGenerateUrl;
    @Value("${app.einvoice.gsp.servers.primary.irn-url:}")
    private String einvPrimaryIrnUrl;
    @Value("${app.einvoice.gsp.servers.primary.ewb-by-irn-url:}")
    private String einvPrimaryEwbByIrnUrl;
    @Value("${app.einvoice.gsp.servers.primary.cancel-url:}")
    private String einvPrimaryCancelUrl;

    // E-Invoice Backup1 Server Configuration
    @Value("${app.einvoice.gsp.servers.backup1.id:backup1-mumbai}")
    private String einvBackup1Id;
    @Value("${app.einvoice.gsp.servers.backup1.name:Backup 1 - Mumbai}")
    private String einvBackup1Name;
    @Value("${app.einvoice.gsp.servers.backup1.description:TaxPro GSP Production Server (Mumbai)}")
    private String einvBackup1Description;
    @Value("${app.einvoice.gsp.servers.backup1.auth-url:}")
    private String einvBackup1AuthUrl;
    @Value("${app.einvoice.gsp.servers.backup1.generate-url:}")
    private String einvBackup1GenerateUrl;
    @Value("${app.einvoice.gsp.servers.backup1.irn-url:}")
    private String einvBackup1IrnUrl;
    @Value("${app.einvoice.gsp.servers.backup1.ewb-by-irn-url:}")
    private String einvBackup1EwbByIrnUrl;
    @Value("${app.einvoice.gsp.servers.backup1.cancel-url:}")
    private String einvBackup1CancelUrl;

    // E-Invoice Backup2 Server Configuration
    @Value("${app.einvoice.gsp.servers.backup2.id:backup2-delhi}")
    private String einvBackup2Id;
    @Value("${app.einvoice.gsp.servers.backup2.name:Backup 2 - Delhi}")
    private String einvBackup2Name;
    @Value("${app.einvoice.gsp.servers.backup2.description:TaxPro GSP Production Server (Delhi)}")
    private String einvBackup2Description;
    @Value("${app.einvoice.gsp.servers.backup2.auth-url:}")
    private String einvBackup2AuthUrl;
    @Value("${app.einvoice.gsp.servers.backup2.generate-url:}")
    private String einvBackup2GenerateUrl;
    @Value("${app.einvoice.gsp.servers.backup2.irn-url:}")
    private String einvBackup2IrnUrl;
    @Value("${app.einvoice.gsp.servers.backup2.ewb-by-irn-url:}")
    private String einvBackup2EwbByIrnUrl;
    @Value("${app.einvoice.gsp.servers.backup2.cancel-url:}")
    private String einvBackup2CancelUrl;

    // Cached server URL maps (initialized once during bean creation)
    private Map<String, Map<String, String>> ewbServerUrlsCache;
    private Map<String, Map<String, String>> einvServerUrlsCache;

    /**
     * Initialize server URL maps after properties are injected
     * This runs once during bean creation for performance
     */
    @PostConstruct
    public void init() {
        log.info("Initializing GSP server configuration maps");
        
        // Initialize E-Way Bill server URLs cache
        ewbServerUrlsCache = new HashMap<>();
        
        Map<String, String> primaryEwbUrls = new HashMap<>();
        primaryEwbUrls.put("auth-url", ewbPrimaryAuthUrl);
        primaryEwbUrls.put("ewb-url", ewbPrimaryEwbUrl);
        primaryEwbUrls.put("print-url", ewbPrimaryPrintUrl);
        ewbServerUrlsCache.put(ewbPrimaryId, primaryEwbUrls);
        
        Map<String, String> backup1EwbUrls = new HashMap<>();
        backup1EwbUrls.put("auth-url", ewbBackup1AuthUrl);
        backup1EwbUrls.put("ewb-url", ewbBackup1EwbUrl);
        backup1EwbUrls.put("print-url", ewbBackup1PrintUrl);
        ewbServerUrlsCache.put(ewbBackup1Id, backup1EwbUrls);
        
        Map<String, String> backup2EwbUrls = new HashMap<>();
        backup2EwbUrls.put("auth-url", ewbBackup2AuthUrl);
        backup2EwbUrls.put("ewb-url", ewbBackup2EwbUrl);
        backup2EwbUrls.put("print-url", ewbBackup2PrintUrl);
        ewbServerUrlsCache.put(ewbBackup2Id, backup2EwbUrls);
        
        // Initialize E-Invoice server URLs cache
        einvServerUrlsCache = new HashMap<>();
        
        Map<String, String> primaryEinvUrls = new HashMap<>();
        primaryEinvUrls.put("auth-url", einvPrimaryAuthUrl);
        primaryEinvUrls.put("generate-url", einvPrimaryGenerateUrl);
        primaryEinvUrls.put("irn-url", einvPrimaryIrnUrl);
        primaryEinvUrls.put("ewb-by-irn-url", einvPrimaryEwbByIrnUrl);
        primaryEinvUrls.put("cancel-url", einvPrimaryCancelUrl);
        einvServerUrlsCache.put(einvPrimaryId, primaryEinvUrls);
        
        Map<String, String> backup1EinvUrls = new HashMap<>();
        backup1EinvUrls.put("auth-url", einvBackup1AuthUrl);
        backup1EinvUrls.put("generate-url", einvBackup1GenerateUrl);
        backup1EinvUrls.put("irn-url", einvBackup1IrnUrl);
        backup1EinvUrls.put("ewb-by-irn-url", einvBackup1EwbByIrnUrl);
        backup1EinvUrls.put("cancel-url", einvBackup1CancelUrl);
        einvServerUrlsCache.put(einvBackup1Id, backup1EinvUrls);
        
        Map<String, String> backup2EinvUrls = new HashMap<>();
        backup2EinvUrls.put("auth-url", einvBackup2AuthUrl);
        backup2EinvUrls.put("generate-url", einvBackup2GenerateUrl);
        backup2EinvUrls.put("irn-url", einvBackup2IrnUrl);
        backup2EinvUrls.put("ewb-by-irn-url", einvBackup2EwbByIrnUrl);
        backup2EinvUrls.put("cancel-url", einvBackup2CancelUrl);
        einvServerUrlsCache.put(einvBackup2Id, backup2EinvUrls);
        
        log.info("GSP server configuration maps initialized successfully. EWB servers: {}, EINV servers: {}", 
                ewbServerUrlsCache.size(), einvServerUrlsCache.size());
    }

    /**
     * Get list of available E-Way Bill GSP servers
     * 
     * @return List of server DTOs with availability status
     */
    public List<GspServerDTO> getAvailableEwbServers() {
        List<GspServerDTO> servers = new ArrayList<>();

        // Add primary server
        servers.add(GspServerDTO.builder()
            .id(ewbPrimaryId)
            .name(ewbPrimaryName)
            .description(ewbPrimaryDescription)
            .enabled(isEwbServerConfigured(ewbPrimaryAuthUrl, ewbPrimaryEwbUrl, ewbPrimaryPrintUrl))
            .build());

        // Add backup1 server
        servers.add(GspServerDTO.builder()
            .id(ewbBackup1Id)
            .name(ewbBackup1Name)
            .description(ewbBackup1Description)
            .enabled(isEwbServerConfigured(ewbBackup1AuthUrl, ewbBackup1EwbUrl, ewbBackup1PrintUrl))
            .build());

        // Add backup2 server
        servers.add(GspServerDTO.builder()
            .id(ewbBackup2Id)
            .name(ewbBackup2Name)
            .description(ewbBackup2Description)
            .enabled(isEwbServerConfigured(ewbBackup2AuthUrl, ewbBackup2EwbUrl, ewbBackup2PrintUrl))
            .build());

        log.debug("Available E-Way Bill servers: {}", servers.size());
        return servers;
    }

    /**
     * Get list of available E-Invoice GSP servers
     * 
     * @return List of server DTOs with availability status
     */
    public List<GspServerDTO> getAvailableEinvServers() {
        List<GspServerDTO> servers = new ArrayList<>();

        // Add primary server
        servers.add(GspServerDTO.builder()
            .id(einvPrimaryId)
            .name(einvPrimaryName)
            .description(einvPrimaryDescription)
            .enabled(isEinvServerConfigured(einvPrimaryAuthUrl, einvPrimaryGenerateUrl))
            .build());

        // Add backup1 server
        servers.add(GspServerDTO.builder()
            .id(einvBackup1Id)
            .name(einvBackup1Name)
            .description(einvBackup1Description)
            .enabled(isEinvServerConfigured(einvBackup1AuthUrl, einvBackup1GenerateUrl))
            .build());

        // Add backup2 server
        servers.add(GspServerDTO.builder()
            .id(einvBackup2Id)
            .name(einvBackup2Name)
            .description(einvBackup2Description)
            .enabled(isEinvServerConfigured(einvBackup2AuthUrl, einvBackup2GenerateUrl))
            .build());

        log.debug("Available E-Invoice servers: {}", servers.size());
        return servers;
    }

    /**
     * Get E-Way Bill server URLs by server ID
     * Returns cached URL map for performance
     * 
     * @param serverId Server ID selected by user
     * @return Map containing auth-url, ewb-url, and print-url
     * @throws IllegalArgumentException if server ID is invalid or not configured
     */
    public Map<String, String> getEwbServerUrls(String serverId) {
        if (serverId == null || serverId.isEmpty()) {
            serverId = ewbPrimaryId; // Default to primary
        }

        Map<String, String> urls = ewbServerUrlsCache.get(serverId);
        
        if (urls == null) {
            throw new IllegalArgumentException("Invalid E-Way Bill server ID: " + serverId);
        }

        // Validate that URLs are configured
        if (!isEwbServerConfigured(urls.get("auth-url"), urls.get("ewb-url"), urls.get("print-url"))) {
            throw new IllegalStateException("E-Way Bill server '" + serverId + "' is not configured");
        }

        return urls;
    }

    /**
     * Get E-Invoice server URLs by server ID
     * Returns cached URL map for performance
     * 
     * @param serverId Server ID selected by user
     * @return Map containing auth-url, generate-url, irn-url, ewb-by-irn-url, cancel-url
     * @throws IllegalArgumentException if server ID is invalid or not configured
     */
    public Map<String, String> getEinvServerUrls(String serverId) {
        if (serverId == null || serverId.isEmpty()) {
            serverId = einvPrimaryId; // Default to primary
        }

        Map<String, String> urls = einvServerUrlsCache.get(serverId);
        
        if (urls == null) {
            throw new IllegalArgumentException("Invalid E-Invoice server ID: " + serverId);
        }

        // Validate that URLs are configured
        if (!isEinvServerConfigured(urls.get("auth-url"), urls.get("generate-url"))) {
            throw new IllegalStateException("E-Invoice server '" + serverId + "' is not configured");
        }

        return urls;
    }

    /**
     * Check if E-Way Bill server is configured
     */
    private boolean isEwbServerConfigured(String authUrl, String ewbUrl, String printUrl) {
        return authUrl != null && !authUrl.isEmpty() &&
               ewbUrl != null && !ewbUrl.isEmpty() &&
               printUrl != null && !printUrl.isEmpty();
    }

    /**
     * Check if E-Invoice server is configured
     */
    private boolean isEinvServerConfigured(String authUrl, String generateUrl) {
        return authUrl != null && !authUrl.isEmpty() &&
               generateUrl != null && !generateUrl.isEmpty();
    }
}
