package com.jangid.forging_process_management_service.dto.gst.einvoice;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Response DTO for E-Invoice Generation API
 * Maps to response from: POST /eicore/dec/v1.03/Invoice
 * 
 * Response Structure:
 * - Success: {"Status":"1","Data":"<JSON string>","ErrorDetails":null,"InfoDtls":null}
 * - Error: {"Status":"0","Data":null,"ErrorDetails":[...],"InfoDtls":[...]}
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
@JsonIgnoreProperties(ignoreUnknown = true)
public class EInvoiceGenerateResponse {

    /**
     * Status: "1" = Success, "0" = Error
     */
    @JsonProperty("Status")
    private String status;

    /**
     * Data: JSON string containing invoice details (only present on success)
     * This is a JSON string that needs to be parsed to get actual invoice data
     */
    @JsonProperty("Data")
    private String data;

    /**
     * ErrorDetails: Array of error objects (only present on error)
     */
    @JsonProperty("ErrorDetails")
    private List<ErrorDetail> errorDetails;

    /**
     * InfoDtls: Array of info objects (may be present on error, e.g., duplicate IRN)
     */
    @JsonProperty("InfoDtls")
    private List<InfoDetail> infoDtls;

    // Transient parsed data (not from JSON)
    private transient InvoiceData parsedInvoiceData;

    /**
     * Error Detail object
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ErrorDetail {
        @JsonProperty("ErrorCode")
        private String errorCode;

        @JsonProperty("ErrorMessage")
        private String errorMessage;
    }

    /**
     * Info Detail object
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class InfoDetail {
        @JsonProperty("InfCd")
        private String infoCode;

        @JsonProperty("Desc")
        private Object description; // Can be object with AckNo, AckDt, Irn for duplicate IRN case
    }

    /**
     * Invoice Data (parsed from Data JSON string)
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class InvoiceData {
        @JsonProperty("AckNo")
        private String ackNo;

        @JsonProperty("AckDt")
        private String ackDt;

        @JsonProperty("Irn")
        private String irn;

        @JsonProperty("SignedInvoice")
        private String signedInvoice;

        @JsonProperty("SignedQRCode")
        private String signedQRCode;

        @JsonProperty("Status")
        private String status; // ACT, CNL, etc.

        @JsonProperty("EwbNo")
        private String ewbNo;

        @JsonProperty("EwbDt")
        private String ewbDt;

        @JsonProperty("EwbValidTill")
        private String ewbValidTill;

        @JsonProperty("ExtractedSignedInvoiceData")
        private Object extractedSignedInvoiceData;

        @JsonProperty("ExtractedSignedQrCode")
        private Object extractedSignedQrCode;

        @JsonProperty("QrCodeImage")
        private String qrCodeImage;

        @JsonProperty("JwtIssuer")
        private String jwtIssuer;

        /**
         * Check if E-Way Bill was also generated
         */
        public boolean hasEwayBill() {
            return ewbNo != null && !"0".equals(ewbNo) && !ewbNo.isEmpty();
        }

        /**
         * Get E-Way Bill Number as Long
         */
        public Long getEwbNoAsLong() {
            if (ewbNo == null || ewbNo.isEmpty() || "0".equals(ewbNo)) {
                return null;
            }
            try {
                return Long.parseLong(ewbNo);
            } catch (NumberFormatException e) {
                log.warn("Failed to parse EwbNo as Long: {}", ewbNo);
                return null;
            }
        }

        /**
         * Get Acknowledgement Number as Long
         */
        public Long getAckNoAsLong() {
            if (ackNo == null || ackNo.isEmpty()) {
                return null;
            }
            try {
                return Long.parseLong(ackNo);
            } catch (NumberFormatException e) {
                log.warn("Failed to parse AckNo as Long: {}", ackNo);
                return null;
            }
        }
    }

    /**
     * Check if E-Invoice generation was successful
     */
    public boolean isSuccess() {
        return "1".equals(status) && data != null && !data.isEmpty();
    }

    /**
     * Parse the Data JSON string to get invoice details
     */
    public InvoiceData parseInvoiceData() {
        if (parsedInvoiceData != null) {
            return parsedInvoiceData;
        }

        if (!isSuccess()) {
            return null;
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            parsedInvoiceData = mapper.readValue(data, InvoiceData.class);
            return parsedInvoiceData;
        } catch (JsonProcessingException e) {
            log.error("Failed to parse invoice data from Data field", e);
            return null;
        }
    }

    /**
     * Get IRN from parsed data (convenience method)
     */
    public String getIrn() {
        InvoiceData invoiceData = parseInvoiceData();
        return invoiceData != null ? invoiceData.getIrn() : null;
    }

    /**
     * Get Acknowledgement Number from parsed data (convenience method)
     */
    public Long getAckNo() {
        InvoiceData invoiceData = parseInvoiceData();
        return invoiceData != null ? invoiceData.getAckNoAsLong() : null;
    }

    /**
     * Get Acknowledgement Date from parsed data (convenience method)
     */
    public String getAckDt() {
        InvoiceData invoiceData = parseInvoiceData();
        return invoiceData != null ? invoiceData.getAckDt() : null;
    }

    /**
     * Get Signed QR Code from parsed data (convenience method)
     */
    public String getSignedQRCode() {
        InvoiceData invoiceData = parseInvoiceData();
        return invoiceData != null ? invoiceData.getSignedQRCode() : null;
    }

    /**
     * Get E-Way Bill Number from parsed data (convenience method)
     */
    public Long getEwbNo() {
        InvoiceData invoiceData = parseInvoiceData();
        return invoiceData != null ? invoiceData.getEwbNoAsLong() : null;
    }

    /**
     * Get E-Way Bill Date from parsed data (convenience method)
     */
    public String getEwbDt() {
        InvoiceData invoiceData = parseInvoiceData();
        return invoiceData != null ? invoiceData.getEwbDt() : null;
    }

    /**
     * Get E-Way Bill Valid Till from parsed data (convenience method)
     */
    public String getEwbValidTill() {
        InvoiceData invoiceData = parseInvoiceData();
        return invoiceData != null ? invoiceData.getEwbValidTill() : null;
    }

    /**
     * Check if E-Way Bill was also generated (convenience method)
     */
    public boolean hasEwayBill() {
        InvoiceData invoiceData = parseInvoiceData();
        return invoiceData != null && invoiceData.hasEwayBill();
    }

    /**
     * Get error details as formatted string
     */
    public String getErrorDetailsString() {
        if (errorDetails == null || errorDetails.isEmpty()) {
            return "E-Invoice generation failed";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < errorDetails.size(); i++) {
            ErrorDetail error = errorDetails.get(i);
            sb.append(String.format("Error [%s]: %s", error.getErrorCode(), error.getErrorMessage()));
            if (i < errorDetails.size() - 1) {
                sb.append("; ");
            }
        }
        return sb.toString();
    }

    /**
     * Get error details - convenience method for backward compatibility
     */
    public String getErrorDetails() {
        return getErrorDetailsString();
    }

    /**
     * Get the alert message (currently not in API response structure)
     */
    public String getAlert() {
        // This field is not present in the provided API response structure
        // Keeping for backward compatibility, returns null
        return null;
    }
}
