package com.jangid.forging_process_management_service.entities.gst;

import lombok.Getter;

@Getter
public enum InvoiceType {
    REGULAR("Regular Invoice"),
    AMENDED("Amended Invoice"),
    CREDIT_NOTE("Credit Note"),
    DEBIT_NOTE("Debit Note");

    private final String displayName;

    InvoiceType(String displayName) {
        this.displayName = displayName;
    }
}
