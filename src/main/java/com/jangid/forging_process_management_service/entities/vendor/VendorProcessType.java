package com.jangid.forging_process_management_service.entities.vendor;

public enum VendorProcessType {
    FORGING("Forging"),
    HEAT_TREATMENT("Heat Treatment"),
    MACHINING("Machining"),
    INSPECTION("Inspection");

    private final String displayName;

    VendorProcessType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
} 