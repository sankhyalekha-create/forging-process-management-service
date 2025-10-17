package com.jangid.forging_process_management_service.entities.order;

import lombok.Getter;

@Getter
public enum WorkType {
    /**
     * Job Work Only: Only processing/manufacturing charges without material cost.
     * Unit price includes only the labor and processing charges.
     */
    JOB_WORK_ONLY("Job Work Only", "Only processing/manufacturing charges without material"),
    
    /**
     * With Material: Material cost + processing/manufacturing charges.
     * Unit price includes both material cost and job work charges.
     */
    WITH_MATERIAL("With Material", "Material cost + processing/manufacturing charges");

    private final String displayName;
    private final String description;

    WorkType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    /**
     * Get WorkType from string value (case-insensitive)
     */
    public static WorkType fromString(String text) {
        if (text != null) {
            for (WorkType workType : WorkType.values()) {
                if (workType.name().equalsIgnoreCase(text)) {
                    return workType;
                }
            }
        }
        return WITH_MATERIAL; // Default
    }

    /**
     * Check if this work type includes material cost
     */
    public boolean includesMaterialCost() {
        return this == WITH_MATERIAL;
    }

    /**
     * Check if this work type is job work only
     */
    public boolean isJobWorkOnly() {
        return this == JOB_WORK_ONLY;
    }
}

