package com.jangid.forging_process_management_service.entities.gst;

import lombok.Getter;

@Getter
public enum EWayBillStatus {
    ACTIVE("Active"),
    CANCELLED("Cancelled"),
    EXPIRED("Expired");

    private final String displayName;

    EWayBillStatus(String displayName) {
        this.displayName = displayName;
    }
}
