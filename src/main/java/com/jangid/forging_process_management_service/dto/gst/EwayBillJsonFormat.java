package com.jangid.forging_process_management_service.dto.gst;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Root DTO for E-Way Bill JSON export in NIC format
 * Used for offline E-Way Bill generation - user downloads this JSON and uploads to ewaybillgst.gov.in
 * 
 * JSON structure:
 * {
 *   "version": "1.0.0918",
 *   "billLists": [
 *     { ... e-way bill data ... }
 *   ]
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EwayBillJsonFormat {
    
    /**
     * Version of the E-Way Bill JSON format
     * As per NIC specification
     */
    @JsonProperty("version")
    @Builder.Default
    private String version = "1.0.0918";
    
    /**
     * List of E-Way Bill data (typically one, but can be multiple for bulk generation)
     * JSON property name must be "billLists" as per NIC specification
     */
    @JsonProperty("billLists")
    private List<EwayBillData> billLists;
}
