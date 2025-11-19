package com.jangid.forging_process_management_service.dto.gst;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Request DTO for updating E-Way Bill details after manual generation on GST portal
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EwayBillUpdateRequest {
    
    @NotNull(message = "E-Way Bill number is required")
    @Pattern(regexp = "^[0-9]{12}$", message = "E-Way Bill number must be exactly 12 digits")
    private String ewayBillNumber;
    
    @NotNull(message = "E-Way Bill date is required")
    private LocalDateTime ewayBillDate;
    
    @NotNull(message = "E-Way Bill validity date is required")
    private LocalDateTime ewayBillValidUntil;
}
