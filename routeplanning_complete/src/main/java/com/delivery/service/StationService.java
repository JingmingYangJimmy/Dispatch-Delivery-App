package com.delivery.service;

import com.delivery.dto.*;
import com.delivery.entity.Station;
import com.delivery.repository.StationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.stream.Collectors;

@Service
public class StationService {

    @Autowired
    private StationRepository stationRepository;

    public StationsResponse getAllStations() {
        StationsResponse response = new StationsResponse();
        response.setStations(stationRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList()));
        return response;
    }

    private StationDto convertToDto(Station station) {
        StationDto dto = new StationDto();
        dto.setStationId(station.getStationId());
        dto.setName(station.getName());

        Location location = new Location();
        location.setLat(station.getLatitude());
        location.setLng(station.getLongitude());
        dto.setLocation(location);

        dto.setAvailableDrones(station.getAvailableDrones());
        dto.setAvailableRobots(station.getAvailableRobots());
        return dto;
    }
}