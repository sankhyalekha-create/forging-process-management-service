package com.jangid.forging_process_management_service.entities.gst;

import lombok.Getter;

@Getter
public enum ChallanStatus {
    GENERATED("Generated"),
    DISPATCHED("Dispatched"),
    CONVERTED_TO_INVOICE("Converted to Invoice"),
    CANCELLED("Cancelled");

    private final String displayName;

    ChallanStatus(String displayName) {
        this.displayName = displayName;
    }
}
