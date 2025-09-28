package com.delivery.routeplanning.controller;

import com.delivery.routeplanning.dto.StationsResponse;
import com.delivery.routeplanning.service.StationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stations")
@CrossOrigin(origins = "*")
public class StationController {

    @Autowired
    private StationService stationService;

    @GetMapping
    public StationsResponse getStations() {
        return stationService.getAllStations();
    }
}