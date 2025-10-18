package com.jangid.forging_process_management_service.entities.gst;

import lombok.Getter;

@Getter
public enum ChallanType {
    JOB_WORK("Job Work"),
    BRANCH_TRANSFER("Branch Transfer"),
    SAMPLE_DISPATCH("Sample Dispatch"),
    RETURN_GOODS("Return Goods"),
    OTHER("Other");

    private final String displayName;

    ChallanType(String displayName) {
        this.displayName = displayName;
    }
}
