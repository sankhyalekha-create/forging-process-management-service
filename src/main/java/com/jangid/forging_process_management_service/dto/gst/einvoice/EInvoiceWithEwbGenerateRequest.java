package com.jangid.forging_process_management_service.dto.gst.einvoice;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Wrapper DTO for combined E-Invoice and E-Way Bill generation request
 * This request will:
 * 1. Generate E-Invoice via GSP API
 * 2. Generate E-Way Bill by IRN using the generated IRN
 * 3. Generate E-Invoice PDF (3 pages)
 * 4. Generate E-Way Bill PDF (1 page)
 * 5. Merge both PDFs and return as base64 string
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EInvoiceWithEwbGenerateRequest {
    
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
    
    /**
     * E-Way Bill generation data (transportation details, distance, vehicle info, etc.)
     * IRN will be automatically set after E-Invoice generation
     */
    @Valid
    @NotNull(message = "E-Way Bill data is required")
    private EInvoiceGenerateEwbByIrnRequest ewbData;
}
