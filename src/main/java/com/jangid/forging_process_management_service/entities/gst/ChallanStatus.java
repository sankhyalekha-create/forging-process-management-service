package com.jangid.forging_process_management_service.entities.gst;

import lombok.Getter;

@Getter
public enum ChallanStatus {
    DRAFT("Draft"),
    GENERATED("Generated"),
    DISPATCHED("Dispatched"),
    DELIVERED("Delivered"),
    CONVERTED_TO_INVOICE("Converted to Invoice"),
    CANCELLED("Cancelled");

    private final String displayName;

    ChallanStatus(String displayName) {
        this.displayName = displayName;
    }
}
