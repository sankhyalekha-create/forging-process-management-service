package com.jangid.forging_process_management_service.entities.gst;

import lombok.Getter;

@Getter
public enum InvoiceStatus {
    DRAFT("Draft"),
    GENERATED("Generated"),
    SENT("Sent"),
    PAID("Paid"),
    CANCELLED("Cancelled");

    private final String displayName;

    InvoiceStatus(String displayName) {
        this.displayName = displayName;
    }
}
