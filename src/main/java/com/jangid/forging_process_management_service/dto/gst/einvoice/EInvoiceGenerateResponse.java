package com.jangid.forging_process_management_service.dto.gst.einvoice;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for E-Invoice Generation API
 * Maps to response from: POST /eicore/dec/v1.03/Invoice
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class EInvoiceGenerateResponse {

    /**
     * Success status
     */
    @JsonProperty("Success")
    private String success;

    /**
     * Invoice Reference Number (IRN) - 64 character unique identifier
     */
    @JsonProperty("Irn")
    private String irn;

    /**
     * Acknowledgement Number
     */
    @JsonProperty("AckNo")
    private Long ackNo;

    /**
     * Acknowledgement Date (format: dd/MM/yyyy HH:mm:ss)
     */
    @JsonProperty("AckDt")
    private String ackDt;

    /**
     * Signed Invoice - JSON string
     */
    @JsonProperty("SignedInvoice")
    private String signedInvoice;

    /**
     * Signed QR Code - Base64 encoded
     */
    @JsonProperty("SignedQRCode")
    private String signedQRCode;

    /**
     * Status of the invoice
     */
    @JsonProperty("Status")
    private String status;

    /**
     * E-Way Bill Number (if generated along with E-Invoice)
     */
    @JsonProperty("EwbNo")
    private Long ewbNo;

    /**
     * E-Way Bill Date
     */
    @JsonProperty("EwbDt")
    private String ewbDt;

    /**
     * E-Way Bill Valid Until Date
     */
    @JsonProperty("EwbValidTill")
    private String ewbValidTill;

    /**
     * Alert message
     */
    @JsonProperty("alert")
    private String alert;

    /**
     * Error message if generation fails
     */
    @JsonProperty("ErrorMessage")
    private String errorMessage;

    /**
     * Error code if generation fails
     */
    @JsonProperty("ErrorCode")
    private String errorCode;

    /**
     * Info messages
     */
    @JsonProperty("InfoDtls")
    private String infoDtls;

    /**
     * Check if E-Invoice generation was successful
     */
    public boolean isSuccess() {
        return "Y".equalsIgnoreCase(success) && irn != null && !irn.isEmpty();
    }

    /**
     * Check if E-Way Bill was also generated
     */
    public boolean hasEwayBill() {
        return ewbNo != null && ewbNo > 0;
    }

    /**
     * Get error details for failed generation
     */
    public String getErrorDetails() {
        if (errorMessage != null) {
            return String.format("Error [%s]: %s", errorCode, errorMessage);
        }
        return "E-Invoice generation failed";
    }
}
