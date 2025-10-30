package com.jangid.forging_process_management_service.entities.gst;

import lombok.Getter;

/**
 * Enum representing the status of a payment.
 */
@Getter
public enum PaymentStatus {
  RECEIVED("Received"),
  PENDING_CLEARANCE("Pending Clearance"),
  REVERSED("Reversed"),
  CANCELLED("Cancelled");

  private final String displayName;

  PaymentStatus(String displayName) {
    this.displayName = displayName;
  }
}

