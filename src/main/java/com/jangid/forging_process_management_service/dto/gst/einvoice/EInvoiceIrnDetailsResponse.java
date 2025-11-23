package com.jangid.forging_process_management_service.dto.gst.einvoice;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for Get IRN Details API
 * Maps to response from: GET /eicore/dec/v1.03/Invoice/irn/{irn}
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class EInvoiceIrnDetailsResponse {

    /**
     * Invoice Reference Number (IRN)
     */
    @JsonProperty("Irn")
    private String irn;

    /**
     * Acknowledgement Number
     */
    @JsonProperty("AckNo")
    private Long ackNo;

    /**
     * Acknowledgement Date
     */
    @JsonProperty("AckDt")
    private String ackDt;

    /**
     * Document Number
     */
    @JsonProperty("DocNo")
    private String docNo;

    /**
     * Document Date
     */
    @JsonProperty("DocDt")
    private String docDt;

    /**
     * Document Type
     */
    @JsonProperty("DocTyp")
    private String docTyp;

    /**
     * Seller GSTIN
     */
    @JsonProperty("SellerGstin")
    private String sellerGstin;

    /**
     * Buyer GSTIN
     */
    @JsonProperty("BuyerGstin")
    private String buyerGstin;

    /**
     * Total Invoice Value
     */
    @JsonProperty("TotInvVal")
    private Double totInvVal;

    /**
     * Signed Invoice
     */
    @JsonProperty("SignedInvoice")
    private String signedInvoice;

    /**
     * Signed QR Code
     */
    @JsonProperty("SignedQRCode")
    private String signedQRCode;

    /**
     * Status (ACT, CNL, etc.)
     */
    @JsonProperty("Status")
    private String status;

    /**
     * E-Way Bill Number (if generated)
     */
    @JsonProperty("EwbNo")
    private Long ewbNo;

    /**
     * E-Way Bill Date
     */
    @JsonProperty("EwbDt")
    private String ewbDt;

    /**
     * E-Way Bill Valid Until
     */
    @JsonProperty("EwbValidTill")
    private String ewbValidTill;

    /**
     * Cancelled Date (if cancelled)
     */
    @JsonProperty("CancelDate")
    private String cancelDate;

    /**
     * Error message if retrieval fails
     */
    @JsonProperty("ErrorMessage")
    private String errorMessage;

    /**
     * Error code if retrieval fails
     */
    @JsonProperty("ErrorCode")
    private String errorCode;

    /**
     * Check if IRN is active
     */
    public boolean isActive() {
        return "ACT".equalsIgnoreCase(status);
    }

    /**
     * Check if IRN is cancelled
     */
    public boolean isCancelled() {
        return "CNL".equalsIgnoreCase(status);
    }

    /**
     * Check if E-Way Bill exists
     */
    public boolean hasEwayBill() {
        return ewbNo != null && ewbNo > 0;
    }

    /**
     * Check if retrieval was successful
     */
    public boolean isSuccess() {
        return irn != null && !irn.isEmpty() && errorMessage == null;
    }

    /**
     * Get error details
     */
    public String getErrorDetails() {
        if (errorMessage != null) {
            return String.format("Error [%s]: %s", errorCode, errorMessage);
        }
        return "Failed to retrieve IRN details";
    }
}
