package com.jangid.forging_process_management_service.dto.gst.einvoice;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Wrapper DTO for E-Invoice generation requests with session credentials
 * Contains both the E-Invoice data and session authentication information
 * Similar to EwayBillGenerateRequest
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EInvoiceGenerateRequest {
    
    /**
     * Session credentials (username/password for first request, or sessionToken for subsequent requests)
     */
    @Valid
    @NotNull(message = "Session credentials are required")
    private EInvoiceSessionCredentialsDTO sessionCredentials;
    
    /**
     * E-Invoice data for generation (NIC schema format)
     */
    @Valid
    @NotNull(message = "E-Invoice data is required")
    private Object einvoiceData;
}
