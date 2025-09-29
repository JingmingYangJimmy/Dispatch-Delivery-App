package com.delivery.service;

import com.delivery.dto.*;
import com.delivery.entity.Device;
import com.delivery.repository.DeviceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DeviceService {

    @Autowired
    private DeviceRepository deviceRepository;

    public AvailableDevicesResponse getAvailableDevices(double lat, double lng, double radius) {
        List<Device> devices = deviceRepository.findAvailableDevicesNearLocation(lat, lng, radius);

        AvailableDevicesResponse response = new AvailableDevicesResponse();
        response.setDevices(devices.stream().map(this::convertToDto).collect(Collectors.toList()));
        return response;
    }

    private DeviceDto convertToDto(Device device) {
        DeviceDto dto = new DeviceDto();
        dto.setDeviceId(device.getDeviceId());
        dto.setType(device.getType());
        dto.setStationId(device.getStationId());

        Location location = new Location();
        location.setLat(device.getLatitude());
        location.setLng(device.getLongitude());
        dto.setLocation(location);

        dto.setBattery(device.getBattery());
        dto.setMaxWeight(device.getMaxWeight());
        dto.setStatus(device.getStatus());
        return dto;
    }
}