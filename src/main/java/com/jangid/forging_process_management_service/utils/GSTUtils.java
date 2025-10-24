package com.jangid.forging_process_management_service.utils;

import com.jangid.forging_process_management_service.entities.buyer.BuyerEntity;
import com.jangid.forging_process_management_service.entities.vendor.VendorEntity;

/**
 * Utility class for GST (Goods and Services Tax) related operations.
 * Provides helper methods for GST compliance, state code mapping, and place of supply determination.
 */
public class GSTUtils {

  /**
   * Determines the Place of Supply for GST purposes from the recipient entity.
   * Returns state name based on state code (GST-compliant, within 50 chars).
   * 
   * @param buyerEntity Buyer entity (if recipient is buyer)
   * @param vendorEntity Vendor entity (if recipient is vendor)
   * @return Place of supply (state name or state code)
   */
  public static String getPlaceOfSupply(BuyerEntity buyerEntity, VendorEntity vendorEntity) {
    String stateCode = null;
    
    // Get state code from buyer or vendor entity
    if (buyerEntity != null) {
      stateCode = buyerEntity.getStateCode();
    } else if (vendorEntity != null) {
      stateCode = vendorEntity.getStateCode();
    }
    
    // Map state code to state name (GST state codes)
    String stateName = getStateNameFromCode(stateCode);
    return stateName != null ? stateName : "State Code: " + stateCode;
  }

  /**
   * Maps GST state code to state name.
   * Reference: GST state codes as per Indian GST system.
   * 
   * @param stateCode 2-digit state code
   * @return State name or null if state code is invalid
   */
  public static String getStateNameFromCode(String stateCode) {
    if (stateCode == null || stateCode.trim().isEmpty()) {
      return null;
    }
    
    return switch (stateCode.trim()) {
      case "01" -> "Jammu and Kashmir";
      case "02" -> "Himachal Pradesh";
      case "03" -> "Punjab";
      case "04" -> "Chandigarh";
      case "05" -> "Uttarakhand";
      case "06" -> "Haryana";
      case "07" -> "Delhi";
      case "08" -> "Rajasthan";
      case "09" -> "Uttar Pradesh";
      case "10" -> "Bihar";
      case "11" -> "Sikkim";
      case "12" -> "Arunachal Pradesh";
      case "13" -> "Nagaland";
      case "14" -> "Manipur";
      case "15" -> "Mizoram";
      case "16" -> "Tripura";
      case "17" -> "Meghalaya";
      case "18" -> "Assam";
      case "19" -> "West Bengal";
      case "20" -> "Jharkhand";
      case "21" -> "Odisha";
      case "22" -> "Chhattisgarh";
      case "23" -> "Madhya Pradesh";
      case "24" -> "Gujarat";
      case "25" -> "Daman and Diu";
      case "26" -> "Dadra and Nagar Haveli";
      case "27" -> "Maharashtra";
      case "28" -> "Andhra Pradesh";
      case "29" -> "Karnataka";
      case "30" -> "Goa";
      case "31" -> "Lakshadweep";
      case "32" -> "Kerala";
      case "33" -> "Tamil Nadu";
      case "34" -> "Puducherry";
      case "35" -> "Andaman and Nicobar Islands";
      case "36" -> "Telangana";
      case "37" -> "Andhra Pradesh (New)";
      case "38" -> "Ladakh";
      case "97" -> "Other Territory";
      case "99" -> "Centre Jurisdiction";
      default -> null;
    };
  }

  /**
   * Validates if a given GSTIN (GST Identification Number) format is valid.
   * GSTIN format: 2-digit state code + 10-digit PAN + 1-digit entity number + 1-letter Z + 1-digit checksum
   * Example: 27AABCU9603R1ZM
   * 
   * @param gstin GST Identification Number
   * @return true if format is valid, false otherwise
   */
  public static boolean isValidGSTINFormat(String gstin) {
    if (gstin == null || gstin.length() != 15) {
      return false;
    }
    
    // GSTIN format: 2 digits + 10 alphanumeric (PAN) + 1 digit + Z + 1 alphanumeric
    String gstinPattern = "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$";
    return gstin.matches(gstinPattern);
  }

  /**
   * Extracts state code from GSTIN.
   * The first 2 digits of GSTIN represent the state code.
   * 
   * @param gstin GST Identification Number
   * @return State code (2 digits) or null if invalid
   */
  public static String extractStateCodeFromGSTIN(String gstin) {
    if (gstin == null || gstin.length() < 2) {
      return null;
    }
    
    String stateCode = gstin.substring(0, 2);
    
    // Validate if it's a numeric state code
    try {
      Integer.parseInt(stateCode);
      return stateCode;
    } catch (NumberFormatException e) {
      return null;
    }
  }

  /**
   * Determines if a transaction is inter-state based on state codes.
   * Inter-state: Supplier and recipient are in different states
   * Intra-state: Supplier and recipient are in the same state
   * 
   * @param supplierStateCode Supplier's state code
   * @param recipientStateCode Recipient's state code
   * @return true if inter-state, false if intra-state
   */
  public static boolean isInterStateTransaction(String supplierStateCode, String recipientStateCode) {
    if (supplierStateCode == null || recipientStateCode == null) {
      return false;
    }
    
    return !supplierStateCode.trim().equals(recipientStateCode.trim());
  }

  /**
   * Determines the applicable GST type based on state codes.
   * 
   * @param supplierStateCode Supplier's state code
   * @param recipientStateCode Recipient's state code
   * @return "IGST" for inter-state, "CGST+SGST" for intra-state
   */
  public static String getApplicableGSTType(String supplierStateCode, String recipientStateCode) {
    return isInterStateTransaction(supplierStateCode, recipientStateCode) ? "IGST" : "CGST+SGST";
  }
}

