package com.delivery.dto;

import java.util.List;

public class StationsResponse {
    private List<StationDto> stations;

    public List<StationDto> getStations() { return stations; }
    public void setStations(List<StationDto> stations) { this.stations = stations; }
}
