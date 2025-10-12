package com.laioffer.robotcontrol.dto;

import com.fasterxml.jackson.annotation.JsonValue;

/** 仅 tracking 用到的运行态；若要与站点可派发态(available/in_use/locked/maintenance)映射，可在 service 层转换 */
public enum TrackingStatus {
    PENDING("pending"),
    IN_TRANSIT("in_transit"),
    ARRIVED("arrived"),
    CANCELLED("cancelled");

    private final String value;
    TrackingStatus(String v) { this.value = v; }

    @JsonValue
    public String getValue() { return value; }
}
