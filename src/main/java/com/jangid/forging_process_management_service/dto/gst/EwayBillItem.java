package com.jangid.forging_process_management_service.dto.gst;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * E-Way Bill item/line item details as per NIC format
 * Note: Tax rates should be -1 if not applicable (e.g., sgstRate=-1 for inter-state transactions)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EwayBillItem {
    
    private Integer itemNo; // Serial number (1, 2, 3...)
    private String productName; // Product name (max 100 chars)
    private String productDesc; // Product description (max 300 chars)
    private Long hsnCode; // HSN/SAC code (4/6/8 digits)
    private Double quantity; // Quantity
    private String qtyUnit; // Unit quantity code (PCS, KGS, MTR, etc.)
    private Double taxableAmount; // Taxable value before tax
    
    // Tax rates: Use actual rate if applicable, -1 if not applicable
    // For inter-state: sgstRate=-1, cgstRate=-1, igstRate=actual
    // For intra-state: igstRate=-1, sgstRate=actual, cgstRate=actual
    private Integer sgstRate; // SGST rate (e.g., 9 for 9%) or -1 if not applicable
    private Integer cgstRate; // CGST rate or -1 if not applicable
    private Integer igstRate; // IGST rate or -1 if not applicable
    private Integer cessRate; // Cess rate or -1 if not applicable
    private Integer cessNonAdvol; // Non-Advol Cess or -1 if not applicable
}
