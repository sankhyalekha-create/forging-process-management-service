package com.jangid.forging_process_management_service.dto.gst.einvoice;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Request DTO for generating E-Way Bill from existing E-Invoice (by IRN)
 * Used for API: POST /eiewb/dec/v1.03/ewaybill
 * 
 * GSP API Specification:
 * - IRN: Invoice Reference Number (required)
 * - Distance: Distance in km between source and destination (required)
 * - TransMode: Mode of transport - 1:Road, 2:Rail, 3:Air, 4:Ship (required)
 * - TransId: Transporter ID (GSTIN) - required if TransMode is 1,2,3,4 and distance > 50km
 * - TransName: Transporter Name
 * - TransDocDt: Transport Document Date (format: dd/MM/yyyy)
 * - TransDocNo: Transport Document Number (pattern: ^([0-9A-Z/-]){1,15}$)
 * - VehNo: Vehicle Number (required if TransMode is 1 Road)
 * - VehType: Vehicle Type - R:Regular, O:ODC (Over Dimensional Cargo)
 * - ExpShipDtls: Export/Ship Details (for export invoices)
 * - DispDtls: Dispatch Details (optional dispatch from address)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EInvoiceGenerateEwbByIrnRequest {

    /**
     * Invoice Reference Number (IRN) - Required
     */
    @NotNull(message = "IRN is required")
    @JsonProperty("Irn")
    private String irn;

    /**
     * Transportation distance in kilometers - Required
     * Distance between source and destination PIN codes
     */
    @NotNull(message = "Distance is required")
    @JsonProperty("Distance")
    private Integer distance;

    /**
     * Transportation Mode - Required
     * 1 - Road
     * 2 - Rail
     * 3 - Air
     * 4 - Ship
     */
    @NotNull(message = "Transportation mode is required")
    @Pattern(regexp = "[1-4]", message = "TransMode must be 1 (Road), 2 (Rail), 3 (Air), or 4 (Ship)")
    @JsonProperty("TransMode")
    private String transMode;

    /**
     * Transporter ID (GSTIN) - Optional
     * Required if TransMode is 1,2,3,4 and distance > 50km
     * Format: 15 characters GSTIN
     */
    @JsonProperty("TransId")
    private String transId;

    /**
     * Transporter Name - Optional
     */
    @JsonProperty("TransName")
    private String transName;

    /**
     * Transport Document Date - Optional
     * Format: dd/MM/yyyy (e.g., "07/10/2022")
     * Pattern: [0-3][0-9]/[0-1][0-9]/[2][0][1-2][0-9]
     */
    @JsonProperty("TransDocDt")
    private String transDocDt;

    /**
     * Transport Document Number - Optional
     * Pattern: ^([0-9A-Z/-]){1,15}$
     * Max length: 15 characters
     */
    @Pattern(regexp = "^([0-9A-Z/-]){1,15}$", message = "TransDocNo must be 1-15 alphanumeric characters with / or -")
    @JsonProperty("TransDocNo")
    private String transDocNo;

    /**
     * Vehicle Number - Optional but required if TransMode is 1 (Road)
     * Format: Vehicle registration number without spaces or hyphens
     * Example: KA12ER1234
     */
    @JsonProperty("VehNo")
    private String vehNo;

    /**
     * Vehicle Type - Optional
     * R - Regular
     * O - Over Dimensional Cargo (ODC)
     */
    @Pattern(regexp = "[RO]", message = "VehType must be R (Regular) or O (ODC)")
    @JsonProperty("VehType")
    private String vehType;

    /**
     * Export/Ship Details - Optional
     * Required for export invoices with shipping details
     */
    @JsonProperty("ExpShipDtls")
    private ExpShipDetails expShipDtls;

    /**
     * Dispatch Details - Optional
     * Dispatch from address details (if different from supplier address)
     */
    @JsonProperty("DispDtls")
    private DispatchDetails dispDtls;

    /**
     * Export/Ship Details nested class
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExpShipDetails {
        @JsonProperty("Addr1")
        private String addr1;

        @JsonProperty("Addr2")
        private String addr2;

        @JsonProperty("Loc")
        private String loc;

        @JsonProperty("Pin")
        private Integer pin;

        @JsonProperty("Stcd")
        private String stcd;
    }

    /**
     * Dispatch Details nested class
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DispatchDetails {
        @JsonProperty("Nm")
        private String nm;

        @JsonProperty("Addr1")
        private String addr1;

        @JsonProperty("Addr2")
        private String addr2;

        @JsonProperty("Loc")
        private String loc;

        @JsonProperty("Pin")
        private Integer pin;

        @JsonProperty("Stcd")
        private String stcd;
    }
}
