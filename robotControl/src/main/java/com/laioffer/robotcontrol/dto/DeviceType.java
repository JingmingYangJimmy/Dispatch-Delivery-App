package com.laioffer.robotcontrol.dto;

import com.fasterxml.jackson.annotation.JsonValue;

public enum DeviceType {
    ROBOT("robot"),
    DRONE("drone");

    private final String value;
    DeviceType(String v) { this.value = v; }

    @JsonValue
    public String getValue() { return value; }
}
