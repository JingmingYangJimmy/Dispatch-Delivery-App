package com.delivery.routeplanning.dto;

import java.util.List;

public class AvailableDevicesResponse {
    private List<DeviceDto> devices;

    public List<DeviceDto> getDevices() { return devices; }
    public void setDevices(List<DeviceDto> devices) { this.devices = devices; }
}
