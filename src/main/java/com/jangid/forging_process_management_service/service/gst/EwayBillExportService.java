package com.jangid.forging_process_management_service.service.gst;

import com.jangid.forging_process_management_service.dto.gst.EwayBillData;
import com.jangid.forging_process_management_service.dto.gst.EwayBillItem;
import com.jangid.forging_process_management_service.dto.gst.EwayBillJsonFormat;
import com.jangid.forging_process_management_service.entities.Tenant;
import com.jangid.forging_process_management_service.entities.buyer.Buyer;
import com.jangid.forging_process_management_service.entities.buyer.BuyerEntity;
import com.jangid.forging_process_management_service.entities.gst.ChallanLineItem;
import com.jangid.forging_process_management_service.entities.gst.ChallanStatus;
import com.jangid.forging_process_management_service.entities.gst.ChallanVendorLineItem;
import com.jangid.forging_process_management_service.entities.gst.DeliveryChallan;
import com.jangid.forging_process_management_service.entities.gst.Invoice;
import com.jangid.forging_process_management_service.entities.gst.InvoiceLineItem;
import com.jangid.forging_process_management_service.entities.gst.InvoiceStatus;
import com.jangid.forging_process_management_service.entities.gst.TransportationMode;
import com.jangid.forging_process_management_service.entities.vendor.Vendor;
import com.jangid.forging_process_management_service.entities.vendor.VendorEntity;
import com.jangid.forging_process_management_service.repositories.gst.DeliveryChallanRepository;
import com.jangid.forging_process_management_service.repositories.gst.InvoiceRepository;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for generating E-Way Bill JSON in NIC format for offline/manual upload
 * This service provides read-only operations with no performance impact
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EwayBillExportService {

  private final InvoiceRepository invoiceRepository;
  private final DeliveryChallanRepository challanRepository;

  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

  /**
   * Generate E-Way Bill JSON for Invoice
   * Read-only operation - no database writes
   */
  @Transactional(readOnly = true)
  public EwayBillJsonFormat generateEwayBillJsonForInvoice(Long tenantId, Long invoiceId) {
    log.info("Generating E-Way Bill JSON for invoice: {}, tenant: {}", invoiceId, tenantId);

    Invoice invoice = invoiceRepository.findByIdAndTenantIdAndDeletedFalse(invoiceId, tenantId)
        .orElseThrow(() -> new IllegalArgumentException("Invoice not found with id: " + invoiceId));

    if (invoice.getTenant().getId() != tenantId) {
      throw new IllegalArgumentException("Invoice does not belong to the specified tenant");
    }

    // Validate invoice status
    if (invoice.getStatus() != InvoiceStatus.GENERATED && invoice.getStatus() != InvoiceStatus.SENT) {
      throw new IllegalArgumentException(
          "E-Way Bill can only be generated for GENERATED or SENT invoices. Current status: " + invoice.getStatus()
      );
    }

    // Map invoice to E-Way Bill format
    EwayBillData billData = mapInvoiceToEwayBill(invoice);

    log.info("Successfully generated E-Way Bill JSON for invoice: {}", invoice.getInvoiceNumber());

    return EwayBillJsonFormat.builder()
        .billLists(List.of(billData))
        .build();
  }

  /**
   * Map Invoice entity to E-Way Bill data structure
   */
  private EwayBillData mapInvoiceToEwayBill(Invoice invoice) {
    Tenant tenant = invoice.getTenant();
    Buyer buyer = invoice.getBuyer();
    
    // Determine transaction type based on billing and shipping entities
    String transactionType = determineTransactionType(invoice);
    
    // Get billing and shipping addresses
    BuyerEntity billingEntity = invoice.getBuyerBillingEntity();
    BuyerEntity shippingEntity = invoice.getBuyerShippingEntity();
    
    // Fallback logic for addresses and GSTIN
    String billingAddress;
    String billingPincode;
    String billingCity;
    String billingGstin;
    String shippingAddress;
    String shippingPincode;
    String shippingCity;
    
    if (buyer == null) {
      log.warn("Buyer details not found in invoice, using vendor details");
      Vendor vendor = invoice.getVendor();
      billingAddress = invoice.getVendorBillingEntity().getAddress();
      billingPincode = invoice.getVendorBillingEntity().getPincode();
      billingCity = invoice.getVendorBillingEntity().getCity();
      billingGstin = invoice.getVendorBillingEntity().getGstinUin();
      shippingAddress = invoice.getVendorShippingEntity().getAddress();
      shippingPincode = invoice.getVendorShippingEntity().getPincode();
      shippingCity = invoice.getVendorShippingEntity().getCity();
    } else {
      // Use billing entity if available, otherwise use buyer's main address
      if (billingEntity != null) {
        billingAddress = billingEntity.getAddress();
        billingPincode = billingEntity.getPincode();
        billingCity = billingEntity.getCity();
        billingGstin = billingEntity.getGstinUin();
      } else {
        billingAddress = buyer.getAddress();
        billingPincode = buyer.getPincode();
        billingCity = buyer.getCity();
        billingGstin = buyer.getGstinUin();
      }
      
      // Use shipping entity if available, otherwise use billing address
      if (shippingEntity != null) {
        shippingAddress = shippingEntity.getAddress();
        shippingPincode = shippingEntity.getPincode();
        shippingCity = shippingEntity.getCity();
      } else {
        shippingAddress = billingAddress;
        shippingPincode = billingPincode;
        shippingCity = billingCity;
      }
    }

    // Parse addresses
    AddressComponents fromAddress = parseAddress(tenant.getAddress(), tenant.getPincode(), tenant.getCity());
    AddressComponents billingAddressComponents = parseAddress(billingAddress, billingPincode, billingCity);
    AddressComponents shippingAddressComponents = parseAddress(shippingAddress, shippingPincode, shippingCity);

    // Extract state codes from GSTIN
    Integer fromStateCode = extractStateCodeFromGstin(tenant.getGstin());
    Integer billingStateCode = extractStateCodeFromGstin(billingGstin);
    Integer shippingStateCode = extractStateCodeFromGstin(
        shippingEntity != null && shippingEntity.getGstinUin() != null ? 
        shippingEntity.getGstinUin() : billingGstin
    );

    // Map line items
    List<EwayBillItem> items = mapInvoiceLineItems(invoice.getLineItems(), invoice.getIsInterState());
    
    // Get main HSN code from first item
    Long mainHsnCode = items.isEmpty() ? 0L : items.get(0).getHsnCode();

    return EwayBillData.builder()
        // User GSTIN (same as fromGstin for supplier)
        .userGstin(tenant.getGstin())
        
        .supplyType("O") // Outward supply
        .subSupplyType(1) // 1 = Supply (as integer, not string)
        .subSupplyDesc(null) // Optional description
        .docType("INV")
        .docNo(truncate(invoice.getInvoiceNumber(), 16))
        .docDate(formatDate(invoice.getInvoiceDate()))
        
        // Transaction type - Required by portal
        .transactionType(transactionType)

        // From (Supplier)
        .fromGstin(tenant.getGstin())
        .fromTrdName(truncate(tenant.getTenantName(), 100))
        .fromAddr1(truncate(fromAddress.getAddr1(), 100))
        .fromAddr2(truncate(fromAddress.getAddr2(), 100))
        .fromPlace(truncate(fromAddress.getCity(), 50))
        .fromPincode(fromAddress.getPincode())
        .actFromStateCode(fromStateCode)
        .fromStateCode(fromStateCode)

        // To (Consignee - Ship To Address)
        // For Bill To-Ship To: to* fields contain shipping address, toStateCode = billing state
        .toGstin(billingGstin)
        .toTrdName(truncate(buyer != null ? buyer.getBuyerName() : invoice.getVendor().getVendorName(), 100))
        .toAddr1(truncate(shippingAddressComponents.getAddr1(), 100))
        .toAddr2(truncate(shippingAddressComponents.getAddr2(), 100))
        .toPlace(truncate(shippingAddressComponents.getCity(), 50))
        .toPincode(shippingAddressComponents.getPincode())
        .actToStateCode(shippingStateCode) // Actual delivery state (Ship To)
        .toStateCode(billingStateCode) // Billing state (Bill To)

        // Financial details
        .totalValue(invoice.getTotalTaxableValue().doubleValue())
        .cgstValue(invoice.getTotalCgstAmount().doubleValue())
        .sgstValue(invoice.getTotalSgstAmount().doubleValue())
        .igstValue(invoice.getTotalIgstAmount().doubleValue())
        .cessValue(0.0)
        .cessNonAdvolValue(0.0)
        .otherValue(0.0)
        .totInvValue(invoice.getTotalInvoiceValue().doubleValue())

        // Transportation
        .transporterId(invoice.getTransporterId() != null ? invoice.getTransporterId() : "")
        .transporterName(invoice.getTransporterName() != null ? truncate(invoice.getTransporterName(), 100) : "")
        .transDocNo(invoice.getTransportDocumentNumber() != null && !invoice.getTransportDocumentNumber().trim().isEmpty() ?
                    truncate(invoice.getTransportDocumentNumber(), 15) : "0")
        .transMode(mapTransportModeToInt(invoice.getTransportationMode()))
        .transDistance(0)
        .transDocDate(invoice.getTransportDocumentDate() != null ?
                      formatDate(invoice.getTransportDocumentDate()) : formatDate(invoice.getInvoiceDate()))
        .vehicleNo(invoice.getVehicleNumber() != null ? invoice.getVehicleNumber() : "")
        .vehicleType("R") // Regular
        
        // Main HSN Code
        .mainHsnCode(mainHsnCode)

        // Items
        .itemList(items)
        .build();
  }

  /**
   * Determine transaction type based on billing and shipping addresses
   * 
   * For GST E-Way Bill:
   * - Transaction Type "1" (Regular): Billing and shipping locations are the same
   * - Transaction Type "2" (Bill To-Ship To): Goods are shipped to a location different from billing address
   *   In this case:
   *   - to* fields (toAddr1, toAddr2, toPlace, etc.) contain SHIPPING address (actual delivery location)
   *   - toStateCode contains the BILLING state code
   *   - actToStateCode contains the SHIPPING state code (actual delivery state)
   *   - toGstin contains the billing party's GSTIN
   * 
   * @param invoice The invoice to analyze
   * @return Transaction type code:
   *         "1" = Regular (Bill To = Ship To)
   *         "2" = Bill To-Ship To (Billing and Shipping addresses are different)
   */
  private String determineTransactionType(Invoice invoice) {
    BuyerEntity billingEntity = invoice.getBuyerBillingEntity();
    BuyerEntity shippingEntity = invoice.getBuyerShippingEntity();
    
    // If no separate entities are specified, it's a regular transaction
    if (billingEntity == null || shippingEntity == null) {
      log.debug("No separate billing/shipping entities - Transaction Type: Regular (1)");
      return "1";
    }
    
    // If billing and shipping entities are the same, it's regular
    if (billingEntity.getId().equals(shippingEntity.getId())) {
      log.debug("Billing and shipping entities are same - Transaction Type: Regular (1)");
      return "1";
    }
    
    // If billing and shipping entities are different, it's Bill To-Ship To
    log.debug("Billing entity: {}, Shipping entity: {} - Transaction Type: Bill To-Ship To (2)", 
              billingEntity.getId(), shippingEntity.getId());
    return "2";
  }

  /**
   * Map invoice line items to E-Way Bill items
   * @param lineItems Invoice line items
   * @param isInterState Whether the transaction is inter-state
   */
  private List<EwayBillItem> mapInvoiceLineItems(List<InvoiceLineItem> lineItems, Boolean isInterState) {
    List<EwayBillItem> ewbItems = new ArrayList<>();
    int itemNo = 1;

    for (InvoiceLineItem lineItem : lineItems) {
      // For inter-state: IGST applicable, CGST/SGST = -1
      // For intra-state: CGST/SGST applicable, IGST = -1
      Integer cgstRate = (isInterState != null && isInterState) ? -1 : lineItem.getCgstRate().intValue();
      Integer sgstRate = (isInterState != null && isInterState) ? -1 : lineItem.getSgstRate().intValue();
      Integer igstRate = (isInterState != null && isInterState) ? lineItem.getIgstRate().intValue() : -1;
      
      ewbItems.add(EwayBillItem.builder()
                       .itemNo(itemNo++)
                       .productName(truncate(lineItem.getItemName(), 100))
                       .productDesc(truncate(lineItem.getItemName(), 300))
                       .hsnCode(parseHsnCode(lineItem.getHsnCode()))
                       .quantity(lineItem.getQuantity().doubleValue())
                       .qtyUnit(mapUnitToUqc(lineItem.getUnitOfMeasurement()))
                       .taxableAmount(lineItem.getTaxableValue().doubleValue())
                       .sgstRate(sgstRate)
                       .cgstRate(cgstRate)
                       .igstRate(igstRate)
                       .cessRate(-1) // -1 if not applicable
                       .cessNonAdvol(-1) // -1 if not applicable
                       .build());
    }

    return ewbItems;
  }

  /**
   * Extract state code from GSTIN (first 2 digits)
   */
  private Integer extractStateCodeFromGstin(String gstin) {
    if (gstin == null || gstin.length() < 2) {
      log.warn("Invalid GSTIN format: {}", gstin);
      return 0;
    }
    try {
      return Integer.parseInt(gstin.substring(0, 2));
    } catch (NumberFormatException e) {
      log.error("Failed to parse state code from GSTIN: {}", gstin, e);
      return 0;
    }
  }

  /**
   * Format LocalDateTime to DD/MM/YYYY for E-Way Bill
   */
  private String formatDate(LocalDateTime dateTime) {
    if (dateTime == null) {
      return LocalDateTime.now().format(DATE_FORMATTER);
    }
    return dateTime.format(DATE_FORMATTER);
  }

  /**
   * Format LocalDate to DD/MM/YYYY for E-Way Bill
   */
  private String formatDate(java.time.LocalDate date) {
    if (date == null) {
      return LocalDateTime.now().format(DATE_FORMATTER);
    }
    return date.format(DATE_FORMATTER);
  }

  /**
   * Parse and clean HSN/SAC code (remove non-numeric characters)
   */
  private Long parseHsnCode(String hsnSacCode) {
    if (hsnSacCode == null || hsnSacCode.trim().isEmpty()) {
      log.warn("HSN/SAC code is empty, using default 0");
      return 0L;
    }
    try {
      String cleanedHsn = hsnSacCode.replaceAll("[^0-9]", "");
      return cleanedHsn.isEmpty() ? 0L : Long.parseLong(cleanedHsn);
    } catch (NumberFormatException e) {
      log.error("Failed to parse HSN code: {}", hsnSacCode, e);
      return 0L;
    }
  }

  /**
   * Map TransportationMode enum to E-Way Bill trans mode code (Integer)
   */
  private Integer mapTransportModeToInt(TransportationMode mode) {
    if (mode == null) {
      return 1; // Default: Road
    }
    switch (mode) {
      case ROAD:
        return 1;
      case RAIL:
        return 2;
      case AIR:
        return 3;
      case SHIP:
        return 4;
      default:
        return 1;
    }
  }

  /**
   * Map unit of measurement to GST Unit Quantity Code (UQC)
   */
  private String mapUnitToUqc(String unit) {
    if (unit == null || unit.trim().isEmpty()) {
      return "OTH"; // Others
    }

    String upperUnit = unit.toUpperCase().trim();

    // Common mappings
    if (upperUnit.contains("PCS") || upperUnit.contains("PIECE") || upperUnit.contains("NOS") || upperUnit.contains("NUMBER")) {
      return "PCS";
    } else if (upperUnit.contains("KG") || upperUnit.contains("KILOGRAM")) {
      return "KGS";
    } else if (upperUnit.contains("MTR") || upperUnit.contains("METER") || upperUnit.contains("METRE")) {
      return "MTR";
    } else if (upperUnit.contains("TON") || upperUnit.contains("TONNE")) {
      return "TON";
    } else if (upperUnit.contains("LTR") || upperUnit.contains("LITRE") || upperUnit.contains("LITER")) {
      return "LTR";
    } else {
      return "OTH";
    }
  }

  /**
   * Truncate string to max length
   */
  private String truncate(String str, int maxLength) {
    if (str == null) {
      return "";
    }
    return str.length() > maxLength ? str.substring(0, maxLength) : str;
  }

  /**
   * Parse address into components
   * Simple implementation - can be enhanced based on address format
   */
  private AddressComponents parseAddress(String address, String pincode, String city) {
    if (address == null) {
      address = "";
    }

    // Split by newline or comma
    String[] lines = address.split("[\\n,]+");

    String addr1 = lines.length > 0 ? lines[0].trim() : "";
    String addr2 = lines.length > 1 ? lines[1].trim() : "";
    String cityName = (city != null && !city.trim().isEmpty()) ? city :
                      (lines.length > 2 ? lines[2].trim() : "Unknown");

    return AddressComponents.builder()
        .addr1(addr1)
        .addr2(addr2)
        .city(cityName)
        .pincode(parsePincode(pincode))
        .build();
  }

  /**
   * Parse pincode to integer
   */
  private Integer parsePincode(String pincode) {
    if (pincode == null || pincode.trim().isEmpty()) {
      return 0;
    }
    try {
      return Integer.parseInt(pincode.replaceAll("[^0-9]", ""));
    } catch (NumberFormatException e) {
      log.error("Failed to parse pincode: {}", pincode, e);
      return 0;
    }
  }

  /**
   * Generate E-Way Bill JSON for Delivery Challan
   * Read-only operation - no database writes
   */
  @Transactional(readOnly = true)
  public EwayBillJsonFormat generateEwayBillJsonForChallan(Long tenantId, Long challanId) {
    log.info("Generating E-Way Bill JSON for challan: {}, tenant: {}", challanId, tenantId);

    DeliveryChallan challan = challanRepository.findByIdAndTenantIdAndDeletedFalse(challanId, tenantId)
        .orElseThrow(() -> new IllegalArgumentException("Delivery Challan not found with id: " + challanId));

    if (challan.getTenant().getId() != tenantId) {
      throw new IllegalArgumentException("Delivery Challan does not belong to the specified tenant");
    }

    // Validate challan status
    if (challan.getStatus() != ChallanStatus.GENERATED && challan.getStatus() != ChallanStatus.DISPATCHED) {
      throw new IllegalArgumentException(
          "E-Way Bill can only be generated for GENERATED or DISPATCHED challans. Current status: " + challan.getStatus()
      );
    }

    // Map challan to E-Way Bill format
    EwayBillData billData = mapChallanToEwayBill(challan);

    log.info("Successfully generated E-Way Bill JSON for challan: {}", challan.getChallanNumber());

    return EwayBillJsonFormat.builder()
        .billLists(List.of(billData))
        .build();
  }

  /**
   * Map Delivery Challan entity to E-Way Bill data structure
   */
  private EwayBillData mapChallanToEwayBill(DeliveryChallan challan) {
    Tenant tenant = challan.getTenant();
    
    // Determine transaction type based on billing and shipping entities
    String transactionType = determineChallanTransactionType(challan);
    
    // Get consignee details (buyer or vendor)
    String consigneeName;
    String billingAddress;
    String billingPincode;
    String billingCity;
    String billingGstin;
    String shippingAddress;
    String shippingPincode;
    String shippingCity;
    Integer billingStateCode;
    Integer shippingStateCode;
    
    if (challan.getBuyer() != null) {
      // Buyer challan
      Buyer buyer = challan.getBuyer();
      consigneeName = buyer.getBuyerName();
      
      BuyerEntity billingEntity = challan.getBuyerBillingEntity();
      BuyerEntity shippingEntity = challan.getBuyerShippingEntity();
      
      // Use billing entity if available, otherwise use buyer's main address
      if (billingEntity != null) {
        billingAddress = billingEntity.getAddress();
        billingPincode = billingEntity.getPincode();
        billingCity = billingEntity.getCity();
        billingGstin = billingEntity.getGstinUin();
        billingStateCode = extractStateCodeFromGstin(billingEntity.getGstinUin());
      } else {
        billingAddress = buyer.getAddress();
        billingPincode = buyer.getPincode();
        billingCity = buyer.getCity();
        billingGstin = buyer.getGstinUin();
        billingStateCode = extractStateCodeFromGstin(buyer.getGstinUin());
      }
      
      // Use shipping entity if available, otherwise use billing address
      if (shippingEntity != null) {
        shippingAddress = shippingEntity.getAddress();
        shippingPincode = shippingEntity.getPincode();
        shippingCity = shippingEntity.getCity();
        shippingStateCode = extractStateCodeFromGstin(shippingEntity.getGstinUin());
      } else {
        shippingAddress = billingAddress;
        shippingPincode = billingPincode;
        shippingCity = billingCity;
        shippingStateCode = billingStateCode;
      }
    } else if (challan.getVendor() != null) {
      // Vendor challan
      Vendor vendor = challan.getVendor();
      consigneeName = vendor.getVendorName();
      
      VendorEntity billingEntity = challan.getVendorBillingEntity();
      VendorEntity shippingEntity = challan.getVendorShippingEntity();
      
      // Use billing entity if available, otherwise use vendor's main address
      if (billingEntity != null) {
        billingAddress = billingEntity.getAddress();
        billingPincode = billingEntity.getPincode();
        billingCity = billingEntity.getCity();
        billingGstin = billingEntity.getGstinUin();
        billingStateCode = extractStateCodeFromGstin(billingEntity.getGstinUin());
      } else {
        billingAddress = vendor.getAddress();
        billingPincode = vendor.getPincode();
        billingCity = vendor.getCity();
        billingGstin = vendor.getGstinUin();
        billingStateCode = extractStateCodeFromGstin(vendor.getGstinUin());
      }
      
      // Use shipping entity if available, otherwise use billing address
      if (shippingEntity != null) {
        shippingAddress = shippingEntity.getAddress();
        shippingPincode = shippingEntity.getPincode();
        shippingCity = shippingEntity.getCity();
        shippingStateCode = extractStateCodeFromGstin(shippingEntity.getGstinUin());
      } else {
        shippingAddress = billingAddress;
        shippingPincode = billingPincode;
        shippingCity = billingCity;
        shippingStateCode = billingStateCode;
      }
    } else {
      throw new IllegalArgumentException("Delivery Challan must have either a buyer or vendor");
    }

    // Parse addresses
    AddressComponents fromAddress = parseAddress(tenant.getAddress(), tenant.getPincode(), tenant.getCity());
    AddressComponents billingAddressComponents = parseAddress(billingAddress, billingPincode, billingCity);
    AddressComponents shippingAddressComponents = parseAddress(shippingAddress, shippingPincode, shippingCity);

    // Extract state codes
    Integer fromStateCode = extractStateCodeFromGstin(tenant.getGstin());
    
    // Determine if interstate
    Boolean isInterState = !fromStateCode.equals(shippingStateCode);

    // Map line items
    List<EwayBillItem> items;
    if (challan.getIsVendorChallan()) {
      items = mapChallanVendorLineItems(challan.getChallanVendorLineItems(), isInterState);
    } else {
      items = mapChallanLineItems(challan.getLineItems(), isInterState);
    }
    
    // Get main HSN code from first item
    Long mainHsnCode = items.isEmpty() ? 0L : items.get(0).getHsnCode();
    
    // Determine supply type and sub-supply type for challan
    // For challans: Supply Type = "O" (Outward), Sub Supply Type depends on purpose
    Integer subSupplyType = determineChallanSubSupplyType(challan);

    return EwayBillData.builder()
        // User GSTIN (same as fromGstin for supplier)
        .userGstin(tenant.getGstin())
        
        .supplyType("O") // Outward supply
        .subSupplyType(subSupplyType) // Determined by challan type
        .subSupplyDesc(challan.getTransportationReason()) // Reason for transportation
        .docType("CHL") // Challan document type
        .docNo(truncate(challan.getChallanNumber(), 16))
        .docDate(formatDate(challan.getChallanDateTime()))
        
        // Transaction type - Required by portal
        .transactionType(transactionType)

        // From (Supplier/Consignor)
        .fromGstin(tenant.getGstin())
        .fromTrdName(truncate(tenant.getTenantName(), 100))
        .fromAddr1(truncate(fromAddress.getAddr1(), 100))
        .fromAddr2(truncate(fromAddress.getAddr2(), 100))
        .fromPlace(truncate(fromAddress.getCity(), 50))
        .fromPincode(fromAddress.getPincode())
        .actFromStateCode(fromStateCode)
        .fromStateCode(fromStateCode)

        // To (Consignee - Ship To Address)
        .toGstin(billingGstin)
        .toTrdName(truncate(consigneeName, 100))
        .toAddr1(truncate(shippingAddressComponents.getAddr1(), 100))
        .toAddr2(truncate(shippingAddressComponents.getAddr2(), 100))
        .toPlace(truncate(shippingAddressComponents.getCity(), 50))
        .toPincode(shippingAddressComponents.getPincode())
        .actToStateCode(shippingStateCode) // Actual delivery state (Ship To)
        .toStateCode(billingStateCode) // Billing state (Bill To)

        // Financial details
        .totalValue(challan.getTotalTaxableValue().doubleValue())
        .cgstValue(challan.getTotalCgstAmount().doubleValue())
        .sgstValue(challan.getTotalSgstAmount().doubleValue())
        .igstValue(challan.getTotalIgstAmount().doubleValue())
        .cessValue(0.0)
        .cessNonAdvolValue(0.0)
        .otherValue(0.0)
        .totInvValue(challan.getTotalValue().doubleValue())

        // Transportation
        .transporterId(challan.getTransporterId() != null ? challan.getTransporterId() : "")
        .transporterName(challan.getTransporterName() != null ? truncate(challan.getTransporterName(), 100) : "")
        .transDocNo("0") // Usually not applicable for challans
        .transMode(mapTransportModeToInt(challan.getTransportationMode()))
        .transDistance(0)
        .transDocDate(formatDate(challan.getChallanDateTime()))
        .vehicleNo(challan.getVehicleNumber() != null ? challan.getVehicleNumber() : "")
        .vehicleType("R") // Regular
        
        // Main HSN Code
        .mainHsnCode(mainHsnCode)

        // Items
        .itemList(items)
        .build();
  }

  /**
   * Determine transaction type for delivery challan based on billing and shipping addresses
   */
  private String determineChallanTransactionType(DeliveryChallan challan) {
    if (challan.getBuyer() != null) {
      BuyerEntity billingEntity = challan.getBuyerBillingEntity();
      BuyerEntity shippingEntity = challan.getBuyerShippingEntity();
      
      if (billingEntity == null || shippingEntity == null) {
        return "1";
      }
      
      if (billingEntity.getId().equals(shippingEntity.getId())) {
        return "1";
      }
      
      return "2";
    } else if (challan.getVendor() != null) {
      VendorEntity billingEntity = challan.getVendorBillingEntity();
      VendorEntity shippingEntity = challan.getVendorShippingEntity();
      
      if (billingEntity == null || shippingEntity == null) {
        return "1";
      }
      
      if (billingEntity.getId().equals(shippingEntity.getId())) {
        return "1";
      }
      
      return "2";
    }
    
    return "1"; // Default to regular
  }

  /**
   * Determine sub-supply type for delivery challan
   * Based on GST E-Way Bill sub-supply type codes for challans:
   * 4 = Job Work
   * 5 = For Own Use
   * 6 = Job work Returns
   * 7 = Sales Return
   * 8 = Others (default for challans)
   */
  private Integer determineChallanSubSupplyType(DeliveryChallan challan) {
    switch (challan.getChallanType()) {
      case JOB_WORK:
        return 4; // Job Work
      case RETURN_GOODS:
        return 7; // Sales Return
      default:
        return 8; // Others
    }
  }

  /**
   * Map challan line items to E-Way Bill items
   */
  private List<EwayBillItem> mapChallanLineItems(List<ChallanLineItem> lineItems, Boolean isInterState) {
    List<EwayBillItem> ewbItems = new ArrayList<>();
    int itemNo = 1;

    for (ChallanLineItem lineItem : lineItems) {
      // For inter-state: IGST applicable, CGST/SGST = -1
      // For intra-state: CGST/SGST applicable, IGST = -1
      Integer cgstRate = (isInterState != null && isInterState) ? -1 : 
                         (lineItem.getCgstRate() != null ? lineItem.getCgstRate().intValue() : -1);
      Integer sgstRate = (isInterState != null && isInterState) ? -1 : 
                         (lineItem.getSgstRate() != null ? lineItem.getSgstRate().intValue() : -1);
      Integer igstRate = (isInterState != null && isInterState) ? 
                         (lineItem.getIgstRate() != null ? lineItem.getIgstRate().intValue() : -1) : -1;
      
      ewbItems.add(EwayBillItem.builder()
                       .itemNo(itemNo++)
                       .productName(truncate(lineItem.getItemName(), 100))
                       .productDesc(truncate(lineItem.getItemName(), 300))
                       .hsnCode(parseHsnCode(lineItem.getHsnCode()))
                       .quantity(lineItem.getQuantity().doubleValue())
                       .qtyUnit(mapUnitToUqc(lineItem.getUnitOfMeasurement()))
                       .taxableAmount(lineItem.getTaxableValue().doubleValue())
                       .sgstRate(sgstRate)
                       .cgstRate(cgstRate)
                       .igstRate(igstRate)
                       .cessRate(-1) // -1 if not applicable
                       .cessNonAdvol(-1) // -1 if not applicable
                       .build());
    }

    return ewbItems;
  }

  /**
   * Map vendor challan line items to E-Way Bill items
   */
  private List<EwayBillItem> mapChallanVendorLineItems(List<ChallanVendorLineItem> lineItems, Boolean isInterState) {
    List<EwayBillItem> ewbItems = new ArrayList<>();
    int itemNo = 1;

    for (ChallanVendorLineItem lineItem : lineItems) {
      Integer cgstRate = (isInterState != null && isInterState) ? -1 : 
                         (lineItem.getCgstRate() != null ? lineItem.getCgstRate().intValue() : -1);
      Integer sgstRate = (isInterState != null && isInterState) ? -1 : 
                         (lineItem.getSgstRate() != null ? lineItem.getSgstRate().intValue() : -1);
      Integer igstRate = (isInterState != null && isInterState) ? 
                         (lineItem.getIgstRate() != null ? lineItem.getIgstRate().intValue() : -1) : -1;
      
      ewbItems.add(EwayBillItem.builder()
                       .itemNo(itemNo++)
                       .productName(truncate(lineItem.getItemName(), 100))
                       .productDesc(truncate(lineItem.getItemName(), 300))
                       .hsnCode(parseHsnCode(lineItem.getHsnCode()))
                       .quantity(lineItem.getQuantity().doubleValue())
                       .qtyUnit(mapUnitToUqc(lineItem.getUnitOfMeasurement()))
                       .taxableAmount(lineItem.getTaxableValue().doubleValue())
                       .sgstRate(sgstRate)
                       .cgstRate(cgstRate)
                       .igstRate(igstRate)
                       .cessRate(-1)
                       .cessNonAdvol(-1)
                       .build());
    }

    return ewbItems;
  }

  /**
   * Internal class to hold parsed address components
   */
  @Data
  @Builder
  @AllArgsConstructor
  private static class AddressComponents {

    private String addr1;
    private String addr2;
    private String city;
    private Integer pincode;
  }
}
