package com.jangid.forging_process_management_service.entities.gst;

import lombok.Getter;

@Getter
public enum TransportationMode {
    ROAD("Road"),
    RAIL("Rail"),
    AIR("Air"),
    SHIP("Ship"),
    OTHER("Other");

    private final String displayName;

    TransportationMode(String displayName) {
        this.displayName = displayName;
    }
}
