package com.jangid.forging_process_management_service.dto.gst.einvoice;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Wrapper DTO for E-Way Bill generation by IRN with session credentials
 * Contains both the E-Way Bill generation request and session authentication information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EInvoiceEwbByIrnRequest {
    
    /**
     * Session credentials (username/password for first request, or sessionToken for subsequent requests)
     */
    @Valid
    @NotNull(message = "Session credentials are required")
    private EInvoiceSessionCredentialsDTO sessionCredentials;
    
    /**
     * E-Way Bill generation data (transportation details, etc.)
     */
    @Valid
    @NotNull(message = "E-Way Bill data is required")
    private EInvoiceGenerateEwbByIrnRequest ewbData;
}
