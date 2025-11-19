package com.jangid.forging_process_management_service.dto.gst;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * E-Way Bill data structure as per NIC format
 * Maps to FOPMAS Invoice or Delivery Challan
 * 
 * JSON structure matches the GST portal upload format
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EwayBillData {
    
    // User GSTIN - Required field
    private String userGstin;
    
    // Document Details
    private String supplyType; // "O" = Outward, "I" = Inward
    private Integer subSupplyType; // 1 = Supply, 2 = Import, 3 = Export, 4 = Job Work, etc.
    private String subSupplyDesc; // Description of sub supply type (optional)
    private String docType; // "INV" = Tax Invoice, "CHL" = Delivery Challan
    private String docNo; // Invoice/Challan number
    private String docDate; // Format: DD/MM/YYYY
    
    // Transaction Details (not needed in bulk JSON format - removed)
    // private String transactionType;
    
    // From (Supplier/Consignor) Details
    private String fromGstin;
    private String fromTrdName;
    private String fromAddr1;
    private String fromAddr2;
    private String fromPlace;
    private Integer fromPincode;
    
    @JsonProperty("actualFromStateCode")
    private Integer actualFromStateCode; // Actual state code (same as fromStateCode for regular cases)
    
    private Integer fromStateCode;
    
    // To (Buyer/Consignee) Details
    private String toGstin;
    private String toTrdName;
    private String toAddr1;
    private String toAddr2;
    private String toPlace;
    private Integer toPincode;
    
    @JsonProperty("actualToStateCode")
    private Integer actualToStateCode;
    
    private Integer toStateCode;
    
    // Financial Details
    private Double totalValue; // Total taxable value (sum of all line items)
    private Double cgstValue; // CGST amount
    private Double sgstValue; // SGST amount
    private Double igstValue; // IGST amount
    private Double cessValue; // Cess amount
    
    @JsonProperty("TotNonAdvolVal")
    private Double totNonAdvolVal; // Non-Advol Cess
    
    @JsonProperty("OthValue")
    private Double othValue; // Other charges
    
    private Double totInvValue; // Total invoice value (totalValue + taxes + other charges)
    
    // Transportation Details
    private String transporterId; // Transporter GSTIN (15 chars)
    private String transporterName; // Transporter name
    private String transDocNo; // Transport document number (for Rail/Air/Ship)
    
    @JsonProperty("transMode")
    private Integer transMode; // 1 = Road, 2 = Rail, 3 = Air, 4 = Ship
    
    private Integer transDistance; // Distance in kilometers
    private String transDocDate; // Transport document date (DD/MM/YYYY)
    private String vehicleNo; // Vehicle registration number (for Road)
    private String vehicleType; // "R" = Regular, "O" = Over Dimensional Cargo
    
    // Main HSN Code (most prominent/first item's HSN)
    private Long mainHsnCode;
    
    // Item Details
    private List<EwayBillItem> itemList;
}
