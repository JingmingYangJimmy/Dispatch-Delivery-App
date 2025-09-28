package com.delivery.routeplanning.controller;

import com.delivery.routeplanning.dto.AvailableDevicesResponse;
import com.delivery.routeplanning.service.DeviceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/devices")
@CrossOrigin(origins = "*")
public class DeviceController {

    @Autowired
    private DeviceService deviceService;

    @GetMapping("/available")
    public AvailableDevicesResponse getAvailableDevices(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "5") double radius) {
        return deviceService.getAvailableDevices(lat, lng, radius);
    }
}