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
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EInvoiceGenerateEwbByIrnRequest {

    /**
     * Invoice Reference Number (IRN)
     */
    @NotNull(message = "IRN is required")
    @JsonProperty("Irn")
    private String irn;

    /**
     * Transportation distance in kilometers
     */
    @NotNull(message = "Distance is required")
    @JsonProperty("Distance")
    private Integer distance;

    /**
     * Transportation Mode: 1-Road, 2-Rail, 3-Air, 4-Ship
     */
    @NotNull(message = "Transportation mode is required")
    @Pattern(regexp = "[1-4]", message = "TransMode must be 1, 2, 3, or 4")
    @JsonProperty("TransMode")
    private String transMode;

    /**
     * Transporter ID (GSTIN)
     */
    @JsonProperty("TransId")
    private String transId;

    /**
     * Transporter Name
     */
    @JsonProperty("TransName")
    private String transName;

    /**
     * Transport Document Date (format: dd/MM/yyyy)
     */
    @JsonProperty("TransDocDt")
    private String transDocDt;

    /**
     * Transport Document Number
     */
    @JsonProperty("TransDocNo")
    private String transDocNo;

    /**
     * Vehicle Number (format: XX00XX0000, without spaces/hyphens)
     */
    @JsonProperty("VehNo")
    private String vehNo;

    /**
     * Vehicle Type: R-Regular, O-Over Dimensional Cargo
     */
    @Pattern(regexp = "[RO]", message = "VehType must be R or O")
    @JsonProperty("VehType")
    private String vehType;

    /**
     * Export/Ship Details
     */
    @JsonProperty("ExpShipDtls")
    private ExpShipDetails expShipDtls;

    /**
     * Dispatch Details
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
