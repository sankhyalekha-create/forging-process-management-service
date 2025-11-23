package com.jangid.forging_process_management_service.dto.gst.gsp;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for GetEwayBill API response
 * Contains complete E-Way Bill details including items and vehicle history
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GspEwbDetailResponse {

    @JsonProperty("ewbNo")
    private Long ewbNo;

    @JsonProperty("ewayBillDate")
    private String ewayBillDate;

    @JsonProperty("genMode")
    private String genMode;

    @JsonProperty("userGstin")
    private String userGstin;

    @JsonProperty("supplyType")
    private String supplyType;

    @JsonProperty("subSupplyType")
    private String subSupplyType;

    @JsonProperty("docType")
    private String docType;

    @JsonProperty("docNo")
    private String docNo;

    @JsonProperty("docDate")
    private String docDate;

    // From (Supplier) Details
    @JsonProperty("fromGstin")
    private String fromGstin;

    @JsonProperty("fromTrdName")
    private String fromTrdName;

    @JsonProperty("fromAddr1")
    private String fromAddr1;

    @JsonProperty("fromAddr2")
    private String fromAddr2;

    @JsonProperty("fromPlace")
    private String fromPlace;

    @JsonProperty("fromPincode")
    private Integer fromPincode;

    @JsonProperty("fromStateCode")
    private Integer fromStateCode;

    // To (Recipient) Details
    @JsonProperty("toGstin")
    private String toGstin;

    @JsonProperty("toTrdName")
    private String toTrdName;

    @JsonProperty("toAddr1")
    private String toAddr1;

    @JsonProperty("toAddr2")
    private String toAddr2;

    @JsonProperty("toPlace")
    private String toPlace;

    @JsonProperty("toPincode")
    private Integer toPincode;

    @JsonProperty("toStateCode")
    private Integer toStateCode;

    // Transaction Values
    @JsonProperty("totalValue")
    private Double totalValue;

    @JsonProperty("totInvValue")
    private Double totInvValue;

    @JsonProperty("cgstValue")
    private Double cgstValue;

    @JsonProperty("sgstValue")
    private Double sgstValue;

    @JsonProperty("igstValue")
    private Double igstValue;

    @JsonProperty("cessValue")
    private Double cessValue;

    @JsonProperty("otherValue")
    private Double otherValue;

    @JsonProperty("cessNonAdvolValue")
    private Double cessNonAdvolValue;

    // Transport Details
    @JsonProperty("transporterId")
    private String transporterId;

    @JsonProperty("transporterName")
    private String transporterName;

    @JsonProperty("transMode")
    private String transMode;

    @JsonProperty("transDocNo")
    private String transDocNo;

    @JsonProperty("transDocDate")
    private String transDocDate;

    // E-Way Bill Status
    @JsonProperty("status")
    private String status;

    @JsonProperty("actualDist")
    private Integer actualDist;

    @JsonProperty("noValidDays")
    private Integer noValidDays;

    @JsonProperty("validUpto")
    private String validUpto;

    @JsonProperty("extendedTimes")
    private Integer extendedTimes;

    @JsonProperty("rejectStatus")
    private String rejectStatus;

    @JsonProperty("vehicleType")
    private String vehicleType;

    @JsonProperty("actFromStateCode")
    private Integer actFromStateCode;

    @JsonProperty("actToStateCode")
    private Integer actToStateCode;

    @JsonProperty("transactionType")
    private Integer transactionType;

    // Item Details
    @JsonProperty("itemList")
    private List<EwbItemDetail> itemList;

    // Vehicle History
    @JsonProperty("VehiclListDetails")
    private List<EwbVehicleDetail> vehicleListDetails;

    /**
     * E-Way Bill Item Details
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EwbItemDetail {
        @JsonProperty("itemNo")
        private Integer itemNo;

        @JsonProperty("productId")
        private Long productId;

        @JsonProperty("productName")
        private String productName;

        @JsonProperty("productDesc")
        private String productDesc;

        @JsonProperty("hsnCode")
        private Long hsnCode;

        @JsonProperty("quantity")
        private Double quantity;

        @JsonProperty("qtyUnit")
        private String qtyUnit;

        @JsonProperty("cgstRate")
        private Double cgstRate;

        @JsonProperty("sgstRate")
        private Double sgstRate;

        @JsonProperty("igstRate")
        private Double igstRate;

        @JsonProperty("cessRate")
        private Double cessRate;

        @JsonProperty("cessNonAdvol")
        private Double cessNonAdvol;

        @JsonProperty("taxableAmount")
        private Double taxableAmount;
    }

    /**
     * E-Way Bill Vehicle Update History
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EwbVehicleDetail {
        @JsonProperty("updMode")
        private String updMode;

        @JsonProperty("vehicleNo")
        private String vehicleNo;

        @JsonProperty("fromPlace")
        private String fromPlace;

        @JsonProperty("fromState")
        private Integer fromState;

        @JsonProperty("tripshtNo")
        private Long tripshtNo;

        @JsonProperty("userGSTINTransin")
        private String userGSTINTransin;

        @JsonProperty("enteredDate")
        private String enteredDate;

        @JsonProperty("transMode")
        private String transMode;

        @JsonProperty("transDocNo")
        private String transDocNo;

        @JsonProperty("transDocDate")
        private String transDocDate;

        @JsonProperty("groupNo")
        private String groupNo;
    }
}
