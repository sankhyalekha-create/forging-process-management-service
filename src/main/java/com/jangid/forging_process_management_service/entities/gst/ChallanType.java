package com.jangid.forging_process_management_service.entities.gst;

import lombok.Getter;

@Getter
public enum ChallanType {
    JOB_WORK("Job Work"),
    WITH_MATERIAL("With Material"),
    RETURN_GOODS("Return Goods"),
    OTHER("Other");

    private final String displayName;

    ChallanType(String displayName) {
        this.displayName = displayName;
    }
}
