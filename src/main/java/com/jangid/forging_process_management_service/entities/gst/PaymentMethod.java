package com.jangid.forging_process_management_service.entities.gst;

import lombok.Getter;

/**
 * Enum representing various payment methods for invoice payments.
 */
@Getter
public enum PaymentMethod {
  CASH("Cash"),
  CHEQUE("Cheque"),
  BANK_TRANSFER("Bank Transfer"),
  UPI("UPI"),
  NEFT_RTGS("NEFT/RTGS"),
  DEMAND_DRAFT("Demand Draft"),
  CREDIT_CARD("Credit Card"),
  DEBIT_CARD("Debit Card"),
  OTHER("Other");

  private final String displayName;

  PaymentMethod(String displayName) {
    this.displayName = displayName;
  }
}

