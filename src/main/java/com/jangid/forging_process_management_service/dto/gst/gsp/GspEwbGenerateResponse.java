package com.jangid.forging_process_management_service.dto.gst.gsp;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for E-Way Bill Generation
 * Based on GSP v1.03 API response format
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GspEwbGenerateResponse {

    @JsonProperty("ewayBillNo")
    private Long ewayBillNo;

    @JsonProperty("ewayBillDate")
    private String ewayBillDate; // DD/MM/YYYY HH:MM AM/PM format

    @JsonProperty("validUpto")
    private String validUpto; // DD/MM/YYYY HH:MM AM/PM format

    @JsonProperty("alert")
    private String alert;

    @JsonProperty("error")
    private String error; // Error message if any

    public boolean isSuccess() {
        return ewayBillNo != null && ewayBillNo > 0;
    }
    
    public String getErrorMessage() {
        return error;
    }

    // For backward compatibility
    @Deprecated
    public String getEwbDt() {
        return ewayBillDate;
    }
}
