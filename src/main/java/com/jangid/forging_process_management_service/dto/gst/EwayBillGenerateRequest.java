package com.jangid.forging_process_management_service.dto.gst;

import com.jangid.forging_process_management_service.dto.gst.gsp.EwayBillSessionCredentialsDTO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Wrapper DTO for E-Way Bill generation requests with session credentials
 * Contains both the E-Way Bill data and session authentication information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EwayBillGenerateRequest {
    
    /**
     * Session credentials (username/password for first request, or sessionToken for subsequent requests)
     */
    @Valid
    @NotNull(message = "Session credentials are required")
    private EwayBillSessionCredentialsDTO sessionCredentials;
    
    /**
     * E-Way Bill data for generation
     */
    @Valid
    @NotNull(message = "E-Way Bill data is required")
    private EwayBillData ewayBillData;
}
